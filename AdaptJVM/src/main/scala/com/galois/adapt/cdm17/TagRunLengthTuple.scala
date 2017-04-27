package com.galois.adapt.cdm17

import java.util.UUID

import com.bbn.tc.schema.avro.cdm17
import com.galois.adapt.{DBNodeable, DBWritable}
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class TagRunLengthTuple(
  numValueElements: Int,
  tagId: UUID
) extends CDM17 with DBWritable with DBNodeable {
  lazy val getUuid = UUID.randomUUID()

  def asDBKeyValues = List(
    label, "TagRunLengthTuple",
    "numValueElements", numValueElements,
    "uuid", tagId
  )

  def asDBEdges = List(("tagId", tagId))
}


case object TagRunLengthTuple extends CDM17Constructor[TagRunLengthTuple] {
  type RawCDMType = cdm17.TagRunLengthTuple

  def from(cdm: RawCDM15Type): Try[TagRunLengthTuple] = Try {
    TagRunLengthTuple(
      cdm.getNumValueElements,
      cdm.getTagId
    )
  }
}
