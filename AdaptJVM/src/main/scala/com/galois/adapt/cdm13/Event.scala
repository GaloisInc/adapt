package com.galois.adapt.cdm13

import java.util.UUID

import scala.util.Try


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
) extends CDM13

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