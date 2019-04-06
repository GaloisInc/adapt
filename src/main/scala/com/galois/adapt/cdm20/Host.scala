package com.galois.adapt.cdm20

import java.util.UUID

import com.bbn.tc.schema.avro.cdm20
import com.galois.adapt.{DBNodeable, DBWritable}
// import scala.collection.JavaConverters._
import scala.language.implicitConversions

import scala.util.Try

case class Host (
  uuid: UUID, // universally unique identifier for the host
  hostName: String, // hostname or machine name
  ta1Version: String,
  hostIdentifiers: Seq[HostIdentifier], // list of identifiers, such as serial number, IMEI number
  osDetails: Option[String], // OS level details revealed by tools such as uname -a
  hostType: HostType, // host's role or device type, such as mobile, server, desktop
  interfaces: Seq[Interface] // names and addresses of network interfaces
) extends CDM20 with DBWritable with DBNodeable[CDM20.EdgeTypes.EdgeTypes] {
  def asDBKeyValues: List[(String, Any)] = List(
    ("uuid", uuid.toString),
    ("hostName", hostName),
    ("ta1Version", ta1Version),
    ("hostIdentifiers", hostIdentifiers.map(_.toString).mkString(":")),
    ("osDetails", osDetails.getOrElse("")),
    ("hostType", hostType.toString),
    ("interfaces", interfaces.map(_.toString).mkString(":"))
  )

  def asDBEdges = Nil

  def getUuid = uuid

  override def getHostId: Option[UUID] = Some(uuid)

  def toMap: Map[String, Any] = asDBKeyValues.toMap
}

case object Host extends CDM20Constructor[Host] {
  type RawCDMType = cdm20.Host

  def from(cdm: RawCDM20Type): Try[Host] = Try {
    Host(
      cdm.getUuid,
      cdm.getHostName,
      cdm.getTa1Version,
      AvroOpt.listHostIdentifier(cdm.getHostIdentifiers).getOrElse(Seq()),
      AvroOpt.str(cdm.getOsDetails),
      cdm.getHostType,
      AvroOpt.listInterfaces(cdm.getInterfaces).getOrElse(Seq())
    )
  }
}
