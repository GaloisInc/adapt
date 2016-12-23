package com.galois.adapt.scepter

import akka.actor.{Actor, ActorRef}
import com.galois.adapt.cdm13._

class EventCountingTestActor(dbActor: ActorRef) extends Actor {
  var typeCounter = Map.empty[String,Int]

  private def incrementTypeCount(name: String): Unit = {
    typeCounter = typeCounter.updated(name, typeCounter.getOrElse(name, 0) + 1)
  }

  def receive = {
    case _: AbstractObject => incrementTypeCount("AbstractObject")
    case cdm: Event =>
      incrementTypeCount("Event")
      dbActor ! cdm
    case cdm: FileObject =>
      incrementTypeCount("FileObject")
      dbActor ! cdm
    case _: MemoryObject => incrementTypeCount("MemoryObject")
    case _: NetFlowObject => incrementTypeCount("NetFlowObject")
    case _: Principal => incrementTypeCount("Principal")
    case _: ProvenanceTagNode => incrementTypeCount("ProvenanceTagNode")
    case _: RegistryKeyObject => incrementTypeCount("RegistryKeyObject")
    case _: SimpleEdge => incrementTypeCount("SimpleEdge")
    case _: SrcSinkObject => incrementTypeCount("SrcSinkObject")
    case cdm: Subject =>
      incrementTypeCount("Subject")
      dbActor ! cdm
    case _: TagEntity => incrementTypeCount("TagEntity")
    case _: Value => incrementTypeCount("Value")
    case HowMany(name) =>
      sender() ! (name match {
        case "total" => typeCounter.values.sum
        case "each" => typeCounter
        case _ => typeCounter.getOrElse(name,0)
      })
  }
}
