package com.galois.adapt.cdm20

import com.bbn.tc.schema.avro.cdm20
import com.galois.adapt.DBWritable

import scala.util.Try

// Host identifier, such as serial number, IMEI number
case class HostIdentifier(
     idType: String,
     idValue: String
 ) extends CDM20 with DBWritable {
  def asDBKeyValues: List[(String, Any)] = List(
    ("idType", idType),
    ("idValue", idValue)
  )
}

case object HostIdentifier extends CDM20Constructor[HostIdentifier] {
  type RawCDMType = cdm20.HostIdentifier

  def from(cdm: RawCDM20Type): Try[HostIdentifier] = Try {
    HostIdentifier(
      cdm.getIdType,
      cdm.getIdValue
    )
  }
}
