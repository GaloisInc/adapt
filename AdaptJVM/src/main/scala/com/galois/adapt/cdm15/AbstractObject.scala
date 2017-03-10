package com.galois.adapt.cdm15
import com.bbn.tc.schema.avro.cdm15
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try
import scala.collection.JavaConverters._


case class AbstractObject(
  permission: Option[FixedShort] = None,  // fixed size = 2
  epoch: Option[Int] = None,
  properties: Option[Map[String,String]] = None
) extends CDM15 with DBWritable {
  def asDBKeyValues = List(
    //    label, "AbstractObject",
  ) ++
    permission.fold[List[Any]](List.empty)(v => List("permission", v.bytes.toString)) ++
    epoch.fold[List[Any]](List.empty)(v => List("epoch", v)) ++
    DBOpt.fromKeyValMap(properties)

  def asDBEdges = throw new RuntimeException("AbstractObject has no edges... ever.")

  def getUuid = throw new RuntimeException("AbstractObject has no UUID")
}

case object AbstractObject extends CDM15Constructor[AbstractObject] {
  type RawCDMType = cdm15.AbstractObject

  def from(cdm: RawCDM15Type): Try[AbstractObject] = Try {
    AbstractObject(
      AvroOpt.fixedShort(cdm.getPermission),
      AvroOpt.int(cdm.getEpoch),
      AvroOpt.map(cdm.getProperties)
    )
  }
}
