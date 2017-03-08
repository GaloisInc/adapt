package com.galois.adapt.cdm15

import com.bbn.tc.schema.avro.cdm15
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
                ) extends CDM15 with DBWritable {
  def asDBKeyValues = List(
    label, "Value",
    "size", size,
    "valueType", valueType,
    "valueDataType", valueDataType,
    "isNull", isNull
  ) ++
    name.fold[List[Any]](List.empty)(v => List("name", v)) ++
    runtimeDataType.fold[List[Any]](List.empty)(v => List("runtimeDataType", v)) ++
    valueBytes.fold[List[Any]](List.empty)(v => List("valueBytes", v)) ++
    tag.fold[List[Any]](List.empty)(v => List("tag", v)) ++
    components.fold[List[Any]](List.empty)(v => List("components", v))

  def asDBEdges = Nil
  
  def getUuid = throw new RuntimeException("Value has no UUID")
}

case object Value extends CDM15Constructor[Value] {
  type RawCDMType = cdm15.Value

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
