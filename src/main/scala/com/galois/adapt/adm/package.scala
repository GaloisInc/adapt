package com.galois.adapt

import java.io.ByteArrayOutputStream
import java.util.UUID

import com.galois.adapt.cdm17._

import scala.language.implicitConversions

// TODO: convert `toMap` to use Shapeless. It is _begging_ to be done with shapeless
package object adm {

  case class CdmUUID(uuid: UUID) extends AnyVal
  case class AdmUUID(uuid: UUID) extends AnyVal

  implicit def unwrapAdmUUID(admUuid: AdmUUID): UUID = admUuid.uuid

  // Edges are now first class values in the stream.
  sealed trait Edge[From, To]
  final case class EdgeCdm2Cdm(src: CdmUUID, label: String, tgt: CdmUUID) extends Edge[CDM17, CDM17]
  final case class EdgeCdm2Adm(src: CdmUUID, label: String, tgt: AdmUUID) extends Edge[CDM17, ADM]
  final case class EdgeAdm2Cdm(src: AdmUUID, label: String, tgt: CdmUUID) extends Edge[ADM, CDM17]
  final case class EdgeAdm2Adm(src: AdmUUID, label: String, tgt: AdmUUID) extends Edge[ADM, ADM]

  /* Stands for Adapt Data Model. This is generated from CDM by
   *
   *   - throwing out some structure that we don't know how to use
   *   - performing entity resolution
   */
  sealed trait ADM extends DBWritable {
    val uuid: AdmUUID                  // The current UUID
    val originalCdmUuids: Seq[CdmUUID] // The UUIDs of all the CDM nodes that were merged to produce this node

    def toMap: Map[String, Any]        // A property map (keys are constant for a given type
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
  final case class ADMEvent(
    uuid: AdmUUID,
    originalCdmUuids: Seq[CdmUUID],

    eventType: EventType,
    earliestTimestampNanos: Long,
    latestTimestampNanos: Long
  ) extends ADM with DBWritable {

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "eventType" -> eventType.toString,
      "earliestTimestampNanos" -> earliestTimestampNanos,
      "latestTimestampNanos" -> latestTimestampNanos
    )

    def toMap = Map(
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "eventType" -> eventType.toString,
      "earliestTimestampNanos" -> earliestTimestampNanos,
      "latestTimestampNanos" -> latestTimestampNanos
    )
  }


  /* Compared to 'cdm.Subject', the following are omitted
   *
   *  - 'unitId'
   *  - 'iteration'
   *  - 'count'
   *  - 'privilegeLevel'
   *  - 'importedLibraries' and 'exportedLibraries' aren't used
   */
  final case class ADMSubject(
    uuid: AdmUUID,
    originalCdmUuids: Seq[CdmUUID],

    subjectTypes: Set[SubjectType],
    cid: Int,
    startTimestampNanos: Long
  ) extends ADM with DBWritable {

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "subjectType" -> subjectTypes.map(_.toString).toList.sorted.mkString(";"),
      "cid" -> cid,
      "startTimestampNanos" -> startTimestampNanos
    )

    def toMap = Map(
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "subjectType" -> subjectTypes.map(_.toString).toList.sorted.mkString(";"),
      "cid" -> cid,
      "startTimestampNanos" -> startTimestampNanos
    )
  }

  case class ADMPathNode(
     path: String
   ) extends ADM with DBWritable {
    val uuid = AdmUUID(DeterministicUUID(this))
    val originalCdmUuids: Seq[CdmUUID] = Nil

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "path" -> path
    )

    def toMap = Map(
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
  final case class ADMFileObject(
     uuid: AdmUUID,
     originalCdmUuids: Seq[CdmUUID],

     fileObjectType: FileObjectType,
     size: Option[Long]
  ) extends ADM with DBWritable {

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "fileObjectType" -> fileObjectType.toString
    ) ++
      size.fold[List[(String,Any)]](Nil)(v => List("size" -> v))

    def toMap = Map(
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "fileObjectType" -> fileObjectType.toString,
      "size" -> size.getOrElse("")
    )
  }

  /* Compared to 'cdm.NetFlowObject' this leaves out
   *
   *  - ipProtocol
   *  - fileDescriptor
   */
  final case class ADMNetFlowObject(
    originalCdmUuids: Seq[CdmUUID],

    localAddress: String,
    localPort: Int,
    remoteAddress: String,
    remotePort: Int
  ) extends ADM with DBWritable {

    val uuid = AdmUUID(DeterministicUUID(this))

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "localAddress" -> localAddress,
      "localPort" -> localPort,
      "remoteAddress" -> remoteAddress,
      "remotePort" -> remotePort
    )

    def toMap = Map(
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
  final case class ADMSrcSinkObject(
    uuid: AdmUUID,
    originalCdmUuids: Seq[CdmUUID],

    srcSinkType: SrcSinkType
  ) extends ADM with DBWritable {

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "srcSinkType" -> srcSinkType.toString
    )

    def toMap = Map(
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "srcSinkType" -> srcSinkType.toString
    )
  }

  /* These don't occur very much, so we are likely to do lots of analysis on them. Still worth
   * distinguishing the regular and super users. Compared to 'cdm.Principal', this only omits the
   * property map.
   *
   * TODO: get rid of this in favor of an enumeration (this is a lot of edges for not much)
   */
  final case class ADMPrincipal(
    originalCdmUuids: Seq[CdmUUID],

    userId: String,
    groupIds: Seq[String],
    principalType: PrincipalType = PRINCIPAL_LOCAL,
    username: Option[String] = None
  ) extends ADM with DBWritable {

    val uuid = AdmUUID(DeterministicUUID(this))

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "userId" -> userId,
      "principalType" -> principalType.toString
    ) ++
      (if (groupIds.nonEmpty) List("groupIds" -> groupIds.mkString(", ")) else Nil) ++
      username.fold[List[(String,Any)]](Nil)(v => List("username" -> v))

    def toMap = Map(
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
   */
  final case class ADMProvenanceTagNode(
    uuid: AdmUUID,
    originalCdmUuids: Seq[CdmUUID],

    programPoint: Option[String] = None
  ) extends ADM with DBWritable {

    type SelfType = ADMProvenanceTagNode

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";")
    ) ++
      programPoint.fold[List[(String,Any)]](Nil)(p => List("programPoint" -> p))

    def toMap = Map(
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "programPoint" -> programPoint.getOrElse("")
    )
  }

  // TODO: consider emitting this on timeout
  final case class ADMSynthesized(
    uuid: AdmUUID,
    originalCdmUuids: Seq[CdmUUID]
  ) extends ADM with DBWritable {

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";")
    )

    def toMap = Map(
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";")
    )
  }

}

object DeterministicUUID {

  // Utility function for generating deterministic UUIDs from case classes.
  def apply[T <: Product](product: T): UUID = {
    val byteOutputStream: ByteArrayOutputStream = new ByteArrayOutputStream()
    for (value <- product.productIterator) {
      byteOutputStream.write(value.hashCode())
    }
    UUID.nameUUIDFromBytes(byteOutputStream.toByteArray)
  }

}
