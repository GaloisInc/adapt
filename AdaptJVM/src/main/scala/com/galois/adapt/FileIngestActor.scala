package com.galois.adapt

import akka.actor._
import com.galois.adapt.cdm13.CDM13
import collection.mutable.Queue

class FileIngestActor(val registry: ActorRef) extends Actor with ActorLogging with ServiceClient {

  var subscribers = Seq.empty[ActorRef]

  val dependencies = "DevDBActor" :: Nil   // TODO: this shouldn't really depend on something down stream!

  var jobQueue = Queue.empty[IngestFile]

  def processJobQueue() = while (subscribers.nonEmpty && jobQueue.nonEmpty) {
    val j = jobQueue.dequeue()
    log.info(s"Starting ingest from file: ${j.path}" + j.loadLimit.fold("")(i => "  of " + i.toString + " CDM statements"))
    val data = CDM13.readData(j.path, j.loadLimit).get
    var counter = 0
    data.foreach { d =>
      subscribers.foreach(_ ! d.get)
      counter = counter + 1
    }
    log.info(s"Ingested total events: $counter  from: ${j.path}")
  }

  def beginService() = { //()  // TODO
    log.info("Ingest Actor is starting up")
    subscribers = subscribers :+ dependencyMap("DevDBActor").get
    processJobQueue()
//    context.self ! IngestFile(
//      "Engagement1DataCDM13/pandex/ta1-cadets-cdm13_pandex.bin.1",
//      Some(100)
//    )

  }

  def endService() = ()  // TODO

  def localReceive: PartialFunction[Any,Unit] = {
    case Subscription =>
      log.info("Received subscription from: {}", sender())
      context watch sender()
      subscribers = subscribers :+ sender()

    case msg @ IngestFile(path, limitOpt) =>
      log.info("Received ingest request from: {} to ingest: {} events from file: {}", sender(), limitOpt.getOrElse("ALL"), path)
      jobQueue.enqueue(msg)
      processJobQueue()
  }
}

case class IngestFile(path: String, loadLimit: Option[Int] = None)

case object Subscription
