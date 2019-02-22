package com.galois.adapt.cdm20

import java.util.UUID

import com.bbn.tc.schema.avro.cdm20
import com.galois.adapt.{DBNodeable, DBWritable}

import scala.util.Try

// No change
case class TagRunLengthTuple(
  numValueElements: Int,
  tagId: UUID
) extends CDM20 with DBWritable with DBNodeable[CDM20.EdgeTypes.EdgeTypes] {
  lazy val getUuid: UUID = UUID.randomUUID()

  def asDBKeyValues: List[(String, Any)] = List(
    ("numValueElements", numValueElements),
    ("uuid", tagId)
  )

  // TODO CDM20 edges
  def asDBEdges = List((CDM20.EdgeTypes.tagId, tagId))
}


case object TagRunLengthTuple extends CDM20Constructor[TagRunLengthTuple] {
  type RawCDMType = cdm20.TagRunLengthTuple

  def from(cdm: RawCDM20Type): Try[TagRunLengthTuple] = Try {
    TagRunLengthTuple(
      cdm.getNumValueElements,
      cdm.getTagId
    )
  }
}
