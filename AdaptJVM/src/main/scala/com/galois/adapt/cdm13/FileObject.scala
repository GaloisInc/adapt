package com.galois.adapt.cdm13

import java.util.UUID

import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class FileObject(
  uuid: UUID,
  baseObject: AbstractObject,
  url: String,
  isPipe: Boolean = false,
  version: Int = 1,
  size: Option[Long] = None
) extends CDM13 with DBWritable {
  def asDBKeyValues = baseObject.asDBKeyValues ++
    List(
      label, "FileObject",
      "uuid", uuid,
//      "url", url,   // Some TA1s leave this blank. Should an empty string set no property? No. Handled below.
//      "isPipe", isPipe,  // Let's only set this key if the value is true.  See below.
      "version", version
    ) ++
    size.fold[List[Any]](List.empty)(v => List("size", v)) ++
    (if (url.nonEmpty) List("url", url) else List.empty) ++
    (if (isPipe) List("isPipe", isPipe) else List.empty)
}


case object FileObject extends CDM13Constructor[FileObject] {
  type RawCDMType = com.bbn.tc.schema.avro.FileObject

  def from(cdm: RawCDM13Type): Try[FileObject] = Try(
    FileObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getUrl,
      cdm.getIsPipe,
      cdm.getVersion,
      AvroOpt.long(cdm.getSize)
    )
  )
}