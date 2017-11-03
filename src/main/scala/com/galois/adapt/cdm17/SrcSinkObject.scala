package com.galois.adapt.cdm17

import java.util.UUID

import com.bbn.tc.schema.avro.cdm17
import com.galois.adapt.{DBNodeable, DBWritable}
import com.rrwright.quine.language.{FreeDomainNode, FreeNodeConstructor}
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class SrcSinkObject(
  uuid: UUID,
  baseObject: AbstractObject,
  srcSinkType: SrcSinkType,
  fileDescriptor: Option[Int]
) extends FreeDomainNode[SrcSinkObject] with CDM17 with DBWritable with DBNodeable {

  val companion = SrcSinkObject

  def asDBKeyValues = List(
    label, "SrcSinkObject",
    "uuid", uuid,
    "srcSinkType", srcSinkType.toString
  ) ++
    baseObject.asDBKeyValues ++
    fileDescriptor.fold[List[Any]](List.empty)(v => List("fileDescriptor", v))

  def asDBEdges = Nil

  def getUuid = uuid

  def toMap: Map[String, Any] = Map(
    "uuid" -> uuid,
    "srcSinkType" -> srcSinkType,
    "fileSescriptor" -> fileDescriptor.getOrElse(""),
    "properties" -> baseObject.properties.getOrElse(Map.empty)
  )
}

case object SrcSinkObject extends FreeNodeConstructor with CDM17Constructor[SrcSinkObject] {

  type ClassType = SrcSinkObject

  type RawCDMType = cdm17.SrcSinkObject

  def from(cdm: RawCDM17Type): Try[SrcSinkObject] = Try(
    SrcSinkObject(
      cdm.getUuid,
      cdm.getBaseObject,
      cdm.getType,
      AvroOpt.int(cdm.getFileDescriptor)
    )
  )
}
