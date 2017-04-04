package com.galois.adapt.cdm17
import com.bbn.tc.schema.avro.cdm17
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try
import scala.collection.JavaConverters._


case class CryptographicHash(
  cryptoType: CryptoHashType,
  hash: String
) extends CDM17 with DBWritable {
  def asDBKeyValues = List(
    //    label, "CryptographicHash",
    "type", cryptoType.toString,
    "hash", hash
  )
}

case object CryptographicHash extends CDM17Constructor[CryptographicHash] {
  type RawCDMType = cdm17.CryptographicHash

  def from(cdm: RawCDM15Type): Try[CryptographicHash] = Try {
    CryptographicHash(
      cdm.getType,
      cdm.getHash
    )
  }
}
