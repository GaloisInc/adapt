package com.galois.adapt.cdm16
import com.bbn.tc.schema.avro.cdm16
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try
import scala.collection.JavaConverters._


case class AbstractObject(
  permission: Option[FixedShort] = None,  // fixed size = 2
  epoch: Option[Int] = None,
  properties: Option[Map[String,String]] = None
) extends CDM16 with DBWritable {
  def asDBKeyValues = List(
    //    label, "AbstractObject",
  ) ++
    permission.fold[List[Any]](List.empty)(v => List("permission", v.bytes.toString)) ++
    epoch.fold[List[Any]](List.empty)(v => List("epoch", v)) ++
    DBOpt.fromKeyValMap(properties)
}

case object AbstractObject extends CDM16Constructor[AbstractObject] {
  type RawCDMType = cdm16.AbstractObject

  def from(cdm: RawCDM15Type): Try[AbstractObject] = Try {
    AbstractObject(
      AvroOpt.fixedShort(cdm.getPermission),
      AvroOpt.int(cdm.getEpoch),
      AvroOpt.map(cdm.getProperties)
    )
  }
}
