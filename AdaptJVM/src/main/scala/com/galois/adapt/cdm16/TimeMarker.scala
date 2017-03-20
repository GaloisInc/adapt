package com.galois.adapt.cdm16

import java.util.UUID

import com.bbn.tc.schema.avro.cdm16
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class TimeMarker(
  timestampNanos: Long
) extends CDM16 with DBWritable {
  def asDBKeyValues = List(
//    label, "TimeMarker",
    "timestampNanos", timestampNanos
  )
}


case object TimeMarker extends CDM16Constructor[TimeMarker] {
  type RawCDMType = cdm16.TimeMarker

  def from(cdm: RawCDM15Type): Try[TimeMarker] = Try {
    TimeMarker(
      cdm.getTsNanos
    )
  }
}
