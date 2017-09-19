package com.galois.adapt.cdm17

import java.util.UUID

import com.bbn.tc.schema.avro.cdm17
import com.galois.adapt.{DBWritable, DBNodeable}
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class UnitDependency(
  unit: UUID,
  dependentUnit: UUID
) extends CDM17 with DBWritable with DBNodeable {
  override def getLabels: List[String] = List("CDM17", "UnitDependency")

  def asDBKeyValues = List(
    ("unitUuid", unit),
    ("dependentUnitUuid", dependentUnit)
  )

  def asDBEdges = List((CDM17.EdgeTypes.dependentUnit,dependentUnit),(CDM17.EdgeTypes.unit,unit))

  val thisUUID = UUID.randomUUID()

  def getUuid = thisUUID
}


case object UnitDependency extends CDM17Constructor[UnitDependency] {
  type RawCDMType = cdm17.UnitDependency

  def from(cdm: RawCDM17Type): Try[UnitDependency] = Try {
    UnitDependency(
      cdm.getUnit,
      cdm.getDependentUnit
    )
  }
}
