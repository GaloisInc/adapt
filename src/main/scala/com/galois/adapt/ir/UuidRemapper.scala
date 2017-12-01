package com.galois.adapt.ir

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
  var cdm2cdm: mutable.Map[CDM_UUID, CDM_UUID] = mutable.Map.empty
  var cdm2ir:  mutable.Map[CDM_UUID, IR_UUID]  = mutable.Map.empty

  // However, we also keep track of a Map of "CDM_UUID -> the Actors that want to know what that ID maps to"
  var blocking: mutable.Map[CDM_UUID, List[ActorRef]] = mutable.Map.empty

  // Apply as many CDM remaps as possible
  def advanceCdm(cdmUuid: CDM_UUID): CDM_UUID = {
    if (cdm2cdm contains cdmUuid)
      advanceCdm(cdm2cdm(cdmUuid))
    else
      cdmUuid
  }

  def removeFromBlocking(cdmUuid: CDM_UUID): List[ActorRef] = {
    blocking.remove(cdmUuid).getOrElse(Nil)
  }

  def addToBlocking(cdmUuid: CDM_UUID, blocked: List[ActorRef]): Unit = {
    blocking(cdmUuid) = blocked ++ blocking.getOrElse(cdmUuid, Nil)
  }

  def advanceAndNotify(keyCdm: CDM_UUID, interested: List[ActorRef]): Unit = {
    // Apply as many CDM remaps as possible
    val advancedCdm: CDM_UUID = advanceCdm(keyCdm)

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

      advanceAndNotify(target, removeFromBlocking(source))

      sender() ! PutConfirm


    // Put CDM information
    case PutCdm2Cdm(source, target) =>
      cdm2cdm(source) = target

      advanceAndNotify(target, removeFromBlocking(source))

      sender() ! PutConfirm


    // Retrieve information
    case GetCdm2Ir(keyCdm) => advanceAndNotify(keyCdm, List(sender()))


    case msg => log.error("UuidRemapper: received an unexpected message: ", msg)
  }

}

object UuidRemapper {

  type CDM_UUID = UUID
  type IR_UUID = UUID

  // UuidRemapper recieves "PutCdm2Cdm" or "PutCdm2Ir" and returns "PutConfirm"
  case class PutCdm2Cdm(source: CDM_UUID, target: CDM_UUID)
  case class PutCdm2Ir (source: CDM_UUID, target: IR_UUID)
  case object PutConfirm

  // UuidRemapper recieves "GetCdm2Ir" returns "GetResult"
  case class GetCdm2Ir(source: CDM_UUID)
  case class ResultOfGetCdm2Ir(target: IR_UUID)
}