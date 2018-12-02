package com.galois.adapt.cdm19

import com.bbn.tc.schema.avro.cdm19
import com.galois.adapt.DBWritable

import scala.util.Try

// Host identifier, such as serial number, IMEI number
case class HostIdentifier(
     idType: String,
     idValue: String
 ) extends CDM19 with DBWritable {
  def asDBKeyValues: List[(String, Any)] = List(
    ("idType", idType),
    ("idValue", idValue)
  )
}

case object HostIdentifier extends CDM19Constructor[HostIdentifier] {
  type RawCDMType = cdm19.HostIdentifier

  def from(cdm: RawCDM19Type): Try[HostIdentifier] = Try {
    HostIdentifier(
      cdm.getIdType,
      cdm.getIdValue
    )
  }
}