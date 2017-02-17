package com.galois.adapt

import akka.actor._
import com.galois.adapt.cdm13.CDM13
import collection.mutable.Queue

class FileIngestActor(val registry: ActorRef) extends Actor with ActorLogging with ServiceClient {

  var subscribers = Seq.empty[ActorRef]

  val dependencies = "DevDBActor" :: Nil   // TODO: this shouldn't really depend on something down stream!

  var jobQueue = Queue.empty[IngestFile]

  def processJobQueue() = while (jobQueue.nonEmpty) {
    val j = jobQueue.dequeue()
    val data = CDM13.readData(j.path, j.loadLimit).get
    var counter = 0
    data.foreach { d =>
      subscribers.foreach(_ ! d.get)
    }
    log.info(s"Ingested total events: $counter")
  }

  def beginService() = { //()  // TODO
    log.info("Ingest Actor is starting ingest process")
    subscribers = subscribers :+ dependencyMap("DevDBActor").get
    context.self ! IngestFile(
      "/Users/ryan/Code/adapt/AdaptJVM/Engagement1DataCDM13/pandex/ta1-cadets-cdm13_pandex.bin.1",
      Some(10000)
    )

  }

  def endService() = ()  // TODO

  def localReceive: PartialFunction[Any,Unit] = {
    case Subscription =>
      log.info("got subscription from: {}", sender())
      context watch sender()
      subscribers = subscribers :+ sender()

    case msg @ IngestFile(path, limitOpt) if subscribers.nonEmpty =>
      log.info("got ingest request from: {} to ingest file: {}", sender())
      jobQueue.enqueue(msg)
      processJobQueue()
  }
}

case class IngestFile(path: String, loadLimit: Option[Int] = None)

case object Subscription
