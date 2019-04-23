package com.galois.adapt.adm

import com.galois.adapt.AdaptConfig.HostName
import com.galois.adapt.adm.UuidRemapper.CdmMerge
import com.galois.adapt.cdm20._

object ERRules {

  // Map one CDM node to one ADM node

  // Resolve a 'ProvenanceTagNode'
  object ProvenanceTagNodeEdges {
    type TagIdEdges = Seq[EdgeAdm2Cdm]
    type Subject = EdgeAdm2Cdm
    type FlowObject = Option[EdgeAdm2Cdm]
    type PrevTagID = Option[EdgeAdm2Cdm]
  }
  def resolveProvenanceTagNode(provider: String, p: ProvenanceTagNode, hostName: HostName):
    (
      AdmProvenanceTagNode,
      ProvenanceTagNodeEdges.FlowObject,
      ProvenanceTagNodeEdges.Subject,
      ProvenanceTagNodeEdges.PrevTagID,
      ProvenanceTagNodeEdges.TagIdEdges
    ) = {
      val newPtn = AdmProvenanceTagNode(Set(CdmUUID(p.getUuid, provider)), p.programPoint, hostName, provider)
      (
        newPtn,
        p.flowObject.map(flow => EdgeAdm2Cdm(newPtn.uuid, "flowObject", CdmUUID(flow, provider))),
        EdgeAdm2Cdm(newPtn.uuid, "provSubject", CdmUUID(p.subjectUuid, provider)),
        p.prevTagId.map(tagId => EdgeAdm2Cdm(newPtn.uuid, "prevTagId", CdmUUID(tagId, provider))),
        p.tagIds.getOrElse(Nil).map(tagId => EdgeAdm2Cdm(newPtn.uuid, "tagId", CdmUUID(tagId, provider)))
      )
    }

  // Resolve a 'Principal'
  def resolvePrincipal(provider: String, p: Principal, hostName: HostName): AdmPrincipal
    = AdmPrincipal(Set(CdmUUID(p.getUuid, provider)), p.userId, p.groupIds, p.principalType, p.username, hostName, provider)

  // Resolve a 'SrcSinkObject'
  def resolveSrcSink(provider: String, s: SrcSinkObject, hostName: HostName): AdmSrcSinkObject
    = AdmSrcSinkObject(Set(CdmUUID(s.getUuid, provider)), s.srcSinkType, hostName, provider)

  // Resolve a 'NetFlowObject'
  object NetflowObjectEdges {
    type AddressEdgeNode = Option[(EdgeAdm2Adm, AdmAddress)]
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
      val newN = AdmNetFlowObject(Set(CdmUUID(n.getUuid, provider)), n.localAddress, n.localPort, n.remoteAddress, n.remotePort, provider)

      val newLP = n.localPort.map(AdmPort.apply)
      val newLA = n.localAddress.map(AdmAddress.apply)
      val newRP = n.remotePort.map(AdmPort.apply)
      val newRA = n.remoteAddress.map(AdmAddress.apply)
      (
        newN,
        newLA.map(la => (EdgeAdm2Adm(newN.uuid, "localAddress", la.uuid), la)),
        newRA.map(ra => (EdgeAdm2Adm(newN.uuid, "remoteAddress", ra.uuid), ra)),
        newLP.filterNot(_.port == -1).map(lp => (EdgeAdm2Adm(newN.uuid, "localPort", lp.uuid), lp)),
        newRP.filterNot(_.port == -1).map(rp => (EdgeAdm2Adm(newN.uuid, "remotePort", rp.uuid), rp))
      )
    }

  // TODO: We may want to try to merge together fileobjects based on path/principal (this may make the Execute/Delete alarm more effective)
  //
  // Resolve a 'FileObject'
  object FileObjectEdges {
    type LocalPrincipalEdge = Option[EdgeAdm2Cdm]

    type FilePathEdgeNode = Option[(EdgeAdm2Adm, AdmPathNode)]
  }
  def resolveFileObject(provider: String, f: FileObject, isWindows: Boolean, hostName: HostName):
  (
    AdmFileObject,
    FileObjectEdges.LocalPrincipalEdge,
    FileObjectEdges.FilePathEdgeNode
  ) = {
    val newFo = AdmFileObject(Set(CdmUUID(f.getUuid, provider)), f.fileObjectType, f.size, hostName, provider)
    val pathOpt1: Option[AdmPathNode] = f.peInfo.flatMap(p => AdmPathNode.normalized(p, provider, isWindows))
    val pathOpt2: Option[AdmPathNode] = f.baseObject.properties.flatMap(_.get("filename")).flatMap(p => AdmPathNode.normalized(p, provider, isWindows))
    val pathOpt3: Option[AdmPathNode] = f.baseObject.properties.flatMap(_.get("path")).flatMap(p => AdmPathNode.normalized(p, provider, isWindows))
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
  def resolveRegistryKeyObject(provider: String, r: RegistryKeyObject, isWindows: Boolean, hostName: HostName):
    (
      AdmFileObject,
      RegistryKeyObjectEdges.FilePathEdgeNode
    ) = {
      val newFo = AdmFileObject(Set(CdmUUID(r.getUuid, provider)), FILE_OBJECT_FILE, None, hostName, provider)
      (
        newFo,
        AdmPathNode.normalized(r.key, provider, isWindows).map(pathNode =>
          (EdgeAdm2Adm(newFo.uuid, "path", pathNode.uuid), pathNode)
        )
      )
    }

  // Resolve an 'UnnamedPipeObject'
  //
  // TODO: sourceUUID, sinkUUID
  def resolveUnnamedPipeObject(provider: String, u: IpcObject, hostName: HostName): AdmFileObject
    = AdmFileObject(Set(CdmUUID(u.getUuid, provider)), FILE_OBJECT_NAMED_PIPE, None, hostName, provider)

  def resolveMemoryObject(provider: String, m: MemoryObject, hostName: HostName): AdmSrcSinkObject
    = AdmSrcSinkObject(Set(CdmUUID(m.uuid, provider)), MEMORY_SRCSINK, hostName, provider)

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
  def resolveEventAndPaths(provider: String, e: Event, isWindows: Boolean, hostName: HostName):
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
      val newEvent = AdmEvent(
        originalCdmUuids = Set(CdmUUID(e.getUuid, provider)),
        eventType = e.eventType,
        earliestTimestampNanos = e.timestampNanos,
        latestTimestampNanos = e.timestampNanos,
        deviceType = e.properties.flatMap(_.get("deviceType")),
        inputType = e.properties.flatMap(_.get("inputType")),
        hostName,
        provider
      )

      (
        newEvent,
        e.subjectUuid.map(subj => EdgeCdm2Cdm(CdmUUID(e.getUuid, provider), "subject", CdmUUID(subj, provider))),
        e.predicateObject.map(obj => EdgeCdm2Cdm(CdmUUID(e.getUuid, provider), "predicateObject", CdmUUID(obj, provider))),
        e.predicateObject2.map(obj => EdgeCdm2Cdm(CdmUUID(e.getUuid, provider), "predicateObject2", CdmUUID(obj, provider))),

        e.predicateObjectPath.flatMap(p => AdmPathNode.normalized(p, provider, isWindows)).flatMap(pathNode => {
          e.predicateObject.map(predicateObject => {
            (EdgeCdm2Adm(CdmUUID(predicateObject, provider), "path", pathNode.uuid), pathNode)
          })
        }),
        e.predicateObject2Path.flatMap(p => AdmPathNode.normalized(p, provider, isWindows)).flatMap(pathNode => {
          e.predicateObject2.map(predicateObject2 => {
            (EdgeCdm2Adm(CdmUUID(predicateObject2, provider), "path", pathNode.uuid), pathNode)
          })
        }),
        e.properties.getOrElse(Map()).get("exec").flatMap(p => AdmPathNode.normalized(p, provider, isWindows)).flatMap(pathNode => {
          e.subjectUuid.map(subj =>
            (EdgeCdm2Adm(CdmUUID(subj, provider), "path", pathNode.uuid), pathNode)
          )
        }),
//        e.properties.getOrElse(Map()).get("exec").flatMap(p => AdmPathNode.normalized(p, provider, isWindows)).map(pathNode => {
//          (EdgeCdm2Adm(CdmUUID(e.getUuid, provider), "eventPath", pathNode.uuid), pathNode)
//        })
        None  // Choosing not to create "eventPath" edges.
      )
    }

  // Resolve a 'Host'
  def resolveHost(provider: String, h: Host): AdmHost =
    AdmHost(
      Set(CdmUUID(h.uuid,provider)),
      h.hostName,
      h.hostIdentifiers,
      h.osDetails,
      h.hostType,
      h.interfaces,
      provider
    )

  // Resolve a 'Subject'
  object SubjectEdges {
    type LocalPrincipalEdge = Option[EdgeAdm2Cdm]
    type ParentSubject = Option[EdgeAdm2Cdm]

    type CmdLinePathEdgeNode = Option[(EdgeAdm2Adm, AdmPathNode)]
    type CmdLineIndirectPathEdgeNode = Option[(EdgeCdm2Adm, AdmPathNode)]
  }
  def resolveSubject(provider: String, s: Subject, isWindows: Boolean, hostName: HostName): Either[
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
        s.cmdLine.flatMap(p => AdmPathNode.normalized(p, provider, isWindows)).map(pathNode => {
          (EdgeCdm2Adm(CdmUUID(s.parentSubject.get, provider), "path", pathNode.uuid), pathNode)
        }),
        UuidRemapper.CdmMerge(CdmUUID(s.getUuid, provider), CdmUUID(s.parentSubject.get, provider))
      ))
    } else {
      val newSubj = AdmSubject(Set(CdmUUID(s.getUuid, provider)), Set(s.subjectType), s.cid, s.startTimestampNanos.getOrElse(0), hostName, provider)

      Left((
        newSubj,
        s.localPrincipal.map(principal => EdgeAdm2Cdm(newSubj.uuid, "localPrincipal", CdmUUID(principal, provider))),
        s.parentSubject.map(parent => EdgeAdm2Cdm(newSubj.uuid, "parentSubject", CdmUUID(parent, provider))),
        s.cmdLine.flatMap(p => AdmPathNode.normalized(p, provider, isWindows)).map(pathNode => {
          (EdgeAdm2Adm(newSubj.uuid, "path", pathNode.uuid), pathNode)
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
          originalCdmUuids = e2.originalCdmUuids + CdmUUID(e1.getUuid, provider)
        ))
      } else {
        Right((e1, e2))
      }
    }

}
