package com.galois.adapt

import akka.actor._

import com.galois.adapt.cdm13.{EpochMarker, Event, FileObject, SUBJECT_PROCESS, SimpleEdge, Subject}
import com.galois.adapt.scepter.HowMany

import org.apache.tinkerpop.gremlin.structure.io.graphson._
import org.apache.tinkerpop.gremlin.structure.{Edge, Vertex, T}
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph

import java.nio.file.{Files, Paths}
import java.util.UUID
import java.io.ByteArrayOutputStream

import collection.JavaConverters._
import scala.util.Try

class DevDBActor(graph: TinkerGraph) extends Actor {

  var nodeIds = collection.mutable.Map.empty[UUID, Vertex]

  var missingFromUuid = collection.mutable.Map.empty[UUID, SimpleEdge]
  var missingToUuid = collection.mutable.Map.empty[UUID, SimpleEdge]
  
  def updateIncompleteEdges(uuid: UUID, node: Vertex) = {
    if (missingFromUuid contains uuid) {
      val edge = missingFromUuid(uuid)
      missingFromUuid -= uuid
      findNode("uuid", edge.toUuid).fold[Unit] {
        missingToUuid += (edge.toUuid -> edge)
      }{ toNode =>
        node.addEdge(edge.edgeType.toString, toNode)
      }
    } else {
      if (missingToUuid contains uuid) {
        val edge = missingToUuid(uuid)
        missingToUuid -= uuid
        findNode("uuid", edge.fromUuid).fold[Unit] {
          missingFromUuid += (edge.fromUuid -> edge)
        } { fromNode =>
          fromNode.addEdge(edge.edgeType.toString, node)
        }
      } else {
        // Do nothing.
      }
    }
  }
  
  var pids = collection.mutable.Map.empty[Int,Vertex]
  var missingToPid = collection.mutable.Map.empty[Int, Seq[Vertex]]

  // Update dangling edges created by nodes who pointed to a PID that didn't exist at the time
  // TODO: Check that this works - we don't currently have data to try it out
  def updatePid(pid: Int, node: Vertex, parent: Option[Int]) = {
    pids += (pid -> node)

    parent.foreach { ppid =>
        pids get ppid match {
          case Some(pnode) =>
            node.addEdge("child_process_of", pnode)
          case None => 
            missingToPid += (ppid -> (node +: missingToPid.get(ppid).getOrElse(Seq())))
        }
    }

    (missingToPid get pid).foreach { children =>
      missingToPid -= pid
      children.foreach(child => child.addEdge("child_of", node))
    }
  }

  var libraries = collection.mutable.Map.empty[String, Vertex]

  // TODO: Check that this works - we don't currently have data to try it out
  def getLibrary(name: String): Vertex = libraries.get(name) match {
      case Some(v) => v
      case None => 
        val v = graph.addVertex("Library","name",name)
        libraries(name) = v
        v
    }

  // TODO: TinkerGraph doesn't update the starting IDs when reading data in from a file.
  // Will result in: IllegalArgumentException: Vertex with id already exists: 0
  //  ... on the THIRD run. (after it _writes_ a file containing two of the same index ids)

  def findNode(key: String, value: Any): Option[Vertex] = {
    if (key == "uuid" && value.isInstanceOf[UUID]) nodeIds.get(value.asInstanceOf[UUID])
    else graph.traversal().V().has(key,value).toList.asScala.headOption
  }

  def receive = {

    case f: FileObject =>
      val l: List[Object] = f.asDBKeyValues.asInstanceOf[List[Object]]
      val v = graph.addVertex(l:_*)
      nodeIds += (f.uuid -> v)
//      println(s"Nodes so far: ${nodeIds.size}")
      updateIncompleteEdges(f.uuid, v)

    case e: Event =>
      val l: List[Object] = e.asDBKeyValues.asInstanceOf[List[Object]]
      val v = graph.addVertex(l:_*)
      nodeIds += (e.uuid -> v)
      updateIncompleteEdges(e.uuid, v)

    case s: Subject =>
      val l: List[Object] = s.asDBKeyValues.asInstanceOf[List[Object]]
      val subjectNode = graph.addVertex(l:_*)
      nodeIds += (s.uuid -> subjectNode)
      updatePid(s.pid, subjectNode, Some(s.ppid))
      updateIncompleteEdges(s.uuid, subjectNode)
      s.importedLibraries.foreach(_.foreach { name => subjectNode.addEdge("imports",getLibrary(name)) })
      s.exportedLibraries.foreach(_.foreach { name => subjectNode.addEdge("exports",getLibrary(name)) })

    case e: SimpleEdge =>
      val fromOpt = findNode("uuid", e.fromUuid)
      fromOpt.fold[Unit] {
        missingFromUuid += (e.fromUuid -> e)
      } { from =>
        val toOpt = findNode("uuid", e.toUuid)
        toOpt.fold[Unit] {
          missingToUuid += (e.toUuid -> e)
        } { to =>
          from.addEdge(e.edgeType.toString, to)
        }
      }

    case EpochMarker =>
      println(s"EPOCH BOUNDARY!")
      println(s"FROM nodes missed during epoch: ${missingFromUuid.size}")
      println(s"TO nodes missed during epoch: ${missingToUuid.size}")
      println(s"TO PIDs missed during epoch: ${missingToPid.size}")
      println("Creating all missing nodes...")
      var nodeCreatedCounter = 0
      var edgeCreatedCounter = 0
      missingFromUuid.foreach { case (u,e) =>
        val v = findNode("uuid",u).getOrElse {
          nodeCreatedCounter += 1
          val newNode = graph.addVertex(
            "uuid", u,
            "synthetic", "MISSING UUID"
          )
          nodeIds += (u -> newNode)
          newNode
        }
        findNode("uuid", e.toUuid).fold[Unit] {
          missingToUuid += (e.toUuid -> e)
        } { toNode =>
          edgeCreatedCounter += 1
          v.addEdge(e.edgeType.toString, toNode)
        }
      }
      missingFromUuid = collection.mutable.Map.empty
      missingToUuid foreach { case (u,e) =>
        val v = findNode("uuid",u).getOrElse {
          nodeCreatedCounter += 1
          val newNode = graph.addVertex("uuid", u)
          nodeIds += (u -> newNode)
          newNode
        }
        findNode("uuid", e.fromUuid).fold[Unit]{
          throw new RuntimeException(s"But everything should have been created by now!")
        } { fromNode =>
          edgeCreatedCounter += 1
          fromNode.addEdge(e.edgeType.toString, v)
        }
      }
      missingToPid.foreach { case (p,vs) => 
        nodeCreatedCounter += 1
        val newNode = graph.addVertex(
          T.label, "Subject",
          "subjectType", "SUBJECT_PROCESS",
          "pid", p.asInstanceOf[java.lang.Object],
          "synthetic", "MISSING_PID"
        )
        updatePid(p, newNode, None)
      }
      //graph.tx().commit()
        
      missingToUuid = collection.mutable.Map.empty
      println(s"Nodes created at epoch close: $nodeCreatedCounter")
      println(s"Edges created at epoch close: $edgeCreatedCounter")
      println("Done creating all missing nodes.")

    case cdm13: DBWritable =>
      val l: List[Object] = cdm13.asDBKeyValues.asInstanceOf[List[Object]]
      if (l.length % 2 == 1) println(s"OFFENDING: $cdm13\n$l")
      val newVertex = graph.addVertex(l:_*)

      // TODO: so gross!
      val idx = l.indexOf("uuid")
      if (idx >= 0) {  // TODO: don't be lazy
        nodeIds += (l(idx + 1).asInstanceOf[UUID] -> newVertex)
        updateIncompleteEdges(l(idx + 1).asInstanceOf[UUID], newVertex)
      }

    case EdgesForNodes(nodeIdList) =>
      sender() ! Try {
        graph.traversal().V(nodeIdList.asJava.toArray).bothE().toList.asScala.mkString("[",",","]")
      }

    case HowMany("missing") =>
      sender() ! (missingFromUuid.size + missingToUuid.size)

    case HowMany("total") =>
      sender() ! graph.vertices().asScala.size
  }
}


case class EdgesForNodes(nodeIdList: Seq[Int])

