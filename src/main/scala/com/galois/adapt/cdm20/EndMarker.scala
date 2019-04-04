package com.galois.adapt.cdm20

import java.util.UUID

import com.bbn.tc.schema.avro.cdm20
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
) extends CDM20 with DBWritable {
  override def asDBKeyValues: List[(String,Any)] =
    List(("sessionNumber", sessionNumber)) ++
    recordCounts.toList
}


case object EndMarker extends CDM20Constructor[EndMarker] {
  type RawCDMType = cdm20.EndMarker

  def from(cdm: RawCDM20Type): Try[EndMarker] = Try {
    EndMarker(
      cdm.getSessionNumber,
      cdm.getHostId.get,
      AvroOpt.map(cdm.getRecordCounts).get
    )
  }
}
