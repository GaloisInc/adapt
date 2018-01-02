package com.galois.adapt.cdm18

import com.bbn.tc.schema.avro.cdm18
import com.galois.adapt.DBWritable

import scala.util.Try

// No changes
case class TimeMarker(
  timestampNanos: Long
) extends CDM18 with DBWritable {
  def asDBKeyValues: List[(String, Any)] = Nil
}


case object TimeMarker extends CDM18Constructor[TimeMarker] {
  type RawCDMType = cdm18.TimeMarker

  def from(cdm: RawCDM18Type): Try[TimeMarker] = Try {
    TimeMarker(
      cdm.getTsNanos
    )
  }
}
