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
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.Producer
import akka.kafka.scaladsl.Consumer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.apache.avro.specific.{SpecificDatumReader, SpecificDatumWriter}
import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
import GraphDSL.Implicits._
import akka.util.ByteString
import org.mapdb.{DB, DBMaker, HTreeMap}
import collection.JavaConverters._
import scala.collection.mutable
import scala.sys.process._
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success, Try}
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.tinkerpop.gremlin.structure.Vertex
import scala.concurrent.{ExecutionContext, Future}
import scala.io.{Source => FileSource}
import NetFlowStream._
import FileStream._
import ProcessStream._
import MemoryStream._
import com.typesafe.config.ConfigFactory


object FlowComponents {

  val config = ConfigFactory.load()


  def predicateTypeLabeler(commandSource: Source[ProcessingCommand,_], db: DB): Flow[CDM17, (String, UUID, Event, CDM17), _] = {
//    val dbMap = db.hashMap("typeSorter_" + Random.nextLong()).createOrOpen().asInstanceOf[HTreeMap[UUID,mutable.SortedSet[Event]]]
    Flow[CDM17]
      .mapConcat[(UUID, String, CDM17)] {
        case e: Event if e.predicateObject.isDefined && e.eventType != EVENT_OTHER && e.eventType != EVENT_CHECK_FILE_ATTRIBUTES =>    // Throw away all EVENT_OTHERs
          if (e.predicateObject2.isDefined) List((e.predicateObject.get, "Event", e), (e.predicateObject2.get, "Event", e))
          else List((e.predicateObject.get, "Event", e))
        case n: NetFlowObject => List((n.uuid, "NetFlowObject", n))
        case f: FileObject => List((f.uuid, "FileObject", f))
        case s: Subject => List((s.uuid, "Subject", s))
        case m: MemoryObject => List((m.uuid, "MemoryObject", m))
        case s: SrcSinkObject => List((s.uuid, "SrcSinkObject", s))
        case u: UnnamedPipeObject => List((u.uuid, "UnnamedPipeObject", u))
        case p: Principal => List((p.uuid, "Principal", p))
//        case msg @ => List(msg) }
        case _ => List.empty
    }
      .groupBy(Int.MaxValue, _._1)
      .merge(commandSource)
      .statefulMapConcat[(String, UUID, Event, CDM17)] { () =>
        var targetUuid: Option[UUID] = None
        var idOpt: Option[(UUID,String,CDM17)] = None
        val events = mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))
        var cleanupCount = 0
        var shouldStore = true

        {
          case Tuple3(predicateUuid: UUID, "Event", e: Event) =>
            if (targetUuid.isEmpty) targetUuid = Some(predicateUuid)
            if (idOpt.isDefined)
              List((idOpt.get._2, predicateUuid, e, idOpt.get._3))
            else {
              if (shouldStore) events += e
              List.empty
            }

          case Tuple3(objectUuid: UUID, labelName: String, cdm: CDM17) =>
            cleanupCount = 0
            if (targetUuid.isEmpty) targetUuid = Some(objectUuid)
            if (idOpt.isEmpty) {
              idOpt = Some((objectUuid, labelName, cdm))
              if (! shouldStore) println(s"WARNING: Previously gave up trying to predicate-type-label events for $objectUuid, but the object has now just arrived.")
//              val existingSet = dbMap.getOrDefault(objectUuid, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos)))
//              events ++= existingSet
//              dbMap.remove(objectUuid)
            }
            val toSend = events.toList.map(event => (labelName, objectUuid, event, cdm))
            events.clear()
            toSend

          case CleanUp =>
            if (idOpt.isEmpty) cleanupCount += 1
            if (idOpt.isEmpty && events.nonEmpty && cleanupCount >= config.getInt("adapt.runtime.cleanupthreshold")) {
              println(s"Never received the object for events with a predicate to: ${targetUuid.getOrElse("never known")}")
              shouldStore = false
              events.clear()
            }
            List.empty

////            if (events.nonEmpty) {
////              val existingSet = dbMap.getOrDefault(uuidOpt.get, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos)))
////              events ++= existingSet
////  //            println(s"UNMATCHED: ${uuidOpt}  size: ${events.size}    ${events.map(_.eventType)}")  // TODO
////              dbMap.put(uuidOpt.get, events)
////              events.clear()
////            }
//            List.empty
//
          case EmitCmd => List.empty
        }
      }.mergeSubstreams
  }

//case class LabeledPredicateType(labelName: String, predicateObjectUuid: UUID, event: Event, cdm: CDM17)


  def printCounter[T](name: String, every: Int = 10000) = Flow[T].statefulMapConcat { () =>
    var counter = config.getLong("adapt.ingest.startatoffset")
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


  def csvFileSink(path: String) = Flow[(UUID, Map[String,Any])]
    .statefulMapConcat{ () =>
      var needsHeader = true;
      { case Tuple2(u: UUID, m: Map[String,Any]) =>
        val noUuid = m.-("uuid").mapValues {
          case m: Map[_,_] => if (m.isEmpty) "" else m
          case x => x
        }.toList.sortBy(_._1)
        val row = List(ByteString(s"$u,${noUuid.map(_._2.toString.replaceAll(",","|")).mkString(",")}\n"))
        if (needsHeader) {
          needsHeader = false
          List(ByteString(s"uuid,${noUuid.map(_._1.toString.replaceAll(",","|")).mkString(",")}\n")) ++ row
        } else row
      }
    }.toMat(FileIO.toPath(Paths.get(path)))(Keep.right)


  def commandSource(cleanUpSeconds: Int, emitSeconds: Int) =
    Source.tick[ProcessingCommand](cleanUpSeconds seconds, cleanUpSeconds seconds, CleanUp).buffer(1, OverflowStrategy.backpressure)
      .merge(Source.tick[ProcessingCommand](emitSeconds seconds, emitSeconds seconds, EmitCmd).buffer(1, OverflowStrategy.backpressure))


  type MilliSeconds = Long
  type NanoSeconds = Long

  implicit class EventCollection(es: Iterable[Event]) {
    def timeBetween(first: Option[EventType], second: Option[EventType]): NanoSeconds = {
      val foundFirst = if (first.isDefined) es.dropWhile(_.eventType != first.get) else es
      val foundSecond = if (second.isDefined) foundFirst.drop(1).find(_.eventType == second.get) else es.lastOption
      foundFirst.headOption.flatMap(f => foundSecond.map(s => s.timestampNanos - f.timestampNanos)).getOrElse(0L)
    }

    def sizePerSecond(t: EventType): Float = {
      val events = es.filter(_.eventType == t)
      val lengthOpt = events.headOption.flatMap(h => events.lastOption.map(l => l.timestampNanos / 1e9 - (h.timestampNanos / 1e9)))
      val totalSize = events.toList.map(_.size.getOrElse(0L)).sum
      lengthOpt.map(l => if (l > 0D) totalSize / l else 0D).getOrElse(0D).toFloat
    }
  }
}


trait ProcessingCommand extends CDM17
case class AdaptProcessingInstruction(id: Long) extends ProcessingCommand
case object EmitCmd extends ProcessingCommand
case object CleanUp extends ProcessingCommand
