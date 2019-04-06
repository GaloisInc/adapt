package com.galois.adapt.cdm20

import java.util.UUID

import com.bbn.tc.schema.avro.cdm20
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
) extends CDM20 with DBWritable with DBNodeable[CDM20.EdgeTypes.EdgeTypes] {

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

case object MemoryObject extends CDM20Constructor[MemoryObject] {
  type RawCDMType = cdm20.MemoryObject

  def from(cdm: RawCDM20Type): Try[MemoryObject] = Try(
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
