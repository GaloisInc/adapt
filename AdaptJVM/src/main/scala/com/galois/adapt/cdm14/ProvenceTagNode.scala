package com.galois.adapt.cdm14

import java.util.UUID

import com.bbn.tc.schema.avro.cdm14
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class ProvenanceTagNode(
                              tagId: UUID,
                              programPoint: Option[String] = None,
                              prevTagId: Option[UUID] = None,
                              opcode: Option[TagOpCode] = None,
                              tagIds: Option[Seq[UUID]] = None,
                              itag: Option[IntegrityTag] = None,
                              ctag: Option[ConfidentialityTag] = None,
                              properties: Option[Map[String,String]] = None
                            ) extends CDM14 with DBWritable {
  def asDBKeyValues = List(
    label, "ProvenanceTagNode",
    "tagId", tagId
  ) ++
    programPoint.fold[List[Any]](List.empty)(v => List("programPoint", v)) ++
    prevTagId.fold[List[Any]](List.empty)(v => List("prevTagId", v.toString)) ++
    opcode.fold[List[Any]](List.empty)(v => List("opcode", v.toString)) ++
    tagIds.fold[List[Any]](List.empty)(v => List("tagIds", v.toString())) ++
    itag.fold[List[Any]](List.empty)(v => List("itag", v.toString)) ++
    ctag.fold[List[Any]](List.empty)(v => List("ctag", v.toString)) ++
    DBOpt.fromKeyValMap(properties)
}

case object ProvenanceTagNode extends CDM14Constructor[ProvenanceTagNode] {
  type RawCDMType = cdm14.ProvenanceTagNode

  def from(cdm: RawCDM14Type): Try[ProvenanceTagNode] = Try(
    ProvenanceTagNode(
      cdm.getTagId,
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