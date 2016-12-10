package com.galois.adapt.cdm13

import java.util.UUID

import scala.util.Try


case class FileObject(
  uuid: UUID,
  baseObject: AbstractObject,
  url: String,
  isPipe: Boolean = false,
  version: Int = 1,
  size: Option[Long] = None
) extends CDM13

case object FileObject extends CDM13Constructor[FileObject] {
  type RawCDMType = com.bbn.tc.schema.avro.FileObject

  def from(cdm: RawCDM13Type): Try[FileObject] = Try(
    FileObject(
      cdm.getUuid,
      AbstractObject.from(new RawCDM13Type(cdm.getBaseObject)).get,
      cdm.getUrl,
      cdm.getIsPipe,
      cdm.getVersion,
      AvroOpt.long(cdm.getSize)
    )
  )
}