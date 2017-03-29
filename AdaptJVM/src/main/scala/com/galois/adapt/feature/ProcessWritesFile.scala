package com.galois.adapt.feature

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.galois.adapt.cdm16.{Event, FileObject, Subject, TimeMarker}
import com.galois.adapt.{ReportsStatus, ServiceClient, Subscription, SubscriptionActor}

import scala.collection.mutable.{Map => MutableMap}

class ProcessWritesFile(val registry: ActorRef) extends Actor with ActorLogging with ServiceClient with SubscriptionActor[(Subject,Event,FileObject)] with ReportsStatus {

  val dependencies = "FileWrites" :: "ProcessWrites" :: Nil

  def beginService() = initialize()

  def endService() = ()

  def subscriptions = Set(
    Subscription(dependencyMap("FileWrites").get, _ => true),
    Subscription(dependencyMap("ProcessWrites").get, _ => true)
  )


//  val items = MutableMap.empty[Event,(Option[Subject], Option[FileObject])]


  def statusReport = Map(
    "total_received" -> totalReceived,
    "missingFiles_size" -> missingFiles.size,
    "missingProcesses_size" -> missingProcesses.size,
    "total_sent" -> totalSent
  )

  val missingFiles = MutableMap.empty[Event, Subject]
  val missingProcesses = MutableMap.empty[Event, FileObject]
  var totalReceived = 0
  var totalSent = 0

  def process = {
    case msg @ (f: FileObject, e: Event) =>
//      log.info(s"ProcessWritesFile got: $msg")
      totalReceived = totalReceived + 1
      missingFiles.get(e).fold(
        missingProcesses(e) = f
      ){ process =>
        totalSent = totalSent + 1
        broadCast((process, e, f))
      }

    case msg @ (s: Subject, e: Event) =>
//      log.info(s"ProcessWritesFile got: $msg")
      totalReceived = totalReceived + 1
      missingProcesses.get(e).fold(
        missingFiles(e) = s
      ){ file =>
        totalSent = totalSent + 1
        broadCast((s, e, file))
      }
  }
}
