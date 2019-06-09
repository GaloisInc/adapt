package com.galois.adapt.cdm20

import com.bbn.tc.schema.avro.cdm20
import com.galois.adapt.DBWritable
import scala.util.Try


case class AbstractObject(
  permission: Option[FixedShort] = None,  // fixed size = 2
  epoch: Option[Int] = None,
  properties: Option[Map[String,String]] = None
) extends CDM20 with DBWritable {
  def asDBKeyValues: List[(String, Any)] = List() ++
    permission.fold[List[(String, Any)]](List.empty)(v => List(("permission", v.bytes.toString))) ++
    epoch.fold[List[(String, Any)]](List.empty)(v => List(("epoch", v))) ++
    DBOpt.fromKeyValMap(properties)
}

case object AbstractObject extends CDM20Constructor[AbstractObject] {
  type RawCDMType = cdm20.AbstractObject

  def from(cdm: RawCDM20Type): Try[AbstractObject] = Try {
    AbstractObject(
      AvroOpt.fixedShort(cdm.getPermission),
      AvroOpt.int(cdm.getEpoch),
      AvroOpt.map(cdm.getProperties)
    )
  }
}
