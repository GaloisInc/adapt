package com.galois.adapt.cdm17

import java.util.UUID

import com.bbn.tc.schema.avro.cdm17
import com.galois.adapt.{DBNodeable, DBWritable}
import com.rrwright.quine.language.{FreeDomainNode, FreeNodeConstructor}
import scala.util.Try


case class Subject(
  uuid: UUID,
  subjectType: SubjectType,
  cid: Int,
  localPrincipal: UUID,
  startTimestampNanos: Long,
  parentSubject: Option[UUID] = None,
  unitId: Option[Int] = None,
  iteration: Option[Int] = None,
  count: Option[Int] = None,
  cmdLine: Option[String] = None,
  privilegeLevel: Option[PrivilegeLevel] = None,
  importedLibraries: Option[List[String]] = None,
  exportedLibraries: Option[List[String]] = None,
  properties: Option[Map[String,String]] = None
) extends FreeDomainNode[Subject] with CDM17 with DBWritable with DBNodeable[CDM17.EdgeTypes.EdgeTypes] {

  val companion = Subject

  def asDBKeyValues = List(
    ("uuid", uuid),
    ("subjectType", subjectType.toString),
    ("cid", cid),
    ("localPrincipalUuid", localPrincipal),
    ("startTimestampNanos", startTimestampNanos)
  ) ++
    parentSubject.fold[List[(String,Any)]](List.empty)(v => List(("parentSubjectUuid", v))) ++
    unitId.fold[List[(String,Any)]](List.empty)(v => List(("unitId", v))) ++
    iteration.fold[List[(String,Any)]](List.empty)(v => List(("iteration", v))) ++
    count.fold[List[(String,Any)]](List.empty)(v => List(("count", v))) ++
    cmdLine.fold[List[(String,Any)]](List.empty)(v => List(("cmdLine", v))) ++
    privilegeLevel.fold[List[(String,Any)]](List.empty)(v => List(("privilegeLevel", v.toString))) ++
    importedLibraries.fold[List[(String,Any)]](List.empty)(v => if (v.isEmpty) List.empty else List(("importedLibraries", v.mkString(", ")))) ++
    exportedLibraries.fold[List[(String,Any)]](List.empty)(v => if (v.isEmpty) List.empty else List(("exportedLibraries", v.mkString(", ")))) ++
    DBOpt.fromKeyValMap(properties)

  def asDBEdges = List((CDM17.EdgeTypes.localPrincipal,localPrincipal)) ++
    parentSubject.fold[List[(CDM17.EdgeTypes.EdgeTypes,UUID)]](Nil)(v => List((CDM17.EdgeTypes.parentSubject, v)))

  def getUuid = uuid

  def toMap: Map[String, Any] = Map(
    "uuid" -> uuid,
    "subjectType" -> subjectType.toString,
    "cid" -> cid,
    "localPrincipalUuid" -> localPrincipal,
    "startTimestampNanos" -> startTimestampNanos,
    "parentSubjectUuid" -> parentSubject.getOrElse(""),
    "unitId" -> unitId.getOrElse(""),
    "iteration" -> iteration.getOrElse(""),
    "count" -> count.getOrElse(""),
    "cmdLine" -> cmdLine.getOrElse(""),
    "privilegeLevel" -> privilegeLevel.getOrElse(""),
    "importedLibraries" -> importedLibraries.getOrElse(Seq.empty).mkString("|"),
    "importedLibraries" -> exportedLibraries.getOrElse(Seq.empty).mkString("|"),
    "properties" -> properties.getOrElse(Map.empty)
  )
}


case object Subject extends FreeNodeConstructor with CDM17Constructor[Subject] {
  type ClassType = Subject

  type RawCDMType = cdm17.Subject

  def from(cdm: RawCDM17Type): Try[Subject] = Try {
    Subject(
      cdm.getUuid,
      cdm.getType,
      cdm.getCid,
      cdm.getLocalPrincipal,
      cdm.getStartTimestampNanos,
      AvroOpt.uuid(cdm.getParentSubject),
      AvroOpt.int(cdm.getUnitId),
      AvroOpt.int(cdm.getIteration),
      AvroOpt.int(cdm.getCount),
      AvroOpt.str(cdm.getCmdLine),
      AvroOpt.privilegeLevel(cdm.getPrivilegeLevel),
      AvroOpt.listStr(cdm.getImportedLibraries),
      AvroOpt.listStr(cdm.getExportedLibraries),
      AvroOpt.map(cdm.getProperties)
    )
  }
}
