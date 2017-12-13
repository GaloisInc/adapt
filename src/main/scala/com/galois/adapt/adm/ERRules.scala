package com.galois.adapt.adm

import java.util.UUID

import com.galois.adapt.cdm17._

object ERRules {

  // Map one CDM node to one ADM node

  // Resolve a 'ProvenanceTagNode'
  object ProvenanceTagNodeEdges {
    type TagIdEdges = Seq[Edge[ADM, CDM17]]
    type Subject = Edge[ADM, CDM17]
    type FlowObject = Option[Edge[ADM, CDM17]]
    type PrevTagID = Option[Edge[ADM, CDM17]]
  }
  def resolveProvenanceTagNode(p: ProvenanceTagNode):
    (
      AdmProvenanceTagNode,
      UuidRemapper.PutCdm2Adm,
      ProvenanceTagNodeEdges.FlowObject,
      ProvenanceTagNodeEdges.Subject,
      ProvenanceTagNodeEdges.PrevTagID,
      ProvenanceTagNodeEdges.TagIdEdges
    ) = {
      val newPtn = AdmProvenanceTagNode(Seq(CdmUUID(p.getUuid)), p.programPoint)
      (
        newPtn,
        UuidRemapper.PutCdm2Adm(CdmUUID(p.getUuid), newPtn.uuid),
        p.flowObject.map(flow => EdgeAdm2Cdm(newPtn.uuid, "flowObject", CdmUUID(flow))),
        EdgeAdm2Cdm(newPtn.uuid, "subject", CdmUUID(p.subjectUuid)),
        p.prevTagId.map(tagId => EdgeAdm2Cdm(newPtn.uuid, "prevTagId", CdmUUID(tagId))),
        p.tagIds.getOrElse(Nil).map(tagId => EdgeAdm2Cdm(newPtn.uuid, "tagId", CdmUUID(tagId)))
      )
    }

  // Resolve a 'Principal'
  def resolvePrincipal(p: Principal):
    (
      AdmPrincipal,
      UuidRemapper.PutCdm2Adm
    ) = {
      val newP = AdmPrincipal(Seq(CdmUUID(p.getUuid)), p.userId, p.groupIds, p.principalType, p.username)
      (
        newP,
        UuidRemapper.PutCdm2Adm(CdmUUID(p.getUuid), newP.uuid)
      )
    }

  // Resolve a 'SrcSinkObject'
  def resolveSrcSink(s: SrcSinkObject):
    (
      AdmSrcSinkObject,
      UuidRemapper.PutCdm2Adm
    ) = {
      val newSrcSink = AdmSrcSinkObject(Seq(CdmUUID(s.getUuid)), s.srcSinkType)
      (
        newSrcSink,
        UuidRemapper.PutCdm2Adm(CdmUUID(s.getUuid), newSrcSink.uuid)
      )
    }

  // Resolve a 'NetFlowObject'
  def resolveNetflow(n: NetFlowObject):
    (
      AdmNetFlowObject,
      UuidRemapper.PutCdm2Adm
    ) = {
      val newN = AdmNetFlowObject(Seq(CdmUUID(n.getUuid)), n.localAddress, n.localPort, n.remoteAddress, n.remotePort)
      (
        newN,
        UuidRemapper.PutCdm2Adm(CdmUUID(n.getUuid), newN.uuid)
      )
    }

  // TODO: We may want to try to merge together fileobjects based on path/principal (this may make the Execute/Delete alarm more effective)
  //
  // Resolve a 'FileObject'
  object FileObjectEdges {
    type LocalPrincipalEdge = Option[Edge[ADM, CDM17]]

    type FilePathEdgeNode = Option[(Edge[ADM,ADM], AdmPathNode)]
  }
  def resolveFileObject(f: FileObject):
  (
    AdmFileObject,
    UuidRemapper.PutCdm2Adm,
    FileObjectEdges.LocalPrincipalEdge,
    FileObjectEdges.FilePathEdgeNode
  ) = {
    val newFo = AdmFileObject(Seq(CdmUUID(f.getUuid)), f.fileObjectType, f.size)
    (
      newFo,
      UuidRemapper.PutCdm2Adm(CdmUUID(f.getUuid), newFo.uuid),
      f.localPrincipal.map(prinicpal => EdgeAdm2Cdm(newFo.uuid, "principal", CdmUUID(prinicpal))),
      f.peInfo.map(path => {
        val pathNode = AdmPathNode(path)
        (EdgeAdm2Adm(newFo.uuid, "path", pathNode.uuid), pathNode)
      })
    )
  }

  // Resolve a 'RegistryKeyObject'
  object RegistryKeyObjectEdges {
    type FilePathEdgeNode = (Edge[ADM,ADM], AdmPathNode)
  }
  def resolveRegistryKeyObject(r: RegistryKeyObject):
    (
      AdmFileObject,
      UuidRemapper.PutCdm2Adm,
      RegistryKeyObjectEdges.FilePathEdgeNode
    ) = {
      val newFo = AdmFileObject(Seq(CdmUUID(r.getUuid)), FILE_OBJECT_FILE, None)
      (
        newFo,
        UuidRemapper.PutCdm2Adm(CdmUUID(r.getUuid), newFo.uuid),
        {
          val pathNode = AdmPathNode(r.key)
          (EdgeAdm2Adm(newFo.uuid, "path", pathNode.uuid), pathNode)
        }
      )
    }

  // Resolve an 'Event'
  object EventEdges {
    type Subject = Edge[ADM, CDM17]
    type PredicateObject = Option[Edge[ADM, CDM17]]
    type PredicateObject2 = Option[Edge[ADM, CDM17]]

    type PredicatePathEdgeNode = Option[(Edge[CDM17,ADM], AdmPathNode)]
    type Predicate2PathEdgeNode = Option[(Edge[CDM17,ADM], AdmPathNode)]
    type ExecSubjectPathEdgeNode = Option[(Edge[CDM17,ADM], AdmPathNode)]
    type ExecPathEdgeNode = Option[(Edge[ADM,ADM], AdmPathNode)]
  }
  def resolveEventAndPaths(e: Event):
    (
      AdmEvent,
      UuidRemapper.PutCdm2Adm,
      EventEdges.Subject,
      EventEdges.PredicateObject,
      EventEdges.PredicateObject2,

      EventEdges.PredicatePathEdgeNode,
      EventEdges.Predicate2PathEdgeNode,
      EventEdges.ExecSubjectPathEdgeNode,
      EventEdges.ExecPathEdgeNode
    ) = {
      val newEvent = AdmEvent(Seq(CdmUUID(e.getUuid)), e.eventType, e.timestampNanos, e.timestampNanos)
      (
        newEvent,
        UuidRemapper.PutCdm2Adm(CdmUUID(e.getUuid), newEvent.uuid),
        EdgeAdm2Cdm(newEvent.uuid, "subject", CdmUUID(e.subjectUuid)),
        e.predicateObject.map(obj => EdgeAdm2Cdm(newEvent.uuid, "predicateObject", CdmUUID(obj))),
        e.predicateObject2.map(obj => EdgeAdm2Cdm(newEvent.uuid, "predicateObject2", CdmUUID(obj))),

        e.predicateObjectPath.flatMap(path => {
          e.predicateObject.map(predicateObject => {
            val pathNode = AdmPathNode(path)
            val label = if (e.eventType == EVENT_EXECUTE || e.eventType == EVENT_FORK) { "(cmdLine)" } else { "(path)" }
            (EdgeCdm2Adm(CdmUUID(predicateObject), label, pathNode.uuid), pathNode)
          })
        }),
        e.predicateObject2Path.flatMap(path => {
          e.predicateObject2.map(predicateObject2 => {
            val pathNode = AdmPathNode(path)
            val label = if (e.eventType == EVENT_FORK) { "(cmdLine)" } else { "(path)" }
            (EdgeCdm2Adm(CdmUUID(predicateObject2), label, pathNode.uuid), pathNode)
          })
        }),
        e.properties.getOrElse(Map()).get("exec").map(cmdLine => {
          val pathNode = AdmPathNode(cmdLine)
          (EdgeCdm2Adm(CdmUUID(e.subjectUuid), "exec", pathNode.uuid), pathNode)
        }),
        e.properties.getOrElse(Map()).get("exec").map(cmdLine => {
          val pathNode = AdmPathNode(cmdLine)
          (EdgeAdm2Adm(newEvent.uuid, "exec", pathNode.uuid), pathNode)
        })
      )
    }

  // Resolve a 'Subject'
  object SubjectEdges {
    type LocalPrincipalEdge = Edge[ADM, CDM17]
    type ParentSubject = Option[Edge[ADM, CDM17]]

    type CmdLinePathEdgeNode = Option[(Edge[ADM,ADM], AdmPathNode)]
    type CmdLineIndirectPathEdgeNode = Option[(Edge[CDM17, ADM], AdmPathNode)]
  }
  def resolveSubject(s: Subject): Either[
    (
      AdmSubject,
      UuidRemapper.PutCdm2Adm,
      SubjectEdges.LocalPrincipalEdge,
      SubjectEdges.ParentSubject,

      SubjectEdges.CmdLinePathEdgeNode
    ),
    (
      SubjectEdges.CmdLineIndirectPathEdgeNode,  // The edge SRC will be the parent subject
      UuidRemapper.PutCdm2Cdm                    // We remap to the parent subject
    )] =
    if (s.subjectType != SUBJECT_PROCESS && s.parentSubject.isDefined) {
      Right((
        s.cmdLine.map(cmd => {
          val pathNode = AdmPathNode(cmd)
          (EdgeCdm2Adm(CdmUUID(s.parentSubject.get), "cmdLine", pathNode.uuid), pathNode)
        }),
        UuidRemapper.PutCdm2Cdm(CdmUUID(s.getUuid), CdmUUID(s.parentSubject.get))
      ))
    } else {
      val newSubj = AdmSubject(Seq(CdmUUID(s.getUuid)), Set(s.subjectType), s.cid, s.startTimestampNanos)

      Left((
        newSubj,
        UuidRemapper.PutCdm2Adm(CdmUUID(s.getUuid), newSubj.uuid),
        EdgeAdm2Cdm(newSubj.uuid, "localPrincipal", CdmUUID(s.localPrincipal)),
        s.parentSubject.map(parent => EdgeAdm2Cdm(newSubj.uuid, "parentSubject", CdmUUID(parent))),
        s.cmdLine.map(cmd => {
          val pathNode = AdmPathNode(cmd)
          (EdgeAdm2Adm(newSubj.uuid, "cmdLine", pathNode.uuid), pathNode)
        })
      ))
    }

    // Collapse event
    //
    // TODO: better logic than just merge same successive events
    def collapseEvents(e1: Event, e2: AdmEvent): Either[(UuidRemapper.PutCdm2Adm, AdmEvent), (Event, AdmEvent)] = {
      if (e1.eventType == e2.eventType) {
        val e2Updated = e2.copy(
          earliestTimestampNanos = Math.min(e1.timestampNanos, e2.earliestTimestampNanos),
          latestTimestampNanos = Math.min(e1.timestampNanos, e2.latestTimestampNanos),
          originalCdmUuids = CdmUUID(e1.getUuid) +: e2.originalCdmUuids
        )
        Left((UuidRemapper.PutCdm2Adm(CdmUUID(e1.getUuid), e2.uuid), e2Updated))
      } else {
        Right((e1, e2))
      }
    }

}
