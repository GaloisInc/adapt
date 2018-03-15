package com.galois.adapt

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.galois.adapt.adm.{ADM, AdmEvent, AdmSubject, AdmUUID, EdgeAdm2Adm}
import com.galois.adapt.cdm18.{EVENT_READ, EVENT_WRITE}

import scala.collection.mutable

object FSOX {

  type Time = Long
  case class Result(writer: AdmUUID, writeEvent: AdmUUID, obj: AdmUUID, readEvent: AdmUUID, reader: AdmUUID)

  /* We are looking for the following structure:
   *
   *   MATCH (s1: AdmSubject)<-[:subject]-(e_w: AdmEvent { eventType: 'EVENT_WRITE' })-[:predicateObject]->(o)
   *         (s2: AdmSubject)<-[:subject]-(e_r: AdmEvent { eventType: 'EVENT_READ'  })-[:predicateObject]->(o)
   *   WHERE e_w.earliestTimestampNanos < e_r.latestTimestampNanos
   *   RETURN s1.uuid, o.uuid, s2.uuid
   */

  def apply: Flow[Either[EdgeAdm2Adm, ADM], Result, NotUsed] = Flow[Either[EdgeAdm2Adm, ADM]]
    .statefulMapConcat[Result] { () =>

    val objectWrittenTo: mutable.Map[
      AdmUUID,             /* object written */
      mutable.Map[AdmUUID, /* who did the writing */ (Time /* earliest time written to */, AdmUUID /* proof of that write */)]
    ] = mutable.Map.empty
    val objectReadFrom:  mutable.Map[
      AdmUUID,             /* object read */
      mutable.Map[AdmUUID, /* who did the reading */ (Time /* latest time read */, AdmUUID /* proof of the read */)]
    ] = mutable.Map.empty

    // These maps should not get big: elements from these get removed as we find the subject/pred obj the event is supposed to associated with
    val writeEvents: mutable.Map[AdmUUID /* event uuid */, (Time /* earliest time written to */,
                                                            Option[AdmUUID] /* subject */,
                                                            Option[AdmUUID] /* predicate object */)] = mutable.Map.empty
    val readEvents:  mutable.Map[AdmUUID /* event uuid */, (Time /* latest time read from */ ,
                                                            Option[AdmUUID] /* subject */,
                                                            Option[AdmUUID] /* predicate object */)] = mutable.Map.empty

    def addObjWrite(obj: AdmUUID, tWrite: Time, writeEvent: AdmUUID, subj: AdmUUID): List[Result] = {
      val laterReads = objectReadFrom.getOrElse(obj, Nil).collect {
        case (s, (tRead, readEvent)) if tRead > tWrite && subj != s => (readEvent, s)
      }.toSet

      val submap: mutable.Map[AdmUUID, (Time, AdmUUID)] = objectWrittenTo.getOrElseUpdate(obj, mutable.Map.empty)
      if (tWrite < submap.get(obj).map(_._1).getOrElse(Long.MaxValue)) {
        submap(subj) = (tWrite, writeEvent)
      }

      laterReads
        .toList
        .map { case (readEvent, reader) => Result(subj, writeEvent, obj, readEvent, reader) }
    }

    def addObjRead(obj: AdmUUID, tRead: Time, readEvent: AdmUUID, subj: AdmUUID): List[Result] = {
      val earlierWrites = objectWrittenTo.getOrElse(obj, Nil).collect {
        case (s, (tWrite, writeEvent)) if tRead > tWrite && subj != s => (writeEvent, s)
      }.toSet

      val submap: mutable.Map[AdmUUID, (Time, AdmUUID)] = objectReadFrom.getOrElseUpdate(obj, mutable.Map.empty)
      if (tRead > submap.get(obj).map(_._1).getOrElse(Long.MinValue)) {
        submap(subj) = (tRead, readEvent)
      }

      earlierWrites
        .toList
        .map { case (writeEvent, writer) => Result(writer, writeEvent, obj, readEvent, subj) }
    }


    {
      case Right(e: AdmEvent) if e.eventType == EVENT_READ =>
        readEvents += (e.uuid -> (e.earliestTimestampNanos, None, None))
        Nil

      case Right(e: AdmEvent) if e.eventType == EVENT_WRITE =>
        writeEvents += (e.uuid -> (e.latestTimestampNanos, None, None))
        Nil

      case Left(EdgeAdm2Adm(src, "predicateObject", tgt)) =>

        var toRet: List[Result] = Nil

        for ((time, subjectOpt, None) <- writeEvents.remove(src)) {
          subjectOpt match {
            case None => writeEvents(src) = (time, None, Some(tgt))
            case Some(subject) => toRet ++= addObjWrite(tgt, time, src, subject)
          }
        }

        for ((time, subjectOpt, None) <- readEvents.remove(src)) {
          subjectOpt match {
            case None => readEvents(src) = (time, None, Some(tgt))
            case Some(subject) => toRet ++= addObjRead(tgt, time, src, subject)
          }
        }

        toRet


      case Left(EdgeAdm2Adm(src, "subject", tgt)) =>

        var toRet: List[Result] = Nil

        for ((time, None, objectObject) <- writeEvents.remove(src)) {
          objectObject match {
            case None => writeEvents(src) = (time, Some(tgt), None)
            case Some(object_) => toRet ++= addObjWrite(object_, time, src, tgt)
          }
        }

        for ((time, None, objectObject) <- readEvents.remove(src)) {
          objectObject match {
            case None => readEvents(src) = (time, Some(tgt), None)
            case Some(object_) => toRet ++= addObjRead(object_, time, src, tgt)
          }
        }

        toRet

      case _ => Nil
    }

  }

}
