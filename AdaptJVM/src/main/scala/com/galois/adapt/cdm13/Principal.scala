package com.galois.adapt.cdm13

import java.util.UUID

import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class Principal(
  uuid: UUID,
  userId: String,
  groupIds: Seq[String],
  source: InstrumentationSource,
  principalType: PrincipalType = PRINCIPAL_LOCAL,
  properties: Option[Map[String,String]] = None
) extends CDM13 with DBWritable {
  def asDBKeyValues = List(
    label, "Principal",
    "uuid", uuid,
    "userId", userId,
//    "groupIds", groupIds.mkString(", "),
    "source", source.toString,
    "principalType", principalType.toString
  ) ++
    (if (groupIds.nonEmpty) List("groupIds", groupIds.mkString(", ")) else List.empty) ++
    DBOpt.fromKeyValMap(properties)
}

case object Principal extends CDM13Constructor[Principal] {
  type RawCDMType = com.bbn.tc.schema.avro.cdm13.Principal

  def from(cdm: RawCDM13Type): Try[Principal] = Try {
    Principal(
      cdm.getUuid,
      cdm.getUserId,
      cdm.getGroupIds,
      cdm.getSource,
      cdm.getType,
      AvroOpt.map(cdm.getProperties)
    )
  }
}
