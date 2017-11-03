package com.galois.adapt.cdm17

import java.util.UUID

import com.bbn.tc.schema.avro.cdm17
import com.galois.adapt.{DBNodeable, DBWritable}
import com.rrwright.quine.language.{FreeDomainNode, FreeNodeConstructor}
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class RegistryKeyObject(
  uuid: UUID,
  baseObject: AbstractObject,
  key: String,
  value: Option[Value] = None,
  size: Option[Long] = None
) extends FreeDomainNode[RegistryKeyObject] with CDM17 with DBWritable with DBNodeable {

  val companion = RegistryKeyObject

  def asDBKeyValues = List(
    label, "RegistryKeyObject",
    "uuid", uuid,
    "registryKeyOrPath", key
  ) ++
    baseObject.asDBKeyValues ++
    value.fold[List[Any]](List.empty)(v => List("value", v.asDBKeyValues)) ++
    size.fold[List[Any]](List.empty)(v => List("size", v))

  def asDBEdges = Nil

  def getUuid = uuid

  def toMap: Map[String, Any] = Map(
//    "label" -> "RegistryKeyObject",
    "uuid" -> uuid,
    "registryKeyOrPath" -> key,
    "value" -> value.getOrElse(""),
    "size" -> size.getOrElse(""),
    "properties" -> baseObject.properties.getOrElse(Map.empty)
  ) //++ baseObject.properties.getOrElse(Map.empty)
}

case object RegistryKeyObject extends FreeNodeConstructor with CDM17Constructor[RegistryKeyObject] {

  type ClassType = RegistryKeyObject

  type RawCDMType = cdm17.RegistryKeyObject

  def from(cdm: RawCDM17Type): Try[RegistryKeyObject] = Try(
    RegistryKeyObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getKey,
      AvroOpt.value(cdm.getValue),
      AvroOpt.long(cdm.getSize)
    )
  )
}
