package com.galois.adapt

import akka.actor._
import com.galois.adapt.cdm13.CDM13

class FileIngestActor extends Actor with ActorLogging {

  var subscribers = IndexedSeq.empty[ActorRef]

  def receive = {
    case Subscription =>
      log.info("got subscription from: {}", sender())
      context watch sender()
      subscribers = subscribers :+ sender()

    case IngestFile(path, limitOpt) if subscribers.nonEmpty =>
      log.info("got ingest request from: {} to ingest file: {}", sender())
      val data = CDM13.readData(path, limitOpt).get
      var counter = 0
      data.foreach { d =>
        subscribers.foreach(_ ! d.get)
      }
  }
}

case class IngestFile(path: String, loadLimit: Option[Int] = None)

case object Subscription
