package com.galois.adapt.cdm19

import com.bbn.tc.schema.avro.cdm19
import com.galois.adapt.DBWritable
import scala.collection.JavaConverters._
import scala.language.implicitConversions

import scala.util.Try

// Interface name and addresses
case class Interface(
    name: String,
    macAddress: String,
    ipAddresses: List[String]
) extends CDM19 with DBWritable {
  def asDBKeyValues: List[(String, Any)] = List(
    ("name", name),
    ("macAddress", macAddress),
    ("ipAddresses", ipAddresses.mkString(":"))
  )
}

case object Interface extends CDM19Constructor[Interface] {
  type RawCDMType = cdm19.Interface

  def from(cdm: RawCDM19Type): Try[Interface] = Try {
    Interface(
      cdm.getName,
      cdm.getMacAddress,
      AvroOpt.listStr(cdm.getIpAddresses).getOrElse(List.empty)
    )
  }
}