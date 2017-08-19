package com.galois.adapt.cdm17

import java.util.UUID

import com.bbn.tc.schema.avro.cdm17
import com.galois.adapt.{DBNodeable, DBWritable}
import com.rrwright.quine.language.{FreeDomainNode, FreeNodeConstructor}
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class Principal(
  uuid: UUID,
  userId: String,
  groupIds: List[String],
  principalType: PrincipalType = PRINCIPAL_LOCAL,
  username: Option[String] = None,
  properties: Option[Map[String,String]] = None
) extends FreeDomainNode[Principal] with CDM17 with DBWritable with DBNodeable {

  val companion = Principal

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

  def toMap: Map[String, Any] = Map(
    "uuid" -> uuid,
    "userId" ->  userId,
    "principalType" -> principalType,
    "groupIds" -> groupIds.mkString("|"),
    "username" -> username.getOrElse(""),
    "properties" -> properties.getOrElse(Map.empty)
  )
}

case object Principal extends FreeNodeConstructor with CDM17Constructor[Principal] {
  type ClassType = Principal

  type RawCDMType = cdm17.Principal

  def from(cdm: RawCDM17Type): Try[Principal] = Try {
    Principal(
      cdm.getUuid,
      cdm.getUserId,
      cdm.getGroupIds.toList,
      cdm.getType,
      AvroOpt.str(cdm.getUsername),
      AvroOpt.map(cdm.getProperties)
    )
  }
}
