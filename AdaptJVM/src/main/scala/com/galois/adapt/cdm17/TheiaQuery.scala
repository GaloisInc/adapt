package com.galois.adapt.cdm17

import com.bbn.tc.schema.avro.cdm17
import scala.util.Try

case class TheiaQuery() extends CDM17 {
}


case object TheiaQuery extends CDM17Constructor[TheiaQuery] {
  type RawCDMType = cdm17.TheiaQuery

  def from(cdm: RawCDM17Type): Try[TheiaQuery] = Try { TheiaQuery() }
}