package com.galois.adapt.cdm19

import java.util.UUID

import com.bbn.tc.schema.avro.cdm19
import com.galois.adapt.DBWritable

import scala.util.Try

// No changes
case class TimeMarker(
  timestampNanos: Long,
  host: UUID
) extends CDM19 with DBWritable {
  def asDBKeyValues: List[(String, Any)] = Nil
}


case object TimeMarker extends CDM19Constructor[TimeMarker] {
  type RawCDMType = cdm19.TimeMarker

  def from(cdm: RawCDM19Type): Try[TimeMarker] = Try {
    TimeMarker(
      cdm.getTsNanos,
      cdm.getHostId.get
    )
  }
}
