package com.galois.adapt.cdm15

import java.util.UUID

import com.bbn.tc.schema.avro.cdm15
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class Subject(
                    uuid: UUID,
                    subjectType: SubjectType,
                    cid: Int,
                    parentSubject: UUID,
                    localPrincipal: UUID,
                    source: InstrumentationSource,
                    startTimestampNanos: Long,
                    unitId: Option[Int] = None,
                    iteration: Option[Int] = None,
                    count: Option[Int] = None,
                    cmdLine: Option[String] = None,
                    privilegeLevel: Option[PrivilegeLevel] = None,
                    importedLibraries: Option[Seq[String]] = None,
                    exportedLibraries: Option[Seq[String]] = None,
                    properties: Option[Map[String,String]] = None
                  ) extends CDM15 with DBWritable {
  def asDBKeyValues = List(
    label, "Subject",
    "uuid", uuid,
    "subjectType", subjectType.toString,
    "cid", cid,
    "parentSubject", parentSubject,
    "localPrincipal", localPrincipal,
    "source", source.toString,
    "startTimestampNanos", startTimestampNanos
  ) ++
    unitId.fold[List[Any]](List.empty)(v => List("unitId", v)) ++
    iteration.fold[List[Any]](List.empty)(v => List("iteration", v)) ++
    count.fold[List[Any]](List.empty)(v => List("count", v)) ++
    cmdLine.fold[List[Any]](List.empty)(v => List("cmdLine", v)) ++
    privilegeLevel.fold[List[Any]](List.empty)(v => List("privilegeLevel", v.toString)) ++
    importedLibraries.fold[List[Any]](List.empty)(v => List("importedLibraries", v)) ++
    exportedLibraries.fold[List[Any]](List.empty)(v => List("exportedLibraries", v)) ++
    DBOpt.fromKeyValMap(properties)
}


case object Subject extends CDM15Constructor[Subject] {
  type RawCDMType = cdm15.Subject

  def from(cdm: RawCDM15Type): Try[Subject] = Try {
    Subject(
      cdm.getUuid,
      cdm.getType,
      cdm.getCid,
      cdm.getParentSubject,
      cdm.getLocalPrincipal,
      cdm.getSource,
      cdm.getStartTimestampNanos,
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
