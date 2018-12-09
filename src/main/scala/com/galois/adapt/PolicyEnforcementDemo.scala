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
    case class InsufficientData(msg: String = "") extends Policy3Result
    case class Policy3Pass(msg: String = "") extends Policy3Result
    case class Policy3Fail(msg: String = "") extends Policy3Result

    type TimestampNanos = Long

    //trait NodeId{type T; def apply(x: Int): T}
    trait NodeId
    case class ProcessNodeId(nodeId: Int) extends NodeId {}
    case class NetflowNodeId(nodeId: Int) extends NodeId {}

    type TimeInterval = (TimestampNanos, TimestampNanos)

    def toInterval(t: TimestampNanos): TimeInterval = (t, t + 1e9.toLong)

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
    def getProcessWhichTriggeredNetflow(nflowNode: NetflowNodeId, timeInterval: TimeInterval) = {
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

    def theia(processId: ProcessNodeId): Future[Policy3Result] = {
      ???
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

    def trace(processIds: List[ProcessNodeId]) = {
      //      - check for process in parent tree that is connected to: /dev/tty*  e.g. "/dev/tty5" number is between 1 and 7. Connection indicates human activity.
      //        - ensure no ancestor has closed the connection to /dev/tty* in the relevant time window.
      //
      //        CLOSE: process p at t1
      //        NETFLOW: at t2  (t2 > t1)
      //      It is possible that the human-generated activity is read from memory _after_ the CLOSE event. (but we can ignore this condition for the demo).

      //todo: Recursively Traverse up the chain of parent processes
      def getRelatedProcess(processIds: List[ProcessNodeId]) = ???

      def didProcessRecvUIEvents(processNodeId: ProcessNodeId) = {
        //todo: filter on types of events? EVENT_CLOSE/EVENT_OPEN
        //todo: /dev/tty ?
        val query =
        s"""g.V(${processNodeId.nodeId}).in('subject').hasLabel('AdmEvent').out('predicateObject','predicateObject2').hasLabel('AdmFileObject').out('path','(path)').hasLabel('AdmPathNode').has('path', regex('/dev/pts/[0-9]*'))"""
        getGenericNodesFromQuery(query).map { ttyNodes => if (ttyNodes.isEmpty) Policy3Pass() else Policy3Fail() }
      }

      //didProcessRecvUIEvents()
      ???
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

      //for a process, get their connected process [These could be their parent, children or siblings]
      def getRelatedProcess(p: ProcessNodeId): Future[RelatedSubjects] = {
        val maxN = 1
        val queryChildren = s"""g.V(${p.nodeId}).repeat(_.in('parentSubject')).times(${maxN}I).emit().dedup()"""
        val queryAncestors = s"""g.V(${p.nodeId}).repeat(_.out('parentSubject')).times(${maxN}I).emit().dedup()"""
        val querySiblings = s"""g.V(${p.nodeId}).out('parentSubject')""" + queryChildren //g.V(2286).out('parentSubject').repeat(_.in('parentSubject')).emit().dedup()

        val children = getUniqueProcessFromQuery(queryChildren)
        val ancestors = getUniqueProcessFromQuery(queryAncestors)
        val siblings = getUniqueProcessFromQuery(querySiblings)

        for {a <- ancestors; c <- children; s <- siblings} yield RelatedSubjects(a, c, s)
      }

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
          val relevantProcess: Future[Map[ProcessNodeId, Future[Set[String]]]] = getRelevantProcess(pNodeId, getRelatedProcess(pNodeId))

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


    def nicholesQuery(localAddress: String, localPort: Int, remoteAddress: String, remotePort: Int, timestampSeconds: Long): Future[Option[Policy3Result]] = Future {
      Try {
      println("HI!?")

      // Make this interval bigger?
      val maxTimestampNanos = (timestampSeconds + 8) * 1000000000
      val minTimestampNanos = (timestampSeconds - 2)* 1000000000

      // These utility functions Alec wrote are great!
      def flattenFutureTry[A](futFutTry: Future[Future[Try[A]]]): Future[A] =
        futFutTry.flatMap(futTry => futTry.flatMap {
          case Failure(f) => Future.failed(f)
          case Success(a) => Future.successful(a)
        })

        println(maxTimestampNanos)
        println(minTimestampNanos)


        // Get PTN corresponding to netflows
      def startingTagIds: List[Long] = Await.result(flattenFutureTry[JsArray](
        (dbActor ? StringQuery(
          s"""g.V()
             |     .hasLabel('NetFlowObject')
             |     .has('localAddress', '$localAddress').has('remoteAddress', '$remoteAddress')
             |     .has('localPort', $localPort)      .has('remotePort', $remotePort)
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
        println("HI!??")
        var toExplore: List[Long] = startingTagIds
        var nextLevelToExplore: ListBuffer[Long] = ListBuffer.empty
        println(s"depth: $maxDepth")
        for (_ <- 0 until maxDepth) {
          for (ptnId <- toExplore) {
            println(s"Inspecting node id $ptnId")
            if (uiFlowObject(ptnId))
              return true
            nextLevelToExplore ++= provenanceStepBack(ptnId)
          }
          toExplore = nextLevelToExplore.toList
          nextLevelToExplore = ListBuffer.empty
        }
        println("Ran out of patience")
        false
      }

      Some(if (startingTagIds.isEmpty) {
        println("No starting IDs")
        Policy3Fail()
      } else if (findUiProvenance(20)) {
        println("FOUND provenance")
        Policy3Pass()
      } else {
        println("didnt find provenance")
        Policy3Fail()
      })
      } match {
        case Success(s) => s
        case Failure(f) => f.printStackTrace(); throw f
      }
    }


    def clearscope(localAddress: String, localPort: Int, remoteAddress: String, remotePort: Int, timestampSeconds: Long) = {
      val resultFuture = nicholesQuery(localAddress: String, localPort: Int, remoteAddress: String, remotePort: Int, timestampSeconds: Long)

      resultFuture
    }

    def checkPolicy(localAddress: String, localPort: Int, remoteAddress: String, remotePort: Int, timestampSeconds: Long) = {
      println("checkPolicy: " + hostName)

      def processNodes(netflowNodeId: NetflowNodeId) = getProcessWhichTriggeredNetflow(netflowNodeId, toInterval(timestampSeconds))

      def fpfp = Future(Policy3Fail("error: no relevant Process found"))

      def fpfn = Future(Policy3Fail("error: no relevant Netflow found"))

      hostName match {
        case "fiveDirections" => getNetFlows().map {
          _.map { nflow => processNodes(nflow).map(_.map { i => fived(i) }.getOrElse(fpfp)).flatMap(identity) }.getOrElse(fpfn)
        }.flatMap(identity).map(Some(_))
        case "trace" => ??? //trace(processNodes).map(Some(_))
        case "marple" => getNetFlows().map {
          _.map { nflow => processNodes(nflow).map(_.map { i => marple(i) }.getOrElse(fpfp)).flatMap(identity) }.getOrElse(fpfn)
        }.flatMap(identity).map(Some(_))
        case "theia" => ??? //theia(processNode).map(Some(_))
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
        case InsufficientData(msg) => {
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

    val resultFuture = alecsQuery(fileName).map {
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
    val originatingUserFromNetflow =
      s""".in('predicateObject','predicateObject2').out('subject').dedup().as('pId')
         |.out('cmdLine','(cmdLine)','exec').values('path').as('pName')
         |.select('pId').out('localPrincipal').as('p')
         |.values('userId').as('uId').select('p')
         |.select('pId', 'pName','uId')
     """.stripMargin

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

    val nflowMain = (dbActor ? StringQuery(queryNetflow, shouldReturnJson = true)).mapTo[Future[Try[JsArray]]].flatMap(identity)
    val nflowSecondary = (dbActor ? StringQuery(queryNetflow+otherHostConnectionsFromNetflow, shouldReturnJson = true)).mapTo[Future[Try[JsArray]]].flatMap(identity)

    val userMain = (dbActor ? StringQuery(queryNetflow+originatingUserFromNetflow, shouldReturnJson = true)).mapTo[Future[Try[JsArray]]].flatMap(identity)
    val userSecondary = (dbActor ? StringQuery(queryNetflow+otherHostConnectionsFromNetflow+originatingUserFromNetflow, shouldReturnJson = true)).mapTo[Future[Try[JsArray]]].flatMap(identity)

    val x1: Future[List[Map[String, String]]] = userMain
      .map(_.get.elements.toList)
      .map{_.map{i: JsValue => i.convertTo[Map[String, String]]}}

    val x2: Future[List[Map[String, String]]] = userSecondary
      .map(_.get.elements.toList)
      .map{_.map{i: JsValue => i.convertTo[Map[String, String]]}}

    val x3 = for(y1 <- x1; y2 <- x2)yield{x2.map(i => println(s"xxxx: $i")); y1 ++ y2}

    val resultFuture = x3.map{
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