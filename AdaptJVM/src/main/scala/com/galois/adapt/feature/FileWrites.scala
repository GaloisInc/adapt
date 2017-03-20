package com.galois.adapt.feature

import com.galois.adapt._
import com.galois.adapt.cdm16._

import java.util.UUID

import scala.collection.mutable.{Set => MutableSet, Map => MutableMap, ListBuffer}

import akka.actor._

/*
 * Finds all file subjects / file write event pairs
 */
class FileWrites(val registry: ActorRef)
  extends Actor with ActorLogging with ServiceClient with SubscriptionActor[(Float,FileObject,Event)] { 
  
  val dependencies = "FileIngestActor" :: Nil
  lazy val subscriptions =
    Set[Subscription](Subscription(
      target = dependencyMap("FileIngestActor").get,
      interested = {
        case s: FileObject => true
        case e: Event if e.eventType == EVENT_WRITE => true
        case _ => false
      }
    ))

  def beginService() = initialize()
  def endService() = ()

  private val files = MutableMap.empty[UUID, FileObject]  // File UUID -> FileObject

  override def process = {
    case f: FileObject => files(f.uuid) = f
    case e: Event if e.eventType == EVENT_WRITE =>
      for (uuid <- e.predicateObject if files.isDefinedAt(uuid))
        broadCast((scala.util.Random.nextFloat,files(uuid),e))
  }
}

