package com.galois.adapt.cdm20

import java.util.UUID

import com.bbn.tc.schema.avro.cdm20
import com.galois.adapt.{DBWritable, DBNodeable}

import scala.util.Try


case class UnknownProvenanceNode(
  tagIdUuid: UUID,
  subjectUuid: Option[UUID] = None,
  programPoint: Option[String] = None,
  properties: Option[Map[String,String]] = None
) extends CDM20 with DBWritable with DBNodeable[CDM20.EdgeTypes.EdgeTypes] {

  def asDBKeyValues = List(
    ("uuid", tagIdUuid)
  ) ++
    subjectUuid.fold[List[(String,Any)]](List.empty)(v => List(("subjectUuid", v))) ++
    programPoint.fold[List[(String,Any)]](List.empty)(v => List(("programPoint", v))) ++
    DBOpt.fromKeyValMap(properties)

  def asDBEdges =
    subjectUuid.fold[List[(CDM20.EdgeTypes.EdgeTypes,UUID)]](Nil)(f => List((CDM20.EdgeTypes.subject, f)))

  def getUuid = tagIdUuid

  override def getHostId: Option[UUID] = None

  def toMap: Map[String, Any] = Map(
    "uuid" -> tagIdUuid,
    "subjectUuid" -> subjectUuid,
    "programPoint" -> programPoint.getOrElse(""),
    "properties" -> properties.getOrElse(Map.empty)
  )
}

case object UnknownProvenanceNode extends CDM20Constructor[UnknownProvenanceNode] {
  type RawCDMType = cdm20.UnknownProvenanceNode

  def from(cdm: RawCDM20Type): Try[UnknownProvenanceNode] = Try(
    UnknownProvenanceNode(
      cdm.getUpnTagId,
      AvroOpt.uuid(cdm.getSubject),
      AvroOpt.str(cdm.getProgramPoint),
      AvroOpt.map(cdm.getProperties)
    )
  )
}

