package com.galois.adapt

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._
import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity, MediaTypes}
import akka.http.scaladsl.server.Directives.{complete, formField, formFieldMap, get, getFromResource, getFromResourceDirectory, path, pathPrefix, post}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import org.apache.tinkerpop.gremlin.structure.{Element => VertexOrEdge}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.PredefinedFromStringUnmarshallers._
import akka.stream.Materializer
import com.bbn.tc.schema.avro.TheiaQueryType
import com.typesafe.config.ConfigFactory
import spray.json.{JsString, JsValue}
//import akka.http.scaladsl.marshalling._
import java.util.UUID
import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern.ask
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import akka.http.scaladsl.model.headers._
//import ApiJsonProtocol._


object Routes {

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

  def queryResult[T <: VertexOrEdge](query: RestQuery, dbActor: ActorRef)(implicit ec: ExecutionContext) = (dbActor ? query)
    .mapTo[Future[Try[JsValue]]].flatMap(identity).map(_.get)


  def mainRoute(dbActor: ActorRef, anomalyActor: ActorRef, statusActor: ActorRef, ppmActor: ActorRef)(implicit ec: ExecutionContext, system: ActorSystem, materializer: Materializer) =
    respondWithHeader(`Access-Control-Allow-Origin`(HttpOriginRange.*)) {
      PolicyEnforcementDemo.route(dbActor) ~
      get {
        pathPrefix("api") {
          pathPrefix("status") {
            import ApiJsonProtocol.statusReport
            complete(
              (statusActor ? GetStats).mapTo[StatusReport]
            )
          } ~
          pathPrefix("ppm") {
            path(Segment) { treeName =>
              parameter('query.as(CsvSeq[String]).?) { querySeq =>
                val query = querySeq.getOrElse(Seq.empty).toList
                import ApiJsonProtocol._
                complete(
                  (ppmActor ? PpmTreeQuery(treeName, query)).mapTo[PpmTreeResult].map(_.toUiTree)
                )
              }
            }
          }
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
                  queryResult(CypherQuery(queryString.toString), dbActor)
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
                      case "forward" | "forwards" => TheiaQueryType.FORWARD
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
                queryResult(CypherQuery(queryString), dbActor)
              )
            }
          }
        }
      }
    }
}

