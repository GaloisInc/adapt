package com.galois.adapt

import java.nio.file.Paths
import java.util.UUID
import akka.stream.scaladsl._
import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
import akka.actor.ActorRef
import akka.util.ByteString
import scala.collection.mutable
import com.galois.adapt.adm.EntityResolution
import com.galois.adapt.cdm18._
import com.typesafe.config.ConfigFactory
import collection.JavaConverters._
import scala.util.Try


object FlowComponents {

  val config = ConfigFactory.load()


  def printCounter[T](counterName: String, statusActor: ActorRef, every: Int = 10000) = Flow[T].statefulMapConcat { () =>
    var counter = config.getLong("adapt.ingest.startatoffset")
    var originalStartTime = 0L
    var lastTimestampNanos = 0L
    val populationCounter = MutableMap.empty[String, Long]

    { item: T =>  // Type annotation T is a compilation hack! No runtime effect because it's generic.
      if (lastTimestampNanos == 0L) {
        originalStartTime = System.nanoTime()
        lastTimestampNanos = System.nanoTime()
      }
      counter = counter + 1
      val className = item.getClass.getSimpleName
      populationCounter += (className -> (populationCounter.getOrElse(className, 0L) + 1))

      if (counter % every == 0) {
        val nowNanos = System.nanoTime()
        val durationSeconds = (nowNanos - lastTimestampNanos) / 1e9
        import collection.JavaConverters._

        val admFuturesCount = EntityResolution.asyncTime.values().asScala.size
        val measuredMillis = EntityResolution.totalHistoricalTimeInAsync
        val countOutOfBuffer = EntityResolution.totalHistoricalCountInAsync
        val averageMillisInAsyncBuffer = Try(measuredMillis.toFloat / countOutOfBuffer).getOrElse(0F)

        val blockEdgesCount = EntityResolution.blockedEdgesCount.get()
        val blockingNodes = EntityResolution.blockingNodes.size
        val currentTime = EntityResolution.currentTime

        println(s"$counterName ingested: $counter   Elapsed: ${f"$durationSeconds%.3f"} seconds.  Rate: ${(every / durationSeconds).toInt} items/second.  Rate since beginning: ${(counter / ((nowNanos - originalStartTime) / 1e9)).toInt} items/second.  ADM buffer: $admFuturesCount.  Edges waiting: $blockEdgesCount.  Nodes blocking edges: $blockingNodes")

        statusActor ! PopulationLog(
          counterName,
          counter,
          every,
          populationCounter.toMap,
          admFuturesCount,
          countOutOfBuffer,
          averageMillisInAsyncBuffer,
          durationSeconds,
          blockEdgesCount,
          blockingNodes
        )

        populationCounter.clear()

        lastTimestampNanos = nowNanos
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
