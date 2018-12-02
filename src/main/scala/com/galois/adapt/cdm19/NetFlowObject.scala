package com.galois.adapt.cdm19

import java.util.UUID
import com.bbn.tc.schema.avro.cdm19
import com.galois.adapt.{DBWritable, DBNodeable}
import scala.util.Try


// No changes
case class NetFlowObject(
  uuid: UUID,
  host: UUID,
  baseObject: AbstractObject,
  localAddress: Option[String],
  localPort: Option[Int],
  remoteAddress: Option[String],
  remotePort: Option[Int],
  ipProtocol: Option[Int] = None,
  fileDescriptor: Option[Int] = None
) extends CDM19 with DBWritable with DBNodeable[CDM19.EdgeTypes.EdgeTypes] {

  def asDBKeyValues: List[(String, Any)] =
      baseObject.asDBKeyValues ++
      List(("uuid", uuid)) ++
      localAddress.fold[List[(String,Any)]](List.empty)(v => List(("localAddress", v))) ++
      localPort.fold[List[(String,Any)]](List.empty)(v => List(("localPort", v))) ++
      remoteAddress.fold[List[(String,Any)]](List.empty)(v => List(("remoteAddress", v))) ++
      remotePort.fold[List[(String,Any)]](List.empty)(v => List(("remotePort", v))) ++
      ipProtocol.fold[List[(String,Any)]](List.empty)(v => List(("ipProtocol", v))) ++
      fileDescriptor.fold[List[(String,Any)]](List.empty)(v => List(("fileDescriptor", v)))

  def asDBEdges = Nil

  def getUuid: UUID = uuid

  override def getHostId: Option[UUID] = Some(host)

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

case object NetFlowObject extends CDM19Constructor[NetFlowObject] {
  type RawCDMType = cdm19.NetFlowObject

  def from(cdm: RawCDM19Type): Try[NetFlowObject] = Try(
    NetFlowObject(
      cdm.getUuid,
      cdm.getHostId.get,
      cdm.getBaseObject,
      AvroOpt.str(cdm.getLocalAddress),
      AvroOpt.int(cdm.getLocalPort),
      AvroOpt.str(cdm.getRemoteAddress),
      AvroOpt.int(cdm.getRemotePort),
      AvroOpt.int(cdm.getIpProtocol),
      AvroOpt.int(cdm.getFileDescriptor)
    )
  )
}
