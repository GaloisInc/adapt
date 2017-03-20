package com.galois.adapt.cdm16

import java.util.UUID

import com.bbn.tc.schema.avro.cdm16
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class TagRunLengthTuple(
  numValueElements: Int,
  tagId: UUID
) extends CDM16 with DBWritable {
  def asDBKeyValues = List(
//    label, this.getClass.getSimpleName,// "TagRunLengthTuple",
    "numValueElements", numValueElements,
    "uuid", tagId
  )
}


case object TagRunLengthTuple extends CDM16Constructor[TagRunLengthTuple] {
  type RawCDMType = cdm16.TagRunLengthTuple

  def from(cdm: RawCDM15Type): Try[TagRunLengthTuple] = Try {
    TagRunLengthTuple(
      cdm.getNumValueElements,
      cdm.getTagId
    )
  }
}
