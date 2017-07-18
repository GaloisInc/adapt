package com.galois.adapt

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._

import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity, MediaTypes}
import akka.http.scaladsl.server.Directives.{complete, formField, formFieldMap, get, getFromResource, getFromResourceDirectory, path, pathPrefix, post}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import org.apache.tinkerpop.gremlin.structure.{Element => VertexOrEdge}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
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


object ProdRoutes {

  val config = ConfigFactory.load()

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


  implicit val timeout = Timeout(config.getInt("adapt.apitimeout") seconds)

  def queryResult[T <: VertexOrEdge](query: RestQuery, dbActor: ActorRef)(implicit ec: ExecutionContext) = {
     (dbActor ? query)
      .mapTo[Future[Try[JsValue]]].flatMap(identity).map { s =>
      s match {
        case Success(json) => json
        case Failure(e) =>
          println(e.getMessage)
          JsString("\"" + e.getMessage + "\"")
      }
    }
  }


  def mainRoute(dbActor: ActorRef, anomalyActor: ActorRef, statusActor: ActorRef)(implicit ec: ExecutionContext) =
    get {
      pathPrefix("ranked") {
        path(RemainingPath) { count =>
          val limit = Try(count.toString.toInt).getOrElse(Int.MaxValue)
          complete(
            (anomalyActor ? GetRankedAnomalies(limit))
              .mapTo[List[(UUID, Map[String, (Double, Set[UUID])])]]
              .map(convertToJsonTheOtherHardWay)
          )
        }
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
      pathPrefix("api") {
        pathPrefix("weights") {
          complete{
            val mapF = (anomalyActor ? GetWeights).mapTo[Map[String, Double]]
            mapF.map{ x =>
              val s = x.map{ case (k,v) => s""""${k.trim}": $v"""}.mkString("{",",","}")
              HttpEntity(ContentTypes.`application/json`, s)
            }
          }
        } ~
        pathPrefix("threshold") {
          complete{
            (anomalyActor ? GetThreshold).mapTo[Double].map(_.toString)
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
        }
      } ~
      serveStaticFilesRoute
    } ~
    post {
      pathPrefix("api") {
        pathPrefix("saveNotes") {
          formFieldMap { fields =>
            complete {
              val notes = fields.getOrElse("notes", "").replaceAll(""""""","").replaceAll("\\\\", "")
              val keyUuid = UUID.fromString(fields("keyUuid"))
              val rating = fields("rating").toInt
              val subgraph = fields("subgraph").split(",").filter(_.nonEmpty).map(s => UUID.fromString(s.trim())).toSet
              (anomalyActor ? SavedNotes(keyUuid, rating, notes, subgraph)).mapTo[Try[Unit]]
                .map(_.map(_ => "OK").getOrElse("FAILED"))
            }
          }
        } ~
        pathPrefix("getNotes") {
          formField('uuids) { uuids =>
            complete {
              val uuidSeq = if (uuids.isEmpty) Seq.empty[UUID] else uuids.split(",").map(UUID.fromString).toSeq
              val notesF = (anomalyActor ? GetNotes(uuidSeq)).mapTo[Seq[SavedNotes]]
              notesF.map{n =>
                val s = n.map(_.toJsonString).mkString("[",",","]")
                HttpEntity(ContentTypes.`application/json`, s)
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
                    case "forward"  | "forwards"   => TheiaQueryType.FORWARD
                    case "point_to_point" | "pointtopoint" => TheiaQueryType.POINT_TO_POINT
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
        } ~
        pathPrefix("weights") {
          formField('weights) { `k:v;k:v` =>
            complete {
              `k:v;k:v`.split(";").foreach{ `k:v` =>
                val pair = `k:v`.split(":")
                if (pair.length == 2) anomalyActor ! SetWeight(pair(0), pair(1).toDouble)
              }
//              redirect(Uri("/"), StatusCodes.TemporaryRedirect)
              "OK"
            }
          }
        } ~
        pathPrefix("threshold") {
          formField('threshold) { threshold =>
            complete {
              anomalyActor ! SetThreshold(threshold.toDouble)
              "OK"
            }
          }
        }
      } ~
      pathPrefix("query") {
        path("anomalyScores") {
          formField('uuids) { commaSeparatedUuidString =>
            val uuids = commaSeparatedUuidString.split(",").map(s => UUID.fromString(s.trim))
            val mapF = (anomalyActor ? QueryAnomalies(uuids)).mapTo[Map[UUID, Map[String, (Double, Set[UUID])]]]
            val f = mapF.map(anoms => convertToJsonTheHardWay(anoms) )
            complete(f.map(s => HttpEntity(ContentTypes.`application/json`, s)))
          }
        } ~
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
          } ~
          formField('nodes) { nodeListString =>
            println(s"getting edges for nodes: $nodeListString")
            val idList = nodeListString.split(",").map(_.toInt)
            val futureResponse = (dbActor ? EdgesForNodes(idList)).mapTo[Try[String]].map { s =>
              HttpEntity(ContentTypes.`application/json`, s.get)
            }
            complete(futureResponse)
          }
        } ~
        path("generic") {
          formField('query) { queryString =>
            complete(
              queryResult(StringQuery(queryString), dbActor)
            )
          }
        }
      }
    }


  def convertToJsonTheHardWay(anomalyMap: Map[UUID, Map[String, (Double, Set[UUID])]]): String =    // Bad programmer! You know better than this. You should be ashamed of yourself.
    anomalyMap.mapValues(inner => inner.mapValues(t => s"""{"score":${t._1},"subgraph":${t._2.toList.map(u => s""""$u"""").mkString("[",",","]")}}""").map(t => s""""${t._1}":${t._2}""").mkString("{",",","}")  ).map(t => s""""${t._1}":${t._2}""").mkString("{",",","}")

  def convertToJsonTheOtherHardWay(anomalyList: List[(UUID, Map[String, (Double, Set[UUID])])]): String = {   // Didn't I already tell you shouldn't be doing this...?!
    anomalyList.map(anom => s"""{"${anom._1}":${anom._2.map(x => s""""${x._1}":{"score":${x._2._1},"subgraph":${x._2._2.map(u => s""""$u"""").mkString("[",",","]")}}""").mkString("{",",","}")}}""").mkString("[",",","]")
  }

}

