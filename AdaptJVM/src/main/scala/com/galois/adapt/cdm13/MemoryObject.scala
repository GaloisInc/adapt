package com.galois.adapt.cdm13

import java.util.UUID

import scala.util.Try

case class MemoryObject(
  uuid: UUID,
  baseObject: AbstractObject,
  memoryAddress: Long,
  pageNumber: Option[Long] = None
) extends CDM13

case object MemoryObject extends CDM13Constructor[MemoryObject] {
  type RawCDMType = com.bbn.tc.schema.avro.MemoryObject

  def from(cdm: RawCDM13Type): Try[MemoryObject] = Try(
    MemoryObject(
      cdm.getUuid,
      AbstractObject.from(new RawCDM13Type(cdm.getBaseObject)).get,
      cdm.getMemoryAddress,
      AvroOpt.long(cdm.getPageNumber)
    )
  )
}