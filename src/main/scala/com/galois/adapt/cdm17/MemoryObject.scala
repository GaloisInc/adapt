package com.galois.adapt.cdm17

import java.util.UUID

import com.bbn.tc.schema.avro.cdm17
import com.galois.adapt.{DBWritable, DBNodeable}
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try

case class MemoryObject(
  uuid: UUID,
  baseObject: AbstractObject,
  memoryAddress: Long,
  pageNumber: Option[Long] = None,
  pageOffset: Option[Long] = None,
  size: Option[Long] = None
) extends CDM17 with DBWritable with DBNodeable {
  def asDBKeyValues = baseObject.asDBKeyValues ++ List(
    label, "MemoryObject",
    "titanType", "MemoryObject",
    "uuid", uuid,
    "memoryAddress", memoryAddress
  )  ++
    pageNumber.fold[List[Any]](List.empty)(v => List("pageNumber", v)) ++
    pageOffset.fold[List[Any]](List.empty)(v => List("pageOffset", v)) ++
    size.fold[List[Any]](List.empty)(v => List("size", v))

  def asDBEdges = Nil

  def getUuid = uuid
}

case object MemoryObject extends CDM17Constructor[MemoryObject] {
  type RawCDMType = cdm17.MemoryObject

  def from(cdm: RawCDM17Type): Try[MemoryObject] = Try(
    MemoryObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getMemoryAddress,
      AvroOpt.long(cdm.getPageNumber),
      AvroOpt.long(cdm.getPageOffset),
      AvroOpt.long(cdm.getSize)
    )
  )
}
