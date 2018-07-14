package com.galois.adapt.adm

import akka.NotUsed
import akka.event.LoggingAdapter
import akka.stream.scaladsl.Flow
import com.galois.adapt.MapSetUtils.AlmostMap
import com.galois.adapt.adm.EntityResolution.{Time, Timed}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object UuidRemapper {

  // This describes the type of input that the UUID remapping stage expects to get
  sealed trait UuidRemapperInfo
  case class AnAdm(adm: ADM) extends UuidRemapperInfo
  case class AnEdge(edge: Edge) extends UuidRemapperInfo
  case class CdmMerge(merged: CdmUUID, into: CdmUUID) extends UuidRemapperInfo
  case object JustTime extends UuidRemapperInfo


  type UuidRemapperFlow = Flow[Timed[UuidRemapperInfo], Either[ADM, EdgeAdm2Adm], NotUsed]

  def apply(
    expiryTime: Time,                      // How long to hold on to a CdmUUID until we expire it

    cdm2cdm: AlmostMap[CdmUUID, CdmUUID],  // Mapping for CDM uuids that have been mapped onto other CDM uuids
    cdm2adm: AlmostMap[CdmUUID, AdmUUID],  // Mapping for CDM uuids that have been mapped onto ADM uuids
    blockedEdges: mutable.Map[CdmUUID, (List[Edge], Set[CdmUUID])],

    ignoreEvents: Boolean,
    log: LoggingAdapter
  ): UuidRemapperFlow = Flow[Timed[UuidRemapperInfo]].statefulMapConcat[Either[ADM, EdgeAdm2Adm]] { () =>

    // Keep track of current time art the tip of the stream, along with things that will expire
    var currentTime: Time = Time(0,0)
    var expiryTimes: Fridge[CdmUUID] = Fridge.empty


    // Add some 'CDM -> ADM' to the maps and notify those previously blocked
    def putCdm2Adm(source: CdmUUID, target: AdmUUID): List[Either[ADM, EdgeAdm2Adm]] = {
      // Yell loudly if we are about to overwrite something

      val otherTarget = cdm2cdm.get(source)
      if (otherTarget.isDefined)
        log.warning( s"UuidRemapper: $source should not map to $target (since it already maps to $otherTarget")

      val prevTarget = cdm2adm.update(source, target)
      if (prevTarget.exists(_ != target))
        log.warning(s"UuidRemapper: $source should not map to $target (since it already maps to $prevTarget")

      advanceAndNotify(source, blockedEdges.remove(source).getOrElse((Nil, Set.empty[CdmUUID])))
    }

    // Add some 'CDM -> CDM' to the maps and notify those previously blocked
    def putCdm2Cdm(source: CdmUUID, target: CdmUUID): List[Either[ADM, EdgeAdm2Adm]] = {
      // Yell loudly if we are about to overwrite something

      val otherTarget = cdm2adm.get(source)
      if (otherTarget.isDefined)
        log.warning( s"UuidRemapper: $source should not map to $target (since it already maps to $otherTarget")

      val prevTarget = cdm2cdm.update(source, target)
      if (prevTarget.exists(_ != target))
        log.warning(s"UuidRemapper: $source cannot map to $target (since it already maps to $prevTarget")

      advanceAndNotify(source, blockedEdges.remove(source).getOrElse((Nil, Set.empty[CdmUUID])))
    }

    def expireKey(cdmUuid: CdmUUID, cause: String, expireInto: ListBuffer[Either[ADM, EdgeAdm2Adm]]): Unit = {
      for ((edges, originalCdmUuids) <- blockedEdges.get(cdmUuid).toList) {
        val originalCdms = originalCdmUuids.toList
        val synthesizedAdm = AdmSynthesized(originalCdms)

        println(s"Expired ${synthesizedAdm.uuid} ($cause)")

        expireInto += Left(synthesizedAdm)
        for (originalCdm <- originalCdms) {
          expireInto ++= putCdm2Adm(cdmUuid, synthesizedAdm.uuid)
        }
      }
    }

    // Expire old UUIDs based on the current time. Note all this does is create a new node and call out to `putCdm2Adm`.
    def updateTimeAndExpireOldUuids(time: Time, expireInto: ListBuffer[Either[ADM, EdgeAdm2Adm]]): Unit = {
      var toReturn: mutable.ListBuffer[Either[ADM, EdgeAdm2Adm]] = ListBuffer.empty

      if (time.nanos > currentTime.nanos) {
        // Unfortunately time information observed by UuidRemapper can be jittery since it is interacting with several
        // "tip of the streams".
        currentTime = currentTime.copy(nanos = time.nanos)

        // Expire based on nanosecond timestamps
        var keepExpiringNanos: Boolean = true
        while (keepExpiringNanos) {
          expiryTimes.popFirstNanosToExpireIf(_ <= currentTime.nanos) match {
            case None => keepExpiringNanos = false
            case Some((keysToExpire, _)) => keysToExpire.foreach(k => expireKey(k, "time based", toReturn))
          }
        }
      }

      if (time.count > currentTime.count) {
        // Unfortunately time information observed by UuidRemapper can be jittery since it is interacting with several
        // "tip of the streams".
        currentTime = currentTime.copy(count = time.count)


        // Expire based on node counts
        var keepExpiringCounts: Boolean = true
        while (keepExpiringCounts) {
          expiryTimes.popFirstCountToExpireIf(_ <= currentTime.count) match {
            case None => keepExpiringCounts = false
            case Some((keysToExpire, _)) => keysToExpire.foreach(k => expireKey(k, "count based", toReturn))
          }
        }
      }

      toReturn.toList
    }

    // Apply as many CDM remaps as possible
    @tailrec
    def advanceCdm(cdmUuid: CdmUUID, visited: Set[CdmUUID]): (CdmUUID, Set[CdmUUID]) = {  // This makes recursive calls to the map (maybe on disk)
    if (cdm2cdm contains cdmUuid)
      advanceCdm(cdm2cdm(cdmUuid), visited + cdmUuid)
    else
      (cdmUuid, visited)
    }

    // Apply as many CDM remaps as possible, then try to apply an ADM remap. If that succeeds, update all of the
    // dependent/blocked edges. Otherwise, add those edges back into the blocked map under the final CDM we had
    // advanced to.
    def advanceAndNotify(keyCdm: CdmUUID, previous: (List[Edge], Set[CdmUUID])): List[Either[ADM, EdgeAdm2Adm]] = {

      // TODO: (lower priority) Reconsider how to make this more performent in cases of data arriving wildly out of order very often.

      val (dependent: List[Edge], prevOriginals: Set[CdmUUID]) = previous
      val (advancedCdm: CdmUUID, originals: Set[CdmUUID]) = advanceCdm(keyCdm, prevOriginals)

      cdm2adm.get(advancedCdm) match {
        case None =>
          val (prevBlocked, prevOriginals1: Set[CdmUUID]) = blockedEdges.getOrElse(advancedCdm, (Nil, Set.empty[CdmUUID]))
          blockedEdges(advancedCdm) = (dependent ++ prevBlocked, originals | prevOriginals1 + advancedCdm)

          // Set an expiry time, but only if there isn't one already
          if (!(expiryTimes.keySet contains advancedCdm)) {
            expiryTimes.updateExpiryTime(advancedCdm, currentTime.plus(expiryTime))
          }

          List.empty

        case Some(adm) => dependent.flatMap(e => addEdge(e.applyRemap(Seq(keyCdm), adm)))
      }
    }

    // Advance an edge as far as possible
    def addEdge(edge: Edge): List[Either[ADM, EdgeAdm2Adm]] = edge match {
      case e: EdgeAdm2Adm => List(Right(e))
      case e @ EdgeAdm2Cdm(src, lbl, tgt) => advanceAndNotify(tgt, (List(e), Set(tgt)))
      case e @ EdgeCdm2Adm(src, lbl, tgt) => advanceAndNotify(src, (List(e), Set(src)))
      case e @ EdgeCdm2Cdm(src, lbl, tgt) => advanceAndNotify(src, (List(e), Set(src)))
    }


    {
      // Ignore events if `ignoreEvents`
      case Timed(_, AnAdm(e: AdmEvent)) if ignoreEvents => Nil

      // Given an ADM node, map all the original CDM UUIDs to an ADM UUID
      case Timed(t, AnAdm(adm)) =>
        val toReturn: ListBuffer[Either[ADM, EdgeAdm2Adm]] = ListBuffer.empty

        toReturn += Left(adm)
        for (cdmUuid <- adm.originalCdmUuids) {
          if (!cdm2cdm.contains(cdmUuid))
            toReturn ++= putCdm2Adm(cdmUuid, adm.uuid)
        }
        updateTimeAndExpireOldUuids(t, toReturn)

        toReturn.toList

      // Add an edge
      case Timed(t, AnEdge(edge)) =>
        val toReturn: ListBuffer[Either[ADM, EdgeAdm2Adm]] = ListBuffer.empty

        toReturn ++= addEdge(edge)
        updateTimeAndExpireOldUuids(t, toReturn)

        toReturn.toList

      // Add information about a CDM to CDM mapping
      case Timed(t, CdmMerge(merged, into)) =>
        val toReturn: ListBuffer[Either[ADM, EdgeAdm2Adm]] = ListBuffer.empty

        toReturn ++= putCdm2Cdm(merged, into)
        updateTimeAndExpireOldUuids(t, toReturn)

        toReturn.toList

      // Update just the time
      case Timed(t, JustTime) =>
        val toReturn: ListBuffer[Either[ADM, EdgeAdm2Adm]] = ListBuffer.empty

        if (t == Time.max) { println("UUID remapping stage is emitting all its state...") }
        updateTimeAndExpireOldUuids(t, toReturn)

        toReturn.toList
    }
  }
}
