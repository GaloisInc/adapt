package com.galois.adapt

import java.io.ByteArrayOutputStream

import akka.actor._
import org.apache.tinkerpop.gremlin.structure.{Edge, Vertex}
import org.apache.tinkerpop.gremlin.structure.io.IoCore
import spray.json._

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
          if (shouldParse) {
            toJson(results.toList) 
          } else {
            JsString(results.map(r => s""""${r.toString.replace("\\", "\\\\").replace("\"", "\\\"")}"""").mkString("[",",","]"))
          }
        }
      )

    case EdgesForNodes(nodeIdList) =>
      sender() ! Try {
        graph.traversal().V(nodeIdList.asJava.toArray).bothE().toList.asScala.mkString("[",",","]")
      }

//    case GiveMeTheGraph => sender() ! graph
  }

  import scala.collection.JavaConversions._

  def toJson: Any => JsValue = {
  
    // Numbers
    case n: Int => JsNumber(n)
    case n: Long => JsNumber(n)
    case n: Double => JsNumber(n)
    case n: java.lang.Long => JsNumber(n)
    case n: java.lang.Double => JsNumber(n)
    
    // Strings 
    case s: String => JsString(s) 
    
    // Lists
    case l: java.util.List[_] => toJson(l.toList)
    case l: List[_] => JsArray(l map toJson)
  
    // Maps
    case m: java.util.Map[_,_] => toJson(m.toMap)
    case m: Map[_,_] => JsObject(m map { case (k,v) => (k.toString, toJson(v)) })
  
    // Special cases (commented out because they are pretty verbose)
    // case v: Vertex => ApiJsonProtocol.vertexToJson(v)
    // case e: Edge => ApiJsonProtocol.edgeToJson(e)
  
    // Other
    //
    // Any custom 'toString' that is longer than 250 characters is probably not a good idea...
    case o => JsString(o.toString.take(250))
  
  }
}


sealed trait RestQuery { val query: String }
case class NodeQuery(query: String, shouldReturnJson: Boolean = true) extends RestQuery
case class EdgeQuery(query: String, shouldReturnJson: Boolean = true) extends RestQuery
case class StringQuery(query: String, shouldReturnJson: Boolean = false) extends RestQuery

case class EdgesForNodes(nodeIdList: Seq[Int])
//case object GiveMeTheGraph



