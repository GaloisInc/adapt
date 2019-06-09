package com.galois.adapt.cdm20

import java.util.UUID
import com.bbn.tc.schema.avro.cdm20
import com.galois.adapt.{DBNodeable, DBWritable}
import com.rrwright.quine.language.NoConstantsDomainNode
import scala.util.Try


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
) extends NoConstantsDomainNode with CDM20 with DBWritable with DBNodeable[CDM20.EdgeTypes.EdgeTypes] {

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
    localPrincipal.map(p => (CDM20.EdgeTypes.localPrincipal,p))
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


case object FileObject extends CDM20Constructor[FileObject] {
  type RawCDMType = cdm20.FileObject

  def from(cdm: RawCDM20Type): Try[FileObject] = Try(
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
