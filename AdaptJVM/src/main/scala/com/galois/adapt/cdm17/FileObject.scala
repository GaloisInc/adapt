package com.galois.adapt.cdm17

import java.util.UUID

import com.bbn.tc.schema.avro.cdm17
import com.galois.adapt.{DBWritable, DBNodeable}
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class FileObject(
  uuid: UUID,
  baseObject: AbstractObject,
  fileObjectType: FileObjectType,
  fileDescriptor: Option[Int] = None,
  localPrincipal: Option[UUID] = None,
  size: Option[Long] = None,
  peInfo: Option[String] = None,
  hashes: Option[Seq[CryptographicHash]] = None
) extends CDM17 with DBWritable with DBNodeable {
  def asDBKeyValues = baseObject.asDBKeyValues ++
    List(
      label, "FileObject",
      "uuid", uuid,
      "fileObjectType", fileObjectType.toString
    ) ++
    fileDescriptor.fold[List[Any]](List.empty)(v => List("fileDescriptor", v)) ++
    localPrincipal.fold[List[Any]](List.empty)(v => List("localPrincipal", v)) ++
    size.fold[List[Any]](List.empty)(v => List("size", v)) ++
    peInfo.fold[List[Any]](List.empty)(v => List("peInfo", v)) ++
    hashes.fold[List[Any]](List.empty)(v => List("hashes", v.map(h => s"${h.cryptoType}:${h.hash}").mkString(", ")))  // TODO: Revisit how we should represent this in the DB

  def asDBEdges = List.concat(
    localPrincipal.map(p => ("localPrincipal",p))
  )

  def getUuid = uuid
}


case object FileObject extends CDM17Constructor[FileObject] {
  type RawCDMType = cdm17.FileObject

  def from(cdm: RawCDM17Type): Try[FileObject] = Try(
    FileObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getType,
      AvroOpt.int(cdm.getFileDescriptor),
      AvroOpt.uuid(cdm.getLocalPrincipal),
      AvroOpt.long(cdm.getSize),
      AvroOpt.str(cdm.getPeInfo),
      AvroOpt.listCryptographicHash(cdm.getHashes)
    )
  )
}
