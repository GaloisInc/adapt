package com.galois.adapt

import java.io.ByteArrayOutputStream

import akka.actor._
import org.apache.tinkerpop.gremlin.structure.{Edge, Vertex}
import org.apache.tinkerpop.gremlin.structure.io.IoCore
import collection.JavaConverters._
import scala.util.Try


class TitanDBQueryProxy() extends Actor with ActorLogging {

  val graph = TitanFlowComponents.graph
  val jsonWriter = TitanFlowComponents.graph.io(IoCore.graphson).writer().create()

  def receive = {

    case NodeQuery(q, shouldParse) =>
      sender() ! Query.run[Vertex](q, graph).map { vertices =>
//        println(s"Found: ${vertices.length}")
        if (shouldParse) {
          val byteStream = new ByteArrayOutputStream
          jsonWriter.writeVertices(byteStream, vertices.toIterator.asJava)
          byteStream.toString.split("\n").map { v =>
            if (v.nonEmpty) {
              val (first, second) = v.splitAt(1)
              first + """"type":"vertex",""" + second
            } else v
          }.mkString("[", ",", "]")
        } else vertices
      }

    case EdgeQuery(q, shouldParse) =>
      sender() ! Query.run[Edge](q, graph).map { edges =>
        if (shouldParse) {
//          println(s"Found: ${edges.length}")
          val byteStream = new ByteArrayOutputStream
          edges.foreach { edge =>
            jsonWriter.writeEdge(byteStream, edge)
          }
          byteStream.toString.split("\\}\\{").mkString("[", "},{", "]")
        } else edges
      }

    case StringQuery(q, shouldParse) =>
      sender() ! Query.run[java.lang.Object](q, graph).map { results =>
        println(s"Found: ${results.length}")
        results.map(r => s""""${r.toString.replace("\\","\\\\").replace("\"","\\\"")}"""")
          .mkString("[",",","]")
      }

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

