package com.galois.adapt.cdm19
import com.bbn.tc.schema.avro.cdm19
import com.galois.adapt.DBWritable

import scala.util.Try
import scala.collection.JavaConverters._

// No change
case class CryptographicHash(
  cryptoType: CryptoHashType,
  hash: String
) extends CDM19 with DBWritable {
  def asDBKeyValues = List(
    ("type", cryptoType.toString),
    ("hash", hash)
  )
}

case object CryptographicHash extends CDM19Constructor[CryptographicHash] {
  type RawCDMType = cdm19.CryptographicHash

  def from(cdm: RawCDM19Type): Try[CryptographicHash] = Try {
    CryptographicHash(
      cdm.getType,
      cdm.getHash
    )
  }
}
