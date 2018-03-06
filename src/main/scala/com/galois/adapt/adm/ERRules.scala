package com.galois.adapt.adm

import com.galois.adapt.Application
import com.galois.adapt.cdm18._

object ERRules {

  // Map one CDM node to one ADM node

  // Resolve a 'ProvenanceTagNode'
  object ProvenanceTagNodeEdges {
    type TagIdEdges = Seq[Edge[ADM, CDM18]]
    type Subject = Edge[ADM, CDM18]
    type FlowObject = Option[Edge[ADM, CDM18]]
    type PrevTagID = Option[Edge[ADM, CDM18]]
  }
  def resolveProvenanceTagNode(provider: String, p: ProvenanceTagNode):
    (
      AdmProvenanceTagNode,
      UuidRemapper.PutCdm2Adm,
      ProvenanceTagNodeEdges.FlowObject,
      ProvenanceTagNodeEdges.Subject,
      ProvenanceTagNodeEdges.PrevTagID,
      ProvenanceTagNodeEdges.TagIdEdges
    ) = {
      val newPtn = AdmProvenanceTagNode(Seq(CdmUUID(p.getUuid, provider)), p.programPoint, provider)
      (
        newPtn,
        UuidRemapper.PutCdm2Adm(CdmUUID(p.getUuid, provider), newPtn.uuid),
        p.flowObject.map(flow => EdgeAdm2Cdm(newPtn.uuid, "flowObject", CdmUUID(flow, provider))),
        EdgeAdm2Cdm(newPtn.uuid, "provSubject", CdmUUID(p.subjectUuid, provider)),
        p.prevTagId.map(tagId => EdgeAdm2Cdm(newPtn.uuid, "prevTagId", CdmUUID(tagId, provider))),
        p.tagIds.getOrElse(Nil).map(tagId => EdgeAdm2Cdm(newPtn.uuid, "tagId", CdmUUID(tagId, provider)))
      )
    }

  // Resolve a 'Principal'
  def resolvePrincipal(provider: String, p: Principal):
    (
      AdmPrincipal,
      UuidRemapper.PutCdm2Adm
    ) = {
      val newP = AdmPrincipal(Seq(CdmUUID(p.getUuid, provider)), p.userId, p.groupIds, p.principalType, p.username, provider)
      (
        newP,
        UuidRemapper.PutCdm2Adm(CdmUUID(p.getUuid, provider), newP.uuid)
      )
    }

  // Resolve a 'SrcSinkObject'
  def resolveSrcSink(provider: String, s: SrcSinkObject):
    (
      AdmSrcSinkObject,
      UuidRemapper.PutCdm2Adm
    ) = {
      val newSrcSink = AdmSrcSinkObject(Seq(CdmUUID(s.getUuid, provider)), s.srcSinkType, provider)
      (
        newSrcSink,
        UuidRemapper.PutCdm2Adm(CdmUUID(s.getUuid, provider), newSrcSink.uuid)
      )
    }

  // Resolve a 'NetFlowObject'
  object NetflowObjectEdges {
    type AddressEdgeNode = (Edge[ADM,ADM], AdmAddress)
    type PortEdgeNode = Option[(Edge[ADM,ADM], AdmPort)]
  }
  def resolveNetflow(provider: String, n: NetFlowObject):
    (
      AdmNetFlowObject,
      UuidRemapper.PutCdm2Adm,
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
        UuidRemapper.PutCdm2Adm(CdmUUID(n.getUuid, provider), newN.uuid),
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
    type LocalPrincipalEdge = Option[Edge[ADM, CDM18]]

    type FilePathEdgeNode = Option[(Edge[ADM,ADM], AdmPathNode)]
  }
  def resolveFileObject(provider: String, f: FileObject):
  (
    AdmFileObject,
    UuidRemapper.PutCdm2Adm,
    FileObjectEdges.LocalPrincipalEdge,
    FileObjectEdges.FilePathEdgeNode
  ) = {
    val newFo = AdmFileObject(Seq(CdmUUID(f.getUuid, provider)), f.fileObjectType, f.size, provider)
    (
      newFo,
      UuidRemapper.PutCdm2Adm(CdmUUID(f.getUuid, provider), newFo.uuid),
      f.localPrincipal.map(prinicpal => EdgeAdm2Cdm(newFo.uuid, "principal", CdmUUID(prinicpal, provider))),
      f.peInfo.flatMap(p => AdmPathNode.normalized(p, provider)).map(pathNode => {
        (EdgeAdm2Adm(newFo.uuid, "path", pathNode.uuid), pathNode)
      })
    )
  }

  // Resolve a 'RegistryKeyObject'
  object RegistryKeyObjectEdges {
    type FilePathEdgeNode = Option[(Edge[ADM,ADM], AdmPathNode)]
  }
  def resolveRegistryKeyObject(provider: String, r: RegistryKeyObject):
    (
      AdmFileObject,
      UuidRemapper.PutCdm2Adm,
      RegistryKeyObjectEdges.FilePathEdgeNode
    ) = {
      val newFo = AdmFileObject(Seq(CdmUUID(r.getUuid, provider)), FILE_OBJECT_FILE, None, provider)
      (
        newFo,
        UuidRemapper.PutCdm2Adm(CdmUUID(r.getUuid, provider), newFo.uuid),
        AdmPathNode.normalized(r.key, provider).map(pathNode =>
          (EdgeAdm2Adm(newFo.uuid, "path", pathNode.uuid), pathNode)
        )
      )
    }

  // Resolve an 'UnnamedPipeObject'
  //
  // TODO: sourceUUID, sinkUUID
  def resolveUnnamedPipeObject(provider: String, u: UnnamedPipeObject):
    (
      AdmFileObject,
      UuidRemapper.PutCdm2Adm
    ) = {
      val newFo = AdmFileObject(Seq(CdmUUID(u.getUuid, provider)), FILE_OBJECT_NAMED_PIPE, None, provider)
      (
        newFo,
        UuidRemapper.PutCdm2Adm(CdmUUID(u.getUuid, provider), newFo.uuid)
      )
    }

  def resolveMemoryObject(provider: String, m: MemoryObject):
    (
      AdmSrcSinkObject,
      UuidRemapper.PutCdm2Adm
    ) = {
      val newSrcSink = AdmSrcSinkObject(Seq(CdmUUID(m.uuid, provider)), MEMORY_SRCSINK, provider)
      (
        newSrcSink,
        UuidRemapper.PutCdm2Adm(CdmUUID(m.uuid, provider), newSrcSink.uuid)
      )
    }

  // Resolve an 'Event'
  object EventEdges {
    type Subject = Option[Edge[CDM18, CDM18]]
    type PredicateObject = Option[Edge[CDM18, CDM18]]
    type PredicateObject2 = Option[Edge[CDM18, CDM18]]

    type PredicatePathEdgeNode = Option[(Edge[CDM18,ADM], AdmPathNode)]
    type Predicate2PathEdgeNode = Option[(Edge[CDM18,ADM], AdmPathNode)]
    type ExecSubjectPathEdgeNode = Option[(Edge[CDM18,ADM], AdmPathNode)]
    type ExecPathEdgeNode = Option[(Edge[CDM18,ADM], AdmPathNode)]
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

  // Resolve a 'Subject'
  object SubjectEdges {
    type LocalPrincipalEdge = Edge[ADM, CDM18]
    type ParentSubject = Option[Edge[ADM, CDM18]]

    type CmdLinePathEdgeNode = Option[(Edge[ADM,ADM], AdmPathNode)]
    type CmdLineIndirectPathEdgeNode = Option[(Edge[CDM18, ADM], AdmPathNode)]
  }
  def resolveSubject(provider: String, s: Subject): Either[
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
        s.cmdLine.flatMap(p => AdmPathNode.normalized(p, provider)).map(pathNode => {
          (EdgeCdm2Adm(CdmUUID(s.parentSubject.get, provider), "cmdLine", pathNode.uuid), pathNode)
        }),
        UuidRemapper.PutCdm2Cdm(CdmUUID(s.getUuid, provider), CdmUUID(s.parentSubject.get, provider))
      ))
    } else {
      val newSubj = AdmSubject(Seq(CdmUUID(s.getUuid, provider)), Set(s.subjectType), s.cid, s.startTimestampNanos, provider)

      Left((
        newSubj,
        UuidRemapper.PutCdm2Adm(CdmUUID(s.getUuid, provider), newSubj.uuid),
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
    val maxEventsMerged: Int = Application.config.getInt("adapt.adm.maxeventsmerged")
    def collapseEvents(provider: String, e1: Event, e2: AdmEvent, lastCdmUuid: CdmUUID, merged: Int): Either[(UuidRemapper.PutCdm2Cdm, CdmUUID, AdmEvent), (Event, AdmEvent)] = {
      if (e1.eventType == e2.eventType && merged < maxEventsMerged) {
        val e2Updated = e2.copy(
          earliestTimestampNanos = Math.min(e1.timestampNanos, e2.earliestTimestampNanos),
          latestTimestampNanos = Math.min(e1.timestampNanos, e2.latestTimestampNanos),
          originalCdmUuids = CdmUUID(e1.getUuid, provider) +: e2.originalCdmUuids
        )
        val newCdmUuid = CdmUUID(e1.getUuid, provider)
        Left((UuidRemapper.PutCdm2Cdm(lastCdmUuid, newCdmUuid), newCdmUuid, e2Updated))
      } else {
        Right((e1, e2))
      }
    }

}
