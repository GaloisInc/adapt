package com.galois.adapt.cdm18

import java.util.UUID
import com.bbn.tc.schema.avro.cdm18
import com.galois.adapt.{DBWritable, DBNodeable}
import scala.util.Try

// No change
case class SrcSinkObject(
  uuid: UUID,
  baseObject: AbstractObject,
  srcSinkType: SrcSinkType,
  fileDescriptor: Option[Int]
) extends CDM18 with DBWritable with DBNodeable {

  def asDBKeyValues: List[(String, Any)] = List(
    ("uuid", uuid),
    ("srcSinkType", srcSinkType.toString)
  ) ++
    baseObject.asDBKeyValues ++
    fileDescriptor.fold[List[(String,Any)]](List.empty)(v => List(("fileDescriptor", v)))

  def asDBEdges = Nil

  def getUuid: UUID = uuid

  def toMap: Map[String, Any] = Map(
    "uuid" -> uuid,
    "srcSinkType" -> srcSinkType,
    "fileSescriptor" -> fileDescriptor.getOrElse(""),
    "properties" -> baseObject.properties.getOrElse(Map.empty)
  )
}

case object SrcSinkObject extends CDM18Constructor[SrcSinkObject] {
  type RawCDMType = cdm18.SrcSinkObject

  def from(cdm: RawCDM18Type): Try[SrcSinkObject] = Try(
    SrcSinkObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getType,
      AvroOpt.int(cdm.getFileDescriptor)
    )
  )
}
