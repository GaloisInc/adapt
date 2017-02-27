package com.galois.adapt.cdm15

import java.util.UUID

import com.bbn.tc.schema.avro.cdm15
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try

case class MemoryObject(
                         uuid: UUID,
                         baseObject: AbstractObject,
                         pageOffset: Long,
                         pageNumber: Option[Long] = None,
                         size: Option[Long] = None
                       ) extends CDM15 with DBWritable {
  def asDBKeyValues = baseObject.asDBKeyValues ++ List(
    label, "MemoryObject",
    "uuid", uuid,
    "pageOffset", pageOffset
  )  ++
    pageNumber.fold[List[Any]](List.empty)(v => List("pageNumber", v)) ++
    size.fold[List[Any]](List.empty)(v => List("size", v))
}

case object MemoryObject extends CDM15Constructor[MemoryObject] {
  type RawCDMType = cdm15.MemoryObject

  def from(cdm: RawCDM15Type): Try[MemoryObject] = Try(
    MemoryObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getPageOffset,
      AvroOpt.long(cdm.getPageNumber),
      AvroOpt.long(cdm.getSize)
    )
  )
}
