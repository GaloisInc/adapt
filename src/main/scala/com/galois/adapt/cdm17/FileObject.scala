package com.galois.adapt.cdm17

import java.util.UUID

import com.bbn.tc.schema.avro.cdm17
import com.galois.adapt.{DBNodeable, DBWritable}
import scala.util.Try
import com.rrwright.quine.language._
import com.rrwright.quine.language.EdgeDirections._


case class FileObject(
  uuid: UUID,
  baseObject: AbstractObject,
  fileObjectType: FileObjectType,
  fileDescriptor: Option[Int] = None,
  localPrincipal: Option[-->[UUID]] = None,
  size: Option[Long] = None,
  peInfo: Option[String] = None,
  hashes: Option[Seq[CryptographicHash]] = None
) extends NoConstantsDomainNode
  with CDM17 with DBWritable with DBNodeable[CDM17.EdgeTypes.EdgeTypes] {

  val companion = FileObject

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

  def asDBEdges = List.concat(
    localPrincipal.map(p => (CDM17.EdgeTypes.localPrincipal,p.target))
  )

  def getUuid = uuid

  def toMap: Map[String,Any] = Map(
//    "label" -> "FileObject",
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


case object FileObject extends CDM17Constructor[FileObject] {
  type ClassType = FileObject

  type RawCDMType = cdm17.FileObject

  def from(cdm: RawCDM17Type): Try[FileObject] = Try(
    FileObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getType,
      AvroOpt.int(cdm.getFileDescriptor),
      AvroOpt.uuid(cdm.getLocalPrincipal).map(u => toOutgoingId(u)),
      AvroOpt.long(cdm.getSize),
      AvroOpt.str(cdm.getPeInfo),
      AvroOpt.listCryptographicHash(cdm.getHashes)
    )
  )
}
