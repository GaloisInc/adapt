package com.galois.adapt

import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph
import org.neo4j.tinkerpop.api.impl.Neo4jGraphAPIImpl
import akka.actor._
import org.apache.tinkerpop.gremlin.structure.{Edge, Graph, Vertex}
import org.neo4j.graphdb.{GraphDatabaseService, Label, Node => NeoNode}
import spray.json.{JsArray, JsString}

import collection.mutable.{Map => MutableMap}
import collection.JavaConverters._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class Neo4jDBQueryProxy(neoGraph: GraphDatabaseService) extends Actor with ActorLogging {

  val graph: Graph = Neo4jGraph.open(new Neo4jGraphAPIImpl(neoGraph))
  implicit val ec = context.dispatcher

  var counter = 0L

  def receive = {

    // Run the given query with the expectation that the output type be vertices. Optionally encode
    // the resulting list into JSON
    case NodeQuery(q, shouldParse) =>
      println(s"Received node query: $q")
      sender() ! Future(
        Query.run[Vertex](q, graph).map { vertices =>
          println(s"Found: ${vertices.length} nodes")
          if (shouldParse)
            JsArray(vertices.map(ApiJsonProtocol.vertexToJson).toVector)
          else
            vertices
        }
      )

    // Run the given query with the expectation that the output type be edges. Optionally encode
    // the resulting list into JSON
    case EdgeQuery(q, shouldParse) =>
      println(s"Received new edge query: $q")
      sender() ! Future(
        Query.run[Edge](q, graph).map { edges =>
          println(s"Found: ${edges.length} edges")
          if (shouldParse)
            JsArray(edges.map(ApiJsonProtocol.edgeToJson).toVector)
          else
            edges
        }
      )

    // Run the given query without specifying what the output type will be. This is the variant used
    // by 'cmdline_query.py'
    case StringQuery(q, shouldParse) =>
      println(s"Received string query: $q")
      sender() ! Future(
        Query.run[java.lang.Object](q, graph).map { results =>
          println(s"Found: ${results.length} items")
          JsString(results.map(r => s""""${r.toString.replace("\\", "\\\\").replace("\"", "\\\"")}"""").mkString("[",",","]"))
        }
      )

    // Get all the edges that touch the given nodes (either incoming or outgoing) 
    case EdgesForNodes(nodeIdList) =>
      sender() ! Try {
        graph.traversal().V(nodeIdList.asJava.toArray).bothE().toList.asScala.mkString("[",",","]")
      }

    // Use with care! Unless you have a really good reason (like running acceptance tests), you
    // probably shouldn't be asking for the whole graph.
    case GiveMeTheGraph => sender() ! graph


    case WriteToNeo4jDB(cdms) =>
      counter = counter + cdms.size
//      log.info(s"DBActor received: $counter")
      sender() ! Neo4jFlowComponents.neo4jTx(cdms, neoGraph)

    case FailureMsg(e: Throwable) =>
      log.error(s"FAILED: {}", e)
    case CompleteMsg =>
      log.info(s"GOT complete message")
      sender() ! Success(())
    case InitMsg =>
      log.info(s"GOT init message")
      sender() ! Success(())
  }
}


sealed trait RestQuery { val query: String }
case class NodeQuery(query: String, shouldReturnJson: Boolean = true) extends RestQuery
case class EdgeQuery(query: String, shouldReturnJson: Boolean = true) extends RestQuery
case class StringQuery(query: String, shouldReturnJson: Boolean = false) extends RestQuery

case class EdgesForNodes(nodeIdList: Seq[Int])
case object GiveMeTheGraph

case class WriteToNeo4jDB(cdms: Seq[DBNodeable])