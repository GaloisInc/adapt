package com.galois.adapt

import java.nio.ByteBuffer
import java.util
import java.util.{Arrays, Comparator, UUID}

import com.galois.adapt.AdaptConfig.HostName
import com.galois.adapt.adm.AdmUUID
import com.galois.adapt.cdm20._
import org.mapdb.{DataInput2, DataOutput2, Serializer}
import org.mapdb.serializer.GroupSerializer

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.language.implicitConversions
import com.rrwright.quine.language._
import shapeless._
import shapeless.record.Record
import com.github.blemale.scaffeine.{Cache, Scaffeine}

// TODO: convert `toMap` to use Shapeless. It is _begging_ to be done with shapeless
package object adm {

  sealed trait NamespacedUuid extends Product {
    val uuid: UUID
    val namespace: String
    def rendered: String
  }

  final case class CdmUUID(uuid: UUID, namespace: String) extends NamespacedUuid with Serializable { // extends AnyVal
    // Raw DB representation with namespace
    def rendered: String = if (this.namespace.isEmpty) { s"cdm_${uuid.toString}" } else { s"cdm_${this.namespace}_${uuid.toString}" }
  }
  object CdmUUID {
    // Decode raw DB representation
    def fromRendered(s: String): CdmUUID = {
      val (provider, uuid) = s.splitAt(s.length - 36)
      CdmUUID(UUID.fromString(uuid), provider.stripPrefix("cdm_").stripSuffix("_"))
    }
  }

  final case class AdmUUID(uuid: UUID, namespace: String) extends NamespacedUuid with Serializable { // extends AnyVal
    // Raw DB representation with namespace
    def rendered: String = if (this.namespace.isEmpty) { uuid.toString } else { s"${this.namespace}_${uuid.toString}" }
  }
  object AdmUUID {
    // Decode raw DB representation
    def fromRendered(s: String): AdmUUID = {
      val (provider, uuid) = s.splitAt(s.length - 36)
      AdmUUID(UUID.fromString(uuid), provider.stripSuffix("_"))
    }
  }

  implicit def cdmToTuple(c: CdmUUID): (UUID, String) = (c.uuid, c.namespace)
  implicit def admToTuple(a: AdmUUID): (UUID, String) = (a.uuid, a.namespace)

  implicit def orderingCdm: Ordering[CdmUUID] = new Ordering[CdmUUID] {
    override def compare(x: CdmUUID, y: CdmUUID) = {
      import scala.math.Ordered.orderingToOrdered
      (x.uuid, x.namespace) compare (y.uuid, y.namespace)
    }
  }

  implicit def orderingAdm: Ordering[AdmUUID] = new Ordering[AdmUUID] {
    override def compare(x: AdmUUID, y: AdmUUID) = {
      import scala.math.Ordered.orderingToOrdered
      (x.uuid, x.namespace) compare (y.uuid, y.namespace)
    }
  }

  implicit def unwrapAdmUUID(admUuid: AdmUUID): UUID = admUuid.uuid

  // Edges are now first class values in the stream.
  sealed trait Edge {
    def applyRemap(cdmUuids: Set[CdmUUID], admUUID: AdmUUID): Edge = this match {
      case EdgeCdm2Cdm(s, l, t) if cdmUuids.contains(s) => EdgeAdm2Cdm(admUUID, l, t)
      case EdgeCdm2Cdm(s, l, t) if cdmUuids.contains(t) => EdgeCdm2Adm(s, l, admUUID)
      case EdgeCdm2Adm(s, l, t) if cdmUuids.contains(s) => EdgeAdm2Adm(admUUID, l, t)
      case EdgeAdm2Cdm(s, l, t) if cdmUuids.contains(t) => EdgeAdm2Adm(s, l, admUUID)
      case e => e
    }

    def applyRemaps(cdmUuids: Set[CdmUUID], admUUID: AdmUUID): Edge = {
      var curr = this
      var next = this.applyRemap(cdmUuids, admUUID)
      while (curr != next) {
        curr = next
        next = curr.applyRemap(cdmUuids, admUUID)
      }
      curr
    }

    // Get the next CdmUUID in the edge (if there is one)
    def nextCdmUUID: Option[CdmUUID] = this match {
      case EdgeCdm2Cdm(cdm, _, _) => Some(cdm)
      case EdgeCdm2Adm(cdm, _, _) => Some(cdm)
      case EdgeAdm2Cdm(_, _, cdm) => Some(cdm)
      case EdgeAdm2Adm(_, _, _) => None
    }

    def applyCdmRemap(cdmUuidOld: CdmUUID, cdmUuidNew: CdmUUID): Edge = this match {
      case EdgeCdm2Cdm(s, l, t) if cdmUuidOld == s => EdgeCdm2Cdm(cdmUuidNew, l, t)
      case EdgeCdm2Cdm(s, l, t) if cdmUuidOld == t => EdgeCdm2Cdm(s, l, cdmUuidNew)
      case EdgeCdm2Adm(s, l, t) if cdmUuidOld == s => EdgeCdm2Adm(cdmUuidNew, l, t)
      case EdgeAdm2Cdm(s, l, t) if cdmUuidOld == t => EdgeAdm2Cdm(s, l, cdmUuidNew)
      case e => e
    }

    def applyAdmRemap(cdmUuid: CdmUUID, admUuid: AdmUUID): Edge = this match {
      case EdgeCdm2Cdm(s, l, t) if cdmUuid == s => EdgeAdm2Cdm(admUuid, l, t)
      case EdgeCdm2Cdm(s, l, t) if cdmUuid == t => EdgeCdm2Adm(s, l, admUuid)
      case EdgeCdm2Adm(s, l, t) if cdmUuid == s => EdgeAdm2Adm(admUuid, l, t)
      case EdgeAdm2Cdm(s, l, t) if cdmUuid == t => EdgeAdm2Adm(s, l, admUuid)
      case e => e
    }
  }
  final case class EdgeCdm2Cdm(src: CdmUUID, label: String, tgt: CdmUUID) extends Edge
  final case class EdgeCdm2Adm(src: CdmUUID, label: String, tgt: AdmUUID) extends Edge
  final case class EdgeAdm2Cdm(src: AdmUUID, label: String, tgt: CdmUUID) extends Edge
  final case class EdgeAdm2Adm(src: AdmUUID, label: String, tgt: AdmUUID) extends Edge with Serializable

  /* Stands for Adapt Data Model. This is generated from CDM by
   *
   *   - throwing out some structure that we don't know how to use
   *   - performing entity resolution
   */
  sealed trait ADM extends DBWritable with Product with DomainNode {
    val uuid: AdmUUID                  // The current UUID
    val originalCdmUuids: Set[CdmUUID] // The UUIDs of all the CDM nodes that were merged to produce this node

    def getHostName: Option[HostName]

    def toMap: Map[String, Any]        // A property map (keys are constant for a given type

    override def hashCode: Int = uuid.hashCode
    override def equals(that: Any): Boolean = that match {
      case a: ADM => uuid == a.uuid
      case _ => false
    }
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
    originalCdmUuids: Set[CdmUUID],

    eventType: EventType,
    earliestTimestampNanos: Long,
    latestTimestampNanos: Long,

    // 5D specific
    // See <https://git.tc.bbn.com/tc-all/cdm-docs/blob/master/ta1-fivedirections/operations/user_interaction.md>
    deviceType: Option[String],
    inputType: Option[String],

    hostName: HostName,
    provider: String
  ) extends ADM with DBWritable with DomainNode {

    def getHostName: Option[HostName] = Some(hostName)

    val uuid = AdmUUID(DeterministicUUID(originalCdmUuids.toList.sorted.map(_.uuid)), provider)

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "eventType" -> eventType.toString,
      "earliestTimestampNanos" -> earliestTimestampNanos,
      "latestTimestampNanos" -> latestTimestampNanos
    )  ++ (if (provider.isEmpty) Nil else List("provider" -> provider)) ++ {if (deviceType == None) Nil else List("deviceType" -> deviceType.get)} ++ (if (inputType == None) Nil else List("inputType" -> inputType.get))

    def toMap = Map(
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "eventType" -> eventType.toString,
      "earliestTimestampNanos" -> earliestTimestampNanos,
      "latestTimestampNanos" -> latestTimestampNanos,
      "provider" -> provider
    )

    object nodeConstants extends NodeConstants(Record(type_of = "AdmEvent"))
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
    originalCdmUuids: Set[CdmUUID],

    subjectTypes: Set[SubjectType],
    cid: Int,
    startTimestampNanos: Long,

    hostName: HostName,
    provider: String
  ) extends ADM with DBWritable with DomainNode {

    def getHostName: Option[HostName] = Some(hostName)

    val uuid = AdmUUID(DeterministicUUID(originalCdmUuids.toList.sorted.map(_.uuid)), provider)

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "subjectType" -> subjectTypes.map(_.toString).toList.sorted.mkString(";"),
      "cid" -> cid,
      "startTimestampNanos" -> startTimestampNanos
    ) ++
      (if (provider.isEmpty) { Nil } else { List("provider" -> provider) })

    def toMap = Map(
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "subjectType" -> subjectTypes.map(_.toString).toList.sorted.mkString(";"),
      "cid" -> cid,
      "startTimestampNanos" -> startTimestampNanos,
      "provider" -> provider
    )

    val nodeConstants = AdmSubject
  }
  object AdmSubject extends NodeConstants(Record(type_of = "AdmSubject"))

  case class AdmPathNode(
     path: String,
     provider: String
  ) extends ADM with DBWritable with DomainNode {
    val uuid = AdmUUID(DeterministicUUID(path), provider)
    val originalCdmUuids: Set[CdmUUID] = Set.empty

    def getHostName: Option[HostName] = None

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "path" -> path
    ) ++
      (if (provider.isEmpty) { Nil } else { List("provider" -> provider) })

    def toMap = Map(
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "path"-> path,
      "provider" -> provider
    )

    case object nodeConstants extends  NodeConstants(Record(type_of = "AdmPathNode"))
  }

  case object AdmPathNode {
   
    // Keep a cache of path nodes already seen. If we see the same input again, no point in
    // recomputing the output!
    val admPathNodeCacheSize = 10000
    val cache: Cache[(String, String, Boolean), AdmPathNode] = Scaffeine()
      .maximumSize(admPathNodeCacheSize)
      .build[(String, String, Boolean), AdmPathNode]()
    
    def normalized(path: String, provider: String, isWindows: Boolean): Option[AdmPathNode] = {

      // Start by checking the cache...
      val cacheKey = (path, provider, isWindows)
      for (admPathNode <- cache.getIfPresent(cacheKey)) {
        return Some(admPathNode)
      }

      var pathFixed: String = path.trim

      // Garbage
      if (pathFixed == "" || pathFixed == "<unknown>" || pathFixed == "unknown")
      return None

      val n: Int = pathFixed.length

      // For command lines, we try to rip off arguments.
      val end: Int = if (!isWindows) {

        // We don't try this for windows because their paths too often have unescaped spaces. Think "start menu" not
        // quoted and without an escaping '\' before the space.

        // This will produce the first index into the string
        @tailrec
        def commandEndIndex(acc: Int): Int = {
          if (acc >= n)
            return n

          pathFixed.charAt(acc) match {
            // Break on whitespace
            case c if c.isWhitespace => acc

            // Skip over the next character if the current one is a backslash
            case '\\' => commandEndIndex(acc + 2)

            // Whenever you encounter quotes, keep consuming characters until you find the matching quote on the other
            // side
            case c@('\"' | '\'') =>
              var j = acc + 1
              while (j < n && pathFixed.charAt(j) != c) j += 1
              commandEndIndex(j + 1)

            // For everything else just advance one charactet
            case _ => commandEndIndex(acc + 1)
          }
        }

        commandEndIndex(0)

      } else {

        // The only thing that is safe for windows is to take drop what comes after a quoted path.
        if (pathFixed.charAt(0) == '\"') {
          var j = 1
          while (j < n && pathFixed.charAt(j) != '\"') j += 1
          j + 1
        } else {
          n
        }
      }

      pathFixed = pathFixed.substring(0,end)

      // Some 5D paths have extra quotes around them: "\"C:\\ProgData\\ .... \\ ...\"". This step removes them
      if (pathFixed.startsWith("\"") && pathFixed.endsWith("\"") && pathFixed.length > 1) {
        pathFixed = pathFixed.substring(1, pathFixed.length - 1)
      }

      if (isWindows) {
        // Paths are often case insensitive
        pathFixed = pathFixed.toLowerCase

        // Some windows paths have path variables in them. We make a best effort to expand these
        pathFixed = pathFixed
          .replaceAll("%systemroot%","\\\\windows")
          .replaceAll("%windir%","\\\\windows")
          .replaceAll("%system32%","\\\\windows\\\\system32")
          .replaceAll("%programfiles%", "\\\\program files")
          .replaceAll("%osdrive%", "\\\\")
          .replaceAll("%systemdrive%", "\\\\")

        // Some windows paths start with "C:\\" and others with "\\". We strip off the "C:\\"
        if (pathFixed.startsWith("c:\\")) {
          pathFixed = pathFixed.substring(2, pathFixed.length)
        }

        // Ditto for "\\Device\\HarddiskVolume1" and "Device\\HarddiskVolume1"
        if (pathFixed.startsWith("\\device\\harddiskvolume1\\")) {
          pathFixed = pathFixed.substring(23, pathFixed.length)
        } else if (pathFixed.startsWith("device\\harddiskvolume1\\")) {
          pathFixed = pathFixed.substring(22, pathFixed.length)
        }
      }

      val (sep,splitSep) = if (isWindows) { ("\\", "\\\\") } else { ("/", "/") }
      var segs: List[String] = pathFixed.split(splitSep,-1).toList

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
          case "." | "" => { /* this adds no information, ignore it */ }
          case ".." => if (segsRev.isEmpty) { backhops += 1 } else { segsRev = segsRev.tail }
          case other => segsRev = other :: segsRev
        }
      }

      // This is for nonsense like `/../foo`.
      if (absolute && backhops > 0) return None

      // This is for filtering out paths that have no meaningful information
      if (segsRev.isEmpty && !absolute) return None

      val norm = (if (absolute) { sep } else { "" }) + ((1 to backhops).map(_ => "..") ++ segsRev.reverse).mkString(sep)
      val admPathNode = AdmPathNode(norm, provider)
      cache.put(cacheKey, admPathNode)
      Some(admPathNode)
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
     originalCdmUuids: Set[CdmUUID],

     fileObjectType: FileObjectType,
     size: Option[Long],

     hostName: HostName,
     provider: String
  ) extends ADM with DBWritable with DomainNode {

    val uuid = AdmUUID(DeterministicUUID(originalCdmUuids.toList.sorted.map(_.uuid)), provider)

    def getHostName: Option[HostName] = Some(hostName)

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "fileObjectType" -> fileObjectType.toString,
      "hostName" -> hostName
    ) ++
      size.fold[List[(String,Any)]](Nil)(v => List("size" -> v)) ++
      (if (provider.isEmpty) { Nil } else { List("provider" -> provider) })

    def toMap = Map(
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "fileObjectType" -> fileObjectType.toString,
      "size" -> size.getOrElse(""),
      "provider" -> provider,
      "hostName" -> hostName
    )

    val nodeConstants = AdmFileObject
  }
  object AdmFileObject extends NodeConstants(Record(type_of = "AdmFileObject"))

  /* Compared to 'cdm.NetFlowObject' this leaves out
   *
   *  - ipProtocol
   *  - fileDescriptor
   *
   *  It also splits ports and addresses into seperate nodes
   */
  final case class AdmNetFlowObject(
    originalCdmUuids: Set[CdmUUID],

    localAddress: Option[String],
    localPort: Option[Int],
    remoteAddress: Option[String],
    remotePort: Option[Int],

    provider: String
  ) extends ADM with DBWritable with DomainNode {

    val uuid = AdmUUID({
        val detUuidOpt = for {
          la <- localAddress
          lp <- localPort
          ra <- remoteAddress
          rp <- remotePort
        } yield DeterministicUUID(la, lp, ra, rp)

        detUuidOpt.getOrElse(DeterministicUUID(originalCdmUuids.toList.sorted.map(_.uuid)))
      },
      provider
    )

    def getHostName: Option[HostName] = None

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";")
    ) ++
      localAddress.fold[List[(String,Any)]](Nil)(v => List("localAddress" -> v)) ++
      localPort.fold[List[(String,Any)]](Nil)(v => List("localPort" -> v)) ++
      remoteAddress.fold[List[(String,Any)]](Nil)(v => List("remoteAddress" -> v)) ++
      remotePort.fold[List[(String,Any)]](Nil)(v => List("remotePort" -> v)) ++
      (if (provider.isEmpty) { Nil } else { List("provider" -> provider) })

    def toMap = Map(
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "localAddress" -> localAddress,
      "localPort" -> localPort,
      "remoteAddress" -> remoteAddress,
      "remotePort" -> remotePort,
      "provider" -> provider
    )

    val nodeConstants = AdmNetFlowObject
  }
  object AdmNetFlowObject extends NodeConstants(Record(type_of = "AdmNetFlowObject"))

  // Represents a local and/or remote address of netflows
  final case class AdmAddress(
    address: String
  ) extends ADM with DBWritable with DomainNode {

    val uuid = AdmUUID(DeterministicUUID(address), "")
    override val originalCdmUuids: Set[CdmUUID] = Set.empty

    def getHostName: Option[HostName] = None

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "address" -> address
    )

    def toMap = Map(
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "address" -> address
    )

    val nodeConstants = AdmAddress
  }
  object AdmAddress extends NodeConstants(Record(type_of = "AdmAddress"))

  // Represents a local and/or remote port of netflows
  final case class AdmPort(
    port: Int
  ) extends ADM with DBWritable with DomainNode {

    val uuid = AdmUUID(DeterministicUUID(port.toString), "")
    override val originalCdmUuids: Set[CdmUUID] = Set.empty

    def getHostName: Option[HostName] = None

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "port" -> port
    )

    def toMap = Map(
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "port" -> port
    )

    val nodeConstants = AdmPort
  }
  object AdmPort extends NodeConstants(Record(type_of = "AdmPort"))

  /* Compared to 'cdm.SrcSinkObject' this leaves out
   *
   *  - fileDescriptor
   */
  final case class AdmSrcSinkObject(
    originalCdmUuids: Set[CdmUUID],

    srcSinkType: SrcSinkType,

    hostName: String,
    provider: String
  ) extends ADM with DBWritable with DomainNode {

    val uuid = AdmUUID(DeterministicUUID(originalCdmUuids.toList.sorted.map(_.uuid)), provider)

    def getHostName: Option[HostName] = Some(hostName)

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "srcSinkType" -> srcSinkType.toString,
      "hostName" -> hostName
    ) ++
      (if (provider.isEmpty) { Nil } else { List("provider" -> provider) })

    def toMap = Map(
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "srcSinkType" -> srcSinkType.toString,
      "provider" -> provider,
      "hostName" -> hostName
    )

    val nodeConstants = AdmSrcSinkObject
  }
  object AdmSrcSinkObject extends NodeConstants(Record(type_of = "AdmSrcSinkObject"))

  /* These don't occur very much, so we are likely to do lots of analysis on them. Still worth
   * distinguishing the regular and super users. Compared to 'cdm.Principal', this only omits the
   * property map.
   *
   * TODO: get rid of this in favor of an enumeration (this is a lot of edges for not much)
   */
  final case class AdmPrincipal(
    originalCdmUuids: Set[CdmUUID],

    userId: String,
    groupIds: Seq[String],
    principalType: PrincipalType = PRINCIPAL_LOCAL,
    username: Option[String] = None,

    hostName: String,
    provider: String
  ) extends ADM with DBWritable with DomainNode {

    val uuid = AdmUUID(
      DeterministicUUID(userId + groupIds.sorted.mkString("") + principalType.toString + username + provider),
      provider
    )

    def getHostName: Option[HostName] = Some(hostName)

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "userId" -> userId,
      "principalType" -> principalType.toString,
      "hostName" -> hostName
    ) ++
      (if (groupIds.nonEmpty) List("groupIds" -> groupIds.mkString(",")) else Nil) ++
      username.fold[List[(String,Any)]](Nil)(v => List("username" -> v)) ++
      (if (provider.isEmpty) { Nil } else { List("provider" -> provider) })

    def toMap = Map(
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "userId" -> userId,
      "principalType" -> principalType,
      "groupIds" -> groupIds.toList.sorted.mkString(";"),
      "username" -> username.getOrElse(""),
      "provider" -> provider,
      "hostName" -> hostName
    )

    val nodeConstants = AdmPrincipal
  }
  object AdmPrincipal extends NodeConstants(Record(type_of = "AdmPrincipal"))

  /* Compared to 'cdm.ProvenanceTagNode', this leaves out
   *
   *  - 'systemCall'
   *  - 'opcode'
   *  - 'itag'
   *  - 'ctag'
   */
  final case class AdmProvenanceTagNode(
    originalCdmUuids: Set[CdmUUID],

    programPoint: Option[String] = None,

    hostName: String,
    provider: String
  ) extends ADM with DBWritable with DomainNode {

    val uuid = AdmUUID(DeterministicUUID(originalCdmUuids.toList.sorted.map(_.uuid)), provider)

    def getHostName: Option[HostName] = Some(hostName)

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "hostName" -> hostName
    ) ++
      programPoint.fold[List[(String,Any)]](Nil)(p => List("programPoint" -> p)) ++
      (if (provider.isEmpty) { Nil } else { List("provider" -> provider) })


    def toMap = Map(
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "programPoint" -> programPoint.getOrElse(""),
      "provider" -> provider,
      "hostName" -> hostName
    )

    val nodeConstants = AdmProvenanceTagNode
  }
  object AdmProvenanceTagNode extends NodeConstants(Record(type_of = "AdmProvenanceTagNode"))

  final case class AdmHost(
    originalCdmUuids: Set[CdmUUID],          // universally unique identifier for the host

    hostName: String,                        // hostname or machine name
    hostIdentifiers: Seq[HostIdentifier],    // list of identifiers, such as serial number, IMEI number
    osDetails: Option[String],               // OS level details revealed by tools such as uname -a
    hostType: HostType,                      // host's role or device type, such as mobile, server, desktop
    interfaces: Seq[Interface],              // names and addresses of network interfaces

    provider: String
  ) extends ADM with DBWritable with DomainNode {

    def getHostName: Option[HostName] = Some(hostName)

    val uuid = AdmUUID(DeterministicUUID(originalCdmUuids.toList.sorted.map(_.uuid)), provider)

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,

      "hostName" -> hostName,
      "hostIdentifiers" -> hostIdentifiers.map(_.toString).mkString(";"),
      "hostType" -> hostType.toString,
      "interfaces" -> interfaces.map(_.toString).mkString(";"),

      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";")
    ) ++
      osDetails.fold[List[(String,Any)]](Nil)(p => List("osDetails" -> p)) ++
      (if (provider.isEmpty) { Nil } else { List("provider" -> provider) })

    def toMap = Map(
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";"),
      "hostName" -> hostName,
      "hostIdentifiers" -> hostIdentifiers.map(_.toString).mkString(";"),
      "osDetails" -> osDetails,
      "hostType" -> hostType.toString,
      "interfaces" -> interfaces.map(_.toString).mkString(";"),
      "provider" -> provider
    )

    val nodeConstants = AdmHost
  }
  object AdmHost extends NodeConstants(Record(type_of = "AdmHost"))

  // TODO: make this deterministic
  final case class AdmSynthesized(
    originalCdmUuids: Set[CdmUUID]
  ) extends ADM with DBWritable with DomainNode {

    def getHostName: Option[HostName] = None

    val uuid = {
      val original = originalCdmUuids.toList.sorted
      AdmUUID(DeterministicUUID(original.map(_.uuid)), original.headOption.fold("")(_.namespace))
    }

    def asDBKeyValues = List(
      "uuid" -> uuid.uuid,
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";")
    )

    def toMap = Map(
      "originalCdmUuids" -> originalCdmUuids.map(_.uuid).toList.sorted.mkString(";")
    )

    val nodeConstants = AdmSynthesized
  }
  object AdmSynthesized extends NodeConstants(Record(type_of = "AdmSynthesized"))

}

object DeterministicUUID {

  // Deterministically build a UUID from a String 
  final def apply(str: String): UUID = cacheApply1.get(str, applyUncached(_))
  
  private val cacheApply1: Cache[String, UUID] = Scaffeine()
    .maximumSize(1000)
    .build[String, UUID]()
    
  private def applyUncached(str: String): UUID = UUID.nameUUIDFromBytes(str.getBytes)

  // Deterministically build a UUID from a sequence of UUIDs
  final def apply(fields: Seq[UUID]): UUID = cacheApply2.get(fields, applyUncached(_))
  
  private val cacheApply2: Cache[Seq[UUID], UUID] = Scaffeine()
    .maximumSize(1000)
    .build[Seq[UUID], UUID]()
    
  private def applyUncached(fields: Seq[UUID]): UUID = {
    val byteBuffer: ByteBuffer = ByteBuffer.allocate(8 * 2 * fields.length)
    for (value <- fields) {
      byteBuffer.putLong(value.getLeastSignificantBits)
      byteBuffer.putLong(value.getMostSignificantBits)
    }
    UUID.nameUUIDFromBytes(byteBuffer.array())
  }

  def apply(localAddress: String, localPort: Int, remoteAddress: String, remotePort: Int): UUID =
    DeterministicUUID(localAddress + localPort + remoteAddress + remotePort)
}
