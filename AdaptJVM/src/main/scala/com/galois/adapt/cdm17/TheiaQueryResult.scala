package com.galois.adapt.cdm17

import com.bbn.tc.schema.avro.cdm17
import scala.util.Try

case class TheiaQueryResult() extends CDM17 {
}


case object TheiaQueryResult extends CDM17Constructor[TheiaQueryResult] {
  type RawCDMType = cdm17.TheiaQueryResult

  def from(cdm: RawCDM17Type): Try[TheiaQueryResult] = Try { TheiaQueryResult() }
}