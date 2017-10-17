package com.galois.adapt

import java.util.UUID

import scala.concurrent._

import org.apache.tinkerpop.gremlin.structure.T.label
import com.galois.adapt.cdm17._
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout

// TODO: put the remap functions in the IR trait

package object ir {
  
  sealed trait IR { self => 
    // F-bound modelled as type member. We need this type to give a more precise type to 'remapUuids' 
    type SelfType >: self.type <: IR

    val uuid: UUID;
    val originalEntities: Seq[UUID]  // TODO: is this really a Seq?
   
    // Defines how to remap UUIDs from CDM to IR
    def remapUuids(renameActor: ActorRef, createIfNotFound: Boolean)
                  (implicit ec: ExecutionContext, t: Timeout): Future[(SelfType, Seq[UUID])]
  }

  import RemapUtils._


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
    subject: UUID,
    earliestTimestampNanos: Long,
    latestTimestampNanos: Long,
    exec: Option[String] = None,    // On cadets - tells you what command (if any) led to this event (eg: 'sh')

    predicateObject: Option[UUID] = None,
    predicateObject2: Option[UUID] = None
  ) extends IR with DBWritable with DBNodeable {

    type SelfType = IrEvent

    def asDBKeyValues =
      List(
        label, "IrEvent",
        "titanType", "IrEvent",
        "uuid", uuid,
        "eventType", eventType,
        "subjectUuid", subject,
        "earliestTimestampNanos", earliestTimestampNanos,
        "latestTimestampNanos", latestTimestampNanos,
        "exec", exec
      ) ++
      exec.fold[List[Any]](Nil)(v => List("exec", v)) ++
      predicateObject.fold[List[Any]](Nil)(v => List("predicateObjectUuid", v)) ++
      predicateObject2.fold[List[Any]](Nil)(v => List("predicateObject2Uuid", v))

    def asDBEdges = List(("subject", subject)) ++ List.concat(
      predicateObject.map(p => ("predicateObject", p)),
      predicateObject2.map(p => ("predicateObject2", p))
    )

    def getUuid = uuid

    def remapUuids(ra: ActorRef, c: Boolean)
                  (implicit ec: ExecutionContext, t: Timeout): Future[(IrEvent,Seq[UUID])] = for {
      (s, n1) <- remapUuid(ra, subject, c)
      (po1, n2) <- remapOptUuid(ra, predicateObject, c)
      (po2, n3) <- remapOptUuid(ra, predicateObject2, c)
    } yield (this.copy(subject = s, predicateObject = po1, predicateObject2 = po2), n1 ++ n2 ++ n3)

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

    subjectType: SubjectType,
    localPrincipal: UUID,
    startTimestampNanos: Long,
    cmdLine: Option[String],
    parentSubject: Option[UUID] = None
  ) extends IR with DBWritable with DBNodeable {
    
    type SelfType = IrSubject
  
    def asDBKeyValues =
      List(
        label, "IrSubject",
        "titanType", "IrSubject",
        "uuid", uuid,
        "subjectType", subjectType,
        "localPrincipalUuid", localPrincipal,
        "startTimestampNanos", startTimestampNanos,
        "cmdLine", cmdLine
      ) ++
      parentSubject.fold[List[Any]](Nil)(v => List("parentSubjectUuid", v))

    def asDBEdges = List(("localPrincipal",localPrincipal)) ++ 
      parentSubject.fold[List[(String,UUID)]](Nil)(v => List(("parentSubject", v)))

    def getUuid = uuid

    def remapUuids(ra: ActorRef, c: Boolean)
                  (implicit ec: ExecutionContext, t: Timeout): Future[(IrSubject, Seq[UUID])] = for {
      (p, n1) <- remapUuid(ra, localPrincipal, c)
      (ps, n2) <- remapOptUuid(ra, parentSubject, c)
    } yield (this.copy(localPrincipal = p, parentSubject = ps), n1 ++ n2)

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

    path: Option[String],
    fileObjectType: FileObjectType,
    localPrincipal: Option[UUID] = None
  ) extends IR with DBWritable with DBNodeable {
     
    type SelfType = IrFileObject

    def asDBKeyValues =
      List(
        label, "IrFileObject",
        "titanType", "IrFileObject",
        "uuid", uuid,
        "fileObjectType", fileObjectType
      ) ++
      path.fold[List[Any]](Nil)(v => List("path", path)) ++
      localPrincipal.fold[List[Any]](Nil)(v => List("localPrincipalUuid", v))

    def asDBEdges = List.concat(
      localPrincipal.map(p => ("localPrincipal", p))
    )

    def getUuid = uuid

    def remapUuids(ra: ActorRef, c: Boolean)
                  (implicit ec: ExecutionContext, t: Timeout): Future[(IrFileObject, Seq[UUID])] = for {
      (p, n) <- remapOptUuid(ra, localPrincipal, c)
    } yield (this.copy(localPrincipal = p), n)
  
  }

  /* Compared to 'cdm.NetFlowObject' this leaves out
   *
   *  - ipProtocol
   *  - fileDescriptor
   */
  final case class IrNetFlowObject(
    uuid: UUID,
    originalEntities: Seq[UUID],

    localAddress: String,
    localPort: Int,
    remoteAddress: String,
    remotePort: Int
  ) extends IR with DBNodeable with DBWritable {
     
    type SelfType = IrNetFlowObject
    
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

    def asDBEdges = Nil

    def getUuid = uuid

    def remapUuids(ra: ActorRef, c: Boolean)
                  (implicit ec: ExecutionContext, t: Timeout): Future[(IrNetFlowObject, Seq[UUID])] =
      Future { (this, Nil) }
  
  }

  
  /* Compared to 'cdm.SrcSinkObject' this leaves out
   *
   *  - fileDescriptor
   */
  final case class IrSrcSinkObject(
    uuid: UUID,
    originalEntities: Seq[UUID],
    
    srcSinkType: SrcSinkType
  ) extends IR with DBNodeable with DBWritable {
     
    type SelfType = IrSrcSinkObject

    def asDBKeyValues = 
      List(
        label, "IrSrcSinkObject",
        "titanType", "IrSrcSinkObject",
        "uuid", uuid,
        "srcSinkType", srcSinkType
      )

    def asDBEdges = Nil

    def getUuid = uuid

    def remapUuids(ra: ActorRef, c: Boolean)
                  (implicit ec: ExecutionContext, t: Timeout): Future[(IrSrcSinkObject, Seq[UUID])] =
      Future { (this, Nil) }

  }

  /* These don't occur very much, so we are likely to do lots of analysis on them. Still worth
   * distinguishing the regular and super users. Compared to 'cdm.Principal', this only omits the
   * property map.
   *
   * TODO: get rid of this in favor of an enumeration (this is a lot of edges for not much)
   */
  final case class IrPrincipal(
    uuid: UUID,
    originalEntities: Seq[UUID],

    userId: String,
    groupIds: Seq[String],
    principalType: PrincipalType = PRINCIPAL_LOCAL,
    username: Option[String] = None
  ) extends IR with DBNodeable with DBWritable {
     
    type SelfType = IrPrincipal
    
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

    def asDBEdges = Nil

    def getUuid = uuid

    def remapUuids(ra: ActorRef, c: Boolean)
                  (implicit ec: ExecutionContext, t: Timeout): Future[(IrPrincipal, Seq[UUID])] =
      Future { (this, Nil) }
    
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
    
    subjectUuid: UUID,
    flowObject: Option[UUID] = None,
    programPoint: Option[String] = None,
    prevTagId: Option[UUID] = None,
    tagIds: Seq[UUID] = Seq()
  ) extends IR with DBNodeable with DBWritable {
  
    type SelfType = IrProvenanceTagNode
    
    def asDBKeyValues =
      List(
        label, "IrProvenanceTagNode",
        "titanType", "IrProvenanceTagNode",
        "uuid", uuid,
        "subjectUuid", subjectUuid
      ) ++
      flowObject.fold[List[Any]](Nil)(f => List("flowObjectUuid", f)) ++
      programPoint.fold[List[Any]](Nil)(p => List("programPoint", p)) ++
      prevTagId.fold[List[Any]](Nil)(p => List("prevTagIdUuid", p)) ++
      tagIds.flatMap(t => List("tagIdUuid", t))

      
    def asDBEdges = List(("subject",subjectUuid)) ++
      flowObject.fold[List[(String,UUID)]](Nil)(f => List(("flowObject", f))) ++
      prevTagId.fold[List[(String,UUID)]](Nil)(p => List(("prevTagId", p))) ++
      tagIds.map(t => ("tagId", t))

    def getUuid = uuid

    def remapUuids(ra: ActorRef, c: Boolean)
                  (implicit ec: ExecutionContext, t: Timeout): Future[(IrProvenanceTagNode, Seq[UUID])] = for {
      (s, n1) <- remapUuid(ra, subjectUuid, c)
      (fo, n2) <- remapOptUuid(ra, flowObject, c)
      (pt, n3) <- remapOptUuid(ra, prevTagId, c)
      (ts, n4) <- remapSeqUuid(ra, tagIds, c)
    } yield (this.copy(
      subjectUuid = s,
      flowObject = fo,
      prevTagId = pt,
      tagIds = ts
    ),
      n1 ++ n2 ++ n3 ++ n4
    )

  }

  final case class IrSynthesized(
    uuid: UUID
  ) extends IR with DBNodeable with DBWritable {
    
    val originalEntities = Nil
    
    type SelfType = IrSynthesized
    
    def getUuid = uuid

    def asDBKeyValues =
      List(
        label, "IrSynthesized",
        "titanType", "IrSynthesized",
        "uuid", uuid
      )

    def asDBEdges = Nil

    def remapUuids(ra: ActorRef, c: Boolean)
                  (implicit ec: ExecutionContext, t: Timeout): Future[(IrSynthesized, Seq[UUID])] = 
      throw new Exception("IrSynthesized nodes cannot be remapped")
  }

  // Utilities for remapping UUIDs. Used in the IR's implementation of 'remapUuids'
  private object RemapUtils {
    def remapUuid(renameActor: ActorRef, uuid: UUID, createIfNotFound: Boolean)
                 (implicit ec: ExecutionContext, t: Timeout): Future[(UUID, Seq[UUID])] = {
      (renameActor ? Get(uuid))
        .mapTo[MapMessage[UUID,UUID]]
        .flatMap {
          case Val(None) => Future { throw new Exception(s"UUID $uuid wasn't there (yet)") }
          case Val(Some(newUuid)) => Future { (newUuid, Nil) }
          case SynVal(newUuid) => Future { (newUuid, Seq(newUuid)) }
        }
    }

    // TODO: merge this into remapSeqUuid (the signature will be ugly)
    def remapOptUuid(renameActor: ActorRef, uuidOpt: Option[UUID], createIfNotFound: Boolean)
                    (implicit ec: ExecutionContext, t: Timeout): Future[(Option[UUID], Seq[UUID])] = {
      uuidOpt match {
        case None => Future { (None, Nil) }
        case Some(uuid) => remapUuid(renameActor, uuid, createIfNotFound) map {
          case (newUuid, syn) => (Some(newUuid),syn)
        }
      }
    }
    
    def remapSeqUuid(renameActor: ActorRef, uuidSeq: Seq[UUID], createIfNotFound: Boolean)
                    (implicit ec: ExecutionContext, t: Timeout): Future[(Seq[UUID], Seq[UUID])] = {
      Future
        .sequence(uuidSeq map { uuid => remapUuid(renameActor, uuid, createIfNotFound) })
        .map { case seq: Seq[(UUID, Seq[UUID])] => 
          val (uuids, synUuids): (Seq[UUID], Seq[Seq[UUID]]) = seq.unzip
          (uuids, synUuids.flatten)
        }
    }
  }

}
