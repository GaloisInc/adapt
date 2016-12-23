package com.galois.adapt

import java.io.ByteArrayOutputStream

import akka.actor._
import com.galois.adapt.cdm13.{Event, FileObject, Subject}
import com.galois.adapt.scepter.HowMany
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.io.IoCore
import org.apache.tinkerpop.gremlin.structure.{Edge, Vertex}
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.structure.T.label
import java.nio.file.{Files, Paths}

import com.thinkaurelius.titan.core.TitanFactory
import org.apache.tinkerpop.gremlin.structure.io.graphson._

import collection.JavaConverters._
import scala.util.Try

class DatabaseActor(localStorage: Option[String] = None) extends Actor{

  val graph = TinkerGraph.open()   // TODO: maybe don't hold this state inside the actor...?
//  val graph = TitanFactory.build.set("storage.backend", "inmemory").open

  graph.createIndex("pid", classOf[Vertex])
  graph.createIndex("url", classOf[Vertex])

  // Optionally load in existing data:
  localStorage.foreach( path =>
    if (Files.exists(Paths.get(path))) graph.io(IoCore.graphson()).readGraph(path)
  )


  // TODO: TinkerGraph doesn't update the starting IDs when reading data in from a file.
  // Will result in: IllegalArgumentException: Vertex with id already exists: 0
  //  ... on the THIRD run. (after it _writes_ a file containing two of the same index ids)



  def receive = {
    case f: FileObject =>
      val l = List(
        label, "FileObject",
        "uuid", f.uuid,
        "url", f.url,
        "version", new Integer(f.version)
      )
      val v = graph.addVertex(l:_*)

    case e: Event =>
      graph.addVertex(
        label, "Event",
        "uuid", e.uuid,
        "EventType", e.eventType.toString,
        "ThreadId", new Integer(e.threadId)
      )

    case s: Subject =>
      val subjectNode = graph.addVertex(label, "Subject", "uuid", s.uuid, "pid", new Integer(s.pid))
      val parentVertexOpt = graph.traversal().V().has("pid",s.ppid).toList.asScala.headOption
      subjectNode.addEdge("child_of", parentVertexOpt.getOrElse(
        graph.addVertex(
          label, "Subject",
          "pid", new Integer(s.ppid)
        )
      ))

    case NodeQuery(q) =>
      val t = Try {
        QueryEval.evaluate(graph, q).asInstanceOf[GraphTraversal[String,Vertex]] //.toList.asScala.toList
      }
      val jsonTry = t.map { vertices =>
        val byteStream = new ByteArrayOutputStream
        GraphSONWriter.build().create().writeVertices(byteStream, vertices)

        val jsonString = byteStream.toString.split("\n").map { v =>
          val (first, second) = v.splitAt(1)
          first + """"type":"vertex",""" + second
        }.mkString("[", ",", "]")
//        println(s"NodeQuery results: $jsonString")
        jsonString
      }
      sender() ! jsonTry

    case EdgeQuery(q) =>
      val t = Try {
        QueryEval.evaluate(graph, q).asInstanceOf[GraphTraversal[String,Edge]].toList.asScala
      }
      println(s"EdgeQuery results: $t")

    case HowMany(_) =>
      sender() ! graph.vertices().asScala.size

    case Shutdown =>
      localStorage.fold()(path => graph.io(IoCore.graphson()).writeGraph(path))
      sender() ! ()
  }
}

case class NodeQuery(queryReturningNodes: String)
case class EdgeQuery(queryReturningEdges: String)


case object Shutdown