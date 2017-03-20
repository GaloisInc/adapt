package com.galois.adapt.cdm16

import java.util.UUID

import com.bbn.tc.schema.avro.cdm16
import com.galois.adapt.{DBWritable, DBNodeable}
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class RegistryKeyObject(
  uuid: UUID,
  baseObject: AbstractObject,
  key: String,
  value: Option[Value] = None,
  size: Option[Long] = None
) extends CDM16 with DBWritable with DBNodeable {
  def asDBKeyValues = List(
    label, "RegistryKeyObject",
    "uuid", uuid,
    "key", key
  ) ++
    baseObject.asDBKeyValues ++
    value.fold[List[Any]](List.empty)(v => List("value", v.asDBKeyValues)) ++
    size.fold[List[Any]](List.empty)(v => List("size", v))

  def asDBEdges = Nil

  def getUuid = uuid
}

case object RegistryKeyObject extends CDM16Constructor[RegistryKeyObject] {
  type RawCDMType = cdm16.RegistryKeyObject

  def from(cdm: RawCDM15Type): Try[RegistryKeyObject] = Try(
    RegistryKeyObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getKey,
      AvroOpt.value(cdm.getValue),
      AvroOpt.long(cdm.getSize)
    )
  )
}
