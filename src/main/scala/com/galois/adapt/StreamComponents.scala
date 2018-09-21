package com.galois.adapt

import java.nio.file.Paths
import java.util.UUID

import akka.stream.scaladsl._

import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
import akka.actor.ActorRef
import akka.stream.{FlowShape, OverflowStrategy}
import akka.util.ByteString

import scala.collection.mutable
import com.galois.adapt.adm.EntityResolution
import com.galois.adapt.adm.EntityResolution.sampledTime
import com.galois.adapt.cdm18._
import com.typesafe.config.ConfigFactory

import collection.JavaConverters._
import scala.util.Try


object FlowComponents {

  val config = ConfigFactory.load()


  def printCounter[T](counterName: String, statusActor: ActorRef, every: Int = 10000) = Flow[T].statefulMapConcat { () =>
    val startingCount = Try(config.getLong("adapt.ingest.startatoffset")).getOrElse(0L)
    var counter = startingCount
    var originalStartTime = 0L
    var lastTimestampNanos = 0L
    val populationCounter = MutableMap.empty[String, Long]

    { item: T =>  // Type annotation T is a compilation hack! No runtime effect because it's generic.
      if (lastTimestampNanos == 0L) {
        originalStartTime = System.nanoTime()
        lastTimestampNanos = System.nanoTime()
      }
      counter = counter + 1
      val className = item match {
        case (_, e: Event) => e.eventType.toString
        case (_, i: AnyRef) => i.getClass.getSimpleName
        case i => i.getClass.getSimpleName
      }
      populationCounter += (className -> (populationCounter.getOrElse(className, 0L) + 1))

      if (counter % every == 0) {
        val nowNanos = System.nanoTime()
        val durationSeconds = (nowNanos - lastTimestampNanos) / 1e9

        // Ordering nodes stats
        val blockEdgesCount = EntityResolution.blockedEdgesCount
        val blockingNodes = Application.blockedEdges.size

        val currentTime = EntityResolution.monotonicTime
        val sampledTime = EntityResolution.sampledTime

        // UuidRemapper related stats
        val uuidsBlocking: Int = Application.blockedEdges.size
        val blockedUuidResponses: Int = Application.blockedEdges.values.map(_._1.length).sum

        val activeEventChains = EntityResolution.activeChains.size

        val cdm2cdmSize = Application.cdm2cdmMaps.map(_.size()).sum
        val cdm2admSize = Application.cdm2admMaps.map(_.size()).sum

        val seenNodesSize = Application.seenNodes.size()
        val seenEdgesSize = Application.seenEdges.size()

        println(s"$counterName ingested: $counter   Elapsed: ${f"$durationSeconds%.3f"} seconds.  Rate: ${(every / durationSeconds).toInt} items/second.  Rate since beginning: ${((counter - startingCount) / ((nowNanos - originalStartTime) / 1e9)).toInt} items/second.  Edges waiting: $blockEdgesCount.  Nodes blocking edges: $blockingNodes")

        statusActor ! PopulationLog(
          counterName,
          counter,
          every,
          populationCounter.toMap,
          durationSeconds,
          blockEdgesCount,
          blockingNodes,
          uuidsBlocking,
          blockedUuidResponses,
          activeEventChains,
          cdm2cdmSize,
          cdm2admSize,
          seenNodesSize,
          seenEdgesSize,
          currentTime,
          sampledTime
        )

        populationCounter.clear()

        lastTimestampNanos = nowNanos
      }
      List(item)
    }
  }


  def splitToSink[T](sink: Sink[T, _], bufferSize: Int = 0) = Flow.fromGraph( GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._
    val broadcast = b.add(Broadcast[T](2))
    bufferSize match {
      case x if x <= 0 => broadcast.out(0) ~> sink
      case s => broadcast.out(0).buffer(s, OverflowStrategy.backpressure) ~> sink
    }
    FlowShape(broadcast.in, broadcast.out(1))
  })


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


trait ProcessingCommand extends CDM18
case class AdaptProcessingInstruction(id: Long) extends ProcessingCommand
case object EmitCmd extends ProcessingCommand
case object CleanUp extends ProcessingCommand
