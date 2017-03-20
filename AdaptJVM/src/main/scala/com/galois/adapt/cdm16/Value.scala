package com.galois.adapt.cdm16

import com.bbn.tc.schema.avro.cdm16
import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try


case class Value(
  size: Int = -1,
  valueType: ValueType,
  valueDataType: ValueDataType,
  isNull: Boolean = false,     // TODO: What are the semantics of this?!?
  name: Option[String] = None,
  runtimeDataType: Option[String] = None,
  valueBytes: Option[Array[Byte]] = None,
  tag: Option[Seq[TagRunLengthTuple]] = None,
  components: Option[Seq[Value]] = None
) extends CDM16 with DBWritable {
  def asDBKeyValues = List(
    "size", size,
    "valueType", valueType.toString,
    "valueDataType", valueDataType.toString,
    "isNull", isNull
  ) ++
    name.fold[List[Any]](List.empty)(v => List("name", v)) ++
    runtimeDataType.fold[List[Any]](List.empty)(v => List("runtimeDataType", v)) ++
    valueBytes.fold[List[Any]](List.empty)(v => List("valueBytes", v.toString)) ++
    tag.fold[List[Any]](List.empty)(v => if (v.isEmpty) List.empty else List("tag", v.map(_.asDBKeyValues).mkString(", "))) ++
    components.fold[List[Any]](List.empty)(v => List("components", v.map(_.asDBKeyValues).mkString(", ")))   // TODO: This should probably be made into a more meaningful data structure instead of dumping a Seq[Value] to the DB.
}

case object Value extends CDM16Constructor[Value] {
  type RawCDMType = cdm16.Value

  def from(cdm: RawCDM15Type): Try[Value] = Try(
    Value(
      cdm.getSize,
      cdm.getType,
      cdm.getValueDataType,
      cdm.getIsNull,
      AvroOpt.str(cdm.getName),
      AvroOpt.str(cdm.getRuntimeDataType),
      AvroOpt.byteArr(cdm.getValueBytes),
      AvroOpt.listTagRunLengthTuple(cdm.getTag),
      AvroOpt.listValue(cdm.getComponents)
    )
  )
}
