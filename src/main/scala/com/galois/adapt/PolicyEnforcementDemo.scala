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
import spray.json.DefaultJsonProtocol
import edazdarevic.commons.net.CIDRUtils
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
  var policyRequests = Map.empty[RequestId,Future[(Int, Option[String])]]

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
              complete {
                answerPolicy1(permissionType, permissionList, serverIp, serverPort, responseUri, requestId, dbActor)
                StatusCodes.Accepted -> "Started the policy check process, will respond later"
              }
            } ~
            parameters('policy ! 2, 'restrictedHost.as(validIpAddress)) { restrictedHost => // `restrictedHost` is a CIDR range or maybe a single IP...?
              complete {
                answerPolicy2(restrictedHost, serverIp, serverPort, responseUri, requestId, dbActor)
                StatusCodes.Accepted -> "Started the policy check process, will respond later"
              }
            } ~
            parameters('policy ! 3, 'keyboardAction.as[Boolean], 'guiEventAction.as[Boolean], 'requestId.as(validRequestId), 'responseUri.as(validUri)) { (keyboardAction, guiEventAction, requestId, responseUri) =>
              complete {
                answerPolicy3(responseUri, requestId, dbActor)
                StatusCodes.NotImplemented -> "Not implemented" //"Started the policy check process, will respond later"
              }
            } ~
            parameters('policy ! 4, 'fileName.as[String], 'requestId.as(validRequestId), 'responseUri.as(validUri)) { (fileName, requestId, responseUri) =>
              complete {
                answerPolicy4(fileName, responseUri, requestId, dbActor)
                StatusCodes.NotImplemented -> "Not implemented" //"Started the policy check process, will respond later"
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



  def answerPolicy1(permissionType: String, permissionList: Seq[String], serverIp: String, serverPort: Int, responseUri: String, requestId: Int, dbActor: ActorRef)
    (implicit timeout: Timeout, ec: ExecutionContext, system: ActorSystem, materializer: Materializer): Unit = {
    // https://git.tc.bbn.com/bbn/tc-policy-enforcement/wikis/Policy_User
    // BLOCK if the user originating the process that sent the HTTP requests is NOT an allowed user or in an allowed user group.
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
          if (result) {
            returnPolicyResult(200, None, responseUri)
            200 -> None
          } else {
            val message = Some(s"username used to make the request was: $value  This query was testing for: ${permissionList.mkString(",")}")
            returnPolicyResult(400, message, responseUri)
            400 -> message
          }
        case "group" | _ =>
          val result = value.split(",").toSet[String].intersect(permissionList.toSet[String].map(_.toLowerCase)).nonEmpty
          if (result) {
            returnPolicyResult(200, None, responseUri)
            200 -> None
          }
          else {
            val message = Some(s"user's set of groupIds that made this request was: $value  This query was testing for: ${permissionList.mkString(",")}")
            returnPolicyResult(400, message, responseUri)
            400 -> message
          }
      }
      case x =>
        val message = Some(s"Found ${if (x.isEmpty) "no" else "multiple"} Principal nodes with a username for the process(es) communicating with that netflow.")
        returnPolicyResult(500, message, responseUri)
        500 -> message
    }.recover {
      case e: Throwable =>
        val message = Some(s"An error occurred during the processing of the request: ${e.getMessage}")
        returnPolicyResult(500, message, responseUri)
        500 -> message
    }
    policyRequests = policyRequests + (requestId -> resultFuture)
  }


  def answerPolicy2(restrictedHost: String, serverIp: String, serverPort: Int, responseUri: String, requestId: Int, dbActor: ActorRef)
    (implicit timeout: Timeout, ec: ExecutionContext, system: ActorSystem, materializer: Materializer): Unit = {
    // https://git.tc.bbn.com/bbn/tc-policy-enforcement/wikis/Policy_Communication

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
    val resultFuture: Future[(Int, Option[String])] = doesViolateF.map { results =>
      if (results.forall(t => ! t._2)) {
        val result = 200
        returnPolicyResult(result, None, responseUri) // should allow
        result -> None
      }
      else {
        val result = 400
        val message = Some(s"The following violating addresses were also contacted by this process: ${results.collect{case t if t._2 => t._1}.mkString(", ")}")
        returnPolicyResult(result, message, responseUri)
        result -> message
      }
    }.recover {
      case e: Throwable =>
        val result = 500
        val message = Some(s"An error occurred during the processing of the request: ${e.getMessage}")
        returnPolicyResult(result, message, responseUri)
        result -> message
    }
    policyRequests = policyRequests + (requestId -> resultFuture)
  }


  def answerPolicy3(responseUri: String, requestId: Int, dbActor: ActorRef)(implicit system: ActorSystem, materializer: Materializer): Unit = {
    // https://git.tc.bbn.com/bbn/tc-policy-enforcement/wikis/Policy_UIAction
    // TODO
    implicit val ec = system.dispatcher

    def nicholesQuery(whatDoYouNeed: String): Future[Option[String]] = ???

    val resultFuture = nicholesQuery("I don't know").map{
      case Some(s) =>
        val result: Int = ??? //400
        returnPolicyResult(result, Some(s), responseUri)
        result -> Some(s)
      case None =>
        val result: Int = ??? //200
        val messageOpt: Option[String] = None
        returnPolicyResult(result, messageOpt, responseUri)
        result -> messageOpt
    }

    policyRequests = policyRequests + (requestId -> resultFuture)
  }


  def answerPolicy4(fileName: String, responseUri: String, requestId: Int, dbActor: ActorRef)(implicit system: ActorSystem, materializer: Materializer): Unit = {
    // https://git.tc.bbn.com/bbn/tc-policy-enforcement/wikis/Policy_NetData
    // https://git.tc.bbn.com/bbn/tc-policy-enforcement/wikis/Policy4V1
    // TODO
    implicit val ec = system.dispatcher

    def alecsQuery(fileName: String): Future[Option[String]] = ???

    val resultFuture = alecsQuery(fileName).map{
      case Some(s) =>
        val result = 400
        returnPolicyResult(result, Some(s), responseUri)
        result -> Some(s)
      case None =>
        val result = 200
        val message = s"No network source data for the file: $fileName"
        returnPolicyResult(result, Some(message), responseUri)
        result -> Some(message)
    }

    policyRequests = policyRequests + (requestId -> resultFuture)
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
      case Failure(e) => println(s"Sending a response failed:"); e.printStackTrace()
    }

    responseF
  }


}