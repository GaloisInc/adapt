package com.galois.adapt.cdm18

import com.bbn.tc.schema.avro.cdm18
import com.galois.adapt.DBWritable

import scala.util.Try

// StartMarker records delineate system (re)starts in a data stream.
case class StartMarker(
  sessionNumber: Int // sequence number that monotonically increases each time the system is started
) extends CDM18 with DBWritable {
  override def asDBKeyValues = List(("sessionNumber", sessionNumber))
}


case object StartMarker extends CDM18Constructor[StartMarker] {
  type RawCDMType = cdm18.StartMarker

  def from(cdm: RawCDM18Type): Try[StartMarker] = Try {
    StartMarker(
      cdm.getSessionNumber
    )
  }
}
