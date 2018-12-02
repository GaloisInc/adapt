package com.galois.adapt.cdm19

import java.util.UUID

import com.bbn.tc.schema.avro.cdm19
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
) extends CDM19 with DBWritable with DBNodeable[CDM19.EdgeTypes.EdgeTypes] {

  def asDBKeyValues = List(
    ("uuid", uuid),
    ("userId", userId),
    ("principalType", principalType.toString),
    ("host", host)
  ) ++
    (if (groupIds.nonEmpty) List(("groupIds", groupIds.mkString(", "))) else List.empty) ++
    username.fold[List[(String,Any)]](List.empty)(v => List(("username", v))) ++
    DBOpt.fromKeyValMap(properties)
  
  def asDBEdges = List(
//    (CDM19.EdgeTypes.host,host)
  )

  def getUuid = uuid

  override def getHostId: Option[UUID] = Some(host)

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

case object Principal extends CDM19Constructor[Principal] {
  type RawCDMType = cdm19.Principal

  def from(cdm: RawCDM19Type): Try[Principal] = Try {
    Principal(
      cdm.getUuid,
      cdm.getUserId,
      AvroOpt.listStr(cdm.getGroupIds).getOrElse(Seq.empty),
      cdm.getType,
      cdm.getHostId.get,
      AvroOpt.str(cdm.getUsername),
      AvroOpt.map(cdm.getProperties)
    )
  }
}
