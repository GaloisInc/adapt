package com.galois.adapt.cdm13
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label
import scala.util.Try
import scala.collection.JavaConverters._


case class AbstractObject(
  source: InstrumentationSource,
  permission: Option[FixedShort] = None,  // fixed size = 2
  lastTimestampMicros: Option[Long] = None,
  properties: Option[Map[String,String]] = None
) extends CDM13 with DBWritable {
  def asDBKeyValues = List(
//    label, "AbstractObject",
    "source", source.toString
  ) ++
    permission.fold[List[Any]](List.empty)(v => List("permission", v.bytes.toString)) ++
    lastTimestampMicros.fold[List[Any]](List.empty)(v => List("lastTimestampMicros", v)) ++
    DBOpt.fromKeyValMap(properties)
//    properties.fold[List[Any]](List.empty)(v => if (v.isEmpty) List.empty else List("properties", v.mkString("{",", ", "}")))
}

case object AbstractObject extends CDM13Constructor[AbstractObject] {
  type RawCDMType = com.bbn.tc.schema.avro.cdm13.AbstractObject

  def from(cdm: RawCDM13Type): Try[AbstractObject] = Try {
    AbstractObject(
      cdm.getSource,
      AvroOpt.fixedShort(cdm.getPermission),
      AvroOpt.long(cdm.getLastTimestampMicros),
      AvroOpt.map(cdm.getProperties)
    )
  }
}
