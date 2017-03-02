package com.galois.adapt

import akka.actor._
import com.galois.adapt.cdm13.CDM13
import collection.mutable.Queue
import scala.util.{Try,Success,Failure}

class FileIngestActor(val registry: ActorRef)
  extends Actor with ActorLogging with ServiceClient with SubscriptionActor[CDM13] {

  log.info("FileIngestActor created")

  val dependencies = List.empty
  val subscriptions = Set[Subscription]()

  val jobQueue = Queue.empty[IngestFile]
  var errors = Nil

  def processJobQueue() = while (subscribers.size >= 2 && jobQueue.nonEmpty) {
    val j = jobQueue.dequeue()
    log.info(s"Starting ingest from file: ${j.path}" + j.loadLimit.fold("")(i => "  of " +
    i.toString + s" CDM statements"))

    // Starting to process file
    broadCastUnsafe(BeginFile(j.path))

    log.info("Ingesting")
    log.info("subscribers: " + subscribers.toString)

    CDM13.readData(j.path, j.loadLimit) match {
      case Failure(t) =>
        broadCastUnsafe(ErrorReadingFile(j.path,t));
        log.info(s"Ingestion from ${j.path} failed")
      case Success(data) =>
        var counter = 0
        data.foreach {
          case Failure(t) => broadCastUnsafe(ErrorReadingStatement(t))
          case Success(cdm) =>
            broadCast(cdm)
            counter += 1
        }
        log.info(s"Ingested total events: $counter  from: ${j.path}")
    }
 
    // Finished processing file
    broadCastUnsafe(DoneFile(j.path))
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
case class BeginFile(path: String/*, TODO source: InstrumentationSource */)
case class DoneFile(path: String/*, TODO source: InstrumentationSource */)
case class ErrorReadingStatement(exception: Throwable)
case class ErrorReadingFile(path: String, exception: Throwable)

