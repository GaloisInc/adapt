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

  def statusReport = Map(
    "total_received" -> (filesReceived + writesReceived),
    "writes_received" -> writesReceived,
    "files_received" -> filesReceived,
    "files_collected" -> files.size,
    "unmatched_writes" -> unmatchedWrites.values.map(_.size).sum,
    "total_sent" -> totalSent
  )

  type FileUUID = UUID

  val files = MutableMap.empty[FileUUID, FileObject]
  val unmatchedWrites = MutableMap.empty[FileUUID, Set[Event]]
  var filesReceived = 0
  var writesReceived = 0
  var totalSent = 0

  def process = {
    case f: FileObject =>
//      log.info(s"FileWrites is saving: $f")
      filesReceived += 1
      files(f.uuid) = f
      unmatchedWrites.get(f.uuid).foreach { writes =>
        unmatchedWrites -= f.uuid
        writes.foreach { w =>
          broadCast(f -> w)
          totalSent += 1
        }
      }

    case e: Event if e.eventType == EVENT_WRITE =>
//      log.info(s"FileWrites got: $e")
      writesReceived += 1
      val fileUuids = List(e.predicateObject, e.predicateObject2).flatten
      fileUuids foreach { fu =>
        files.get(fu).fold {
          unmatchedWrites(fu) = unmatchedWrites.getOrElse(fu, Set.empty[Event]) + e
        } { file =>
          broadCast(file -> e)
          totalSent += 1
        }
      }
  }
}

