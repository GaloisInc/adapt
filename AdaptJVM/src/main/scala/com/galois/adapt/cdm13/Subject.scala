package com.galois.adapt.cdm13

import java.util.UUID
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
) extends CDM13


case object Subject extends CDM13Constructor[Subject] {
  type RawCDMType = com.bbn.tc.schema.avro.Subject

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