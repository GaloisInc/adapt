package com.galois.adapt

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.http.scaladsl.unmarshalling.PredefinedFromStringUnmarshallers._
import akka.stream.Materializer
import akka.pattern.ask
import akka.util.Timeout
import spray.json.{DefaultJsonProtocol, JsArray}
import edazdarevic.commons.net.CIDRUtils
import org.apache.tinkerpop.gremlin.structure.Vertex

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


object PolicyEnforcementDemo extends SprayJsonSupport with DefaultJsonProtocol {

  case class ValueName(name: String)
  implicit val simpleResponseFormat = jsonFormat1(ValueName)

  val validIpAddress = Unmarshaller.strict[String, String] { string =>
    string.split("\\.") match {
      case l if l.length == 4 && Try(require(l.map(_.toInt).forall(i => i >= 0 && i <= 255))).isSuccess => string
      case _ => throw new IllegalArgumentException(s"'$string' is not a valid IPv4 address.")
    }
  }

  val validUri = Unmarshaller.strict[String, String] {
    case uri if uri.startsWith("http://") || uri.startsWith("https://") => uri  // TODO: Something smarter here?!
    case invalid => throw new IllegalArgumentException(s"'$invalid' is not a valid URI.")
  }


  def route(dbActor: ActorRef)(implicit ec: ExecutionContext) =
    get {
      path("ping") {
        complete(
          ValueName("ADAPT")
        )
      } ~
      path("checkPolicy") {

        implicit val timeout = Timeout(60 seconds)

        parameters('clientIp.as(validIpAddress), 'clientPort.as[Int], 'serverIp.as(validIpAddress), 'serverPort.as[Int], 'timestamp.as[Long], 'requestId.as[Int], 'responseUri/*.as(validUri)*/) {
          (clientIp, clientPort, serverIp, serverPort, timestamp, requestId, responseUri) =>
          parameters('policy ! 1, 'permissionType.as[String], 'permissionList.as(CsvSeq[String])) { (permissionType, permissionList) =>
            // https://git.tc.bbn.com/bbn/tc-policy-enforcement/wikis/Policy_User
            complete {
              // TODO
              val a = (dbActor ? NodeQuery("g.V().hasLabel('AdmNetFlowObject')")).mapTo[Future[Try[JsArray]]].flatMap(identity).map(_.get)
//              (dbActor ? NodeQuery("g.V().hasLabel('AdmNetFlowObject')", false)).mapTo[Future[Try[Stream[Vertex]]]].flatMap(identity).map(_.get)
              a.map(arr =>
                StatusCodes.Accepted -> arr.toString // "Started the policy check process, will respond later"
              )
            }
          } ~
          parameters('policy ! 2, 'restrictedHost.as[String]) { restrictedHost => // `restrictedHost` is a CIDR range or maybe a single IP...?
            // https://git.tc.bbn.com/bbn/tc-policy-enforcement/wikis/Policy_Communication
            val cidrRange = if (restrictedHost.contains("/")) new CIDRUtils(restrictedHost) else new CIDRUtils(s"$restrictedHost/32")
            // TODO: single IPv4? single IPv6?
            complete {
              // TODO
              StatusCodes.Accepted -> "Started the policy check process, will respond later"
            }
          } ~
          parameters('policy ! 3, 'keyboardAction.as[Boolean], 'guiEventAction.as[Boolean]) { (keyboardAction, guiEventAction) =>
            // https://git.tc.bbn.com/bbn/tc-policy-enforcement/wikis/Policy_UIAction
            complete {
              // TODO
              StatusCodes.Accepted -> "Started the policy check process, will respond later"
            }
          } ~
          parameters('policy ! 4) {
            // https://git.tc.bbn.com/bbn/tc-policy-enforcement/wikis/Policy_NetData
            complete {
              // TODO
              StatusCodes.Accepted -> "Started the policy check process, will respond later"
            }
          }
        }
      } ~
      path("status") {
        parameter('requestId.as[Int]) { requestId =>
          complete(
            // TODO
            if (requestId == 1) StatusCodes.Accepted -> "Working on it"
            else StatusCodes.NotFound -> s"Don't have an active request for that Id: $requestId"
          )
        }
      }
    }


  def policyResponse(responseUri: String)(implicit system: ActorSystem, materializer: Materializer): Future[HttpResponse] = {
    implicit val ec = system.dispatcher
    val body = HttpEntity("{ message: \"[Optional debugging/explanation information] CDM Record {EVENT_READ...}\" }")
    val result = 200
//    Passed policy check: 200
//    Failed policy check: 400
//    Error: 500

//    Marshal(ValueName("your asynchronous results")).to[RequestEntity].flatMap( reqEntity =>
    val responseF = Http().singleRequest(HttpRequest(HttpMethods.POST, uri = responseUri + s"?result=$result", entity = body))
//    )

    responseF onComplete {
      case Success(r) => println(r)
      case Failure(e) => e.printStackTrace()
    }

    responseF
  }


}