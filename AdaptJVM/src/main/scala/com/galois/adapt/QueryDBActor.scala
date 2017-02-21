package com.galois.adapt

import akka.actor._

import org.apache.tinkerpop.gremlin.structure.{Graph, Edge, Vertex}
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONWriter

import java.io.ByteArrayOutputStream

import collection.JavaConverters._

// Encodes the various expected return/type formats of queries
sealed trait RestQuery { val query: String }
case class NodeQuery(query: String) extends RestQuery
case class EdgeQuery(query: String) extends RestQuery
case class StringQuery(query: String) extends RestQuery

/*
 * This actor receives string queries (that should follow the grammar in 'Query.scala'), attempts to
 * parse and run these, then returns the result encoded as JSON to the sender. Depending on the type
 * of the query sent, it will do more or less unpacking of information into a JSON array
 *
 *   - NodeQuery: All of the node fields are expanded out in JSON objects
 *   - EdgeQuery: Edge type, source and destination nodes are expanded in JSON objects
 *   - StringQuery: The results are kept as strings
 */
class QueryDBActor(graph: Graph, writer: GraphSONWriter) extends Actor {

  def receive = {
    case NodeQuery(q) =>
      sender() ! Query.run[Vertex](q, graph).map { vertices =>

        // Give a lower bound on the number of vertices
        println(s"Found: ${if (vertices.lengthCompare(1000) > 0) "> 1000" else vertices.length}")
        
        // Generate JSON to send back
        val byteStream = new ByteArrayOutputStream
        writer.writeVertices(byteStream, vertices.toIterator.asJava)

        byteStream.toString.split("\n").map { v =>
          if (v.length > 0) {
            val (first, second) = v.splitAt(1)
            first + """"type":"vertex",""" + second
          } else v
        }.mkString("[", ",", "]")
      }

    case EdgeQuery(q) =>
      sender() ! Query.run[Edge](q, graph).map { edges =>

        // Give a lower bound on the number of vertices
        println(s"Found: ${if (edges.lengthCompare(1000) > 0) "> 1000" else edges.length}")
        
        // Generate JSON to send back
        val byteStream = new ByteArrayOutputStream
        edges.foreach { edge => writer.writeEdge(byteStream, edge) }
        
        byteStream.toString.split("\\}\\{").mkString("[", "},{", "]")
     }

    case StringQuery(q) =>
      sender() ! Query.run[java.lang.Object](q, graph).map { results =>  
        
        // Give a lower bound on the number of vertices
        println(s"Found: ${results.length}")
       
        // Generate JSON to send back
        results.map(r => "\"" + r.toString.replaceAll("\"", "\\\"") + "\"") // escape quotation marks
               .mkString("[",",","]")                                       // return an array of strings
      }
  }
}
