package com.galois.adapt.cdm13
import scala.util.Try


case class AbstractObject(
  source: InstrumentationSource,
  permission: Option[FixedShort] = None,  // fixed size = 2
  lastTimestampMicros: Option[Long] = None,
  properties: Option[Map[String,String]] = None
) extends CDM13

case object AbstractObject extends CDM13Constructor[AbstractObject] {
  type RawCDMType = com.bbn.tc.schema.avro.AbstractObject

  def from(cdm: RawCDM13Type): Try[AbstractObject] = Try {
    AbstractObject(
      cdm.getSource,
      AvroOpt.fixedShort(cdm.getPermission),
      AvroOpt.long(cdm.getLastTimestampMicros),
      AvroOpt.map(cdm.getProperties)
    )
  }
}