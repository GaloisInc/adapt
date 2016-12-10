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
    val s = cdm.asType[RawCDMType]
    Subject(
      cdm.getUuid,
      SubjectType.from(s.getType.toString).get,
      s.getPid,
      s.getPpid,
      InstrumentationSource.from(s.getSource.toString).get,
      AvroOpt.long(s.getStartTimestampMicros),
      AvroOpt.int(s.getUnitId),
      AvroOpt.long(s.getEndTimestampMicros),
      AvroOpt.str(s.getCmdLine),
      AvroOpt.listStr(s.getImportedLibraries),
      AvroOpt.listStr(s.getExportedLibraries),
      AvroOpt.str(s.getPInfo),
      AvroOpt.map(s.getProperties)
    )
  }
}