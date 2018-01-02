package com.galois.adapt.cdm18

import java.util.UUID

import com.bbn.tc.schema.avro.cdm18
import com.galois.adapt.{DBNodeable, DBWritable}
// import scala.collection.JavaConverters._
import scala.language.implicitConversions

import scala.util.Try

case class Host (
  uuid: UUID, // universally unique identifier for the host
  hostName: String, // hostname or machine name
  hostIdentifiers: Seq[HostIdentifier], // list of identifiers, such as serial number, IMEI number
  osDetails: String, // OS level details revealed by tools such as uname -a
  hostType: HostType, // host's role or device type, such as mobile, server, desktop
  interfaces: Seq[Interface] // names and addresses of network interfaces
) extends CDM18 with DBWritable with DBNodeable[CDM18.EdgeTypes.EdgeTypes] {
  def asDBKeyValues: List[(String, Any)] = List(
    ("uuid", uuid.toString),
    ("hostName", hostName),
    ("hostIdentifiers", hostIdentifiers.map(_.toString).mkString(":")),
    ("osDetails", osDetails),
    ("hostType", hostType.toString),
    ("interfaces", interfaces.map(_.toString).mkString(":"))
  )

  def asDBEdges = Nil

  def getUuid = uuid

  def toMap: Map[String, Any] = asDBKeyValues.toMap
}

case object Host extends CDM18Constructor[Host] {
  type RawCDMType = cdm18.Host

  def from(cdm: RawCDM18Type): Try[Host] = Try {
    Host(
      cdm.getUuid,
      cdm.getHostName,
      AvroOpt.listHostIdentifier(cdm.getHostIdentifiers).getOrElse(Seq()),
      cdm.getOsDetails,
      cdm.getHostType,
      AvroOpt.listInterfaces(cdm.getInterfaces).getOrElse(Seq())
    )
  }
}
