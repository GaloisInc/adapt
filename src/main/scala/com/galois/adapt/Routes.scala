package com.galois.adapt

import akka.actor.ActorSystem
import spray.json._
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity, MediaTypes}
import akka.http.scaladsl.server.Directives.{complete, formField, formFieldMap, get, getFromResource, getFromResourceDirectory, path, pathPrefix, post}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshaller
//import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.PredefinedFromStringUnmarshallers._
import akka.http.scaladsl.marshalling.PredefinedToEntityMarshallers._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshaller._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import org.apache.tinkerpop.gremlin.structure.{Element => VertexOrEdge}
import akka.stream.Materializer
import com.bbn.tc.schema.avro.TheiaQueryType
import com.typesafe.config.ConfigFactory
import spray.json.{JsString, JsValue}
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


  val validRating = Unmarshaller.strict[String, Int] {
    case i if Set("0","1","2","3","4","5") contains i => i.toInt
    case i => throw new IllegalArgumentException(s"'$i' is not a valid rating.")
  }


  def mainRoute(dbActor: ActorRef, statusActor: ActorRef, ppmActor: ActorRef)(implicit ec: ExecutionContext, system: ActorSystem, materializer: Materializer) =
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
            path("listTrees") {
              complete(
                (ppmActor ? ListPpmTrees).mapTo[PpmTreeNames].map(_.names)
              )
            } ~
            path(Segment) { treeName =>
              parameter('query.as[String].?, 'namespace ? "adapt") { (queryString, namespace) =>
                val query = queryString.map(_.split("∫", -1)).getOrElse(Array.empty[String]).toList
                import ApiJsonProtocol._
                complete(
                  (ppmActor ? PpmTreeAlarmQuery(treeName, query, namespace.toLowerCase)).mapTo[PpmTreeAlarmResult]
                    .map(t => List(UiTreeFolder(treeName, true, UiDataContainer.empty, t.toUiTree.toSet)))
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
          pathPrefix("ppm") {
            path(Segment / "setRating") { treeName =>
              parameters('query.as[String], 'rating.as(validRating), 'namespace ? "adapt") { (queryString, rating, namespace) =>
                complete {
                  val query = queryString.split("∫",-1).toList
                  (ppmActor ? SetPpmRatings(treeName, List(query), rating, namespace.toLowerCase)).mapTo[Option[List[Boolean]]].map {
                    case Some(l) if l.forall(x => x) => StatusCodes.Created -> s"Rating for $queryString set to: $rating"
                    case Some(l) => StatusCodes.NotFound -> s"Could not find key for $queryString"
                    case None => StatusCodes.BadRequest -> s"Could not find tree: $treeName"
                  }
                }
              }
            } ~
            path("setRatings") {
              formFields('pathsPerTree.as[Map[String, List[String]]], 'rating.as(validRating), 'namespace ? "adapt") { (pathsPerTree, rating, namespace) =>

                val perTreeResultFutures = pathsPerTree.map {
                  case (treeName, paths) =>
                    val parsedPaths = paths.map(p => p.split("∫", -1).toList)
                    (ppmActor ? SetPpmRatings(treeName, parsedPaths, rating, namespace.toLowerCase)).mapTo[Option[List[Boolean]]]
                      .map(v => treeName -> v)
                }.toList
                val perTreeResultFuture = Future.sequence(perTreeResultFutures)

                complete{
                  perTreeResultFuture.map {
                    case l if l.forall(_._2.exists(_.forall(r => r))) => StatusCodes.Created -> s"Rating for all paths succeeded."
                    case l if l.exists(_._2.isEmpty) => StatusCodes.BadRequest -> s"""Could not find trees: ${l.collect{ case x if x._2.isEmpty => x._1}.mkString(" & ")} Ratings for other trees might have succeeded...?  ¯\\_(ツ)_/¯"""
                    case l if l.exists(_._2.exists(_.exists(r => ! r))) => StatusCodes.UnprocessableEntity -> s"Some ratings were not set: ${l.toMap}"
                    case l => StatusCodes.ImATeapot -> s" ¯\\_(ツ)_/¯ \n$l"
                  }
                }
              }
            }
          } ~
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
                  // TODO: Come on... fix this.
                  (ppmActor ? q).mapTo[Future[String]].flatMap(identity)
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

