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


  def statusReport = Map("processes_size" -> processes.size)

  private val processes = MutableMap.empty[UUID, Subject]  // File UUID -> Subject

  override def process = {
    case s: Subject if s.subjectType == SUBJECT_PROCESS =>
//      log.info(s"ProcessWrites got: $s")
      processes(s.uuid) = s
    case e: Event if e.eventType == EVENT_WRITE && processes.isDefinedAt(e.subject) =>      // TODO: this will silently throw away events that arrive before files.
//      log.info(s"ProcessWrites got: $e")
      broadCast(processes(e.subject) -> e)
//    case t: TimeMarker => broadCast(t)
  }
}

