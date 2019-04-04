package com.galois.adapt.cdm20

import java.util.UUID

import com.bbn.tc.schema.avro.cdm20
import com.galois.adapt.{DBNodeable, DBWritable}

import scala.util.Try

// Represents a packet socket. Instantiates an AbstractObject.
case class PacketSocketObject(
  uuid: UUID,
  host: UUID,
  baseObject: AbstractObject,

  proto: FixedShort, // Physical-layer protocol
  ifIndex: Int, // Interface number
  haType: FixedShort, // ARP hardware type
  pktType: FixedByte, // Packet type
  addr: Seq[Byte] // Physical-layer address
) extends CDM20 with DBWritable with DBNodeable[CDM20.EdgeTypes.EdgeTypes] {
  override def asDBKeyValues: List[(String, Any)] = List(
    ("proto", proto.toString),
    ("ifIndex", ifIndex),
    ("haType", haType.toString),
    ("pktType", pktType.toString),
    ("addr", addr.toString)
  )

  override def getUuid: UUID = uuid

  override def getHostId: Option[UUID] = Some(host)

  override def asDBEdges = Nil
}

case object PacketSocketObject extends CDM20Constructor[PacketSocketObject] {
  type RawCDMType = cdm20.PacketSocketObject

  def from(cdm: RawCDM20Type): Try[PacketSocketObject] = Try(
    PacketSocketObject(
      cdm.getUuid,
      cdm.getHostId.get,
      cdm.getBaseObject,
      cdm.getProto,
      cdm.getIfIndex,
      cdm.getHaType,
      cdm.getPktType,
      cdm.getAddr.array()
    )
  )
}
