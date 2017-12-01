package com.galois.adapt

import java.io.ByteArrayOutputStream
import java.util.UUID

import scala.concurrent._
import org.apache.tinkerpop.gremlin.structure.T.label
import com.galois.adapt.cdm17._
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout

import scala.reflect.ClassTag

// TODO: do this with shapeless. It is _begging_ to be done with shapeless


package object ir {

  type IR_UUID = UUID
  type CDM_UUID = UUID

  /* Edges are now first class values in the stream.
   */
  case class Edge[From: ClassTag, To: ClassTag](src: UUID, label: String, tgt: UUID)

  sealed trait IR { self: DBWritable =>
    val uuid: IR_UUID                     // The current UUID
    val originalEntities: Seq[CDM_UUID]   // The UUIDs of all the CDM nodes that were merged to produce this node

    def toMap: Map[String, Any]

    def getUuid: IR_UUID = uuid
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
    uuid: UUID,
    originalEntities: Seq[UUID],

    eventType: EventType,
    earliestTimestampNanos: Long,
    latestTimestampNanos: Long
  ) extends IR with DBWritable {

    def asDBKeyValues =
      List(
        label, "IrEvent",
        "titanType", "IrEvent",
        "uuid", uuid,
        "eventType", eventType,
        "earliestTimestampNanos", earliestTimestampNanos,
        "latestTimestampNanos", latestTimestampNanos
      )

    def toMap =
      Map(
        "originalEntities" -> originalEntities.mkString(", "),
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
    uuid: UUID,
    originalEntities: Seq[UUID],

    subjectTypes: Set[SubjectType],
    startTimestampNanos: Long
  ) extends IR with DBWritable {

    def asDBKeyValues =
      List(
        label, "IrSubject",
        "titanType", "IrSubject",
        "uuid", uuid,
        "subjectType", subjectTypes.toString,
        "startTimestampNanos", startTimestampNanos
      )

    def toMap =
      Map(
        "originalEntities" -> originalEntities.mkString(", "),
        "subjectType" -> subjectTypes.toString,
        "startTimestampNanos" -> startTimestampNanos
      )
  }

  case class IrPathNode(
     path: String
   ) extends IR with DBWritable {
    val uuid: UUID = DeterministicUUID(this)
    val originalEntities = Nil

    def asDBKeyValues =
      List(
        label, "IrPathNode",
        "titanType", "IrPathNode",
        "uuid", uuid,
        "path", path
      )

    def toMap =
      Map(
        "originalEntities" -> originalEntities.mkString(", "),
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
    uuid: UUID,
    originalEntities: Seq[UUID],

    fileObjectType: FileObjectType
  ) extends IR with DBWritable {

    def asDBKeyValues =
      List(
        label, "IrFileObject",
        "titanType", "IrFileObject",
        "uuid", uuid,
        "fileObjectType", fileObjectType
      )

    def toMap =
      Map(
        "originalEntities" -> originalEntities.mkString(", "),
        "fileObjectType" -> fileObjectType
      )
  }

  /* Compared to 'cdm.NetFlowObject' this leaves out
   *
   *  - ipProtocol
   *  - fileDescriptor
   */
  final case class IrNetFlowObject(
    originalEntities: Seq[UUID],

    localAddress: String,
    localPort: Int,
    remoteAddress: String,
    remotePort: Int
  ) extends IR with DBWritable {

    val uuid: UUID = DeterministicUUID(this)

    def asDBKeyValues =
      List(
        label, "IrNetFlowObject",
        "titanType", "IrNetflowObject",
        "uuid", uuid,
        "localAddress", localAddress,
        "localPort", localPort,
        "remoteAddress", remoteAddress,
        "remotePort", remotePort
      )

    def toMap =
      Map(
        "originalEntities" -> originalEntities.mkString(", "),
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
    uuid: UUID,
    originalEntities: Seq[UUID],

    srcSinkType: SrcSinkType
  ) extends IR with DBWritable {

    def asDBKeyValues =
      List(
        label, "IrSrcSinkObject",
        "titanType", "IrSrcSinkObject",
        "uuid", uuid,
        "srcSinkType", srcSinkType
      )

    def toMap =
      Map(
        "originalEntities" -> originalEntities.mkString(", "),
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
    originalEntities: Seq[UUID],

    userId: String,
    groupIds: Seq[String],
    principalType: PrincipalType = PRINCIPAL_LOCAL,
    username: Option[String] = None
  ) extends IR with DBWritable {

    val uuid: UUID = DeterministicUUID(this)

    def asDBKeyValues =
      List(
        label, "IrPrincipal",
        "titanType", "IrPrincipal",
        "uuid", uuid,
        "userId", userId,
        "principalType", principalType
      ) ++
        (if (groupIds.nonEmpty) List("groupIds", groupIds.mkString(", ")) else Nil) ++
        username.fold[List[Any]](Nil)(v => List("username", v))

    def toMap =
      Map(
        "originalEntities" -> originalEntities.mkString(", "),
        "userId" -> userId,
        "principalType" -> principalType,
        "groupIds" -> groupIds.mkString(", "),
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
    uuid: UUID,
    originalEntities: Seq[UUID],

    programPoint: Option[String] = None
  ) extends IR with DBWritable {

    type SelfType = IrProvenanceTagNode

    def asDBKeyValues =
      List(
        label, "IrProvenanceTagNode",
        "titanType", "IrProvenanceTagNode",
        "uuid", uuid
      ) ++
        programPoint.fold[List[Any]](Nil)(p => List("programPoint", p))

    def toMap =
      Map(
        "originalEntities" -> originalEntities.mkString(", "),
        "programPoint" -> programPoint.getOrElse("")
      )
  }

  final case class IrSynthesized(
    uuid: UUID,
    originalEntities: Seq[UUID]
  ) extends IR with DBWritable {

    def asDBKeyValues =
      List(
        label, "IrSynthesized",
        "titanType", "IrSynthesized",
        "uuid", uuid
      )

    def toMap =
      Map(
        "originalEntities" -> originalEntities.mkString(", ")
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
