package com.galois.adapt.cdm14

import java.util.UUID

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
  type RawCDMType = com.bbn.tc.schema.avro.TimeMarker

  def from(cdm: RawCDM14Type): Try[TimeMarker] = Try {
    TimeMarker(
      cdm.getTimeStampNanos
    )
  }
}