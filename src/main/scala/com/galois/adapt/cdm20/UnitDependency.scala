package com.galois.adapt.cdm20

import java.util.UUID
import com.bbn.tc.schema.avro.cdm20
import com.galois.adapt.{DBWritable, DBNodeable}
import scala.util.Try

// No changes
case class UnitDependency(
  unit: UUID,
  dependentUnit: UUID,
  host: UUID
) extends CDM20 with DBWritable with DBNodeable[CDM20.EdgeTypes.EdgeTypes] {

  def asDBKeyValues: List[(String, Any)] = List(
    ("unitUuid", unit),
    ("dependentUnitUuid", dependentUnit)
  )

  def asDBEdges = List((CDM20.EdgeTypes.dependentUnit,dependentUnit),(CDM20.EdgeTypes.unit,unit))

  val thisUUID: UUID = UUID.randomUUID()

  def getUuid: UUID = thisUUID

  override def getHostId: Option[UUID] = Some(host)
}


case object UnitDependency extends CDM20Constructor[UnitDependency] {
  type RawCDMType = cdm20.UnitDependency

  def from(cdm: RawCDM20Type): Try[UnitDependency] = Try {
    UnitDependency(
      cdm.getUnit,
      cdm.getDependentUnit,
      cdm.getHostId.get
    )
  }
}
