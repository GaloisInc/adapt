package com.galois.adapt.cdm16

import java.util.UUID

import com.bbn.tc.schema.avro.cdm16
import com.galois.adapt.{DBWritable, DBNodeable}
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class UnitDependency(
  unit: UUID,
  dependentUnit: UUID
) extends CDM16 with DBWritable with DBNodeable {
  def asDBKeyValues = List(
    label, "UnitDependency",
    "unit", unit,
    "dependentUnit", dependentUnit
  )

  def asDBEdges = List(("dependentUnit",dependentUnit),("unit",unit))

  val thisUUID = UUID.randomUUID()

  def getUuid = thisUUID
}


case object UnitDependency extends CDM16Constructor[UnitDependency] {
  type RawCDMType = cdm16.UnitDependency

  def from(cdm: RawCDM15Type): Try[UnitDependency] = Try {
    UnitDependency(
      cdm.getUnit,
      cdm.getDependentUnit
    )
  }
}
