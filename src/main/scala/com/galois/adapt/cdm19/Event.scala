package com.galois.adapt.cdm19

import java.io

import com.galois.adapt.{DBNodeable, DBWritable}

import scala.util.{Failure, Try}
import java.util.UUID

import com.bbn.tc.schema.avro.cdm19

import scala.collection.JavaConverters._

case class Event(
  uuid: UUID,
  sequence: Option[Long] = None,
  eventType: EventType,
  threadId: Option[Int] = None,
  host: UUID,
  subjectUuid: Option[UUID] = None, // required for all events, except the EVENT_ADD_OBJECT_ATTRIBUTE and EVENT_FLOWS_TO event.
  timestampNanos: Long,
  predicateObject: Option[UUID] = None,
  predicateObjectPath: Option[String] = None,
  predicateObject2: Option[UUID] = None,
  predicateObject2Path: Option[String] = None,
  names: List[String] = List(),
  parameters: Option[Seq[Value]] = None,
  location: Option[Long] = None,
  size: Option[Long] = None,
  programPoint: Option[String] = None,
  properties: Option[Map[String,String]] = None
) extends CDM19 with DBWritable with Comparable[Event] with Ordering[Event] with DBNodeable[CDM19.EdgeTypes.EdgeTypes] {
  val foldedParameters: List[Value] = parameters.fold[List[Value]](List.empty)(_.toList)

  def asDBKeyValues: List[(String, Any)] = List(
    ("uuid", uuid),
    ("eventType", eventType.toString),
    ("timestampNanos", timestampNanos),
    ("names", names.mkString("|"))
//    ("host", host)  // Let's just throw this away since the subject and predObj will always have the same value.
  ) ++
    sequence.fold[List[(String,Any)]](List.empty)(v => List(("sequence", v))) ++
    threadId.fold[List[(String,Any)]](List.empty)(v => List(("threadId", v))) ++
    subjectUuid.fold[List[(String,Any)]](List.empty)(v => List(("subjectUuid", v))) ++
    predicateObject.fold[List[(String,Any)]](List.empty)(v => List(("predicateObjectUuid", v))) ++
    predicateObjectPath.fold[List[(String,Any)]](List.empty)(v => List(("predicateObjectPath", v))) ++
    predicateObject2.fold[List[(String,Any)]](List.empty)(v => List(("predicateObject2Uuid", v))) ++
    predicateObject2Path.fold[List[(String,Any)]](List.empty)(v => List(("predicateObject2Path", v))) ++
    parameters.fold[List[(String,Any)]](List.empty)(v => if (v.isEmpty) List.empty else List(("parameters", v.map(_.asDBKeyValues).mkString(", ")))) ++
    location.fold[List[(String,Any)]](List.empty)(v => List(("location", v))) ++
    size.fold[List[(String,Any)]](List.empty)(v => List(("size", v))) ++
    programPoint.fold[List[(String,Any)]](List.empty)(v => List(("programPoint", v))) ++
    DBOpt.fromKeyValMap(properties) ++  // Flattens out nested "properties"
  {
    // For the policy enforcement demo of policy 3 with Clearscope's data
    // we only care about parameters on EVENT_WRITEs where the second elt has valueBytes and tagRunLengthTuples
    val peParam = parameters.filter {
      p => p.lengthCompare(1)>0 && p.last.valueBytes.isDefined && p.last.tagRunLengthTuples.isDefined
    }


    val decodedOption: Option[String] = peParam match {
      case Some(params) => Some(new String(params.last.valueBytes.get))
      case _ => None
      }

    if (decodedOption.exists(_.contains("GET "))) {
      peParam.fold[List[(String,Any)]](List.empty){v => List("peTagIds" -> v(1).tagRunLengthTuples.get.map(_.tagId).toSet.mkString(","))}
    } else {
      List.empty
    }
  }

  def asDBEdges = List.concat(
    subjectUuid.map(s => (CDM19.EdgeTypes.subject,s)),
    predicateObject.map(p => (CDM19.EdgeTypes.predicateObject,p)),
    predicateObject2.map(p => (CDM19.EdgeTypes.predicateObject2,p)),
    foldedParameters.flatMap(value => value.tagsFolded.map(tag => (CDM19.EdgeTypes.parameterTagId, tag.tagId)))
  ) ++ {
    // For the policy enforcement demo of policy 3 with Clearscope's data
    // we only care about parameters on EVENT_WRITEs where the second elt has valueBytes and tagRunLengthTuples
    val peParam = parameters
      .filter(p => p.lastOption.fold(false)(l => l.valueBytes.isDefined && l.tagRunLengthTuples.isDefined))

    val decodedOption: Option[String] = peParam match {
      case Some(params) => Some(new String(params.last.valueBytes.get))
      case _ => None
    }

    if (decodedOption.exists(_.contains("GET "))) {
      peParam.fold[List[(CDM19.EdgeTypes.Value,UUID)]](List.empty)(v => v(1).tagRunLengthTuples.get.map(t => CDM19.EdgeTypes.peTagId -> t.tagId).toList)
    } else {
      List.empty
    }
  }

  def getUuid: UUID = uuid

  override def getHostId: Option[UUID] = Some(host)

  def compare(x: Event, y: Event) = x.timestampNanos compare y.timestampNanos

  def compareTo(o: Event) = this.timestampNanos.compare(o.timestampNanos)

  def toMap: Map[String,Any] = Map(
//    "label" -> "Event",
    "uuid" -> uuid,
    "sequence" -> sequence.getOrElse(""),
    "eventType" -> eventType.toString,
    "threadId" -> threadId.getOrElse(""),
    "subjectUuid" -> subjectUuid.getOrElse(""),
    "timestampNanos" -> timestampNanos,
    "predicateObjectUuid" -> predicateObject.getOrElse(""),
    "predicateObjectPath" -> predicateObjectPath.getOrElse(""),
    "predicateObject2Uuid" -> predicateObject2.getOrElse(""),
    "predicateObject2Path" -> predicateObject2Path.getOrElse(""),
    "name" -> names.mkString("|"),
    "parameters" -> parameters.getOrElse(Seq.empty).mkString("|"),
    "location" -> location.getOrElse(""),
    "size" -> size.getOrElse(""),
    "programPoint" -> programPoint.getOrElse(""),
    "properties" -> properties.getOrElse(Map.empty)
  ) //++ properties.getOrElse(Map.empty)  // Flattens out nested "properties"

//  override val supportNodes =
//    foldedParameters.flatMap(t => (t.getUuid, t.asDBKeyValues, t.asDBEdges) :: t.supportNodes)
}

case object Event extends CDM19Constructor[Event] {
  type RawCDMType = cdm19.Event

  def from(cdm: RawCDM19Type): Try[Event] = Try(
    Event(
      cdm.getUuid,
      AvroOpt.long(cdm.getSequence),
      cdm.getType,
      AvroOpt.int(cdm.getThreadId),
      cdm.getHostId.get,
      AvroOpt.uuid(cdm.getSubject),
      cdm.getTimestampNanos,
      AvroOpt.uuid(cdm.getPredicateObject),
      AvroOpt.str(cdm.getPredicateObjectPath),
      AvroOpt.uuid(cdm.getPredicateObject2),
      AvroOpt.str(cdm.getPredicateObject2Path),
      AvroOpt.listStr(cdm.getNames).toList.flatten,
      AvroOpt.listValue(cdm.getParameters),
      AvroOpt.long(cdm.getLocation),
      AvroOpt.long(cdm.getSize),
      AvroOpt.str(cdm.getProgramPoint),
      AvroOpt.map(cdm.getProperties)
    )
  )
}
