package com.galois.adapt

import java.nio.file.Paths
import java.text.NumberFormat
import java.util.UUID
import akka.NotUsed
import akka.stream.scaladsl._
import scala.collection.mutable.{Map => MutableMap}
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.{FlowShape, OverflowStrategy}
import akka.util.ByteString
import scala.collection.mutable
import com.galois.adapt.cdm20._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration


object FlowComponents {

  def printCounter[T](counterName: String, statusActor: ActorRef, startingCount: Long = 0, every: Int = 10000) = Flow[T].statefulMapConcat { () =>
    var counter = startingCount
    var originalStartTime = 0L
    var lastTimestampNanos = 0L
    val recentPopulationCounter = MutableMap.empty[String, Long]
    val totalPopulationCounter = MutableMap.empty[String, Long]

    val numberFormatter = NumberFormat.getInstance()

    { item: T =>  // Type annotation T is a compilation hack! No runtime effect because it's generic.
      if (lastTimestampNanos == 0L) {
        originalStartTime = System.nanoTime()
        lastTimestampNanos = System.nanoTime()
      }
      counter = counter + 1
      def makeName(thing: Any): String = thing match {
        case (_, e: Event) => e.eventType.toString
        case (_, i: AnyRef) => i.getClass.getSimpleName
        case e: Event => e.eventType.toString
        case Left(l) => makeName(l)
        case Right(r) => s"Right[${r.getClass.getSimpleName}]"
        case i => i.getClass.getSimpleName
      }
      val className = makeName(item)
      recentPopulationCounter += (className -> (recentPopulationCounter.getOrElse(className, 0L) + 1))
      totalPopulationCounter  += (className -> (totalPopulationCounter.getOrElse(className, 0L)  + 1))

      if (counter % every == 0) {
        val nowNanos = System.nanoTime()
        val durationSeconds = (nowNanos - lastTimestampNanos) / 1e9

        println(s"$counterName ingested: ${numberFormatter.format(counter)}   Elapsed: ${f"$durationSeconds%.3f"} seconds.  Rate: ${numberFormatter.format((every / durationSeconds).toInt)} items/second. At time: ${System.currentTimeMillis}") //  Rate since beginning: ${((counter - startingCount) / ((nowNanos - originalStartTime) / 1e9)).toInt} items/second.")

        statusActor ! PopulationLog(
          counterName,
          counter,
          every,
          recentPopulationCounter.toMap,
          totalPopulationCounter.toMap,
          durationSeconds
        )

        recentPopulationCounter.clear()

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


trait ProcessingCommand extends CurrentCdm
case class AdaptProcessingInstruction(id: Long) extends ProcessingCommand
case object EmitCmd extends ProcessingCommand
case object CleanUp extends ProcessingCommand




/**
  * Utility for debugging which components are bottlenecks in a backpressured stream system. Instantiate one of these
  * per stream system, then place [[debugBuffer]] between stages to find out whether the bottleneck is upstream (in
  * which case the buffer will end up empty) or downstream (in which case the buffer will end up full).
  *
  * @param prefix prompt with which to start status update lines
  * @param printEvery how frequently to print out status updates
  * @param reportEvery how frequently should the debug buffers report their status to the [[StreamDebugger]]
  */
class StreamDebugger(prefix: String, printEvery: FiniteDuration, reportEvery: FiniteDuration)
  (implicit system: ActorSystem) {

  implicit val ec: ExecutionContext = system.dispatchers.lookup("quine.actor.node-dispatcher")

  import java.util.concurrent.ConcurrentHashMap
  import java.util.concurrent.atomic.AtomicInteger
  import java.util.function.BiConsumer

  val bufferCounts: ConcurrentHashMap[String, Long] = new ConcurrentHashMap()

  val scheduledStreamBuffersReport = system.scheduler.schedule(printEvery, printEvery, new Runnable {
    override def run(): Unit = {
      val listBuffer = mutable.ListBuffer.empty[(String, Long)]
      bufferCounts.forEach(new BiConsumer[String, Long] {
        override def accept(key: String, count: Long): Unit = listBuffer += (key -> count)
      })
      println(listBuffer
        .sortBy(_._1)
        .toList
        .map { case (stage, count) => s"$prefix $stage: $count" }
        .mkString(s"$prefix ==== START OF STREAM-BUFFERS REPORT ====\n", "\n", s"\n$prefix ==== END OF STREAM-BUFFERS REPORT ====")
      )
    }
  })
  system.registerOnTermination(scheduledStreamBuffersReport.cancel())

  /**
    * Create a new debug buffer flow. This is just like a (backpressured) buffer flow, but it keeps track of how many
    * items are in the buffer (possibly plus one) and reports this number periodically to the object on which this
    * method is called.
    *
    * @param name what label to associate with this debug buffer (should be unique per [[StreamDebugger]]
    * @param bufferSize size of the buffer being created
    * @tparam T type of thing flowing through the buffer
    * @return a buffer flow which periodically reports stats about how full its buffer is
    */
  def debugBuffer[T](name: String, bufferSize: Int = 10000): Flow[T,T,NotUsed] =
    Flow.fromGraph(GraphDSL.create() {
      implicit graph =>

        import GraphDSL.Implicits._

        val bufferCount: AtomicInteger = new AtomicInteger(0)

        // Write the count out to the buffer count map regularly
        system.scheduler.schedule(reportEvery, reportEvery, new Runnable {
          override def run(): Unit = bufferCounts.put(name, bufferCount.get())
        })

        // Increment the count when entering the buffer, decrement it when exiting
        val incrementCount = graph.add(Flow.fromFunction[T,T](x => { bufferCount.incrementAndGet(); x }))
        val buffer = graph.add(Flow[T].buffer(bufferSize, OverflowStrategy.backpressure))
        val decrementCount = graph.add(Flow.fromFunction[T,T](x => { bufferCount.decrementAndGet(); x }))

        incrementCount.out ~> buffer ~> decrementCount

        FlowShape(incrementCount.in, decrementCount.out)
    })
}
