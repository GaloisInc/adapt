package com.galois.adapt.cdm13

import java.util.UUID
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label
import scala.util.Try


case class RegistryKeyObject(
  uuid: UUID,
  baseObject: AbstractObject,
  key: String,
  version: Int = 1,
  size: Option[Long] = None
) extends CDM13 with DBWritable {
  def asDBKeyValues = List(
    label, "RegistryKeyObject",
    "uuid", uuid,
    "key", key,
    "version", version
  ) ++
  baseObject.asDBKeyValues ++
  size.fold[List[Any]](List.empty)(v => List("size", v))
}

case object RegistryKeyObject extends CDM13Constructor[RegistryKeyObject] {
  type RawCDMType = com.bbn.tc.schema.avro.RegistryKeyObject

  def from(cdm: RawCDM13Type): Try[RegistryKeyObject] = Try(
    RegistryKeyObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getKey,
      cdm.getVersion,
      AvroOpt.long(cdm.getSize)
    )
  )
}