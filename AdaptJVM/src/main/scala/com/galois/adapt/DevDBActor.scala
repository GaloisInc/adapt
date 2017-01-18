package com.galois.adapt

import java.io.ByteArrayOutputStream

import akka.actor._
import com.galois.adapt.cdm13.{EpochMarker, Event, FileObject, SUBJECT_PROCESS, SimpleEdge, Subject}
import com.galois.adapt.scepter.HowMany
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.io.IoCore
import org.apache.tinkerpop.gremlin.structure.{Edge, Vertex}
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.structure.T.label
import java.nio.file.{Files, Paths}
import java.util.UUID

import com.thinkaurelius.titan.core.TitanFactory
import com.thinkaurelius.titan.core.schema.TitanGraphIndex
import org.apache.tinkerpop.gremlin.groovy.jsr223.GremlinGroovyScriptEngine
import org.apache.tinkerpop.gremlin.structure.io.graphson._

import collection.JavaConverters._
import scala.util.Try

class DevDBActor(localStorage: Option[String] = None) extends Actor{

  val graph = TinkerGraph.open()   // TODO: maybe don't hold this state inside the actor...?
//  val graph = TitanFactory.build.set("storage.backend", "inmemory").open


  graph.createIndex("uuid", classOf[Vertex])
  graph.createIndex("pid", classOf[Vertex])
//  graph.createIndex("url", classOf[Vertex])

  // Optionally load in existing data:
  localStorage.foreach( path =>
    if (Files.exists(Paths.get(path))) graph.io(IoCore.graphson()).readGraph(path)
  )

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
//      updateIncompleteEdges(f.uuid, v)

    case e: Event =>
      val l: List[Object] = e.asDBKeyValues.asInstanceOf[List[Object]]
      val v = graph.addVertex(l:_*)
      nodeIds += (e.uuid -> v)
//      updateIncompleteEdges(e.uuid, v)

    case s: Subject =>
      val l: List[Object] = s.asDBKeyValues.asInstanceOf[List[Object]]
      val subjectNode = graph.addVertex(l:_*)
      nodeIds += (s.uuid -> subjectNode)
      val parentVertexOpt = graph.traversal().V().has("pid",s.ppid).toList.asScala.headOption

      // TODO: do this with internal edges:
      subjectNode.addEdge("child_of", parentVertexOpt.getOrElse{
        val newUuid = UUID.randomUUID()
        val v = graph.addVertex(
          label, "Subject",
          "uuid", newUuid,
          "pid", new Integer(s.ppid),
          "subjectType", SUBJECT_PROCESS.toString
        )
        nodeIds += (newUuid -> v)
        v
      })

//      updateIncompleteEdges(s.uuid, subjectNode)

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
      println("Creating all missing nodes...")
      var nodeCreatedCounter = 0
      var edgeCreatedCounter = 0
      missingFromUuid.foreach { case (u,e) =>
        val v = findNode("uuid",u).getOrElse {
          nodeCreatedCounter += 1
          val newNode = graph.addVertex("uuid", u)
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
      if (idx >= 0)  // TODO: don't be lazy
      nodeIds += (l(idx + 1).asInstanceOf[UUID] -> newVertex)
//      updateIncompleteEdges(l(idx + 1).asInstanceOf[UUID], newVertex)


    case NodeQuery(q) =>
      val t = Try {
        val results = QueryRunner.eval(graph,
//          q
          transformQueryIntoSomethingThatStupidGremlinWillEvaluateCorrectlyThisIsABadIdeaShouldDoItAnotherWay(q)
        ).asInstanceOf[GraphTraversal[_,Vertex]].toList.asScala.toList
        println(s"Found: ${results.length}")
        results
      }
      val jsonTry = t.map { vertices =>
        val byteStream = new ByteArrayOutputStream
        GraphSONWriter.build().create().writeVertices(byteStream, vertices.toIterator.asJava)

        byteStream.toString.split("\n").map { v =>
          if (v.length > 0) {
            val (first, second) = v.splitAt(1)
            first + """"type":"vertex",""" + second
          } else v
        }.mkString("[", ",", "]")
      }
      sender() ! jsonTry

    case EdgeQuery(q) =>
      val t = Try {
        val results = QueryRunner.eval(graph,
//          q
          transformQueryIntoSomethingThatStupidGremlinWillEvaluateCorrectlyThisIsABadIdeaShouldDoItAnotherWay(q)
        ).asInstanceOf[GraphTraversal[_,Edge]].toList.asScala.toList
        println(s"Found: ${results.length}")
        results

      }
      val jsonTry = t.map { edges =>
        val byteStream = new ByteArrayOutputStream
        edges.foreach { e =>
          GraphSONWriter.build().create().writeEdge(byteStream, e)
        }
        byteStream.toString.split("\\}\\{").mkString("[", "},{", "]")
      }
      sender() ! jsonTry

    case StringQuery(q) =>
      val t = Try {
        val results = QueryRunner.eval(graph,
//          q
          transformQueryIntoSomethingThatStupidGremlinWillEvaluateCorrectlyThisIsABadIdeaShouldDoItAnotherWay(q)
        ).asInstanceOf[GraphTraversal[_,_]].toList.toString
        println(s"Found: ${results.length}")
        results

      }
      sender() ! t
   
    case AnyQuery(q) =>
      sender ! Try {
        //println(s"query: $q")
        val results = QueryRunner.eval(graph,
          transformQueryIntoSomethingThatStupidGremlinWillEvaluateCorrectlyThisIsABadIdeaShouldDoItAnotherWay(q)
        ).asInstanceOf[GraphTraversal[_,_]].toList.asScala.toList
       // println(s"Found: ${results.length}")
        results
      }

    case EdgesForNodes(nodeIdList) =>
      val t = Try(
        graph.traversal().V(nodeIdList.asJava.toArray).bothE().toList.asScala.mkString("[",",","]")
      )
      sender() ! t

    case HowMany(_) =>
      sender() ! graph.vertices().asScala.size

    case GiveMeTheGraph => sender() ! graph

    case Shutdown =>
//      println(s"Incomplete Edge count: ${missingFromUuid.size + missingToUuid.size}")
      localStorage.fold()(path => graph.io(IoCore.graphson()).writeGraph(path))
      sender() ! (missingFromUuid.size + missingToUuid.size)
  }


  def transformQueryIntoSomethingThatStupidGremlinWillEvaluateCorrectlyThisIsABadIdeaShouldDoItAnotherWay(q: String): String = {
    val lineSeparatedQuery = q.split(";")
    var finalQuery = lineSeparatedQuery.last.trim
    if (lineSeparatedQuery.length > 1) (0 until lineSeparatedQuery.length - 1).foreach { idx =>
      val x = lineSeparatedQuery(idx).split("=")
      val symbol = x.head.trim
      val statement = x.last.trim
      val individualItemList = statement.stripPrefix("[").stripSuffix(".toArray()").stripSuffix("]").split(",")
      val newStatement = individualItemList.map(_.trim + "L").mkString("[",",","].toArray()")
      finalQuery = finalQuery.replaceFirst(symbol, newStatement)   //replaceFirst("g.V("+symbol+")", "g.V("+newStatement+")")
    } else {
      if (finalQuery.startsWith("g.V([")) {
        val splitArray = finalQuery.stripPrefix("g.V([").split("].toArray\\([)]\\)", 2)
        val items = splitArray(0).split(",")
        val remainder = splitArray(1)
        finalQuery = items.mkString("g.V([","L,","L].toArray())") + remainder
      } else if (finalQuery.startsWith("g.V(") && ! finalQuery.startsWith("g.V()")) {
        val splitArray = finalQuery.stripPrefix("g.V(").split("[)]", 2)
        val items = splitArray(0).stripPrefix("[").stripSuffix("]").split(",")
        val remainder = splitArray(1)
        finalQuery = items.mkString("g.V(","L,","L)") + remainder
      }
    }
    //println(s"rewritten query: $finalQuery")
    finalQuery
  }
}


trait RestQuery { val query: String }
case class NodeQuery(query: String) extends RestQuery
case class EdgeQuery(query: String) extends RestQuery
case class StringQuery(query: String) extends RestQuery
case class AnyQuery(query: String) extends RestQuery

case class EdgesForNodes(nodeIdList: Seq[Int])

case object GiveMeTheGraph


case object Shutdown
