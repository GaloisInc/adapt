package com.galois.adapt.cdm14

import java.util.UUID

import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class FileObject(
                       uuid: UUID,
                       baseObject: AbstractObject,
                       fileDescriptor: Option[Int] = None,
                       localPrincipal: Option[UUID] = None,
                       size: Option[Long] = None,
                       peInfo: Option[String] = None,
                       hashes: Option[Seq[CryptographicHash]] = None
                     ) extends CDM14 with DBWritable {
  def asDBKeyValues = baseObject.asDBKeyValues ++
    List(
      label, "FileObject",
      "uuid", uuid
    ) ++
    fileDescriptor.fold[List[Any]](List.empty)(v => List("fileDescriptor", v)) ++
    localPrincipal.fold[List[Any]](List.empty)(v => List("localPrincipal", v)) ++
    size.fold[List[Any]](List.empty)(v => List("size", v)) ++
    peInfo.fold[List[Any]](List.empty)(v => List("peInfo", v)) ++
    hashes.fold[List[Any]](List.empty)(v => List("hashes", v.mkString(", ")))
}


case object FileObject extends CDM14Constructor[FileObject] {
  type RawCDMType = com.bbn.tc.schema.avro.FileObject

  def from(cdm: RawCDM14Type): Try[FileObject] = Try(
    FileObject(
      cdm.getUuid,
      cdm.getBaseObject,
      AvroOpt.int(cdm.getFileDescriptor),
      AvroOpt.uuid(cdm.getLocalPrincipal),
      AvroOpt.long(cdm.getSize),
      AvroOpt.str(cdm.getPeInfo),
      AvroOpt.listCryptographicHash(cdm.getHashes)
    )
  )
}