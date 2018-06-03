package com.galois.adapt.cdm18

import com.bbn.tc.schema.avro.cdm18
import com.galois.adapt.DBWritable

import scala.util.Try

// EndMarker records marks the end of a data stream..
case class EndMarker(
  sessionNumber: Int, // session number in the corresponding StartMarker

  // Reports countc of each record type that has been published
  // since the the start of the data stream.
  // (Alec: they could at least have made the value type of this map something numeric)
  recordCounts: Map[String,String]
) extends CDM18 with DBWritable {
  override def asDBKeyValues: List[(String,Any)] =
    List(("sessionNumber", sessionNumber)) ++
    recordCounts.toList
}


case object EndMarker extends CDM18Constructor[EndMarker] {
  type RawCDMType = cdm18.EndMarker

  def from(cdm: RawCDM18Type): Try[EndMarker] = Try {
    EndMarker(
      cdm.getSessionNumber,
      AvroOpt.map(cdm.getRecordCounts).get
    )
  }
}
