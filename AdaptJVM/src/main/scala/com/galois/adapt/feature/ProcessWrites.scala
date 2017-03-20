package com.galois.adapt.feature

import com.galois.adapt._
import com.galois.adapt.cdm16._

import java.util.UUID

import scala.collection.mutable.{Set => MutableSet, Map => MutableMap, ListBuffer}

import akka.actor._

/*
 * Finds all process subjects / file write event pairs
 */
class ProcessWrites(val registry: ActorRef)
  extends Actor with ActorLogging with ServiceClient with SubscriptionActor[(Set[UUID],String,Seq[Double])] { 
  
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

  private val processes = MutableMap.empty[UUID, Subject]  // File UUID -> Subject

  override def process = {
    case s: Subject if s.subjectType == SUBJECT_PROCESS => processes(s.uuid) = s
    case e: Event if e.eventType == EVENT_WRITE && processes.isDefinedAt(e.subject) =>
      val size: Long = e.size.getOrElse(0)
      broadCast((Set(e.uuid,processes(e.subject).uuid),"FileWrite",Seq(size.toDouble)))
  }
}

