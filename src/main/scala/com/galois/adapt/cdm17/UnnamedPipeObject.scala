package com.galois.adapt.cdm17

import java.util.UUID
import com.bbn.tc.schema.avro.cdm17
import com.galois.adapt.{DBWritable, DBNodeable}
import scala.util.Try


case class UnnamedPipeObject(
  uuid: UUID,
  baseObject: AbstractObject,
  sourceFileDescriptor: Int,
  sinkFileDescriptor: Int
) extends CDM17 with DBWritable with DBNodeable[CDM17.EdgeTypes.EdgeTypes] {

  def asDBKeyValues = List(
    ("uuid", uuid),
    ("sourceFileDescriptor", sourceFileDescriptor),
    ("sinkFileDescriptor", sinkFileDescriptor)
  ) ++
    baseObject.asDBKeyValues

  def asDBEdges = Nil

  def getUuid = uuid
}


case object UnnamedPipeObject extends CDM17Constructor[UnnamedPipeObject] {
  type RawCDMType = cdm17.UnnamedPipeObject

  def from(cdm: RawCDM17Type): Try[UnnamedPipeObject] = Try {
    UnnamedPipeObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getSourceFileDescriptor,
      cdm.getSinkFileDescriptor
    )
  }
}
