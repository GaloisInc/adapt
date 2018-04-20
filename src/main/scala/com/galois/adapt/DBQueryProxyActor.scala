package com.galois.adapt

import akka.actor.{Actor, ActorLogging}
import com.galois.adapt.adm.{ADM, EdgeAdm2Adm}
import org.apache.tinkerpop.gremlin.structure.{Edge, Graph, Vertex}
import spray.json.{JsArray, JsNull, JsNumber, JsObject, JsString, JsValue}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

trait DBQueryProxyActor extends Actor with ActorLogging {
  implicit val ec: ExecutionContext = context.dispatcher

  // The actor is responsible for the graph
  val graph: Graph

  // How to make a future of a transation
  def FutureTx[T](body: => T)(implicit ec: ExecutionContext): Future[T]

  // Writing CDM and ADM in a transaction
  def DBNodeableTx(cdms: Seq[DBNodeable[_]]): Try[Unit]
  def AdmTx(adms: Seq[Either[ADM, EdgeAdm2Adm]]): Try[Unit]


  var streamsFlowingInToThisActor = 0


  def receive = {

    case Ready => sender() ! Ready

    // Run a query that returns vertices
    case NodeQuery(q, shouldParse) =>
//      log.info(s"Received node query: $q")
      sender() ! FutureTx {
        Query.run[Vertex](q, graph).map { vertices =>
//          log.info(s"Found: ${vertices.length} nodes")
          if (shouldParse) JsArray(vertices.map(ApiJsonProtocol.vertexToJson).toVector)
          else vertices.toList.toStream
        }
      }

    // Run a query that returns edges
    case EdgeQuery(q, shouldParse) =>
//      log.info(s"Received new edge query: $q")
      sender() ! FutureTx {
        Query.run[Edge](q, graph).map { edges =>
//          log.info(s"Found: ${edges.length} edges")
          if (shouldParse)
            JsArray(edges.map(ApiJsonProtocol.edgeToJson).toVector)
          else
            edges.toList.toStream
        }
      }

    // Run the query without specifying what the output type will be. This is the variant used by 'cmdline_query.py'
    case StringQuery(q, shouldParse) =>
//      log.info(s"Received string query: $q")
      sender() ! FutureTx {
        Query.run[java.lang.Object](q, graph).map { results =>
//          log.info(s"Found: ${results.length} items")
          if (shouldParse) {
            DBQueryProxyActor.toJson(results.toList)
          } else {
            JsString(results.map(r => s""""${r.toString.replace("\\", "\\\\").replace("\"", "\\\"")}"""").mkString("[", ",", "]"))
          }
        }
      }

    // Write a batch of CDM to the DB
    case WriteCdmToNeo4jDB(cdms) =>
      DBNodeableTx(cdms).getOrElse(log.error(s"Failure writing to DB with CDMs: $cdms"))
      sender() ! Ack

    // Write a batch of ADM to the DB
    case WriteAdmToNeo4jDB(adms) =>
      AdmTx(adms).getOrElse(log.error(s"Failure writing to DB with ADMs: ")) //$adms"))
      sender() ! Ack

    case Failure(e: Throwable) =>
      log.error(s"FAILED in DBActor: {}", e)
      // sender() ! Ack   // TODO: Should this ACK?

    case CompleteMsg =>
      streamsFlowingInToThisActor -= 1
      log.info(s"DBActor received a completion message. Remaining streams: $streamsFlowingInToThisActor")
      // sender() ! Ack

    case KillJVM =>
      streamsFlowingInToThisActor -= 1
      log.warning(s"DBActor received a termination message. Remaining streams: $streamsFlowingInToThisActor")
      if (streamsFlowingInToThisActor == 0) {
        log.warning(s"Shutting down the JVM.")
        graph.close()
        Runtime.getRuntime.halt(0)
      }
      // sender() ! Ack

    case InitMsg =>
      log.info(s"DBActor received an initialization message")
      streamsFlowingInToThisActor += 1
      sender() ! Ack
  }

}

sealed trait RestQuery { val query: String }
case class NodeQuery(query: String, shouldReturnJson: Boolean = true) extends RestQuery
case class EdgeQuery(query: String, shouldReturnJson: Boolean = true) extends RestQuery
case class StringQuery(query: String, shouldReturnJson: Boolean = false) extends RestQuery
case class CypherQuery(query: String, shouldReturnJson: Boolean = true) extends RestQuery

case class EdgesForNodes(nodeIdList: Seq[Int])
case object Ready

case class WriteCdmToNeo4jDB(cdms: Seq[DBNodeable[_]])
case class WriteAdmToNeo4jDB(irs: Seq[Either[ADM,EdgeAdm2Adm]])


case object Ack
case object CompleteMsg
case object KillJVM
case object InitMsg

object DBQueryProxyActor {
  import scala.collection.JavaConversions._

  def toJson: Any => JsValue = {
    // Null value
    case x if Option(x).isEmpty => JsNull

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

    // Special cases (commented out because they are pretty verbose) and functionality is
    // anyways accessible via the "vertex" and "edges" endpoints
    //   case v: Vertex => ApiJsonProtocol.vertexToJson(v)
    //   case e: Edge => ApiJsonProtocol.edgeToJson(e)

    // Other: Any custom 'toString'
    case o => JsString(o.toString)

  }
}