package com.galois.adapt.cdm15

import java.util.UUID

import com.bbn.tc.schema.avro.cdm15
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class ProvenanceTagNode(
                              tagId: UUID,
                              subject: UUID,
                              flowObject: Option[UUID] = None,
                              systemCall: Option[String] = None,
                              programPoint: Option[String] = None,
                              prevTagId: Option[UUID] = None,
                              opcode: Option[TagOpCode] = None,
                              tagIds: Option[Seq[UUID]] = None,
                              itag: Option[IntegrityTag] = None,
                              ctag: Option[ConfidentialityTag] = None,
                              properties: Option[Map[String,String]] = None
                            ) extends CDM15 with DBWritable {
  def asDBKeyValues = List(
    label, "ProvenanceTagNode",
    "tagId", tagId,
    "subject", subject
  ) ++
    flowObject.fold[List[Any]](List.empty)(v => List("flowObject", v)) ++
    systemCall.fold[List[Any]](List.empty)(v => List("systemCall", v)) ++
    programPoint.fold[List[Any]](List.empty)(v => List("programPoint", v)) ++
    prevTagId.fold[List[Any]](List.empty)(v => List("prevTagId", v.toString)) ++
    opcode.fold[List[Any]](List.empty)(v => List("opcode", v.toString)) ++
    tagIds.fold[List[Any]](List.empty)(v => List("tagIds", v.toString())) ++
    itag.fold[List[Any]](List.empty)(v => List("itag", v.toString)) ++
    ctag.fold[List[Any]](List.empty)(v => List("ctag", v.toString)) ++
    DBOpt.fromKeyValMap(properties)

  def asDBEdges =  List(("subject",subject)) ++
    flowObject.fold[List[(String,UUID)]](Nil)(f => List(("flowObject", f))) ++
    prevTagId.fold[List[(String,UUID)]](Nil)(p => List(("prevTagId", p))) ++
    tagIds.fold[List[(String,UUID)]](Nil)(ts => ts.toList.map(t => ("tagId", t)))

  def getUuid = tagId
}

case object ProvenanceTagNode extends CDM15Constructor[ProvenanceTagNode] {
  type RawCDMType = cdm15.ProvenanceTagNode

  def from(cdm: RawCDM15Type): Try[ProvenanceTagNode] = Try(
    ProvenanceTagNode(
      cdm.getTagId,
      cdm.getSubject,
      AvroOpt.uuid(cdm.getFlowObject),
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


case class ProvTagValueType(o: Any)   // TODO: This is cheating! Consider shapeless coproduct.
