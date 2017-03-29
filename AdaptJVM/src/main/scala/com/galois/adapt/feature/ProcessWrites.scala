package com.galois.adapt.feature

import com.galois.adapt._
import com.galois.adapt.cdm16._
import java.util.UUID
import scala.collection.mutable.{Map => MutableMap}
import akka.actor._


/*
 * Finds all process subjects / file write event pairs
 */
class ProcessWrites(val registry: ActorRef)
  extends Actor with ActorLogging with ServiceClient with SubscriptionActor[(Subject,Event)] with ReportsStatus {
  
  val dependencies = "FileIngestActor" :: Nil
  lazy val subscriptions =
    Set[Subscription](Subscription(
      target = dependencyMap("FileIngestActor").get,
      interested = {
        case s: Subject if s.subjectType == SUBJECT_PROCESS  => true
        case e: Event if e.eventType == EVENT_WRITE => true
        case _ => false
      }
    ))

  def beginService() = initialize()
  def endService() = ()


  def statusReport = Map(
    "processes" -> processes.size,
    "unmatched writes" -> unmatchedWrites.values.map(_.size).sum,
    "total_received" -> totalReceived,
    "total_sent" -> totalSent
  )

  type ProcessUUID = UUID
  val processes = MutableMap.empty[ProcessUUID, Subject]
  val unmatchedWrites = MutableMap.empty[ProcessUUID, Set[Event]]
  var totalReceived = 0
  var totalSent = 0

  override def process = {
    case s: Subject if s.subjectType == SUBJECT_PROCESS =>
//      log.info(s"ProcessWrites got: $s")
      totalReceived = totalReceived + 1
      processes(s.uuid) = s
      unmatchedWrites.get(s.uuid).foreach { writes =>
        unmatchedWrites -= s.uuid
        writes.foreach { w =>
          totalSent = totalSent + 1
          broadCast(s -> w)
        }
      }

    case e: Event if e.eventType == EVENT_WRITE =>
//      log.info(s"ProcessWrites got: $e")
      totalReceived = totalReceived + 1
      processes.get(e.subject).fold(
        unmatchedWrites(e.subject) = unmatchedWrites.getOrElse(e.subject, Set.empty) + e
      ){ subject =>
        broadCast(subject -> e)
        totalSent = totalSent + 1
      }


//    case t: TimeMarker => broadCast(t)
  }
}

