package com.galois.adapt.cdm17

import java.util.UUID

import com.bbn.tc.schema.avro.cdm17
import com.galois.adapt.{DBWritable, DBNodeable}
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class SrcSinkObject(
  uuid: UUID,
  baseObject: AbstractObject,
  srcSinkType: SrcSinkType,
  fileDescriptor: Option[Int]
) extends CDM17 with DBWritable with DBNodeable {
  def asDBKeyValues = List(
    label, "SrcSinkObject",
    "uuid", uuid,
    "srcSinkType", srcSinkType.toString
  ) ++
    baseObject.asDBKeyValues ++
    fileDescriptor.fold[List[Any]](List.empty)(v => List("fileDescriptor", v))

  def asDBEdges = Nil

  def getUuid = uuid
}

case object SrcSinkObject extends CDM17Constructor[SrcSinkObject] {
  type RawCDMType = cdm17.SrcSinkObject

  def from(cdm: RawCDM15Type): Try[SrcSinkObject] = Try(
    SrcSinkObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getType,
      AvroOpt.int(cdm.getFileDescriptor)
    )
  )
}
