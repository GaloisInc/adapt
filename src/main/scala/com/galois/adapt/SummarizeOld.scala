//
//
//package com.galois.adapt
//
//import java.util.UUID
//
//import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
//
//import scala.util.control.Breaks._
//import akka.pattern.ask
//
//import scala.concurrent.{Await, Future}
//import akka.util.Timeout
//
////import com.galois.adapt.cdm17.{FILE_OBJECT_DIR, FILE_OBJECT_FILE, FILE_OBJECT_UNIX_SOCKET}
//import com.galois.adapt.cdm18._//EventType
//
//import scala.concurrent.duration._
//import scala.util.Try
//import spray.json._
//
//
///*
//object PrinterActor {
//  //def props(message: String, printerActor: ActorRef): Props = Props(new Greeter(message, printerActor))
//  def props(x:Int): Props = Props(new PrinterActor(x))
//
//}*/
//
///*
//class SummarizerActor(dbActor:ActorRef) extends Actor{
//  //import PrinterActor._
//
//  def receive = {
//    case msg:Any =>
//      println("Msg received: ")
//      println(msg)
//  }
//}
//*/
//
///*
//def findAllFilesRead() = {
//    g.V(id)
//}
//*/
//
//
///*
//General usage scenario:
//  - Given a handle: [process, file, ...]
//  - Actions/context is fixed: defined by the function
//  - Enumerate ...?
// */
//
//case class ProcessActivity(processName: String, event: EventType)
//case class ProcessActivity2(processName: String, event: EventType, subject: Subject)
//
//
//trait ProcessRef
//case class ProcessName(pName: String) extends ProcessRef
//case class ProcessPID(pPID: Int) extends ProcessRef
//case class ProcessUUID(pUUID: UUID) extends ProcessRef
//case class ProcessNode(pNode: Int)
//
//
//object SummarizeOld {
//  implicit val timeout = Timeout(10 seconds)
//  implicit val executionContext = Application.system.dispatcher
//  val dedup = ".dedup()"
//
//  def process(p: ProcessRef) = {
//    ProcessSummary.fromProcessRef(p)
//
//    //activitiesOfProcess(p)
//  }
//
//  def processQuery(p: ProcessRef) = p match {
//
//    //example: http://0.0.0.0:8080/api/summarize/process/name/trivial-rewrite
//    case ProcessName(pName) => s"g.V().hasLabel('AdmSubject').has('subjectType', 'SUBJECT_PROCESS').outE('cmdLine', 'exec', '(cmdLine)').inV().hasLabel('AdmPathNode').has('path', '$pName')"
//
//    //example: http://0.0.0.0:8080/api/summarize/process/uuid/0177deb7-8534-3771-9f72-4288a77ab175
//    case ProcessUUID(pUUID) => s"g.V().hasLabel('AdmSubject').has('subjectType', 'SUBJECT_PROCESS').has('uuid', '$pUUID')"
//
//    case ProcessPID(pPID) => s"g.V().hasLabel('AdmSubject').has('cid', $pPID)"
//  }
//
//  def getProcessNode(p: ProcessRef): Future[Int] = {
//
//
//    val ret = Application.dbActor ? StringQuery(processQuery(p) + dedup, shouldReturnJson = true)
//    val xx = ret.mapTo[Future[Try[JsArray]]].flatMap(identity)
//    val y = xx.map(_.get.elements.toList.map(i => i.toString
//      .replace("v", "")
//      .replace("[", "")
//      .replace("]", "")
//      .replaceAll("\"", "").toInt))
//    //TODO:
//    //check that the length of the list is 0
//    y.map(_.head)
//  }
//
//
//  def processNode(p: ProcessRef): Future[String] = {
//    getProcessNode(p).map("Node: " + _.toString)
//  }
//
//
//  def removeQuotes(s: String) = {
//    s.replace("\"", "")
//  }
//
//  class ProcessSummary(processName: ProcessName, processPIDs: ProcessPID, processUUID: ProcessUUID) {
//  }
//  object ProcessSummary {
//
//    def getNodeOf(p: ProcessRef): Future[String] = {
//
//      val processQuery = p match {
//        //example: http://0.0.0.0:8080/api/summarize/process/name/trivial-rewrite
//        case ProcessName(pName) => s"g.V().hasLabel('AdmPathNode').has('path', '$pName').in('exec', 'cmdLine', '(cmdLine)').has('subjectType', 'SUBJECT_PROCESS')"
//        //example: http://0.0.0.0:8080/api/summarize/process/uuid/0177deb7-8534-3771-9f72-4288a77ab175
//        case ProcessUUID(pUUID) => s"g.V().has('uuid', '$pUUID').hasLabel('AdmSubject').has('subjectType', 'SUBJECT_PROCESS')"
//        case ProcessPID(pPID) => s"g.V().hasLabel('AdmSubject').has('cid', $pPID)"
//      }
//
//      val JsonArrayOfProcesses = Application.dbActor ? StringQuery(processQuery + dedup, shouldReturnJson = true)
//      JsonArrayOfProcesses.mapTo[Future[Try[Stream[JsValue]]]].flatMap(_.map(_.toString()))
//    }
//
//    def fromProcessRef(p: ProcessRef): ProcessSummary = {
//      val pNode = getNodeOf(p)
//
//      val ps = p match {
//        case ProcessName(pName) => {
//
//          val node = getNodeOf(p)
//          val pUUID = UUID.randomUUID
//          val pPID = 0
//          new ProcessSummary(ProcessName(Await.result(getUUID(0), timeout.duration)), ProcessPID(pPID), ProcessUUID(pUUID))
//        }
//        //case ProcessUUID(pUUID) => s"g.V().hasLabel('AdmSubject').has('subjectType', 'SUBJECT_PROCESS').has('uuid', '$pUUID')"
//        //case ProcessPID(pPID) => s"g.V().hasLabel('AdmSubject').has('cid', $pPID)"
//        case default => ???
//      }
//      ps
//    }
//
//    // Type node
//    def getUUID(node: Int): Future[String] = {
//      val q = s"g.V(27979).values('uuid').dedup()"
//      val x = Application.dbActor ? StringQuery(q, shouldReturnJson = true)
//      x.mapTo[Future[Try[JsValue]]].flatMap(_.map(_.toString()))
//    }
//
//    def getProcessNameFromNode(node: Int): Future[Any] = {
//      ???
//      val q = s"g.V(pNode.toString).values('uuid')"
//      Application.dbActor ? StringQuery(q, shouldReturnJson = true)
//    }
//
//    def getProcessPidsFromNode(node: Int): Future[Any] = {
//      ???
//      val q = s"g.V(pNode.toString).values('uuid')"
//      Application.dbActor ? StringQuery(q, shouldReturnJson = true)
//    }
//
//    def getProcessNam(node: Int): Future[Any] = {
//      ???
//      val q = s"g.V(pNode.toString).values('uuid')"
//      Application.dbActor ? StringQuery(q, shouldReturnJson = true)
//    }
//  }
//
//
//
//  // Affects of a process: Forward mapping
//  //example: http://0.0.0.0:8080/api/summarize/process/name/trivial-rewrite
//  def activitiesOfProcess(p: ProcessRef): Future[List[ProcessActivity]] = {
//
//
//    def getEvents(p: ProcessRef): Future[List[ProcessActivity]] = {
//      val eventsQuery = processQuery(p) + s".in('subject').values('eventType')"
//      val eventsJson = Application.dbActor ? StringQuery(eventsQuery, shouldReturnJson = true)
//      val jsarrayOfEventType = eventsJson.mapTo[Future[Try[JsArray]]].flatMap(identity)
//
//      //don't use get(), can throw an exception
//      jsarrayOfEventType.map {
//        _.get.elements.toList.flatMap {
//          jv =>
//            val activity = EventType.values.find(_.toString == removeQuotes(jv.toString()))
//            activity.map(e => ProcessActivity(p.toString, e))
//        }
//      }
//    }
//
//    val pNode = getProcessNode(p)
//    val eventsList = getEvents(p)
//
//    /*
//    val x = eventsList match {
//      case hd::tail => summarizeEvent()
//      case Nil =>???
//    }
//   */
//
//    def summarizeNWActivities(pPath: String) = {
//      def query(pPath_new: String, eventType: EventType) = s"g.V().hasLabel('AdmPathNode').has('path', '$pPath_new').in('exec', 'cmdLine', '(cmdLine)').has('subjectType', 'SUBJECT_PROCESS').in('subject').has('eventType', '$eventType').out('predicateObject', 'predicateObject2').hasLabel('AdmNetFlowObject').out('remoteAddress').values('address').dedup()"
//      //val summary = eventType match {
//
//      def sendQuery(query: String) = {
//        val ret = Application.dbActor ? StringQuery(query, shouldReturnJson = true)
//      }
//
//
//      sendQuery(query("??", EVENT_ACCEPT))
//      sendQuery(query("??", EVENT_ACCEPT))
//      /*
//      query.replaceAll("\\$pPath", pPath).replaceAll("\\$eventType", "EVENT_SENDMSG")
//      query.replaceAll("\\$pPath", pPath).replaceAll("\\$eventType", "EVENT_CLOSE")
//      query.replaceAll("\\$pPath", pPath).replaceAll("\\$eventType", "EVENT_WRITE")
//      query.replaceAll("\\$pPath", pPath).replaceAll("\\$eventType", "EVENT_RECVFROM")
//      query.replaceAll("\\$pPath", pPath).replaceAll("\\$eventType", "EVENT_READ")
//      query.replaceAll("\\$pPath", pPath).replaceAll("\\$eventType", "EVENT_OTHER")
//          */
//    }
//
//
//    // Affects of a process: Forward mapping
//    //example: http://0.0.0.0:8080/api/summarize/process/name/trivial-rewrite
//    /*
//  def ActivitiesOfProcessWithName(pName: String): Future[List[ProcessActivity]]  = {
//    //val getProcessByName = s"""g.V().hasLabel('AdmSubject').has('subjectType', 'SUBJECT_PROCESS').outE('cmdLine', 'exec', '(cmdLine)').inV().hasLabel('AdmPathNode').has('path', '$pName')"""
//    val process_query = s"g.V().hasLabel('AdmPathNode').has('path', '$pName').in('exec', 'cmdLine', '(cmdLine)').has('subjectType', 'SUBJECT_PROCESS')"
//    val events_query = process_query + s".in('subject').values('eventType')"
//    val ret = Application.dbActor ? StringQuery(events_query, shouldReturnJson = true)
//    val x = ret.mapTo[Future[Try[JsArray]]].flatMap(identity)
//    x.map{_.get.elements.toList.flatMap { jv =>
//      val activity = jv.toString().replace("\"", "")
//      val xx = EventType.values.find(_.toString == activity)
//      xx.map(e => ProcessActivity(pName, e))
//    }
//      //dont do this, can throw an exception
//    }
//
//    /*
//    Future.successful(List(
//      ProcessActivity(pName, EVENT_READ),
//      ProcessActivity(pName, EVENT_READ) ))
//      */
//    //val ret = Application.dbActor ? StringQuery(getProcessByName + dedup)
//    //
//  }
//*/
//
//    def processActionsFileIO(pName: String): Future[String] = {
//      ???
//      //FILE_OBJECT_.*
//      //FILE_OBJECT_DIR
//    }
//
//    //  def processActionsNWIO(pName: String): Future[String] = {
//    //    ???
//    //    FILE_OBJECT_UNIX_SOCKET
//    //  }
//
//    // get nodes near a process reading/writing a port
//    def processPortRW(pName: String): Future[String] = {
//      ???
//
//    }
//
//    // Intelligently enumerate the names and types of adjacent nodes upto a max. depth from the start node.
//    def listAdjacentNodes(node: String, depth: Int): Future[String] = {
//      ???
//    }
//
//    // trials
//
//
//
//
//    /*
//
//Events done on a file object
//==> g.V().hasLabel('AdmFileObject').in('predicateObject', 'predicateObject2').values('eventType').dedup()
//[
//    "EVENT_READ",
//    "EVENT_SENDTO",
//    "EVENT_RECVFROM",
//    "EVENT_CLOSE",
//    "EVENT_OPEN",
//    "EVENT_MMAP",
//    "EVENT_LSEEK",
//    "EVENT_WRITE",
//    "EVENT_RENAME",
//    "EVENT_MODIFY_PROCESS",
//    "EVENT_CONNECT",
//    "EVENT_CREATE_OBJECT",
//    "EVENT_ACCEPT",
//    "EVENT_UNLINK",
//    "EVENT_MODIFY_FILE_ATTRIBUTES",
//    "EVENT_LINK",
//    "EVENT_EXECUTE",
//    "EVENT_FCNTL",
//    "EVENT_SENDMSG",
//    "EVENT_RECVMSG",
//    "EVENT_OTHER",
//    "EVENT_BIND"
//
//
//Events done on a NETFLOW object
//==> g.V().hasLabel('AdmNetFlowObject').in('predicateObject', 'predicateObject2').values('eventType').dedup()
//[
//    "EVENT_ACCEPT",
//    "EVENT_SENDMSG",
//    "EVENT_CLOSE",
//    "EVENT_WRITE",
//    "EVENT_RECVFROM",
//    "EVENT_READ",
//    "EVENT_OTHER"
//]
//
//rest of the events...
//
//        case EVENT_ACCEPT =>
//        case EVENT_ADD_OBJECT_ATTRIBUTE =>
//        case EVENT_BIND =>
//        case EVENT_BLIND =>
//        case EVENT_BOOT =>
//        case EVENT_CHANGE_PRINCIPAL =>
//        case EVENT_CHECK_FILE_ATTRIBUTES =>
//        case EVENT_CLONE =>
//        case EVENT_CLOSE =>
//        case EVENT_CONNECT =>
//        case EVENT_CREATE_OBJECT =>
//        case EVENT_CREATE_THREAD =>
//        case EVENT_DUP =>
//        case EVENT_EXECUTE =>
//        case EVENT_EXIT =>
//        case EVENT_FLOWS_TO =>
//        case EVENT_FCNTL =>
//        case EVENT_FORK =>
//        case EVENT_LINK =>
//        case EVENT_LOADLIBRARY =>
//        case EVENT_LOGCLEAR =>
//        case EVENT_LOGIN =>
//        case EVENT_LOGOUT =>
//        case EVENT_LSEEK =>
//        case EVENT_MMAP =>
//        case EVENT_MODIFY_FILE_ATTRIBUTES =>
//        case EVENT_MODIFY_PROCESS =>
//        case EVENT_MOUNT =>
//        case EVENT_MPROTECT =>
//        case EVENT_OPEN =>
//        case EVENT_OTHER =>
//        case EVENT_READ =>
//        case EVENT_READ_SOCKET_PARAMS =>
//        case EVENT_RECVFROM =>
//        case EVENT_RECVMSG =>
//        case EVENT_RENAME =>
//        case EVENT_SENDTO =>
//        case EVENT_SENDMSG =>
//        case EVENT_SERVICEINSTALL =>
//        case EVENT_SHM =>
//        case EVENT_SIGNAL =>
//        case EVENT_STARTSERVICE =>
//        case EVENT_TRUNCATE =>
//        case EVENT_UMOUNT =>
//        case EVENT_UNIT =>
//        case EVENT_UNLINK =>
//        case EVENT_UPDATE =>
//        case EVENT_WAIT =>
//        case EVENT_WRITE =>
//        case EVENT_WRITE_SOCKET_PARAMS =>
//        case PSEUDO_EVENT_PARENT_SUBJECT =>
//* */
//
//
//    class XX {
//      //get all the pids used by a process: Process
//      // for now just use name: String
//      def processPIDs(pName: String): Future[String] = {
//        ???
//        ///x = 515; g.V(x).inE('cmdLine','exec','(cmdLine)').outV().hasLabel('AdmSubject')
//        //val getProcessPIDs = s"""g.V($pName).inE('cmdLine','exec','(cmdLine)').outV().hasLabel('AdmSubject').values('cid')"""
//        //sendQuery(StringQuery(getProcessPIDs + dedup))
//      }
//
//      //get all affected files by a process
//      //get all the process which read a file
//      def filesAffected(fName: String): Future[String] = {
//        ???
//        //x = 1797457; g.V(x).in('subject').hasLabel('AdmEvent').out('predicateObject','predicateObject2').hasLabel('AdmFileObject').out('path', '(path)').hasLabel('AdmPathNode')
//        //val getProcessByName = s""""""
//        //sendQuery(StringQuery(getProcessByName + dedup))
//      }
//
//
//      //get all the process which read a specific file
//      def ActivityOnFile(fName: String): Future[String] = {
//        ???
//        //
//        //val getProcessByName = s""""""
//        //sendQuery(StringQuery(getProcessByName + dedup))
//      }
//    }
//    ???
//  }
//}
//
///*
//  def startSummarizer() = {
///*
//    val printer_actor: ActorRef =
//        system.actorOf(Props(new PrinterActor(dbActor)), name = "PrinterActor")
//*/
//
//        //system.actorOf(PrinterActor.props(4), name = "PrinterActor")
//
//
//    implicit val timeout = Timeout(10 seconds)
//    //val queryString = s"g.V(0)"
//    //val provResultF = (dbActor ? NodeQuery(provQueryString, shouldReturnJson = false)).mapTo[Future[Try[Stream[Vertex]]]].flatMap(identity)
//    //val provResultF = (dbActor ? NodeQuery(provQueryString, shouldReturnJson = false)).mapTo[Future[Try[Stream[Vertex]]]].flatMap(identity)
//
//    import scala.concurrent.Future
//    import org.apache.tinkerpop.gremlin.structure.{Edge, Vertex}
//
//    // REPL: quick and dirty testing
//    breakable {
//        while(true){
//            val ip = readLine("Enter your msg: ")
//
//            if(ip != "exit"){
//              /*
//                 val provResultF = (dbActor ? StringQuery(ip, shouldReturnJson = true))
//                    .mapTo[Future[Try[Stream[Vertex]]]]
//                    .flatMap(identity)
//                 provResultF.map{
//                   x =>
//                   println(x)
//                 }
//              */
//
//             val provResultF = dbActor ? StringQuery(ip, shouldReturnJson = true)
//             val resultF = Await.result(provResultF, timeout.duration).asInstanceOf[Future[Try[Int]]]
//             val resultFF = Await.result(resultF, timeout.duration)
//             /*
//             resultFF match{
//               case Success x => println(x)
//
//             }
//             */
//            }
//            else{
//                break
//            }
//        }
//    }
//    system.terminate()
//    Runtime.getRuntime.halt(0)
//    //System.exit(0)
//  }
//
//*/
//
//
///*
//
//  def template(x: String): Future[String] = {
//    ///???
//    val msg = StringQuery(s"""g.V().has('x','$x')""")
//
//    // Find the process nodes using the given process name
//    //StringQuery("g.V().limit(1)")
//
//    val ret = Application.dbActor ? msg
//
//    //ret.map(_.toString())
//    //ret.asInstanceOf[Future[Future[...]]
//
//    ret.mapTo[Future[Try[Stream[JsValue]]]].flatMap(_.map(_.toString()))
//    //ret.mapTo[Future[Try[Int]]].flatMap(_.map(_.toString()))
//    //g.V().hasLabel('AdmSubject').has('subjectType', 'SUBJECT_PROCESS').has('uuid','7bc9092f-f09f-31d3-ab0f-296fdb3a17dd')
//
//  }
//
// */