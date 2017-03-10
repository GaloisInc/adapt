package com.galois.adapt.cdm15

import java.util.UUID

import akka.protobuf.Descriptors.FileDescriptor
import com.bbn.tc.schema.avro.cdm15
import com.galois.adapt.DBWritable
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
) extends CDM15 with DBWritable {
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
}

case object NetFlowObject extends CDM15Constructor[NetFlowObject] {
  type RawCDMType = cdm15.NetFlowObject

  def from(cdm: RawCDM15Type): Try[NetFlowObject] = Try(
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
