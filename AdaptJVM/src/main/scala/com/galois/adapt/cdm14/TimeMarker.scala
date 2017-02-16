package com.galois.adapt.cdm14

import java.util.UUID

import com.bbn.tc.schema.avro.cdm14
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class TimeMarker(
                    timestampNanos: Long
                  ) extends CDM14 with DBWritable {
  def asDBKeyValues = List(
    label, "TimeMarker",
    "timestampNanos", timestampNanos
  )
}


case object TimeMarker extends CDM14Constructor[TimeMarker] {
  type RawCDMType = cdm14.TimeMarker

  def from(cdm: RawCDM14Type): Try[TimeMarker] = Try {
    TimeMarker(
      cdm.getTimestampNanos
    )
  }
}