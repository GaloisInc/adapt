package com.galois.adapt.cdm19

import java.util.UUID

import com.bbn.tc.schema.avro.cdm19
import com.galois.adapt.{DBWritable, DBNodeable}

import scala.util.Try

// No change
case class FileObject(
  uuid: UUID,
  host: UUID,
  baseObject: AbstractObject,
  fileObjectType: FileObjectType,
  fileDescriptor: Option[Int] = None,
  localPrincipal: Option[UUID] = None,
  size: Option[Long] = None,
  peInfo: Option[String] = None,
  hashes: Option[Seq[CryptographicHash]] = None
) extends CDM19 with DBWritable with DBNodeable[CDM19.EdgeTypes.EdgeTypes] {

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

  def asDBEdges =List.concat(
    localPrincipal.map(p => (CDM19.EdgeTypes.localPrincipal,p))
  )

  def getUuid = uuid

  override def getHostId: Option[UUID] = Some(host)

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


case object FileObject extends CDM19Constructor[FileObject] {
  type RawCDMType = cdm19.FileObject

  def from(cdm: RawCDM19Type): Try[FileObject] = Try(
    FileObject(
      cdm.getUuid,
      cdm.getHostId.get,
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
