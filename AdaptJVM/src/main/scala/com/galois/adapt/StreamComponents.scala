package com.galois.adapt

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.file.Paths
import java.util.UUID
import java.io._

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.galois.adapt.cdm17._
import GraphDSL.Implicits._
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.Producer
import akka.kafka.scaladsl.Consumer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.apache.avro.specific.{SpecificDatumReader, SpecificDatumWriter}

import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
import GraphDSL.Implicits._
import akka.util.ByteString
import ch.qos.logback.classic.LoggerContext
import org.mapdb.{DB, DBMaker, HTreeMap}

import collection.JavaConverters._
import scala.collection.mutable
import scala.sys.process._
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success, Try}
import com.thinkaurelius.titan.core._
import com.thinkaurelius.titan.graphdb.database.management.ManagementSystem
import com.thinkaurelius.titan.core.schema.{SchemaAction, SchemaStatus}
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.slf4j.LoggerFactory

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
case object EmitCmd extends ProcessingCommand
case object CleanUp extends ProcessingCommand

object Streams {

  def kafkaProducer(file: String, producerSettings: ProducerSettings[Array[Byte], Array[Byte]], topic: String) = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit graph =>
      val datums: Iterator[com.bbn.tc.schema.avro.cdm17.TCCDMDatum] = CDM17.readAvroAsTCCDMDatum(file)
      Source.fromIterator(() => datums).map(elem => {
        val baos = new ByteArrayOutputStream
        val writer = new SpecificDatumWriter(classOf[com.bbn.tc.schema.avro.cdm17.TCCDMDatum])
        val encoder = EncoderFactory.get.binaryEncoder(baos, null)
        writer.write(elem, encoder)
        encoder.flush()
        baos.toByteArray
      }).map(elem => new ProducerRecord[Array[Byte], Array[Byte]](topic, elem)) ~> Producer.plainSink(producerSettings)

      ClosedShape
    }
  )

  def kafkaIngest(consumerSettings: ConsumerSettings[Array[Byte], Array[Byte]], topic: String) = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit graph =>
      Consumer.committableSource(consumerSettings, Subscriptions.topics(topic)).map{ msg =>
        val bais = new ByteArrayInputStream(msg.record.value())
        val reader = new SpecificDatumReader(classOf[com.bbn.tc.schema.avro.cdm17.TCCDMDatum])
        val decoder = DecoderFactory.get.binaryDecoder(bais, null)
        val elem: com.bbn.tc.schema.avro.cdm17.TCCDMDatum = reader.read(null, decoder)
        val cdm = new RawCDM15Type(elem.getDatum)
        msg.committableOffset.commitScaladsl()
        cdm
      }.map(CDM17.parse).map(println) ~> Sink.ignore
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
        .collect { case e: Event if e.eventType == t => e.subjectUuid -> e }
      case SubjectKey(None) => Flow[CDM17]
        .collect { case e: Event => e.subjectUuid -> e }
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
          case EmitCmd =>
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


  def sortedEventAccumulator[K](groupBy: ((UUID,Event,CDM17)) => K, commandSource: Source[ProcessingCommand,_], db: DB) = {
    val dbMap = db.hashMap("sortedEventAccumulator" + Random.nextInt()).createOrOpen().asInstanceOf[HTreeMap[UUID, mutable.SortedSet[Event]]]
    Flow[(UUID,Event,CDM17)]
      .groupBy(Int.MaxValue, groupBy) // TODO: Limited to ~4 billion unique UUIDs!!!
      .merge(commandSource)
      .statefulMapConcat { () =>
        var uuid: Option[UUID] = None
        val events = mutable.SortedSet.empty[Event](Ordering.by[Event, Long](_.sequence))

        {
          case EmitCmd =>
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

          case Tuple3(u: UUID, e: Event, _: CDM17) =>
            if (uuid.isEmpty) uuid = Some(u)
            events += e
            List.empty
        }
      }.mergeSubstreams
  }



  def predicateTypeLabeler(commandSource: Source[ProcessingCommand,_], db: DB) = {
    val dbMap = db.hashMap("typeSorter_" + Random.nextLong()).createOrOpen().asInstanceOf[HTreeMap[UUID,mutable.SortedSet[Event]]]
    Flow[CDM17]
      .mapConcat[(UUID, String, CDM17)] {
        case e: Event if e.predicateObject.isDefined =>
          if (e.predicateObject2.isDefined) List((e.predicateObject.get, "Event", e), (e.predicateObject2.get, "Event", e))
          else List((e.predicateObject.get, "Event", e))
        case n: NetFlowObject => List((n.uuid, "NetFlowObject", n))
        case f: FileObject => List((f.uuid, "FileObject", f))
        case s: Subject => List((s.uuid, "Subject", s))
        case _ => List.empty }
//        case msg @ => List(msg) }
      .filterNot {
        case (uuid, e, _) =>
          val excluded = List("00000000-0000-0000-0000-000000000000", "071fbdeb-131c-11e7-bfbf-f55a9065b18e", "19f119de-131b-11e7-bfbf-f55a9065b18e").map(UUID.fromString)
          excluded.contains(uuid) // TODO: why are there these special cases?!?!?!?!?
        case _ => false }
      .groupBy(Int.MaxValue, _._1)
      .merge(commandSource)
      .statefulMapConcat[(String, UUID, Event, CDM17)] { () =>
        var uuidOpt: Option[UUID] = None
        var idOpt: Option[(UUID,String,CDM17)] = None
        val events = mutable.SortedSet.empty[Event](Ordering.by(_.sequence))

        {
          case Tuple3(predicateUuid: UUID, "Event", e: Event) =>
            if (uuidOpt.isEmpty) uuidOpt = Some(predicateUuid)
            if (idOpt.isDefined) List((idOpt.get._2, predicateUuid, e, idOpt.get._3))
            else {
              events += e
              List.empty
            }
  //          List(labelOpt.map(label => (label, predicateUuid, e))).flatten   // TODO: interesting. maybe this _should_ throw away events for Objects we never see.

          case Tuple3(objectUuid: UUID, labelName: String, cdm: CDM17) =>
            if (uuidOpt.isEmpty) uuidOpt = Some(objectUuid)
            if (idOpt.isEmpty) {
              idOpt = Some((objectUuid, labelName, cdm))
              val existingSet = dbMap.getOrDefault(objectUuid, mutable.SortedSet.empty[Event](Ordering.by(_.sequence)))
              events ++= existingSet
              dbMap.remove(objectUuid)
            }
            val toSend = events.toList.map(event => (labelName, objectUuid, event, cdm))
            events.clear()
            toSend

          case CleanUp =>
//            if (events.nonEmpty) {
//              val existingSet = dbMap.getOrDefault(uuidOpt.get, mutable.SortedSet.empty[Event](Ordering.by(_.sequence)))
//              events ++= existingSet
//  //            println(s"UNMATCHED: ${uuidOpt}  size: ${events.size}    ${events.map(_.eventType)}")  // TODO
//              dbMap.put(uuidOpt.get, events)
//              events.clear()
//            }
            List.empty

          case EmitCmd => List.empty
        }
      }.mergeSubstreams
  }



  val netflowEventTypes = List(EVENT_ACCEPT, EVENT_CONNECT, EVENT_OPEN, EVENT_CLOSE, EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG, EVENT_SENDTO, EVENT_SENDMSG, EVENT_WRITE)

  def netFlowFeatureExtractor = Flow[(UUID, mutable.SortedSet[Event])]
    .map { case (u, eSet) =>
      val eList = eSet.toList
      val m = MutableMap.empty[String, Any]
//      m("execCountByThisNetFlowsProcess") = "This should probably be on the Process"   // TODO: don't do.
      m("lifetimeWriteRateBytesPerSecond") = eSet.sizePerSecond(EVENT_WRITE)
      m("lifetimeReadRateBytesPerSecond") = eSet.sizePerSecond(EVENT_READ)
      m("duration-SecondsBetweenFirstAndLastEvent") = eSet.timeBetween(None, None) / 1000
      m("countOfDistinctSubjectsWithEventToThisNetFlow") = eSet.map(_.subjectUuid).size
//      m("distinctFileReadCountByProcessesWritingToThisNetFlow") = "TODO"                                // TODO: needs pairing with Files (and join on Process UUID)
      m("totalBytesRead") = eList.collect{ case e if e.eventType == EVENT_READ => e.size.getOrElse(0L)}.sum
      m("totalBytesWritten") = eList.collect{ case e if e.eventType == EVENT_WRITE => e.size.getOrElse(0L)}.sum
      netflowEventTypes.foreach( t =>
        m("count_"+ t.toString) = eSet.count(_.eventType == t)
      )
      u -> m
    }

//  def netFlowFeatureEventAccumulator(commandSource: Source[ProcessingCommand,_], db: DB) = {
//    val netFlowEventsDBMap = db.hashMap("NetFlowEventsByPredicate").createOrOpen().asInstanceOf[HTreeMap[UUID, mutable.SortedSet[Event]]]
//    Flow[CDM17]
//      .collect { case e: Event if FlowComponents.netflowEventTypes.contains(e.eventType) => e }
//      .via(eventsGroupedByKey(commandSource, netFlowEventsDBMap, PredicateObjectKey).mergeSubstreams)
//  }

  def testNetFlowFeatureExtractor(commandSource: Source[ProcessingCommand,_], db: DB) = {
    predicateTypeLabeler(commandSource, db)
      .collect{ case Tuple4("NetFlowObject", predUuid, event, netFlowOpt: CDM17) => (predUuid, event, netFlowOpt) }
//      .via(printCounter("NetFlow counter", 100))
      .via(sortedEventAccumulator(_._1, commandSource, db))
      .via(netFlowFeatureExtractor)
  }






  def fileFeatureGenerator(commandSource: Source[ProcessingCommand,_], db: DB) = {
    val dbMap = db.hashMap("fileFeatureGenerator" + Random.nextInt()).createOrOpen().asInstanceOf[HTreeMap[UUID,mutable.SortedSet[Event]]]

    predicateTypeLabeler(commandSource, db)
      .filter(x => x._1 == "NetFlowObject" || x._1 == "FileObject")
//      .via(printCounter("NetFlow or File Counter", 100))
      .groupBy(Int.MaxValue, _._3.subjectUuid)
      .merge(commandSource)
      .statefulMapConcat[((UUID, mutable.SortedSet[Event]), Set[(UUID, mutable.SortedSet[Event])])]{ () =>
        var processUuidOpt: Option[UUID] = None
        var fileUuids = MutableSet.empty[UUID]
        val fileEvents = MutableMap.empty[UUID, mutable.SortedSet[Event]]
        val netFlowUuids = MutableSet.empty[UUID]
        val netFlowEvents = MutableMap.empty[UUID, mutable.SortedSet[Event]]


        // TODO: Use the right EC!  (if using Futures below)
        import scala.concurrent.ExecutionContext.Implicits.global


        {
          case Tuple4("NetFlowObject", uuid: UUID, event: Event, _: CDM17) =>
            if (processUuidOpt.isEmpty) processUuidOpt = Some(event.subjectUuid)
            netFlowEvents(uuid) = netFlowEvents.getOrElse(uuid, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) + event
            List.empty

          case Tuple4("FileObject", uuid: UUID, event: Event, _: CDM17) =>
            if (processUuidOpt.isEmpty) processUuidOpt = Some(event.subjectUuid)
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

          case EmitCmd =>
            fileUuids.foreach(u =>
              fileEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) ++
                fileEvents.getOrElse(u, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) )

            netFlowUuids.foreach(u =>
              netFlowEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) ++
                netFlowEvents.getOrElse(u, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) )

            fileEvents.toList.map { case (u, fes) => ((u, fes), netFlowEvents.toSet) }
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
        while (remainder.nonEmpty && remainder.exists(_.eventType == EVENT_EXECUTE)) {
          val execOpt = remainder.find(_.eventType == EVENT_WRITE).flatMap(w => remainder.find(x => x.eventType == EVENT_EXECUTE && w.subjectUuid == x.subjectUuid))
          found = execOpt.exists(x => netFlowEventsFromIntersectingProcesses.exists(p => p._2.exists(e => List(EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG).contains(e.eventType))))
          if ( ! found) remainder = remainder.drop(1).dropWhile(_.eventType != EVENT_WRITE)
        }
        found
      }
      m("execAfterPermissionChangeToExecutable") = fileEventList.dropWhile(_.eventType != EVENT_MODIFY_FILE_ATTRIBUTES).exists(_.eventType == EVENT_EXECUTE)
      m("deletedImmediatelyAfterExec") = fileEventList.dropWhile(_.eventType != EVENT_EXECUTE).drop(1).headOption.exists(_.eventType == EVENT_UNLINK)
      m("deletedRightAfterProcessWithOpenNetFlowsWrites") =
        if (fileEventList.exists(_.eventType == EVENT_UNLINK)) {
          fileEventList.collect { case writeEvent if writeEvent.eventType == EVENT_WRITE =>
            val deleteAfterWriteOpt = fileEventList.find(deleteEvent =>
              deleteEvent.eventType == EVENT_UNLINK &&
                (deleteEvent.timestampNanos - writeEvent.timestampNanos >= 0) && // delete happened AFTER the write
                (deleteEvent.timestampNanos - writeEvent.timestampNanos <= 3e10) // within 30 seconds. This is the interpretation of "right after"
            )
            deleteAfterWriteOpt.exists { deleteAfterWriteEvent => // IFF we found a Delete after WRITE...
              netFlowEventsFromIntersectingProcesses.exists(t => t._2 // t._2 is a process's events, in order.
                .dropWhile(_.eventType != EVENT_OPEN)
                .takeWhile(_.eventType != EVENT_CLOSE)
                .exists(testEvent => // in the events between OPEN and CLOSE...
                  testEvent.subjectUuid == deleteAfterWriteEvent.subjectUuid && // event by the same process as the UNLINK?
                    t._2.find(_.eventType == EVENT_CLOSE).exists(closeEvent => // If so, get the CLOSE event and
                      deleteAfterWriteEvent.timestampNanos <= closeEvent.timestampNanos // test if the UNLINK occurred before the CLOSE
                    )
                )
              )
            }
          }.foldLeft(false)(_ || _) // is there a single `true`?
        } else false

      m("isReadByAProcessWritingToNetFlows") = fileEventList
        .collect{ case e if e.eventType == EVENT_READ => e.subjectUuid}
        .flatMap( processUuid =>
          netFlowEventsFromIntersectingProcesses.toList.map(_._2.exists(ne =>
            ne.subjectUuid == processUuid &&
            List(EVENT_SENDTO, EVENT_SENDMSG, EVENT_WRITE).contains(ne.eventType)
          ))
        ).foldLeft(false)(_ || _)
      m("isInsideTempDirectory") = fileEventList.flatMap(_.predicateObjectPath).exists(path => List("/tmp", "/temp", "\\temp").exists(tmp => path.toLowerCase.contains(tmp)))  // TODO: revisit the list of temp locations.
      m("execDeleteGapMillis") = fileEventList.timeBetween(Some(EVENT_EXECUTE), Some(EVENT_UNLINK))
      m("attribChangeEventThenExecuteGapMillis") = fileEventList.timeBetween(Some(EVENT_MODIFY_FILE_ATTRIBUTES), Some(EVENT_EXECUTE))
      m("downloadExecutionGapMillis") = "TODO"                       // TODO: needs pairing with NetFlow events (and join on process UUID)
      m("uploadDeletionGapMillis") = "TODO"                          // TODO: needs pairing with NetFlow events (and join on process UUID)
      m("countDistinctProcessesHaveEventToFile") = fileEventSet.map(_.subjectUuid).size
      m("countDistinctNetFlowConnectionsByProcess") = "This should probably be on Processes"  // TODO: don't do.
      m("totalBytesRead") = fileEventList.filter(_.eventType == EVENT_READ).flatMap(_.size).sum
      m("totalBytesWritten") = fileEventList.filter(_.eventType == EVENT_WRITE).flatMap(_.size).sum
      fileEventTypes.foreach( t =>
        m("count_"+ t.toString) = fileEventSet.count(_.eventType == t)
      )
      fileUuid -> m
    }


  def testFileFeatureEventAccumulator(commandSource: Source[ProcessingCommand,_], db: DB) = {
    val fileEventsDBMap = db.hashMap("FileEventsByPredicate_" + Random.nextLong()).createOrOpen().asInstanceOf[HTreeMap[UUID, mutable.SortedSet[Event]]]
    Flow[CDM17]
      .collect{ case e: Event if FlowComponents.fileEventTypes.contains(e.eventType) => e}
      .via(eventsGroupedByKey(commandSource, fileEventsDBMap, PredicateObjectKey).mergeSubstreams)
  }


  def processFeatureGenerator(commandSource: Source[ProcessingCommand,_], db: DB) = {
    val dbMap = db.hashMap("fileFeatureGenerator_" + Random.nextLong()).createOrOpen().asInstanceOf[HTreeMap[UUID,mutable.SortedSet[Event]]]

    predicateTypeLabeler(commandSource, db)
//      .filter(x => List("NetFlowObject", "FileObject", "Subject").contains(x._1))
      .groupBy(Int.MaxValue, _._3.subjectUuid)
      .merge(commandSource)
      .statefulMapConcat[((UUID, mutable.SortedSet[Event]), Set[(UUID, mutable.SortedSet[(Event,NetFlowObject)])], Set[(UUID, mutable.SortedSet[Event])])] { () =>
        var processUuidOpt: Option[UUID] = None
        val eventsToThisProcess = mutable.SortedSet.empty[Event](Ordering.by(_.sequence))
//        val allEventsByThisProcess = mutable.SortedSet.empty[Event](Ordering.by(_.sequence))
        val fileUuids = MutableSet.empty[UUID]
        val fileEvents = MutableMap.empty[UUID, mutable.SortedSet[Event]]
        val netFlowUuids = MutableSet.empty[UUID]
        val netFlowEvents = MutableMap.empty[UUID, mutable.SortedSet[(Event,NetFlowObject)]]

        {
          case Tuple4("Subject", uuid: UUID, event: Event, _: CDM17) =>
            if (processUuidOpt.isEmpty) processUuidOpt = Some(uuid)
//            if (event.subject == processUuidOpt.get) allEventsByThisProcess += event
            eventsToThisProcess += event
            List.empty

          case Tuple4("NetFlowObject", uuid: UUID, event: Event, cdmOpt: CDM17) =>
            if (processUuidOpt.isEmpty) processUuidOpt = Some(event.subjectUuid)
            netFlowEvents(uuid) = netFlowEvents.getOrElse(uuid, mutable.SortedSet.empty[(Event,NetFlowObject)](Ordering.by(_._1.sequence))) +
              (event -> cdmOpt.asInstanceOf[NetFlowObject])
            List.empty

          case Tuple4("FileObject", uuid: UUID, event: Event, _: CDM17) =>
            if (processUuidOpt.isEmpty) processUuidOpt = Some(event.subjectUuid)
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
//            if (eventsToThisProcess.nonEmpty) {
//              val mergedEvents = dbMap.getOrDefault(processUuidOpt.get, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) ++ eventsToThisProcess
//              dbMap.put(processUuidOpt.get, mergedEvents)
//              eventsToThisProcess.clear()
//            }
//            and similar for all events BY this process
            List.empty

          case EmitCmd =>
            fileUuids.foreach(u =>
              fileEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) ++
                fileEvents.getOrElse(u, mutable.SortedSet.empty[Event](Ordering.by(_.sequence))) )

            netFlowUuids.foreach(u =>
              netFlowEvents(u) = //dbMap.getOrDefault(u, mutable.SortedSet.empty[(Event,NetFlowObject)](Ordering.by(_._1.sequence))) ++
                netFlowEvents.getOrElse(u, mutable.SortedSet.empty[(Event,NetFlowObject)](Ordering.by(_._1.sequence))) )

            eventsToThisProcess ++= dbMap.getOrDefault(processUuidOpt.get, mutable.SortedSet.empty[Event](Ordering.by(_.sequence)))

            List(((processUuidOpt.get, eventsToThisProcess), netFlowEvents.toSet, fileEvents.toSet))
        }
      }
      .mergeSubstreams
      .via(processFeatureExtractor)
  }



  val processEventTypes = EventType.values.toList

  val processFeatureExtractor = Flow[((UUID, mutable.SortedSet[Event]), Set[(UUID, mutable.SortedSet[(Event,NetFlowObject)])], Set[(UUID, mutable.SortedSet[Event])])]
    .map { case ((processUuid, eventsDoneToThisProcessSet), netFlowEventSets, fileEventSets) =>
      val eventsDoneToThisProcessList = eventsDoneToThisProcessSet.toList
      val processEventSet: mutable.SortedSet[Event] = eventsDoneToThisProcessSet ++ netFlowEventSets.flatMap(_._2.map(_._1)) ++ fileEventSets.flatMap(_._2)
      val processEventList: List[Event] = processEventSet.toList
      val m = MutableMap.empty[String, Any]
//      m("countOfImmediateChildProcesses") = "TODO"                                        // TODO: needs process tree
//      m("countOfAllChildProcessesInTree") = "TODO"                                        // TODO: needs process tree
//      m("countOfUniquePortAccesses") = "TODO"                                             // TODO: needs pairing with NetFlows —— not just events!
//      m("parentProcessUUID") = "I THINK WE DON'T NEED THIS"                               // TODO: don't do.
      // TODO: consider emitting the collected Process Tree
      m("countOfDistinctMemoryObjectsMProtected") = processEventSet.collect { case e if e.eventType == EVENT_MPROTECT && e.predicateObject.isDefined => e.predicateObject }.size
//      m("isProcessRunning_cmd.exe_or-powershell.exe_whileParentRunsAnotherExe") = "TODO"  // TODO: needs process tree
      m("countOfAllConnect+AcceptEventsToPorts22or443") =
        netFlowEventSets.toList.map(s => s._2.toList.collect{
          case (e,n) if List(EVENT_CONNECT, EVENT_ACCEPT).contains(e.eventType) &&
            (List(22,443).contains(n.localPort) || List(22.443).contains(n.remotePort)) => 1
        }.sum).sum
      m("countOfAllConnect+AcceptEventsToPortsOtherThan22or443") =
        netFlowEventSets.toList.map(s => s._2.toList.collect{
          case (e,n) if List(EVENT_CONNECT, EVENT_ACCEPT).contains(e.eventType) &&
            ( ! List(22,443).contains(n.localPort) || ! List(22.443).contains(n.remotePort)) => 1
        }.sum).sum

//      m("touchesAPasswordFile") = "TODO"                                                  // TODO: needs pairing with Files. Or does it? Path is probably on events.
      m("readsFromNetFlowThenWritesAFileThenExecutesTheFile") = netFlowEventSets.map(i => i._1 -> i._2.map(_._1)).flatMap(
          _._2.collect{ case e if e.subjectUuid == processUuid && List(EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG).contains(e.eventType) => e.timestampNanos }   // TODO: revisit these event types
        ).toList.sorted.headOption.exists(netFlowReadTime =>
          fileEventSets.exists(
            _._2.dropWhile(write =>
              write.eventType != EVENT_WRITE && write.timestampNanos > netFlowReadTime
            ).exists(ex => ex.eventType == EVENT_EXECUTE)
          )
        )

      m("changesFilePermissionsThenExecutesIt") = processEventList.dropWhile(_.eventType != EVENT_MODIFY_FILE_ATTRIBUTES).exists(_.eventType == EVENT_EXECUTE)
      m("executedThenImmediatelyDeletedAFile") = processEventList.groupBy(_.predicateObject).-(None).values.exists(l => l.sortBy(_.sequence).dropWhile(_.eventType != EVENT_EXECUTE).drop(1).headOption.exists(_.eventType == EVENT_UNLINK))
      m("readFromNetFlowThenDeletedFile") = netFlowEventSets.map(i => i._1 -> i._2.map(_._1)).flatMap(
        _._2.collect{ case e if e.subjectUuid == processUuid && List(EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG).contains(e.eventType) => e.timestampNanos }   // TODO: revisit these event types
      ).toList.sorted.headOption.exists(netFlowReadTime =>
        fileEventSets.exists(
          _._2.exists(delete =>
            delete.eventType != EVENT_UNLINK && delete.timestampNanos > netFlowReadTime
          )
        )
      )

      // TODO: consider: process takes any local action after reading from NetFlow

      m("countOfDistinctFileWrites") = processEventSet.collect { case e if e.eventType == EVENT_WRITE && e.predicateObject.isDefined => e.predicateObject }.size
//      m("countOfFileUploads") = "TODO"                                                    // TODO: needs pairing with Files (to ensure reads are from Files)
//      m("countOfFileDownloads") = "TODO"                                                  // TODO: needs pairing with Files (to ensure writes are to Files)
      m("isAccessingTempDirectory") = processEventList.flatMap(e => List(e.predicateObjectPath, e.predicateObject2Path).flatten).exists(path => List("/tmp", "/temp", "\\temp").exists(tmp => path.toLowerCase.contains(tmp)))  // TODO: revisit the list of temp locations.
      m("thisProcessIsTheObjectOfACHANGE_PRINCIPALEvent") = eventsDoneToThisProcessList.exists(e => e.eventType == EVENT_CHANGE_PRINCIPAL)
      m("thisProcessIsTheObjectOfAMODIFY_PROCESSEvent") = eventsDoneToThisProcessList.exists(e => e.eventType == EVENT_MODIFY_PROCESS)
      m("totalBytesSentToNetFlows") = processEventList.collect { case e if e.eventType == EVENT_SENDTO => e.size.getOrElse(0L)}.sum
      m("totalBytesReceivedFromNetFlows") = processEventList.collect { case e if e.eventType == EVENT_RECVFROM => e.size.getOrElse(0L)}.sum
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

    { item: T =>  // Type annotation T is a compilation hack! No runtime effect because it's generic.
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
        List(item)
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
    .statefulMapConcat[(UUID, Double)] { () =>
      var matrix = MutableMap.empty[UUID, String]
      var headerOpt: Option[String] = None

      {
        case Tuple2(uuid: UUID, featureMap: mutable.Map[String,Any]) =>
          if (headerOpt.isEmpty) headerOpt = Some(s"uuid,${featureMap.toList.sortBy(_._1).map(_._1).mkString(",")}\n")
          val row = s"${featureMap.toList.sortBy(_._1).map(_._2).mkString(",")}\n"
          matrix(uuid) = row
          List.empty

        case CleanUp => List.empty

        case EmitCmd =>
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
      .merge(Source.tick[ProcessingCommand](emitSeconds seconds, emitSeconds seconds, EmitCmd).buffer(1, OverflowStrategy.backpressure))


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


  def kafkaSource(consumerSettings: ConsumerSettings[Array[Byte], Array[Byte]], topic: String) = Source.fromGraph(
    GraphDSL.create() { implicit graph =>
      val kafkaSource = Consumer.committableSource(consumerSettings, Subscriptions.topics(topic)).map{ msg =>
        val bais = new ByteArrayInputStream(msg.record.value())
        val reader = new SpecificDatumReader(classOf[com.bbn.tc.schema.avro.cdm17.TCCDMDatum])
        val decoder = DecoderFactory.get.binaryDecoder(bais, null)
        val elem: com.bbn.tc.schema.avro.cdm17.TCCDMDatum = reader.read(null, decoder)
        val cdm = new RawCDM15Type(elem.getDatum)
        msg.committableOffset.commitScaladsl()
        cdm
      }.map(CDM17.parse)
      SourceShape(kafkaSource.shape.out)
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

    // This allows multiple edges when they are labelled 'tagId'
    if(!management.containsEdgeLabel("tagId")) { management.makeEdgeLabel("tagId").multiplicity(Multiplicity.MULTI).make() }

    val edgeLabels = List("localPrincipal", "subject", "predicateObject", "predicateObject2",
    "parameterTagId", "flowObject", "prevTagId", "parentSubject", "dependentUnit", "unit",
    "tag")
    for (edgeLabel <- edgeLabels)
      if(!management.containsEdgeLabel(edgeLabel)) { management.makeEdgeLabel(edgeLabel).make() }

    val propertyKeys = List(
      ("cid", classOf[Integer]),
      ("cmdLine", classOf[String]),
      ("count", classOf[Integer]),
//      ("ctag", classOf[ConfidentialityTag]),
      ("ctag", classOf[String]),
//      ("components", classOf[Seq[Value]]),
      ("compoents", classOf[String]),
      ("dependentUnitUuid", classOf[UUID]),
      ("epoch", classOf[Integer]),
//      ("exportedLibraries", classOf[Seq[String]]),
      ("exportedLibraries", classOf[String]),
//      ("eventType", classOf[EventType]),
      ("eventType", classOf[String]),
      ("fileDescriptor", classOf[Integer]),
//      ("fileObjectType", classOf[FileObjectType]),
      ("fileObjectType", classOf[String]),
      ("flowObjectUuid", classOf[UUID]),
//      ("groupIds", classOf[Seq[String]]),
      ("groupIds", classOf[String]),
      ("hash", classOf[String]),
//      ("hashes", classOf[Seq[CryptographicHash]]),
      ("hashes", classOf[String]),
//      ("importedLibraries", classOf[Seq[String]]),
      ("importedLibraries", classOf[String]),
      ("ipProtocol", classOf[Integer]),
      ("isNull", classOf[java.lang.Boolean]),
//      ("itag", classOf[IntegrityTag]),
      ("itag", classOf[String]),
      ("iteration", classOf[Integer]),
      ("registryKeyOrPath", classOf[String]),
      ("localAddress", classOf[String]),
      ("localPort", classOf[Integer]),
      ("localPrincipalUuid", classOf[UUID]),
      ("location", classOf[java.lang.Long]),
      ("memoryAddress", classOf[java.lang.Long]),
      ("name", classOf[String]),
      ("numValueElements", classOf[Integer]),
//      ("opcode", classOf[TagOpCode]),
      ("opcode", classOf[String]),
      ("pageNumber", classOf[java.lang.Long]),
      ("pageOffset", classOf[java.lang.Long]),
//      ("parameters", classOf[Seq[Value]]),
      ("parameters", classOf[String]),
      ("parentSubjectUuid", classOf[UUID]),
      ("peInfo", classOf[String]),
//      ("permission", classOf[FixedShort]),
      ("permission", classOf[String]),
      ("predicateObjectPath", classOf[String]),
      ("predicateObjectUuid", classOf[UUID]),
      ("predicateObject2Path", classOf[String]),
      ("predicateObject2Uuid", classOf[UUID]),
      ("prevTagIdUuid", classOf[UUID]),
//      ("principalType", classOf[PrincipalType]),
      ("principalType", classOf[String]),
//      ("privilegeLevel", classOf[PrivilegeLevel]),
      ("privilegeLevel", classOf[String]),
      ("programPoint", classOf[String]),
      ("remoteAddress", classOf[String]),
      ("remotePort", classOf[Integer]),
      ("runtimeDataType", classOf[String]),
      ("sequence", classOf[java.lang.Long]),
      ("sinkFileDescriptor", classOf[Integer]),
      ("size", classOf[java.lang.Long]),
      ("sourceFileDescriptor", classOf[Integer]),
//      ("srcSinkType", classOf[SrcSinkType]),
      ("srcSinkType", classOf[String]),
      ("startTimestampNanos", classOf[java.lang.Long]),
//      ("subjectType", classOf[SubjectType]),
      ("subjectType", classOf[String]),
      ("subjectUuid", classOf[UUID]),
      ("systemCall", classOf[String]),
//      ("tag", classOf[Seq[TagRunLengthTuple]]),
      ("tagRunLengthTuples", classOf[String]),
      ("tagIds", classOf[UUID], Cardinality.LIST),
      ("threadId", classOf[Integer]),
      ("timestampNanos", classOf[java.lang.Long]),
//      ("type", classOf[CryptoHashType]),
      ("type", classOf[String]),
      ("unitId", classOf[Integer]),
      ("unitUuid", classOf[UUID]),
      ("userId", classOf[String]),
      ("username", classOf[String]),
      ("uuid", classOf[UUID]),
//      ("value", classOf[Value]),
      ("value", classOf[String]),
//      ("valueBytes", classOf[Array[Byte]]),
      ("valueBytes", classOf[String]),
//      ("valueDataType", classOf[ValueDataType]),
      ("valueDataType", classOf[String]),
//      ("valueType", classOf[ValueType])
      ("valueType", classOf[String])
    )
    for (propertyKey <- propertyKeys)
      //if(!management.containsPropertyKey(propertyKey._1)) { management.makePropertyKey(propertyKey._1).dataType(propertyKey._2).make() }
      propertyKey match {
        case (name: String, pClass: Class[_]) if !management.containsPropertyKey(name) =>
          management.makePropertyKey(name).dataType(pClass).cardinality(Cardinality.SINGLE).make()
        case (name: String, pClass: Class[_], cardinality: Cardinality) if !management.containsPropertyKey(name) =>
          management.makePropertyKey(name).dataType(pClass).cardinality(cardinality).make()
        case _ => ()
      }

    // This makes a unique index for 'uuid'
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
    .groupedWithin(20000, 5 seconds)
    .via(FlowComponents.printCounter("Titan Writer", 10000))
    .toMat(
      Sink.foreach[collection.immutable.Seq[DBNodeable]]{ cdms =>
      val transaction = graph.newTransaction()

      // For the duration of the transaction, we keep a 'Map[UUID -> Vertex]' of vertices created
      // during this transaction (since we don't look those up in the usual manner).
      val newVertices = collection.mutable.Map.empty[UUID,Vertex]

      // We also need to keep track of edges that point to nodes we haven't found yet (this lets us
      // handle cases where nodes are out of order).
      var missingToUuid = collection.mutable.Map.empty[UUID, Set[(Vertex,String)]]

      // Accordingly, we define a function which lets us look up a vertex by UUID - first by checking
      // the 'newVertices' map, then falling back on a query to Titan.
      def findNode(uuid: UUID): Option[Vertex] = newVertices.get(uuid) orElse {
        val iterator = graph.traversal().V().has("uuid", uuid)
        if (iterator.hasNext()) { Some(iterator.next()) } else { None}
      }

      // Process all of the nodes
        for (cdm <- cdms) {
          // Note to Ryan: I'm sticking with the try block here instead of .recover since that seems to cancel out all following cdm statements.
          // iIf we have a failure on one CDM statement my thought is we want to log the failure but continue execution.
          try {
            val props: List[Object] = cdm.asDBKeyValues.asInstanceOf[List[Object]]
            assert(props.length % 2 == 0, s"Node ($cdm) has odd length properties list: $props.")
            val newTitanVertex = transaction.addVertex(props: _*)
            newVertices += (cdm.getUuid -> newTitanVertex)

            for ((label,toUuid) <- cdm.asDBEdges) {
              findNode(toUuid) match {
                case Some(toTitanVertex) =>
                  newTitanVertex.addEdge(label, toTitanVertex)
                case None =>
                  missingToUuid(toUuid) = missingToUuid.getOrElse(toUuid, Set[(Vertex,String)]()) + (newTitanVertex -> label)
              }
            }
          }
          catch {
            // TODO make this more useful
            case e: java.lang.IllegalArgumentException =>
              if (!e.getMessage.contains("byUuidUnique")) {
                println("Failed CDM statement: " + cdm)
                println(e.getMessage) // Bad query
                e.printStackTrace()
                throw e
              }
            case unk: Throwable => println(unk)
          }
        }

      // Try to complete missing edges. If the node pointed to is _still_ not found, we
      // synthetically create it.
      var nodeCreatedCounter = 0
      var edgeCreatedCounter = 0

      for ((uuid,edges) <- missingToUuid; (fromTitanVertex,label) <- edges) {

        // Find or create the missing vertex (it may have been created earlier in this loop)
        val toTitanVertex = findNode(uuid) getOrElse {
          nodeCreatedCounter += 1
          val newNode = transaction.addVertex("uuid", UUID.randomUUID()) // uuid)
          newVertices += (uuid -> newNode)
          newNode
        }

        // Create the missing edge
        edgeCreatedCounter += 1
        fromTitanVertex.addEdge(label, toTitanVertex)
      }

      println(s"Created $nodeCreatedCounter synthetic nodes and $edgeCreatedCounter edges")

      transaction.commit()
      println(s"Committed transaction with ${cdms.length}")
    }
  )(Keep.right)
}




object TestGraph extends App {
  implicit val system = ActorSystem("test")
  implicit val ec = system.dispatcher
  implicit val mat = ActorMaterializer()


  val path = "/Users/erin/Documents/proj/adapt/git/Adapt/AdaptJVM/ta1-clearscope-cdm17.bin" // cdm17_0407_1607.bin" //ta1-clearscope-cdm17.bin"  //
  val data = CDM17.readData(path, None).get._2.map(_.get)
  val source = Source.fromIterator[CDM17](() => CDM17.readData(path, None).get._2.map(_.get))
//    .concat(Source.fromIterator[CDM17](() => CDM17.readData(path + ".1", None).get._2.map(_.get)))
//    .concat(Source.fromIterator[CDM17](() => CDM17.readData(path + ".2", None).get._2.map(_.get)))
    .via(FlowComponents.printCounter("CDM Source", 1e5.toInt))
//    .via(Streams.titanWrites(graph))

  println("Total CDM statements: " + data.length)


//  // TODO: this should be a single source (instead of multiple copies) that broadcasts into all the necessary places.
//  val fastCommandSource = Source.tick[ProcessingCommand](6 seconds, 6 seconds, CleanUp).buffer(1, OverflowStrategy.backpressure)
//    .merge(Source.tick[ProcessingCommand](20 seconds, 20 seconds, Emit).buffer(1, OverflowStrategy.backpressure))
////    .via(FlowComponents.printCounter("Command Source", 1))
//
//  val slowCommandSource = Source.tick[ProcessingCommand](30 seconds, 30 seconds, CleanUp).buffer(1, OverflowStrategy.backpressure)
//    .merge(Source.tick[ProcessingCommand](50 seconds, 50 seconds, Emit).buffer(1, OverflowStrategy.backpressure))


  val dbFilePath = "/tmp/map.db"
  val db = DBMaker.fileDB(dbFilePath).fileMmapEnable().make()
  new File(dbFilePath).deleteOnExit()  // Only meant as ephemeral on-disk storage.


//  TitanUtils.titanWrites(TitanUtils.graph)
//    .runWith(source.via(FlowComponents.printCounter("titan write count", 1)), Sink.ignore)

//  FlowComponents.testNetFlowFeatureExtractor(commandSource, db)
//    .runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/netFlowFeatures.csv"))

//  FlowComponents.fileFeatureGenerator(commandSource, db)
//    .runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/fileFeatures.csv"))



//  Flow[CDM17].collect{ case e: Event => e }.groupBy(Int.MaxValue, _.toString).mergeSubstreams.via(FlowComponents.printCounter[Event]("Event counter", 100)).recover{ case e: Throwable => e.printStackTrace()}.runWith(source, Sink.ignore)




  source.runForeach(println)

  Flow[CDM17].runWith(source, TitanFlowComponents.titanWrites(TitanFlowComponents.graph))


//  FlowComponents.normalizedScores(db).recover{ case e: Throwable => e.printStackTrace()}.runWith(source, Sink.foreach(println))


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
