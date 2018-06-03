package com.galois.adapt.adm

import com.galois.adapt.adm.UuidRemapper.CdmMerge
import com.galois.adapt.cdm18._

object ERRules {

  // Map one CDM node to one ADM node

  // Resolve a 'ProvenanceTagNode'
  object ProvenanceTagNodeEdges {
    type TagIdEdges = Seq[EdgeAdm2Cdm]
    type Subject = EdgeAdm2Cdm
    type FlowObject = Option[EdgeAdm2Cdm]
    type PrevTagID = Option[EdgeAdm2Cdm]
  }
  def resolveProvenanceTagNode(provider: String, p: ProvenanceTagNode):
    (
      AdmProvenanceTagNode,
      ProvenanceTagNodeEdges.FlowObject,
      ProvenanceTagNodeEdges.Subject,
      ProvenanceTagNodeEdges.PrevTagID,
      ProvenanceTagNodeEdges.TagIdEdges
    ) = {
      val newPtn = AdmProvenanceTagNode(Seq(CdmUUID(p.getUuid, provider)), p.programPoint, provider)
      (
        newPtn,
        p.flowObject.map(flow => EdgeAdm2Cdm(newPtn.uuid, "flowObject", CdmUUID(flow, provider))),
        EdgeAdm2Cdm(newPtn.uuid, "provSubject", CdmUUID(p.subjectUuid, provider)),
        p.prevTagId.map(tagId => EdgeAdm2Cdm(newPtn.uuid, "prevTagId", CdmUUID(tagId, provider))),
        p.tagIds.getOrElse(Nil).map(tagId => EdgeAdm2Cdm(newPtn.uuid, "tagId", CdmUUID(tagId, provider)))
      )
    }

  // Resolve a 'Principal'
  def resolvePrincipal(provider: String, p: Principal): AdmPrincipal
    = AdmPrincipal(Seq(CdmUUID(p.getUuid, provider)), p.userId, p.groupIds, p.principalType, p.username, provider)

  // Resolve a 'SrcSinkObject'
  def resolveSrcSink(provider: String, s: SrcSinkObject): AdmSrcSinkObject
    = AdmSrcSinkObject(Seq(CdmUUID(s.getUuid, provider)), s.srcSinkType, provider)

  // Resolve a 'NetFlowObject'
  object NetflowObjectEdges {
    type AddressEdgeNode = (EdgeAdm2Adm, AdmAddress)
    type PortEdgeNode = Option[(EdgeAdm2Adm, AdmPort)]
  }
  def resolveNetflow(provider: String, n: NetFlowObject):
    (
      AdmNetFlowObject,
      NetflowObjectEdges.AddressEdgeNode,
      NetflowObjectEdges.AddressEdgeNode,
      NetflowObjectEdges.PortEdgeNode,
      NetflowObjectEdges.PortEdgeNode
    ) = {
      val newN = AdmNetFlowObject(Seq(CdmUUID(n.getUuid, provider)), n.localAddress, n.localPort, n.remoteAddress, n.remotePort, provider)
      val newLP = AdmPort(n.localPort)
      val newLA = AdmAddress(n.localAddress)
      val newRP = AdmPort(n.remotePort)
      val newRA = AdmAddress(n.remoteAddress)
      (
        newN,
        (EdgeAdm2Adm(newN.uuid, "localAddress", newLA.uuid), newLA),
        (EdgeAdm2Adm(newN.uuid, "remoteAddress", newRA.uuid), newRA),
        if (n.localPort == -1) { None } else { Some((EdgeAdm2Adm(newN.uuid, "localPort", newLP.uuid), newLP)) },
        if (n.remotePort == -1) { None } else { Some((EdgeAdm2Adm(newN.uuid, "remotePort", newRP.uuid), newRP)) }
      )
    }

  // TODO: We may want to try to merge together fileobjects based on path/principal (this may make the Execute/Delete alarm more effective)
  //
  // Resolve a 'FileObject'
  object FileObjectEdges {
    type LocalPrincipalEdge = Option[EdgeAdm2Cdm]

    type FilePathEdgeNode = Option[(EdgeAdm2Adm, AdmPathNode)]
  }
  def resolveFileObject(provider: String, f: FileObject):
  (
    AdmFileObject,
    FileObjectEdges.LocalPrincipalEdge,
    FileObjectEdges.FilePathEdgeNode
  ) = {
    val newFo = AdmFileObject(Seq(CdmUUID(f.getUuid, provider)), f.fileObjectType, f.size, provider)
    val pathOpt1: Option[AdmPathNode] = f.peInfo.flatMap(p => AdmPathNode.normalized(p, provider))
    val pathOpt2: Option[AdmPathNode] = f.baseObject.properties.flatMap(_.get("filename")).flatMap(p => AdmPathNode.normalized(p, provider))
    val pathOpt3: Option[AdmPathNode] = f.baseObject.properties.flatMap(_.get("path")).flatMap(p => AdmPathNode.normalized(p, provider))
    (
      newFo,
      f.localPrincipal.map(prinicpal => EdgeAdm2Cdm(newFo.uuid, "principal", CdmUUID(prinicpal, provider))),
      (pathOpt1 orElse pathOpt2 orElse pathOpt3).map(pathNode => {
        (EdgeAdm2Adm(newFo.uuid, "path", pathNode.uuid), pathNode)
      })
    )
  }

  // Resolve a 'RegistryKeyObject'
  object RegistryKeyObjectEdges {
    type FilePathEdgeNode = Option[(EdgeAdm2Adm, AdmPathNode)]
  }
  def resolveRegistryKeyObject(provider: String, r: RegistryKeyObject):
    (
      AdmFileObject,
      RegistryKeyObjectEdges.FilePathEdgeNode
    ) = {
      val newFo = AdmFileObject(Seq(CdmUUID(r.getUuid, provider)), FILE_OBJECT_FILE, None, provider)
      (
        newFo,
        AdmPathNode.normalized(r.key, provider).map(pathNode =>
          (EdgeAdm2Adm(newFo.uuid, "path", pathNode.uuid), pathNode)
        )
      )
    }

  // Resolve an 'UnnamedPipeObject'
  //
  // TODO: sourceUUID, sinkUUID
  def resolveUnnamedPipeObject(provider: String, u: UnnamedPipeObject): AdmFileObject
    = AdmFileObject(Seq(CdmUUID(u.getUuid, provider)), FILE_OBJECT_NAMED_PIPE, None, provider)

  def resolveMemoryObject(provider: String, m: MemoryObject): AdmSrcSinkObject
    = AdmSrcSinkObject(Seq(CdmUUID(m.uuid, provider)), MEMORY_SRCSINK, provider)

  // Resolve an 'Event'
  object EventEdges {
    type Subject = Option[EdgeCdm2Cdm]
    type PredicateObject = Option[EdgeCdm2Cdm]
    type PredicateObject2 = Option[EdgeCdm2Cdm]

    type PredicatePathEdgeNode = Option[(EdgeCdm2Adm, AdmPathNode)]
    type Predicate2PathEdgeNode = Option[(EdgeCdm2Adm, AdmPathNode)]
    type ExecSubjectPathEdgeNode = Option[(EdgeCdm2Adm, AdmPathNode)]
    type ExecPathEdgeNode = Option[(EdgeCdm2Adm, AdmPathNode)]
  }
  def resolveEventAndPaths(provider: String, e: Event):
    (
      AdmEvent,
      EventEdges.Subject,
      EventEdges.PredicateObject,
      EventEdges.PredicateObject2,

      EventEdges.PredicatePathEdgeNode,
      EventEdges.Predicate2PathEdgeNode,
      EventEdges.ExecSubjectPathEdgeNode,
      EventEdges.ExecPathEdgeNode
    ) = {
      val newEvent = AdmEvent(Seq(CdmUUID(e.getUuid, provider)), e.eventType, e.timestampNanos, e.timestampNanos, provider)
      (
        newEvent,
        e.subjectUuid.map(subj => EdgeCdm2Cdm(CdmUUID(e.getUuid, provider), "subject", CdmUUID(subj, provider))),
        e.predicateObject.map(obj => EdgeCdm2Cdm(CdmUUID(e.getUuid, provider), "predicateObject", CdmUUID(obj, provider))),
        e.predicateObject2.map(obj => EdgeCdm2Cdm(CdmUUID(e.getUuid, provider), "predicateObject2", CdmUUID(obj, provider))),

        e.predicateObjectPath.flatMap(p => AdmPathNode.normalized(p, provider)).flatMap(pathNode => {
          e.predicateObject.map(predicateObject => {
            val label = if (e.eventType == EVENT_EXECUTE || e.eventType == EVENT_FORK) { "(cmdLine)" } else { "(path)" }
            (EdgeCdm2Adm(CdmUUID(predicateObject, provider), label, pathNode.uuid), pathNode)
          })
        }),
        e.predicateObject2Path.flatMap(p => AdmPathNode.normalized(p, provider)).flatMap(pathNode => {
          e.predicateObject2.map(predicateObject2 => {
            val label = if (e.eventType == EVENT_FORK) { "(cmdLine)" } else { "(path)" }
            (EdgeCdm2Adm(CdmUUID(predicateObject2, provider), label, pathNode.uuid), pathNode)
          })
        }),
        e.properties.getOrElse(Map()).get("exec").flatMap(p => AdmPathNode.normalized(p, provider)).flatMap(pathNode => {
          e.subjectUuid.map(subj =>
            (EdgeCdm2Adm(CdmUUID(subj, provider), "exec", pathNode.uuid), pathNode)
          )
        }),
        e.properties.getOrElse(Map()).get("exec").flatMap(p => AdmPathNode.normalized(p, provider)).map(pathNode => {
          (EdgeCdm2Adm(CdmUUID(e.getUuid, provider), "eventExec", pathNode.uuid), pathNode)
        })
      )
    }

  // Resolve a 'Host'
  def resolveHost(provider: String, h: Host): AdmHost =
    AdmHost(
      Seq(CdmUUID(h.uuid,provider)),
      h.hostName,
      h.hostIdentifiers,
      h.osDetails,
      h.hostType,
      h.interfaces,
      provider
    )

  // Resolve a 'Subject'
  object SubjectEdges {
    type LocalPrincipalEdge = EdgeAdm2Cdm
    type ParentSubject = Option[EdgeAdm2Cdm]

    type CmdLinePathEdgeNode = Option[(EdgeAdm2Adm, AdmPathNode)]
    type CmdLineIndirectPathEdgeNode = Option[(EdgeCdm2Adm, AdmPathNode)]
  }
  def resolveSubject(provider: String, s: Subject): Either[
    (
      AdmSubject,
      SubjectEdges.LocalPrincipalEdge,
      SubjectEdges.ParentSubject,

      SubjectEdges.CmdLinePathEdgeNode
    ),
    (
      SubjectEdges.CmdLineIndirectPathEdgeNode,  // The edge SRC will be the parent subject
      CdmMerge                                   // We remap to the parent subject
    )] =
    if (s.subjectType != SUBJECT_PROCESS && s.parentSubject.isDefined) {
      Right((
        s.cmdLine.flatMap(p => AdmPathNode.normalized(p, provider)).map(pathNode => {
          (EdgeCdm2Adm(CdmUUID(s.parentSubject.get, provider), "cmdLine", pathNode.uuid), pathNode)
        }),
        UuidRemapper.CdmMerge(CdmUUID(s.getUuid, provider), CdmUUID(s.parentSubject.get, provider))
      ))
    } else {
      val newSubj = AdmSubject(Seq(CdmUUID(s.getUuid, provider)), Set(s.subjectType), s.cid, s.startTimestampNanos, provider)

      Left((
        newSubj,
        EdgeAdm2Cdm(newSubj.uuid, "localPrincipal", CdmUUID(s.localPrincipal, provider)),
        s.parentSubject.map(parent => EdgeAdm2Cdm(newSubj.uuid, "parentSubject", CdmUUID(parent, provider))),
        s.cmdLine.flatMap(p => AdmPathNode.normalized(p, provider)).map(pathNode => {
          (EdgeAdm2Adm(newSubj.uuid, "cmdLine", pathNode.uuid), pathNode)
        })
      ))
    }

    // Collapse event
    //
    // TODO: better logic than just merge same successive events
    def collapseEvents(provider: String, e1: Event, e2: AdmEvent, merged: Int, maxEventsMerged: Int): Either[AdmEvent, (Event, AdmEvent)] = {
      if (e1.eventType == e2.eventType && merged < maxEventsMerged) {
        Left(e2.copy(
          earliestTimestampNanos = Math.min(e1.timestampNanos, e2.earliestTimestampNanos),
          latestTimestampNanos = Math.min(e1.timestampNanos, e2.latestTimestampNanos),
          originalCdmUuids = CdmUUID(e1.getUuid, provider) +: e2.originalCdmUuids
        ))
      } else {
        Right((e1, e2))
      }
    }

}
