package com.galois.adapt.cdm18

import java.util.UUID

import com.bbn.tc.schema.avro.cdm18
import com.galois.adapt.{DBWritable, DBNodeable}

import scala.util.Try

// No change
case class FileObject(
  uuid: UUID,
  baseObject: AbstractObject,
  fileObjectType: FileObjectType,
  fileDescriptor: Option[Int] = None,
  localPrincipal: Option[UUID] = None,
  size: Option[Long] = None,
  peInfo: Option[String] = None,
  hashes: Option[Seq[CryptographicHash]] = None
) extends CDM18 with DBWritable with DBNodeable {

  def asDBKeyValues = baseObject.asDBKeyValues ++
    List(
      ("uuid", uuid),
      ("fileObjectType", fileObjectType.toString)
    ) ++
    fileDescriptor.fold[List[(String,Any)]](List.empty)(v => List(("fileDescriptor", v))) ++
    localPrincipal.fold[List[(String,Any)]](List.empty)(v => List(("localPrincipalUuid", v))) ++
    size.fold[List[(String,Any)]](List.empty)(v => List(("size", v))) ++
    peInfo.fold[List[(String,Any)]](List.empty)(v => List(("peInfo", v))) ++
    hashes.fold[List[(String,Any)]](List.empty)(v => List(("hashes", v.map(h => s"${h.cryptoType}:${h.hash}").mkString(", "))))  // TODO: Revisit how we should represent this in the DB

  // TODO CDM18 edges
  def asDBEdges = Nil
  /* List.concat(
    localPrincipal.map(p => (CDM17.EdgeTypes.localPrincipal,p))
  ) */

  def getUuid = uuid

  def toMap: Map[String,Any] = Map(
    "uuid" -> uuid,
    "fileObjectType" -> fileObjectType,
    "fileDescriptor" -> fileDescriptor.getOrElse(""),
    "localPrincipalUuid" -> localPrincipal.getOrElse(""),
    "size" -> size.getOrElse(""),
    "peInfo" -> peInfo.getOrElse(""),
    "hashes" -> hashes.getOrElse(Seq.empty).map(h => s"${h.cryptoType}:${h.hash}").mkString("|"),
    "properties" -> baseObject.properties.getOrElse(Map.empty)
  ) //++ baseObject.properties.getOrElse(Map.empty)
}


case object FileObject extends CDM18Constructor[FileObject] {
  type RawCDMType = cdm18.FileObject

  def from(cdm: RawCDM18Type): Try[FileObject] = Try(
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
