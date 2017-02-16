package com.galois.adapt.cdm14

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
                      username: Option[String] = None,
                      properties: Option[Map[String,String]] = None
                    ) extends CDM14 with DBWritable {
  def asDBKeyValues = List(
    label, "Principal",
    "uuid", uuid,
    "userId", userId,
    //    "groupIds", groupIds.mkString(", "),
    "source", source.toString,
    "principalType", principalType.toString
  ) ++
    (if (groupIds.nonEmpty) List("groupIds", groupIds.mkString(", ")) else List.empty) ++
    username.fold[List[Any]](List.empty)(v => List("username", v)) ++
    DBOpt.fromKeyValMap(properties)
}

case object Principal extends CDM14Constructor[Principal] {
  type RawCDMType = com.bbn.tc.schema.avro.Principal

  def from(cdm: RawCDM14Type): Try[Principal] = Try {
    Principal(
      cdm.getUuid,
      cdm.getUserId,
      cdm.getGroupIds,
      cdm.getSource,
      cdm.getType,
      AvroOpt.str(cdm.getUsername),
      AvroOpt.map(cdm.getProperties)
    )
  }
}