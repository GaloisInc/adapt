package com.galois.adapt.cdm19

import java.util.UUID
import com.bbn.tc.schema.avro.cdm19
import com.galois.adapt.{DBWritable, DBNodeable}
import scala.util.Try

// No change
case class SrcSinkObject(
  uuid: UUID,
  host: UUID,
  baseObject: AbstractObject,
  srcSinkType: SrcSinkType,
  fileDescriptor: Option[Int]
) extends CDM19 with DBWritable with DBNodeable[CDM19.EdgeTypes.EdgeTypes] {

  def asDBKeyValues: List[(String, Any)] = List(
    ("uuid", uuid),
    ("srcSinkType", srcSinkType.toString)
  ) ++
    baseObject.asDBKeyValues ++
    fileDescriptor.fold[List[(String,Any)]](List.empty)(v => List(("fileDescriptor", v)))

  def asDBEdges = Nil

  def getUuid: UUID = uuid

  override def getHostId: Option[UUID] = Some(host)

  def toMap: Map[String, Any] = Map(
    "uuid" -> uuid,
    "srcSinkType" -> srcSinkType,
    "fileSescriptor" -> fileDescriptor.getOrElse(""),
    "properties" -> baseObject.properties.getOrElse(Map.empty)
  )
}

case object SrcSinkObject extends CDM19Constructor[SrcSinkObject] {
  type RawCDMType = cdm19.SrcSinkObject

  def from(cdm: RawCDM19Type): Try[SrcSinkObject] = Try(
    SrcSinkObject(
      cdm.getUuid,
      cdm.getHostId.get,
      cdm.getBaseObject,
      cdm.getType,
      AvroOpt.int(cdm.getFileDescriptor)
    )
  )
}
