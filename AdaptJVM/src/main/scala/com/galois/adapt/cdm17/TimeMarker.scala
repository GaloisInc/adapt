package com.galois.adapt.cdm17

import java.util.UUID

import com.bbn.tc.schema.avro.cdm17
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class TimeMarker(
  timestampNanos: Long
) extends CDM17 with DBWritable {
  def asDBKeyValues = List(
//    label, "TimeMarker",
    "timestampNanos", timestampNanos
  )
}


case object TimeMarker extends CDM17Constructor[TimeMarker] {
  type RawCDMType = cdm17.TimeMarker

  def from(cdm: RawCDM15Type): Try[TimeMarker] = Try {
    TimeMarker(
      cdm.getTsNanos
    )
  }
}
