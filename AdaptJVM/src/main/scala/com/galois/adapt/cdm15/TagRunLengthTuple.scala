package com.galois.adapt.cdm15

import java.util.UUID

import com.bbn.tc.schema.avro.cdm15
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class TagRunLengthTuple(
  numValueElements: Int,
  tagId: UUID
) extends CDM15 with DBWritable {
  def asDBKeyValues = List(
//    label, this.getClass.getSimpleName,// "TagRunLengthTuple",
    "numValueElements", numValueElements,
    "uuid", tagId
  )

  def asDBEdges = Nil

  def getUuid = tagId
}


case object TagRunLengthTuple extends CDM15Constructor[TagRunLengthTuple] {
  type RawCDMType = cdm15.TagRunLengthTuple

  def from(cdm: RawCDM15Type): Try[TagRunLengthTuple] = Try {
    TagRunLengthTuple(
      cdm.getNumValueElements,
      cdm.getTagId
    )
  }
}
