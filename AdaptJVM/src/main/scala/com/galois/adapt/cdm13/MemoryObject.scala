package com.galois.adapt.cdm13

import java.util.UUID

import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try

case class MemoryObject(
  uuid: UUID,
  baseObject: AbstractObject,
  memoryAddress: Long,
  pageNumber: Option[Long] = None
) extends CDM13 with DBWritable {
  def asDBKeyValues = baseObject.asDBKeyValues ++ List(
    label, "MemoryObject",
    "uuid", uuid,
    "memoryAddress", memoryAddress
  )  ++
    pageNumber.fold[List[Any]](List.empty)(v => List("pageNumber", v))
}

case object MemoryObject extends CDM13Constructor[MemoryObject] {
  type RawCDMType = com.bbn.tc.schema.avro.MemoryObject

  def from(cdm: RawCDM13Type): Try[MemoryObject] = Try(
    MemoryObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getMemoryAddress,
      AvroOpt.long(cdm.getPageNumber)
    )
  )
}