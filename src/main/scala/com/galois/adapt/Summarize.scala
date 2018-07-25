package com.galois.adapt

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

import scala.util.control.Breaks._
import akka.pattern.ask

import scala.concurrent.{Await, Future}
import akka.util.Timeout

//import com.galois.adapt.cdm17.{FILE_OBJECT_DIR, FILE_OBJECT_FILE, FILE_OBJECT_UNIX_SOCKET}
import com.galois.adapt.cdm18._//EventType

import scala.concurrent.duration._
import scala.util.Try
import spray.json._


/*
object PrinterActor {
  //def props(message: String, printerActor: ActorRef): Props = Props(new Greeter(message, printerActor))
  def props(x:Int): Props = Props(new PrinterActor(x))

}*/

/*
class SummarizerActor(dbActor:ActorRef) extends Actor{
  //import PrinterActor._

  def receive = {
    case msg:Any =>
      println("Msg received: ")
      println(msg)
  }
}
*/

/*
def findAllFilesRead() = {
    g.V(id)
}
*/


/*
General usage scenario:
  - Given a handle: [process, file, ...]
  - Actions/context is fixed: defined by the function
  - Enumerate ...?
 */

case class ProcessActivity(processName: String, event: EventType)
case class ProcessActivity2(processName: String, event: EventType, subject: Subject)

trait ProcessRef
case class ProcessName(pName: String) extends ProcessRef
case class ProcessPID(pPID: Int) extends ProcessRef
case class ProcessUUID(pUUID: UUID) extends ProcessRef


object Summarize {
  implicit val timeout = Timeout(10 seconds)
  implicit val executionContext = Application.system.dispatcher
  val dedup = ".dedup()"

  def template(x: String): Future[String] = {
    ///???
    val msg = StringQuery(s"""g.V().has('x','$x')""")

    // Find the process nodes using the given process name
    //StringQuery("g.V().limit(1)")

    val ret = Application.dbActor ? msg

    //ret.map(_.toString())
    //ret.asInstanceOf[Future[Future[...]]

    ret.mapTo[Future[Try[Stream[JsValue]]]].flatMap(_.map(_.toString()))
    //ret.mapTo[Future[Try[Int]]].flatMap(_.map(_.toString()))
    //g.V().hasLabel('AdmSubject').has('subjectType', 'SUBJECT_PROCESS').has('uuid','7bc9092f-f09f-31d3-ab0f-296fdb3a17dd')

  }


  def processNode(p: ProcessRef): Future[String] = {
    val processQuery = p match {

      //example: http://0.0.0.0:8080/api/summarize/process/name/trivial-rewrite
      case ProcessName(pName) => s"g.V().hasLabel('AdmSubject').has('subjectType', 'SUBJECT_PROCESS').outE('cmdLine', 'exec', '(cmdLine)').inV().hasLabel('AdmPathNode').has('path', '$pName')"

      //example: http://0.0.0.0:8080/api/summarize/process/uuid/0177deb7-8534-3771-9f72-4288a77ab175
      case ProcessUUID(pUUID) => s"g.V().hasLabel('AdmSubject').has('subjectType', 'SUBJECT_PROCESS').has('uuid', '$pUUID')"

      case ProcessPID(pPID) => s"g.V().hasLabel('AdmSubject').has('cid', $pPID)"
    }

    def sendQuery(query: StringQuery): Future[String] = {
      val ret = Application.dbActor ? query
      ret.mapTo[Future[Try[Stream[JsValue]]]].flatMap(_.map(_.toString()))
    }
    sendQuery(StringQuery(processQuery + dedup, shouldReturnJson = true))
  }

    //val msg = StringQuery(s"""g.V().has('uuid','$uuid')""")
    //g.V().hasLabel('AdmSubject').has('subjectType', 'SUBJECT_PROCESS').has('uuid','7bc9092f-f09f-31d3-ab0f-296fdb3a17dd')


  //get all the pids used by a process: Process
  // for now just use name: String
  def processPIDs(pName: String): Future[String] = {
    ???
    ///x = 515; g.V(x).inE('cmdLine','exec','(cmdLine)').outV().hasLabel('AdmSubject')
    //val getProcessPIDs = s"""g.V($pName).inE('cmdLine','exec','(cmdLine)').outV().hasLabel('AdmSubject').values('cid')"""
    //sendQuery(StringQuery(getProcessPIDs + dedup))
  }

  //get all affected files by a process
  //get all the process which read a file
  def filesAffected(fName: String): Future[String] = {
    ???
    //x = 1797457; g.V(x).in('subject').hasLabel('AdmEvent').out('predicateObject','predicateObject2').hasLabel('AdmFileObject').out('path', '(path)').hasLabel('AdmPathNode')
    //val getProcessByName = s""""""
    //sendQuery(StringQuery(getProcessByName + dedup))
  }


  //get all the process which read a specific file
  def ActivityOnFile(fName: String): Future[String] = {
    ???
    //
    //val getProcessByName = s""""""
    //sendQuery(StringQuery(getProcessByName + dedup))
  }

  // Affects of a process: Forward mapping
  //example: http://0.0.0.0:8080/api/summarize/process/name/trivial-rewrite
  def ActivitiesOfProcess(p: ProcessRef): Future[List[ProcessActivity]]  = {

    val processQuery = p match {
      case ProcessName(pName) => s"g.V().hasLabel('AdmPathNode').has('path', '$pName').in('exec', 'cmdLine', '(cmdLine)').has('subjectType', 'SUBJECT_PROCESS')"
      case ProcessUUID(pUUID) => s"g.V().has('uuid', '$pUUID').hasLabel('AdmSubject').has('subjectType', 'SUBJECT_PROCESS')"
      case ProcessPID(pPID) => s"g.V().hasLabel('AdmSubject').has('cid', $pPID)"
    }

    val eventsQuery = processQuery + s".in('subject').values('eventType')"
    val eventsJson = Application.dbActor ? StringQuery(eventsQuery, shouldReturnJson = true)
    val x = eventsJson.mapTo[Future[Try[JsArray]]].flatMap(identity)
    //println(x)
    x.map { _.get.elements.toList.flatMap { jv => //don't use get(), can throw an exception
      val activity = jv.toString().replace("\"", "")
      val xx = EventType.values.find(_.toString == activity)
      xx.map(e => ProcessActivity(p.toString, e))
    }
    }
  }



  // Affects of a process: Forward mapping
  //example: http://0.0.0.0:8080/api/summarize/process/name/trivial-rewrite
  def ActivitiesOfProcessWithName(pName: String): Future[List[ProcessActivity]]  = {
    //val getProcessByName = s"""g.V().hasLabel('AdmSubject').has('subjectType', 'SUBJECT_PROCESS').outE('cmdLine', 'exec', '(cmdLine)').inV().hasLabel('AdmPathNode').has('path', '$pName')"""
    val process_query = s"g.V().hasLabel('AdmPathNode').has('path', '$pName').in('exec', 'cmdLine', '(cmdLine)').has('subjectType', 'SUBJECT_PROCESS')"
    val events_query = process_query + s".in('subject').values('eventType')"
    val ret = Application.dbActor ? StringQuery(events_query, shouldReturnJson = true)
    val x = ret.mapTo[Future[Try[JsArray]]].flatMap(identity)
    x.map{_.get.elements.toList.flatMap { jv =>
      val activity = jv.toString().replace("\"", "")
      val xx = EventType.values.find(_.toString == activity)
      xx.map(e => ProcessActivity(pName, e))
    }
      //dont do this, can throw an exception
    }

    /*
    Future.successful(List(
      ProcessActivity(pName, EVENT_READ),
      ProcessActivity(pName, EVENT_READ) ))
      */
    //val ret = Application.dbActor ? StringQuery(getProcessByName + dedup)
    //
  }


  def processActionsFileIO(pName: String): Future[String] = {
    ???
    //FILE_OBJECT_.*
    //FILE_OBJECT_DIR
  }

  //  def processActionsNWIO(pName: String): Future[String] = {
  //    ???
  //    FILE_OBJECT_UNIX_SOCKET
  //  }

  // get nodes near a process reading/writing a port
  def processPortRW(pName: String): Future[String] = {
    ???

  }

  // Intelligently enumerate the names and types of adjacent nodes upto a max. depth from the start node.
  def listAdjacentNodes(node: String, depth: Int): Future[String] = {
    ???
  }

  // trials

}


/*

Events done on a file object
==> g.V().hasLabel('AdmFileObject').in('predicateObject', 'predicateObject2').values('eventType').dedup()
[
    "EVENT_READ",
    "EVENT_SENDTO",
    "EVENT_RECVFROM",
    "EVENT_CLOSE",
    "EVENT_OPEN",
    "EVENT_MMAP",
    "EVENT_LSEEK",
    "EVENT_WRITE",
    "EVENT_RENAME",
    "EVENT_MODIFY_PROCESS",
    "EVENT_CONNECT",
    "EVENT_CREATE_OBJECT",
    "EVENT_ACCEPT",
    "EVENT_UNLINK",
    "EVENT_MODIFY_FILE_ATTRIBUTES",
    "EVENT_LINK",
    "EVENT_EXECUTE",
    "EVENT_FCNTL",
    "EVENT_SENDMSG",
    "EVENT_RECVMSG",
    "EVENT_OTHER",
    "EVENT_BIND"


Rest of the events


* */