package com.galois.adapt.adm

import java.io.{File, FileWriter}
import java.util.UUID
import java.util.function.BiConsumer

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.ask
import com.galois.adapt.MapDBUtils.AlmostMap
import org.mapdb.HTreeMap

import scala.annotation.tailrec
import scala.collection.mutable

// This object contains all of the types of messages the 'UuidRemapper' actor is ever expected to encounter/send.
object UuidRemapper {

  /* UuidRemapper recieves "PutCdm2Cdm" or "PutCdm2Adm" and returns "PutConfirm"
   *
   * The "PutConfirm" response is only sent after (1) the remap has been registered and (2) the actors that had been
   * waiting for this information have been notified.
   */
  case class PutCdm2Cdm(source: CdmUUID, target: CdmUUID, time: Option[Long] = None)
  case class PutCdm2Adm(source: CdmUUID, target: AdmUUID, time: Option[Long] = None)
  case object PutConfirm

  /* UuidRemapper recieves "GetCdm2Adm" returns "GetResult"
   *
   * The "ResultOfGetCdm2Adm" response is sent only once the 'UuidRemapper' has information about the source CDM UUID.
   * That means that the "ResultOfGetCdm2Adm" reponse may come a while after the initial "GetCdm2Adm" message.
   */
  case class GetCdm2Adm(source: CdmUUID, time: Option[Long] = None)
  case class ResultOfGetCdm2Adm(target: AdmUUID)

  // TODO: do this nicely
  // For debugging: tell me about nodes that are still blocked
  case object GetStillBlocked

  // TODO: need finer grain control of this
  case object ExpireEverything
}

class UuidRemapper(
    synActor: ActorRef,                     // Actor to whom synthesized nodes should be sent
    expiryTime: Long,                       // How long to hold on to a CdmUUID (waiting for a remap) until we expire it
    cdm2cdmMap: AlmostMap[CdmUUID,CdmUUID], // Mapping for CDM uuids that have been mapped onto other CDM uuids
    cdm2admMap: AlmostMap[CdmUUID,AdmUUID]  // Mapping for CDM uuids that have been mapped onto ADM uuids
  ) extends Actor with ActorLogging {

  import UuidRemapper._

  // We keep track of two large Maps containing UUID remap information. For performance/memory reasons, we use MapDB.
  // We are wrapping the MapDB maps with Scala-like methods you'd find on something like:
  //
  //     val cdm2cdm: mutable.Map[CdmUUID, CdmUUID] = mutable.Map.empty
  //     val cdm2adm: mutable.Map[CdmUUID, AdmUUID] = mutable.Map.empty
  //
  private val cdm2cdm = cdm2cdmMap
  private val cdm2adm = cdm2admMap

  // However, we also keep track of a Map of "CDM_UUID -> the Actors that want to know what that ID maps to"
  private val blocking: mutable.Map[CdmUUID, (List[ActorRef], Set[CdmUUID])] = mutable.Map.empty

  // Keep track of current time art the tip of the stream, along with things that will expire
  var currentTime: Long = 0
  var expiryTimes: Fridge[CdmUUID] = Fridge.empty

  // Expire old UUIDs based on the current time
  private def updateTimeAndExpireOldUuids(time: Long): Unit = {

    // Unfortunately time information observed by UuidRemapper can be jittery since it is interacting with several "tip
    // of the streams".
    if (time <= currentTime) return
    currentTime = time

    while (expiryTimes.peekFirstToExpire.exists { case (_,t) => t <= currentTime }) {
      val (keysToExpire, _) = expiryTimes.popFirstToExpire().get

      for (cdmUuid <- keysToExpire) {
        for ((interested, originalCdmUuids) <- blocking.get(cdmUuid)) {
          val synthesizedAdm = AdmSynthesized(originalCdmUuids.toList) // TODO: track CdmUuids remapping to this, then make this deterministic
          synActor ! synthesizedAdm
          self ! PutCdm2Adm(cdmUuid, synthesizedAdm.uuid)
          println(s"Expired ${synthesizedAdm.uuid}")
        }
      }
    }
  }

  // Apply as many CDM remaps as possible
  @tailrec
  private def advanceCdm(cdmUuid: CdmUUID, visited: Set[CdmUUID]): (CdmUUID, Set[CdmUUID]) = {
    if (cdm2cdm contains cdmUuid)
      advanceCdm(cdm2cdm(cdmUuid), visited | Set(cdmUuid))
    else
      (cdmUuid, visited)
  }

  // Apply as many CDM remaps as possible, then try to apply an ADM remap. If that succeeds, notify all the 'interested'
  // actors. Otherwise, add the 'interested' actors back into the blocked map under the final CDM we had advanced to.
  private def advanceAndNotify(keyCdm: CdmUUID, previous: (List[ActorRef], Set[CdmUUID])): Unit = {
    val (interested: List[ActorRef], prevOriginals: Set[CdmUUID]) = previous
    val (advancedCdm: CdmUUID, originals: Set[CdmUUID]) = advanceCdm(keyCdm, prevOriginals)

    cdm2adm.get(advancedCdm) match {
      case None =>
        val (prevInterested, prevOriginals1: Set[CdmUUID]) = blocking.getOrElse(advancedCdm, (Nil, Set.empty[CdmUUID]))
        blocking(advancedCdm) = (interested ++ prevInterested, originals | prevOriginals1 | Set(advancedCdm))

        // Set an expiry time, but only if there isn't one already
        if (!(expiryTimes.keySet contains advancedCdm)) {
          expiryTimes.updateExpiryTime(advancedCdm, currentTime + expiryTime)
        }
      case Some(adm) => interested.foreach(actor => actor ! ResultOfGetCdm2Adm(adm))
    }
  }


  override def receive: Receive = {

    // Put ADM information
    case PutCdm2Adm(source, target, time) =>
      time.foreach(updateTimeAndExpireOldUuids)

      // Yell loudly if we are about to overwrite something
      assert(!(cdm2adm contains source) || cdm2adm(source) == target,
        s"UuidRemapper: $source cannot map to $target (since it already maps to ${cdm2adm(source)}"
      )
      assert(!(cdm2cdm contains source),
        s"UuidRemapper: $source cannot map to $target (since it already maps to ${cdm2cdm(source)}"
      )

      cdm2adm(source) = target
      advanceAndNotify(source, blocking.remove(source).getOrElse((Nil, Set.empty[CdmUUID])))
      sender() ! PutConfirm

    // Put CDM information
    case PutCdm2Cdm(source, target, time) =>
      time.foreach(updateTimeAndExpireOldUuids)

      // Yell loudly if we are about to overwrite something
      assert(!(cdm2adm contains source),
        s"UuidRemapper: $source cannot map to $target (since it already maps to ${cdm2adm(source)}"
      )
      assert(!(cdm2cdm contains source) || cdm2cdm(source) == target,
        s"UuidRemapper: $source cannot map to $target (since it already maps to ${cdm2cdm(source)}"
      )

      cdm2cdm(source) = target
      advanceAndNotify(source, blocking.remove(source).getOrElse((Nil, Set.empty[CdmUUID])))
      sender() ! PutConfirm

    // Retrieve information
    case GetCdm2Adm(keyCdm, time) =>
      time.foreach(updateTimeAndExpireOldUuids)

      advanceAndNotify(keyCdm, (List(sender()), Set.empty))


    // Debug information
    case GetStillBlocked =>
      val blockedCsv = new FileWriter(new File("blocked.csv"))

      blockedCsv.write("blockCdm,blockingActors\n")
      for ((k,v) <- blocking) {
     //   blockedCsv.write(k.uuid.toString ++ "," ++ v.map(_.toString()).mkString(";") ++ "\n")
      }

      blockedCsv.close()

      val cdm2cdmCsv = new FileWriter(new File("cdm2cdmCsv.csv"))

      cdm2cdmCsv.write("remappedCdmUuid,cdmUuid\n")
      cdm2cdm.foreach { case (k,v) =>
        cdm2cdmCsv.write(k.uuid.toString ++ "," ++ v.toString() ++ "\n")
      }

      cdm2cdmCsv.close()

      val cdm2admCsv = new FileWriter(new File("cdm2admCsv.csv"))

      cdm2admCsv.write("remappedCdmUuid,admUuid\n")
      cdm2adm.foreach { case (k,v) =>
        cdm2admCsv.write(k.uuid.toString ++ "," ++ v.toString() ++ "\n")
      }

      cdm2admCsv.close()

    //  sender() ! ()


    // TODO: do this better
    case ExpireEverything =>

      // Just send ourself messages with synthesized UUIDs
      for ((cdmUuid, (waiting, originalCdmUuids)) <- blocking) {
        val synthesizedAdm = AdmSynthesized(originalCdmUuids.toList) // TODO: track CdmUuids remapping to this, then make this deterministic
        synActor ! synthesizedAdm
        self ! PutCdm2Adm(cdmUuid, synthesizedAdm.uuid)
        println(s"Expired ${synthesizedAdm.uuid}")
      }
      synActor ! akka.actor.Status.Success(()) // Terminate synthesizing stream

    case PutConfirm => ;

    case msg => log.error("UuidRemapper: received an unexpected message: {}", msg)
  }

}
