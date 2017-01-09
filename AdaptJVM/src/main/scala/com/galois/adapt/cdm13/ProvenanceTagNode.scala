package com.galois.adapt.cdm13

import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class ProvenanceTagNode(
  value: ProvTagValueType,
  children: Option[Seq[ProvenanceTagNode]] = None,
  tagId: Option[Int] = None,
  properties: Option[Map[String,String]] = None
) extends CDM13 with DBWritable {
  def asDBKeyValues = List(
    label, "ProvenanceTagNode",
    "value", value.o
  ) ++
  children.fold[List[Any]](List.empty)(v => List("children", v)) ++
  tagId.fold[List[Any]](List.empty)(v => List("tagId", v)) ++
  DBOpt.fromKeyValMap(properties)
}

case object ProvenanceTagNode extends CDM13Constructor[ProvenanceTagNode] {
  type RawCDMType = com.bbn.tc.schema.avro.ProvenanceTagNode

  def from(cdm: RawCDM13Type): Try[ProvenanceTagNode] = Try(
    ProvenanceTagNode(
      ProvTagValueType(Try(cdm.getValue)),
      AvroOpt.listProvTagNode(cdm.getChildren),
      AvroOpt.int(cdm.getTagId),
      AvroOpt.map(cdm.getProperties)
    )
  )
}


case class ProvTagValueType(o: Any)   // TODO: This is cheating! Consider shapeless coproduct.