package com.galois.adapt

import java.nio.file.{Files, Paths}

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import com.galois.adapt.ApiJsonProtocol._
import akka.pattern.ask
import akka.util.Timeout
import org.apache.tinkerpop.gremlin.structure.{Vertex, Element => VertexOrEdge}

import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Try, Success, Failure}
import scala.concurrent.duration._

import scala.language.postfixOps

object Routes {

  val workingDirectory = System.getProperty("user.dir")
  val resourcesDir = workingDirectory + "/src/main/resources/web"

  def getExtensions(fileName: String): String = {
    val index = fileName.lastIndexOf('.')
    if (index != 0) fileName.drop(index + 1) else ""
  }

  val serveStaticFilesRoute =
    entity(as[HttpRequest]) { requestData =>
      complete {
        val fullPath = requestData.uri.path.toString match {
          case "/" | ""=> Paths.get(resourcesDir + "/index.html")
          case _ => Paths.get(resourcesDir +  requestData.uri.path.toString)
        }

        val ext = getExtensions(fullPath.getFileName.toString)
        val mediaType = MediaTypes.forExtensionOption(ext).getOrElse(MediaTypes.`text/plain`)
        val c : ContentType = ContentType(mediaType, () => HttpCharsets.`UTF-8`)
        val byteArray = Files.readAllBytes(fullPath)

        HttpResponse(OK, entity = HttpEntity(c, byteArray))
      }
    }


  implicit val timeout = Timeout(10 seconds)

  def completedQuery[T <: VertexOrEdge](query: RestQuery, queryActor: ActorRef)(implicit ec: ExecutionContext) = {
    val qType = query match {
      case _: NodeQuery => "node"
      case _: EdgeQuery => "edge"
      case _: StringQuery => "generic"
    }
    
    println(s"Got $qType query: ${query.query}")
    val futureResponse = (queryActor ? query).mapTo[Try[String]].map { s =>
      println("returning...")
      
      val toReturn = s match {
        case Success(json) => json
        case Failure(e) => Application.debug(e.getMessage); ("\"" + e.getMessage + "\"")
      }
      HttpEntity(ContentTypes.`application/json`, toReturn)
    }
    complete(futureResponse)
  }


  def mainRoute(queryActor: ActorRef)(implicit ec: ExecutionContext) =
    get {
      pathPrefix("query") {
        pathPrefix("nodes") {
          path(RemainingPath) { queryString =>
            completedQuery(NodeQuery(queryString.toString), queryActor)
          }
        } ~
        pathPrefix("edges") {
          path(RemainingPath) { queryString =>
            completedQuery(EdgeQuery(queryString.toString), queryActor)
          }
        } ~
        pathPrefix("generic") {
          path(RemainingPath) { queryString =>
            completedQuery(StringQuery(queryString.toString), queryActor)
          }
        }
      } ~
      serveStaticFilesRoute
    } ~
    post {
      pathPrefix("query") {
        path("nodes") {
          formField('query) { queryString =>
            completedQuery(NodeQuery(queryString), queryActor)
          }
        } ~
        path("edges") {
          formField('query) { queryString =>
            completedQuery(EdgeQuery(queryString), queryActor)
          } ~
          formField('nodes) { nodeListString =>
            println(s"getting edges for nodes: $nodeListString")
            val idList = nodeListString.split(",").map(_.toInt)
            val futureResponse = (queryActor ? EdgesForNodes(idList)).mapTo[Try[String]].map { s =>
              println("returning...")
              HttpEntity(ContentTypes.`application/json`, s.get)
            }
            complete(futureResponse)
          }
        } ~
        path("generic") {
          formField('query) { queryString =>
            completedQuery(StringQuery(queryString), queryActor)
          }
        }
      }
    }

}
