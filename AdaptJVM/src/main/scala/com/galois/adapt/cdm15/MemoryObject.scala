package com.galois.adapt.cdm15

import java.util.UUID

import com.bbn.tc.schema.avro.cdm15
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try

case class MemoryObject(
                         uuid: UUID,
                         baseObject: AbstractObject,
                         memoryAddress: Long,
                         pageNumber: Option[Long] = None,
                         pageOffset: Option[Long] = None,
                         size: Option[Long] = None
                       ) extends CDM15 with DBWritable {
  def asDBKeyValues = baseObject.asDBKeyValues ++ List(
    label, "MemoryObject",
    "uuid", uuid,
    "memoryAddress", memoryAddress
  )  ++
    pageNumber.fold[List[Any]](List.empty)(v => List("pageNumber", v)) ++
    pageOffset.fold[List[Any]](List.empty)(v => List("pageOffset", v)) ++
    size.fold[List[Any]](List.empty)(v => List("size", v))

  def asDBEdges = Nil

  def getUuid = uuid
}

case object MemoryObject extends CDM15Constructor[MemoryObject] {
  type RawCDMType = cdm15.MemoryObject

  def from(cdm: RawCDM15Type): Try[MemoryObject] = Try(
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
