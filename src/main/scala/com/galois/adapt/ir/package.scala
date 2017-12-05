package com.galois.adapt

import java.io.ByteArrayOutputStream
import java.util.UUID

import scala.concurrent._
import org.apache.tinkerpop.gremlin.structure.T.label
import com.galois.adapt.cdm17._
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout

// TODO: do this with shapeless. It is _begging_ to be done with shapeless


package object ir {

  case class CdmUUID(uuid: UUID) extends AnyVal
  case class IrUUID(uuid: UUID) extends AnyVal

  implicit def unwrapIR_UUID(irUUID: IrUUID): UUID = irUUID.uuid

  /* Edges are now first class values in the stream.
   */
  sealed trait Edge[From, To]
  final case class EdgeCdm2Cdm(src: CdmUUID, label: String, tgt: CdmUUID) extends Edge[CDM17, CDM17]
  final case class EdgeCdm2Ir (src: CdmUUID, label: String, tgt: IrUUID) extends Edge[CDM17, IR]
  final case class EdgeIr2Cdm (src: IrUUID, label: String, tgt: CdmUUID) extends Edge[IR, CDM17]
  final case class EdgeIr2Ir  (src: IrUUID, label: String, tgt: IrUUID) extends Edge[IR, IR]

  sealed trait IR extends DBWritable { self =>
    val uuid: IrUUID                     // The current UUID
    val originalCdmUuids: Seq[CdmUUID]   // The UUIDs of all the CDM nodes that were merged to produce this node

    def toMap: Map[String, Any]

    def getUuid: IrUUID = uuid
  }


  /* Compared to 'cdm.Event':
   *
   *   - 'predicateObjectPath' and 'predicateObject2Path' belong on the predicate object, _not_ the event
   *
   *   - 'name' is empty (always?) on Clearscope, too specific (syscall) on Cadets
   *   - 'threadId' is useless (we get the interesting information from the subject)
   *   - 'parameters' is too much information
   *   - 'size' is too much information
   *   - 'location' is too much information
   *   - 'programPoint' is too much information
   */
  final case class IrEvent(
    uuid: IrUUID,
    originalCdmUuids: Seq[CdmUUID],

    eventType: EventType,
    earliestTimestampNanos: Long,
    latestTimestampNanos: Long
  ) extends IR with DBWritable {

    def asDBKeyValues =
      List(
//        label, "IrEvent",
        "uuid" -> uuid.uuid,
        "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
        "eventType" -> eventType.toString,
        "earliestTimestampNanos" -> earliestTimestampNanos,
        "latestTimestampNanos" -> latestTimestampNanos
      )

    def toMap =
      Map(
        "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
        "eventType" -> eventType,
        "earliestTimestampNanos" -> earliestTimestampNanos,
        "latestTimestampNanos" -> latestTimestampNanos
      )
  }


  /* Compared to 'cdm.Subject', the following are omitted
   *
   *  - 'cid'
   *  - 'unitId'
   *  - 'iteration'
   *  - 'count'
   *  - 'privilegeLevel'
   *  - 'importedLibraries' and 'exportedLibraries' aren't used
   */
  final case class IrSubject(
    uuid: IrUUID,
    originalCdmUuids: Seq[CdmUUID],

    subjectTypes: Set[SubjectType],
    startTimestampNanos: Long
  ) extends IR with DBWritable {

    def asDBKeyValues =
      List(
//        label, "IrSubject",
        "uuid" -> uuid.uuid,
        "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
        "subjectType" -> subjectTypes.toString,
        "startTimestampNanos" -> startTimestampNanos
      )

    def toMap =
      Map(
        "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
        "subjectType" -> subjectTypes.map(_.toString).toList.sorted.mkString(";"),
        "startTimestampNanos" -> startTimestampNanos
      )
  }

  case class IrPathNode(
     path: String
   ) extends IR with DBWritable {
    val uuid = IrUUID(DeterministicUUID(this))
    val originalCdmUuids: Seq[CdmUUID] = Nil

    def asDBKeyValues =
      List(
//        label, "IrPathNode",
        "uuid" -> uuid.uuid,
        "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
        "path" -> path
      )

    def toMap =
      Map(
        "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
        "path"-> path
      )
  }

  /* Compared to 'cdm.FileObject', this leaves out
   *
   *  - fileDescriptor
   *  - size
   *  - peInfo
   *  - hashes
   */
  final case class IrFileObject(
     uuid: IrUUID,
     originalCdmUuids: Seq[CdmUUID],

     fileObjectType: FileObjectType
  ) extends IR with DBWritable {

    def asDBKeyValues =
      List(
//        label, "IrFileObject",
        "uuid" -> uuid.uuid,
        "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
        "fileObjectType" -> fileObjectType.toString
      )

    def toMap =
      Map(
        "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
        "fileObjectType" -> fileObjectType
      )
  }

  /* Compared to 'cdm.NetFlowObject' this leaves out
   *
   *  - ipProtocol
   *  - fileDescriptor
   */
  final case class IrNetFlowObject(
    originalCdmUuids: Seq[CdmUUID],

    localAddress: String,
    localPort: Int,
    remoteAddress: String,
    remotePort: Int
  ) extends IR with DBWritable {

    val uuid = IrUUID(DeterministicUUID(this))

    def asDBKeyValues =
      List(
//        label, "IrNetFlowObject",
        "uuid" -> uuid.uuid,
        "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
        "localAddress" -> localAddress,
        "localPort" -> localPort,
        "remoteAddress" -> remoteAddress,
        "remotePort" -> remotePort
      )

    def toMap =
      Map(
        "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
        "localAddress" -> localAddress,
        "localPort" -> localPort,
        "remoteAddress" -> remoteAddress,
        "remotePort" -> remotePort
      )
  }


  /* Compared to 'cdm.SrcSinkObject' this leaves out
   *
   *  - fileDescriptor
   */
  final case class IrSrcSinkObject(
    uuid: IrUUID,
    originalCdmUuids: Seq[CdmUUID],

    srcSinkType: SrcSinkType
  ) extends IR with DBWritable {

    def asDBKeyValues =
      List(
//        label, "IrSrcSinkObject",
        "uuid" -> uuid.uuid,
        "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
        "srcSinkType" -> srcSinkType.toString
      )

    def toMap =
      Map(
        "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
        "srcSinkType" -> srcSinkType
      )
  }

  /* These don't occur very much, so we are likely to do lots of analysis on them. Still worth
   * distinguishing the regular and super users. Compared to 'cdm.Principal', this only omits the
   * property map.
   *
   * TODO: get rid of this in favor of an enumeration (this is a lot of edges for not much)
   */
  final case class IrPrincipal(
    originalCdmUuids: Seq[CdmUUID],

    userId: String,
    groupIds: Seq[String],
    principalType: PrincipalType = PRINCIPAL_LOCAL,
    username: Option[String] = None
  ) extends IR with DBWritable {

    val uuid = IrUUID(DeterministicUUID(this))

    def asDBKeyValues =
      List(
//        label, "IrPrincipal",
        "uuid" -> uuid.uuid,
        "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
        "userId" -> userId,
        "principalType" -> principalType.toString
      ) ++
        (if (groupIds.nonEmpty) List("groupIds" -> groupIds.mkString(", ")) else Nil) ++
        username.fold[List[(String,Any)]](Nil)(v => List("username" -> v))

    def toMap =
      Map(
        "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
        "userId" -> userId,
        "principalType" -> principalType,
        "groupIds" -> groupIds.toList.sorted.mkString(";"),
        "username" -> username.getOrElse("")
      )
  }

  /* Compared to 'cdm.ProvenanceTagNode', this leaves out
   *
   *  - 'systemCall'
   *  - 'opcode'
   *  - 'itag'
   *  - 'ctag'
   *
   * TODO: should tagIds just have type 'Seq[UUID]'?
   */
  final case class IrProvenanceTagNode(
    uuid: IrUUID,
    originalCdmUuids: Seq[CdmUUID],

    programPoint: Option[String] = None
  ) extends IR with DBWritable {

    type SelfType = IrProvenanceTagNode

    def asDBKeyValues =
      List(
//        label, "IrProvenanceTagNode",
        "uuid" -> uuid.uuid,
        "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";")
      ) ++
        programPoint.fold[List[(String,Any)]](Nil)(p => List("programPoint" -> p))

    def toMap =
      Map(
        "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
        "programPoint" -> programPoint.getOrElse("")
      )
  }

  final case class IrSynthesized(
    uuid: IrUUID,
    originalCdmUuids: Seq[CdmUUID]
  ) extends IR with DBWritable {

    def asDBKeyValues =
      List(
//        label, "IrSynthesized",
        "uuid" -> uuid.uuid,
        "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";")
      )

    def toMap =
      Map(
        "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";")
      )
  }

}

object DeterministicUUID {

  def apply[T <: Product](product: T): UUID = {
    val byteOutputStream: ByteArrayOutputStream = new ByteArrayOutputStream()
    for (value <- product.productIterator) {
      byteOutputStream.write(value.hashCode())
    }
    UUID.nameUUIDFromBytes(byteOutputStream.toByteArray)
  }

}
