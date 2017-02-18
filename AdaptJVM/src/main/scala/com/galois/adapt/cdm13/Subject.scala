package com.galois.adapt.cdm13

import java.util.UUID

import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class Subject(
  uuid: UUID,
  subjectType: SubjectType,
  pid: Int,
  ppid: Int,
  source: InstrumentationSource,
  startTimestampMicros: Option[Long] = None,
  unitId: Option[Int] = None,
  endTimestampMicros: Option[Long] = None,
  cmdLine: Option[String] = None,
  importedLibraries: Option[Seq[String]] = None,
  exportedLibraries: Option[Seq[String]] = None,
  pInfo: Option[String] = None,
  properties: Option[Map[String,String]] = None
) extends CDM13 with DBWritable {
  def asDBKeyValues = List(
    label, "Subject",
    "uuid", uuid,
    "subjectType", subjectType.toString,
    "pid", pid,
    "ppid", ppid,
    "source", source.toString
  ) ++
    startTimestampMicros.fold[List[Any]](List.empty)(v => List("startTimestampMicros", v)) ++
    unitId.fold[List[Any]](List.empty)(v => List("unitId", v)) ++
    endTimestampMicros.fold[List[Any]](List.empty)(v => List("endTimestampMicros", v)) ++
    cmdLine.fold[List[Any]](List.empty)(v => List("cmdLine", v)) ++
    importedLibraries.fold[List[Any]](List.empty)(v => List("importedLibraries", v)) ++
    exportedLibraries.fold[List[Any]](List.empty)(v => List("exportedLibraries", v)) ++
    pInfo.fold[List[Any]](List.empty)(v => List("pInfo", v)) ++
    DBOpt.fromKeyValMap(properties)
}


case object Subject extends CDM13Constructor[Subject] {
  type RawCDMType = com.bbn.tc.schema.avro.cdm13.Subject

  def from(cdm: RawCDM13Type): Try[Subject] = Try {
    Subject(
      cdm.getUuid,
      cdm.getType,
      cdm.getPid,
      cdm.getPpid,
      cdm.getSource,
      AvroOpt.long(cdm.getStartTimestampMicros),
      AvroOpt.int(cdm.getUnitId),
      AvroOpt.long(cdm.getEndTimestampMicros),
      AvroOpt.str(cdm.getCmdLine),
      AvroOpt.listStr(cdm.getImportedLibraries),
      AvroOpt.listStr(cdm.getExportedLibraries),
      AvroOpt.str(cdm.getPInfo),
      AvroOpt.map(cdm.getProperties)
    )
  }
}
