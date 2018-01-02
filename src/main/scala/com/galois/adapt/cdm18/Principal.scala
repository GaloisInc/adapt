package com.galois.adapt.cdm18

import java.util.UUID

import com.bbn.tc.schema.avro.cdm18
import com.galois.adapt.{DBNodeable, DBWritable}

import scala.util.Try


case class Principal(
  uuid: UUID,
  userId: String,
  groupIds: Seq[String],
  principalType: PrincipalType = PRINCIPAL_LOCAL,
  host: UUID, // Host where principal exists
  username: Option[String] = None,
  properties: Option[Map[String,String]] = None
) extends CDM18 with DBWritable with DBNodeable[CDM18.EdgeTypes.EdgeTypes] {

  def asDBKeyValues = List(
    ("uuid", uuid),
    ("userId", userId),
    ("principalType", principalType.toString)
  ) ++
    (if (groupIds.nonEmpty) List(("groupIds", groupIds.mkString(", "))) else List.empty) ++
    username.fold[List[(String,Any)]](List.empty)(v => List(("username", v))) ++
    DBOpt.fromKeyValMap(properties)
  
  def asDBEdges = List((CDM18.EdgeTypes.host,host))

  def getUuid = uuid

  def toMap: Map[String, Any] = Map(
    "uuid" -> uuid,
    "userId" ->  userId,
    "principalType" -> principalType,
    "host" -> host,
    "groupIds" -> groupIds.mkString("|"),
    "username" -> username.getOrElse(""),
    "properties" -> properties.getOrElse(Map.empty)
  )
}

case object Principal extends CDM18Constructor[Principal] {
  type RawCDMType = cdm18.Principal

  def from(cdm: RawCDM18Type): Try[Principal] = Try {
    Principal(
      cdm.getUuid,
      cdm.getUserId,
      cdm.getGroupIds,
      cdm.getType,
      cdm.getHostId,
      AvroOpt.str(cdm.getUsername),
      AvroOpt.map(cdm.getProperties)
    )
  }
}
