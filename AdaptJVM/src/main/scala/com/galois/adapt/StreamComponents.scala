package com.galois.adapt

import java.io.File
import java.util.UUID

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.galois.adapt.cdm17._

import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
import GraphDSL.Implicits._
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

  val processTrees = Flow[CDM17]
    .filter(c => (c.isInstanceOf[Subject] && c.asInstanceOf[Subject].subjectType == SUBJECT_PROCESS) || c.isInstanceOf[AdaptProcessingInstruction])
    .statefulMapConcat{() =>
      val p = MutableSet.empty[UUID]

      { case s: Subject => p += s.localPrincipal; List.empty
        case AdaptProcessingInstruction(i) => p.toList }
  }

  def eventsPerPredObj(cmdSource: Source[ProcessingCommand,_], dbMap: HTreeMap[UUID,mutable.SortedSet[Event]]) = Flow[CDM17]
      .collect{ case c: Event if c.predicateObject.isDefined => c }
      .mapConcat(e =>
        if (e.predicateObject2.isDefined) List((e.predicateObject.get, e), (e.predicateObject2.get, e))
        else List((e.predicateObject.get, e)))
      .filter(_._1 != UUID.fromString("00000000-0000-0000-0000-000000000000"))
      .groupBy(Int.MaxValue, _._1)   // TODO: Limited to ~4 billion unique UUIDs!!!
      .merge(cmdSource)
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
              val newSet = existingSet ++= events
              dbMap.put(uuid.get, newSet)
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
      }//.mergeSubstreams


  val fileEventTypes = List(EVENT_CHECK_FILE_ATTRIBUTES, EVENT_CLOSE, EVENT_CREATE_OBJECT, EVENT_DUP, EVENT_EXECUTE, EVENT_FNCTL, EVENT_LINK, EVENT_LSEEK, EVENT_MMAP, EVENT_MODIFY_FILE_ATTRIBUTES, EVENT_OPEN, EVENT_READ, EVENT_RENAME, EVENT_TRUNCATE, EVENT_UNLINK, EVENT_UPDATE, EVENT_WRITE)

  val netflowEventTypes = List(EVENT_ACCEPT, EVENT_CONNECT, EVENT_OPEN, EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG, EVENT_SENDTO, EVENT_SENDMSG, EVENT_WRITE)

  val fileFeatures = Flow[(UUID, mutable.SortedSet[Event])]
    .map{ case (u, es) =>
      val m = MutableMap.empty[String,Any]
      m("execAfterWriteByNetFlowReadingProcess") = false   // TODO
      m("execAfterPermissionChangeToExecutable") = false   // TODO
      m("deletedImmediatelyAfterExec") = es.dropWhile(_.eventType != EVENT_EXECUTE).drop(1).headOption.exists(_.eventType == EVENT_UNLINK)
      m("deletedRightAfterProcessWithOpenNetFlowsWrites") = false   // TODO
      m("isReadByAProcessWritingWritingToNetFlows") = false   // TODO
      m("isInsideTempDirectory") = false   // TODO
      m("execDeleteGapMillis") = es.timeBetween(EVENT_EXECUTE, EVENT_UNLINK)
      m("attribChangeEventGapMillis") = es.timeBetween(EVENT_MODIFY_FILE_ATTRIBUTES, EVENT_EXECUTE)
      m("downloadExecutionGapMillis") = 0   // TODO
      m("uploadDeletionGapMillis") = 0   // TODO
      m("countDistinctProcessesHaveEventToFile") = es.map(_.subject).size
      m("countDistinctNetFlowConnectionsByProcess") = 0   // TODO
      m("totalBytesRead") = es.filter(_.eventType == EVENT_READ).flatMap(_.size).sum
      m("totalBytesWritten") = es.filter(_.eventType == EVENT_WRITE).flatMap(_.size).sum

      fileEventTypes.foreach( t =>
        m("count_"+ t.toString) = es.count(_.eventType == t)
      )
      u -> m
    }


  def printCounter[T](name: String, every: Int = 10000) = Flow[CDM17].statefulMapConcat { () =>
    var counter = 0

    { case item: T =>  // Type is a hack to compile!! No runtime effect because Generic.
        counter = counter + 1
        if (counter % every == 0)
          println(s"$name ingested: $counter")
        List(item)
    }
  }

  implicit class EventCollection(es: Iterable[Event]) {
    def timeBetween(first: EventType, second: EventType): Long = {
      val foundFirst = es.dropWhile(_.eventType != first)
      val foundSecond = foundFirst.drop(1).find(_.eventType == second)
      foundFirst.headOption.flatMap(f => foundSecond.map(s => (s.timestampNanos / 1e6 - (f.timestampNanos / 1e6)).toLong)).getOrElse(0L)
    }
  }
}



object TestGraph extends App {
  implicit val system = ActorSystem("test")
  implicit val ec = system.dispatcher
  implicit val mat = ActorMaterializer()

  val path = "/Users/ryan/Desktop/ta1-cadets-cdm17-3.bin" // cdm17_0407_1607.bin" //
  val source = Source.fromIterator(() => CDM17.readData(path, None).get._2.map(_.get))
    .via(FlowComponents.printCounter("CDM Source", 1000000))


  val sink = Sink.actorRef(system.actorOf(Props[PrintActor]()), TimeMarker(0L))

  def commandSource = Source.tick[ProcessingCommand](1 seconds, 6 seconds, CleanUp).buffer(1, OverflowStrategy.backpressure)
    .merge(Source.tick[ProcessingCommand](10 seconds, 10 seconds, Emit).buffer(1, OverflowStrategy.backpressure))
//    .via(FlowComponents.printCounter("Command Source", 1))

  val dbFilePath = "/Users/ryan/Desktop/map.db"
  val db = DBMaker.fileDB(dbFilePath).fileMmapEnable().make()
  val dbMap = db.hashMap("eventsByPredicate").createOrOpen().asInstanceOf[HTreeMap[UUID,mutable.SortedSet[Event]]]
  new File(dbFilePath).deleteOnExit()


  Flow[CDM17]
    .collect{ case e: Event if FlowComponents.fileEventTypes.contains(e.eventType) => e}
    .via(FlowComponents.eventsPerPredObj(commandSource, dbMap).mergeSubstreams)
    .via(FlowComponents.fileFeatures)
    .runWith(source, sink)

//    .map {
//      case (u, es) => u -> es.toList.sliding(2).forall(l => if (l.length > 1) l(0).sequence < l(1).sequence else true)   //es.toList.reverse.dropWhile(_.eventType != EVENT_EXECUTE).find(_.eventType == EVENT_UNLINK).fold(false)(_ => true)
//    }
//    .mergeSubstreams
//    .recover{ case e: Throwable => e.printStackTrace() }
//    .filter(_.isInstanceOf[Tuple2[UUID,_]])
//    .filterNot(_._2)
//    .runWith(source, sink)

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
