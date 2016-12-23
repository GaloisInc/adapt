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

import scala.concurrent.ExecutionContext
import scala.util.Try
import scala.concurrent.duration._

object Routes {

  val workingDirectory = System.getProperty("user.dir")
  val resourcesDir = workingDirectory + "/src/main/resources/web"

  def getExtensions(fileName: String): String = {
    val index = fileName.lastIndexOf('.')
    if (index != 0) fileName.drop(index + 1) else ""
  }

  val serveStaticsFileRoute =
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

  def completedQuery[T <: VertexOrEdge](query: String, dbActor: ActorRef)(implicit ec: ExecutionContext) =
    complete {
      println(s"Got query: $query")
      (dbActor ? NodeQuery(query)).mapTo[Try[String]].map { s =>
        HttpEntity(ContentTypes.`application/json`, s.get)
      }
    }


  def mainRoute(dbActor: ActorRef)(implicit ec: ExecutionContext) =
    get {
      pathPrefix("query") {
        pathPrefix("nodes") {
          path(RemainingPath) { queryString =>
            completedQuery(queryString.toString, dbActor)
          }
        } ~
        path("edges") {
          complete("NOT IMPLEMENTED")
        }
      } ~ serveStaticsFileRoute
    } ~
    post {
      pathPrefix("query") {
        path("nodes") {
          formField('query) { queryString =>
            completedQuery(queryString, dbActor)
          }
        } ~
        path("edges") {
          complete("NOT IMPLEMENTED")
        }
      }
    }

}
