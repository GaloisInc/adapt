package com.galois.adapt.cdm20
import com.bbn.tc.schema.avro.cdm20
import com.galois.adapt.DBWritable

import scala.util.Try
import scala.collection.JavaConverters._

// No change
case class CryptographicHash(
  cryptoType: CryptoHashType,
  hash: String
) extends CDM20 with DBWritable {
  def asDBKeyValues = List(
    ("type", cryptoType.toString),
    ("hash", hash)
  )
}

case object CryptographicHash extends CDM20Constructor[CryptographicHash] {
  type RawCDMType = cdm20.CryptographicHash

  def from(cdm: RawCDM20Type): Try[CryptographicHash] = Try {
    CryptographicHash(
      cdm.getType,
      cdm.getHash
    )
  }
}
