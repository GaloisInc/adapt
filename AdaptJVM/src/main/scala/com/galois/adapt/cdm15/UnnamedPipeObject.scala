package com.galois.adapt.cdm15

import java.util.UUID

import com.bbn.tc.schema.avro.cdm15
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class UnnamedPipeObject(
  uuid: UUID,
  baseObject: AbstractObject,
  sourceFileDescriptor: Int,
  sinkFileDescriptor: Int
) extends CDM15 with DBWritable {
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


case object UnnamedPipeObject extends CDM15Constructor[UnnamedPipeObject] {
  type RawCDMType = cdm15.UnnamedPipeObject

  def from(cdm: RawCDM15Type): Try[UnnamedPipeObject] = Try {
    UnnamedPipeObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getSourceFileDescriptor,
      cdm.getSinkFileDescriptor
    )
  }
}
