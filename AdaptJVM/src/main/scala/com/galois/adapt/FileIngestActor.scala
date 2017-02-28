package com.galois.adapt

import akka.actor._
import com.galois.adapt.cdm13.CDM13
import collection.mutable.Queue

class FileIngestActor(val registry: ActorRef)
  extends Actor with ActorLogging with ServiceClient with SubscriptionActor[CDM13] {

  val dependencies = List.empty
  val subscriptions = Set[Subscription]()

  var jobQueue = Queue.empty[IngestFile]

  def processJobQueue() = while (subscribers.nonEmpty && jobQueue.nonEmpty) {
    val j = jobQueue.dequeue()
    log.info(s"Starting ingest from file: ${j.path}" + j.loadLimit.fold("")(i => "  of " +
    i.toString + s" CDM statements"))
    val data = CDM13.readData(j.path, j.loadLimit).get
    var counter = 0
    data.foreach { d =>
      broadCast(d.get)
      counter = counter + 1
    }
    log.info(s"Ingested total events: $counter  from: ${j.path}")
  }

  def beginService() = {
    log.info("Ingest Actor is starting up")
    initialize()
    processJobQueue()
  }
  def endService() = ()  // TODO

  override def receive: PartialFunction[Any,Unit] = ({
    case msg @ IngestFile(path, limitOpt) =>
      log.info("Received ingest request from: {} to ingest: {} events from file: {}", sender(), limitOpt.getOrElse("ALL"), path)
      jobQueue.enqueue(msg)
      processJobQueue()
    case s: Subscription =>
      subscribers += s
      processJobQueue()
  }: PartialFunction[Any,Unit]) orElse super.receive
}

case class IngestFile(path: String, loadLimit: Option[Int] = None)

