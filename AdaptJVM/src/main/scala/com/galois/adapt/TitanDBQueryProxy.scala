package com.galois.adapt

import java.io.ByteArrayOutputStream

import akka.actor._
import org.apache.tinkerpop.gremlin.structure.{Edge, Vertex}
import org.apache.tinkerpop.gremlin.structure.io.IoCore
import collection.JavaConverters._
import scala.concurrent.Future
import scala.util.Try


class TitanDBQueryProxy() extends Actor with ActorLogging {

  val graph = TitanFlowComponents.graph
  val jsonWriter = TitanFlowComponents.graph.io(IoCore.graphson).writer().create()

  implicit val ec = context.dispatcher

  def receive = {

    case NodeQuery(q, shouldParse) =>
      println(s"Received node query: $q")
      sender() ! Future(
        Query.run[Vertex](q, graph).map { vertices =>
          println(s"Found nodes: ${vertices.length}")
//          println(s"Nodes = $vertices")
          val x = if (shouldParse) vertices.map(ApiJsonProtocol.vertexToJson).mkString("[",",","]")
                  else vertices
//          println(s"Returning: ${vertices.size} nodes")
          x
        }
      )

    case EdgeQuery(q, shouldParse) =>
      println(s"Received new edge query: $q")
      sender() ! Future(
        Query.run[Edge](q, graph).map { edges =>
          println(s"Found edges: ${edges.length}")
          val x = if (shouldParse) {
            edges.map(ApiJsonProtocol.edgeToJson).mkString("[", ",", "]")
          } else edges
//          println(s"Returning edges: $x\n\n")
          x
        }
      )

    case StringQuery(q, shouldParse) =>
      println(s"Received string query: $q")
      sender() ! Future(
        Query.run[java.lang.Object](q, graph).map { results =>
          println(s"Found: ${results.length} items")
          val x = results.map(r => s""""${r.toString.replace("\\", "\\\\").replace("\"", "\\\"")}"""")
            .mkString("[", ",", "]")
//          println(s"Returning: ${results.size} items")
          x
        }
      )

    case EdgesForNodes(nodeIdList) =>
      sender() ! Try {
        graph.traversal().V(nodeIdList.asJava.toArray).bothE().toList.asScala.mkString("[",",","]")
      }

    case GiveMeTheGraph => sender() ! graph
  }
}


//sealed trait RestQuery { val query: String }
//case class NodeQuery(query: String) extends RestQuery
//case class EdgeQuery(query: String) extends RestQuery
//case class StringQuery(query: String) extends RestQuery
//
//case class EdgesForNodes(nodeIdList: Seq[Int])
//case object GiveMeTheGraph

