package com.galois.adapt.cdm15

import java.util.UUID

import com.bbn.tc.schema.avro.cdm15
import com.galois.adapt.{DBWritable, DBNodeable}
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class UnitDependency(
  unit: UUID,
  dependentUnit: UUID
) extends CDM15 with DBWritable with DBNodeable {
  def asDBKeyValues = List(
    label, "UnitDependency",
    "unit", unit,
    "dependentUnit", dependentUnit
  )

  def asDBEdges = List(("dependentUnit",dependentUnit))

  def getUuid = unit
}


case object UnitDependency extends CDM15Constructor[UnitDependency] {
  type RawCDMType = cdm15.UnitDependency

  def from(cdm: RawCDM15Type): Try[UnitDependency] = Try {
    UnitDependency(
      cdm.getUnit,
      cdm.getDependentUnit
    )
  }
}
