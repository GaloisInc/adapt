package com.galois.adapt.cdm19

import java.util.UUID
import com.bbn.tc.schema.avro.cdm19
import com.galois.adapt.{DBWritable, DBNodeable}
import scala.util.Try

// No changes
case class UnitDependency(
  unit: UUID,
  dependentUnit: UUID,
  host: UUID
) extends CDM19 with DBWritable with DBNodeable[CDM19.EdgeTypes.EdgeTypes] {

  def asDBKeyValues: List[(String, Any)] = List(
    ("unitUuid", unit),
    ("dependentUnitUuid", dependentUnit)
  )

  def asDBEdges = List((CDM19.EdgeTypes.dependentUnit,dependentUnit),(CDM19.EdgeTypes.unit,unit))

  val thisUUID: UUID = UUID.randomUUID()

  def getUuid: UUID = thisUUID

  override def getHostId: Option[UUID] = Some(host)
}


case object UnitDependency extends CDM19Constructor[UnitDependency] {
  type RawCDMType = cdm19.UnitDependency

  def from(cdm: RawCDM19Type): Try[UnitDependency] = Try {
    UnitDependency(
      cdm.getUnit,
      cdm.getDependentUnit,
      cdm.getHostId.get
    )
  }
}
