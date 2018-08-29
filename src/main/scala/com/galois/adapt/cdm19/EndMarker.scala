package com.galois.adapt.cdm19

import java.util.UUID

import com.bbn.tc.schema.avro.cdm19
import com.galois.adapt.DBWritable

import scala.util.Try

// EndMarker records marks the end of a data stream..
case class EndMarker(
  sessionNumber: Int, // session number in the corresponding StartMarker
  host: UUID,

  // Reports countc of each record type that has been published
  // since the the start of the data stream.
  // (Alec: they could at least have made the value type of this map something numeric)
  recordCounts: Map[String,String]
) extends CDM19 with DBWritable {
  override def asDBKeyValues: List[(String,Any)] =
    List(("sessionNumber", sessionNumber)) ++
    recordCounts.toList
}


case object EndMarker extends CDM19Constructor[EndMarker] {
  type RawCDMType = cdm19.EndMarker

  def from(cdm: RawCDM19Type): Try[EndMarker] = Try {
    EndMarker(
      cdm.getSessionNumber,
      cdm.getHostId.get,
      AvroOpt.map(cdm.getRecordCounts).get
    )
  }
}
