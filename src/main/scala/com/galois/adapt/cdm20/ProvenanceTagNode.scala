package com.galois.adapt.cdm20

import java.util.UUID
import com.bbn.tc.schema.avro.cdm20
import com.galois.adapt.{DBNodeable, DBWritable}
import com.rrwright.quine.language.NoConstantsDomainNode
import scala.util.Try


case class ProvenanceTagNode(
  tagIdUuid: UUID,
  subjectUuid: UUID,
  flowObject: Option[UUID] = None,
  host: UUID,  // Host on which the src/sink action is occuring
  systemCall: Option[String] = None,
  programPoint: Option[String] = None,
  prevTagId: Option[UUID] = None,
  opcode: Option[TagOpCode] = None,
  tagIds: Option[Seq[UUID]] = None,
  itag: Option[IntegrityTag] = None,
  ctag: Option[ConfidentialityTag] = None,
  properties: Option[Map[String,String]] = None
) extends NoConstantsDomainNode with CDM20 with DBWritable with DBNodeable[CDM20.EdgeTypes.EdgeTypes] {

  def asDBKeyValues = List(
    ("uuid", tagIdUuid),
    ("subjectUuid", subjectUuid),
    ("host", host)
  ) ++
    flowObject.fold[List[(String,Any)]](List.empty)(v => List(("flowObjectUuid", v))) ++
    systemCall.fold[List[(String,Any)]](List.empty)(v => List(("systemCall", v))) ++
    programPoint.fold[List[(String,Any)]](List.empty)(v => List(("programPoint", v))) ++
    prevTagId.fold[List[(String,Any)]](List.empty)(v => List(("prevTagIdUuid", v.toString))) ++
    opcode.fold[List[(String,Any)]](List.empty)(v => List(("opcode", v.toString))) ++
    tagIds.fold[List[(String,Any)]](List.empty)(v => List(("tagIds", v.toList.mkString(",")))) ++
    itag.fold[List[(String,Any)]](List.empty)(v => List(("itag", v.toString))) ++
    ctag.fold[List[(String,Any)]](List.empty)((v => List(("ctag", v.toString)))) ++
    DBOpt.fromKeyValMap(properties)

  def asDBEdges = Nil
    List((CDM20.EdgeTypes.subject,subjectUuid)) ++
//    List((CDM20.EdgeTypes.host,host)) ++
    flowObject.fold[List[(CDM20.EdgeTypes.EdgeTypes,UUID)]](Nil)(f => List((CDM20.EdgeTypes.flowObject, f))) ++
    prevTagId.fold[List[(CDM20.EdgeTypes.EdgeTypes,UUID)]](Nil)(p => List((CDM20.EdgeTypes.prevTagId, p))) ++
    tagIds.fold[List[(CDM20.EdgeTypes.EdgeTypes,UUID)]](Nil)(ts => ts.toList.map(t => (CDM20.EdgeTypes.tagId, t)))

  def getUuid = tagIdUuid

  override def getHostId: Option[UUID] = Some(host)

  def toMap: Map[String, Any] = Map(
    "uuid" -> tagIdUuid,
    "subjectUuid" -> subjectUuid,
    "host" -> host,
    "flowObjectUuid" -> flowObject.getOrElse(""),
    "systemCall" -> systemCall.getOrElse(""),
    "programPoint" -> programPoint.getOrElse(""),
    "prevTagIdUuid" -> prevTagId.getOrElse(""),
    "opcode" -> opcode.getOrElse(""),
    "tagIds" -> tagIds.getOrElse(Seq.empty).mkString("|"),
    "itag" -> itag.getOrElse(""),
    "ctag" -> ctag.getOrElse(""),
    "properties" -> properties.getOrElse(Map.empty)
  )
}

case object ProvenanceTagNode extends CDM20Constructor[ProvenanceTagNode] {
  type RawCDMType = cdm20.ProvenanceTagNode

  def from(cdm: RawCDM20Type): Try[ProvenanceTagNode] = Try(
    ProvenanceTagNode(
      cdm.getTagId,
      cdm.getSubject,
      AvroOpt.uuid(cdm.getFlowObject),
      cdm.getHostId.get,
      AvroOpt.str(cdm.getSystemCall),
      AvroOpt.str(cdm.getProgramPoint),
      AvroOpt.uuid(cdm.getPrevTagId),
      AvroOpt.tagOpCode(cdm.getOpcode),
      AvroOpt.listUuid(cdm.getTagIds),
      AvroOpt.integrityTag(cdm.getItag),
      AvroOpt.confidentialityTag(cdm.getCtag),
      AvroOpt.map(cdm.getProperties)
    )
  )
}


//case class ProvTagValueType(o: Any)   // TODO: This is cheating! Consider shapeless coproduct.
