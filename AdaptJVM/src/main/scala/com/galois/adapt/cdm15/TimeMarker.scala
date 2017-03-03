package com.galois.adapt.cdm15

import java.util.UUID

import com.bbn.tc.schema.avro.cdm15
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class TimeMarker(
                    timestampNanos: Long
                  ) extends CDM15 with DBWritable {
  def asDBKeyValues = List(
    label, "TimeMarker",
    "timestampNanos", timestampNanos
  )

  def asDBEdges = Nil

  def getUuid = throw new RuntimeException("TimeMarker has no UUID")
}


case object TimeMarker extends CDM15Constructor[TimeMarker] {
  type RawCDMType = cdm15.TimeMarker

  def from(cdm: RawCDM15Type): Try[TimeMarker] = Try {
    TimeMarker(
      cdm.getTsNanos
    )
  }
}
