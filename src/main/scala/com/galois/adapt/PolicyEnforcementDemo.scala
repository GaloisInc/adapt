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
import spray.json.{DefaultJsonProtocol, JsArray, JsNumber, JsString, JsValue}
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

  def testIpOrCidr(cidr: String): Option[String] = cidr.split("/").toList match {
    case ip :: range :: Nil if testIpAddress(ip).isDefined && Try(range.toInt >= 0 && range.toInt <= 32).getOrElse(false) => Some(cidr)
    case ip :: Nil if testIpAddress(ip).isDefined => Some(cidr)
    case _ => None
  }

  val validIpAddress = Unmarshaller.strict[String, String] { string =>
    testIpAddress(string) match {
      case Some(s) => s
      case _ => throw new IllegalArgumentException(s"'$string' is not a valid IPv4 address.")
    }
  }

  val validIpOrCidr = Unmarshaller.strict[String, String] { string =>
    testIpOrCidr(string) match {
      case Some(s) => s
      case _ => throw new IllegalArgumentException(s"'$string' is not a valid IPv4 address or CIDR range.")
    }
  }

  val validUri = Unmarshaller.strict[String, String] {
    case uri if uri.startsWith("http://") /*|| uri.startsWith("https://")*/ => uri  // TODO: Something smarter here?!
    case uri if uri.startsWith("https://") => throw new IllegalArgumentException(s"SSL is not currently supported. Supplied URL: $uri")
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
                println(s"Check Policy 1: $permissionType, $permissionList, $serverIp, $serverPort, $responseUri, $requestId")
                answerPolicy1(permissionType, permissionList, serverIp, serverPort, clientIp, clientPort, responseUri, requestId, dbActor)
                StatusCodes.Accepted -> "Started the policy check process, will respond later"
              }
            } ~
            parameters('policy ! 2, 'restrictedHost.as(validIpOrCidr)) { restrictedHost => // `restrictedHost` is a CIDR range or maybe a single IP...?
              complete {
                println(s"Check Policy 2: $restrictedHost, $serverIp, $serverPort, $responseUri, $requestId")
                answerPolicy2(restrictedHost, clientIp, clientPort, serverIp, serverPort, responseUri, requestId, dbActor)
                StatusCodes.Accepted -> "Started the policy check process, will respond later"
              }
            } ~
            parameters('policy ! 3, 'requestId.as(validRequestId), 'responseUri.as(validUri)) { (requestId, responseUri) =>
              complete {
                println(s"Check Policy 3: $responseUri, $requestId")
                answerPolicy3(clientIp, clientPort, serverIp, serverPort, timestamp, responseUri, requestId, dbActor)
                StatusCodes.Accepted -> "Started the policy check process, will respond later"
              }
            } ~
            parameters('policy ! 4, 'fileName.as[String], 'requestId.as(validRequestId), 'responseUri.as(validUri)) { (fileName, requestId, responseUri) =>
              complete {
                println(s"Check Policy 4: $fileName, $responseUri, $requestId")
                answerPolicy4(fileName, responseUri, requestId, dbActor)
                StatusCodes.Accepted -> "Started the policy check process, will respond later"
              }
            }
        }
      } ~
      path("status") {
        parameter('requestId.as[Int]) { requestId =>
          complete {
            if (policyRequests contains requestId) {
              val result = if (policyRequests(requestId).isCompleted) policyRequests(requestId).value.get.toString else s"Request #$requestId is not yet complete."
              println(s"Status Check for request ID $requestId returned: $result")
              StatusCodes.Accepted -> result
            } else {
              println(s"Status Check for request ID: $requestId is not yet complete.")
              StatusCodes.NotFound -> s"We do not have an active request for that Id: $requestId"
            }
          }
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



  def answerPolicy1(permissionType: String, permissionList: Seq[String], serverIp: String, serverPort: Int, clientIp: String, clientPort: Int, responseUri: String, requestId: Int, dbActor: ActorRef)
    (implicit timeout: Timeout, ec: ExecutionContext, system: ActorSystem, materializer: Materializer): Unit = {
    // https://git.tc.bbn.com/bbn/tc-policy-enforcement/wikis/Policy_User
    // BLOCK if the user originating the process that sent the HTTP requests is NOT an allowed user or in an allowed user group.
    val desiredProperty = if (permissionType.toLowerCase == "user") "username" else "groupId"
    val query =
      s"""g.V().hasLabel('AdmNetFlowObject')
         |.has('remoteAddress','$serverIp').has('remotePort',$serverPort)
         |.has('localAddress','$clientIp').has('localPort',$clientPort)
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
            println(s"Policy 1 Result for requestId: $requestId is: $result with message: $None")
            returnPolicyResult(200, None, responseUri)
            200 -> None
          } else {
            val message = Some(s"username used to make the request was: $value  This query was testing for: ${permissionList.mkString(",")}")
            println(s"Policy 1 Result for requestId: $requestId is: $result with message: $message")
            returnPolicyResult(400, message, responseUri)
            400 -> message
          }
        case "group" | _ =>
          val result = value.split(",").toSet[String].intersect(permissionList.toSet[String].map(_.toLowerCase)).nonEmpty
          if (result) {
            returnPolicyResult(200, None, responseUri)
            println(s"Policy 1 Result for requestId: $requestId is: $result with message: $None")
            200 -> None
          } else {
            val message = Some(s"user's set of groupIds that made this request was: $value  This query was testing for: ${permissionList.mkString(",")}")
            println(s"Policy 1 Result for requestId: $requestId is: $result with message: $message")
            returnPolicyResult(400, message, responseUri)
            400 -> message
          }
      }
      case x =>
        val message = Some(s"Found ${if (x.isEmpty) "no" else "multiple"} Principal nodes with a username for the process(es) communicating with that netflow: $x")
        println(s"Policy 2 Result for requestId: $requestId is: ERROR with message: $message")
        returnPolicyResult(500, message, responseUri)
        500 -> message
    }.recover {
      case e: Throwable =>
        val message = Some(s"An error occurred during the processing of the request: ${e.getMessage}")
        println(s"Policy 2 Result for requestId: $requestId is: ERROR2 with message: $message")
        returnPolicyResult(500, message, responseUri)
        500 -> message
    }
    policyRequests = policyRequests + (requestId -> resultFuture)
  }


  def answerPolicy2(restrictedHost: String, clientIp: String, clientPort: Int, serverIp: String, serverPort: Int, responseUri: String, requestId: Int, dbActor: ActorRef)
    (implicit timeout: Timeout, ec: ExecutionContext, system: ActorSystem, materializer: Materializer): Unit = {
    // https://git.tc.bbn.com/bbn/tc-policy-enforcement/wikis/Policy_Communication

    val disallowedRange = if (restrictedHost.contains("/")) new CIDRUtils(restrictedHost) else new CIDRUtils(s"$restrictedHost/32")

    val query =  // Get all netflows associated with the process of the netflow in question.
      s"""g.V().hasLabel('AdmNetFlowObject')
         |.has('remoteAddress','$serverIp').has('remotePort',$serverPort)
         |.has('localAddress','$clientIp').has('localPort',$clientPort)
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
        println(s"Policy 2 Result for requestId: $requestId is: $result with message: $None")
        returnPolicyResult(result, None, responseUri) // should allow
        result -> None
      } else {
        val result = 400
        val message = Some(s"The following violating addresses were also contacted by this process: ${results.collect{case t if t._2 => t._1}.mkString(", ")}")
        println(s"Policy 2 Result for requestId: $requestId is: $result with message: $message")
        returnPolicyResult(result, message, responseUri)
        result -> message
      }
    }.recover {
      case e: Throwable =>
        val result = 500
        val message = Some(s"An error occurred during the processing of the request: ${e.getMessage}")
        println(s"Policy 2 Result for requestId: $requestId is: $result with message: $message")
        returnPolicyResult(result, message, responseUri)
        result -> message
    }
    policyRequests = policyRequests + (requestId -> resultFuture)
  }


  def answerPolicy3(localAddress: String, localPort: Int, remoteAddress: String, remotePort: Int, timestampSeconds: Long, responseUri: String, requestId: Int, dbActor: ActorRef)(implicit system: ActorSystem, materializer: Materializer): Unit = {
    // https://git.tc.bbn.com/bbn/tc-policy-enforcement/wikis/Policy_UIAction

    implicit val ec = system.dispatcher

    trait Policy3Result
    trait InsufficientData extends Policy3Result
    trait Pass extends Policy3Result

    def traceTA1PolicyCheck(localAddress: String, localPort: Int, remoteAddress: String, remotePort: Int, timestampSeconds: Long): Future[Option[Policy3Result]] = {
      import scala.concurrent.duration._

      implicit val _: Timeout = 10.minutes

      ???
    }

    def nicholesQuery(localAddress: String, localPort: Int, remoteAddress: String, remotePort: Int, timestampSeconds: Long): Future[Option[Policy3Result]] = {
      import scala.concurrent.duration._

      implicit val _: Timeout = 10.minutes

      // Make this interval bigger?
      val maxTimestampNanos = (timestampSeconds + 1) * 1000000000
      val minTimestampNanos = timestampSeconds * 1000000000

      // These utility functions Alec wrote are great!
      def flattenFutureTry[A](futFutTry: Future[Future[Try[A]]]): Future[A] =
        futFutTry.flatMap(futTry => futTry.flatMap {
          case Failure(f) => Future.failed(f)
          case Success(a) => Future.successful(a)
        })

      def futQuerySplitValueToList(query: String, key: String): Future[List[String]] = flattenFutureTry[JsValue]((dbActor ? CypherQuery(query))
        .mapTo[Future[Try[JsValue]]])
        .map(arr => arr.asInstanceOf[JsArray].elements.toList).map(arr => arr
        .flatMap(obj => obj.asJsObject.getFields(key))
        .flatMap(strUUIDs => strUUIDs.asInstanceOf[JsString].value.split(",")))

      def futQueryGetValue(query: String, key: String): Future[Option[String]] = flattenFutureTry[JsValue]((dbActor ? CypherQuery(query))
        .mapTo[Future[Try[JsValue]]])
        .map(arr => arr.asInstanceOf[JsArray].elements.toList).map(arr => arr
        .flatMap(obj => obj.asJsObject.getFields(key))
        .map(str => str.asInstanceOf[JsString].value.toString).headOption)


      val tagIdsFromEvents = s"""MATCH (n:NetFlowObject)<-[:predicateObject]-(e:Event)
                                 |WHERE e.eventType = "EVENT_WRITE" AND n.localAddress = "${localAddress}"
                                 |AND n.localPort=${localPort} AND n.remoteAddress="${remoteAddress}"
                                 |AND n.remotePort=${remotePort}
                                 |AND e.timestampNanos <= $maxTimestampNanos AND e.timestampNanos >= $minTimestampNanos
                                 |RETURN e.peTagIds as peTagIds
                                 |""".stripMargin('|')

      def getSrcSinkTypesAssociatedWithEvent(tagId: String) = s"""MATCH (p:ProvenanceTagNode)
                                                                 |WHERE p.uuid="$tagId"
                                                                 |AND EXISTS(p.prevTagIdUuid)
                                                                 |MATCH (q:ProvenanceTagNode)
                                                                 |WHERE q.uuid=p.prevTagIdUuid
                                                                 |AND EXISTS(q.flowObjectUuid)
                                                                 |MATCH (s:SrcSinkObject)
                                                                 |WHERE s.uuid=q.flowObjectUuid
                                                                 |RETURN s.srcSinkType as srcSinkType
                                                                  """.stripMargin('|')

      // Look up the provenance tag ids on the event write node which was
      // created at 'timestampSeconds' adjacent to the NetFlowObject
      // with the local and remote addresses and ports given in the request.
      val initialTagIdsFut: Future[List[String]] = futQuerySplitValueToList(tagIdsFromEvents,"peTagIds")

      initialTagIdsFut.flatMap{
        case Nil => Future.successful(Some(new InsufficientData {}))
        case tagIds =>

          println(tagIds)

          val hasUISeqFut: Seq[Future[Option[Policy3Result]]] = tagIds.map { tagId =>

            val srcSinkTypeFut = futQueryGetValue(getSrcSinkTypesAssociatedWithEvent(tagId),"srcSinkType")

            srcSinkTypeFut.map {
              case Some("SRCSINK_UI") => Some(new Pass {})
              case _ => None
            }
          }
          
          // If any of the provenance tag nodes listed on the event node (in the peTagIds field) are associated with
          // a SRCSINK_UI, then the event was generated from the UI.
          Future.fold(hasUISeqFut)(None:Option[Policy3Result]){(acc,inst) => if (inst.isDefined) inst else acc}
      }
    }


    val resultFuture = nicholesQuery(localAddress: String, localPort: Int, remoteAddress: String, remotePort: Int, timestampSeconds: Long).map{
      case Some(s) => s match {
        case _ : Pass =>
          val result: Int = 200
          val messageOpt = Some("PASS")
          returnPolicyResult(result, messageOpt, responseUri)
          result -> messageOpt
        case _ : InsufficientData =>
          val result: Int = 500
          val messageOpt = Some("Insufficient data for the given query parameters.")
          returnPolicyResult(result, messageOpt, responseUri)
          result -> messageOpt
      }
      case None =>
        val result: Int = 400
        val messageOpt: Option[String] = Some("No UI provenance associated with the query parameters.")
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

    def alecsQuery(fileName: String): Future[Option[String]] = {
      import scala.concurrent.duration._

      implicit val _: Timeout = 10.minutes

      val escapedPath = fileName
//        .replaceAll("\\", "\\\\")
//        .replaceAll("\b", "\\b")
//        .replaceAll("\n", "\\n")
//        .replaceAll("\t", "\\t")
//        .replaceAll("\r", "\\r")
//        .replaceAll("\"", "\\\"")

      // Pure utility
      def flattenFutureTry[A](futFutTry: Future[Future[Try[A]]]): Future[A] =
        futFutTry.flatMap(futTry => futTry.flatMap {
          case Failure(f) => Future.failed(f)
          case Success(a) => Future.successful(a)
        })

      def futQuery(query: String): Future[List[JsValue]] = flattenFutureTry[JsValue]((dbActor ? CypherQuery(query)).mapTo[Future[Try[JsValue]]])
          .map(arr => arr.asInstanceOf[JsArray].elements.toList)

      // Files possible corresponding to the given path
      val fileIdsFut: Future[List[Long]] = futQuery(s"""MATCH (f: AdmFileObject)-->(p: AdmPathNode) WHERE p.path =~ '.*${escapedPath}' RETURN ID(f)""")
        .map(arr => arr
          .flatMap(obj => obj.asJsObject.getFields("ID(f)"))
          .map(num => num.asInstanceOf[JsNumber].value.longValue())
        )

      type ID = Long
      type TimestampNanos = Long


      // I want to have a loop over the monad `Future[_]`. That's not possible with a regular `while`, so the loop is a
      // recursive function.
      def loop(toVisit: collection.mutable.Queue[(ID, TimestampNanos)], visited: collection.mutable.Set[ID]): Future[Option[String]] =
        if (toVisit.isEmpty) {
          Future.successful(None)
        } else {
          val (id: ID, time: TimestampNanos) = toVisit.dequeue()

          val checkEnd = s"MATCH (n: AdmNetFlowObject) WHERE ID(n) = $id RETURN n.remoteAddress, n.remotePort"
          futQuery(checkEnd).flatMap { netflows =>
            if (netflows.nonEmpty) {

              val address = netflows.head.asJsObject.getFields("n.remoteAddress").head.toString
              val port = netflows.head.asJsObject.getFields("n.remotePort").head.toString

              Future.successful(Some(address +  ":" + port))
            } else {
              //  OR e.eventType = "EVENT_CREATE_OBJECT"
              val stepWrite  = s"""MATCH (o1)<-[:predicateObject]-(e: AdmEvent)-[:subject]->(o2)
                                  |WHERE (e.eventType = "EVENT_WRITE" OR e.eventType = "EVENT_SENDTO" OR e.eventType = "EVENT_SENDMSG") AND ID(o1) = $id AND e.earliestTimestampNanos <= $time
                                  |RETURN ID(o2), e.latestTimestampNanos
                                  |""".stripMargin('|')

              val stepRead   = s"""MATCH (o1)<-[:subject]-(e: AdmEvent)-[:predicateObject]->(o2)
                                  |WHERE (e.eventType = "EVENT_READ" OR e.eventType = "EVENT_RECV" OR e.eventType = "EVENT_RECVMSG") AND ID(o1) = $id AND e.earliestTimestampNanos <= $time
                                  |RETURN ID(o2), e.latestTimestampNanos
                                  |""".stripMargin('|')

              val stepRename = s"""MATCH (o1)<-[:predicateObject|predicateObject2]-(e: AdmEvent)-[:predicateObject|predicateObject2]->(o2)
                                  |WHERE e.eventType = "EVENT_RENAME" AND ID(o1) = $id AND ID(o1) <> ID(o2) AND e.earliestTimestampNanos <= $time
                                  |RETURN ID(o2), e.latestTimestampNanos
                                  |""".stripMargin('|')

              val stepParent = s"""MATCH (o1)-[:parentSubject]->(o2)
                                  |WHERE ID(o1) = $id
                                  |RETURN ID(o2)
                                  |""".stripMargin('|')

              val foundFut: Future[List[(ID, TimestampNanos)]] = for {
                writes <- futQuery(stepWrite)
                reads <- futQuery(stepRead)
                rename <- futQuery(stepRename)
                parent <- futQuery(stepParent)
              } yield (writes ++ reads ++ rename ++ parent).map(obj => {
                val id = obj.asJsObject.getFields("ID(o2)").head.asInstanceOf[JsNumber].value.longValue()
                val timestamp = obj.asJsObject.getFields("e.latestTimestampNanos").headOption.map(_.asInstanceOf[JsNumber].value.longValue())
                    .getOrElse(time)
                (id, timestamp)
              })

              foundFut.flatMap { found =>
                toVisit ++= found.filter { case (id, _) => !visited.contains(id) }
                visited ++= found.map(_._1)

                loop(toVisit, visited)
              }
            }
          }


        }

      fileIdsFut.flatMap((filesIds: List[Long]) => {
        // toVisit = the IDs of node which could have contributed to the file being sent out
        val toVisit: scala.collection.mutable.Queue[(ID, TimestampNanos)] = collection.mutable.Queue.empty
        val visited: scala.collection.mutable.Set[ID] = scala.collection.mutable.Set.empty

        toVisit ++= filesIds.map(id => (id, Long.MaxValue))
        visited ++= filesIds

        loop(toVisit, visited)
      })
    }

    val resultFuture = alecsQuery(fileName).map{
      case Some(s) =>
        val result = 400
        println(s"Policy 4 Result for requestId: $requestId is: $result with message: $s")
        returnPolicyResult(result, Some(s), responseUri)
        result -> Some(s)
      case None =>
        val result = 200
        val message = s"No network source data for the file: $fileName"
        println(s"Policy 4 Result for requestId: $requestId is: $result with message: $message")
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
      case Success(r) => println(s"Result from _sending_ the response: $r");println(responseCode,message)
      case Failure(e) => println(s"Sending a response failed:"); e.printStackTrace()
    }

    responseF
  }


}