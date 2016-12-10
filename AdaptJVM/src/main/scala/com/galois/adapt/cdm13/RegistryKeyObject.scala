package com.galois.adapt.cdm13

import java.util.UUID

import scala.util.Try


case class RegistryKeyObject(
  uuid: UUID,
  baseObject: AbstractObject,
  key: String,
  version: Int = 1,
  size: Option[Long] = None
) extends CDM13

case object RegistryKeyObject extends CDM13Constructor[RegistryKeyObject] {
  type RawCDMType = com.bbn.tc.schema.avro.RegistryKeyObject

  def from(cdm: RawCDM13Type): Try[RegistryKeyObject] = Try(
    RegistryKeyObject(
      cdm.uuid,
      AbstractObject.from(new RawCDM13Type(cdm.getBaseObject)).get,
      cdm.getKey,
      cdm.getVersion,
      AvroOpt.long(cdm.getSize)
    )
  )
}