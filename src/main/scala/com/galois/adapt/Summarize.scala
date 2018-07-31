package com.galois.adapt


import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

import scala.util.control.Breaks._
import akka.pattern.ask

import scala.concurrent.{Await, Future}
import akka.util.Timeout

//import com.galois.adapt.cdm17.{FILE_OBJECT_DIR, FILE_OBJECT_FILE, FILE_OBJECT_UNIX_SOCKET}
import com.galois.adapt.cdm18.{EventType, FileObjectType}

import scala.concurrent.duration._
import scala.util.Try
import spray.json._

case class FilePath(path: String)
case class TimestampNanos(t: Long)

case class ProcessPath(path: String){
  override def toString() ={path}
}

trait ProcessActivity

case class SubjectProcess(uuid: UUID, cid: Int)


case class ProcessFileActivity(p: ProcessPath,
                               s: SubjectProcess,
                               fot: FileObjectType,
                               filePath: FilePath,
                               earliestTimestampNanos: TimestampNanos)extends ProcessActivity

object JsonParser{
  def cleanupJson(s: String) = remove(s, List("\"", "\\{", "\\}"))
  def remove(s: String, removePattern:List[String]): String = removePattern match {
    case hd::tail => remove(s.replaceAll(hd, ""), tail)
    case Nil => s
  }
}

object ProcessFileActivity {
  def fromStringJsonArray(p:ProcessPath, s:String): ProcessFileActivity ={
    //    val m = s.substring(1, myString.length - 1)
    //      .split(",")
    //      .map(_.split(":"))
    //      .map { case Array(k, v) => (k.substring(1, k.length-1), v.substring(1, v.length-1))}
    //      .toMap

    val m = JsonParser.cleanupJson(s)
      .split(",")
      .map(_.split(":"))
      .map {
        case Array(k, v) => (k, v)
      }.toMap

    val subject = SubjectProcess(UUID.fromString(m("uuid")),m("cid").toInt)
    val fot = FileObjectType.values.find(_.toString == m("fot")).get

    ProcessFileActivity(
      p,
      subject,
      fot,
      FilePath(m("filePath")),
      TimestampNanos(m("earliestTimestampNanos").toLong))
  }
}

//case class ProcessFileActivity(processPath: ProcessPath, file: File, eventType: EventType)
case class ProcessNWActivity() extends ProcessActivity
case class ProcessProcessActivity() extends ProcessActivity



object Summarize {
  implicit val timeout = Timeout(10 seconds)
  implicit val executionContext = Application.system.dispatcher

  def summarizeProcess(processPath: ProcessPath): Future[List[ProcessActivity]] = {

    def populateSubject(s: String): SubjectProcess = ???

    def sendQueryFileActivities(): Future[List[ProcessFileActivity]] = {

      val fileQuery =
        s"""g.V().hasLabel('AdmPathNode').has('path', '$processPath').as('processPaths')
           |.values('path').as('pathStrings')
           |.select('processPaths')
             |.in('exec', 'cmdLine', '(cmdLine)').has('subjectType', 'SUBJECT_PROCESS').as('subject')
               |.values('uuid').as('uuid')
             |.select('subject').values('cid').as('cid')
             |.select('subject').in('subject').as('eventNode')
               |.values('earliestTimestampNanos').as('earliestTimestampNanos')
               |.select('eventNode').values('eventType').as('eventType')
               |.select('eventNode').out('predicateObject2', 'predicateObject').hasLabel('AdmFileObject').as('fileObjects')
                 |.values('fileObjectType').as('fot')
               |.select('fileObjects').out('path', '(path)').values('path')
                   |.as('filePath')
           |.select('subject', 'uuid', 'cid', 'filePath','eventType', 'fot', 'earliestTimestampNanos')
           |""".stripMargin//.filter(_>=' ')
      println(fileQuery)
      val result = Application.dbActor ? StringQuery(fileQuery, shouldReturnJson = true)
      val ret = result.mapTo[Future[Try[JsArray]]].flatMap(identity)
      ret.map(_.get.elements.toList.map(i=>ProcessFileActivity.fromStringJsonArray(processPath, i.toString)))
    }

    def newFun(processFileActivities: Future[List[ProcessFileActivity]]): Future[List[ProcessActivity]]={
      processFileActivities
    }


    def sendQueryProcessActivity(query: String): List[ProcessProcessActivity] = {
      val processQuery =""
      ???
    }
    def sendQueryNetworkActivity(query: String): List[ProcessNWActivity] = {
      val nwQuery =""
      ???
    }

    def composeActivities(fa: Future[List[ProcessFileActivity]], nwa: Future[List[ProcessNWActivity]], pwa: Future[List[ProcessProcessActivity]]): Future[List[ProcessActivity]] = {
      ???
    }
    newFun(sendQueryFileActivities())
  }
}