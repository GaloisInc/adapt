package com.galois.adapt.cdm14

import java.util.UUID

import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class TagRunLengthTuple(
                    numValueElements: Int,
                    tagId: UUID
                  ) extends CDM14 with DBWritable {
  def asDBKeyValues = List(
    label, "TagRunLengthTuple",
    "numValueElements", numValueElements,
    "uuid", tagId
  )
}


case object TagRunLengthTuple extends CDM14Constructor[TagRunLengthTuple] {
  type RawCDMType = com.bbn.tc.schema.avro.TagRunLengthTuple

  def from(cdm: RawCDM14Type): Try[TagRunLengthTuple] = Try {
    TagRunLengthTuple(
      cdm.getNumValueElements,
      cdm.getTagId
    )
  }
}