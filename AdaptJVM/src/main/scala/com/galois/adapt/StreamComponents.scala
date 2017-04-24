package com.galois.adapt

import java.nio.file.Paths
import java.util.UUID
import java.io._

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.galois.adapt.cdm17._

import scala.collection.mutable.{ListBuffer, Map => MutableMap, Set => MutableSet}
import GraphDSL.Implicits._
import akka.util.ByteString
import org.mapdb.DB.TreeSetMaker
import org.mapdb.{DB, DBMaker, HTreeMap, Serializer}

import collection.JavaConverters._
import scala.collection.mutable
import scala.sys.process._
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success, Try}
import scala.concurrent.{ExecutionContext, Future}
import com.thinkaurelius.titan.core._
import com.thinkaurelius.titan.graphdb.database.management.ManagementSystem
import com.thinkaurelius.titan.core.schema.{SchemaAction, SchemaStatus, TitanGraphIndex}
import org.apache.tinkerpop.gremlin.structure.{Edge, Graph, Vertex}

import scala.io.{Source => FileSource}


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

   //   .map(messages => InsertMessage.ack(messages.last))


  def iforest(implicit ec: ExecutionContext) = Flow[Any]
    .statefulMapConcat[Future[Map[UUID,Float]]]{ () =>

      // Accumulate 'SubjectEventCount' rows
      val subjectEventCountRows = ListBuffer.empty[SubjectEventCount]
      val subjectEventCountHeader = Seq("subjectUuid", "filesExecuted", "netflowsConnected") ++ EventType.values.map(_.toString)

      // Receiving function
      {
        case s: SubjectEventCount =>
          subjectEventCountRows += s
          Nil

        case t: AdaptProcessingInstruction =>

          println("hi")
          List(Future {

            // Make temporary input/output files
            val inputFile: File = File.createTempFile("input",".csv")
            val outputFile: File = File.createTempFile("output",".csv")
            inputFile.deleteOnExit()
            outputFile.deleteOnExit()

            // Write in data
            val writer: FileWriter = new FileWriter(inputFile)
            writer.write(subjectEventCountHeader.mkString("",",","\n"))
            for (SubjectEventCount(uuid, filesExecuted, netflowsConnected, eventCounts) <- subjectEventCountRows) {
                val row = Seq(uuid, filesExecuted, netflowsConnected) ++ EventType.values.map(k => eventCounts.getOrElse(k,0))
                writer.write(row.map(_.toString).mkString("",",","\n"))
            }
            writer.close()

            // Call IForest
            val succeeded = Seq[String]( "../ad/osu_iforest/iforest.exe"
                                       , "-i", inputFile.getCanonicalPath   // input file
                                       , "-o", outputFile.getCanonicalPath  // output file
                                       , "-m", "1"                          // ignore the first column
                                       , "-t", "100"                        // number of trees
                                       ).! == 0
            println(s"Call to IForest ${if (succeeded) "succeeded" else "failed"}.")

            // Read out data
            val buffer = MutableMap[UUID, Float]()

            if (succeeded) {
              val reader: BufferedReader = new BufferedReader(new FileReader(outputFile))
              var line: String = reader.readLine() // first line is the header

              do {
                line = reader.readLine()
                if (line != null) {
                  val cols = line.split(",").map(_.trim)
                  buffer += UUID.fromString(cols(0)) -> cols.last.toFloat
                }
              } while (line != null)

              reader.close()
            }

            // Final map
            buffer.toMap
          })
        case _ => Nil
      }

    }
    .mapAsync[Map[UUID,Float]](1)(x => x)


  // Aggregate some statistics for each process. See 'SubjectEventCount' for specifically what is
  // aggregated. Emit downstream a stream of these records every time a 'AdaptProcessingInstruction'
  // is recieved.
  def processEventCount(source: Source[CDM17,_], sink: Sink[Map[UUID,Float]/*SubjectEventCount*/,_])(implicit ec: ExecutionContext) = RunnableGraph.fromGraph(
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
        .statefulMapConcat[Any /*SubjectEventCount*/]{ () =>

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
              subjectUuids.toList.map(uuid =>
                SubjectEventCount(
                  uuid,
                  filesExecuted.get(uuid).map(_.size).getOrElse(0),
                  netflowsConnected.get(uuid).map(_.size).getOrElse(0),
                  typesOfEvents.get(uuid).map(_.toMap).getOrElse(Map())
                ).asInstanceOf[Any]
              ) ++ List(t)
          }
        } ~> iforest(ec) ~> sink

      ClosedShape
    }
  )
}


object FlowComponents {

  sealed trait EventsKey

  case object PredicateObjectKey extends EventsKey

  case class SubjectKey(t: Option[EventType]) extends EventsKey

  def eventsGroupedByKey(commandSource: Source[ProcessingCommand, _], dbMap: HTreeMap[UUID, mutable.SortedSet[Event]], key: EventsKey) = {
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
      .filterNot { tup =>
        val excluded = List("00000000-0000-0000-0000-000000000000", "071fbdeb-131c-11e7-bfbf-f55a9065b18e", "19f119de-131b-11e7-bfbf-f55a9065b18e").map(UUID.fromString)
        excluded.contains(tup._1)
      } // TODO: why are there these special cases?!?!?!?!?
      .groupBy(Int.MaxValue, _._1) // TODO: Limited to ~4 billion unique UUIDs!!!
      .merge(commandSource)
      .statefulMapConcat { () =>
        var uuid: Option[UUID] = None
        val events = mutable.SortedSet.empty[Event](Ordering.by[Event, Long](_.sequence))

        {
          case Emit =>
            val existingSet = dbMap.getOrDefault(uuid.get, mutable.SortedSet.empty[Event](Ordering.by[Event, Long](_.sequence)))
            existingSet ++= events
            dbMap.put(uuid.get, existingSet)
            events.clear()
            List(uuid.get -> existingSet)

          case CleanUp =>
            if (events.nonEmpty) {
              val existingSet = dbMap.getOrDefault(uuid.get, mutable.SortedSet.empty[Event](Ordering.by[Event, Long](_.sequence)))
              existingSet ++= events
              dbMap.put(uuid.get, existingSet)
              events.clear()
            }
            List.empty

          case Tuple2(u: UUID, e: Event) =>
            if (uuid.isEmpty) uuid = Some(u)
            //            val emptySet = mutable.SortedSet.empty[Event](Ordering.by[Event, Long](_.sequence))
            //            dbMap.put(u, emptySet)
            events += e
            List.empty
        }
      }
  }


  def sortedEventAccumulator[K](groupBy: ((UUID,Event)) => K, commandSource: Source[ProcessingCommand,_], db: DB) = {
    val dbMap = db.hashMap("sortedEventAccumulator" + Random.nextInt()).createOrOpen().asInstanceOf[HTreeMap[UUID, mutable.SortedSet[Event]]]
    Flow[(UUID,Event)]
      .groupBy(Int.MaxValue, groupBy) // TODO: Limited to ~4 billion unique UUIDs!!!
      .merge(commandSource)
      .statefulMapConcat { () =>
        var uuid: Option[UUID] = None
        val events = mutable.SortedSet.empty[Event](Ordering.by[Event, Long](_.sequence))

        {
          case Emit =>
            val existingSet = dbMap.getOrDefault(uuid.get, mutable.SortedSet.empty[Event](Ordering.by[Event, Long](_.sequence)))
            existingSet ++= events
            dbMap.put(uuid.get, existingSet)
            events.clear()
            List(uuid.get -> existingSet)

          case CleanUp =>
            if (events.nonEmpty) {
              val existingSet = dbMap.getOrDefault(uuid.get, mutable.SortedSet.empty[Event](Ordering.by[Event, Long](_.sequence)))
              existingSet ++= events
              dbMap.put(uuid.get, existingSet)
              events.clear()
            }
            List.empty

          case Tuple2(u: UUID, e: Event) =>
            if (uuid.isEmpty) uuid = Some(u)
            events += e
            List.empty
        }
      }.mergeSubstreams
  }



  def predicateTypeLabeler(commandSource: Source[ProcessingCommand,_], db: DB) = {
    val dbMap = db.hashMap("typeSorter").createOrOpen().asInstanceOf[HTreeMap[UUID,mutable.SortedSet[Event]]]
    Flow[CDM17]
      .collect {
        case e: Event if e.predicateObject.isDefined => e
        case n: NetFlowObject => n.uuid -> "NetFlowObject"
        case f: FileObject => f.uuid -> "FileObject"
        case s: Subject => s.uuid -> "Subject" }
      .mapConcat {
        case e: Event =>
          if (e.predicateObject2.isDefined) List((e.predicateObject.get, e), (e.predicateObject2.get, e))
          else List((e.predicateObject.get, e))
        case msg @ Tuple2(u: UUID, label: String) => List(msg) }
      .filterNot {
        case (uuid, e) =>
          val excluded = List("00000000-0000-0000-0000-000000000000", "071fbdeb-131c-11e7-bfbf-f55a9065b18e", "19f119de-131b-11e7-bfbf-f55a9065b18e").map(UUID.fromString)
          excluded.contains(uuid) // TODO: why are there these special cases?!?!?!?!?
        case _ => false }
      .groupBy(Int.MaxValue, _._1)
      .merge(commandSource)
      .statefulMapConcat { () =>
        var uuidOpt: Option[UUID] = None
        var labelOpt: Option[String] = None
        val events = mutable.SortedSet.empty[Event](Ordering.by(_.sequence))

      {
        case Tuple2(predicateUuid: UUID, e: Event) =>
          if (uuidOpt.isEmpty) uuidOpt = Some(predicateUuid)
          if (labelOpt.isDefined) List((labelOpt.get, predicateUuid, e))
          else {
            events += e
            List.empty
          }
//          List(labelOpt.map(label => (label, predicateUuid, e))).flatten   // TODO: interesting. maybe this _should_ throw away events for Objects we never see.

        case Tuple2(objectUuid: UUID, labelName: String) =>
          if (uuidOpt.isEmpty) uuidOpt = Some(objectUuid)
          if (labelOpt.isEmpty) {
            labelOpt = Some(labelName)
            val existingSet = dbMap.getOrDefault(objectUuid, mutable.SortedSet.empty[Event](Ordering.by(_.sequence)))
            events ++= existingSet
            dbMap.remove(objectUuid)
          }
          val toSend = events.toList.map(event => (labelName, objectUuid, event))
          events.clear()
          toSend

        case CleanUp =>
          if (events.nonEmpty) {
            val existingSet = dbMap.getOrDefault(uuidOpt.get, mutable.SortedSet.empty[Event](Ordering.by(_.sequence)))
            events ++= existingSet
//            println(s"UNMATCHED: ${uuidOpt}  size: ${events.size}    ${events.map(_.eventType)}")  // TODO
            dbMap.put(uuidOpt.get, events)
            events.clear()
          }
          List.empty

        case Emit => List.empty
      }
      }.mergeSubstreams
  }

//  def test(commandSource: Source[ProcessingCommand,_], db: DB) =
//    typeSorter(commandSource, db.hashMap("typeSorter").createOrOpen().asInstanceOf[HTreeMap[UUID, mutable.SortedSet[Event]]])
//    .collect { case (name, uuid, event) if name == "NetFlowObject" => uuid -> event }
//    .via(sortedSetEmitter(commandSource, db.hashMap("sortedSetEmitter").createOrOpen().asInstanceOf[HTreeMap[UUID, mutable.SortedSet[Event]]]))
//      .mergeSubstreams
//    .via(netFlowFeatureExtractor)





  val netflowEventTypes = List(EVENT_ACCEPT, EVENT_CONNECT, EVENT_OPEN, EVENT_CLOSE, EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG, EVENT_SENDTO, EVENT_SENDMSG, EVENT_WRITE)

  def netFlowFeatureExtractor = Flow[(UUID, mutable.SortedSet[Event])]
    .map { case (u, eSet) =>
      val eList = eSet.toList
      val m = MutableMap.empty[String, Any]
//      m("execCountByThisNetFlowsProcess") = "This should probably be on the Process"   // TODO: don't do.
      m("lifetimeWriteRateBytesPerSecond") = eSet.sizePerSecond(EVENT_WRITE)
      m("lifetimeReadRateBytesPerSecond") = eSet.sizePerSecond(EVENT_READ)
      m("duration-SecondsBetweenFirstAndLastEvent") = eSet.timeBetween(None, None) / 1000
      m("countOfDistinctSubjectsWithEventToThisNetFlow") = eSet.map(_.subject).size
//      m("distinctFileReadCountByProcessesWritingToThisNetFlow") = "TODO"                                // TODO: needs pairing with Files (and join on Process UUID)
      m("totalBytesRead") = eList.collect{ case e if e.eventType == EVENT_READ => e.size.getOrElse(0L)}.sum
      m("totalBytesWritten") = eList.collect{ case e if e.eventType == EVENT_WRITE => e.size.getOrElse(0L)}.sum
      netflowEventTypes.foreach( t =>
        m("count_"+ t.toString) = eSet.count(_.eventType == t)
      )
      u -> m
    }

  def netFlowFeatureEventAccumulator(commandSource: Source[ProcessingCommand,_], db: DB) = {
    val netFlowEventsDBMap = db.hashMap("NetFlowEventsByPredicate").createOrOpen().asInstanceOf[HTreeMap[UUID, mutable.SortedSet[Event]]]
    Flow[CDM17]
      .collect { case e: Event if FlowComponents.netflowEventTypes.contains(e.eventType) => e }
      .via(eventsGroupedByKey(commandSource, netFlowEventsDBMap, PredicateObjectKey).mergeSubstreams)
  }

//  def testNetFlowFeatureExtractor(commandSource: Source[ProcessingCommand,_], db: DB) = {
//    netFlowFeatureEventAccumulator(commandSource, db).via(netFlowFeatureExtractor)
//  }
  def testNetFlowFeatureExtractor(commandSource: Source[ProcessingCommand,_], db: DB) = {
    predicateTypeLabeler(commandSource, db)
      .collect{ case ("NetFlowObject", predUuid, event) => (predUuid, event) }
      .via(printCounter("NetFlow counter", 100))
      .via(sortedEventAccumulator(_._1, commandSource, db))
      .via(netFlowFeatureExtractor)
  }






  def fileFeatureGenerator(commandSource: Source[ProcessingCommand,_], db: DB) = {
    val dbMap = db.hashMap("fileFeatureGenerator" + Random.nextInt()).createOrOpen().asInstanceOf[HTreeMap[UUID,mutable.SortedSet[Event]]]

    predicateTypeLabeler(commandSource, db)
      .filter(x => x._1 == "NetFlowObject" || x._1 == "FileObject")
      .via(printCounter("NetFlow or File Counter", 100))
      .groupBy(Int.MaxValue, _._3.subject)
      .merge(commandSource)
      .statefulMapConcat{ () =>
        var processUuidOpt: Option[UUID] = None
        var fileUuids = MutableSet.empty[UUID]
        val fileEvents = MutableMap.empty[UUID, mutable.SortedSet[Event]]
        val netFlowUuids = MutableSet.empty[UUID]
        val netFlowEvents = MutableMap.empty[UUID, mutable.SortedSet[Event]]


        // TODO: Use the right EC!  (if using Futures below)
        import scala.concurrent.ExecutionContext.Implicits.global


        {
          case Tuple3("NetFlowObject", uuid: UUID, event: Event) =>
            if (processUuidOpt.isEmpty) processUuidOpt = Some(event.subject)
            netFlowEvents(uuid) = netFlowEvents.getOrElse(uuid, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) + event
            List.empty

          case Tuple3("FileObject", uuid: UUID, event: Event) =>
            if (processUuidOpt.isEmpty) processUuidOpt = Some(event.subject)
            fileEvents(uuid) = fileEvents.getOrElse(uuid, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) + event
            List.empty

          case CleanUp =>
//            if (netFlowEvents.nonEmpty) {
//              val mergedEvents = MutableMap.empty[UUID,mutable.SortedSet[Event]]
//              netFlowEvents.foreach { case (u, es) =>
//                mergedEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) ++ es
//              }
//              dbMap.putAll(mergedEvents.asJava)
//              netFlowUuids ++= netFlowEvents.keySet
//              netFlowEvents.clear()
//            }
//            if (fileEvents.nonEmpty) {
//              val mergedEvents = MutableMap.empty[UUID,mutable.SortedSet[Event]]
//              fileEvents.foreach { case (u, es) =>
//                mergedEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) ++ es
//              }
//
//              // This gets slower as Event Set gets larger.
//              dbMap.putAll(mergedEvents.asJava)
//
//              fileUuids ++= fileEvents.keySet
//              fileEvents.clear()
//            }
            List.empty

          case Emit =>
            fileUuids.foreach(u =>
              fileEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) ++
                fileEvents.getOrElse(u, mutable.SortedSet.empty[Event](Ordering.by(_.sequence)))
            )
            netFlowUuids.foreach(u =>
              netFlowEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) ++
                netFlowEvents.getOrElse(u, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) )

            fileEvents.toList.map{ case (u, fes) => ((u, fes), netFlowEvents.toSet) }
        }
      }
      .mergeSubstreams
      .via(fileFeatures)
  }




  val fileEventTypes = List(EVENT_CHECK_FILE_ATTRIBUTES, EVENT_CLOSE, EVENT_CREATE_OBJECT, EVENT_DUP, EVENT_EXECUTE, EVENT_FNCTL, EVENT_LINK, EVENT_LSEEK, EVENT_MMAP, EVENT_MODIFY_FILE_ATTRIBUTES, EVENT_OPEN, EVENT_READ, EVENT_RENAME, EVENT_TRUNCATE, EVENT_UNLINK, EVENT_UPDATE, EVENT_WRITE)

  val fileFeatures = Flow[((UUID, mutable.SortedSet[Event]), Set[(UUID, mutable.SortedSet[Event])])]
    .map{ case ((fileUuid, fileEventSet), netFlowEventsFromIntersectingProcesses) =>
      val fileEventList = fileEventSet.toList
      val m = MutableMap.empty[String,Any]
      m("execAfterWriteByNetFlowReadingProcess") = {
        var remainder = fileEventList.dropWhile(_.eventType != EVENT_WRITE)
        var found = false
        while (remainder.nonEmpty) {
          val execOpt = remainder.find(_.eventType == EVENT_WRITE).flatMap(w => remainder.find(x => x.eventType == EVENT_EXECUTE && w.subject == x.subject))
          found = execOpt.exists(x => netFlowEventsFromIntersectingProcesses.exists(p => p._2.exists(e => List(EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG).contains(e.eventType))))
          if ( ! found) remainder = remainder.drop(1).dropWhile(_.eventType != EVENT_WRITE)
        }
        found
      }
      m("execAfterPermissionChangeToExecutable") = fileEventList.dropWhile(_.eventType != EVENT_MODIFY_FILE_ATTRIBUTES).exists(_.eventType == EVENT_EXECUTE)
      m("deletedImmediatelyAfterExec") = fileEventList.dropWhile(_.eventType != EVENT_EXECUTE).drop(1).headOption.exists(_.eventType == EVENT_UNLINK)
      m("deletedRightAfterProcessWithOpenNetFlowsWrites") =
        fileEventList.collect { case writeEvent if writeEvent.eventType == EVENT_WRITE =>
          val deleteAfterWriteOpt = fileEventList.find( deleteEvent =>
            deleteEvent.eventType == EVENT_UNLINK &&
            (deleteEvent.timestampNanos - writeEvent.timestampNanos >= 0) &&  // delete happened AFTER the write
            (deleteEvent.timestampNanos - writeEvent.timestampNanos <= 3e10)  // within 30 seconds. This is the interpretation of "right after"
          )
          deleteAfterWriteOpt.exists { deleteAfterWriteEvent =>        // IFF we found a Delete after WRITE...
            netFlowEventsFromIntersectingProcesses.exists(t => t._2    // t._2 is a process's events, in order.
              .dropWhile(_.eventType != EVENT_OPEN)
              .takeWhile(_.eventType != EVENT_CLOSE)
              .exists( testEvent =>                                    // in the events between OPEN and CLOSE...
                testEvent.subject == deleteAfterWriteEvent.subject &&  // event by the same process as the UNLINK?
                  t._2.find(_.eventType == EVENT_CLOSE).exists( closeEvent =>  // If so, get the CLOSE event and
                  deleteAfterWriteEvent.timestampNanos <= closeEvent.timestampNanos  // test if the UNLINK occurred before the CLOSE
                )
              )
            )
          }
        }.foldLeft(false)(_ || _) // is there a single `true`?

      m("isReadByAProcessWritingToNetFlows") = fileEventList
        .collect{ case e if e.eventType == EVENT_READ => e.subject}
        .flatMap( processUuid =>
          netFlowEventsFromIntersectingProcesses.toList.map(_._2.exists(ne =>
            ne.subject == processUuid &&
            List(EVENT_SENDTO, EVENT_SENDMSG, EVENT_WRITE).contains(ne.eventType)
          ))
        ).foldLeft(false)(_ || _)
      m("isInsideTempDirectory") = fileEventList.flatMap(_.predicateObjectPath).exists(path => List("/tmp", "/temp", "\\temp").exists(tmp => path.toLowerCase.contains(tmp)))  // TODO: revisit the list of temp locations.
      m("execDeleteGapMillis") = fileEventList.timeBetween(Some(EVENT_EXECUTE), Some(EVENT_UNLINK))
      m("attribChangeEventThenExecuteGapMillis") = fileEventList.timeBetween(Some(EVENT_MODIFY_FILE_ATTRIBUTES), Some(EVENT_EXECUTE))
      m("downloadExecutionGapMillis") = "TODO"                       // TODO: needs pairing with NetFlow events (and join on process UUID)
      m("uploadDeletionGapMillis") = "TODO"                          // TODO: needs pairing with NetFlow events (and join on process UUID)
      m("countDistinctProcessesHaveEventToFile") = fileEventSet.map(_.subject).size
      m("countDistinctNetFlowConnectionsByProcess") = "This should probably be on Processes"  // TODO: don't do.
      m("totalBytesRead") = fileEventList.filter(_.eventType == EVENT_READ).flatMap(_.size).sum
      m("totalBytesWritten") = fileEventList.filter(_.eventType == EVENT_WRITE).flatMap(_.size).sum
      fileEventTypes.foreach( t =>
        m("count_"+ t.toString) = fileEventSet.count(_.eventType == t)
      )
      fileUuid -> m
    }


  def testFileFeatureEventAccumulator(commandSource: Source[ProcessingCommand,_], db: DB) = {
    val fileEventsDBMap = db.hashMap("FileEventsByPredicate").createOrOpen().asInstanceOf[HTreeMap[UUID, mutable.SortedSet[Event]]]
    Flow[CDM17]
      .collect{ case e: Event if FlowComponents.fileEventTypes.contains(e.eventType) => e}
      .via(eventsGroupedByKey(commandSource, fileEventsDBMap, PredicateObjectKey).mergeSubstreams)
  }

//  def testFileFeatureExtractor(commandSource: Source[ProcessingCommand,_], db: DB) = {
//    testFileFeatureEventAccumulator(commandSource, db).via(fileFeatures)
//  }


//  def shuffleJoinOnProcessUuid(commandSource: Source[ProcessingCommand,_], db: DB) = Flow[(String, UUID, mutable.SortedSet[Event])]
//    .mapConcat { pair =>
//      val processUUIDs = pair._3.map(_.subject)
//      processUUIDs.toList.map(pUUID => (pUUID, pair._1, pair._2, pair._3))}
//    .groupBy(Int.MaxValue, _._1)
//    .merge(commandSource)
//    .statefulMapConcat { () =>
//      var joinProcessUuid: Option[UUID] = None
//      val fileMap = mutable.Map.empty[UUID, mutable.SortedSet[Event]]
//      val netFlowMap = mutable.Map.empty[UUID, mutable.SortedSet[Event]]
//
//      val fileEventsDBMap = db.hashMap("FileEventsByPredicate").createOrOpen().asInstanceOf[HTreeMap[UUID, mutable.SortedSet[Event]]]
//      val netFlowEventsDBMap = db.hashMap("NetFlowEventsByPredicate").createOrOpen().asInstanceOf[HTreeMap[UUID, mutable.SortedSet[Event]]]
//      val processEventsDBMap = db.hashMap("ProcessEventsForFilesAndNetFlows")
//
//      {
//        case Emit =>
//          fileMap ++= fileEventsDBMap.get(joinProcessUuid.get)
//          val newNetFlowEvents = netFlowEventsDBMap.get(joinProcessUuid.get) ++ netFlowMap
//          fileEventsDBMap.put(joinProcessUuid.get, fileMap)
//          List((joinProcessUuid.get, , ))
//
//        case CleanUp => List.empty
//
//        case Tuple4(processUuid: UUID, "FileEventsByPredicate", eventGroupTypeUuid: UUID, events: mutable.SortedSet[Event]) =>
//          if (joinProcessUuid.isEmpty) joinProcessUuid = Some(processUuid)
//          fileMap(eventGroupTypeUuid) = events
//          List.empty
//
//        case Tuple4(processUuid: UUID, "NetFlowEventsByPredicate", eventGroupTypeUuid: UUID, events: mutable.SortedSet[Event]) =>
//          if (joinProcessUuid.isEmpty) joinProcessUuid = Some(processUuid)
//          netFlowMap(eventGroupTypeUuid) = events
//          List.empty
//      }
//    }



  def processFeatureGenerator(commandSource: Source[ProcessingCommand,_], db: DB) = {
    val dbMap = db.hashMap("fileFeatureGenerator" + Random.nextInt()).createOrOpen().asInstanceOf[HTreeMap[UUID,mutable.SortedSet[Event]]]

    predicateTypeLabeler(commandSource, db)
      .filter(x => List("NetFlowObject", "FileObject", "Subject").contains(x._1))
      .groupBy(Int.MaxValue, _._3.subject)
      .merge(commandSource)
      .statefulMapConcat{ () =>
        var processUuidOpt: Option[UUID] = None
        val processEvents = mutable.SortedSet.empty[Event](Ordering.by(_.sequence))
        val fileUuids = MutableSet.empty[UUID]
        val fileEvents = MutableMap.empty[UUID, mutable.SortedSet[Event]]
        val netFlowUuids = MutableSet.empty[UUID]
        val netFlowEvents = MutableMap.empty[UUID, mutable.SortedSet[Event]]

        {
          case Tuple3("Subject", uuid: UUID, event: Event) =>
            if (processUuidOpt.isEmpty) processUuidOpt = Some(uuid)
            processEvents += event
            List.empty

          case Tuple3("NetFlowObject", uuid: UUID, event: Event) =>
            if (processUuidOpt.isEmpty) processUuidOpt = Some(event.subject)
            netFlowEvents(uuid) = netFlowEvents.getOrElse(uuid, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) + event
            List.empty

          case Tuple3("FileObject", uuid: UUID, event: Event) =>
            if (processUuidOpt.isEmpty) processUuidOpt = Some(event.subject)
            fileEvents(uuid) = fileEvents.getOrElse(uuid, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) + event
            List.empty

          case CleanUp =>
//            if (fileEvents.nonEmpty) {
//              val mergedEvents = MutableMap.empty[UUID,mutable.SortedSet[Event]]
//              fileEvents.foreach { case (u, es) =>
//                mergedEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) ++ es
//              }
//              dbMap.putAll(mergedEvents.asJava)
//              fileUuids ++= fileEvents.keySet
//              fileEvents.clear()
//            }
//            if (netFlowEvents.nonEmpty) {
//              val mergedEvents = MutableMap.empty[UUID,mutable.SortedSet[Event]]
//              netFlowEvents.foreach { case (u, es) =>
//                mergedEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) ++ es
//              }
//              dbMap.putAll(mergedEvents.asJava)
//              netFlowUuids ++= netFlowEvents.keySet
//              netFlowEvents.clear()
//            }
//            if (processEvents.nonEmpty) {
//              val mergedEvents = dbMap.getOrDefault(processUuidOpt.get, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) ++ processEvents
//              dbMap.put(processUuidOpt.get, mergedEvents)
//              processEvents.clear()
//            }
            List.empty

          case Emit =>
            fileUuids.foreach(u =>
              fileEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) ++
                fileEvents.getOrElse(u, mutable.SortedSet.empty[Event](Ordering.by(_.sequence)))
            )
            netFlowUuids.foreach(u =>
              netFlowEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) ++
                netFlowEvents.getOrElse(u, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) )

            processEvents ++= dbMap.getOrDefault(processUuidOpt.get, mutable.SortedSet.empty[Event](Ordering.by(_.sequence)))

            List(((processUuidOpt.get, processEvents), netFlowEvents.toSet, fileEvents.toSet))
  //          fileEvents.toList.map{ case (u, fes) => ((u, fes), netFlowEvents.toSet) }
        }
      }
      .mergeSubstreams
      .via(processFeatureExtractor)
  }



  val processEventTypes = EventType.values.toList

  def processFeatureExtractor = Flow[((UUID, mutable.SortedSet[Event]), Set[(UUID, mutable.SortedSet[Event])], Set[(UUID, mutable.SortedSet[Event])])]
    .map { case ((processUuid, processEventSet), netFlowEventSets, fileEventSets) =>
      val eList = processEventSet.toList
      val m = MutableMap.empty[String, Any]
//      m("countOfImmediateChildProcesses") = "TODO"                                        // TODO: needs process tree
//      m("countOfAllChildProcessesInTree") = "TODO"                                        // TODO: needs process tree
//      m("countOfUniquePortAccesses") = "TODO"                                             // TODO: needs pairing with NetFlows
//      m("parentProcessUUID") = "I THINK WE DON'T NEED THIS"                               // TODO: don't do.
      // TODO: consider emitting the collected Process Tree
      m("countOfDistinctMemoryObjectsMProtected") = processEventSet.collect { case e if e.eventType == EVENT_MPROTECT && e.predicateObject.isDefined => e.predicateObject }.size
//      m("isProcessRunning_cmd.exe_or-powershell.exe_whileParentRunsAnotherExe") = "TODO"  // TODO: needs process tree
      m("countOfAllConnect+AcceptEventsToPorts22or443") = {                                 // TODO: needs pairing with NetFlows
//        netFlowEventSets.head._2.head.
        "TODO"
      }
//      m("countOfAllConnect+AcceptEventsToPortsOtherThan22or443") = "TODO"                 // TODO: needs pairing with NetFlows
//      m("isReferringPasswordFile") = "I HAVE NO IDEA WHAT THIS MEANS"                     // TODO: ¯\_(ツ)_/¯
//      m("readsFromNetFlowThenWritesAFileThenExecutesTheFile") = "TODO"                    // TODO: needs pairing with NetFlows
      m("changesFilePermissionsThenExecutesIt") = eList.dropWhile(_.eventType != EVENT_MODIFY_FILE_ATTRIBUTES).exists(_.eventType == EVENT_EXECUTE)
      m("executedThenImmediatelyDeletedAFile") = eList.groupBy(_.predicateObject).-(None).values.exists(l => l.sortBy(_.sequence).dropWhile(_.eventType != EVENT_EXECUTE).drop(1).headOption.exists(_.eventType == EVENT_UNLINK))
//      m("readFromNetFlowThenDeletedFile") = "TODO"                                        // TODO: needs pairing with NetFlows
      // TODO: consider: process takes any local action after reading from NetFlow
      m("countOfDistinctFileWrites") = processEventSet.collect { case e if e.eventType == EVENT_WRITE && e.predicateObject.isDefined => e.predicateObject }.size
//      m("countOfFileUploads") = "TODO"                                                    // TODO: needs pairing with Files (to ensure reads are from Files)
//      m("countOfFileDownloads") = "TODO"                                                  // TODO: needs pairing with Files (to ensure writes are to Files)
      m("isAccessingTempDirectory") = eList.flatMap(e => List(e.predicateObjectPath, e.predicateObject2Path).flatten).exists(path => List("/tmp", "/temp", "\\temp").exists(tmp => path.toLowerCase.contains(tmp)))  // TODO: revisit the list of temp locations.
//      m("thisProcessIsTheObjectOfA_SETUID_Event") = "TODO"                                // TODO: needs process UUID from predicateObject field (not subject)
      m("totalBytesSentToNetFlows") = eList.collect { case e if e.eventType == EVENT_SENDTO => e.size.getOrElse(0L)}.sum
      m("totalBytesReceivedFromNetFlows") = eList.collect { case e if e.eventType == EVENT_RECVFROM => e.size.getOrElse(0L)}.sum
      processEventTypes.foreach( t =>
        m("count_"+ t.toString) = processEventSet.count(_.eventType == t)
      )
      processUuid -> m
    }

//  def testProcessFeatureExtractor(commandSource: Source[ProcessingCommand,_], db: DB) = {
//    val processEventsDBMap = db.hashMap("ProcessEventsByPredicate").createOrOpen().asInstanceOf[HTreeMap[UUID, mutable.SortedSet[Event]]]
//    Flow[CDM17]
//      .via(eventsGroupedByKey(commandSource, processEventsDBMap, SubjectKey(None)).mergeSubstreams)
//      .via(processFeatureExtractor)
//  }






  def printCounter[T](name: String, every: Int = 10000) = Flow[T].statefulMapConcat { () =>
    var counter = 0L
    var originalStartTime = 0L
    var lastTimestampNanos = 0L

    { case item: T =>  // Type annotation T is a compilation hack! No runtime effect because it's generic.
        if (lastTimestampNanos == 0L) {
          originalStartTime = System.nanoTime()
          lastTimestampNanos = System.nanoTime()
        }
        counter = counter + 1
        if (counter % every == 0) {
          val nowNanos = System.nanoTime()
          val durationSeconds = (nowNanos - lastTimestampNanos) / 1e9
          println(s"$name ingested: $counter   Elapsed for this $every: ${f"$durationSeconds%.3f"} seconds.  Rate for this $every: ${(every / durationSeconds).toInt} items/second.  Rate since beginning: ${(counter / ((nowNanos - originalStartTime) / 1e9)).toInt} items/second")
          lastTimestampNanos = System.nanoTime()
        }
        List[T](item)
    }
  }


  val uuidMapToCSVPrinterSink = Flow[(UUID, mutable.Map[String,Any])]
    .map{ case (u, m) =>
      s"$u,${m.toList.sortBy(_._1).map(_._2).mkString(",")}"
    }.toMat(Sink.foreach(println))(Keep.right)


  def csvFileSink(path: String) = Flow[(UUID, mutable.Map[String,Any])]
    .statefulMapConcat{ () =>
      var needsHeader = true

      { case Tuple2(u: UUID, m: mutable.Map[String,Any]) =>
        val row = List(ByteString(s"$u,${m.toList.sortBy(_._1).map(_._2).mkString(",")}\n"))
        if (needsHeader) {
          needsHeader = false
          List(ByteString(s"uuid,${m.toList.sortBy(_._1).map(_._1).mkString(",")}\n")) ++ row
        } else row
      }
    }.toMat(FileIO.toPath(Paths.get(path)))(Keep.right)


  def anomalyScoreCalculator(commandSource: Source[ProcessingCommand,_]) = Flow[(UUID, mutable.Map[String,Any])]
    .merge(commandSource)
    .statefulMapConcat{ () =>
      var matrix = MutableMap.empty[UUID, String]
      var headerOpt: Option[String] = None

      {
        case Tuple2(uuid: UUID, featureMap: mutable.Map[String,Any]) =>
          if (headerOpt.isEmpty) headerOpt = Some(s"uuid,${featureMap.toList.sortBy(_._1).map(_._1).mkString(",")}\n")
          val row = s"${featureMap.toList.sortBy(_._1).map(_._2).mkString(",")}\n"
          matrix(uuid) = row
          List.empty

        case CleanUp => List.empty

        case Emit =>
          val randomNum = Random.nextLong()
          val inputFile = new File(s"/Users/ryan/Desktop/intermediate_csvs/temp.in_$randomNum.csv")
          val outputFile = new File(s"/Users/ryan/Desktop/intermediate_csvs/temp.out_$randomNum.csv")
//          val inputFile  = File.createTempFile(s"input_$randomNum",".csv")
//          val outputFile = File.createTempFile(s"output_$randomNum",".csv")
          inputFile.deleteOnExit()
          outputFile.deleteOnExit()
          val writer: FileWriter = new FileWriter(inputFile)
          writer.write(headerOpt.get)
          matrix.map(row => s"${row._1},${row._2}").foreach(writer.write)
          writer.close()

          Try( Seq[String](
            this.getClass.getClassLoader.getResource("bin/iforest.exe").getPath, // "../ad/osu_iforest/iforest.exe",
            "-i", inputFile.getCanonicalPath,   // input file
            "-o", outputFile.getCanonicalPath,  // output file
            "-m", "1",                          // ignore the first column
            "-t", "100"                         // number of trees
          ).!!) match {
            case Success(output) => println(s"AD output: $randomNum\n$output")
            case Failure(e)      => e.printStackTrace()
          }

//          val normalizedFile = File.createTempFile(s"normalized_$randomNum", ".csv")
          val normalizedFile = new File(s"/Users/ryan/Desktop/intermediate_csvs/normalized_$randomNum.csv")
          normalizedFile.createNewFile()
//          normalizedFile.deleteOnExit()

          val normalizationCommand = Seq(
            "Rscript",
            this.getClass.getClassLoader.getResource("bin/NormalizeScore.R").getPath,
            "-i", outputFile.getCanonicalPath,       // input file
            "-o", normalizedFile.getCanonicalPath)   // output file

          val normResultTry = Try(normalizationCommand.!!) match {
            case Success(output) => println(s"Normalization output: $randomNum\n$output")
            case Failure(e)      => e.printStackTrace()
          }

          val fileLines = FileSource.fromFile(normalizedFile).getLines()
          if (fileLines.hasNext) fileLines.next()  // Throw away the header row
          fileLines.take(20).toSeq.map{ l =>
            val columns = l.split(",")
            UUID.fromString(columns.head) -> columns.last.toDouble
          }.toList
      }
    }

  def commandSource(cleanUpSeconds: Int, emitSeconds: Int) =
    Source.tick[ProcessingCommand](cleanUpSeconds seconds, cleanUpSeconds seconds, CleanUp).buffer(1, OverflowStrategy.backpressure)
      .merge(Source.tick[ProcessingCommand](emitSeconds seconds, emitSeconds seconds, Emit).buffer(1, OverflowStrategy.backpressure))


  def normalizedScores(db: DB, fastClean: Int = 6, fastEmit: Int = 20, slowClean: Int = 30, slowEmit: Int = 50) = Flow.fromGraph(
    GraphDSL.create(){ implicit graph =>
      val bcast = graph.add(Broadcast[CDM17](3))
      val merge = graph.add(Merge[(String,UUID,Double)](3))

      val fastCommandSource = commandSource(fastClean, fastEmit)   // TODO
      val slowCommandSource = commandSource(slowClean, slowEmit)   // TODO

      bcast.out(0) ~> FlowComponents.testNetFlowFeatureExtractor(fastCommandSource, db).via(FlowComponents.anomalyScoreCalculator(slowCommandSource)).map(t => ("NetFlow", t._1, t._2)) ~> merge
      bcast.out(1) ~> FlowComponents.fileFeatureGenerator(fastCommandSource, db).via(FlowComponents.anomalyScoreCalculator(slowCommandSource)).map(t => ("File", t._1, t._2)) ~> merge
      bcast.out(2) ~> FlowComponents.processFeatureGenerator(fastCommandSource, db).via(FlowComponents.anomalyScoreCalculator(slowCommandSource)).map(t => ("Process", t._1, t._2)) ~> merge
      merge.out

      FlowShape(bcast.in, merge.out)
    }
  )


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

object TitanFlowComponents {

  /* Open a Cassandra-backed Titan graph. If this is failing, make sure you've run something like
   * the following first:
   *
   *   $ rm -rf /usr/local/var/lib/cassandra/data/*            # clear information from previous run */
   *   $ rm -rf /usr/local/var/lib/cassandra/commitlog/*       # clear information from previous run */
   *   $ /usr/local/opt/cassandra@2.1/bin/cassandra -f         # start Cassandra
   *
   * The following also sets up a key index for UUIDs.
   */
  val graph = {
    val graph = TitanFactory.build.set("storage.backend","cassandra").set("storage.hostname","localhost").open

    val management: ManagementSystem = graph.openManagement().asInstanceOf[ManagementSystem]
    management.makeEdgeLabel("tagId").multiplicity(Multiplicity.MULTI).make()
    if (null == management.getGraphIndex("byUuidUnique")) {
    
      var idKey = if (management.getPropertyKey("uuid") != null) {
        management.getPropertyKey("uuid")
      } else {
        management.makePropertyKey("uuid").dataType(classOf[UUID]).make()
      }
      management.buildIndex("byUuidUnique", classOf[Vertex]).addKey(idKey).unique().buildCompositeIndex()
    
      idKey = management.getPropertyKey("uuid")
      val idx = management.getGraphIndex("byUuidUnique")
      if (idx.getIndexStatus(idKey).equals(SchemaStatus.INSTALLED)) {
        ManagementSystem.awaitGraphIndexStatus(graph, "byUuidUnique").status(SchemaStatus.REGISTERED).call()
      }
     
      management.updateIndex(
        management.getGraphIndex("byUuidUnique"),
        SchemaAction.ENABLE_INDEX
      ) 
      management.commit()
      ManagementSystem.awaitGraphIndexStatus(graph, "byUuidUnique").status(SchemaStatus.ENABLED).call()
    } else {
      management.commit()
    }

    graph
  }

  /* Given a 'TitanGraph', make a 'Flow' that writes CDM data into that graph in a buffered manner
   */
  def titanWrites(graph: TitanGraph = graph) = Flow[CDM17]
    .collect{ case cdm: DBNodeable => cdm }
    .groupedWithin(10000, 5 seconds)
    .via(FlowComponents.printCounter("Titan Writer"))
    .toMat(
      Sink.foreach[collection.immutable.Seq[DBNodeable]]{ cdms =>
//      println("opened transaction")
      val transaction = graph.newTransaction()

      for (cdm <- cdms) {
        val props: List[Object] = cdm.asDBKeyValues.asInstanceOf[List[Object]]
        assert(props.length % 2 == 0, s"Node ($cdm) has odd length properties list: $props.")
        val newTitanVertex = transaction.addVertex(props: _*)

        for ((label,toUuid) <- cdm.asDBEdges) {
          val i = graph.traversal().V().has("uuid",cdm.getUuid)
          if (i.hasNext) {
            val toTitanVertex = i.next()
            newTitanVertex.addEdge(label, toTitanVertex)
          }
        }
      }
      transaction.commit()
      println(s"Committed transaction with ${cdms.length}")
    }
  )(Keep.right)

}




object TestGraph extends App {
  implicit val system = ActorSystem("test")
  implicit val ec = system.dispatcher
  implicit val mat = ActorMaterializer()

 

  val path = "/Users/ryan/Desktop/ta1-cadets-cdm17-3.bin" // cdm17_0407_1607.bin" //  ta1-clearscope-cdm17.bin"  //
  val source = Source.fromIterator[CDM17](() => CDM17.readData(path, None).get._2.map(_.get))
//    .concat(Source.fromIterator[CDM17](() => CDM17.readData(path + ".1", None).get._2.map(_.get)))
//    .concat(Source.fromIterator[CDM17](() => CDM17.readData(path + ".2", None).get._2.map(_.get)))
    .via(FlowComponents.printCounter("CDM Source", 1e6.toInt))
//    .via(Streams.titanWrites(graph))


//  // TODO: this should be a single source (instead of multiple copies) that broadcasts into all the necessary places.
//  val fastCommandSource = Source.tick[ProcessingCommand](6 seconds, 6 seconds, CleanUp).buffer(1, OverflowStrategy.backpressure)
//    .merge(Source.tick[ProcessingCommand](20 seconds, 20 seconds, Emit).buffer(1, OverflowStrategy.backpressure))
////    .via(FlowComponents.printCounter("Command Source", 1))
//
//  val slowCommandSource = Source.tick[ProcessingCommand](30 seconds, 30 seconds, CleanUp).buffer(1, OverflowStrategy.backpressure)
//    .merge(Source.tick[ProcessingCommand](50 seconds, 50 seconds, Emit).buffer(1, OverflowStrategy.backpressure))


  val dbFilePath = "/Users/ryan/Desktop/map.db"
  val db = DBMaker.fileDB(dbFilePath).fileMmapEnable().make()
  new File(dbFilePath).deleteOnExit()  // Only meant as ephemeral on-disk storage.


//  TitanUtils.titanWrites(TitanUtils.graph)
//    .runWith(source.via(FlowComponents.printCounter("titan write count", 1)), Sink.ignore)

//  FlowComponents.testNetFlowFeatureExtractor(commandSource, db)
//    .runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/netFlowFeatures.csv"))

//  FlowComponents.fileFeatureGenerator(commandSource, db)
//    .runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/fileFeatures.csv"))



//  Flow[CDM17].collect{ case e: Event => e }.groupBy(Int.MaxValue, _.toString).mergeSubstreams.via(FlowComponents.printCounter[Event]("Event counter", 100)).recover{ case e: Throwable => e.printStackTrace()}.runWith(source, Sink.ignore)




//  Flow[CDM17].runWith(source, TitanUtils.titanWrites(TitanUtils.graph))


  FlowComponents.normalizedScores(db).runWith(source, Sink.foreach(println))


//      .recover{ case e: Throwable => e.printStackTrace() } ~> printSink

//    FlowComponents.testNetFlowFeatureExtractor(fastCommandSource, db)
//      .via(FlowComponents.anomalyScoreCalculator(slowCommandSource))
//      .runWith(source, printSink)

//  FlowComponents.testNetFlowFeatureExtractor(fastCommandSource, db)
//    .via(FlowComponents.anomalyScoreCalculator(slowCommandSource))
//    .recover{ case e: Throwable => e.printStackTrace() }
//    .runWith(source, printSink)

//  FlowComponents.fileFeatureGenerator(fastCommandSource, db)
//    .via(FlowComponents.anomalyScoreCalculator(slowCommandSource))
//    .recover{ case e: Throwable => e.printStackTrace() }
//    .runWith(source, printSink)


//  FlowComponents.processFeatureGenerator(fastCommandSource, db)
//    .via(FlowComponents.anomalyScoreCalculator(slowCommandSource))
//    .recover{ case e: Throwable => e.printStackTrace() }
//    .runWith(source.take(3000000), printSink)

//    .runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/processFeatures.csv"))

//  FlowComponents.testFileFeatureExtractor(commandSource, db)
//    .runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/fileFeatures.csv"))
//




//  FlowComponents.testFileFeatureExtractor(commandSource, db)
//    .runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/fileFeatures.csv"))

//  FlowComponents.testNetFlowFeatureExtractor(commandSource, db)
//    .runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/netflowFeatures.csv"))

//  FlowComponents.testProcessFeatureExtractor(commandSource, db)
//    .runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/processFeatures.csv"))

//  FlowComponents.test(commandSource, db).runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/netflowFeatures9000.csv"))


  //  commandSource.runWith(sink)
}




























sealed trait JoinMultiplicity
case object One extends JoinMultiplicity
case object Many extends JoinMultiplicity

case class Join[A,B,K](
  in0Key: A => K,
  in1Key: B => K,
  in0Multiplicity: JoinMultiplicity = Many, // will there be two elements streamed for which 'in0Key' produces the same value
  in1Multiplicity: JoinMultiplicity = Many  // will there be two elements streamed for which 'in1Key' produces the same value
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
