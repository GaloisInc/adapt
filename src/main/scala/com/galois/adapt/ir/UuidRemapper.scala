package com.galois.adapt.ir

import java.io.{File, FileOutputStream, FileWriter, OutputStreamWriter}
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef}

import scala.collection.mutable

// The protocol of this actor is simple: it receives `CDM_UUID`s returns an `IR_UUID` when that
// information is available
class UuidRemapper extends Actor with ActorLogging {

  import UuidRemapper._


  // TODO: A CDM_UUID should be a key in no more than one of the maps below. That should be enforced in the types.
//  trait Destination
//  case class AnotherCdm(cdm: CDM_UUID) extends Destination
//  case class FinalIr(ir: IR_UUID) extends Destination
//  case class Waiting(waiting: List[ActorRef]) extends Destination
//  var cdmIterate: mutable.Map[CDM_UUID, Destination] = mutable.Map.empty

  // We keep track of two large Maps containing UUID remap information
  var cdm2cdm: mutable.Map[CdmUUID, CdmUUID] = mutable.Map.empty
  var cdm2ir:  mutable.Map[CdmUUID, IrUUID]  = mutable.Map.empty

  // However, we also keep track of a Map of "CDM_UUID -> the Actors that want to know what that ID maps to"
  var blocking: mutable.Map[CdmUUID, List[ActorRef]] = mutable.Map.empty

  // Apply as many CDM remaps as possible
  def advanceCdm(cdmUuid: CdmUUID): CdmUUID = {
    if (cdm2cdm contains cdmUuid)
      advanceCdm(cdm2cdm(cdmUuid))
    else
      cdmUuid
  }

  def removeFromBlocking(cdmUuid: CdmUUID): List[ActorRef] = {
    blocking.remove(cdmUuid).getOrElse(Nil)
  }

  def addToBlocking(cdmUuid: CdmUUID, blocked: List[ActorRef]): Unit = {
    blocking(cdmUuid) = blocked ++ blocking.getOrElse(cdmUuid, Nil)
  }

  def advanceAndNotify(keyCdm: CdmUUID, interested: List[ActorRef]): Unit = {
    // Apply as many CDM remaps as possible
    val advancedCdm: CdmUUID = advanceCdm(keyCdm)

    // Try to apply an IR remap
    cdm2ir.get(advancedCdm) match {
      case None => addToBlocking(advancedCdm, interested)
      case Some(ir) => interested.foreach(actor => actor ! ResultOfGetCdm2Ir(ir))
    }
  }


  override def receive: Receive = {

    // Put IR information
    case PutCdm2Ir(source, target) =>

      cdm2ir(source) = target

      advanceAndNotify(source, removeFromBlocking(source))

      sender() ! PutConfirm


    // Put CDM information
    case PutCdm2Cdm(source, target) =>
      cdm2cdm(source) = target

      advanceAndNotify(target, removeFromBlocking(source))

      sender() ! PutConfirm


    // Retrieve information
    case GetCdm2Ir(keyCdm) =>
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

      val cdm2irCsv = new FileWriter(new File("cdm2irCsv.csv"))

      cdm2irCsv.write("remappedCdmUuid,irUuid\n")
      for ((k,v) <- cdm2ir) {
        cdm2irCsv.write(k.uuid.toString ++ "," ++ v.toString() ++ "\n")
      }

      cdm2irCsv.close()

    //  sender() ! ()


    case msg => log.error("UuidRemapper: received an unexpected message: ", msg)
  }

}

object UuidRemapper {

  // UuidRemapper recieves "PutCdm2Cdm" or "PutCdm2Ir" and returns "PutConfirm"
  case class PutCdm2Cdm(source: CdmUUID, target: CdmUUID)
  case class PutCdm2Ir (source: CdmUUID, target: IrUUID)
  case object PutConfirm

  // UuidRemapper recieves "GetCdm2Ir" returns "GetResult"
  case class GetCdm2Ir(source: CdmUUID)
  case class ResultOfGetCdm2Ir(target: IrUUID)

  // For debugging: tell me about nodes that are still blocked
  case object GetStillBlocked
}