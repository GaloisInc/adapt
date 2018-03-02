package com.galois.adapt

import java.nio.ByteBuffer
import java.util
import java.util.{Arrays, Comparator, UUID}

import com.galois.adapt.cdm18._
import org.mapdb.{DataInput2, DataOutput2, Serializer}
import org.mapdb.serializer.GroupSerializer

import scala.collection.mutable.ListBuffer
import scala.language.implicitConversions

// TODO: convert `toMap` to use Shapeless. It is _begging_ to be done with shapeless
package object adm {

  case class CdmUUID(uuid: UUID) extends AnyVal
  case class AdmUUID(uuid: UUID) extends AnyVal

  implicit def orderingCdm: Ordering[CdmUUID] = new Ordering[CdmUUID] {
    override def compare(x: CdmUUID, y: CdmUUID) = {
      x.uuid.compareTo(y.uuid)
    }
  }

  implicit def orderingAdm: Ordering[AdmUUID] = new Ordering[AdmUUID] {
    override def compare(x: AdmUUID, y: AdmUUID) = {
      x.uuid.compareTo(y.uuid)
    }
  }

  implicit def unwrapAdmUUID(admUuid: AdmUUID): UUID = admUuid.uuid

  // Edges are now first class values in the stream.
  sealed trait Edge[From, To]
  final case class EdgeCdm2Cdm(src: CdmUUID, label: String, tgt: CdmUUID) extends Edge[CDM18, CDM18]
  final case class EdgeCdm2Adm(src: CdmUUID, label: String, tgt: AdmUUID) extends Edge[CDM18, ADM]
  final case class EdgeAdm2Cdm(src: AdmUUID, label: String, tgt: CdmUUID) extends Edge[ADM, CDM18]
  final case class EdgeAdm2Adm(src: AdmUUID, label: String, tgt: AdmUUID) extends Edge[ADM, ADM] with Serializable

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
  final case class AdmEvent(
    originalCdmUuids: Seq[CdmUUID],

    eventType: EventType,
    earliestTimestampNanos: Long,
    latestTimestampNanos: Long
  ) extends ADM with DBWritable {

    val uuid = AdmUUID(DeterministicUUID(originalCdmUuids.sorted.map(_.uuid)))

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
  final case class AdmSubject(
    originalCdmUuids: Seq[CdmUUID],

    subjectTypes: Set[SubjectType],
    cid: Int,
    startTimestampNanos: Long
  ) extends ADM with DBWritable {

    val uuid = AdmUUID(DeterministicUUID(originalCdmUuids.sorted.map(_.uuid)))

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

  case class AdmPathNode(
     path: String
   ) extends ADM with DBWritable {
    val uuid = AdmUUID(DeterministicUUID(path))
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

  case object AdmPathNode {
    def normalized(path: String): Option[AdmPathNode] = {

      // Garbage
      if (path == "" || path == "<unknown>")
        return None

      var segs: List[String] = path.trim.split("/",-1).toList

      val absolute: Boolean = if (segs.head == "") {
        segs = segs.tail
        true
      } else {
        false
      }

      var segsRev: List[String] = List.empty
      var backhops: Int = 0

      for (seg <- segs) {
        seg match {
          case "." => { /* this adds no information, ignore it */ }
          case ".." => if (segsRev.isEmpty) { backhops += 1 } else { segsRev = segsRev.tail }
          case other => segsRev = other :: segsRev
        }
      }

      // This is for nonsense like `/../foo`.
      if (absolute && backhops > 0) return None

      // This is for filtering out paths that have no meaningful information
      if (segsRev.isEmpty && !absolute) return None

      val norm = (if (absolute) { "/" } else { "" }) + ((1 to backhops).map(_ => "..") ++ segsRev.reverse).mkString("/")
      Some(AdmPathNode(norm))
    }
  }

  /* Compared to 'cdm.FileObject', this leaves out
   *
   *  - fileDescriptor
   *  - size
   *  - peInfo
   *  - hashes
   */
  final case class AdmFileObject(
     originalCdmUuids: Seq[CdmUUID],

     fileObjectType: FileObjectType,
     size: Option[Long]
  ) extends ADM with DBWritable {

    val uuid = AdmUUID(DeterministicUUID(originalCdmUuids.sorted.map(_.uuid)))

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
   *
   *  It also splits ports and addresses into seperate nodes
   */
  final case class AdmNetFlowObject(
    originalCdmUuids: Seq[CdmUUID],

    localAddress: String,
    localPort: Int,
    remoteAddress: String,
    remotePort: Int
  ) extends ADM with DBWritable {

    val uuid = AdmUUID(DeterministicUUID(localAddress + localPort + remoteAddress + remotePort))

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

  // Represents a local and/or remote address of netflows
  final case class AdmAddress(
    address: String
  ) extends ADM with DBWritable {

    val uuid = AdmUUID(DeterministicUUID(address))
    override val originalCdmUuids: Seq[CdmUUID] = List.empty

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "address" -> address
    )

    def toMap = Map(
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "address" -> address
    )
  }

  // Represents a local and/or remote port of netflows
  final case class AdmPort(
    port: Int
  ) extends ADM with DBWritable {

    val uuid = AdmUUID(DeterministicUUID(port.toString))
    override val originalCdmUuids: Seq[CdmUUID] = List.empty

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "port" -> port
    )

    def toMap = Map(
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "port" -> port
    )
  }

  /* Compared to 'cdm.SrcSinkObject' this leaves out
   *
   *  - fileDescriptor
   */
  final case class AdmSrcSinkObject(
    originalCdmUuids: Seq[CdmUUID],

    srcSinkType: SrcSinkType
  ) extends ADM with DBWritable {

    val uuid = AdmUUID(DeterministicUUID(originalCdmUuids.sorted.map(_.uuid)))

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
  final case class AdmPrincipal(
    originalCdmUuids: Seq[CdmUUID],

    userId: String,
    groupIds: Seq[String],
    principalType: PrincipalType = PRINCIPAL_LOCAL,
    username: Option[String] = None
  ) extends ADM with DBWritable {

    val uuid = AdmUUID(DeterministicUUID(originalCdmUuids.sorted.map(_.uuid)))

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "userId" -> userId,
      "principalType" -> principalType.toString
    ) ++
      (if (groupIds.nonEmpty) List("groupIds" -> groupIds.mkString(",")) else Nil) ++
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
  final case class AdmProvenanceTagNode(
    originalCdmUuids: Seq[CdmUUID],

    programPoint: Option[String] = None
  ) extends ADM with DBWritable {

    val uuid = AdmUUID(DeterministicUUID(originalCdmUuids.sorted.map(_.uuid)))

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

  // TODO: make this deterministic
  final case class AdmSynthesized(
    originalCdmUuids: Seq[CdmUUID]
  ) extends ADM with DBWritable {

    val uuid = AdmUUID(DeterministicUUID(originalCdmUuids.sorted.map(_.uuid)))

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

  def apply(str: String): UUID = {
    UUID.nameUUIDFromBytes(str.getBytes)
  }

  def apply(fields: Seq[UUID]): UUID = {
    val byteBuffer: ByteBuffer = ByteBuffer.allocate(8 * 2 * fields.length)
    for (value <- fields) {
      byteBuffer.putLong(value.getLeastSignificantBits)
      byteBuffer.putLong(value.getMostSignificantBits)
    }
    UUID.nameUUIDFromBytes(byteBuffer.array())
  }

}
