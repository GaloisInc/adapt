package com.galois.adapt.cdm15

import java.util.UUID

import com.bbn.tc.schema.avro.cdm15
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
                        ) extends CDM15 with DBWritable {
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

case object NetFlowObject extends CDM15Constructor[NetFlowObject] {
  type RawCDMType = cdm15.NetFlowObject

  def from(cdm: RawCDM15Type): Try[NetFlowObject] = Try(
    NetFlowObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getInboundAddress,
      cdm.getInboundPort,
      cdm.getOutboundAddress,
      cdm.getOutboundPort,
      AvroOpt.int(cdm.getIpProtocol)
    )
  )
}
