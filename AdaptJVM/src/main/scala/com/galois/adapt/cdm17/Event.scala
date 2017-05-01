package com.galois.adapt.cdm17

import com.galois.adapt.{DBWritable, DBNodeable}
import org.apache.tinkerpop.gremlin.structure.T.label

import scala.util.Try
import java.util.UUID

import com.bbn.tc.schema.avro.cdm17

import scala.collection.JavaConverters._


case class Event(
  uuid: UUID,
  sequence: Long = 0,
  eventType: EventType,
  threadId: Int,
  subject: UUID,
  timestampNanos: Long,
  predicateObject: Option[UUID] = None,
  predicateObjectPath: Option[String] = None,
  predicateObject2: Option[UUID] = None,
  predicateObject2Path: Option[String] = None,
  name: Option[String] = None,
  parameters: Option[Seq[Value]] = None,
  location: Option[Long] = None,
  size: Option[Long] = None,
  programPoint: Option[String] = None,
  properties: Option[Map[String,String]] = None
) extends CDM17 with DBWritable with Comparable[Event] with Ordering[Event] with DBNodeable {
  val foldedParameters: List[Value] = parameters.fold[List[Value]](List.empty)(_.toList)
  
  def asDBKeyValues = List(
    label, "Event",
    "uuid", uuid,
    "sequence", sequence,
    "eventType", eventType.toString,
    "threadId", threadId,
    "subject", subject,
    "timestampNanos", timestampNanos
  ) ++
    predicateObject.fold[List[Any]](List.empty)(v => List("predicateObject", v)) ++
    predicateObjectPath.fold[List[Any]](List.empty)(v => List("predicateObjectPath", v)) ++
    predicateObject2.fold[List[Any]](List.empty)(v => List("predicateObject2", v.toString)) ++
    predicateObject2Path.fold[List[Any]](List.empty)(v => List("predicateObject2Path", v)) ++
    name.fold[List[Any]](List.empty)(v => List("name", v)) ++
    parameters.fold[List[Any]](List.empty)(v => if (v.isEmpty) List.empty else List("parameters", v.map(_.asDBKeyValues).mkString(", "))) ++
    location.fold[List[Any]](List.empty)(v => List("location", v)) ++
    size.fold[List[Any]](List.empty)(v => List("size", v)) ++
    programPoint.fold[List[Any]](List.empty)(v => List("programPoint", v)) ++
    DBOpt.fromKeyValMap(properties)  // Flattens out nested "properties"

  def asDBEdges = List.concat(
    List(("subject",subject)),
    predicateObject.map(p => ("predicateObject",p)),
    predicateObject2.map(p => ("predicateObject2",p)),
    foldedParameters.flatMap(value => value.tagsFolded.map(tag => ("parameterTagId", tag.tagId)))
  )

  def getUuid = uuid

  def compare(x: Event, y: Event) = x.sequence compare y.sequence

  def compareTo(o: Event) = this.sequence.compare(o.sequence)
  
//  override val supportNodes =
//    foldedParameters.flatMap(t => (t.getUuid, t.asDBKeyValues, t.asDBEdges) :: t.supportNodes)
}

case object Event extends CDM17Constructor[Event] {
  type RawCDMType = cdm17.Event

  def from(cdm: RawCDM17Type): Try[Event] = Try(
    Event(
      cdm.getUuid,
      cdm.getSequence,
      cdm.getType,
      cdm.getThreadId,
      cdm.getSubject,
      cdm.getTimestampNanos,
      AvroOpt.uuid(cdm.getPredicateObject),
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
