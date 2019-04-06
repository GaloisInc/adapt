package com.galois.adapt.cdm20

import java.util.UUID

import com.bbn.tc.schema.avro.cdm20
import com.galois.adapt.DBWritable

import scala.util.Try

// No changes
case class TimeMarker(
  timestampNanos: Long,
  host: UUID
) extends CDM20 with DBWritable {
  def asDBKeyValues: List[(String, Any)] = Nil
}


case object TimeMarker extends CDM20Constructor[TimeMarker] {
  type RawCDMType = cdm20.TimeMarker

  def from(cdm: RawCDM20Type): Try[TimeMarker] = Try {
    TimeMarker(
      cdm.getTsNanos,
      cdm.getHostId.get
    )
  }
}
