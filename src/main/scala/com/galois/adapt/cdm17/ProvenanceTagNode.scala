package com.galois.adapt.cdm17

import java.util.UUID

import com.bbn.tc.schema.avro.cdm17
import com.galois.adapt.{DBNodeable, DBWritable}
import com.rrwright.quine.language.{FreeDomainNode, FreeNodeConstructor}
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class ProvenanceTagNode(
  tagIdUuid: UUID,
  subjectUuid: UUID,
  flowObject: Option[UUID] = None,
  systemCall: Option[String] = None,
  programPoint: Option[String] = None,
  prevTagId: Option[UUID] = None,
  opcode: Option[TagOpCode] = None,
  tagIds: Option[List[UUID]] = None,
  itag: Option[IntegrityTag] = None,
  ctag: Option[ConfidentialityTag] = None,
  properties: Option[Map[String,String]] = None
) extends FreeDomainNode[ProvenanceTagNode] with CDM17 with DBWritable with DBNodeable {

  val companion = ProvenanceTagNode

  def asDBKeyValues = List(
    label, "ProvenanceTagNode",
    "uuid", tagIdUuid,
    "subjectUuid", subjectUuid
  ) ++
    flowObject.fold[List[Any]](List.empty)(v => List("flowObjectUuid", v)) ++
    systemCall.fold[List[Any]](List.empty)(v => List("systemCall", v)) ++
    programPoint.fold[List[Any]](List.empty)(v => List("programPoint", v)) ++
    prevTagId.fold[List[Any]](List.empty)(v => List("prevTagIdUuid", v.toString)) ++
    opcode.fold[List[Any]](List.empty)(v => List("opcode", v.toString)) ++
    tagIds.fold[List[Any]](List.empty)(v => List("tagIds", v.toList.mkString(","))) ++
    itag.fold[List[Any]](List.empty)(v => List("itag", v.toString)) ++
    ctag.fold[List[Any]](List.empty)(v => List("ctag", v.toString)) ++
    DBOpt.fromKeyValMap(properties)

  def asDBEdges =  List(("subject",subjectUuid)) ++
    flowObject.fold[List[(String,UUID)]](Nil)(f => List(("flowObject", f))) ++
    prevTagId.fold[List[(String,UUID)]](Nil)(p => List(("prevTagId", p))) ++
    tagIds.fold[List[(String,UUID)]](Nil)(ts => ts.toList.map(t => ("tagId", t)))

  def getUuid = UUID.randomUUID()

  def toMap: Map[String, Any] = Map(
    "uuid" -> tagIdUuid,
    "subjectUuid" -> subjectUuid,
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

case object ProvenanceTagNode extends FreeNodeConstructor with CDM17Constructor[ProvenanceTagNode] {

  type ClassType = ProvenanceTagNode

  type RawCDMType = cdm17.ProvenanceTagNode

  def from(cdm: RawCDM17Type): Try[ProvenanceTagNode] = Try(
    ProvenanceTagNode(
      cdm.getTagId,
      cdm.getSubject,
      AvroOpt.uuid(cdm.getFlowObject),
      AvroOpt.str(cdm.getSystemCall),
      AvroOpt.str(cdm.getProgramPoint),
      AvroOpt.uuid(cdm.getPrevTagId),
      AvroOpt.tagOpCode(cdm.getOpcode),
      AvroOpt.listUuid(cdm.getTagIds).map(_.toList),
      AvroOpt.integrityTag(cdm.getItag),
      AvroOpt.confidentialityTag(cdm.getCtag),
      AvroOpt.map(cdm.getProperties)
    )
  )
}


//case class ProvTagValueType(o: Any)   // TODO: This is cheating! Consider shapeless coproduct.
