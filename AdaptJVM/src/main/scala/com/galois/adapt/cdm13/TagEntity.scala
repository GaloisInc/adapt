package com.galois.adapt.cdm13

import java.util.UUID

import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try

case class TagEntity(
  uuid: UUID,
  tag: ProvenanceTagNode,
  timestampMicros: Option[Long] = None,
  properties: Option[Map[String,String]] = None
) extends CDM13 with DBWritable {
  def asDBKeyValues = List(
    label, "TagEntity",
    "uuid", uuid,
    "tag", tag
  ) ++
  timestampMicros.fold[List[Any]](List.empty)(v => List("timestampMicros", v)) ++
  DBOpt.fromKeyValMap(properties)
}

case object TagEntity extends CDM13Constructor[TagEntity] {
  type RawCDMType = com.bbn.tc.schema.avro.cdm13.TagEntity

  def from(cdm: RawCDM13Type): Try[TagEntity] = Try(
    TagEntity(
      cdm.getUuid,
      cdm.getTag,
      AvroOpt.long(cdm.getTimestampMicros),
      AvroOpt.map(cdm.getProperties)
    )
  )
}
