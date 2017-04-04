package com.galois.adapt.cdm17

import java.util.UUID

import com.bbn.tc.schema.avro.cdm17
import com.galois.adapt.{DBWritable, DBNodeable}
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class Principal(
  uuid: UUID,
  userId: String,
  groupIds: Seq[String],
  principalType: PrincipalType = PRINCIPAL_LOCAL,
  username: Option[String] = None,
  properties: Option[Map[String,String]] = None
) extends CDM17 with DBWritable with DBNodeable {
  def asDBKeyValues = List(
    label, "Principal",
    "uuid", uuid,
    "userId", userId,
    //    "groupIds", groupIds.mkString(", "),
    "principalType", principalType.toString
  ) ++
    (if (groupIds.nonEmpty) List("groupIds", groupIds.mkString(", ")) else List.empty) ++
    username.fold[List[Any]](List.empty)(v => List("username", v)) ++
    DBOpt.fromKeyValMap(properties)

  def asDBEdges = Nil

  def getUuid = uuid
}

case object Principal extends CDM17Constructor[Principal] {
  type RawCDMType = cdm17.Principal

  def from(cdm: RawCDM15Type): Try[Principal] = Try {
    Principal(
      cdm.getUuid,
      cdm.getUserId,
      cdm.getGroupIds,
      cdm.getType,
      AvroOpt.str(cdm.getUsername),
      AvroOpt.map(cdm.getProperties)
    )
  }
}
