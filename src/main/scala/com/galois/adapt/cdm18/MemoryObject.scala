package com.galois.adapt.cdm18

import java.util.UUID

import com.bbn.tc.schema.avro.cdm18
import com.galois.adapt.{DBWritable, DBNodeable}

import scala.util.Try

// No change
case class MemoryObject(
  uuid: UUID,
  baseObject: AbstractObject,
  memoryAddress: Long,
  pageNumber: Option[Long] = None,
  pageOffset: Option[Long] = None,
  size: Option[Long] = None
) extends CDM18 with DBWritable with DBNodeable {

  def asDBKeyValues: List[(String, Any)] = baseObject.asDBKeyValues ++ List(
    ("uuid", uuid),
    ("memoryAddress", memoryAddress)
  )  ++
    pageNumber.fold[List[(String,Any)]](List.empty)(v => List(("pageNumber", v))) ++
    pageOffset.fold[List[(String,Any)]](List.empty)(v => List(("pageOffset", v))) ++
    size.fold[List[(String,Any)]](List.empty)(v => List(("size", v)))

  def asDBEdges = Nil

  def getUuid: UUID = uuid
}

case object MemoryObject extends CDM18Constructor[MemoryObject] {
  type RawCDMType = cdm18.MemoryObject

  def from(cdm: RawCDM18Type): Try[MemoryObject] = Try(
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
