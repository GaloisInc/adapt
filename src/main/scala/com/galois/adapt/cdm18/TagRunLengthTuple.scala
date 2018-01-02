package com.galois.adapt.cdm18

import java.util.UUID

import com.bbn.tc.schema.avro.cdm18
import com.galois.adapt.{DBNodeable, DBWritable}

import scala.util.Try

// No change
case class TagRunLengthTuple(
  numValueElements: Int,
  tagId: UUID
) extends CDM18 with DBWritable with DBNodeable {
  lazy val getUuid: UUID = UUID.randomUUID()

  def asDBKeyValues: List[(String, Any)] = List(
    ("numValueElements", numValueElements),
    ("uuid", tagId)
  )

  // TODO CDM18 edges
  def asDBEdges = Nil // List((CDM18.EdgeTypes.tagId, tagId))
}


case object TagRunLengthTuple extends CDM18Constructor[TagRunLengthTuple] {
  type RawCDMType = cdm18.TagRunLengthTuple

  def from(cdm: RawCDM18Type): Try[TagRunLengthTuple] = Try {
    TagRunLengthTuple(
      cdm.getNumValueElements,
      cdm.getTagId
    )
  }
}
