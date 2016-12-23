package com.galois.adapt.cdm13

import java.util.UUID

import scala.util.Try

case class NetFlowObject(
  uuid: UUID,
  baseObject: AbstractObject,
  srcAddress: String,
  srcPort: Int,
  destAddress: String,
  destPort: Int,
  ipProtocol: Option[Int] = None
) extends CDM13

case object NetFlowObject extends CDM13Constructor[NetFlowObject] {
  type RawCDMType = com.bbn.tc.schema.avro.NetFlowObject

  def from(cdm: RawCDM13Type): Try[NetFlowObject] = Try(
    NetFlowObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getSrcAddress,
      cdm.getSrcPort,
      cdm.getDestAddress,
      cdm.getDestPort,
      AvroOpt.int(cdm.getIpProtocol)
    )
  )
}