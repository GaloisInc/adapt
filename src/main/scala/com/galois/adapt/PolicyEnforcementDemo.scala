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

  def testIpAddress(ip: String): Option[String] = ip.split("\\.") match {
    case l if l.length == 4 && Try(require(l.map(_.toInt).forall(i => i >= 0 && i <= 255))).isSuccess => Some(ip)
    case _ => None
  }

  val validIpAddress = Unmarshaller.strict[String, String] { string =>
    testIpAddress(string) match {
      case Some(s) => s
      case _ => throw new IllegalArgumentException(s"'$string' is not a valid IPv4 address.")
    }
  }

  val validUri = Unmarshaller.strict[String, String] {
    case uri if uri.startsWith("http://") /*|| uri.startsWith("https://")*/ => uri  // TODO: Something smarter here?!
    case invalid => throw new IllegalArgumentException(s"'$invalid' is not a valid URI.")
  }

  val validPermissionType = Unmarshaller.strict[String, String] { string =>
    string.toLowerCase match {
      case "user" | "group" => string
      case _ => throw new IllegalArgumentException(s"'$string' is not a valid permissionType. It must be one of: [USER,GROUP]")
    }
  }

  type RequestId = Int
  var policyRequests = Map.empty[RequestId,Future[Boolean]]

  val validRequestId = Unmarshaller.strict[String,Int] { string =>
    Try(string.toInt) match {
      case Failure(e) => throw new IllegalArgumentException(s"'$string' is not a valid requestID. It must be an integer")
      case Success(i) if policyRequests.contains(i) => throw new IllegalArgumentException(
        s"Request ID: '$string' has already been used. You must begin a request with an ID which hasn't been used before." +
        s"\nNOTE: the previous value computed for this request ID ${if (policyRequests(i).isCompleted) "was: " + policyRequests(i).value.get.toString else "is not yet complete."}"
      )
      case Success(i) => i
    }
  }

//  def go(query: String, f: (List[String]) => (Int, Option[String]))


  def route(dbActor: ActorRef)(implicit ec: ExecutionContext, system: ActorSystem, materializer: Materializer, timeout: Timeout) = {
    get {
      path("ping") {
        complete(
          ValueName("ADAPT")
        )
      } ~
        path("checkPolicy") {
          parameters('clientIp.as(validIpAddress), 'clientPort.as[Int], 'serverIp.as(validIpAddress), 'serverPort.as[Int], 'timestamp.as[Long], 'requestId.as(validRequestId), 'responseUri.as(validUri)) {
            (clientIp, clientPort, serverIp, serverPort, timestamp, requestId, responseUri) =>
              parameters('policy ! 1, 'permissionType.as(validPermissionType) ? "USER", 'permissionList.as(CsvSeq[String]) ? collection.immutable.Seq[String]("darpa")) { (permissionType, permissionList) =>
                // https://git.tc.bbn.com/bbn/tc-policy-enforcement/wikis/Policy_User
                // BLOCK if the user originating the process that sent the HTTP requests is NOT an allowed user or in an allowed user group.
                complete {
                  val desiredProperty = if (permissionType.toLowerCase == "user") "username" else "groupId"
                  val query =
                    s"""g.V().hasLabel('AdmNetFlowObject').has('remoteAddress','$serverIp').has('remotePort',$serverPort)
                       |.inE('predicateObject','predicateObject2').outV()
                       |.outE('subject').inV()
                       |.outE('localPrincipal').inV()
                       |.dedup().values('$desiredProperty')
                     """.stripMargin.replaceAll("\n","")   // .has('eventType','EVENT_WRITE')

                  val resultFuture = (dbActor ? NodeQuery(query, shouldReturnJson = false)).mapTo[Future[Try[Stream[String]]]].flatMap(identity).map(_.get.toList).map {
                    case value :: Nil => permissionType.toLowerCase match {
                      case "user" =>
                        val result = permissionList.map(_.toLowerCase) contains value
                        if (result) returnPolicyResult(200, None, responseUri)
                        else returnPolicyResult(400, Some(s"username used to make the request was: $value  This query was testing for: ${permissionList.mkString(",")}"), responseUri)
                        result
                      case "group" | _ =>
                        val result = value.split(",").toSet[String].intersect(permissionList.toSet[String].map(_.toLowerCase)).nonEmpty
                        if (result) returnPolicyResult(200, None, responseUri)
                        else returnPolicyResult(400, Some(s"user's set of groupIds that made this request was: $value  This query was testing for: ${permissionList.mkString(",")}"), responseUri)
                        result
                    }
                    case x =>
                      returnPolicyResult(500, Some(s"Found ${if (x.isEmpty) "no" else "multiple"} Principal nodes with a username for the process(es) communicating with that netflow."), responseUri)
                      false   // TODO: Should this use a Try[Boolean]?
                  }.recover {
                    case e: Throwable =>
                      returnPolicyResult(500, Some(s"An error occurred during the processing of the request: ${e.getMessage}"), responseUri)
                      throw e
                  }
                  policyRequests = policyRequests + (requestId -> resultFuture)
                  StatusCodes.Accepted -> "Started the policy check process, will respond later"
                }
              } ~
                parameters('policy ! 2, 'restrictedHost.as(validIpAddress)) { restrictedHost => // `restrictedHost` is a CIDR range or maybe a single IP...?
                  // https://git.tc.bbn.com/bbn/tc-policy-enforcement/wikis/Policy_Communication
                  complete {
                    val disallowedRange = if (restrictedHost.contains("/")) new CIDRUtils(restrictedHost) else new CIDRUtils(s"$restrictedHost/32")

                    val query =  // Get all netflows associated with the process of the netflow in question.
                      s"""g.V().hasLabel('AdmNetFlowObject').has('remoteAddress','$serverIp').has('remotePort',$serverPort)
                         |.in('predicateObject','predicateObject2')
                         |.out('subject')
                         |.in('subject').hasLabel('AdmEvent').out('predicateObject','predicateObject2').hasLabel('AdmNetFlowObject')
                         |.dedup().values('remoteAddress')
                       """.stripMargin.replaceAll("\n","")

                    val doesViolateF = (dbActor ? NodeQuery(query, shouldReturnJson = false)).mapTo[Future[Try[Stream[String]]]].flatMap(identity).map(_.get.toList).map {
                      case Nil => List.empty[(String,Boolean)] // No matching netflows. ——Note: this should never be possible, because you should always get the originating netflow.
                      case l => l.flatMap { string =>
                        val validIpOpt = testIpAddress(string)
                        val doesViolateOpt = validIpOpt.flatMap(ip => Try(disallowedRange.isInRange(ip)).toOption)
                        doesViolateOpt.map(r => string -> r)
                      }
                    }
                    doesViolateF.map{ results =>
                      if (results.forall(t => ! t._2))
                        returnPolicyResult(200, None, responseUri) // should allow
                      else
                        returnPolicyResult(400, Some(s"The following violating addresses were also contacted by this process: ${results.collect{case t if t._2 => t._1}.mkString(", ")}"), responseUri)
                    }.recover {
                      case e: Throwable =>
                        returnPolicyResult(500, Some(s"An error occurred during the processing of the request: ${e.getMessage}"), responseUri)
                        throw e
                    }
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
              if (policyRequests contains requestId) {
                StatusCodes.Accepted -> (if (policyRequests(requestId).isCompleted) policyRequests(requestId).value.get.toString else s"Request #$requestId is not yet complete.")
              } else StatusCodes.NotFound -> s"We do not have an active request for that Id: $requestId"
            )
          }
        }
    } ~
    post {
      path("testResponder") {
        parameter('result) { result =>
          entity(as[String]) { entity =>
            complete {
              println(s"RECEIVED: __${result}__  $entity")
              StatusCodes.OK -> entity
            }
          }
        }
      }
    }
  }


  def returnPolicyResult(responseCode: Int, message: Option[String], responseUri: String)(implicit system: ActorSystem, materializer: Materializer): Future[HttpResponse] = {
    implicit val ec = system.dispatcher
    val body = message.map(m => HttpEntity(m)).getOrElse(HttpEntity.Empty)
//    Response codes:
//    Passed policy check: 200
//    Failed policy check: 400
//    Error: 500

//    Marshal(ValueName("your asynchronous results")).to[RequestEntity].flatMap( reqEntity =>
    val responseF = Http().singleRequest(HttpRequest(HttpMethods.POST, uri = responseUri + s"?result=$responseCode", entity = body))
//    )

    responseF onComplete {
      case Success(r) => println(s"Result from _sending_ the response: $r")
      case Failure(e) => e.printStackTrace()
    }

    responseF
  }


}