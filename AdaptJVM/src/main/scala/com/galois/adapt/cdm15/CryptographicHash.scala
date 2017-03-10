package com.galois.adapt.cdm15
import com.bbn.tc.schema.avro.cdm15
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try
import scala.collection.JavaConverters._


case class CryptographicHash(
                           cryptoType: CryptoHashType,
                           hash: String
                         ) extends CDM15 with DBWritable {
  def asDBKeyValues = List(
    //    label, "CryptographicHash",
    "type", cryptoType.toString,
    "hash", hash
  )

  def asDBEdges = Nil

  def getUuid = throw new RuntimeException("CryptographicHash has no UUID")
}

case object CryptographicHash extends CDM15Constructor[CryptographicHash] {
  type RawCDMType = cdm15.CryptographicHash

  def from(cdm: RawCDM15Type): Try[CryptographicHash] = Try {
    CryptographicHash(
      cdm.getType,
      cdm.getHash
    )
  }
}