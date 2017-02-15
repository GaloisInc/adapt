package com.galois.adapt.cdm14

import java.util.UUID

import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class SrcSinkObject(
                          uuid: UUID,
                          baseObject: AbstractObject,
                          srcSinkType: SrcSinkType
                        ) extends CDM14 with DBWritable {
  def asDBKeyValues = List(
    label, "SrcSinkObject",
    "uuid", uuid,
    "srcSinkType", srcSinkType
  ) ++ baseObject.asDBKeyValues
}

case object SrcSinkObject extends CDM14Constructor[SrcSinkObject] {
  type RawCDMType = com.bbn.tc.schema.avro.SrcSinkObject

  def from(cdm: RawCDM14Type): Try[SrcSinkObject] = Try(
    SrcSinkObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getType
    )
  )
}