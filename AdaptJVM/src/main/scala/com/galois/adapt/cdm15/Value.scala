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
    "size", size,
    "valueType", valueType.toString,
    "valueDataType", valueDataType.toString,
    "isNull", isNull
  ) ++
    name.fold[List[Any]](List.empty)(v => List("name", v)) ++
    runtimeDataType.fold[List[Any]](List.empty)(v => List("runtimeDataType", v)) ++
    valueBytes.fold[List[Any]](List.empty)(v => List("valueBytes", v.toString)) ++
    tag.fold[List[Any]](List.empty)(v => List("tag", v.map(_.asDBKeyValues))) ++
    components.fold[List[Any]](List.empty)(v => List("components", v.map(_.asDBKeyValues)))   // TODO: This should probably be made into a more meaningful data structure instead of dumping a Seq[Value] to the DB.

  def asDBEdges = throw new RuntimeException("Value has no EDGES... ever.")  // There are assumptions elsewhere in the code that there will _never_ be an edges here!
  
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
