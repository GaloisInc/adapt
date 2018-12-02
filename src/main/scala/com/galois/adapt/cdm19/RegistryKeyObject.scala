package com.galois.adapt.cdm19

import java.util.UUID

import com.bbn.tc.schema.avro.cdm19
import com.galois.adapt.{DBWritable, DBNodeable}
import scala.util.Try

// No change
case class RegistryKeyObject(
  uuid: UUID,
  host: UUID,
  baseObject: AbstractObject,
  key: String,
  value: Option[Value] = None,
  size: Option[Long] = None
) extends CDM19 with DBWritable with DBNodeable[CDM19.EdgeTypes.EdgeTypes] {

  def asDBKeyValues: List[(String, Any)] = List(
    ("uuid", uuid),
    ("registryKeyOrPath", key)
  ) ++
    baseObject.asDBKeyValues ++
    value.fold[List[(String,Any)]](List.empty)(v => List(("value", v.toString))) ++
    size.fold[List[(String,Any)]](List.empty)(v => List(("size", v)))

  def asDBEdges = Nil

  def getUuid: UUID = uuid

  override def getHostId: Option[UUID] = Some(host)

  def toMap: Map[String, Any] = Map(
//    "label" -> "RegistryKeyObject",
    "uuid" -> uuid,
    "registryKeyOrPath" -> key,
    "value" -> value.getOrElse(""),
    "size" -> size.getOrElse(""),
    "properties" -> baseObject.properties.getOrElse(Map.empty)
  ) //++ baseObject.properties.getOrElse(Map.empty)
}

case object RegistryKeyObject extends CDM19Constructor[RegistryKeyObject] {
  type RawCDMType = cdm19.RegistryKeyObject

  def from(cdm: RawCDM19Type): Try[RegistryKeyObject] = Try(
    RegistryKeyObject(
      cdm.getUuid,
      cdm.getHostId.get,
      cdm.getBaseObject,
      cdm.getKey,
      AvroOpt.value(cdm.getValue),
      AvroOpt.long(cdm.getSize)
    )
  )
}
