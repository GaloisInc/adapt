package com.galois.adapt.cdm17

import com.bbn.tc.schema.avro.cdm17
import com.galois.adapt.{DBNodeable, DBWritable}
import java.util.UUID
import scala.util.Try


case class Value(
  size: Int = -1,
  valueType: ValueType,
  valueDataType: ValueDataType,
  isNull: Boolean = false, // TODO: What are the semantics of this?!?
  name: Option[String] = None,
  runtimeDataType: Option[String] = None,
  valueBytes: Option[Array[Byte]] = None,
  tagRunLengthTuples: Option[Seq[TagRunLengthTuple]] = None,
  components: Option[Seq[Value]] = None
) extends CDM17 with DBWritable with DBNodeable {
  val tagsFolded = tagRunLengthTuples.fold[List[TagRunLengthTuple]](List.empty)(_.toList)

  def asDBKeyValues = List(
    ("size", size),
    ("valueType", valueType.toString),
    ("valueDataType", valueDataType.toString),
    ("isNull", isNull)
  ) ++
    name.fold[List[(String,Any)]](List.empty)(v => List(("name", v))) ++
    runtimeDataType.fold[List[(String,Any)]](List.empty)(v => List(("runtimeDataType", v))) ++
    valueBytes.fold[List[(String,Any)]](List.empty)(v => List(("valueBytes", new String(v)))) ++
    tagRunLengthTuples.fold[List[(String,Any)]](List.empty)(v => if (v.isEmpty) List.empty else List(("tagRunLengthTuples", v.map(_.asDBKeyValues).mkString(", ")))) ++
    components.fold[List[(String,Any)]](List.empty)(v => List(("components", v.map(_.asDBKeyValues).mkString(", "))))   // TODO: This should probably be made into a more meaningful data structure instead of dumping a Seq[Value] to the DB.

  val getUuid = UUID.randomUUID()

  val asDBEdges: List[(CDM17.EdgeTypes.EdgeTypes,UUID)] = tagsFolded.map(t => (CDM17.EdgeTypes.tag,t.getUuid))

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
