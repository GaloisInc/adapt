package com.galois.adapt.adm

import java.io.{File, FileWriter}

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.galois.adapt.Application.synActor

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.Future

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

class UuidRemapper(synActor: ActorRef) extends Actor with ActorLogging {

  import UuidRemapper._

  // We keep track of two large Maps containing UUID remap information
  private val cdm2cdm: mutable.Map[CdmUUID, CdmUUID] = mutable.Map.empty
  private val cdm2adm: mutable.Map[CdmUUID, AdmUUID]  = mutable.Map.empty

  // However, we also keep track of a Map of "CDM_UUID -> the Actors that want to know what that ID maps to"
  private val blocking: mutable.Map[CdmUUID, (List[ActorRef], Set[CdmUUID])] = mutable.Map.empty

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
  private def advanceAndNotify(keyCdm: CdmUUID, interested: List[ActorRef], prevOriginals: Set[CdmUUID]): Unit = {
    val (advancedCdm: CdmUUID, originals: Set[CdmUUID]) = advanceCdm(keyCdm, prevOriginals)

    cdm2adm.get(advancedCdm) match {
      case None =>
        val (prevInterested, prevOriginals1: Set[CdmUUID]) = blocking.getOrElse(advancedCdm, (Nil, Set.empty[CdmUUID]))
        blocking(advancedCdm) = (interested ++ prevInterested, originals | prevOriginals1 | Set(advancedCdm))
      case Some(adm) => interested.foreach(actor => actor ! ResultOfGetCdm2Adm(adm))
    }
  }


  override def receive: Receive = {

    // Put ADM information
    case PutCdm2Adm(source, target, _) =>
      cdm2adm(source) = target
      val (interested, original) = blocking.remove(source).getOrElse((Nil, Set.empty[CdmUUID]))
      advanceAndNotify(source, interested, original)
      sender() ! PutConfirm

    // Put CDM information
    case PutCdm2Cdm(source, target, _) =>
      cdm2cdm(source) = target
      val (interested, original) = blocking.remove(source).getOrElse((Nil, Set.empty[CdmUUID]))
      advanceAndNotify(source, interested, original)
      sender() ! PutConfirm

    // Retrieve information
    case GetCdm2Adm(keyCdm, _) =>
      advanceAndNotify(keyCdm, List(sender()), Set.empty)


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
      for ((k,v) <- cdm2cdm) {
        cdm2cdmCsv.write(k.uuid.toString ++ "," ++ v.toString() ++ "\n")
      }

      cdm2cdmCsv.close()

      val cdm2admCsv = new FileWriter(new File("cdm2admCsv.csv"))

      cdm2admCsv.write("remappedCdmUuid,admUuid\n")
      for ((k,v) <- cdm2adm) {
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

    case msg => log.error("UuidRemapper: received an unexpected message: {}", msg)
  }

}
