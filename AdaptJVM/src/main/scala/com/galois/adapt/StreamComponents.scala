package com.galois.adapt

import java.io.File
import java.nio.file.Paths
import java.util.UUID

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.galois.adapt.cdm17._

import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
import GraphDSL.Implicits._
import akka.util.ByteString
import org.mapdb.DB.TreeSetMaker
import org.mapdb.{DB, DBMaker, HTreeMap, Serializer}

import collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration._

case class SubjectEventCount(
  subjectUuid: UUID,
  filesExecuted: Int,
  netflowsConnected: Int,
  eventCounts: Map[EventType, Int]
)

// TODO: put this somewhere else
trait ProcessingCommand extends CDM17
case class AdaptProcessingInstruction(id: Long) extends ProcessingCommand
case object Emit extends ProcessingCommand
case object CleanUp extends ProcessingCommand

object Streams {

  def processWritesFile(source: Source[CDM17,_], sink: Sink[Any,_]) = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit graph =>
      val bcast = graph.add(Broadcast[CDM17](3))
      val eventBroadcast = graph.add(Broadcast[Event](2))

      val processFilter = Flow[CDM17].filter(s => s.isInstanceOf[Subject] && s.asInstanceOf[Subject].subjectType == SUBJECT_PROCESS).map(_.asInstanceOf[Subject])
      def eventFilter(eType: EventType) = Flow[CDM17].filter(e => e.isInstanceOf[Event] && e.asInstanceOf[Event].eventType == eType).map(_.asInstanceOf[Event])
      val fileFilter = Flow[CDM17].filter(f => f.isInstanceOf[FileObject] && f.asInstanceOf[FileObject].fileObjectType == FILE_OBJECT_FILE).map(_.asInstanceOf[FileObject])

      val processEvents = graph.add(ProcessEvents())
      val fileEvents = graph.add(FileWrites())
      val processEventsFile = graph.add(ProcessWritesFile())

      source ~> bcast.in;                                   eventBroadcast.out(0) ~> processEvents.in1
                bcast.out(0) ~> processFilter ~>                                     processEvents.in0
                bcast.out(1) ~> eventFilter(EVENT_WRITE) ~> eventBroadcast.in;       processEvents.out ~> processEventsFile.in0
                bcast.out(2) ~> fileFilter ~>                                        fileEvents.in0
                                                            eventBroadcast.out(1) ~> fileEvents.in1
                                                                                     fileEvents.out  ~>   processEventsFile.in1
                                                                                                          processEventsFile.out ~> sink
      ClosedShape
    }
  )


  def processCheckOpenGraph(source: Source[CDM17,_], sink: Sink[Any,_]) = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit graph =>
      source.filter(cdm =>
        (cdm.isInstanceOf[Subject] && cdm.asInstanceOf[Subject].subjectType == SUBJECT_PROCESS) ||
        (cdm.isInstanceOf[Event] && cdm.asInstanceOf[Event].eventType == EVENT_OPEN) ||
        (cdm.isInstanceOf[Event] && cdm.asInstanceOf[Event].eventType == EVENT_CHECK_FILE_ATTRIBUTES)
      ) ~> sink
      ClosedShape
    }
  )

  def processEventCounter(source: Source[CDM17,_], sink: Sink[Any,_]) = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit graph =>

      source
        .collect{ case cdm: Event => cdm }
        .groupBy(1000000, _.subject)   // TODO: pull this number out to somewhere else
        .fold[(Option[UUID], Map[EventType,Int])]((None, Map.empty)) { (a, b) =>
          Some(b.subject) -> (a._2 + (b.eventType -> (a._2.getOrElse(b.eventType, 0) + 1)))
        }
        .mergeSubstreams ~> sink

      ClosedShape
    }
  )


  def processUsedNetFlow(source: Source[CDM17,_], sink: Sink[Any,_]) = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit graph =>
      val bcast = graph.add(Broadcast[CDM17](2))
      val processUsedNetFlow = graph.add(ProcessUsedNetFlow())

      val eventTypesWeCareAbout = List(EVENT_ACCEPT, EVENT_CONNECT, EVENT_OPEN, EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG, EVENT_SENDTO, EVENT_SENDMSG, EVENT_WRITE)

      source ~> bcast.in
      bcast.out(0).collect{ case n: NetFlowObject => n} ~> processUsedNetFlow.in0
      bcast.out(1).collect{ case e: Event if eventTypesWeCareAbout.contains(e.eventType) => e} ~> processUsedNetFlow.in1
      processUsedNetFlow.out ~> sink

      ClosedShape
    }
  )


  // Aggregate some statistics for each process. See 'SubjectEventCount' for specifically what is
  // aggregated. Emit downstream a stream of these records every time a 'AdaptProcessingInstruction'
  // is recieved.
  def processEventCount(source: Source[CDM17,_], sink: Sink[SubjectEventCount,_]) = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit graph =>
      val netflowEventTypes = List(
        EVENT_ACCEPT, EVENT_CONNECT, EVENT_OPEN, EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG, EVENT_SENDTO, 
        EVENT_SENDMSG, EVENT_WRITE
      )

      // TODO remove this
      val executions = Source.tick[AdaptProcessingInstruction](10 seconds, 10 seconds, AdaptProcessingInstruction(0))
      
      source
        .collect[CDM17]{
            case e: Event => e
            case t: AdaptProcessingInstruction => t
        }
        .merge(executions)   // TODO remove this
        .statefulMapConcat[SubjectEventCount]{ () =>

          // Note these map is closed over by the following function
          val filesExecuted     = MutableMap[/* Subject */UUID, Set[/* FileObject */UUID]]()
          val netflowsConnected = MutableMap[/* Subject */UUID, Set[/* Netflow    */UUID]]()
          val typesOfEvents     = MutableMap[/* Subject */UUID, Map[EventType, Int]]()
          val subjectUuids      = MutableSet[/* Subject */UUID]()

          // This is the 'flatMap'ing function that gets called for every CDM passed
          {
            case e: Event  =>

              if (EVENT_EXECUTE == e.eventType)
                filesExecuted(e.subject) = filesExecuted.getOrElse(e.subject, Set()) + e.predicateObject.get

              if (netflowEventTypes contains e.eventType)
                netflowsConnected(e.subject) = netflowsConnected.getOrElse(e.subject, Set()) + e.predicateObject.get

              typesOfEvents(e.subject) = {
                val v = typesOfEvents.getOrElse(e.subject,Map())
                v.updated(e.eventType, 1 + v.getOrElse(e.eventType, 0))
              }

              subjectUuids += e.subject

              Nil

            case t: AdaptProcessingInstruction =>
              subjectUuids.toList.map { uuid =>
                SubjectEventCount(
                  uuid,
                  filesExecuted.get(uuid).map(_.size).getOrElse(0),
                  netflowsConnected.get(uuid).map(_.size).getOrElse(0),
                  typesOfEvents.get(uuid).map(_.toMap).getOrElse(Map())
                )
              }
          }
        } ~> sink
      ClosedShape
    }
  )
}


object FlowComponents {

  sealed trait EventsKey
  case object PredicateObjectKey extends EventsKey
  case class SubjectKey(t: Option[EventType]) extends EventsKey

  def eventsGroupedByKey(commandSource: Source[ProcessingCommand,_], dbMap: HTreeMap[UUID,mutable.SortedSet[Event]], key: EventsKey) = {
    val keyPredicate = key match {
      case PredicateObjectKey => Flow[CDM17]
        .collect { case e: Event if e.predicateObject.isDefined => e }
        .mapConcat(e =>
          if (e.predicateObject2.isDefined) List((e.predicateObject.get, e), (e.predicateObject2.get, e))
          else List((e.predicateObject.get, e)))
      case SubjectKey(Some(t)) => Flow[CDM17]
        .collect { case e: Event if e.eventType == t => e.subject -> e }
      case SubjectKey(None) => Flow[CDM17]
        .collect { case e: Event => e.subject -> e }
    }
    keyPredicate
      .filter(_._2.timestampNanos != 0L)
      .filter{ tup =>
        val excluded = List("00000000-0000-0000-0000-000000000000", "071fbdeb-131c-11e7-bfbf-f55a9065b18e", "19f119de-131b-11e7-bfbf-f55a9065b18e").map(UUID.fromString)
        ! excluded.contains(tup._1) }   // TODO: why are there these special cases?!?!?!?!?
      .groupBy(Int.MaxValue, _._1)   // TODO: Limited to ~4 billion unique UUIDs!!!
      .merge(commandSource)
      .statefulMapConcat { () =>
        var uuid: Option[UUID] = None
        val events = mutable.SortedSet.empty[Event](Ordering.by[Event,Long](_.sequence))

        {
          case Emit =>
            val existingSet = dbMap.get(uuid.get)
            val newSet = existingSet ++= events
            dbMap.put(uuid.get, newSet)
            events.clear()
            List(uuid.get -> newSet)

          case CleanUp =>
            if (events.nonEmpty) {
              val existingSet = dbMap.get(uuid.get)
              existingSet ++= events
              dbMap.put(uuid.get, existingSet)
              events.clear()
            }
            List.empty

          case Tuple2(u: UUID, e: Event) =>
            if (uuid.isEmpty) {
              uuid = Some(u)
              val emptySet = mutable.SortedSet.empty[Event](Ordering.by[Event,Long](_.sequence))
              dbMap.put(u, emptySet)
            }
            events += e
            List.empty
        }
      }
  }//.mergeSubstreams


  val fileEventTypes = List(EVENT_CHECK_FILE_ATTRIBUTES, EVENT_CLOSE, EVENT_CREATE_OBJECT, EVENT_DUP, EVENT_EXECUTE, EVENT_FNCTL, EVENT_LINK, EVENT_LSEEK, EVENT_MMAP, EVENT_MODIFY_FILE_ATTRIBUTES, EVENT_OPEN, EVENT_READ, EVENT_RENAME, EVENT_TRUNCATE, EVENT_UNLINK, EVENT_UPDATE, EVENT_WRITE)

  val fileFeatures = Flow[(UUID, mutable.SortedSet[Event])]
    .map{ case (u, eSet) =>
      val eList = eSet.toList
      val m = MutableMap.empty[String,Any]
      m("execAfterWriteByNetFlowReadingProcess") = "TODO"            // TODO: needs pairing with NetFlow events (and join on process UUID)
      m("execAfterPermissionChangeToExecutable") = eList.dropWhile(_.eventType != EVENT_MODIFY_FILE_ATTRIBUTES).contains(EVENT_EXECUTE)
      m("deletedImmediatelyAfterExec") = eList.dropWhile(_.eventType != EVENT_EXECUTE).drop(1).headOption.exists(_.eventType == EVENT_UNLINK)
      m("deletedRightAfterProcessWithOpenNetFlowsWrites") = "TODO"   // TODO: needs pairing with NetFlow events (and join on process UUID)
      m("isReadByAProcessWritingToNetFlows") = "TODO"                // TODO: needs pairing with NetFlow events (and join on process UUID)
      m("isInsideTempDirectory") = eList.flatMap(_.predicateObjectPath).exists(path => List("/tmp", "/temp", "\\temp").exists(tmp => path.toLowerCase.contains(tmp)))  // TODO: revisit the list of temp locations.
      m("execDeleteGapMillis") = eList.timeBetween(Some(EVENT_EXECUTE), Some(EVENT_UNLINK))
      m("attribChangeEventGapMillis") = eList.timeBetween(Some(EVENT_MODIFY_FILE_ATTRIBUTES), Some(EVENT_EXECUTE))
      m("downloadExecutionGapMillis") = "TODO"                       // TODO: needs pairing with NetFlow events (and join on process UUID)
      m("uploadDeletionGapMillis") = "TODO"                          // TODO: needs pairing with NetFlow events (and join on process UUID)
      m("countDistinctProcessesHaveEventToFile") = eSet.map(_.subject).size
      m("countDistinctNetFlowConnectionsByProcess") = "This should probably be on Processes"  // TODO: don't do.
      m("totalBytesRead") = eList.filter(_.eventType == EVENT_READ).flatMap(_.size).sum
      m("totalBytesWritten") = eList.filter(_.eventType == EVENT_WRITE).flatMap(_.size).sum
      fileEventTypes.foreach( t =>
        m("count_"+ t.toString) = eSet.count(_.eventType == t)
      )
      u -> m
    }

  def testFileFeatureExtractor(commandSource: Source[ProcessingCommand,_], db: DB) = {
    val fileEventsDBMap = db.hashMap("FileEventsByPredicate").createOrOpen().asInstanceOf[HTreeMap[UUID, mutable.SortedSet[Event]]]
    Flow[CDM17]
      .collect{ case e: Event if FlowComponents.fileEventTypes.contains(e.eventType) => e}
      .via(eventsGroupedByKey(commandSource, fileEventsDBMap, PredicateObjectKey).mergeSubstreams)
      .via(fileFeatures)
  }


  val netflowEventTypes = List(EVENT_ACCEPT, EVENT_CONNECT, EVENT_OPEN, EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG, EVENT_SENDTO, EVENT_SENDMSG, EVENT_WRITE)

  def netFlowFeatureExtractor = Flow[(UUID, mutable.SortedSet[Event])]
    .map { case (u, eSet) =>
      val eList = eSet.toList
      val m = MutableMap.empty[String, Any]
      m("execCountByThisNetFlowsProcess") = "This should probably be on the Process"   // TODO: don't do.
      m("lifetimeWriteRateBytesPerSecond") = eSet.sizePerSecond(EVENT_WRITE)
      m("lifetimeReadRateBytesPerSecond") = eSet.sizePerSecond(EVENT_READ)
      m("duration-SecondsBetweenFirstAndLastEvent") = eSet.timeBetween(None, None) / 1000
      m("countOfDistinctSubjectsWithEventToThisNetFlow") = eSet.map(_.subject).size
      m("distinctFileReadCountByProcessesWritingToThisNetFlow") = "TODO"                                // TODO: needs pairing with Files (and join on Process UUID)
      m("totalBytesRead") = eList.collect{ case e if e.eventType == EVENT_READ => e.size.getOrElse(0L)}.sum
      m("totalBytesWritten") = eList.collect{ case e if e.eventType == EVENT_WRITE => e.size.getOrElse(0L)}.sum
      netflowEventTypes.foreach( t =>
        m("count_"+ t.toString) = eSet.count(_.eventType == t)
      )
      u -> m
    }

  def testNetFlowFeatureExtractor(commandSource: Source[ProcessingCommand,_], db: DB) = {
    val netFlowEventsDBMap = db.hashMap("NetFlowEventsByPredicate").createOrOpen().asInstanceOf[HTreeMap[UUID, mutable.SortedSet[Event]]]
    Flow[CDM17]
      .collect{ case e: Event if FlowComponents.netflowEventTypes.contains(e.eventType) => e}
      .via(eventsGroupedByKey(commandSource, netFlowEventsDBMap, PredicateObjectKey).mergeSubstreams)
      .via(netFlowFeatureExtractor)
  }




  val processEventTypes = EventType.values.toList

  def processFeatureExtractor = Flow[(UUID, mutable.SortedSet[Event])]
    .map { case (u, eSet) =>
      val eList = eSet.toList
      val m = MutableMap.empty[String, Any]
      m("countOfImmediateChildProcesses") = "TODO"                                        // TODO: needs process tree
      m("countOfAllChildProcessesInTree") = "TODO"                                        // TODO: needs process tree
      m("countOfUniquePortAccesses") = "TODO"                                             // TODO: needs pairing with NetFlows
      m("parentProcessUUID") = "I THINK WE DON'T NEED THIS"                               // TODO: don't do.
      // TODO: consider emitting the collected Process Tree
      m("countOfDistinctMemoryObjectsMProtected") = eSet.collect { case e if e.eventType == EVENT_MPROTECT && e.predicateObject.isDefined => e.predicateObject }.size
      m("isProcessRunning_cmd.exe_or-powershell.exe_whileParentRunsAnotherExe") = "TODO"  // TODO: needs process tree
      m("countOfAllConnect+AcceptEventsToPorts22or443") = "TODO"                          // TODO: needs pairing with NetFlows
      m("countOfAllConnect+AcceptEventsToPortsOtherThan22or443") = "TODO"                 // TODO: needs pairing with NetFlows
      m("isReferringPasswordFile") = "I HAVE NO IDEA WHAT THIS MEANS"                     // TODO: ¯\_(ツ)_/¯
      m("readsFromNetFlowThenWritesAFileThenExecutesTheFile") = "TODO"                    // TODO: needs pairing with NetFlows
      m("changesFilePermissionsThenExecutesIt") = eList.dropWhile(_.eventType != EVENT_MODIFY_FILE_ATTRIBUTES).exists(_.eventType == EVENT_EXECUTE)
      m("executedThenImmediatelyDeletedAFile") = eList.groupBy(_.predicateObject).-(None).values.exists(l => l.sortBy(_.sequence).dropWhile(_.eventType != EVENT_EXECUTE).drop(1).headOption.exists(_.eventType == EVENT_UNLINK))
      m("readFromNetFlowThenDeletedFile") = "TODO"                                        // TODO: needs pairing with NetFlows
      // TODO: consider: process takes any local action after reading from NetFlow
      m("countOfDistinctFileWrites") = eSet.collect { case e if e.eventType == EVENT_WRITE && e.predicateObject.isDefined => e.predicateObject }.size
      m("countOfFileUploads") = "TODO"                                                    // TODO: needs pairing with Files (to ensure reads are from Files)
      m("countOfFileDownloads") = "TODO"                                                  // TODO: needs pairing with Files (to ensure writes are to Files)
      m("isAccessingTempDirectory") = eList.flatMap(e => List(e.predicateObjectPath, e.predicateObject2Path).flatten).exists(path => List("/tmp", "/temp", "\\temp").exists(tmp => path.toLowerCase.contains(tmp)))  // TODO: revisit the list of temp locations.
      m("thisProcessIsTheObjectOfA_SETUID_Event") = "TODO"                                // TODO: needs process UUID from predicateObject field (not subject)
      m("totalBytesSentToNetFlows") = eList.collect { case e if e.eventType == EVENT_SENDTO => e.size.getOrElse(0L)}.sum
      m("totalBytesReceivedFromNetFlows") = eList.collect { case e if e.eventType == EVENT_RECVFROM => e.size.getOrElse(0L)}.sum
      processEventTypes.foreach( t =>
        m("count_"+ t.toString) = eSet.count(_.eventType == t)
      )
      u -> m
    }

  def testProcessFeatureExtractor(commandSource: Source[ProcessingCommand,_], db: DB) = {
    val processEventsDBMap = db.hashMap("ProcessEventsByPredicate").createOrOpen().asInstanceOf[HTreeMap[UUID, mutable.SortedSet[Event]]]
    Flow[CDM17]
      .via(eventsGroupedByKey(commandSource, processEventsDBMap, SubjectKey(None)).mergeSubstreams)
      .via(processFeatureExtractor)
  }





  def printCounter[T](name: String, every: Int = 10000) = Flow[CDM17].statefulMapConcat { () =>
    var counter = 0

    { case item: T =>  // Type T is a hack to enable compilation!! No runtime effect because Generic.
        counter = counter + 1
        if (counter % every == 0)
          println(s"$name ingested: $counter")
        List(item)
    }
  }


  val uuidMapToCSVPrinterSink = Flow[(UUID, mutable.Map[String,Any])]
    .map{ case (u, m) =>
      s"$u,${m.toList.sortBy(_._1).map(_._2).mkString(",")}"
    }.toMat(Sink.foreach(println))(Keep.right)


  def csvFileSink(path: String) = Flow[(UUID, mutable.Map[String,Any])]
    .statefulMapConcat{ () =>
      var wroteHeader = false

      { case Tuple2(u: UUID, m: mutable.Map[String,Any]) =>
        val headerList = if (! wroteHeader) {
          wroteHeader = true
          List(ByteString(s"uuid,${m.toList.sortBy(_._1).map(_._1).mkString(",")}\n"))
        } else List.empty
        headerList ++ List(ByteString(s"$u,${m.toList.sortBy(_._1).map(_._2).mkString(",")}\n"))
      }
    }.toMat(FileIO.toPath(Paths.get(path)))(Keep.right)


  type Milliseconds = Long

  implicit class EventCollection(es: Iterable[Event]) {
    def timeBetween(first: Option[EventType], second: Option[EventType]): Milliseconds = {
      val foundFirst = if (first.isDefined) es.dropWhile(_.eventType != first.get) else es
      val foundSecond = if (second.isDefined) foundFirst.drop(1).find(_.eventType == second.get) else es.lastOption
      foundFirst.headOption.flatMap(f => foundSecond.map(s => (s.timestampNanos / 1e6 - (f.timestampNanos / 1e6)).toLong)).getOrElse(0L)
    }

    def sizePerSecond(t: EventType): Float = {
      val events = es.filter(_.eventType == t)
      val lengthOpt = events.headOption.flatMap(h => events.lastOption.map(l => l.timestampNanos / 1e9 - (h.timestampNanos / 1e9)))
      val totalSize = events.toList.map(_.size.getOrElse(0L)).sum
      lengthOpt.map(l => if (l > 0D) totalSize / l else 0D).getOrElse(0D).toFloat
    }
  }
}



object TestGraph extends App {
  implicit val system = ActorSystem("test")
  implicit val ec = system.dispatcher
  implicit val mat = ActorMaterializer()

  val path = "/Users/ryan/Desktop/ta1-cadets-cdm17-3.bin" // cdm17_0407_1607.bin" //
  val source = Source.fromIterator[CDM17](() => CDM17.readData(path, None).get._2.map(_.get))
    .via(FlowComponents.printCounter("CDM Source", 1e6.toInt))

  val printSink = Sink.actorRef(system.actorOf(Props[PrintActor]()), TimeMarker(0L))

  // TODO: this should be a single source (instead of multiple copies) that broadcasts into all the necessary places.
  val commandSource = Source.tick[ProcessingCommand](1 seconds, 6 seconds, CleanUp).buffer(1, OverflowStrategy.backpressure)
    .merge(Source.tick[ProcessingCommand](120 seconds, 620 seconds, Emit).buffer(1, OverflowStrategy.backpressure))
//    .via(FlowComponents.printCounter("Command Source", 1))

  val dbFilePath = "/Users/ryan/Desktop/map.db"
  val db = DBMaker.fileDB(dbFilePath).fileMmapEnable().make()
  new File(dbFilePath).deleteOnExit()  // Only meant as ephemeral on-disk storage.


//  FlowComponents.testFileFeatureExtractor(commandSource, db)
//    .runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/fileFeatures.csv"))

  FlowComponents.testNetFlowFeatureExtractor(commandSource, db)
    .runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/netflowFeatures.csv"))

//  FlowComponents.testProcessFeatureExtractor(commandSource, db)
//    .runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/processFeatures.csv"))



  //  commandSource.runWith(sink)
}




























sealed trait Multiplicity
case object One extends Multiplicity
case object Many extends Multiplicity

case class Join[A,B,K](
  in0Key: A => K,
  in1Key: B => K,
  in0Multiplicity: Multiplicity = Many, // will there be two elements streamed for which 'in0Key' produces the same value
  in1Multiplicity: Multiplicity = Many  // will there be two elements streamed for which 'in1Key' produces the same value
) extends GraphStage[FanInShape2[A, B, (K,A,B)]] {
  val shape = new FanInShape2[A, B, (K,A,B)]("Join")
  
  def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {

    val in0Stored = MutableMap.empty[K,Set[A]]
    val in1Stored = MutableMap.empty[K,Set[B]]

    setHandler(shape.in0, new InHandler {
      def onPush() = {
        val a: A = grab(shape.in0)
        val k: K = in0Key(a)

        in1Stored.getOrElse(k,Set()) match {
          case s if s.isEmpty =>
            in0Stored(k) = in0Stored.getOrElse(k,Set[A]()) + a

          case bs =>
            for (b <- bs)
              push(shape.out, (k,a,b))

            if (in0Multiplicity == One)
              in0Stored -= k

            if (in1Multiplicity == One)
              in1Stored -= k
        }
      }
    })

    setHandler(shape.in1, new InHandler {
      def onPush() = {
        val b: B = grab(shape.in1)
        val k: K = in1Key(b)

        in0Stored.getOrElse(k,Set()) match {
          case s if s.isEmpty =>
            in1Stored(k) = in1Stored.getOrElse(k,Set[B]()) + b

          case as =>
            for (a <- as)
              push(shape.out, (k,a,b))

            if (in0Multiplicity == One)
              in0Stored -= k

            if (in1Multiplicity == One)
              in1Stored -= k
        }
      }
    })

    setHandler(shape.out, new OutHandler {
      def onPull() = {
        if (!hasBeenPulled(shape.in0)) pull(shape.in0)
        if (!hasBeenPulled(shape.in1)) pull(shape.in1)
      }
    })
  }
}




class PrintActor extends Actor {
  def receive = {
    case t: TimeMarker => println("TimeMarker!!!")
    case p: AdaptProcessingInstruction => println("Make it so!")
    case x: ProcessingCommand => println(s"Processing Command: $x")
    case x => println(x)
  }
}



case class ProcessUsedNetFlow() extends GraphStage[FanInShape2[NetFlowObject, Event, UUID]] {
  val shape = new FanInShape2[NetFlowObject, Event, UUID]("ProcessUsedNetFlow")
  def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    type ProcessUUID = UUID
    type EventUUID = UUID
    type NetFlowUUID = UUID

    val netflows = MutableSet.empty[NetFlowUUID]
    val events = MutableSet.empty[Event]
    val alreadySent = MutableSet.empty[ProcessUUID]

    setHandler(shape.in0, new InHandler {
      def onPush() = {
        val n = grab(shape.in0)
        netflows += n.uuid
        events.collect {
          case e if e.predicateObject.isDefined && e.predicateObject.get == n.uuid => e
        }.headOption.fold(
          pull(shape.in0)
        ) { e =>
          events -= e
          if ( ! alreadySent.contains(e.subject)) {
            push(shape.out, e.subject)
            alreadySent += e.subject
          }
          else pull(shape.in0)
        }
      }
    })

    setHandler(shape.in1, new InHandler {
      def onPush() = {
        val e = grab(shape.in1)
        e.predicateObject match {
          case None =>
            pull(shape.in1)
          case Some(netflowUuid) =>
            if (netflows.contains(netflowUuid)) {
              if (alreadySent contains e.subject) {
                pull(shape.in1)
              } else {
                push(shape.out, e.subject)
                alreadySent += e.subject
              }
            } else {
              events += e
              pull(shape.in1)
            }
        }
      }
    })

    setHandler(shape.out, new OutHandler {
      def onPull() = {
        if ( ! hasBeenPulled(shape.in0)) pull(shape.in0)
        if ( ! hasBeenPulled(shape.in1)) pull(shape.in1)
      }
    })
  }

}


class ProcessCheckOpenActor() extends Actor {
  type ProcessUUID = UUID
  val processes = MutableMap.empty[ProcessUUID, Subject]
  val opens = MutableMap.empty[ProcessUUID, List[Event]]
  val checks = MutableMap.empty[ProcessUUID, List[Event]]
  def receive = {
    case e: Event if e.eventType == EVENT_OPEN =>
      opens(e.subject) = opens.getOrElse(e.subject, List.empty[Event]) :+ e
    case e: Event if e.eventType == EVENT_CHECK_FILE_ATTRIBUTES =>
      checks(e.subject) = checks.getOrElse(e.subject, List.empty[Event]) :+ e
      println("check")
    case p: Subject =>
      processes(p.uuid) = p
    case t: TimeMarker =>
      println(s"Opens: ${opens.size}\nChecks: ${checks.size}")
      val ratio = opens.map{ case (k,v) => k -> (checks.getOrElse(k, List.empty).length / v.length) }.filterNot(_._2 == 0)
      val sorted = ratio.toList.sortBy(_._2).reverse
      sorted foreach println
  }
}


case class ProcessEvents() extends GraphStage[FanInShape2[Subject, Event, (Subject,Event)]] {
  val shape = new FanInShape2[Subject, Event, (Subject,Event)]("ProcessEvents")
  def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    type ProcessUUID = UUID
    val processes = MutableMap.empty[ProcessUUID, Subject]
    val unmatchedWrites = MutableMap.empty[ProcessUUID, Set[Event]]
    var sendingBuffer = List.empty[(Subject,Event)]

    setHandler(shape.in0, new InHandler {
      def onPush() = {
        val p = grab(shape.in0)
        processes(p.uuid) = p
        unmatchedWrites.get(p.uuid) match {
          case None =>
            pull(shape.in0)
          case Some(writes) => {
            unmatchedWrites -= p.uuid
            val wList = writes.toList
            wList.headOption foreach { w =>
              push(shape.out, p -> w)
            }
            if (wList.size > 1) sendingBuffer = sendingBuffer ++ wList.tail.map(p -> _)
          }
        }
      }
    })

    setHandler(shape.in1, new InHandler {
      def onPush() = {
        val e = grab(shape.in1)
        val processUuid = e.subject
        processes.get(processUuid) match {
          case None =>
            unmatchedWrites(processUuid) = unmatchedWrites.getOrElse(processUuid, Set.empty[Event]) + e
            pull(shape.in1)
          case Some(p) =>
            push(shape.out, p -> e)
        }
      }
    })

    setHandler(shape.out, new OutHandler {
      def onPull() = {
        if (sendingBuffer.nonEmpty) {
          push(shape.out, sendingBuffer.head)
          sendingBuffer = sendingBuffer.drop(1)
        } else {
          if ( ! hasBeenPulled(shape.in0)) pull(shape.in0)
          if ( ! hasBeenPulled(shape.in1)) pull(shape.in1)
        }
      }
    })
  }
}


case class FileWrites() extends GraphStage[FanInShape2[FileObject, Event, (FileObject,Event)]] {
  val shape = new FanInShape2[FileObject, Event, (FileObject,Event)]("FileWrites")

  def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    type FileUUID = UUID
    val files = MutableMap.empty[FileUUID, FileObject]
    val unmatchedWrites = MutableMap.empty[FileUUID, Set[Event]]
    var sendingBuffer = List.empty[(FileObject, Event)]

    setHandler(shape.in0, new InHandler {
      def onPush() = {
        val f = grab(shape.in0)
        files(f.uuid) = f
        unmatchedWrites.get(f.uuid)match {
          case None =>
            pull(shape.in0)
          case Some(writes) => {
            unmatchedWrites -= f.uuid
            val wList = writes.toList
            wList.headOption foreach { w =>
              push(shape.out, f -> w)
            }
            if (wList.size > 1) sendingBuffer = sendingBuffer ++ wList.tail.map(f -> _)
          }
        }
      }
    })

    setHandler(shape.in1, new InHandler {
      def onPush() = {
        val e = grab(shape.in1)
        val fileUuids = List(e.predicateObject, e.predicateObject2).flatten
        fileUuids foreach { fu =>
          files.get(fu) match {
            case None =>
              unmatchedWrites(fu) = unmatchedWrites.getOrElse(fu, Set.empty[Event]) + e
              pull(shape.in1)
            case Some(file) =>
              push(shape.out, file -> e)
          }
        }
      }
    })

    setHandler(shape.out, new OutHandler {
      def onPull() = {
        if (sendingBuffer.nonEmpty) {
          push(shape.out, sendingBuffer.head)
          sendingBuffer = sendingBuffer.drop(1)
        } else {
          if ( ! hasBeenPulled(shape.in0)) pull(shape.in0)
          if ( ! hasBeenPulled(shape.in1)) pull(shape.in1)
        }
      }
    })
  }
}


case class ProcessWritesFile() extends GraphStage[FanInShape2[(Subject, Event), (FileObject, Event), (Subject, Event, FileObject)]] {
  val shape = new FanInShape2[(Subject, Event), (FileObject, Event), (Subject, Event, FileObject)]("ProcessWritesFile")
  def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {

    //    val unmatchedProcesses = MutableMap.empty[Subject,Set[Event]]   // ???
    //    val unmatchedFiles = MutableMap.empty[FileObject, Set[Event]]   // ???
    val unmatchesProcesses = MutableMap.empty[Event, Subject]  // todo: probably inefficient copies of Subject when Event differs, but Subject doesn't.
    val unmatchedFiles = MutableMap.empty[Event, FileObject]

    setHandler(shape.in0, new InHandler {
      def onPush() = {
        val (p, e) = grab(shape.in0)
        unmatchedFiles.get(e) match {
          case None =>
            unmatchesProcesses(e) = p
            pull(shape.in0)
          case Some(file) =>
            push(shape.out, (p, e, file))
             unmatchedFiles -= e
        }
      }
    })

    setHandler(shape.in1, new InHandler {
      def onPush() = {
        val (f, e) = grab(shape.in1)
        unmatchesProcesses.get(e) match {
          case None =>
            unmatchedFiles(e) = f
            pull(shape.in1)
          case Some(process) =>
            push(shape.out, (process, e, f))
        }
      }
    })

    setHandler(shape.out, new OutHandler {
      def onPull() = {
        if ( ! hasBeenPulled(shape.in0)) pull(shape.in0)
        if ( ! hasBeenPulled(shape.in1)) pull(shape.in1)
      }
    })
  }
}
