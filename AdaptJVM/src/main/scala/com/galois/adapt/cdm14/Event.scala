package com.galois.adapt.cdm14

import com.galois.adapt.DBWritable
import org.apache.tinkerpop.gremlin.structure.T.label
import scala.util.Try
import java.util.UUID
import scala.collection.JavaConverters._


case class Event(
                  uuid: UUID,
                  eventType: EventType,
                  threadId: Int,
                  subject: UUID,
                  predicateObject: UUID,
                  source: InstrumentationSource,
                  timestampNanos: Long,
                  predicateObjectPath: Option[String] = None,
                  predicateObject2: Option[UUID] = None,
                  predicateObject2Path: Option[String] = None,
                  name: Option[String] = None,
                  parameters: Option[Seq[Value]] = None,
                  location: Option[Long] = None,
                  size: Option[Long] = None,
                  programPoint: Option[String] = None,
                  properties: Option[Map[String,String]] = None
                ) extends CDM14 with DBWritable {
  def asDBKeyValues = List(
    label, "Event",
    "eventType", eventType.toString,
    "threadId", threadId,
    "subject", subject,
    "predicateObject", predicateObject,
    "source", source.toString,
    "timestampNanos", timestampNanos
  ) ++
    predicateObjectPath.fold[List[Any]](List.empty)(v => List("predicateObjectPath", v)) ++
    predicateObject2.fold[List[Any]](List.empty)(v => List("predicateObject2", v.toString)) ++
    predicateObject2Path.fold[List[Any]](List.empty)(v => List("predicateObject2Path", v)) ++
    name.fold[List[Any]](List.empty)(v => List("name", v)) ++
    parameters.fold[List[Any]](List.empty)(v => List("parameters", v.mkString(", "))) ++
    location.fold[List[Any]](List.empty)(v => List("location", v)) ++
    size.fold[List[Any]](List.empty)(v => List("size", v)) ++
    programPoint.fold[List[Any]](List.empty)(v => List("programPoint", v)) ++
    DBOpt.fromKeyValMap(properties)  // Flattens out nested "properties"
}

case object Event extends CDM14Constructor[Event] {
  type RawCDMType = com.bbn.tc.schema.avro.Event

  def from(cdm: RawCDM14Type): Try[Event] = Try(
    Event(
      cdm.getUuid,
      cdm.getType,
      cdm.getThreadId,
      cdm.getSubject,
      cdm.getPredicateObject,
      cdm.getSource,
      cdm.getTimestampNanos,
      AvroOpt.str(cdm.getPredicateObjectPath),
      AvroOpt.uuid(cdm.getPredicateObject2),
      AvroOpt.str(cdm.getPredicateObject2Path),
      AvroOpt.str(cdm.getName),
      AvroOpt.listValue(cdm.getParameters),
      AvroOpt.long(cdm.getLocation),
      AvroOpt.long(cdm.getSize),
      AvroOpt.str(cdm.getProgramPoint),
      AvroOpt.map(cdm.getProperties)
    )
  )
}