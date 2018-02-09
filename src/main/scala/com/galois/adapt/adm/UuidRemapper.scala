package com.galois.adapt.adm

import java.io.{File, FileWriter}

import akka.actor.{Actor, ActorLogging, ActorRef}

import scala.annotation.tailrec
import scala.collection.mutable

// This object contains all of the types of messages the 'UuidRemapper' actor is ever expected to encounter/send.
object UuidRemapper {

  /* UuidRemapper recieves "PutCdm2Cdm" or "PutCdm2Adm" and returns "PutConfirm"
   *
   * The "PutConfirm" response is only sent after (1) the remap has been registered and (2) the actors that had been
   * waiting for this information have been notified.
   */
  case class PutCdm2Cdm(source: CdmUUID, target: CdmUUID)
  case class PutCdm2Adm(source: CdmUUID, target: AdmUUID)
  case object PutConfirm

  /* UuidRemapper recieves "GetCdm2Adm" returns "GetResult"
   *
   * The "ResultOfGetCdm2Adm" response is sent only once the 'UuidRemapper' has information about the source CDM UUID.
   * That means that the "ResultOfGetCdm2Adm" reponse may come a while after the initial "GetCdm2Adm" message.
   */
  case class GetCdm2Adm(source: CdmUUID)
  case class ResultOfGetCdm2Adm(target: AdmUUID)

  // TODO: do this nicely
  // For debugging: tell me about nodes that are still blocked
  case object GetStillBlocked

  // TODO: need finer grain control of this
  case object ExpireEverything
}

class UuidRemapper extends Actor with ActorLogging {

  import UuidRemapper._

  // We keep track of two large Maps containing UUID remap information
  private val cdm2cdm: mutable.Map[CdmUUID, CdmUUID] = mutable.Map.empty
  private val cdm2adm: mutable.Map[CdmUUID, AdmUUID]  = mutable.Map.empty

  // However, we also keep track of a Map of "CDM_UUID -> the Actors that want to know what that ID maps to"
  private val blocking: mutable.Map[CdmUUID, List[ActorRef]] = mutable.Map.empty

  // Apply as many CDM remaps as possible
  @tailrec
  private def advanceCdm(cdmUuid: CdmUUID): CdmUUID = {
    if (cdm2cdm contains cdmUuid)
      advanceCdm(cdm2cdm(cdmUuid))
    else
      cdmUuid
  }

  // Apply as many CDM remaps as possible, then try to apply an ADM remap. If that succeeds, notify all the 'interested'
  // actors. Otherwise, add the 'interested' actors back into the blocked map under the final CDM we had advanced to.
  private def advanceAndNotify(keyCdm: CdmUUID, interested: List[ActorRef]): Unit = {
    val advancedCdm: CdmUUID = advanceCdm(keyCdm)

    cdm2adm.get(advancedCdm) match {
      case None => blocking(advancedCdm) = interested ++ blocking.getOrElse(advancedCdm, Nil)
      case Some(adm) => interested.foreach(actor => actor ! ResultOfGetCdm2Adm(adm))
    }
  }


  override def receive: Receive = {

    // Put ADM information
    case PutCdm2Adm(source, target) =>
      cdm2adm(source) = target
      advanceAndNotify(source, blocking.remove(source).getOrElse(Nil))
      sender() ! PutConfirm

    // Put CDM information
    case PutCdm2Cdm(source, target) =>
      cdm2cdm(source) = target
      advanceAndNotify(target, blocking.remove(source).getOrElse(Nil))
      sender() ! PutConfirm

    // Retrieve information
    case GetCdm2Adm(keyCdm) =>
      advanceAndNotify(keyCdm, List(sender()))


    // Debug information
    case GetStillBlocked =>
      val blockedCsv = new FileWriter(new File("blocked.csv"))

      blockedCsv.write("blockCdm,blockingActors\n")
      for ((k,v) <- blocking) {
        blockedCsv.write(k.uuid.toString ++ "," ++ v.map(_.toString()).mkString(";") ++ "\n")
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
      for ((cdmUuid, waiting) <- blocking) {
        val synthesizedAdmUuid = AdmUUID(java.util.UUID.randomUUID())
        self ! PutCdm2Adm(cdmUuid, synthesizedAdmUuid)
      }

    case msg => log.error("UuidRemapper: received an unexpected message: {}", msg)
  }

}
