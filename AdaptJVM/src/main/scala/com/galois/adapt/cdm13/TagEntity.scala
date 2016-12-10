package com.galois.adapt.cdm13

import java.util.UUID

import scala.util.Try

case class TagEntity(
  uuid: UUID,
  tag: ProvenanceTagNode,
  timestampMicros: Option[Long] = None,
  properties: Option[Map[String,String]] = None
) extends CDM13

case object TagEntity extends CDM13Constructor[TagEntity] {
  type RawCDMType = com.bbn.tc.schema.avro.TagEntity

  def from(cdm: RawCDM13Type): Try[TagEntity] = Try(
    TagEntity(
      cdm.getUuid,
      cdm.getTag,
      AvroOpt.long(cdm.getTimestampMicros),
      AvroOpt.map(cdm.getProperties)
    )
  )
}