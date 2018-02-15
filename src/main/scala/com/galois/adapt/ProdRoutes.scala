package com.galois.adapt

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives.{complete, formField, formFieldMap, get, getFromResource, getFromResourceDirectory, path, pathPrefix, post, _}
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.bbn.tc.schema.avro.TheiaQueryType
import com.typesafe.config.ConfigFactory
import org.apache.tinkerpop.gremlin.structure.{Element => VertexOrEdge}
import spray.json.JsValue

import scala.concurrent.{ExecutionContext, Future}
//import akka.http.scaladsl.marshalling._
import java.util.UUID

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try


object ProdRoutes {

  val config = ConfigFactory.load()

  val serveStaticFilesRoute =
    path("") {
      getFromResource("web/graph.html")
    } ~
    path("rank") {
      getFromResource("web/ranking.html")
    } ~
    pathPrefix("") {
      getFromResourceDirectory("web")
    }

  implicit val timeout = Timeout(config.getInt("adapt.runtime.apitimeout") seconds)

  // This lets us stream out JSON values
  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  def queryStreamingResult[T <: VertexOrEdge](query: RestQuery, dbActor: ActorRef)
                                    (implicit ec: ExecutionContext): Future[Source[JsValue, NotUsed]] = {
    for {
      futTryIterator <- (dbActor ? query).mapTo[Future[Try[Iterator[JsValue]]]]
      tryIterator <- futTryIterator
      i = tryIterator.get
    } yield Source.fromIterator[JsValue](() => i)
  }


  def queryResult[T <: VertexOrEdge](query: RestQuery, dbActor: ActorRef)
                                    (implicit ec: ExecutionContext): Future[JsValue] = for {
    futTryResult <- (dbActor ? query).mapTo[Future[Try[JsValue]]]
    tryResult <- futTryResult
    result: JsValue = tryResult.get
  } yield result


  def mainRoute(dbActor: ActorRef, anomalyActor: ActorRef, statusActor: ActorRef)(implicit ec: ExecutionContext, system: ActorSystem, materializer: Materializer) =
    PolicyEnforcementDemo.route(dbActor) ~
    get {
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
            complete(
              queryResult(NodeQuery(queryString.toString), dbActor)
            )
          }
        } ~
        pathPrefix("edges") {
          path(RemainingPath) { queryString =>
            complete(
              queryResult(EdgeQuery(queryString.toString), dbActor)
            )
          }
        } ~
        pathPrefix("generic") {
          path(RemainingPath) { queryString =>
            complete(
              queryResult(StringQuery(queryString.toString), dbActor)
            )
          }
        } ~
        pathPrefix("json") {
          path(RemainingPath) { queryString =>
            complete(
              queryResult(StringQuery(queryString.toString, true), dbActor)
            )
          }
        } ~
        pathPrefix("cypher") {
          path(RemainingPath) { queryString =>
            complete(
              queryStreamingResult(CypherQuery(queryString.toString), dbActor)
            )
          }
        }
      } ~
      serveStaticFilesRoute
    } ~
    post {
      pathPrefix("api") {
        pathPrefix("makeTheiaQuery") {
          formFieldMap { fields =>
            complete {
              Try(
                MakeTheiaQuery(
                  fields("type").toLowerCase match {
                    case "backward" | "backwards" => TheiaQueryType.BACKWARD
                    case "forward"  | "forwards"  => TheiaQueryType.FORWARD
                    case "point_to_point" | "pointtopoint" | "ptp" => TheiaQueryType.POINT_TO_POINT
                  },
                  fields.get("sourceId").map(UUID.fromString),
                  fields.get("sinkId").map(UUID.fromString),
                  fields.get("startTimestamp").map(_.toLong),
                  fields.get("endTimestamp").map(_.toLong)
                )
              ).map(q =>
                (anomalyActor ? q).mapTo[Future[String]].flatMap(identity)
              )
            }
          }
        }
      } ~
      pathPrefix("query") {
        path("nodes") {
          formField('query) { queryString =>
            complete(
              queryResult(NodeQuery(queryString), dbActor)
            )
          }
        } ~
        path("edges") {
          formField('query) { queryString =>
            complete(
              queryResult(EdgeQuery(queryString), dbActor)
            )
          }
        } ~
        path("generic") {
          formField('query) { queryString =>
            complete(
              queryResult(StringQuery(queryString), dbActor)
            )
          }
        } ~
        path("json") {
          formField('query) { queryString =>
            complete(
              queryResult(StringQuery(queryString, true), dbActor)
            )
          }
        } ~
        path("cypher") {
          formField('query) { queryString =>
            complete(
              queryStreamingResult(CypherQuery(queryString), dbActor)
            )
          }
        }
      }
    }
}

