package com.galois.adapt

import java.util.UUID

import akka.actor.ActorRef
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import org.apache.tinkerpop.gremlin.structure.{Element => VertexOrEdge}
import ApiJsonProtocol._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._
import scala.language.postfixOps

object Routes {

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
      case _: NodeQuery => "node"
      case _: EdgeQuery => "edge"
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


  def mainRoute(dbActor: ActorRef, rankedList: => List[(String,Set[UUID],Float)], statusList: => List[StatusReport])(implicit ec: ExecutionContext) =
    get {
      pathPrefix("query") {
        pathPrefix("ranked") {
          complete(rankedList.toString)
        } ~
        pathPrefix("status") {
          complete(
            Map(
            "nodes" -> statusList.map(s => UINode(s.from.toString, s.from.path.elements.last, s.measurements.mapValues(_.toString).toList.map(t => s"${t._1}: ${t._2}").mkString("<br />"))).map(ApiJsonProtocol.c.write),
            "edges" -> statusList.flatMap(s => s.subscribers.map(x => UIEdge(s.from.toString, x.toString, ""))).map(ApiJsonProtocol.d.write)
          )
          )
        } ~
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


case class UIEdge(from: String, to: String, label: String)
case class UINode(id: String, label: String, title: String)