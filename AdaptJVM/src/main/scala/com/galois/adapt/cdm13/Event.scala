package com.galois.adapt.cdm13

import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label
import scala.util.Try
import java.util.UUID
import scala.collection.JavaConverters._


case class Event(
  uuid: UUID,
  eventType: EventType,
  threadId: Int,
  source: InstrumentationSource,
  sequence: Long = 0L,
  timestampMicros: Option[Long] = None,
  name: Option[String] = None,
  parameters: Option[Seq[Value]] = None,
  location: Option[Long] = None,
  size: Option[Long] = None,
  programPoint: Option[String] = None,
  properties: Option[Map[String,String]] = None
) extends CDM13 with DBWritable {
  def asDBKeyValues = List(
    label, "Event",
    "eventType", eventType.toString,
    "threadId", threadId,
    "source", source.toString,
    "sequence", sequence
    ) ++
    timestampMicros.fold[List[Any]](List.empty)(v => List("timestampMicros", v)) ++
    name.fold[List[Any]](List.empty)(v => List("name", v)) ++
    parameters.fold[List[Any]](List.empty)(v => List("parameters", v.mkString(", "))) ++
    location.fold[List[Any]](List.empty)(v => List("location", v)) ++
    size.fold[List[Any]](List.empty)(v => List("size", v)) ++
    programPoint.fold[List[Any]](List.empty)(v => List("programPoint", v)) ++
    DBOpt.fromKeyValMap(properties)  // Flattens out nested "properties"
}

case object Event extends CDM13Constructor[Event] {
  type RawCDMType = com.bbn.tc.schema.avro.Event

  def from(cdm: RawCDM13Type): Try[Event] = Try(
    Event(
      cdm.getUuid,
      cdm.getType,
      cdm.getThreadId,
      cdm.getSource,
      cdm.getSequence,
      AvroOpt.long(cdm.getTimestampMicros),
      AvroOpt.str(cdm.getName),
      AvroOpt.listValue(cdm.getParameters),
      AvroOpt.long(cdm.getLocation),
      AvroOpt.long(cdm.getSize),
      AvroOpt.str(cdm.getProgramPoint),
      AvroOpt.map(cdm.getProperties)
    )
  )
}