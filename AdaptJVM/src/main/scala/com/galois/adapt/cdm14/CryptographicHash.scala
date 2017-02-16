package com.galois.adapt.cdm14
import com.bbn.tc.schema.avro.cdm14
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try
import scala.collection.JavaConverters._


case class CryptographicHash(
                           cryptoType: CryptoHashType,
                           hash: String
                         ) extends CDM14 with DBWritable {
  def asDBKeyValues = List(
    //    label, "CryptographicHash",
    "type", cryptoType.toString,
    "hash", hash
  )
}

case object CryptographicHash extends CDM14Constructor[CryptographicHash] {
  type RawCDMType = cdm14.CryptographicHash

  def from(cdm: RawCDM14Type): Try[CryptographicHash] = Try {
    CryptographicHash(
      cdm.getType,
      cdm.getHash
    )
  }
}