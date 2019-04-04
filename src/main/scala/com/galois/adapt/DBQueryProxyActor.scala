package com.galois.adapt

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Keep, Sink}
import akka.util.Timeout
import com.galois.adapt.adm.{ADM, EdgeAdm2Adm}
import com.galois.adapt.cdm18.CDM18
import com.galois.adapt.cdm19.CDM19
import com.galois.adapt.cdm20.CDM20
import org.apache.tinkerpop.gremlin.structure.{Edge, Graph, Vertex}
import spray.json.{JsArray, JsNull, JsNumber, JsObject, JsString, JsValue}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}
import scala.concurrent.duration._

trait DBQueryProxyActor extends Actor with ActorLogging {
  implicit val ec: ExecutionContext = context.dispatcher

  // The actor is responsible for the graph
  val graph: Graph

  // Mutable set tracking all of the namespaces see so far
  val namespaces: mutable.Set[String] = mutable.Set.empty

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
      log.debug(s"Received node query: $q")
      sender() ! FutureTx {
        Query.run[Vertex](q, graph, namespaces).map { vertices =>
//          log.info(s"Found: ${vertices.length} nodes")
          if (shouldParse) JsArray(vertices.map(ApiJsonProtocol.vertexToJson).toVector)
          else vertices.toList.toStream
        }
      }

    // Run a query that returns edges
    case EdgeQuery(q, shouldParse) =>
      log.debug(s"Received new edge query: $q")
      sender() ! FutureTx {
        Query.run[Edge](q, graph, namespaces).map { edges =>
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
        Query.run[java.lang.Object](q, graph, namespaces).map { results =>
//          log.info(s"Found: ${results.length} items")
          if (shouldParse) {
            DBQueryProxyActor.toJson(results.toList)
          } else {
            JsString(results.map(r => s""""${r.toString.replace("\\", "\\\\").replace("\"", "\\\"")}"""").mkString("[", ",", "]"))
          }
        }
      }

    // Write a batch of CDM to the DB
    case WriteCdmToDB(cdms) =>
      DBNodeableTx(cdms).getOrElse(log.error(s"Failure writing to DB with CDMs: $cdms"))
      sender() ! Ack

    // Write a batch of ADM to the DB
    case WriteAdmToDB(adms: Seq[Either[ADM,EdgeAdm2Adm]]) =>
      AdmTx(adms).getOrElse(log.error(s"Failure writing to DB with ADMs: ")) //$adms"))

      // Register the new namespaces
      adms.foreach {
        case Left(adm) => namespaces.add(adm.uuid.namespace)
        case _ => ;
      }

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

case class WriteCdmToDB(cdms: Seq[DBNodeable[_]])
case class WriteAdmToDB(irs: Seq[Either[ADM,EdgeAdm2Adm]])


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

  def graphActorCdm18WriteSink(graphActor: ActorRef, completionMsg: Any = CompleteMsg)(implicit timeout: Timeout): Sink[(String,CDM18), NotUsed] = Flow[(String,CDM18)]
    .collect { case (_, cdm: DBNodeable[_]) => cdm }
    .groupedWithin(1000, 1 second)
    .map(WriteCdmToDB.apply)
    .toMat(Sink.actorRefWithAck[WriteCdmToDB](graphActor, InitMsg, Ack, completionMsg))(Keep.right)

  def graphActorCdm19WriteSink(graphActor: ActorRef, completionMsg: Any = CompleteMsg)(implicit timeout: Timeout): Sink[(String,CDM19), NotUsed] = Flow[(String,CDM19)]
    .collect { case (_, cdm: DBNodeable[_]) => cdm }
    .groupedWithin(1000, 1 second)
    .map(WriteCdmToDB.apply)
    .toMat(Sink.actorRefWithAck[WriteCdmToDB](graphActor, InitMsg, Ack, completionMsg))(Keep.right)

  def graphActorCdm20WriteSink(graphActor: ActorRef, completionMsg: Any = CompleteMsg)(implicit timeout: Timeout): Sink[(String,CDM20), NotUsed] = Flow[(String,CDM20)]
    .collect { case (_, cdm: DBNodeable[_]) => cdm }
    .groupedWithin(1000, 1 second)
    .map(WriteCdmToDB.apply)
    .toMat(Sink.actorRefWithAck[WriteCdmToDB](graphActor, InitMsg, Ack, completionMsg))(Keep.right)


  def graphActorAdmWriteSink(graphActor: ActorRef, completionMsg: Any = CompleteMsg): Sink[Either[ADM,EdgeAdm2Adm], NotUsed] = Flow[Either[ADM,EdgeAdm2Adm]]
    .groupedWithin(1000, 1 second)
    .map(WriteAdmToDB.apply)
    .buffer(4, OverflowStrategy.backpressure)
    .toMat(Sink.actorRefWithAck[WriteAdmToDB](graphActor, InitMsg, Ack, completionMsg))(Keep.right)
}
