package com.galois.adapt


import java.util.UUID

import akka.pattern.ask

import scala.concurrent.{ExecutionContextExecutor, Future, Await}
import akka.util.Timeout
import com.galois.adapt.cdm18.SrcSinkType

//import scala.util.parsing.input.Positional
//import scala.util.parsing.input.{NoPosition, Position, Reader}
//import scala.util.parsing.combinator._

//import com.galois.adapt.cdm17.{FILE_OBJECT_DIR, FILE_OBJECT_FILE, FILE_OBJECT_UNIX_SOCKET}
import com.galois.adapt.cdm18.{EventType, FileObjectType}

import scala.concurrent.duration._
import scala.util.Try
import spray.json._


object JsonParser{
  def cleanupJson(s: String): String = remove(s, List("\"", "\\{", "\\}"))
  def remove(s: String, removePattern:List[String]): String = removePattern match {
    case hd::tail => remove(s.replaceAll(hd, ""), tail)
    case Nil => s
  }
  def toMap(s: String): Map[String, String] = {
    cleanupJson(s)
      .split(",")
      .map(_.split(":"))
      .map {
        case Array(k, v) => (k, v)
      }.toMap
  }

}



object Summarize {
  implicit val timeout: Timeout = Timeout(10 seconds)
  implicit val executionContext: ExecutionContextExecutor = Application.system.dispatcher

  def sendQueryAndTypeResultAsJsonArray(query: String): Future[Try[JsArray]] = {
    //TODO: Log it?
    //println(query)

    val result = Application.dbActor ? StringQuery(query, shouldReturnJson = true)
    result.mapTo[Future[Try[JsArray]]].flatMap(identity)
  }

  def getAllProcesses(): Future[List[String]] = {
    val query =
    s"""g.V().hasLabel('AdmSubject').has('subjectType', 'SUBJECT_PROCESS').out('exec', 'cmdLine', '(cmdLine)').hasLabel('AdmPathNode').values('path').dedup().limit(2)"""
    sendQueryAndTypeResultAsJsonArray(query).map(_.get.elements.toList.map(_.toString))
  }

  def summarizeAllProcess(maxProcess:Int = 2) = {
    val allProcesses: List[String] = Await.result(getAllProcesses(), Timeout(10 seconds).duration).map(i => i.slice(1, i.length-1))
    println(allProcesses)

   val allProcessActivities: Future[List[List[ProcessActivity]]] = Future.sequence(allProcesses.map(p => summarizeProcess(ProcessPath(p))))
    allProcessActivities.map(_.flatMap(identity))
  }

  def summarizeProcess(processPath: ProcessPath): Future[List[ProcessActivity]] = {

    def queryFileActivities(): Future[List[ProcessFileActivity]] = {

      def fromStringJsonArray(processPath: ProcessPath, s: String): ProcessFileActivity = {
        //    val m = s.substring(1, myString.length - 1)
        //      .split(",")
        //      .map(_.split(":"))
        //      .map { case Array(k, v) => (k.substring(1, k.length-1), v.substring(1, v.length-1))}
        //      .toMap

        val m = JsonParser.toMap(s)
        ProcessFileActivity(
          processPath,
          EventType.values.find(_.toString == m("eventType")).get,
          SubjectProcess(UUID.fromString(m("uuid")), m("cid").toInt, processPath),
          FileObjectType.values.find(_.toString == m("fot")).get, //file object type
          FilePath(m("filePath")),
          TimestampNanos(m("earliestTimestampNanos").toLong))
      }

      val query =
        s"""g.V().hasLabel('AdmPathNode').has('path', '${processPath.path}').as('processPaths')
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
           |.select('subject', 'uuid', 'cid', 'filePath','eventType', 'fot', 'earliestTimestampNanos', 'eventType')
           |""".stripMargin //.filter(_>=' ')

      val fileActivities = sendQueryAndTypeResultAsJsonArray(query).map(_.get.elements.toList.map(i => fromStringJsonArray(processPath, i.toString)))
      fileActivities
    }

    def querySrcSinkActivities(): Future[List[ProcessSrcSinkActivity]] = {
      val query =
        s"""g.V().hasLabel('AdmPathNode').has('path','${processPath.path}').as('processPaths').values('path').as('pathStrings')
           |.select('processPaths').in('exec','cmdLine','(cmdLine)').has('subjectType','SUBJECT_PROCESS').as('subject').values('uuid').as('uuid')
           |.select('subject').values('cid').as('cid')
           |.select('subject').in('subject').as('eventNode').values('earliestTimestampNanos').as('earliestTimestampNanos')
           |.select('eventNode').values('eventType').as('eventType')
           |.select('eventNode').out('predicateObject2','predicateObject').hasLabel('AdmSrcSinkObject').as('srcSinkOnject').values('srcSinkType')
           |.select()""".stripMargin

      def srcSinkActivityFromJason(s: String) = {
        val m = JsonParser.toMap(s)
        ProcessSrcSinkActivity(
          processPath,
          EventType.values.find(_.toString == m("eventType")).get,
          SubjectProcess(UUID.fromString(m("uuid1")), m("cid2").toInt, processPath),
          SrcSinkType.values.find(_.toString == m("eventType")).get,
          TimestampNanos(m("earliestTimestampNanos").toLong))
      }

      val srcSinkActivities = sendQueryAndTypeResultAsJsonArray(query).map(_.get.elements.toList.map(
        i => srcSinkActivityFromJason(i.toString))
      )
      srcSinkActivities
    }

    def queryProcessProcessActivity(): Future[List[ProcessProcessActivity]] = {
      val query =
        s"""g.V().hasLabel('AdmPathNode').has('path','${processPath.path}').as('processNodes1').values('path').as('paths1')
           |.select('processNodes1').in('exec','cmdLine','(cmdLine)').has('subjectType','SUBJECT_PROCESS').as('subjectNode1').values('uuid').as('uuid1')
           |.select('subjectNode1').values('cid').as('cid1')
           |.select('subjectNode1').in('subject').as('eventNode').values('earliestTimestampNanos').as('earliestTimestampNanos')
           |.select('eventNode').values('eventType').as('eventType')
           |.select('eventNode').out('predicateObject2','predicateObject').hasLabel('AdmSubject').as('subjectNode2').values('subjectType').as('subjectType2')
           |.select('subjectNode2').values('uuid').as('uuid2')
           |.select('subjectNode2').values('cid').as('cid2')
           |.select('subjectNode2').out('exec', 'cmdLine', '(cmdLine)').values('path').as('paths2')
           |.select('uuid1', 'cid1', 'subjectNode1', 'uuid2', 'cid2', 'subjectNode2', 'earliestTimestampNanos', 'eventType')
           |
       """.stripMargin

      /*
      * Example response
      {
          "cid2": 12108,
          "subjectNode1": "v[862780]",
          "cid1": 12108,
          "subjectNode2": "v[862780]",
          "uuid2": "b4eb323c-6694-3afc-a5e8-13f09d43604d",
          "uuid1": "b4eb323c-6694-3afc-a5e8-13f09d43604d"
      },
      * */

      def processProcessActivityFromJason(s: String) = {
        val m = JsonParser.toMap(s)
        ProcessProcessActivity(
          processPath,
          EventType.values.find(_.toString == m("eventType")).get,
          SubjectProcess(UUID.fromString(m("uuid1")), m("cid2").toInt, processPath),
          //TODO: Fill in the processpath for subject2.
          SubjectProcess(UUID.fromString(m("uuid2")), m("cid2").toInt, ProcessPath("UNKNOWN")),
          TimestampNanos(m("earliestTimestampNanos").toLong))
      }

      val processProcessActivities = sendQueryAndTypeResultAsJsonArray(query).map(_.get.elements.toList.map(
        i => processProcessActivityFromJason(i.toString))
      )
      processProcessActivities
    }

    def queryNetworkActivity(): Future[List[ProcessNWActivity]] = {
      val query =
      //        s"""g.V()
      //           |.hasLabel('AdmPathNode').has('path','${processPath.path}').as('processPaths').values('path').as('pathStrings')
      //           |.select('processPaths').in('exec','cmdLine','(cmdLine)').has('subjectType','SUBJECT_PROCESS').as('subject')
      //           |.values('uuid').as('uuid')
      //           |.select('subject').values('cid').as('cid')
      //           |.select('subject').in('subject').as('eventNode')
      //           |.values('earliestTimestampNanos').as('earliestTimestampNanos')
      //           |.select('eventNode').values('eventType').as('eventType')
      //           |.select('eventNode').out('predicateObject2','predicateObject').hasLabel('AdmNetFlowObject').as('nwObjects')
      //           |.valueMap('localAddress', 'localPort', 'remoteAddress', 'remotePort')
      //           |.as('nwactivity').select('nwactivity', 'earliestTimestampNanos', 'uuid', 'cid', 'subject')
      //           |""".stripMargin

        s"""g.V().hasLabel('AdmPathNode').has('path','${processPath.path}').as('processPaths').values('path').as('pathStrings')
           |.select('processPaths').in('exec','cmdLine','(cmdLine)').has('subjectType','SUBJECT_PROCESS').as('subject').values('uuid').as('uuid')
           |.select('subject').values('cid').as('cid')
           |.select('subject').in('subject').as('eventNode').values('earliestTimestampNanos').as('earliestTimestampNanos')
           |.select('eventNode').values('eventType').as('eventType')
           |.select('eventNode').out('predicateObject2','predicateObject').hasLabel('AdmNetFlowObject').as('nwObjects').values('localAddress').as('localAddress')
           |.select('nwObjects').values('localPort').as('localPort')
           |.select('nwObjects').values('remoteAddress').as('remoteAddress')
           |.select('nwObjects').values('remotePort').as('remotePort')
           |.select('localAddress', 'localPort', 'remoteAddress', 'remotePort', 'earliestTimestampNanos', 'uuid', 'cid', 'subject', 'eventType')
           |""".stripMargin


      // ==================== example output ====================
      //      "earliestTimestampNanos": 1494432627369211462,
      //      "uuid": "1c5e1f03-c457-30a8-bf28-c835080c76d1",
      //      "nwactivity": {
      //        "localPort": [
      //        -1
      //        ],
      //        "localAddress": [
      //        "localhost"
      //        ],
      //        "remotePort": [
      //        55973
      //        ],
      //        "remoteAddress": [
      //        "128.55.12.81"
      //        ]
      //      },
      //      "cid": 12109,
      //      "subject": "v[862879]"

      def processNWActivityFromJason(s: String) = {
        val m = JsonParser.toMap(s)
        ProcessNWActivity(
          processPath,
          EventType.values.find(_.toString == m("eventType")).get,
          SubjectProcess(UUID.fromString(m("uuid")), m("cid").toInt, processPath),
          NWEndpointLocal(
            NWAddress(m("localAddress")),
            NWPort(m("localPort").toInt)
          ),
          NWEndpointRemote(
            NWAddress(m("remoteAddress")),
            NWPort(m("remotePort").toInt)
          ),
          TimestampNanos(m("earliestTimestampNanos").toLong)
        )
      }

      val nwActivities = sendQueryAndTypeResultAsJsonArray(query).map(_.get.elements.toList.map(i => processNWActivityFromJason(i.toString))
      )
      //TODO: Why does the below not work?
      //val xnwActivities = ret.map(_.get.elements.toList.map(processNWActivityFromJason(_.toString)))
      nwActivities
    }

    //    def composeActivities(fa: Future[List[ProcessFileActivity]], nwa: Future[List[ProcessNWActivity]], pwa: Future[List[ProcessProcessActivity]]): Future[List[ProcessActivity]] = {
    //      ???
    //    }

    def combineAndSortActivities(fileActivities: Future[List[ProcessFileActivity]],
                                 nwActivities: Future[List[ProcessNWActivity]],
                                 processProcessActivity: Future[List[ProcessProcessActivity]],
                                 processSrcSinkActivity: Future[List[ProcessSrcSinkActivity]]
                                ): Future[List[ProcessActivity]] = {
      // TODO: better way to do this?
      val activities: Future[List[ProcessActivity]] = Future.reduce(List(fileActivities, nwActivities, processProcessActivity))(_ ++ _)
      //sort all activities
      activities.map(_.sortBy(activity => activity.earliestTimestampNanos.t))
    }

    combineAndSortActivities(queryFileActivities(), queryNetworkActivity(), queryProcessProcessActivity(), querySrcSinkActivities())
  }

  def summarize(processPath:ProcessPath) = {
    val processActivities: Future[List[ProcessActivity]] = summarizeProcess(processPath)


//    val x: Future[List[ProcessActivityAST]] = processActivities.map(_.map{
//      case a: ProcessFileActivity => ProcessFileActivityAST.fromProcessFileActivity(a)
//      case a: ProcessNWActivity => ProcessNWActivityAST.fromProcessNWActivity(a)
//      case a: ProcessSrcSinkActivity => ProcessSrcSinkActivityAST.fromProcessSrcSinkActivity(a)
//      case a: ProcessProcessActivity => ProcessProcessActivityAST.fromProcessProcessActivity(a)
//    })
    val x: Future[List[ProcessActivity]] = processActivities


    SummaryASTParser(Await.result(x, timeout.duration))
    //println(SummaryASTParser(Await.result(x, timeout.duration)))
  }
}



