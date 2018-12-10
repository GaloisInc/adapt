package com.galois.adapt

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.http.scaladsl.unmarshalling.PredefinedFromStringUnmarshallers._
import akka.parboiled2.RuleTrace
import akka.stream.Materializer
import akka.pattern.ask
import akka.util.Timeout
import com.galois.adapt.AdaptConfig.HostName
import com.galois.adapt.adm.AdmSubject
import spray.json.{DefaultJsonProtocol, JsArray, JsNumber, JsString, JsValue}
import edazdarevic.commons.net.CIDRUtils

import scala.collection.immutable._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}
import spray.json._
import ApiJsonProtocol._
import ammonite.ops.ln.s
import ammonite.runtime.tools.tail

import scala.collection.mutable.ListBuffer

object PolicyEnforcementDemo extends SprayJsonSupport with DefaultJsonProtocol {

  case class ValueName(name: String)

  implicit val simpleResponseFormat = jsonFormat1(ValueName)

  case class PolicyServerOriginatingUserReply(processUserMap: Map[String, String])
  implicit val serverOriginatingUserPolicyReplyFormat = jsonFormat1(PolicyServerOriginatingUserReply)


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
    case uri if uri.startsWith("http://") /*|| uri.startsWith("https://")*/ => uri // TODO: Something smarter here?!
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
  var policyRequests = Map.empty[RequestId, Future[(Int, Option[String])]]

  val validRequestId = Unmarshaller.strict[String, Int] { string =>
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
            } ~
            parameters('policy ! "originatingUserServer", 'requestId.as(validRequestId), 'responseUri.as(validUri)) { (requestId, responseUri) =>
              complete {
                println(s"Check Policy originatingUserServer: $responseUri, $requestId")
                answerPolicyServerOriginatingUser(clientIp, clientPort, serverIp, serverPort, timestamp, responseUri, requestId, dbActor)
                StatusCodes.Accepted -> "Started the policy check process, will respond later"
              }
            } ~
            parameters('policy ! "fileOriginationServer", 'requestId.as(validRequestId), 'responseUri.as(validUri)) { (requestId, responseUri) =>
              complete {
                println(s"Check Policy originatingUserServer: $responseUri, $requestId")
                answerPolicyServerFileOrigination(clientIp, clientPort, serverIp, serverPort, timestamp, responseUri, requestId, dbActor)
                StatusCodes.Accepted -> "Started the policy check process, will respond later"
              }
            } ~
            parameters('policy ! "CommunicationServer", 'requestId.as(validRequestId), 'responseUri.as(validUri)) { (requestId, responseUri) =>
              complete {
                println(s"Check Policy originatingUserServer: $responseUri, $requestId")
                answerPolicyServerCommunication(clientIp, clientPort, serverIp, serverPort, timestamp, responseUri, requestId, dbActor)
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
       """.stripMargin.replaceAll("\n", "") // .has('eventType','EVENT_WRITE')

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

    val query = // Get all netflows associated with the process of the netflow in question.
      s"""g.V().hasLabel('AdmNetFlowObject')
         |.has('remoteAddress','$serverIp').has('remotePort',$serverPort)
         |.has('localAddress','$clientIp').has('localPort',$clientPort)
         |.in('predicateObject','predicateObject2')
         |.out('subject')
         |.in('subject').hasLabel('AdmEvent').out('predicateObject','predicateObject2').hasLabel('AdmNetFlowObject')
         |.dedup().values('remoteAddress')
       """.stripMargin.replaceAll("\n", "")

    val doesViolateF = (dbActor ? NodeQuery(query, shouldReturnJson = false)).mapTo[Future[Try[Stream[String]]]].flatMap(identity).map(_.get.toList).map {
      case Nil => List.empty[(String, Boolean)] // No matching netflows. ——Note: this should never be possible, because you should always get the originating netflow.
      case l => l.flatMap { string =>
        val validIpOpt = testIpAddress(string)
        val doesViolateOpt = validIpOpt.flatMap(ip => Try(disallowedRange.isInRange(ip)).toOption)
        doesViolateOpt.map(r => string -> r)
      }
    }
    val resultFuture: Future[(Int, Option[String])] = doesViolateF.map { results =>
      if (results.forall(t => !t._2)) {
        val result = 200
        println(s"Policy 2 Result for requestId: $requestId is: $result with message: $None")
        returnPolicyResult(result, None, responseUri) // should allow
        result -> None
      } else {
        val result = 400
        val message = Some(s"The following violating addresses were also contacted by this process: ${results.collect { case t if t._2 => t._1 }.mkString(", ")}")
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

    import scala.concurrent.duration._
    implicit val ec: ExecutionContextExecutor = system.dispatcher
    implicit val timeout: Timeout = 10.minutes

    trait Policy3Result
    case class Policy3InsufficientData(msg: String = "") extends Policy3Result
    case class Policy3Pass(msg: String = "") extends Policy3Result
    case class Policy3Fail(msg: String = "") extends Policy3Result

    type TimestampNanos = Long

    //trait NodeId{type T; def apply(x: Int): T}
    trait NodeId
    case class ProcessNodeId(nodeId: Int) extends NodeId {}
    case class NetflowNodeId(nodeId: Int) extends NodeId {}

    type TimeInterval = (TimestampNanos, TimestampNanos)

    val secondToNanosecond = 1000000000
    def toInterval(t1Seconds: Long, deltaSeconds:Long): TimeInterval = (t1Seconds*secondToNanosecond, (t1Seconds + deltaSeconds) * secondToNanosecond)

    def jsArrayToIntArray(nodesJsArray: Future[JsArray]) = {
      nodesJsArray.map(_.elements.toList.map(_.toString().replaceAll("\"", "").replace("v", "").replace("[", "").replace("]", "").toInt))
    }

    def jsArrayToIntSet(nodesJsArray: Future[JsArray]): Future[Set[Int]] = {
      nodesJsArray.map(_.elements.toSet.map((i: JsValue) => i.toString().replaceAll("\"", "").replace("v", "").replace("[", "").replace("]", "").toInt))
    }


    def sendQueryAndTypeResultAsJsonArray(query: String) = {
      val result = dbActor ? StringQuery(query, shouldReturnJson = true)
      result.mapTo[Future[Try[JsArray]]].flatMap(identity).map(_.getOrElse(JsArray.empty))
    }

    //todo: generic get
    //    def get[A<:NodeId](nodeId:A, query: String) = {
    //      jsArrayToIntArray(sendQueryAndTypeResultAsJsonArray(query)).map(_.map(nodeId.apply))
    //    }
    def getUniqueProcessFromQuery(query: String): Future[Set[ProcessNodeId]] = {
      jsArrayToIntSet(sendQueryAndTypeResultAsJsonArray(query)).map(_.map(ProcessNodeId))
    }

    def getProcessFromQuery(query: String): Future[List[ProcessNodeId]] = {
      jsArrayToIntArray(sendQueryAndTypeResultAsJsonArray(query)).map(_.map(ProcessNodeId))
    }

    def getNetflowsFromQuery(query: String): Future[List[NetflowNodeId]] = {
      jsArrayToIntArray(sendQueryAndTypeResultAsJsonArray(query)).map(_.map(NetflowNodeId))
    }

    def getGenericNodesFromQuery(query: String): Future[List[Int]] = {
      jsArrayToIntArray(sendQueryAndTypeResultAsJsonArray(query))
    }

    //get all process
    def getProcessWhichTriggeredNetflow(nflowNode: NetflowNodeId) = {
      val query = s"""g.V(${nflowNode.nodeId}).in('predicateObject','predicateObject2').out('subject').hasLabel('AdmSubject').has('subjectType', 'SUBJECT_PROCESS').dedup()"""
      println("getProcessWhichTriggeredNetflow: " + query)
      val processNodes = getProcessFromQuery(query)

      //todo: identity!
      def resolveAmbiguity(nfIds: List[ProcessNodeId]): List[ProcessNodeId] = nfIds

      processNodes.map {
        case pIds@(hd :: tail) => Some(resolveAmbiguity(pIds))
        case Nil => None
      }
    }

    def hostName = {
      println(Application.hostNames); assert(Application.hostNames.size == 1); Application.hostNames.head
    }

    def getNetFlows() = {
      val queryGetNetFlow = s"""g.V().hasLabel('AdmNetFlowObject').has('localAddress', '$localAddress').has('localPort', $localPort).has('remoteAddress', '$remoteAddress').has('remotePort', $remotePort)"""
      val netFlowIds: Future[List[NetflowNodeId]] = getNetflowsFromQuery(queryGetNetFlow)

      def resolveAmbiguity(nfIds: List[NetflowNodeId]) = ???

      netFlowIds.map {
        case List(x) => Some(x)
        case nfIds@(hd :: tail) => resolveAmbiguity(nfIds)
        case Nil => None
      }
    }

    //for a process, get their connected process [These could be their parent, children or siblings]
    def getRelatedProcess(p: ProcessNodeId, maxN:Int): Future[RelatedSubjects] = {
      val queryChildren = s"""g.V(${p.nodeId}).repeat(_.in('parentSubject')).times(${maxN}I).emit().dedup()"""
      val queryAncestors = s"""g.V(${p.nodeId}).repeat(_.out('parentSubject')).times(${maxN}I).emit().dedup()"""
      val querySiblings = s"""g.V(${p.nodeId}).out('parentSubject')""" + queryChildren //g.V(2286).out('parentSubject').repeat(_.in('parentSubject')).emit().dedup()

      val children = getUniqueProcessFromQuery(queryChildren)
      val ancestors = getUniqueProcessFromQuery(queryAncestors)
      val siblings = getUniqueProcessFromQuery(querySiblings)

      for {a <- ancestors; c <- children; s <- siblings} yield RelatedSubjects(a, c, s)
    }

    def theia(processNodeIds: List[ProcessNodeId], timestampSeconds: Long): Future[Policy3Result] = {

      val timeWindowSeconds = 60
      //A seemingly large window if 60 seconds was chosen after looking at provided engagement the data
      val tInterval = toInterval(timestampSeconds, timeWindowSeconds)
      def queryAllUserEvents = {
        val query =
          s"""g.V().hasLabel('AdmEvent').has('eventType','EVENT_OTHER').has('comesFromUI',true)
             |.has('earliestTimestampNanos',gte(${tInterval._1})).has('latestTimestampNanos',lte(${tInterval._2}))""".stripMargin

        println(s"Query: $query")
        getGenericNodesFromQuery(query)
      }

      def processNamesFut = {
        if(processNodeIds.isEmpty) Future.successful(Set.empty[String])
        else {
          val processNodeIdsString = processNodeIds.map {
            _.nodeId
          }.mkString(",")
          val processNamesQuery = s"""g.V($processNodeIdsString).out('cmdLine','(cmdLine)','exec').values('path').dedup()"""
          println(s"Query: $processNamesQuery")
          val res = (dbActor ? NodeQuery(processNamesQuery, shouldReturnJson = false)).mapTo[Future[Try[Stream[String]]]].flatMap(identity)
            .map(_.get.toSet)
          res.map(pNames => println(s"process names: $pNames"))
          res
        }
      }

      //ui events are often recieved by unity-2d and sent to a process running under firefox
      //Whitelist this scenario
      def unity2dRecvEventAndIsParentOf(eventsNodeIds:List[Int], processPath: Set[String]) = {
        if (eventsNodeIds.isEmpty) Future.successful(false) else{
          val query =
            s"""g.V(${eventsNodeIds.mkString(",")})
               |.out('subject').as('process')
               |.out('cmdLine','(cmdLine)','exec').hasLabel('AdmPathNode').has('path','unity-2d-shell')
               |.select('process')
               |.in('parentSubject').out('cmdLine','(cmdLine)','exec').values('path').dedup()
               |
         """.stripMargin
          println(s"Query: $query")
          val queryFut = (dbActor ? NodeQuery(query, shouldReturnJson = false)).mapTo[Future[Try[Stream[String]]]].flatMap(identity)
            .map(_.get.toSet)

          queryFut.map{res =>
            println(s"unity2dRecvEventAndIsParentOf: $res")
            res.intersect(processPath).nonEmpty
          }
        }
      }

      def didRecvUIEvents() = {
        queryAllUserEvents.map {
          case Nil => Future.successful(Policy3Fail())
          case eventNodes => println(s"event nodes found: $eventNodes")
            processNamesFut.map { pName =>
              unity2dRecvEventAndIsParentOf(eventNodes, pName).map { result =>
                val msg =
                  if (result)
                    s"Process of same path/name $pName received UI events within a window of ${timeWindowSeconds}s, indicating possible user interaction."
                  else
                    s"UI events seen within a window of ${timeWindowSeconds}s, indicating possible user interaction."
                Policy3InsufficientData(msg)
              }
            }.flatMap(identity)
        }.flatMap(identity)
      }
      didRecvUIEvents()
    }

    def cadets(netflowNodeId: NetflowNodeId): Future[Policy3Result] = {
      //Trace the process attached to the netflow
      // THen find the affected files. x = 3104; g.V(x).in('subject').hasLabel('AdmEvent').out('predicateObject','predicateObject2').hasLabel('AdmFileObject')
      //If it includes /dev/tty, then done.

      def getProcessWhichTriggeredNetflow(netflowNodeId: NetflowNodeId) = {
        //4 different request lead to 4 different cases
        //case 1: netflow <- predicateObject2- EVENT_ADD_OBJECT_ATTRIBUTE -predicateObject ->  FILE_OBJECT_UNIX_SOCKET <-predicateObject-EVENT(recv/send)-subjsect->AdmSubject
        //case 2: netflow <- predicateObject2- EVENT_ADD_OBJECT_ATTRIBUTE -predicateObject ->  Netflow with Local None:None and same remote<-predicateObject->EVENT(write/connect)-subjsect->AdmSubject
        //case 3: netflow <- predicateObject2- EVENT_ADD_OBJECT_ATTRIBUTE -predicateObject ->  FILE_OBJECT_UNIX_SOCKET <-predicateObject-EVENT(recv/send/close)-subjsect->AdmSubject
        //case 4: netflow <-predicateObject-EVENT(close/recv)-subject->AdmSubject

        val netflowQuery = s"g.V(${netflowNodeId.nodeId})"
        val update = ".in('predicateObject2').hasLabel('AdmEvent').has('eventType','EVENT_ADD_OBJECT_ATTRIBUTE').out('predicateObject')"
        val unixSocket = ".hasLabel('AdmFileObject').has('fileObjectType','FILE_OBJECT_UNIX_SOCKET')"
        val netflow = ".hasLabel('AdmNetFlowObject')"
        val getProcess = ".in('predicateObject').hasLabel('AdmEvent').out('subject').hasLabel('AdmSubject').dedup()"

        val q1 = netflowQuery + update + unixSocket + getProcess
        val q2 = netflowQuery + update + netflow + getProcess
        val q3 = netflowQuery + getProcess

        val queries = List(q1, q2, q3)

        val queryResFuture: List[Future[List[ProcessNodeId]]] = queries.map(getProcessFromQuery)
        val res = Future.sequence(queryResFuture)
        res.foreach(p => println(s"process found: $p"))
        res.map(_.flatten)
      }

      def checkProcessWroteTodevtty(processNodeId: ProcessNodeId) = {
        println(s"proces: $processNodeId")
        //todo: filter by timestamps
        val query = s"g.V(${processNodeId.nodeId}).in('subject').hasLabel('AdmEvent').out('predicateObject','predicateObject2').hasLabel('AdmFileObject').out('path','(path)').hasLabel('AdmPathNode').has('path', regex('/dev/tty[0-9]*'))"
        jsArrayToIntArray(sendQueryAndTypeResultAsJsonArray(query)).map { x => println(s"process wrote: $x"); x.nonEmpty }
      }

      getProcessWhichTriggeredNetflow(netflowNodeId).flatMap { i =>
        Future.sequence {
          i.map {
            checkProcessWroteTodevtty
          }
        }
      }.map { l => if (l.contains(true)) Policy3Pass() else Policy3Fail() }
    }

    def trace(processNodeIds: List[ProcessNodeId]) = {
      //      - check for process in parent tree that is connected to: /dev/tty*  e.g. "/dev/tty5" number is between 1 and 7. Connection indicates human activity.
      //        - ensure no ancestor has closed the connection to /dev/tty* in the relevant time window.
      //
      //        CLOSE: process p at t1
      //        NETFLOW: at t2  (t2 > t1)
      //      It is possible that the human-generated activity is read from memory _after_ the CLOSE event. (but we can ignore this condition for the demo).

//      val processNodeIdsString = processNodeIds.map(_.nodeId).mkString(",")
//      val query =
//        s"""g.V($processNodeIdsString).
//           |
//         """.stripMargin

      //Find all /dev/tty[1-7]. Connection indicates human activity.
      def devttyQuery(): Future[List[Int]] = {
        val query =
          s"""g.V().hasLabel('AdmFileObject').has('fileObjectType','FILE_OBJECT_CHAR')
             |.as('fileObject')
             |.out('path','(path)').hasLabel('AdmPathNode').has('path',regex('/dev/tty[1-7]'))
             |.select('fileObject').dedup()
             |""".stripMargin
        println(s"Query: $query")
        getGenericNodesFromQuery(query)
      }

      def connectedXservers(devttyNodeIds:List[Int]) = {
        val pathXServer = "/usr/bin/X"
        val query =
          s"""g.V(${devttyNodeIds.mkString(",")})
             |.in('predicateObject','predicateObject2').hasLabel('AdmEvent').has('eventType','EVENT_OPEN').out('subject').hasLabel('AdmSubject')
             |.as('XServer')
             |.out('cmdLine','exec','(cmdLine)').hasLabel('AdmPathNode').has('path','$pathXServer')
             |.select('XServer')
           """.stripMargin
        println(s"Query: $query")
        getUniqueProcessFromQuery(query).map(_.toList)
    }

      def processRecvEventsFromProcessViaUnixSocket(xServerNodeIds:List[ProcessNodeId]): Future[Set[ProcessNodeId]] = xServerNodeIds match{
        case Nil => Future.successful(Set.empty[ProcessNodeId])
        case _ =>
          val query =
            //.has('eventType', 'EVENT_ACCEPT')?
            s"""g.V(${xServerNodeIds.map{_.nodeId}.mkString(",")})
               |.in('subject').hasLabel('AdmEvent')
               |.out('predicateObject', 'predicateObject2').hasLabel('AdmFileObject').has('fileObjectType', 'FILE_OBJECT_UNIX_SOCKET')
               |.in('predicateObject', 'predicateObject2').hasLabel('AdmEvent').has('eventType', 'EVENT_CONNECT')
               |.out('subject').hasLabel('AdmSubject')
               |""".stripMargin
          println(s"Query: $query")
          getUniqueProcessFromQuery(query)
      }

      def checkIfAncestorsIn(processNodeIds:List[ProcessNodeId], pxNodeIds: Set[ProcessNodeId]) = {
        val allAncestors = processNodeIds.map {getRelatedProcess(_, 5).map(_.ancestors)}
        Future.sequence(allAncestors).map(_.flatten.toSet | processNodeIds.toSet).map{allProcessInvolvedInRequest =>
          allProcessInvolvedInRequest.intersect(pxNodeIds)
        }
      }

      def checkForXvnc4(processNodeIds: List[ProcessNodeId]) = {
        val xvnc4 = "Xvnc4"
        val queryXvnc4 =
          s"""g.V().hasLabel('AdmPathNode').has('path','$xvnc4')
             |.in('cmdLine','exec','(cmdLine)').hasLabel('AdmSubject')""".stripMargin
        println(queryXvnc4)
        val xvnc4NodeIds = getUniqueProcessFromQuery(queryXvnc4)
        xvnc4NodeIds.map(x => processRecvEventsFromProcessViaUnixSocket(x.toList)).flatMap(identity).map{y =>
          checkIfAncestorsIn(processNodeIds, y).map{ commonProcess =>
            if(commonProcess.nonEmpty)
              Policy3InsufficientData(s"Process $commonProcess received events from $xvnc4.")
            else
              Policy3Fail("No Process received UI events.")
          }
        }.flatMap(identity)
      }

      def didProcessRecvUIEvents(processNodeIds: List[ProcessNodeId]) = {
        devttyQuery().map {
          case Nil => Future.successful(Policy3Fail("No AdmPathNode(type:FILE_OBJECT_CHAR) with path /dev/tty[1-7] found."))
          case devttyNodeIds => connectedXservers(devttyNodeIds).map {
            case Nil => Future.successful(Policy3Fail("No XServer found."))
            case xServers => processRecvEventsFromProcessViaUnixSocket(xServers)
              .map(pxNodeIds => checkIfAncestorsIn(processNodeIds, pxNodeIds).map{
                commonProcess =>
                  if(commonProcess.nonEmpty)
                    Policy3Pass(s"Process $commonProcess received UI events.")
                  else
                    Policy3Fail("No Process received UI events.")
              }).flatMap(identity)
          }.flatMap(identity)
        }.flatMap(identity)
      }

      didProcessRecvUIEvents(processNodeIds).map{
        case Policy3Pass(msg1) => Future.successful(Policy3Pass(msg1))
        case Policy3Fail(msg1) => checkForXvnc4(processNodeIds).map{
          case Policy3InsufficientData(msg2) => Policy3InsufficientData(msg1+msg2)
          case Policy3Fail(msg2) => Policy3Fail(msg1+msg2)
          case _ => Policy3Fail()
        }
        case _ => Future.successful(Policy3Fail())
      }.flatMap(identity)

    }

    case class RelatedSubjects(ancestors: Set[ProcessNodeId], children: Set[ProcessNodeId], siblings: Set[ProcessNodeId]) {

    }

    def getProcessNamesFromNode(processNodeId: ProcessNodeId) = {
      val query = s"""g.V(${processNodeId.nodeId}).out('cmdLine','(cmdLine)','exec').values('path')"""
      sendQueryAndTypeResultAsJsonArray(query).map {
        _.elements.toSet.map((i: JsValue) => i.toString)
      }
    }

    def fived(processIds: List[ProcessNodeId]) = {
      implicit val _: Timeout = 10.minutes

      def processNameFromPath(p: String) = p.substring(p.lastIndexOf("/") + 1)

      //new data
      //x = 2290 g.V(x).in('subject').hasLabel('AdmEvent').out('predicateObject', 'predicateObject2').hasLabel('AdmSrcSinkObject').has('srcSinkType', 'SRCSINK_USER_INPUT')

      // find the netflow, then get the connected process using AdmEvent, finally get all EVENT_RECVMSG into the process
      //g.V().hasLabel('AdmNetFlowObject').has('localAddress','128.55.12.81').has('localPort',50956)
      //x = 2301; g.V(x).in().hasLabel('AdmEvent').has('eventType', 'EVENT_RECVMSG')

      //confirm that tht EVENT_RECVMSG are connected to SRCSINK_USER_INPUT
      //x = 2301; g.V(x).in().hasLabel('AdmEvent').has('eventType', 'EVENT_RECVMSG').out('predicateObject', 'predicateObject2').hasLabel('AdmSrcSinkObject').has('srcSinkType', 'SRCSINK_USER_INPUT').dedup()

      def didProcessRecvUiMsg(p: ProcessNodeId) = {
        val query = s"""g.V(${p.nodeId}).in('subject').hasLabel('AdmEvent').out('predicateObject', 'predicateObject2').hasLabel('AdmSrcSinkObject').has('srcSinkType', 'SRCSINK_USER_INPUT').dedup()"""
        println("didProcessRecvUiMsg: " + query)
        sendQueryAndTypeResultAsJsonArray(query).map { x => println(s"didProcessRecvUiMsg: $x"); x.elements.nonEmpty }
      }


      trait ProcessType
      case class Console(name: String) extends ProcessType
      case class Browser(name: String) extends ProcessType
      case class UnknownProcessType(name: String) extends ProcessType


      def getTypeOfProcess(processNames: Set[String]): ProcessType = {
        val typeMap: Map[String => ProcessType, Set[String]] = Map(
          Browser -> Set("firefox.exe", "chrome.exe", "firefox", "chrome"),
          Console -> Set("cmd", "cmd.exe", "Powershell.exe", "Powershell")
        )

        //todo: what if there are more than one element in the intersection...e.g. Set(firefox, firefox.exe)
        typeMap.collectFirst {
          case (k, v) if v.intersect(processNames).nonEmpty => k(v.intersect(processNames).head)
        }.getOrElse(UnknownProcessType(processNames.mkString(",")))
      }

      def getRelevantProcess(processNodeId: ProcessNodeId, relatedP: Future[RelatedSubjects]): Future[Map[ProcessNodeId, Future[Set[String]]]] = {
        val processNames: Future[Set[String]] = getProcessNamesFromNode(processNodeId)
        val consoleNames = Set("conhost.exe", "conhost")

        processNames.map { pNames =>
          relatedP.map { rp =>
            getTypeOfProcess(pNames) match {
              case Console(name) => {
                //get child process and check if it is conhost.exe
                val childrenNames: Map[ProcessNodeId, Future[Set[String]]] = rp.children.map(i => i -> getProcessNamesFromNode(i)).toMap
                childrenNames.map { case (pNodeId, names) => pNodeId -> names.map(_.intersect(consoleNames)) }
                //Future.sequence(childrenNames).map{_.flatten.intersect(consoleNames)}
              }
              case Browser(name) => {
                //get parent process of the same name
                val ancestorNames = rp.ancestors.map(i => i -> getProcessNamesFromNode(i)).toMap
                //Future.sequence(ancestorNames).map{_.flatten.intersect(Set(name))}
                ancestorNames.map { case (pNodeId, names) => pNodeId -> names.map(_.intersect(Set(name))) }
              }
              case UnknownProcessType(name) => {
                //get the same process
                Map(processNodeId -> Future(Set(name)))
              }
            }
          }
        }.flatMap(identity)
      }

      //val x: Future[List[Future[Boolean]]] = filterProcess(result).map( _.map(checkProcessRecvdUiMsg))//(_.exists(i=>checkProcessRecvdUiMsg(i)))
      //val y = x.map(Future.sequence).flatMap(i=>i)


      /*
      - get all process connected to netflow [filter them by timestamp?]
      - create a family tree of the processes
      - flatten the tree into a list wrt to the processType
      - check if any of the process in the list received a ui event [filter by the timestamp?]
       */

      val didAnyProcessRecvUiMsg: Future[List[Map[ProcessNodeId, List[(ProcessNodeId, Boolean)]]]] = Future.sequence {
        processIds.map { pNodeId =>
          val relevantProcess: Future[Map[ProcessNodeId, Future[Set[String]]]] = getRelevantProcess(pNodeId, getRelatedProcess(pNodeId, 1))

          relevantProcess.map {
            _.map { case (a, b) => b.map { r => println(s"relevantProcess: $a -> $r") } }
          }

          relevantProcess.map {
            _.keySet.map(i => i -> didProcessRecvUiMsg(i))
          }
            .map { l: Set[(ProcessNodeId, Future[Boolean])] =>
              Future.sequence {
                l.toList.map { case (p, fb: Future[Boolean]) => fb.map { b => println(s"didAnyProcessRecvUiMsg: $p, $fb, $b"); (p, b) } }
              }.map {
                _.filter(_._2)
              }
                .map {
                  _.groupBy {
                    _._1
                  }
                }
            }.flatMap(identity)
        }
      }.map {
        _.filter {
          _.values.nonEmpty
        }
      }


      //TODO: FIlter using time stamp -> {time at which event was received and the time at which the netflow was initiated}
      didAnyProcessRecvUiMsg.map { res => if (res.nonEmpty) Policy3Pass() else Policy3Fail() }
    }

    def fivedSimple(processIds: List[ProcessNodeId]) = {
      implicit val _: Timeout = 10.minutes

      //confirm that tht EVENT_RECVMSG are connected to SRCSINK_USER_INPUT
      //x = 2301; g.V(x).in().hasLabel('AdmEvent').has('eventType', 'EVENT_RECVMSG').out('predicateObject', 'predicateObject2').hasLabel('AdmSrcSinkObject').has('srcSinkType', 'SRCSINK_USER_INPUT').dedup()

      println(s"fivedSimple: ${processIds}")

      def didProcessRecvUiMsg(p: ProcessNodeId) = {
        val query = s"""g.V(${p.nodeId}).in('subject').hasLabel('AdmEvent').out('predicateObject', 'predicateObject2').hasLabel('AdmSrcSinkObject').has('srcSinkType', 'SRCSINK_USER_INPUT').dedup()"""
        println("didProcessRecvUiMsg: " + query)
        sendQueryAndTypeResultAsJsonArray(query).map { x => println(s"didProcessRecvUiMsg: $x"); x.elements.nonEmpty }
      }

      Future.sequence(processIds.map(didProcessRecvUiMsg)).map { l =>
        println(s"fivedSimple: $l")
        val p = l.filter(_)
        if (l.fold(false)(_ || _)) Policy3Pass() else Policy3Fail()
      }
    }

    def marple(processNodeIds: List[ProcessNodeId]): Future[Policy3Result] = {
      implicit val _: Timeout = 10.minutes


      def didProcessRecvUiMsg(processNodeIds: List[ProcessNodeId]) = {
        val processIdString = processNodeIds.map {
          _.nodeId
        }.mkString(",")
        //todo: filter by timestamp
        //s"""g.V($processIdString).in('predicateObject', 'predicateObject2').hasLabel('AdmEvent').has('comesFromUI', true).dedup()"""//Marple uses predicateObject to connect UI events with process
        val query =
        s"""g.V($processIdString).in().hasLabel('AdmEvent').has('comesFromUI', true).dedup()"""
        println("didProcessRecvUiMsg: " + query)
        sendQueryAndTypeResultAsJsonArray(query).map { x => println(s"didProcessRecvUiMsg: $x"); x.elements.nonEmpty }
      }

      didProcessRecvUiMsg(processNodeIds).map {
        if (_) Policy3Pass() else Policy3Fail()
      }
    }


    // Alec: The future handling in here is gross and I know it. But it'll work for now and we'll delete this soon anyways :)
    def nicholesQuery(localAddress: String, localPort: Int, remoteAddress: String, remotePort: Int, timestampSeconds: Long): Future[Option[Policy3Result]] = Future {

      // Make this interval bigger?
      val maxTimestampNanos = (timestampSeconds + 8) * 1000000000
      val minTimestampNanos = (timestampSeconds - 2)* 1000000000

      // These utility functions Alec wrote are great!
      def flattenFutureTry[A](futFutTry: Future[Future[Try[A]]]): Future[A] =
        futFutTry.flatMap(futTry => futTry.flatMap {
          case Failure(f) => Future.failed(f)
          case Success(a) => Future.successful(a)
        })
//          .has('timestampNanos', lte($maxTimestampNanos))
//      .has('timestampNanos', gte($minTimestampNanos))

      // Get PTN corresponding to netflows
      def startingTagIds: List[Long] = Await.result(flattenFutureTry[JsArray](
        (dbActor ? StringQuery(
          s"""g.V()
             |     .hasLabel('NetFlowObject')
             |     .has('localAddress', '$localAddress').has('remoteAddress', '$remoteAddress')
             |     .has('localPort', $localPort).has('remotePort', $remotePort)
             |     .in('predicateObject')
             |     .hasLabel('Event').has('eventType',within(['EVENT_SENDTO','EVENT_WRITE']))
             |     .has('timestampNanos', lte($maxTimestampNanos))
             |     .has('timestampNanos', gte($minTimestampNanos))
             |     .out('peTagId')
             |     .id()
             |     .dedup()
             |""".stripMargin,
          true
        )).mapTo[Future[Try[JsArray]]]), 30.seconds).elements.toList.collect { case JsNumber(n) => n.toLong }

      // take one step backward in provenance
      def provenanceStepBack(ptnId: Long): List[Long] = Await.result(flattenFutureTry[JsArray](
        (dbActor ? StringQuery(
          s"""g.V($ptnId)
             |     .hasLabel('ProvenanceTagNode')
             |     .out('tagId','prevTagId')
             |     .id()
             |     .dedup()
             |""".stripMargin,
          true
        )).mapTo[Future[Try[JsArray]]]), 30.seconds).elements.toList.collect { case JsNumber(n) => n.toLong }

      // check if the flowObject of a PTN is a SrcSinkObject with type SRCSINK_UI
      def uiFlowObject(ptnId: Long): Boolean = Await.result(flattenFutureTry[JsArray](
        (dbActor ? StringQuery(
          s"""g.V($ptnId)
             |     .out('flowObject')
             |     .hasLabel('SrcSinkObject')
             |     .has('srcSinkType','SRCSINK_UI')
             |     .dedup()
             |""".stripMargin,
          true
        )).mapTo[Future[Try[JsArray]]]), 30.seconds).elements.toList.nonEmpty

      // search to a certain depth
      def findUiProvenance(maxDepth: Int): Boolean = {
        var toExplore: List[Long] = startingTagIds
        var nextLevelToExplore: ListBuffer[Long] = ListBuffer.empty
        for (_ <- 0 until maxDepth) {
          for (ptnId <- toExplore) {
            if (uiFlowObject(ptnId))
              return true
            nextLevelToExplore ++= provenanceStepBack(ptnId)
          }
          toExplore = nextLevelToExplore.toList
          nextLevelToExplore = ListBuffer.empty
        }
        false
      }

      Some(if (startingTagIds.isEmpty) {
        println("startingTagIds isEmpty")
        Policy3Fail()
      } else if (findUiProvenance(20)) {
        Policy3Pass()
      } else {
        Policy3Fail()
      })
    }


    def clearscope(localAddress: String, localPort: Int, remoteAddress: String, remotePort: Int, timestampSeconds: Long) = {
      val resultFuture = nicholesQuery(localAddress: String, localPort: Int, remoteAddress: String, remotePort: Int, timestampSeconds: Long)

      resultFuture
    }

    def checkPolicy(localAddress: String, localPort: Int, remoteAddress: String, remotePort: Int, timestampSeconds: Long) = {
      println("checkPolicy: " + hostName)

      def processNodes(netflowNodeId: NetflowNodeId) = getProcessWhichTriggeredNetflow(netflowNodeId)

      def fpfp = Future(Policy3Fail("error: no relevant Process found"))

      def fpfn = Future(Policy3Fail("error: no relevant Netflow found"))

      hostName match {
        case "fiveDirections" => getNetFlows().map {
          _.map { nflow => processNodes(nflow).map(_.map { i => fived(i) }.getOrElse(fpfp)).flatMap(identity) }.getOrElse(fpfn)
        }.flatMap(identity).map(Some(_))
        case "trace" => getNetFlows().map {
          _.map { nflow => processNodes(nflow).map(_.map { i => trace(i) }.getOrElse(fpfp)).flatMap(identity) }.getOrElse(fpfn)
        }.flatMap(identity).map(Some(_))
        case "marple" => getNetFlows().map {
          _.map { nflow => processNodes(nflow).map(_.map { i => marple(i) }.getOrElse(fpfp)).flatMap(identity) }.getOrElse(fpfn)
        }.flatMap(identity).map(Some(_))
        case "theia" => getNetFlows().map {
          _.map { nflow => processNodes(nflow).map(_.map { i => theia(i, timestampSeconds) }.getOrElse(fpfp)).flatMap(identity) }.getOrElse(fpfn)
        }.flatMap(identity).map(Some(_))
        case "clearscope" => clearscope(localAddress, localPort, remoteAddress, remotePort, timestampSeconds)
        case "cadets" => getNetFlows().map {
          _.map { nflow => cadets(nflow) }.getOrElse(fpfn)
        }.flatMap(identity).map(Some(_))
        case default => Future(Some(Policy3Fail("error: Unknown host")))
      }
    }

    //    def getResultFuture: Future[Option[Policy3Result]] = {
    //      getNetFlows().map {
    //        case Some(netflowNode) => checkPolicy(netflowNode)
    //        case None => Future(Some(Policy3Fail("error: requested NetFlow not found"))) //error: requested NetFlow not found
    //      }.flatMap(identity)
    //    }

    def getResultFuture: Future[Option[Policy3Result]] = checkPolicy(localAddress, localPort, remoteAddress, remotePort, timestampSeconds)

    val resultFuture = getResultFuture.map {
      case Some(s) => s match {
        case Policy3Pass(msg) => {
          val result: Int = 200
          val messageOpt = Some(s"PASS.$msg")
          returnPolicyResult(result, messageOpt, responseUri)
          result -> messageOpt
        }
        case Policy3InsufficientData(msg) => {
          val result: Int = 500
          val messageOpt = Some(s"Insufficient data for the given query parameters.$msg")
          returnPolicyResult(result, messageOpt, responseUri)
          result -> messageOpt
        }
        case Policy3Fail(msg) => {
          val result: Int = 400
          val messageOpt: Option[String] = Some(s"No UI provenance associated with the query parameters.$msg")
          returnPolicyResult(result, messageOpt, responseUri)
          result -> messageOpt
        }
      }
      case None =>
        val result: Int = 400
        val messageOpt: Option[String] = Some("No UI provenance associated with the query parameters.")
        returnPolicyResult(result, messageOpt, responseUri)
        result -> messageOpt
    }.recover {
      case e: Throwable =>
        val result = 500
        val msg = Some(s"An error occurred during the processing of the request: ${e.getMessage}")
        println(s"Policy 3 Result for requestId: $requestId is: $result with message: $msg")
        returnPolicyResult(result, msg, responseUri)
        result -> msg
    }

    policyRequests = policyRequests + (requestId -> resultFuture)
  }

  def checkFileOrigination(fileName: String, responseUri: String, requestId: Int, dbActor: ActorRef)(implicit system: ActorSystem, materializer: Materializer) = {
    // https://git.tc.bbn.com/bbn/tc-policy-enforcement/wikis/Policy_NetData
    // https://git.tc.bbn.com/bbn/tc-policy-enforcement/wikis/Policy4V1
    // TODO
    implicit val ec = system.dispatcher

    def alecsQuery(fileName: String): Future[Option[String]] = {
      import scala.concurrent.duration._

      implicit val _: Timeout = 10.minutes

      println("===============================================")
      println(fileName)
      val escapedPath = fileName
        .replaceAll("\\\\", "\\\\\\\\\\\\\\\\")
//              .replaceAll("\\", "\\\\")
      //        .replaceAll("\b", "\\b")
      //        .replaceAll("\n", "\\n")
      //        .replaceAll("\t", "\\t")
      //        .replaceAll("\r", "\\r")
//              .replaceAll("\"", "\\\"")

      println(escapedPath)
      // Pure utility
      def flattenFutureTry[A](futFutTry: Future[Future[Try[A]]]): Future[A] =
        futFutTry.flatMap(futTry => futTry.flatMap {
          case Failure(f) => Future.failed(f)
          case Success(a) => Future.successful(a)
        })

      def futQuery(query: String): Future[List[JsValue]] = flattenFutureTry[JsValue]((dbActor ? CypherQuery(query)).mapTo[Future[Try[JsValue]]])
        .map(arr => arr.asInstanceOf[JsArray].elements.toList)

      val qq = s"""MATCH (f: AdmFileObject)-->(p: AdmPathNode) WHERE p.path =~ '.*${escapedPath}' RETURN ID(f)"""
      println(qq)
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

              Future.successful(Some(address + ":" + port))
            } else {
              //  OR e.eventType = "EVENT_CREATE_OBJECT"
              val stepWrite =
                s"""MATCH (o1)<-[:predicateObject]-(e: AdmEvent)-[:subject]->(o2)
                   |WHERE (e.eventType = "EVENT_WRITE" OR e.eventType = "EVENT_SENDTO" OR e.eventType = "EVENT_SENDMSG") AND ID(o1) = $id AND e.earliestTimestampNanos <= $time
                   |RETURN ID(o2), e.latestTimestampNanos
                   |""".stripMargin('|')

              val stepRead =
                s"""MATCH (o1)<-[:subject]-(e: AdmEvent)-[:predicateObject]->(o2)
                   |WHERE (e.eventType = "EVENT_READ" OR e.eventType = "EVENT_RECV" OR e.eventType = "EVENT_RECVMSG") AND ID(o1) = $id AND e.earliestTimestampNanos <= $time
                   |RETURN ID(o2), e.latestTimestampNanos
                   |""".stripMargin('|')

              val stepRename =
                s"""MATCH (o1)<-[:predicateObject|predicateObject2]-(e: AdmEvent)-[:predicateObject|predicateObject2]->(o2)
                   |WHERE e.eventType = "EVENT_RENAME" AND ID(o1) = $id AND ID(o1) <> ID(o2) AND e.earliestTimestampNanos <= $time
                   |RETURN ID(o2), e.latestTimestampNanos
                   |""".stripMargin('|')

              val stepParent =
                s"""MATCH (o1)-[:parentSubject]->(o2)
                   |WHERE ID(o1) = $id
                   |RETURN ID(o2)
                   |""".stripMargin('|')

              val stepOpen =
                s"""MATCH (o1)<-[:predicateObject]-(e: AdmEvent)-[:subject]->(o2)
                   |WHERE (e.eventType = "EVENT_OPEN") AND ID(o1) = $id AND e.earliestTimestampNanos <= $time
                   |RETURN ID(o2), e.latestTimestampNanos
                   |""".stripMargin('|')

              val foundFut: Future[List[(ID, TimestampNanos)]] = for {
                writes <- futQuery(stepWrite)
                reads <- futQuery(stepRead)
                rename <- futQuery(stepRename)
                parent <- futQuery(stepParent)
                open <- futQuery(stepOpen)
              } yield (writes ++ reads ++ rename ++ parent ++ open).map(obj => {
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

    alecsQuery(fileName).map {
      case Some(s) =>
        val result = 400
        println(s"Policy 4 Result for requestId: $requestId is: $result with message: $s")
        (result, Some(s))
      case None =>
        val result = 200
        val message = s"No network source data for the file: $fileName"
        println(s"Policy 4 Result for requestId: $requestId is: $result with message: $message")
        (result, Some(message))
    }
  }

  def answerPolicy4(fileName: String, responseUri: String, requestId: Int, dbActor: ActorRef)(implicit system: ActorSystem, materializer: Materializer): Unit = {
    implicit val ec = system.dispatcher

    val resultFuture: Future[(RequestId, Some[String])] = checkFileOrigination(fileName, responseUri, requestId, dbActor)
      .map{
      case (result, msgOpt) =>
        returnPolicyResult(result, msgOpt, responseUri)
        result -> msgOpt
    }
    policyRequests = policyRequests + (requestId -> resultFuture)
  }

  def answerPolicyServerOriginatingUser(clientIp: String, clientPort: Int, serverIp: String, serverPort: Int, timestampSeconds: Long, responseUri: String, requestId: Int, dbActor: ActorRef)
    (implicit timeout: Timeout, ec: ExecutionContext, system: ActorSystem, materializer: Materializer): Unit = {

    // https://git.tc.bbn.com/bbn/tc-policy-enforcement/wikis/TA2API_OriginatingUser
    // Summarize the user and process

    /*
    Immediate Response
    202 ACCEPTED: Started the policy check process, will respond later
    400 BAD REQUEST: (some parameters were invalid)
    500: Error
    */

    //val desiredProperty = if (permissionType.toLowerCase == "user") "username" else "groupId"

    //    {"users":{"apache2":"admin","mysql":"admin"},
    //      "extra":"optional place to store any extra explanation information"}

    def sendQueryAndTypeResultAsJsonIntArray(query: String) = {
      val result = dbActor ? StringQuery(query, shouldReturnJson = true)
      result.mapTo[Future[Try[JsArray]]].flatMap(identity).map(_.getOrElse(JsArray.empty))
        .map(_.elements.toList.map(_.toString().replaceAll("\"", "").replace("v", "").replace("[", "").replace("]", "").toInt))
    }

    val queryNetflow =
      s"""g.V().hasLabel('AdmNetFlowObject').has('remoteAddress','$clientIp').has('remotePort',$clientPort)
         |.has('localAddress','$serverIp').has('localPort',$serverPort)
       """.stripMargin

    //todo: translate to cypher and add gIds,username, hostName
    //    val originatingUserFromNetflow =
    //    s""".in('predicateObject','predicateObject2').out('subject').dedup().as('pId')
    //       |.out('cmdLine','(cmdLine)','exec').values('path').as('pName')
    //       |.select('pId').out('localPrincipal').as('p')
    //       |.values('userId').as('uId').select('p').values('groupIds').as('gIds')
    //       |.select('pId', 'pName','uId','gIds')
    //     """.stripMargin//.replaceAll("\n", "")
    val originatingProcessAndUserFromNetflow =
      s""".in('predicateObject','predicateObject2').out('subject').dedup().as('pId')
         |.out('cmdLine','(cmdLine)','exec').values('path').as('pName')
         |.select('pId').out('localPrincipal').as('p')
         |.values('userId').as('uId').select('p')
         |.select('pId', 'pName','uId')
     """.stripMargin

    def getAncestorProcessUserDetails(pId: Int) = {
      val maxN = 10
      s"""g.V($pId).out('subject').repeat(_.out('parentSubject')).times(${maxN}I).emit().dedup().as('pId')
         |.out('cmdLine','(cmdLine)','exec').values('path').as('pName')
         |.select('pId').out('localPrincipal').as('p')
         |.values('userId').as('uId').select('p')
         |.select('pId', 'pName','uId')
     """.stripMargin
    }

    val otherHostConnectionsFromNetflow =
      s""".in('predicateObject', 'predicateObject2').out('subject').hasLabel('AdmSubject').dedup()
         |.in('subject').hasLabel('AdmEvent').out('predicateObject', 'predicateObject2').hasLabel('AdmNetFlowObject').dedup()
         |.as('start').out('localPort').in('remotePort').as('portother').out('localPort').in('remotePort').as('portend')
         |.where('start', eq('portend')).out('localAddress').in('remoteAddress').as('addyother').out('localAddress').in('remoteAddress').as('addyend')
         |.where('start', eq('addyend')).where('portother', eq('addyother')).select('portother').dedup()
       """.stripMargin

    def sendResponse(responseCode:Int, result: String, msg: Option[String]) = {
      println(s"Policy 1 Result for requestId: $requestId is: $result with message: $msg")
      returnPolicyResult(responseCode, msg, responseUri)
      responseCode -> msg
    }

    println(s"querying: $queryNetflow")
    val nflowMainServerQueryResult = (dbActor ? StringQuery(queryNetflow, shouldReturnJson = true)).mapTo[Future[Try[JsArray]]].flatMap(identity)
    println(s"querying: $queryNetflow$otherHostConnectionsFromNetflow")
    val nflowSecondaryServerQueryResult = (dbActor ? StringQuery(queryNetflow + otherHostConnectionsFromNetflow, shouldReturnJson = true)).mapTo[Future[Try[JsArray]]].flatMap(identity)

    println(s"querying: $queryNetflow$originatingProcessAndUserFromNetflow")
    val userMainServerQueryResult = (dbActor ? StringQuery(queryNetflow + originatingProcessAndUserFromNetflow, shouldReturnJson = true)).mapTo[Future[Try[JsArray]]].flatMap(identity)
    println(s"querying: $queryNetflow$otherHostConnectionsFromNetflow$originatingProcessAndUserFromNetflow")
    val userSecondaryServerQueryResult = (dbActor ? StringQuery(queryNetflow + otherHostConnectionsFromNetflow + originatingProcessAndUserFromNetflow, shouldReturnJson = true)).mapTo[Future[Try[JsArray]]].flatMap(identity)

    val processUserMainServer = userMainServerQueryResult
      .map(_.get.elements.toList)
      .map{_.map{i: JsValue => i.convertTo[Map[String, String]]}}

    val processUserSecondaryServer: Future[List[Map[String, String]]] = userSecondaryServerQueryResult
      .map(_.get.elements.toList)
      .map{_.map{i: JsValue => i.convertTo[Map[String, String]]}}

    val result = for(x1 <- processUserMainServer; x2 <- processUserSecondaryServer)yield{x1 ++ x2}

    val resultFuture = result.map{
      case l@(hd :: tail) => {
        println(s"l: ${l.toString}")
        val result = l.map(i => PolicyServerOriginatingUserReply(i.filterKeys(Set("pName","uId").contains))).toJson
        sendResponse(200, result.toString, Some(result.toString))
      }
      case Nil => {
        sendResponse(400, "No result", None)
      }

//      case x =>
//        val message = Some(s"Found ${if (x.isEmpty) "no" else "multiple"} Principal nodes with a username for the process(es) communicating with that netflow: $x")
//        println(s"Policy 2 Result for requestId: $requestId is: ERROR with message: $message")
//        returnPolicyResult(500, message, responseUri)
//        500 -> message
    }.recover {
      case e: Throwable =>
        val message = Some(s"An error occurred during the processing of the request: ${e.getMessage}")
        println(s"Policy 2 Result for requestId: $requestId is: ERROR2 with message: $message")
        returnPolicyResult(500, message, responseUri)
        500 -> message
    }
    policyRequests = policyRequests + (requestId -> resultFuture)
  }

  def answerPolicyServerFileOrigination(clientIp: String, clientPort: Int, serverIp: String, serverPort: Int, timestampSeconds: Long, responseUri: String, requestId: Int, dbActor: ActorRef)(implicit system: ActorSystem, materializer: Materializer): Unit = {

    // https://git.tc.bbn.com/bbn/tc-policy-enforcement/wikis/TA2API_FilesRead
    implicit val ec = system.dispatcher
    import scala.concurrent.duration._
    implicit val timeout: Timeout = 10.minutes

    def sendQueryAndTypeResultAsJsonArray(query: String) = {
      val result = dbActor ? StringQuery(query, shouldReturnJson = true)
      result.mapTo[Future[Try[JsArray]]].flatMap(identity).map(_.getOrElse(JsArray.empty))
    }

    def jsArrayToList(nodesJsArray: Future[JsArray]): Future[List[String]] = {
      nodesJsArray.map(_.elements.toList.map((i: JsValue) => i.toString().replaceAll("\"", "")))
    }

    def getNodeIdsFromQuery(query: String): Future[List[Int]] = {
      jsArrayToList(sendQueryAndTypeResultAsJsonArray(query)).map(_.map(_.replace("v", "").replace("[", "").replace("]", "").toInt))
    }
    def getStringsFromQuery(query: String): Future[List[String]] = {
      jsArrayToList(sendQueryAndTypeResultAsJsonArray(query))
    }

    def getProcessWhichTriggeredNetflow(netflowNodeId: Int) = {
      //4 different request lead to 4 different cases
      //case 1: netflow <- predicateObject2- EVENT_ADD_OBJECT_ATTRIBUTE -predicateObject ->  FILE_OBJECT_UNIX_SOCKET <-predicateObject-EVENT(recv/send)-subjsect->AdmSubject
      //case 2: netflow <- predicateObject2- EVENT_ADD_OBJECT_ATTRIBUTE -predicateObject ->  Netflow with Local None:None and same remote<-predicateObject->EVENT(write/connect)-subjsect->AdmSubject
      //case 3: netflow <- predicateObject2- EVENT_ADD_OBJECT_ATTRIBUTE -predicateObject ->  FILE_OBJECT_UNIX_SOCKET <-predicateObject-EVENT(recv/send/close)-subjsect->AdmSubject
      //case 4: netflow <-predicateObject-EVENT(close/recv)-subject->AdmSubject

      val netflowQuery = s"g.V($netflowNodeId)"
      val update = ".in('predicateObject2').hasLabel('AdmEvent').has('eventType','EVENT_ADD_OBJECT_ATTRIBUTE').out('predicateObject')"
      val unixSocket = ".hasLabel('AdmFileObject').has('fileObjectType','FILE_OBJECT_UNIX_SOCKET')"
      val netflow = ".hasLabel('AdmNetFlowObject')"
      val getProcess = ".in('predicateObject').hasLabel('AdmEvent').out('subject').hasLabel('AdmSubject').dedup()"

      val q1 = netflowQuery + update + unixSocket + getProcess
      val q2 = netflowQuery + update + netflow + getProcess
      val q3 = netflowQuery + getProcess

      val queries = List(q1, q2, q3)
      queries.foreach(println)

      val queryResFuture: List[Future[List[Int]]] = queries.map(getNodeIdsFromQuery)
      val res = Future.sequence(queryResFuture)
      res.foreach(p => println(s"process found: $p"))
      res.map(_.flatten.distinct)
    }

    def netFlows() = {
      val query =
        s"""g.V().hasLabel('AdmNetFlowObject')
           |.has('remoteAddress','$clientIp').has('remotePort',$clientPort)
           |.has('localAddress','$serverIp').has('localPort',$serverPort)
           |""".stripMargin
      print(s"Query: $query")
      getNodeIdsFromQuery(query)
    }

    def filesAffected(processNodeIds:List[Int]) = {
      val secondToNanosecond = 1000000000
      val deltaSeconds = 10
      val t1 = timestampSeconds*secondToNanosecond
      val t2 = (timestampSeconds + deltaSeconds) * secondToNanosecond

      val query =
        s"""g.V(${processNodeIds.mkString(",")}).in('subject').hasLabel('AdmEvent')
           |.has('earliestTimestampNanos',gte($t1)).has('earliestTimestampNanos',lte($t2))
           |.out('predicateObject','predicateObject2').hasLabel('AdmFileObject').as('file')
           |.out('path','(path)').hasLabel('AdmPathNode').values('path').as('fileNames').select('fileNames').dedup()
           |""".stripMargin
      print(s"Query: $query")
      getStringsFromQuery(query)
    }

    def otherHostConnectionsFromNetflow (netflowNodeIds: List[Int])= {
      val query =
      s"""g.V(${netflowNodeIds.mkString(",")})
         |.in('predicateObject', 'predicateObject2').out('subject').hasLabel('AdmSubject').dedup()
         |.in('subject').hasLabel('AdmEvent').out('predicateObject', 'predicateObject2').hasLabel('AdmNetFlowObject').dedup()
         |.as('start').out('localPort').in('remotePort').as('portother').out('localPort').in('remotePort').as('portend')
         |.where('start', eq('portend')).out('localAddress').in('remoteAddress').as('addyother').out('localAddress').in('remoteAddress').as('addyend')
         |.where('start', eq('addyend')).where('portother', eq('addyother')).select('portother').dedup()
       """.stripMargin

      println(s"Query: $query")
      getNodeIdsFromQuery(query)
    }

    def associatedNetflows(processIds:List[Int]) = {
      // Get all netflows associated with the process of the netflow in question.
      val query =
        s"""g.V(${processIds.mkString(",")})
         |.in('subject').hasLabel('AdmEvent').out('predicateObject','predicateObject2').hasLabel('AdmNetFlowObject').dedup()
       """.stripMargin

      println(s"Query: $query")
      getNodeIdsFromQuery(query)
    }

    val requestedHostprocesses = netFlows().flatMap{nFlowId => Future.sequence(nFlowId.map(getProcessWhichTriggeredNetflow))}.map(_.flatten)
    val netflowsAssociatedWithHostprocesses = requestedHostprocesses.flatMap{pNodeIds => associatedNetflows(pNodeIds)}

    val otherHostprocessList = netflowsAssociatedWithHostprocesses.flatMap{nNodeIds => otherHostConnectionsFromNetflow(nNodeIds)}
      .flatMap{nFlowId => Future.sequence(nFlowId.map(getProcessWhichTriggeredNetflow))}.map(_.flatten)

    val processList = for(hl1 <- requestedHostprocesses; hl2 <- otherHostprocessList)yield hl1 ++ hl2
    val fileNames: Future[List[String]] = processList.flatMap(filesAffected)

    val msgsFut = fileNames.flatMap{f =>
      val result = f.map {fName: String =>
        checkFileOrigination(fName, responseUri: String, requestId: Int, dbActor: ActorRef)
          .map{
            case (400, Some(msg)) => s"$fName <- $msg"
            case (200, _) => s"$fName <- localhost"
          }
      }
      Future.sequence(result)
    }.map(_.mkString("\n"))

    val resultFuture = msgsFut.map {msgs =>
      returnPolicyResult(200, Some(msgs), responseUri)
      200 -> Some(msgs)
    }.recover{
      case e: Throwable =>
        val result = 500
        val msg = Some(s"An error occurred during the processing of the request: ${e.getMessage}")
        println(s"Policy fileOrigination Result for requestId: $requestId is: $result with message: $msg")
        returnPolicyResult(result, msg, responseUri)
        result -> msg
    }

    policyRequests = policyRequests + (requestId -> resultFuture)
  }
//  def answerPolicyServerCommunication(clientIp: String, clientPort: Int, serverIp: String, serverPort: Int, timestampSeconds: Long, responseUri: String, requestId: Int, dbActor: ActorRef)
//    (implicit timeout: Timeout, ec: ExecutionContext, system: ActorSystem, materializer: Materializer): Unit = {
//    // https://git.tc.bbn.com/bbn/tc-policy-enforcement/wikis/TA2API_CommunicationQuery
//
//    val secondToNanosecond = 1000000000
//    val t1 = timestampSeconds*secondToNanosecond
//    val t2 = (timestampSeconds + 20) * secondToNanosecond
//    //.hasLabel('AdmEvent').has('earliestTimestampNanos',gte($t1)).has('earliestTimestampNanos',lte($t2))
//    //todo: add parent process and add netflows on other hosts
//    val associatedProcesses = {
//      // Get all netflows associated with the process of the netflow in question.
//      s"""g.V().hasLabel('AdmNetFlowObject')
//         |.has('remoteAddress',regex('(::ffff:|)$clientIp')).has('remotePort',$clientPort)
//         |.has('localAddress',regex('(::ffff:|)$serverIp')).has('localPort',$serverPort)
//         |.in('predicateObject','predicateObject2')
//         |.out('subject').dedup()
//       """.stripMargin('|')
//    }
//
//    def processName(processId:String) = s"""g.V($processId).out('cmdLine','(cmdLine)','exec').values('path').dedup()"""
//
//    def associatedNetflows(processId:String) = {
//      // Get all netflows associated with the process of the netflow in question.
//      s"""g.V($processId)
//         |.in('subject').hasLabel('AdmEvent').out('predicateObject','predicateObject2').hasLabel('AdmNetFlowObject').dedup()
//         |.values('remoteAddress')
//       """.stripMargin('|')
//    }
////      .as('remoteAddress')
////    .values('remotePort').as('remotePort')
////    .values('localAddress').as('localAddress')
////    .values('localPort').as('localPort')
////    .select('localAddress', 'localPort', 'remoteAddress', 'remotePort')
//    println(s"Query: $associatedProcesses")
//    val allProcessFut: Future[List[String]] = (dbActor ? NodeQuery(associatedProcesses, shouldReturnJson = false)).mapTo[Future[Try[Stream[String]]]].flatMap(identity).map(_.get.toList)
//      .map{_.map{_.mkString(",").replace("v]","").replace("]","")}}
//
//    def legendFut = allProcessFut.map{allProcess => Future.sequence{allProcess.map{p =>
//      (dbActor ? NodeQuery(processName(p), shouldReturnJson = false)).mapTo[Future[Try[Stream[String]]]].flatMap(identity).map(_.get.toList).map(v => p-> v)}}}.flatMap(identity)
//
//    def allNetflowsFut = allProcessFut.map{allProcess => Future.sequence{allProcess.map{p =>
//      (dbActor ? NodeQuery(associatedNetflows(p), shouldReturnJson = false)).mapTo[Future[Try[Stream[String]]]].flatMap(identity).map(_.get.toList).map(v => p-> v)}}}.flatMap(identity)
//
//    allProcessFut.map{i => println(i)}
//    legendFut.map{println}
//    allNetflowsFut.map{println}
//
//    val message: Future[String] = for{allNetflows <- allNetflowsFut; legend <- legendFut} yield allNetflows.toString + "Legend" + legend.toString
//    //val message = Future.successful("")
//
//    val resultFuture: Future[(Int, Option[String])] = message.map { message =>
//      val msg = Some(message)
//      val result = 200
//      println(s"Policy 2 Result for requestId: $requestId is: $result with message: $msg")
//      returnPolicyResult(result, msg, responseUri) // should allow
//      result -> msg
//
//    }.recover {
//      case e: Throwable =>
//        val result = 500
//        val msg = Some(s"An error occurred during the processing of the request: ${e.getMessage}")
//        println(s"Policy 2 Result for requestId: $requestId is: $result with message: $msg")
//        returnPolicyResult(result, msg, responseUri)
//        result -> msg
//    }
//    policyRequests = policyRequests + (requestId -> resultFuture)
//  }
def answerPolicyServerCommunication(clientIp: String, clientPort: Int, serverIp: String, serverPort: Int, timestampSeconds: Long, responseUri: String, requestId: Int, dbActor: ActorRef)
  (implicit timeout: Timeout, ec: ExecutionContext, system: ActorSystem, materializer: Materializer): Unit = {
  // https://git.tc.bbn.com/bbn/tc-policy-enforcement/wikis/TA2API_CommunicationQuery

  val secondToNanosecond = 1000000000
  val t1 = timestampSeconds*secondToNanosecond
  val t2 = (timestampSeconds + 20) * secondToNanosecond
  //.hasLabel('AdmEvent').has('earliestTimestampNanos',gte($t1)).has('earliestTimestampNanos',lte($t2))
  //todo: add netflows on other hosts


  def sendQueryAndTypeResultAsJsonArray(query: String) = {
    val result = dbActor ? StringQuery(query, shouldReturnJson = true)
    result.mapTo[Future[Try[JsArray]]].flatMap(identity).map(_.getOrElse(JsArray.empty))
  }

  def jsArrayToIntArray(nodesJsArray: Future[JsArray]) = {
    nodesJsArray.map(_.elements.toList.map(_.toString().replaceAll("\"", "").replace("v", "").replace("[", "").replace("]", "").toInt))
  }

  val maxN = 10
  val associatedProcesses = {
    // Get all netflows associated with the process of the netflow in question.
    s"""g.V().hasLabel('AdmNetFlowObject')
       |.has('remoteAddress',regex('(::ffff:|)$clientIp')).has('remotePort',$clientPort)
       |.has('localAddress',regex('(::ffff:|)$serverIp')).has('localPort',$serverPort)
       |.in('predicateObject','predicateObject2')
       |.out('subject').dedup()
       """.stripMargin('|')
  }

  val associatedProcessesAncestors = {
    // Get all netflows associated with the process of the netflow in question.
    s"""g.V().hasLabel('AdmNetFlowObject')
       |.has('remoteAddress',regex('(::ffff:|)$clientIp')).has('remotePort',$clientPort)
       |.has('localAddress',regex('(::ffff:|)$serverIp')).has('localPort',$serverPort)
       |.in('predicateObject','predicateObject2')
       |.out('subject').repeat(_.out('parentSubject')).times(${maxN}I).emit().dedup()
       """.stripMargin('|')
  }

//  .repeat(_.out('parentSubject')).times(${maxN}I).emit().dedup()"""
  def processName(processId:String) = s"""g.V($processId).out('cmdLine','(cmdLine)','exec').values('path').dedup()"""

  def associatedNetflows(processId:String) = {
    val secondToNanosecond = 1000000000
    val deltaSeconds = 60
    val t1 = timestampSeconds*secondToNanosecond
    val t2 = (timestampSeconds + deltaSeconds) * secondToNanosecond
    // Get all netflows associated with the process of the netflow in question.

    //todo: wrong timestamps?? Remove??
    //.has('earliestTimestampNanos',gte($t1)).has('earliestTimestampNanos',lte($t2))


    s"""g.V($processId)
       |.in('subject').hasLabel('AdmEvent')
       |.out('predicateObject','predicateObject2').hasLabel('AdmNetFlowObject').dedup()
       |.values('remoteAddress').dedup()
       """.stripMargin('|')
  }

  println(s"Query: $associatedProcesses")
  val allProcessFut: Future[List[Int]] = jsArrayToIntArray(sendQueryAndTypeResultAsJsonArray(associatedProcesses))
  val allProcessAncestorsFut: Future[List[Int]] = jsArrayToIntArray(sendQueryAndTypeResultAsJsonArray(associatedProcessesAncestors))

  val allP: Future[List[Int]] = allProcessFut.map{p => allProcessAncestorsFut.map{p ++ _}}.flatMap(identity)

  def legendFut = allP.map{allProcess => Future.sequence{allProcess.map{p =>
    (dbActor ? NodeQuery(processName(p.toString), shouldReturnJson = false)).mapTo[Future[Try[Stream[String]]]].flatMap(identity).map(_.get.toList).map(v => p-> v)}}}.flatMap(identity).map{_.toMap}

  def allNetflowsFut = allP.map{allProcess => Future.sequence{allProcess.map{p =>
    (dbActor ? NodeQuery(associatedNetflows(p.toString), shouldReturnJson = false)).mapTo[Future[Try[Stream[String]]]].flatMap(identity).map(_.get.toList).map(v => p-> v)}}}.flatMap(identity).map{_.toMap}

  allP.map{i => println(i)}
  legendFut.map{println}
  allNetflowsFut.map{println}

  val message: Future[String] = for{allNetflows <- allNetflowsFut; legend <- legendFut} yield allNetflows.toString + "\nLegend:\n" + legend.toString
  //val message = Future.successful("")

  val resultFuture: Future[(Int, Option[String])] = message.map { message =>
    val msg = Some(message)
    val result = 200
    println(s"Policy 2 Result for requestId: $requestId is: $result with message: $msg")
    returnPolicyResult(result, msg, responseUri) // should allow
    result -> msg

  }.recover {
    case e: Throwable =>
      val result = 500
      val msg = Some(s"An error occurred during the processing of the request: ${e.getMessage}")
      println(s"Policy 2 Result for requestId: $requestId is: $result with message: $msg")
      returnPolicyResult(result, msg, responseUri)
      result -> msg
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
      case Success(r) => println(s"Result from _sending_ the response: $r"); println(responseCode, message)
      case Failure(e) => println(s"Sending a response failed:"); e.printStackTrace()
    }

    responseF
  }


}
