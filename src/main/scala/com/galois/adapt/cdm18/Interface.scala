package com.galois.adapt.cdm18

import com.bbn.tc.schema.avro.cdm18
import com.galois.adapt.DBWritable
import scala.collection.JavaConverters._
import scala.language.implicitConversions

import scala.util.Try

// Interface name and addresses
case class Interface(
    name: String,
    macAddress: String,
    ipAddresses: List[String]
) extends CDM18 with DBWritable {
  def asDBKeyValues: List[(String, Any)] = List(
    ("name", name),
    ("macAddress", macAddress),
    ("ipAddresses", ipAddresses.mkString(":"))
  )
}

case object Interface extends CDM18Constructor[Interface] {
  type RawCDMType = cdm18.Interface

  def from(cdm: RawCDM18Type): Try[Interface] = Try {
    Interface(
      cdm.getName,
      cdm.getMacAddress,
      cdm.getIpAddresses.asScala.toList.map(makeString)
    )
  }
}