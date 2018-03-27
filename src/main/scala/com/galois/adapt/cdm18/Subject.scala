package com.galois.adapt.cdm18

import java.util.UUID

import com.bbn.tc.schema.avro.cdm18
import com.galois.adapt.{DBWritable, DBNodeable}

import scala.util.Try


case class Subject(
  uuid: UUID,
  subjectType: SubjectType,
  cid: Int,
  localPrincipal: UUID,
  startTimestampNanos: Long,
  parentSubject: Option[UUID] = None,
  host: UUID, // Host where subject is executing
  unitId: Option[Int] = None,
  iteration: Option[Int] = None,
  count: Option[Int] = None,
  cmdLine: Option[String] = None,
  privilegeLevel: Option[PrivilegeLevel] = None,
  importedLibraries: Option[Seq[String]] = None,
  exportedLibraries: Option[Seq[String]] = None,
  properties: Option[Map[String,String]] = None
) extends CDM18 with DBWritable with DBNodeable[CDM18.EdgeTypes.EdgeTypes] {

  def asDBKeyValues: List[(String, Any)] = List(
    ("uuid", uuid),
    ("subjectType", subjectType.toString),
    ("cid", cid),
    ("localPrincipalUuid", localPrincipal),
    ("startTimestampNanos", startTimestampNanos),
    ("host", host)
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

  def asDBEdges =
    List((CDM18.EdgeTypes.localPrincipal,localPrincipal)) ++
//    List((CDM18.EdgeTypes.host,host)) ++
    parentSubject.fold[List[(CDM18.EdgeTypes.EdgeTypes,UUID)]](Nil)(v => List((CDM18.EdgeTypes.parentSubject, v)))


  def getUuid: UUID = uuid

  def toMap: Map[String, Any] = Map(
    "uuid" -> uuid,
    "subjectType" -> subjectType.toString,
    "cid" -> cid,
    "localPrincipalUuid" -> localPrincipal,
    "startTimestampNanos" -> startTimestampNanos,
    "parentSubjectUuid" -> parentSubject.getOrElse(""),
    "host" -> host,
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


case object Subject extends CDM18Constructor[Subject] {
  type RawCDMType = cdm18.Subject

  def from(cdm: RawCDM18Type): Try[Subject] = Try {
    Subject(
      cdm.getUuid,
      cdm.getType,
      cdm.getCid,
      cdm.getLocalPrincipal,
      cdm.getStartTimestampNanos,
      AvroOpt.uuid(cdm.getParentSubject),
      cdm.getHostId,
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
