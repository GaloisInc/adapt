package com.galois.adapt.cdm18

import com.bbn.tc.schema.avro.cdm18
import com.galois.adapt.DBWritable

import scala.util.Try

// Host identifier, such as serial number, IMEI number
case class HostIdentifier(
     idType: String,
     idValue: String
 ) extends CDM18 with DBWritable {
  def asDBKeyValues: List[(String, Any)] = List(
    ("idType", idType),
    ("idValue", idValue)
  )
}

case object HostIdentifier extends CDM18Constructor[HostIdentifier] {
  type RawCDMType = cdm18.HostIdentifier

  def from(cdm: RawCDM18Type): Try[HostIdentifier] = Try {
    HostIdentifier(
      cdm.getIdType,
      cdm.getIdValue
    )
  }
}