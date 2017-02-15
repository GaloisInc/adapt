package com.galois.adapt.cdm14

import java.util.UUID

import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class UnnamedPipeObject(
                    uuid: UUID,
                    baseObject: AbstractObject,
                    sourceFileDescriptor: Int,
                    sinkFileDescriptor: Int,
                    localPrincipal: UUID
                  ) extends CDM14 with DBWritable {
  def asDBKeyValues = List(
    label, "UnnamedPipeObject",
    "uuid", uuid,
    "sourceFileDescriptor", sourceFileDescriptor,
    "sinkFileDescriptor", sinkFileDescriptor,
    "localPrinicpal", localPrincipal
  ) ++
    baseObject.asDBKeyValues
}


case object UnnamedPipeObject extends CDM14Constructor[UnnamedPipeObject] {
  type RawCDMType = com.bbn.tc.schema.avro.UnnamedPipeObject

  def from(cdm: RawCDM14Type): Try[UnnamedPipeObject] = Try {
    UnnamedPipeObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getSourceFileDescriptor,
      cdm.getSinkFileDescriptor,
      cdm.getLocalPrincipal
    )
  }
}