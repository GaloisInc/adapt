package com.galois.adapt.ad

import com.galois.adapt._
import com.galois.adapt.cdm13._

import java.util.UUID

import scala.collection._

import akka.actor._

// TODO: find a better criterion for dumping information than an EpochMarker
// TODO: private maps like Map[UUID, Subject] are begging for some better abstraction...

/*
 * Subject process has a high (relative) ratio of CHECK_FILE_ATTRIBUTE events to OPEN events
 *
 * filter:        Subject with subjectType = Process;
 * features:      events with eventType CHECK_FILE_ATTRIBUTES, events with eventType OPEN;
 * scoring basis: sum of CHECK_FILE_ATTRIBUTEs / sum of OPENs
 */
class AdHighCheckOpenRatio(root: ActorRef) extends  SubscriptionActor[CDM13,Map[Subject,(Int,Int,Int)]] { 
  
  val subscriptions: immutable.Set[Subscription[CDM13]] = immutable.Set(Subscription(
    target = root,
    pack = {
      case s @ Subject(u, SUBJECT_PROCESS, _, _, _, _, _, _, _, _, _, _, _)  => Some(s)
      case e @ Event(u, EVENT_OPEN, _, _, _, _, _, _, _, _, _, _) => Some(e)
      case e @ Event(u, EVENT_WRITE, _, _, _, _, _, _, _, _, _, _) => Some(e)
      case e @ Event(u, EVENT_CHECK_FILE_ATTRIBUTES, _, _, _, _, _, _, _, _, _, _) => Some(e)
      case s @ SimpleEdge(f, t, EDGE_EVENT_ISGENERATEDBY_SUBJECT, _, _) => Some(s)
      case EpochMarker => Some(EpochMarker)
      case _ => None
    }
  ))

  initialize()

  private val opens = mutable.Set.empty[UUID]                // UUIDs of OPEN Events
  private val writes = mutable.Set.empty[UUID]               // UUIDs of WRITE Events
  private val checks = mutable.Set.empty[UUID]               // UUIDs of CHECK_FILE_ATTRIBUTES Events
  private val links = mutable.ListBuffer.empty[(UUID, UUID)] // Subject UUID -> Event UUID
  private val processes = mutable.Map.empty[UUID, Subject]   // Subject UUID -> Subject

  override def process(c: CDM13) = c match {
    case s @ Subject(u, SUBJECT_PROCESS, _, _, _, _, _, _, _, _, _, _, _)  => processes += (s.uuid -> s)
    case e @ Event(u, EVENT_OPEN, _, _, _, _, _, _, _, _, _, _) => opens += e.uuid
    case e @ Event(u, EVENT_WRITE, _, _, _, _, _, _, _, _, _, _) => writes += e.uuid
    case e @ Event(u, EVENT_CHECK_FILE_ATTRIBUTES, _, _, _, _, _, _, _, _, _, _) => checks += e.uuid
    case s @ SimpleEdge(f, t, EDGE_EVENT_ISGENERATEDBY_SUBJECT, _, _) => links += (t -> f)
    case EpochMarker =>
      
      val counts = mutable.Map.empty[Subject, (Int, Int, Int)]

      // Tally up the counts of opens and checks per Subject
      for ((subj,event) <- links if processes isDefinedAt subj) {
        val subject: Subject = processes(subj)
        val (o,w,c): (Int,Int,Int) = counts.getOrElse(subject, (0,0,0))
        
        if      (opens  contains event) counts(subject) = (o+1, w,   c)
        else if (writes contains event) counts(subject) = (o,   w+1, c)
        else if (checks contains event) counts(subject) = (o,   w,   c+1)
      }

      // Broadcast the subjects with high enough ratios
      broadCast(counts)

      // Clear the stored state
      // TODO: Consider storing several generations of cache
      opens.clear()
      checks.clear()
      links.clear()
      processes.clear()

      println("EpochMarker: AdHighCheckOpenRatio")
    }
}

object AdHighCheckOpenRatio {
  def props(root: ActorRef): Props = Props(new AdHighCheckOpenRatio(root))
}

