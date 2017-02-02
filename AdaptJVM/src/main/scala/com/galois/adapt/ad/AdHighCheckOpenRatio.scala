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
class AdHighCheckOpenRatio(root: ActorRef, threshold: Double) extends AdActor[CDM13,(Subject,Double)](
  immutable.Set(Subscription(
    target = root,
    pack = PartialFunction[CDM13, CDM13] {
      case s: Subject if s.subjectType equals SUBJECT_PROCESS => s
      case e: Event if Seq(EVENT_CHECK_FILE_ATTRIBUTES, EVENT_OPEN) contains e.eventType => e
      case s: SimpleEdge if s.edgeType equals EDGE_EVENT_ISGENERATEDBY_SUBJECT => s
      case EpochMarker => EpochMarker
    }
  ))
) {

  private val opens = mutable.Set.empty[UUID]                // UUIDs of OPEN Events
  private val checks = mutable.Set.empty[UUID]               // UUIDs of CHECK_FILE_ATTRIBUTES Events
  private val links = mutable.ListBuffer.empty[(UUID, UUID)] // Subject UUID -> Event UUID
  private val processes = mutable.Map.empty[UUID, Subject]   // Subject UUID -> Subject

  override def process(c: CDM13) = c match {
    case s: Subject => processes += (s.uuid -> s)
    case e: Event if e.eventType equals EVENT_CHECK_FILE_ATTRIBUTES => checks += e.uuid
    case e: Event if e.eventType equals EVENT_OPEN => opens += e.uuid
    case l: SimpleEdge => links += (l.toUuid -> l.fromUuid)
    case EpochMarker =>
      
      val counts = mutable.Map.empty[Subject, (Int, Int)]

      // Tally up the counts of opens and checks per Subject
      for ((subj,event) <- links if processes isDefinedAt subj) {
        val subject: Subject = processes(subj)
        val (o,c): (Int,Int) = counts.getOrElse(subject, (0,0))
        if (opens contains event)
          counts(subject) = (o+1, c)
        else if (checks contains event)
          counts(subject) = (o, c+1)
      }

      // Broadcast the subjects with high enough ratios
      counts.foreach { case (subject, (o, c)) =>
        val ratio = c.toDouble / o.toDouble
        if (ratio > threshold) broadCast(subject -> ratio)
      } 

      opens.clear()
      checks.clear()
      links.clear()
      processes.clear()
  }
}


