package com.galois.adapt

import akka.actor.ActorRef
import akka.stream.scaladsl._
import com.galois.adapt.adm.{ADM, CdmUUID}

object FilterUUIDs {

  def filterUUIDs[T](uuids: List[CdmUUID], statusActor: ActorRef) = Flow[T].filterNot(
    _ match {
      case _ <: ADM => false//_.originalCdmUuids.map(x=>uuids.contains(x)).fold(false)(_||_)
      case _ <: CDM => true
      case _ => true
    })

}
