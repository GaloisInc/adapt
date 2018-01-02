package com.galois.adapt.cdm18

import java.util.UUID
import com.bbn.tc.schema.avro.cdm18
import com.galois.adapt.{DBWritable, DBNodeable}
import scala.util.Try


// No changes
case class NetFlowObject(
  uuid: UUID,
  baseObject: AbstractObject,
  localAddress: String,
  localPort: Int,
  remoteAddress: String,
  remotePort: Int,
  ipProtocol: Option[Int] = None,
  fileDescriptor: Option[Int] = None
) extends CDM18 with DBWritable with DBNodeable {

  def asDBKeyValues: List[(String, Any)] =
    baseObject.asDBKeyValues ++
      List(
        ("uuid", uuid),
        ("localAddress", localAddress),
        ("localPort", localPort),
        ("remoteAddress", remoteAddress),
        ("remotePort", remotePort)
      ) ++
      ipProtocol.fold[List[(String,Any)]](List.empty)(v => List(("ipProtocol", v))) ++
      fileDescriptor.fold[List[(String,Any)]](List.empty)(v => List(("fileDescriptor", v)))

  def asDBEdges = Nil

  def getUuid: UUID = uuid

  def toMap: Map[String, Any] = Map(
    "uuid" -> uuid,
    "localAddress" -> localAddress,
    "localPort" -> localPort,
    "remoteAddress" -> remoteAddress,
    "remotePort" -> remotePort,
    "ipProtocol" -> ipProtocol.getOrElse(""),
    "fileDescriptor" -> fileDescriptor.getOrElse(""),
    "properties" -> baseObject.properties.getOrElse(Map.empty)
  ) //++ baseObject.properties.getOrElse(Map.empty)
}

case object NetFlowObject extends CDM18Constructor[NetFlowObject] {
  type RawCDMType = cdm18.NetFlowObject

  def from(cdm: RawCDM18Type): Try[NetFlowObject] = Try(
    NetFlowObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getLocalAddress,
      cdm.getLocalPort,
      cdm.getRemoteAddress,
      cdm.getRemotePort,
      AvroOpt.int(cdm.getIpProtocol),
      AvroOpt.int(cdm.getFileDescriptor)
    )
  )
}
