package com.galois.adapt.cdm19

import java.util.UUID

import com.bbn.tc.schema.avro.cdm19
import com.galois.adapt.{DBWritable, DBNodeable}

import scala.util.Try

// No change
case class MemoryObject(
  uuid: UUID,
  host: UUID,
  baseObject: AbstractObject,
  memoryAddress: Long,
  pageNumber: Option[Long] = None,
  pageOffset: Option[Long] = None,
  size: Option[Long] = None
) extends CDM19 with DBWritable with DBNodeable[CDM19.EdgeTypes.EdgeTypes] {

  def asDBKeyValues: List[(String, Any)] = baseObject.asDBKeyValues ++ List(
    ("uuid", uuid),
    ("memoryAddress", memoryAddress)
  )  ++
    pageNumber.fold[List[(String,Any)]](List.empty)(v => List(("pageNumber", v))) ++
    pageOffset.fold[List[(String,Any)]](List.empty)(v => List(("pageOffset", v))) ++
    size.fold[List[(String,Any)]](List.empty)(v => List(("size", v)))

  def asDBEdges = Nil

  def getUuid: UUID = uuid

  override def getHostId: Option[UUID] = Some(host)
}

case object MemoryObject extends CDM19Constructor[MemoryObject] {
  type RawCDMType = cdm19.MemoryObject

  def from(cdm: RawCDM19Type): Try[MemoryObject] = Try(
    MemoryObject(
      cdm.getUuid,
      cdm.getHostId.get,
      cdm.getBaseObject,
      cdm.getMemoryAddress,
      AvroOpt.long(cdm.getPageNumber),
      AvroOpt.long(cdm.getPageOffset),
      AvroOpt.long(cdm.getSize)
    )
  )
}
