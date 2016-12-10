package com.galois.adapt.cdm13

import java.util.UUID
import scala.util.Try


case class SrcSinkObject(
  uuid: UUID,
  baseObject: AbstractObject,
  srcSinkType: SrcSinkType
) extends CDM13

case object SrcSinkObject extends CDM13Constructor[SrcSinkObject] {
  type RawCDMType = com.bbn.tc.schema.avro.SrcSinkObject

  def from(cdm: RawCDM13Type): Try[SrcSinkObject] = Try(
    SrcSinkObject(
      cdm.getUuid,
      AbstractObject.from(new RawCDM13Type(cdm.getBaseObject)).get,
      cdm.getType
    )
  )
}