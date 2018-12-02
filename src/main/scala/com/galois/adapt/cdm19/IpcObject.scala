package com.galois.adapt.cdm19

import java.util.UUID
import com.bbn.tc.schema.avro.cdm19
import com.galois.adapt.{DBWritable, DBNodeable}
import scala.util.Try


case class IpcObject(
  uuid: UUID,
  host: UUID,
  baseObject: AbstractObject,
  ipcObjectType: IpcObjectType,

  // If flow is unidirectional, then source is UUID1/fd1 and
  // destination is UUID2/fd2.
  uuid1: Option[UUID] = None,
  uuid2: Option[UUID] = None,
  fd1: Option[Int] = None,
  fd2: Option[Int] = None
) extends CDM19 with DBWritable with DBNodeable[CDM19.EdgeTypes.EdgeTypes] {

  def asDBKeyValues = List(
      ("uuid", uuid)
    ) ++
    fd1.fold[List[(String, Any)]](Nil)(v => List(("sourceFileDescriptor", v))) ++
    fd1.fold[List[(String, Any)]](Nil)(v => List(("sinkFileDescriptor", v))) ++
    baseObject.asDBKeyValues

  def asDBEdges: List[(CDM19.EdgeTypes.EdgeTypes, UUID)] = List.concat(
    uuid1.map(s => (CDM19.EdgeTypes.uuid1, s)),
    uuid2.map(s => (CDM19.EdgeTypes.uuid2, s))
  )

  def getUuid = uuid

  override def getHostId: Option[UUID] = Some(host)
}


case object IpcObject extends CDM19Constructor[IpcObject] {
  type RawCDMType = cdm19.IpcObject

  def from(cdm: RawCDM19Type): Try[IpcObject] = Try {
    IpcObject(
      cdm.getUuid,
      cdm.getHostId.get,
      cdm.getBaseObject,
      cdm.getType,
      AvroOpt.uuid(cdm.getUuid1),
      AvroOpt.uuid(cdm.getUuid2),
      AvroOpt.int(cdm.getFd1),
      AvroOpt.int(cdm.getFd2)
    )
  }
}
