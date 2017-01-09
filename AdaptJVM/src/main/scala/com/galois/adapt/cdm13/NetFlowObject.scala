package com.galois.adapt.cdm13

import java.util.UUID

import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try

case class NetFlowObject(
  uuid: UUID,
  baseObject: AbstractObject,
  srcAddress: String,
  srcPort: Int,
  destAddress: String,
  destPort: Int,
  ipProtocol: Option[Int] = None
) extends CDM13 with DBWritable {
  def asDBKeyValues =
    baseObject.asDBKeyValues ++
    List(
      label, "NetFlowObject",
      "uuid", uuid,
      "srcAddress", srcAddress,
      "srcPort", srcPort,
      "destAddress", destAddress,
      "destPort", destPort
    ) ++
    ipProtocol.fold[List[Any]](List.empty)(v => List("ipProtocol", v))
}

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