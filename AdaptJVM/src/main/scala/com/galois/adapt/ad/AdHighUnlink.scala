package com.galois.adapt.ad

import com.galois.adapt._
import com.galois.adapt.cdm13._

import java.util.UUID

import scala.collection._

import akka.actor._

// TODO: find a better criterion for dumping information than an EpochMarker
// TODO: private maps like Map[UUID, Subject] are begging for some better abstraction...

/*
 * Subject process or its tree performs relatively high number of UNLINK events 
 *
 * filter:        Subject with subjectType = Process;
 * features:      events with eventType UNLINK
 * scoring basis: count all UNLINK events attached to target
 */
class AdHighUnlink(root: ActorRef, threshold: Int) extends SubscriptionActor[CDM13,(Subject,Int)] {
  
  val subscriptions: immutable.Set[Subscription[CDM13]] = immutable.Set(Subscription(
    target = root,
    pack = {
      case s: Subject if s.subjectType equals SUBJECT_PROCESS => Some(s)
      case e: Event if e.eventType equals EVENT_UNLINK => Some(e)
      case s: SimpleEdge if s.edgeType equals EDGE_EVENT_ISGENERATEDBY_SUBJECT => Some(s)
      case EpochMarker => Some(EpochMarker)
      case _ => None
    }
  ))

  initialize()
 
  private val unlinks = mutable.Set.empty[UUID]              // UUIDs of UNLINK Events
  private val links = mutable.ListBuffer.empty[(UUID, UUID)] // Subject UUID -> Event UUID
  private val processes = mutable.Map.empty[UUID, Subject]   // Subject UUID -> Subject

  override def process(c: CDM13) = c match {
    case s: Subject => processes += (s.uuid -> s)
    case e: Event => unlinks += e.uuid
    case l: SimpleEdge => links += (l.toUuid -> l.fromUuid)
    case EpochMarker =>
      
      val counts = mutable.Map.empty[Subject, Int]

      // Tally up the counts of opens and checks per Subject
      for ((subj,event) <- links if processes isDefinedAt subj) {
        val subject: Subject = processes(subj)
        val c: Int = counts.getOrElse(subject, 0)
        if (unlinks contains event)
          counts(subject) = c+1
      }

      // Broadcast the subjects with high enough ratios
      counts.foreach { case (subject, count) =>
        if (count > threshold) broadCast(subject -> count)
      } 

      unlinks.clear()
      links.clear()
      processes.clear()
  }
}

object AdHighUnlink {
  def props(root: ActorRef, threshold: Int): Props = Props(new AdHighUnlink(root, threshold))
}

