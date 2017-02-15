package com.galois.adapt.cdm14

import java.util.UUID

import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try

case class MemoryObject(
                         uuid: UUID,
                         baseObject: AbstractObject,
                         pageOffset: Long,
                         pageNumber: Option[Long] = None,
                         size: Option[Long] = None
                       ) extends CDM14 with DBWritable {
  def asDBKeyValues = baseObject.asDBKeyValues ++ List(
    label, "MemoryObject",
    "uuid", uuid,
    "pageOffset", pageOffset
  )  ++
    pageNumber.fold[List[Any]](List.empty)(v => List("pageNumber", v)) ++
    size.fold[List[Any]](List.empty)(v => List("size", v))
}

case object MemoryObject extends CDM14Constructor[MemoryObject] {
  type RawCDMType = com.bbn.tc.schema.avro.MemoryObject

  def from(cdm: RawCDM14Type): Try[MemoryObject] = Try(
    MemoryObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getPageOffset,
      AvroOpt.long(cdm.getPageNumber),
      AvroOpt.long(cdm.getSize)
    )
  )
}