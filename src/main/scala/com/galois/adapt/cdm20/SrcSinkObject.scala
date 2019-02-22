package com.galois.adapt.cdm20

import java.util.UUID
import com.bbn.tc.schema.avro.cdm20
import com.galois.adapt.{DBWritable, DBNodeable}
import scala.util.Try

// No change
case class SrcSinkObject(
  uuid: UUID,
  host: UUID,
  baseObject: AbstractObject,
  srcSinkType: SrcSinkType,
  fileDescriptor: Option[Int]
) extends CDM20 with DBWritable with DBNodeable[CDM20.EdgeTypes.EdgeTypes] {

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

case object SrcSinkObject extends CDM20Constructor[SrcSinkObject] {
  type RawCDMType = cdm20.SrcSinkObject

  def from(cdm: RawCDM20Type): Try[SrcSinkObject] = Try(
    SrcSinkObject(
      cdm.getUuid,
      cdm.getHostId.get,
      cdm.getBaseObject,
      cdm.getType,
      AvroOpt.int(cdm.getFileDescriptor)
    )
  )
}
