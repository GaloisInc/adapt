package com.galois.adapt.cdm18
import com.bbn.tc.schema.avro.cdm18
import com.galois.adapt.DBWritable

import scala.util.Try


case class AbstractObject(
  permission: Option[FixedShort] = None,  // fixed size = 2
  epoch: Option[Int] = None,
  properties: Option[Map[String,String]] = None
) extends CDM18 with DBWritable {
  def asDBKeyValues: List[(String, Any)] = List() ++
    permission.fold[List[(String, Any)]](List.empty)(v => List(("permission", v.bytes.toString))) ++
    epoch.fold[List[(String, Any)]](List.empty)(v => List(("epoch", v))) ++
    DBOpt.fromKeyValMap(properties)
}

case object AbstractObject extends CDM18Constructor[AbstractObject] {
  type RawCDMType = cdm18.AbstractObject

  def from(cdm: RawCDM18Type): Try[AbstractObject] = Try {
    AbstractObject(
      AvroOpt.fixedShort(cdm.getPermission),
      AvroOpt.int(cdm.getEpoch),
      AvroOpt.map(cdm.getProperties)
    )
  }
}
