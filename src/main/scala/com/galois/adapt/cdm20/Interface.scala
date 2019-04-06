package com.galois.adapt.cdm20

import com.bbn.tc.schema.avro.cdm20
import com.galois.adapt.DBWritable
import scala.collection.JavaConverters._
import scala.language.implicitConversions

import scala.util.Try

// Interface name and addresses
case class Interface(
    name: String,
    macAddress: String,
    ipAddresses: List[String]
) extends CDM20 with DBWritable {
  def asDBKeyValues: List[(String, Any)] = List(
    ("name", name),
    ("macAddress", macAddress),
    ("ipAddresses", ipAddresses.mkString(":"))
  )
}

case object Interface extends CDM20Constructor[Interface] {
  type RawCDMType = cdm20.Interface

  def from(cdm: RawCDM20Type): Try[Interface] = Try {
    Interface(
      cdm.getName,
      cdm.getMacAddress,
      AvroOpt.listStr(cdm.getIpAddresses).getOrElse(List.empty)
    )
  }
}
