package com.galois.adapt.cdm14

import java.util.UUID

import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try

case class NetFlowObject(
                          uuid: UUID,
                          baseObject: AbstractObject,
                          inboundAddress: String,
                          inboundPort: Int,
                          outboundAddress: String,
                          outboundPort: Int,
                          ipProtocol: Option[Int] = None
                        ) extends CDM14 with DBWritable {
  def asDBKeyValues =
    baseObject.asDBKeyValues ++
      List(
        label, "NetFlowObject",
        "uuid", uuid,
        "inboundAddress", inboundAddress,
        "inboundPort", inboundPort,
        "outboundAddress", outboundAddress,
        "outboundPort", outboundPort
      ) ++
      ipProtocol.fold[List[Any]](List.empty)(v => List("ipProtocol", v))
}

case object NetFlowObject extends CDM14Constructor[NetFlowObject] {
  type RawCDMType = com.bbn.tc.schema.avro.NetFlowObject

  def from(cdm: RawCDM14Type): Try[NetFlowObject] = Try(
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