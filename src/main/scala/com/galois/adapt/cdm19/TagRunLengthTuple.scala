package com.galois.adapt.cdm19

import java.util.UUID

import com.bbn.tc.schema.avro.cdm19
import com.galois.adapt.{DBNodeable, DBWritable}

import scala.util.Try

// No change
case class TagRunLengthTuple(
  numValueElements: Int,
  tagId: UUID
) extends CDM19 with DBWritable with DBNodeable[CDM19.EdgeTypes.EdgeTypes] {
  lazy val getUuid: UUID = UUID.randomUUID()

  def asDBKeyValues: List[(String, Any)] = List(
    ("numValueElements", numValueElements),
    ("uuid", tagId)
  )

  // TODO CDM19 edges
  def asDBEdges = List((CDM19.EdgeTypes.tagId, tagId))
}


case object TagRunLengthTuple extends CDM19Constructor[TagRunLengthTuple] {
  type RawCDMType = cdm19.TagRunLengthTuple

  def from(cdm: RawCDM19Type): Try[TagRunLengthTuple] = Try {
    TagRunLengthTuple(
      cdm.getNumValueElements,
      cdm.getTagId
    )
  }
}
