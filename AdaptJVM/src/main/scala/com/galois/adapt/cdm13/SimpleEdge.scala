package com.galois.adapt.cdm13

import java.util.UUID

import scala.util.Try


case class SimpleEdge(
  fromUuid: UUID,
  toUuid: UUID,
  edgeType: EdgeType,
  timestamp: Long,
  properties: Option[Map[String,String]] = None
) extends CDM13

case object SimpleEdge extends CDM13Constructor[SimpleEdge] {
  type RawCDMType = com.bbn.tc.schema.avro.cdm13.SimpleEdge

  def from(cdm: RawCDM13Type): Try[SimpleEdge] = Try(
    SimpleEdge(
      cdm.getFromUuid,
      cdm.getToUuid,
      cdm.getType,
      cdm.getTimestamp,
      AvroOpt.map(cdm.getProperties)
    )
  )
}
