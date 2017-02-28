package com.galois.adapt.cdm15

import java.util.UUID

import com.bbn.tc.schema.avro.cdm15
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class TCCDMDatum(
                       source: InstrumentationSource
                     ) extends CDM15 with DBWritable {
  def asDBKeyValues = List(
    label, "TCCDMDatum",
    "source", source
  )
}


case object TCCDMDatum extends CDM15Constructor[TCCDMDatum] {
  type RawCDMType = cdm15.TCCDMDatum

  def from(cdm: RawCDM15Type): Try[TCCDMDatum] = Try {
    TCCDMDatum(
      cdm.getSource
    )
  }
}
