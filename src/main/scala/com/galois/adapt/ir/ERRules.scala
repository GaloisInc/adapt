package com.galois.adapt.ir

import java.util.UUID

import com.galois.adapt.cdm17._

object ERRules {

  // Map one CDM node to one IR node

  // Resolve a 'ProvenanceTagNode'
  object ProvenanceTagNodeEdges {
    type TagIdEdges = Seq[Edge[IR, CDM17]]
    type Subject = Edge[IR, CDM17]
    type FlowObject = Option[Edge[IR, CDM17]]
    type PrevTagID = Option[Edge[IR, CDM17]]
  }
  def resolveProvenanceTagNode(p: ProvenanceTagNode):
    (
      IrProvenanceTagNode,
      UuidRemapper.PutCdm2Ir,
      ProvenanceTagNodeEdges.FlowObject,
      ProvenanceTagNodeEdges.Subject,
      ProvenanceTagNodeEdges.PrevTagID,
      ProvenanceTagNodeEdges.TagIdEdges
    ) = {
        val newUuid = UUID.randomUUID()
        (
          IrProvenanceTagNode(newUuid, Seq(p.getUuid), p.programPoint),
          UuidRemapper.PutCdm2Ir(p.getUuid, newUuid),
          p.flowObject.map(flow => Edge[IR, CDM17](newUuid, "flowObject", flow)),
          Edge[IR, CDM17](newUuid, "subject", p.subjectUuid),
          p.prevTagId.map(tagId => Edge[IR, CDM17](newUuid, "prevTagId", tagId)),
          p.tagIds.getOrElse(Nil).map(tagId => Edge[IR, CDM17](newUuid, "tagId", tagId))
      )
    }

  // Resolve a 'Principal'
  def resolvePrincipal(p: Principal):
    (
      IrPrincipal,
      UuidRemapper.PutCdm2Ir
    ) = {
      val newP = IrPrincipal(Seq(p.getUuid), p.userId, p.groupIds, p.principalType, p.username)
      (
        newP,
        UuidRemapper.PutCdm2Ir(p.getUuid, newP.uuid)
      )
    }

  // Resolve a 'SrcSinkObject'
  def resolveSrcSink(s: SrcSinkObject):
    (
      IrSrcSinkObject,
      UuidRemapper.PutCdm2Ir
    ) = {
      val newUuid = UUID.randomUUID()
      (
        IrSrcSinkObject(newUuid, Seq(s.getUuid), s.srcSinkType),
        UuidRemapper.PutCdm2Ir(s.getUuid, newUuid)
      )
    }

  // Resolve a 'NetFlowObject'
  def resolveNetflow(n: NetFlowObject):
    (
      IrNetFlowObject,
      UuidRemapper.PutCdm2Ir
    ) = {
      val newN = IrNetFlowObject(Seq(n.getUuid), n.localAddress, n.localPort, n.remoteAddress, n.remotePort)
      (
        newN,
        UuidRemapper.PutCdm2Ir(n.getUuid, newN.getUuid)
      )
    }

  // TODO: We may want to try to merge together fileobjects based on path/principal (this may make the Execute/Delete alarm more effective)
  //
  // Resolve a 'FileObject'
  object FileObjectEdges {
    type LocalPrincipalEdge = Option[Edge[IR, CDM17]]

    type FilePathEdgeNode = Option[(Edge[IR,IR], IrPathNode)]
  }
  def resolveFileObject(f: FileObject):
  (
    IrFileObject,
    UuidRemapper.PutCdm2Ir,
    FileObjectEdges.LocalPrincipalEdge,
    FileObjectEdges.FilePathEdgeNode
  ) = {
    val newUuid = UUID.randomUUID()
    (
      IrFileObject(newUuid, Seq(f.getUuid), f.fileObjectType),
      UuidRemapper.PutCdm2Ir(f.getUuid, newUuid),
      f.localPrincipal.map(prinicpal => Edge[IR, CDM17](newUuid, "principal", prinicpal)),
      f.peInfo.map(path => {
        val pathNode = IrPathNode(path)
        (Edge[IR,IR](newUuid, "path", pathNode.getUuid), pathNode)
      })
    )
  }

  // Resolve a 'RegistryKeyObject'
  object RegistryKeyObjectEdges {
    type FilePathEdgeNode = (Edge[IR,IR], IrPathNode)
  }
  def resolveRegistryKeyObject(r: RegistryKeyObject):
    (
      IrFileObject,
      UuidRemapper.PutCdm2Ir,
      RegistryKeyObjectEdges.FilePathEdgeNode
    ) = {
      val newUuid = UUID.randomUUID()
      (
        IrFileObject(newUuid, Seq(r.getUuid), FILE_OBJECT_FILE),
        UuidRemapper.PutCdm2Ir(r.getUuid, newUuid),
        {
          val pathNode = IrPathNode(r.key)
          (Edge[IR,IR](newUuid, "path", pathNode.getUuid), pathNode)
        }
      )
    }

  // Resolve an 'Event'
  object EventEdges {
    type Subject = Edge[IR, CDM17]
    type PredicateObject = Option[Edge[IR, CDM17]]
    type PredicateObject2 = Option[Edge[IR, CDM17]]

    type PredicatePathEdgeNode = Option[(Edge[IR,IR], IrPathNode)]
    type Predicate2PathEdgeNode = Option[(Edge[IR,IR], IrPathNode)]
    type ExecCommandPathEdgeNode = Option[(Edge[IR,IR], IrPathNode)]
  }
  def resolveEventAndPaths(e: Event):
    (
      IrEvent,
      UuidRemapper.PutCdm2Ir,
      EventEdges.Subject,
      EventEdges.PredicateObject,
      EventEdges.PredicateObject2,

      EventEdges.PredicatePathEdgeNode,
      EventEdges.Predicate2PathEdgeNode,
      EventEdges.ExecCommandPathEdgeNode
    ) = {
      val newUuid = UUID.randomUUID()

      (
        IrEvent(newUuid, Seq(e.getUuid), e.eventType, e.timestampNanos, e.timestampNanos),
        UuidRemapper.PutCdm2Ir(e.getUuid, newUuid),
        Edge[IR, CDM17](newUuid, "subject", e.subjectUuid),
        e.predicateObject.map(obj => Edge[IR, CDM17](newUuid, "predicateObject", obj)),
        e.predicateObject2.map(obj => Edge[IR, CDM17](newUuid, "predicateObject2", obj)),

        e.predicateObjectPath.map(path => {
          val pathNode = IrPathNode(path)
          val label = "eventSynthesized_" ++ e.eventType.toString // if (e.eventType == EVENT_EXECUTE || e.eventType == EVENT_FORK) { "cmdLine" } else { "path" }
          (Edge[IR,IR](newUuid, label, pathNode.getUuid), pathNode)
        }),
        e.predicateObject2Path.map(path => {
          val pathNode = IrPathNode(path)
          val label = "eventSynthesized_" ++ e.eventType.toString // if (e.eventType == EVENT_FORK) { "cmdLine" } else { "path" }
          (Edge[IR,IR](newUuid, label, pathNode.getUuid), pathNode)
        }),
        e.properties.getOrElse(Map()).get("exec").map(cmdLine => {
          val pathNode = IrPathNode(cmdLine)
          (Edge[IR,IR](newUuid, "cmdLine", pathNode.getUuid), pathNode)
        })
      )
    }

  // Resolve a 'Subject'
  object SubjectEdges {
    type LocalPrincipalEdge = Edge[IR, CDM17]
    type ParentSubject = Option[Edge[IR, CDM17]]

    type CmdLinePathEdgeNode = Option[(Edge[IR,IR], IrPathNode)]
    type CmdLineIndirectPathEdgeNode = Option[(Edge[CDM17, IR], IrPathNode)]
  }
  def resolveSubject(s: Subject): Either[
    (
      IrSubject,
      UuidRemapper.PutCdm2Ir,
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
          val pathNode = IrPathNode(cmd)
          (Edge[CDM17,IR](s.parentSubject.get, "cmdLine", pathNode.getUuid), pathNode)
        }),
        UuidRemapper.PutCdm2Cdm(s.getUuid, s.parentSubject.get)
      ))
    } else {
      val newUuid = UUID.randomUUID()

      Left((
        IrSubject(newUuid, Seq(s.getUuid), Set(s.subjectType), s.startTimestampNanos),
        UuidRemapper.PutCdm2Ir(s.getUuid, newUuid),
        Edge[IR, CDM17](newUuid, "localPrincipal", s.localPrincipal),
        s.parentSubject.map(parent => Edge[IR, CDM17](newUuid, "parentSubject", parent)),
        s.cmdLine.map(cmd => {
          val pathNode = IrPathNode(cmd)
          (Edge[IR,IR](newUuid, "cmdLine", pathNode.getUuid), pathNode)
        })
      ))
    }

    // Collapse event
    //
    // TODO: better logic than just merge same successive events
    def collapseEvents(e1: Event, e2: IrEvent): Either[(UuidRemapper.PutCdm2Ir, IrEvent), (Event, IrEvent)] = {
      if (e1.eventType == e2.eventType) {
        val e2Updated = e2.copy(
          earliestTimestampNanos = Math.min(e1.timestampNanos, e2.earliestTimestampNanos),
          latestTimestampNanos = Math.min(e1.timestampNanos, e2.latestTimestampNanos),
          originalEntities = e1.getUuid +: e2.originalEntities
        )
        Left((UuidRemapper.PutCdm2Ir(e1.getUuid, e2.getUuid), e2Updated))
      } else {
        Right((e1, e2))
      }
    }

}
