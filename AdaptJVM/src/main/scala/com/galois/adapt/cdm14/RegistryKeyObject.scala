package com.galois.adapt.cdm14

import java.util.UUID
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label
import scala.util.Try


case class RegistryKeyObject(
                              uuid: UUID,
                              baseObject: AbstractObject,
                              key: String,
                              value: Option[Value] = None,
                              size: Option[Long] = None
                            ) extends CDM14 with DBWritable {
  def asDBKeyValues = List(
    label, "RegistryKeyObject",
    "uuid", uuid,
    "key", key
  ) ++
    baseObject.asDBKeyValues ++
    // TODO value.asDBKeyValues ++
    size.fold[List[Any]](List.empty)(v => List("size", v))
}

case object RegistryKeyObject extends CDM14Constructor[RegistryKeyObject] {
  type RawCDMType = com.bbn.tc.schema.avro.RegistryKeyObject

  def from(cdm: RawCDM14Type): Try[RegistryKeyObject] = Try(
    RegistryKeyObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getKey,
      AvroOpt.value(cdm.getValue),
      AvroOpt.long(cdm.getSize)
    )
  )
}