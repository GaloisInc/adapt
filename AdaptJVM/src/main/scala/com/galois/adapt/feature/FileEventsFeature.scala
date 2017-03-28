package com.galois.adapt.feature

import com.galois.adapt._
import com.galois.adapt.cdm16._

import java.util.UUID

import scala.collection.mutable.{Set => MutableSet, Map => MutableMap, ListBuffer}

import akka.actor._

// TODO CDM15

/*
 * Feature extractor that gets file related information on a per process basis
 *
 * filter:        Subject with subjectType = Process;
 * features:      events with eventType CHECK_FILE_ATTRIBUTES, OPEN, and WRITE;
 */
class FileEventsFeature(val registry: ActorRef, root: ActorRef)
  extends Actor with ActorLogging with ServiceClient with SubscriptionActor[Map[Subject,(Int,Int,Int)]] with ReportsStatus {
  
  val subscriptions: Set[Subscription] = Set(Subscription(
    target = root,
    interested = {
      /*
      case Subject(_, SUBJECT_PROCESS, _, _, _, _, _, _, _, _, _, _, _) => true
      case Event(_, EVENT_OPEN, _, _, _, _, _, _, _, _, _, _) => true
      case Event(_, EVENT_WRITE, _, _, _, _, _, _, _, _, _, _) => true
      case Event(_, EVENT_CHECK_FILE_ATTRIBUTES, _, _, _, _, _, _, _, _, _, _) => true
      case SimpleEdge(_, _, EDGE_EVENT_ISGENERATEDBY_SUBJECT, _, _) => true
      case EpochMarker => true
      */
      case _ => false
    }
  ))

  val dependencies = List.empty
  def beginService() = initialize()
  def endService() = ()

  def statusReport = Map(
    "opens_size" -> opens.size,
    "writes_size" -> writes.size,
    "checks_size" -> checks.size,
    "links_size" -> links.size,
    "processes_size" -> processes.size
  )

  private val opens = MutableSet.empty[UUID]               // UUIDs of OPEN Events
  private val writes = MutableSet.empty[UUID]              // UUIDs of WRITE Events
  private val checks = MutableSet.empty[UUID]              // UUIDs of CHECK_FILE_ATTRIBUTES Events
  private val links = ListBuffer.empty[(UUID, UUID)]       // Subject UUID -> Event UUID
  private val processes = MutableMap.empty[UUID, Subject]  // Subject UUID -> Subject

  override def process = {
    /*
    case s @ Subject(u, SUBJECT_PROCESS, _, _, _, _, _, _, _, _, _, _, _)  => processes += (s.uuid -> s)
    case e @ Event(u, EVENT_OPEN, _, _, _, _, _, _, _, _, _, _) => opens += e.uuid
    case e @ Event(u, EVENT_WRITE, _, _, _, _, _, _, _, _, _, _) => writes += e.uuid
    case e @ Event(u, EVENT_CHECK_FILE_ATTRIBUTES, _, _, _, _, _, _, _, _, _, _) => checks += e.uuid
    case s @ SimpleEdge(f, t, EDGE_EVENT_ISGENERATEDBY_SUBJECT, _, _) => links += (t -> f)
    */
    case EpochMarker =>
      
      val counts = MutableMap.empty[Subject, (Int, Int, Int)]

      // Tally up the counts of opens and checks per Subject
      for ((subj,event) <- links if processes isDefinedAt subj) {
        val subject: Subject = processes(subj)
        val (o,w,c): (Int,Int,Int) = counts.getOrElse(subject, (0,0,0))
        
        if      (opens  contains event) counts(subject) = (o+1, w,   c)
        else if (writes contains event) counts(subject) = (o,   w+1, c)
        else if (checks contains event) counts(subject) = (o,   w,   c+1)
      }

      // Broadcast the subjects with high enough ratios
      broadCast(counts.toMap)

      // Clear the stored state
      // TODO: Consider storing several generations of cache
      opens.clear()
      checks.clear()
      links.clear()
      processes.clear()

      println("EpochMarker: AdHighCheckOpenRatio")
    }
}

object FileEventsFeature {
  def props(registry: ActorRef, root: ActorRef): Props = Props(new FileEventsFeature(registry, root))
}

