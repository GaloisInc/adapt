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
    def remapUuids(renameActor: ActorRef)(implicit ec: ExecutionContext, t: Timeout): Future[SelfType]
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
    timestampNanos: Long,
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
        "timestampNanos", timestampNanos,
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

    def remapUuids(ra: ActorRef)(implicit ec: ExecutionContext, t: Timeout): Future[IrEvent] = for {
      s <- remapUuid(ra, subject)
      po1 <- remapOptUuid(ra, predicateObject)
      po2 <- remapOptUuid(ra, predicateObject2)
    } yield this.copy(subject = s, predicateObject = po1, predicateObject2 = po2)

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

    def remapUuids(ra: ActorRef)(implicit ec: ExecutionContext, t: Timeout): Future[IrSubject] = for {
      p <- remapUuid(ra, localPrincipal)
      ps <- remapOptUuid(ra, parentSubject)
    } yield this.copy(localPrincipal = p, parentSubject = ps)

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

    def remapUuids(ra: ActorRef)(implicit ec: ExecutionContext, t: Timeout): Future[IrFileObject] = for {
      p <- remapOptUuid(ra, localPrincipal)
    } yield this.copy(localPrincipal = p)
  
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

    def remapUuids(ra: ActorRef)(implicit ec: ExecutionContext, t: Timeout): Future[IrNetFlowObject] =
      Future(this)
  
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

    def remapUuids(ra: ActorRef)(implicit ec: ExecutionContext, t: Timeout): Future[IrSrcSinkObject] =
      Future(this)

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

    def remapUuids(ra: ActorRef)(implicit ec: ExecutionContext, t: Timeout): Future[IrPrincipal] =
      Future(this)
    
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

    def remapUuids(ra: ActorRef)(implicit ec: ExecutionContext, t: Timeout): Future[IrProvenanceTagNode] = for {
      s <- remapUuid(ra, subjectUuid)
      fo <- remapOptUuid(ra, flowObject)
      pt <- remapOptUuid(ra, prevTagId)
      ts <- remapSeqUuid(ra, tagIds)
    } yield this.copy(
      subjectUuid = s,
      flowObject = fo,
      prevTagId = pt,
      tagIds = ts
    ) 

  }

  // Utilities for remapping UUIDs. Used in the IR's implementation of 'remapUuids'
  private object RemapUtils {
    def remapUuid(renameActor: ActorRef, uuid: UUID)(implicit ec: ExecutionContext, t: Timeout): Future[UUID] = {
      (renameActor ? Get(uuid))
        .mapTo[Val[UUID,UUID]]
        .flatMap {
          case Val(None) => Future { throw new Exception(s"UUID $uuid wasn't there (yet)") }
          case Val(Some(newUuid)) => Future(newUuid)
        }
    }

    // TODO: merge this into remapSeqUuid (the signature will be ugly)
    def remapOptUuid(renameActor: ActorRef, uuidOpt: Option[UUID])(implicit ec: ExecutionContext, t: Timeout): Future[Option[UUID]] = {
      uuidOpt match {
        case None => Future(None)
        case Some(uuid) => remapUuid(renameActor, uuid) map { Some(_) }
      }
    }
    
    def remapSeqUuid(renameActor: ActorRef, uuidSeq: Seq[UUID])(implicit ec: ExecutionContext, t: Timeout): Future[Seq[UUID]] = {
      Future.sequence(uuidSeq map { uuid => remapUuid(renameActor, uuid) })
    }
  }

}
