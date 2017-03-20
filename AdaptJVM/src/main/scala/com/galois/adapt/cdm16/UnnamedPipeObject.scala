package com.galois.adapt.cdm16

import java.util.UUID

import com.bbn.tc.schema.avro.cdm16
import com.galois.adapt.{DBWritable, DBNodeable}
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class UnnamedPipeObject(
  uuid: UUID,
  baseObject: AbstractObject,
  sourceFileDescriptor: Int,
  sinkFileDescriptor: Int
) extends CDM16 with DBWritable with DBNodeable {
  def asDBKeyValues = List(
    label, "UnnamedPipeObject",
    "uuid", uuid,
    "sourceFileDescriptor", sourceFileDescriptor,
    "sinkFileDescriptor", sinkFileDescriptor
  ) ++
    baseObject.asDBKeyValues

  def asDBEdges = Nil

  def getUuid = uuid
}


case object UnnamedPipeObject extends CDM16Constructor[UnnamedPipeObject] {
  type RawCDMType = cdm16.UnnamedPipeObject

  def from(cdm: RawCDM15Type): Try[UnnamedPipeObject] = Try {
    UnnamedPipeObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getSourceFileDescriptor,
      cdm.getSinkFileDescriptor
    )
  }
}
