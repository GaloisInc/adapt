package com.galois.adapt.cdm18

import java.util.UUID
import com.bbn.tc.schema.avro.cdm18
import com.galois.adapt.{DBWritable, DBNodeable}
import scala.util.Try


case class UnnamedPipeObject(
  uuid: UUID,
  baseObject: AbstractObject,

  // Although file descriptor and UUID src/sink pairs are
  // individually optional, at least one pair MUST be used.
  sourceFileDescriptor: Option[Int] = None,
  source: Option[UUID] = None,
  sinkFileDescriptor: Option[Int] = None,
  sink: Option[UUID] = None
) extends CDM18 with DBWritable with DBNodeable {

  def asDBKeyValues = List(
      ("uuid", uuid),
      ("sourceFileDescriptor", sourceFileDescriptor)
    ) ++
    sourceFileDescriptor.fold[List[(String, Any)]](Nil)(v => List(("sourceFileDescriptor", v))) ++
    sinkFileDescriptor.fold[List[(String, Any)]](Nil)(v => List(("sinkFileDescriptor", v))) ++
    baseObject.asDBKeyValues

  // TODO CDM18 edges (source, sink)
  def asDBEdges = Nil

  def getUuid = uuid
}


case object UnnamedPipeObject extends CDM18Constructor[UnnamedPipeObject] {
  type RawCDMType = cdm18.UnnamedPipeObject

  def from(cdm: RawCDM18Type): Try[UnnamedPipeObject] = Try {
    UnnamedPipeObject(
      cdm.getUuid,
      cdm.getBaseObject,
      AvroOpt.int(cdm.getSourceFileDescriptor),
      AvroOpt.uuid(cdm.getSourceUUID),
      AvroOpt.int(cdm.getSinkFileDescriptor),
      AvroOpt.uuid(cdm.getSinkUUID)
    )
  }
}
