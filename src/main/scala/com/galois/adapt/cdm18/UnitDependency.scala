package com.galois.adapt.cdm18

import java.util.UUID
import com.bbn.tc.schema.avro.cdm18
import com.galois.adapt.{DBWritable, DBNodeable}
import scala.util.Try

// No changes
case class UnitDependency(
  unit: UUID,
  dependentUnit: UUID
) extends CDM18 with DBWritable with DBNodeable {

  def asDBKeyValues: List[(String, Any)] = List(
    ("unitUuid", unit),
    ("dependentUnitUuid", dependentUnit)
  )

  // TODO cdm18 edges
  def asDBEdges = Nil //. List((CDM17.EdgeTypes.dependentUnit,dependentUnit),(CDM17.EdgeTypes.unit,unit))

  val thisUUID: UUID = UUID.randomUUID()

  def getUuid: UUID = thisUUID
}


case object UnitDependency extends CDM18Constructor[UnitDependency] {
  type RawCDMType = cdm18.UnitDependency

  def from(cdm: RawCDM18Type): Try[UnitDependency] = Try {
    UnitDependency(
      cdm.getUnit,
      cdm.getDependentUnit
    )
  }
}
