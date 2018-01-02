package com.galois.adapt.cdm18
import com.bbn.tc.schema.avro.cdm18
import com.galois.adapt.DBWritable

import scala.util.Try
import scala.collection.JavaConverters._

// No change
case class CryptographicHash(
  cryptoType: CryptoHashType,
  hash: String
) extends CDM18 with DBWritable {
  def asDBKeyValues = List(
    ("type", cryptoType.toString),
    ("hash", hash)
  )
}

case object CryptographicHash extends CDM18Constructor[CryptographicHash] {
  type RawCDMType = cdm18.CryptographicHash

  def from(cdm: RawCDM18Type): Try[CryptographicHash] = Try {
    CryptographicHash(
      cdm.getType,
      cdm.getHash
    )
  }
}
