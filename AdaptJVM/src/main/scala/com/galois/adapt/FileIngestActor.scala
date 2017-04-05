package com.galois.adapt

import akka.actor._
import com.galois.adapt.cdm17.{InstrumentationSource, CDM17}
import collection.mutable.Queue
import scala.util.{Try,Success,Failure}

class FileIngestActor(val registry: ActorRef, val minSubscribers: Int)
  extends Actor with ActorLogging with ServiceClient with SubscriptionActor[CDM17] {

  log.info("FileIngestActor created")

  val dependencies = List.empty
  val subscriptions = Set[Subscription]()

  val jobQueue = Queue.empty[IngestFile]
  var errors = Nil

  /*
   * This function checks to see if it has the right number of subscribers and something to ingest.
   * Once those conditions are met, it ingests everything it can, broadcasting CDM statements. It
   * also broadcasts:
   *
   *  - _ErrorReadingFile_ when starting to process a new file which could not be read by Avro
   *  - _BeginFile_ when starting to process a new file which _could_ be read by Avro
   *  - _ErrorReadingStatement_ when encountering a CDM statement that could not be read (usu. wrong
   *  schema)
   *  - _DoneFile_ when done with a file. These pair up with _ErrorReadingFile_/_BeginFile_
   *  - _DoneIngest_ when the queue is empty after having not been empty
   *
   */
  def processJobQueue() = if (subscribers.size >= minSubscribers && jobQueue.nonEmpty) {
    while (subscribers.size >= minSubscribers && jobQueue.nonEmpty) {
      val j = jobQueue.dequeue()
      log.info(s"Starting ingest from file: ${j.path}" + j.loadLimit.fold("")(i => "  of " +
      i.toString + s" CDM statements"))

      log.info(s"Ingesting from file: ${j.path}")
      log.info("subscribers: " + subscribers.toString)

      Thread.sleep(2000)

      CDM17.readData(j.path, j.loadLimit) match {
        case Failure(t) =>
          // Can't ingest file
          println("COULD NOT PARSE!!!!")
          broadCastUnsafe(ErrorReadingFile(j.path,t));
        
        case Success((source,data)) =>
          // Starting to process file
          broadCastUnsafe(BeginFile(j.path, source))
  
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
    
    println("Done ingest")
    // Emptied job queue
    broadCastUnsafe(DoneIngest)
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

sealed trait IngestControl
case class IngestFile(path: String, loadLimit: Option[Int] = None) extends IngestControl
case class BeginFile(path: String, source: InstrumentationSource) extends IngestControl
case class DoneFile(path: String) extends IngestControl
case object DoneIngest extends IngestControl
case class ErrorReadingStatement(exception: Throwable) extends IngestControl
case class ErrorReadingFile(path: String, exception: Throwable) extends IngestControl

