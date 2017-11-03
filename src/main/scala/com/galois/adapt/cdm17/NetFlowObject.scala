package com.galois.adapt.cdm17

import java.util.UUID

import com.bbn.tc.schema.avro.cdm17
import com.galois.adapt.{DBNodeable, DBWritable}
import com.rrwright.quine.language.{FreeDomainNode, FreeNodeConstructor}
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
) extends FreeDomainNode[NetFlowObject] with CDM17 with DBWritable with DBNodeable {

  val companion = NetFlowObject

  def asDBKeyValues =
    baseObject.asDBKeyValues ++
      List(
        label, "NetFlowObject",
        "uuid", uuid,
        "localAddress", localAddress,
        "localPort", localPort,
        "remoteAddress", remoteAddress,
        "remotePort", remotePort
      ) ++
      ipProtocol.fold[List[Any]](List.empty)(v => List("ipProtocol", v)) ++
      fileDescriptor.fold[List[Any]](List.empty)(v => List("fileDescriptor", v))

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

case object NetFlowObject extends FreeNodeConstructor with CDM17Constructor[NetFlowObject] {
  type ClassType = NetFlowObject

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
