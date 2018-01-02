package com.galois.adapt.cdm18

import java.util.UUID

import com.bbn.tc.schema.avro.cdm18
import com.galois.adapt.{DBNodeable, DBWritable}

import scala.util.Try

// Represents a packet socket. Instantiates an AbstractObject.
case class PacketSocketObject(
  uuid: UUID,
  baseObject: AbstractObject,

  proto: FixedShort, // Physical-layer protocol
  ifIndex: Int, // Interface number
  haType: FixedShort, // ARP hardware type
  pktType: FixedByte, // Packet type
  addr: Seq[Byte] // Physical-layer address
) extends CDM18 with DBWritable with DBNodeable[CDM18.EdgeTypes.EdgeTypes] {
  override def asDBKeyValues: List[(String, Any)] = List(
    ("proto", proto.toString),
    ("ifIndex", ifIndex),
    ("haType", haType.toString),
    ("pktType", pktType.toString),
    ("addr", addr.toString)
  )

  override def getUuid: UUID = uuid

  override def asDBEdges = Nil
}

case object PacketSocketObject extends CDM18Constructor[PacketSocketObject] {
  type RawCDMType = cdm18.PacketSocketObject

  def from(cdm: RawCDM18Type): Try[PacketSocketObject] = Try(
    PacketSocketObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getProto,
      cdm.getIfIndex,
      cdm.getHaType,
      cdm.getPktType,
      cdm.getAddr.array()
    )
  )
}
