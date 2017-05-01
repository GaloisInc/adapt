package com.galois.adapt.cdm17

import com.bbn.tc.schema.avro.cdm17
import com.galois.adapt.{DBNodeable, DBWritable}
import org.apache.tinkerpop.gremlin.structure.T.label

import java.util.UUID
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
) extends CDM17 with DBWritable with DBNodeable {
  val tagsFolded = tag.fold[List[TagRunLengthTuple]](List.empty)(_.toList)
  
  def asDBKeyValues = List(
    label, "Value",
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

  val getUuid = UUID.randomUUID()

  val asDBEdges: List[(String,UUID)] = tagsFolded.map(t => ("tag",t.getUuid))

  override val supportNodes =
    tagsFolded.flatMap(t => (t.getUuid, t.asDBKeyValues, t.asDBEdges) :: t.supportNodes)
  
}

case object Value extends CDM17Constructor[Value] {
  type RawCDMType = cdm17.Value

  def from(cdm: RawCDM17Type): Try[Value] = Try(
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
