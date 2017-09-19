package com.galois.adapt.cdm17

import java.util.UUID
import com.bbn.tc.schema.avro.cdm17
import com.galois.adapt.{DBWritable, DBNodeable}
import org.apache.tinkerpop.gremlin.structure.T.label
import scala.util.Try


case class NetFlowObject(
  uuid: UUID,
  baseObject: AbstractObject,
  localAddress: String,
  localPort: Int,
  remoteAddress: String,
  remotePort: Int,
  ipProtocol: Option[Int] = None,
  fileDescriptor: Option[Int] = None
) extends CDM17 with DBWritable with DBNodeable {
  override def getLabels: List[String] = List("CDM17", "NetFlowObject")

  def asDBKeyValues =
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

  def getUuid = uuid

  def toMap: Map[String, Any] = Map(
//    "label" -> "NetFlowObject",
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

case object NetFlowObject extends CDM17Constructor[NetFlowObject] {
  type RawCDMType = cdm17.NetFlowObject

  def from(cdm: RawCDM17Type): Try[NetFlowObject] = Try(
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
