package com.galois.adapt

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.mapdb.DBMaker
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import org.apache.tinkerpop.gremlin.structure.{Element => VertexOrEdge}
import scala.collection.mutable.{Map => MutableMap}
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success, Try}


class AnomalyManager extends Actor with ActorLogging {
  val anomalies = MutableMap.empty[UUID, MutableMap[String, (Double, Set[UUID])]]
  var threshold = 0.7D

  def receive = {
    case card: RankingCard =>
      val existing = anomalies.getOrElse(card.keyNode, MutableMap.empty[String, (Double, Set[UUID])])

      if (card.suspicionScore < threshold) existing -= card.name
      else existing += (card.name -> (card.suspicionScore, card.subgraph))

      if (existing.isEmpty) anomalies -= card.keyNode
      else anomalies += (card.keyNode -> existing)

    case QueryAnomaly(uuid) =>
      sender() ! anomalies.getOrElse(uuid, MutableMap.empty[String, (Double, Set[UUID])])

    case GetRankedAnomalies(topK) =>
      sender() ! anomalies.toList.sortBy(_._2.values.map(_._1).sum)(Ordering.by[Double,Double](identity).reverse).take(topK)

    case SetThreshold(limit) => threshold = limit
  }
}

case class SetThreshold(threshold: Double)
case class QueryAnomaly(uuid: UUID)
case class GetRankedAnomalies(topK: Int = Int.MaxValue)


object ProdRoutes {

  val serveStaticFilesRoute =
    path("") {
      getFromResource("web/index.html")
    } ~
    path("graph") {
      getFromResource("web/graph.html")
    } ~
    pathPrefix("") {
      getFromResourceDirectory("web")
    }


  implicit val timeout = Timeout(20 seconds)

  def completedQuery[T <: VertexOrEdge](query: RestQuery, dbActor: ActorRef)(implicit ec: ExecutionContext) = {
    val qType = query match {
      case _: NodeQuery   => "node"
      case _: EdgeQuery   => "edge"
      case _: StringQuery => "generic"
    }
    Application.debug(s"Got $qType query: ${query.query}")
    val futureResponse = (dbActor ? query).mapTo[Try[String]].map { s =>
      Application.debug("returning...")
      val toReturn = s match {
        case Success(json) => json
        case Failure(e) =>
          Application.debug(e.getMessage)
          "\"" + e.getMessage + "\""
      }
      HttpEntity(ContentTypes.`application/json`, toReturn)
    }
    complete(futureResponse)
  }


  def mainRoute(dbActor: ActorRef, anomalyActor: ActorRef, statusActor: ActorRef)(implicit ec: ExecutionContext) =
    get {
      pathPrefix("ranked") {
        complete((anomalyActor ? GetRankedAnomalies()).mapTo[List[_]].map(_.mkString("\n\n")))
      } ~
      pathPrefix("status") {
        complete(
          "todo"
//          Map(
//            "nodes" -> statusList.map(s => UINode(s.from.toString, s.from.path.elements.last, s.measurements.mapValues(_.toString).toList.map(t => s"${t._1}: ${t._2}").mkString("<br />"))).map(ApiJsonProtocol.c.write),
//            "edges" -> statusList.flatMap(s => s.subscribers.map(x => UIEdge(s.from.toString, x.toString, ""))).map(ApiJsonProtocol.d.write)
//          )
        )
      } ~
      pathPrefix("query") {
          pathPrefix("nodes") {
            path(RemainingPath) { queryString =>
              completedQuery(NodeQuery(queryString.toString), dbActor)
            }
          } ~
          pathPrefix("edges") {
            path(RemainingPath) { queryString =>
              completedQuery(EdgeQuery(queryString.toString), dbActor)
            }
          } ~
          pathPrefix("generic") {
            path(RemainingPath) { queryString =>
              completedQuery(StringQuery(queryString.toString), dbActor)
            }
          }
      } ~
      serveStaticFilesRoute
    } ~
    post {
      pathPrefix("query") {
//        path("anomalyScores") {
//          formField('uuids) { commaSeparatedUuidString =>
//            val uuids = commaSeparatedUuidString.split(",").map(s => UUID.fromString(s.trim))
//            val scores = uuids.map(u => u.toString -> anomalies.mapValues(_.suspicionScore).getOrElse(Set(u), 0D))   // TODO: revisit this to return multiple anomaly scores per node!
//            complete(HttpEntity(ContentType(MediaTypes.`application/json`), scores.map{ case (k, v) => s""""$k":$v""" }.mkString("{",",","}")))  // lazy cheating.
//          }
//        } ~
        path("nodes") {
          formField('query) { queryString =>
            completedQuery(NodeQuery(queryString), dbActor)
          }
        } ~
        path("edges") {
          formField('query) { queryString =>
            completedQuery(EdgeQuery(queryString), dbActor)
          } ~
            formField('nodes) { nodeListString =>
              println(s"getting edges for nodes: $nodeListString")
              val idList = nodeListString.split(",").map(_.toInt)
              val futureResponse = (dbActor ? EdgesForNodes(idList)).mapTo[Try[String]].map { s =>
                println("returning...")
                HttpEntity(ContentTypes.`application/json`, s.get)
              }
              complete(futureResponse)
            }
        } ~
        path("generic") {
          formField('query) { queryString =>
            completedQuery(StringQuery(queryString), dbActor)
          }
        }
      }
    }

}
