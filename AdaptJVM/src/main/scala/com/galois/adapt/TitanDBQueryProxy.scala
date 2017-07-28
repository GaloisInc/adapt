package com.galois.adapt

import java.io.ByteArrayOutputStream

import akka.actor._
import org.apache.tinkerpop.gremlin.structure.{Edge, Vertex}
import org.apache.tinkerpop.gremlin.structure.io.IoCore
import spray.json.{JsArray, JsString}

import collection.JavaConverters._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


class TitanDBQueryProxy() extends Actor with ActorLogging {

  val graph = TitanFlowComponents.graph
//  val jsonWriter = TitanFlowComponents.graph.io(IoCore.graphson).writer().create()   // This was the cause of a lot of failed queries

  implicit val ec = context.dispatcher

  def receive = {

    case NodeQuery(q, shouldParse) =>
      println(s"Received node query: $q")
      sender() ! Future(
        Query.run[Vertex](q, graph).map { vertices =>
          println(s"Found nodes: ${vertices.length}")
//          println(s"Nodes = $vertices")
          val x = if (shouldParse) JsArray(vertices.map(ApiJsonProtocol.vertexToJson).toVector)
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
          val x = if (shouldParse) JsArray(edges.map(ApiJsonProtocol.edgeToJson).toVector)
          else edges
//          println(s"Returning edges: $x\n\n")
          x
        }
      )

    case StringQuery(q, shouldParse) =>
      println(s"Received string query: $q")
      sender() ! Future(
        Query.run[java.lang.Object](q, graph).map { results =>
          println(s"Found: ${results.length} items")
          val x = JsString(results.map(r => s""""${r.toString.replace("\\", "\\\\").replace("\"", "\\\"")}"""").mkString("[",",","]"))
//            .mkString("[", ",", "]")
//          println(s"Returning: ${results.size} items")
          x
        }
      )

    case EdgesForNodes(nodeIdList) =>
      sender() ! Try {
        graph.traversal().V(nodeIdList.asJava.toArray).bothE().toList.asScala.mkString("[",",","]")
      }

//    case GiveMeTheGraph => sender() ! graph
  }
}


sealed trait RestQuery { val query: String }
case class NodeQuery(query: String, shouldReturnJson: Boolean = true) extends RestQuery
case class EdgeQuery(query: String, shouldReturnJson: Boolean = true) extends RestQuery
case class StringQuery(query: String, shouldReturnJson: Boolean = false) extends RestQuery

case class EdgesForNodes(nodeIdList: Seq[Int])
//case object GiveMeTheGraph

