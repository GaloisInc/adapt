package com.galois.adapt.cdm13

import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label
import scala.util.Try


case class Value(
  valueType: ValueType,
  valueDataType: ValueDataType,
  size: Int = 0,
  isNull: Boolean = false,     // TODO: What are the semantics of this?!?
  name: Option[String] = None,
  runtimeDataType: Option[String] = None,
  valueBytes: Option[Array[Byte]] = None,
  tag: Option[Seq[Int]] = None,
  components: Option[Seq[Value]] = None
) extends CDM13 with DBWritable {
  def asDBKeyValues = List(
    label, "Value",
    "valueType", valueType,
    "valueDataType", valueDataType,
    "size", size,
    "isNull", isNull
  ) ++
  name.fold[List[Any]](List.empty)(v => List("name", v)) ++
  runtimeDataType.fold[List[Any]](List.empty)(v => List("runtimeDataType", v)) ++
  valueBytes.fold[List[Any]](List.empty)(v => List("valueBytes", v)) ++
  tag.fold[List[Any]](List.empty)(v => List("tag", v)) ++
  components.fold[List[Any]](List.empty)(v => List("components", v))
}

case object Value extends CDM13Constructor[Value] {
  type RawCDMType = com.bbn.tc.schema.avro.cdm13.Value

  def from(cdm: RawCDM13Type): Try[Value] = Try(
    Value(
      cdm.getType,
      cdm.getValueDataType,
      cdm.getSize,
      cdm.getIsNull,
      AvroOpt.str(cdm.getName),
      AvroOpt.str(cdm.getRuntimeDataType),
      AvroOpt.byteArr(cdm.getValueBytes),
      AvroOpt.listInt(cdm.getTag),
      AvroOpt.listValue(cdm.getComponents)
    )
  )
}
