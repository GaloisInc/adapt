package com.galois.adapt.cdm17
import com.bbn.tc.schema.avro.cdm17
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try
import scala.collection.JavaConverters._


case class AbstractObject(
  permission: Option[FixedShort] = None,  // fixed size = 2
  epoch: Option[Int] = None,
  properties: Option[Map[String,String]] = None
) extends CDM17 with DBWritable {
  def asDBKeyValues: List[(String, Any)] = List() ++
    permission.fold[List[(String, Any)]](List.empty)(v => List(("permission", v.bytes.toString))) ++
    epoch.fold[List[(String, Any)]](List.empty)(v => List(("epoch", v))) ++
    DBOpt.fromKeyValMap(properties)
}

case object AbstractObject extends CDM17Constructor[AbstractObject] {
  type RawCDMType = cdm17.AbstractObject

  def from(cdm: RawCDM17Type): Try[AbstractObject] = Try {
    AbstractObject(
      AvroOpt.fixedShort(cdm.getPermission),
      AvroOpt.int(cdm.getEpoch),
      AvroOpt.map(cdm.getProperties)
    )
  }
}
