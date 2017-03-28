package com.galois.adapt.feature

import com.galois.adapt._
import com.galois.adapt.cdm16._
import java.util.UUID
import scala.collection.mutable.{Map => MutableMap}
import akka.actor._


/*
 * Finds all file subjects / file write event pairs
 */
class FileWrites(val registry: ActorRef)
  extends Actor with ActorLogging with ServiceClient with SubscriptionActor[(FileObject,Event)] with ReportsStatus {
  
  val dependencies = "FileIngestActor" :: Nil
  lazy val subscriptions = Set(Subscription(
    target = dependencyMap("FileIngestActor").get,
    interested = {
      case s: FileObject => true
      case e: Event if e.eventType == EVENT_WRITE => true
      case _ => false
    }
  ))

  def beginService() = initialize()
  def endService() = ()

  def statusReport = Map("files_length" -> files.size)

  private val files = MutableMap.empty[UUID, FileObject]  // File UUID -> FileObject

  def process = {
    case f: FileObject =>
//      log.info(s"FileWrites is saving: $f")
      files(f.uuid) = f
    case e: Event if e.eventType == EVENT_WRITE =>
//      log.info(s"FileWrites got: $e")
      for (
        uuid <- e.predicateObject if files.isDefinedAt(uuid)   // TODO: this will silently throw away events that arrive before files.
      ) {
        log.info(s"FileWrites is sending: ${(files(uuid),e)}")
        broadCast((files(uuid),e))
      }
  }
}

