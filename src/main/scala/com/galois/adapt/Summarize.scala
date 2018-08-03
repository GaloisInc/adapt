package com.galois.adapt


import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

import scala.util.control.Breaks._
import akka.pattern.ask

import scala.concurrent.{Await, Future}
import akka.util.Timeout
import com.galois.adapt.cdm18.SrcSinkType
import io.netty.channel.local.LocalAddress

//import com.galois.adapt.cdm17.{FILE_OBJECT_DIR, FILE_OBJECT_FILE, FILE_OBJECT_UNIX_SOCKET}
import com.galois.adapt.cdm18.{EventType, FileObjectType}

import scala.concurrent.duration._
import scala.util.Try
import spray.json._

case class FilePath(path: String)
case class TimestampNanos(t: Long)

case class ProcessPath(path: String)//{
  //override def toString() = path
//}

sealed trait ProcessActivity{
  val eventType: EventType
  val earliestTimestampNanos: TimestampNanos
}

case class SubjectProcess(uuid: UUID, cid: Int)

case class ProcessFileActivity(p: ProcessPath,
                               eventType: EventType,
                               s: SubjectProcess,
                               fot: FileObjectType,
                               filePath: FilePath,
                               earliestTimestampNanos: TimestampNanos
                               )extends ProcessActivity

/*
object ProcessFileActivity {
  def fromStringJsonArray(p:ProcessPath, s:String): ProcessFileActivity ={
    //    val m = s.substring(1, myString.length - 1)
    //      .split(",")
    //      .map(_.split(":"))
    //      .map { case Array(k, v) => (k.substring(1, k.length-1), v.substring(1, v.length-1))}
    //      .toMap

    val m = JsonParser.toMap(s)
    ProcessFileActivity(
      p,
      EventType.values.find(_.toString == m("eventType")).get,
      SubjectProcess(UUID.fromString(m("uuid")),m("cid").toInt),
      FileObjectType.values.find(_.toString == m("fot")).get,   //file object type
      FilePath(m("filePath")),
      TimestampNanos(m("earliestTimestampNanos").toLong))
  }
}
*/

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

case class NWAddress(a: String)
case class NWPort(p: Int)
case class NWEndpointLocal(a: NWAddress, p: NWPort)
case class NWEndpointRemote(a: NWAddress, p: NWPort)

//case class ProcessFileActivity(processPath: ProcessPath, file: File, eventType: EventType)
case class ProcessNWActivity(processPath: ProcessPath,
                             eventType: EventType,
                             subject: SubjectProcess,
                             /*
                             localAddress: NWAddress,
                             localPort: NWPort,
                             remoteAddress: NWAddress,
                             remotePort: NWPort,
                             */
                             neLocal: NWEndpointLocal,
                             neRemote: NWEndpointRemote,
                             earliestTimestampNanos: TimestampNanos
                            ) extends ProcessActivity{
  /*
  override def toString = {
    s"$processPath,$subject,Local($localAddress:$localPort)<->Remote($remoteAddress:$remotePort),$earliestTimestampNanos"
  }
  */
}

case class ProcessProcessActivity(
                                   processPath: ProcessPath,
                                   eventType: EventType,
                                   subject1: SubjectProcess,
                                   subject2: SubjectProcess,
                                   earliestTimestampNanos: TimestampNanos
                                 ) extends ProcessActivity


case class ProcessSrcSinkActivity(
                                   processPath: ProcessPath,
                                   eventType: EventType,
                                   subject: SubjectProcess,
                                   srcSinkType: SrcSinkType,
                                   earliestTimestampNanos: TimestampNanos
                                 ) extends ProcessActivity


object Summarize {
  implicit val timeout = Timeout(10 seconds)
  implicit val executionContext = Application.system.dispatcher

  def sendQueryAndTypeResultAsJsonArray(query: String): Future[Try[JsArray]] ={
    //TODO: Log it?
    println(query)

    val result = Application.dbActor ? StringQuery(query, shouldReturnJson = true)
    result.mapTo[Future[Try[JsArray]]].flatMap(identity)
  }

  def summarizeProcess(processPath: ProcessPath): Future[List[ProcessActivity]] = {

    def queryFileActivities(): Future[List[ProcessFileActivity]] = {

      def fromStringJsonArray(p:ProcessPath, s:String): ProcessFileActivity ={
        //    val m = s.substring(1, myString.length - 1)
        //      .split(",")
        //      .map(_.split(":"))
        //      .map { case Array(k, v) => (k.substring(1, k.length-1), v.substring(1, v.length-1))}
        //      .toMap

        val m = JsonParser.toMap(s)
        ProcessFileActivity(
          p,
          EventType.values.find(_.toString == m("eventType")).get,
          SubjectProcess(UUID.fromString(m("uuid")),m("cid").toInt),
          FileObjectType.values.find(_.toString == m("fot")).get,   //file object type
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
           |""".stripMargin//.filter(_>=' ')

      val fileActivities = sendQueryAndTypeResultAsJsonArray(query).map(_.get.elements.toList.map(i=>fromStringJsonArray(processPath, i.toString)))
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
          SubjectProcess(UUID.fromString(m("uuid1")),m("cid2").toInt),
          SrcSinkType.values.find(_.toString == m("eventType")).get,
          TimestampNanos(m("earliestTimestampNanos").toLong))
      }

      val srcSinkActivities = sendQueryAndTypeResultAsJsonArray(query).map(_.get.elements.toList.map(
        i=>srcSinkActivityFromJason(i.toString))
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
          SubjectProcess(UUID.fromString(m("uuid1")),m("cid2").toInt),
          SubjectProcess(UUID.fromString(m("uuid2")),m("cid2").toInt),
          TimestampNanos(m("earliestTimestampNanos").toLong))
      }

      val processProcessActivities = sendQueryAndTypeResultAsJsonArray(query).map(_.get.elements.toList.map(
        i=>processProcessActivityFromJason(i.toString))
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
          SubjectProcess(UUID.fromString(m("uuid")),m("cid").toInt),
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

      val nwActivities = sendQueryAndTypeResultAsJsonArray(query).map(_.get.elements.toList.map(i=>processNWActivityFromJason(i.toString))
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
                                ): Future[List[ProcessActivity]]={
      // TODO: better way to do this?
      val activities = Future.reduce(List(fileActivities, nwActivities, processProcessActivity))(_ ++ _)
      //sort all activities
      activities.map(_.sortBy(activity => activity.earliestTimestampNanos.t))
    }

    combineAndSortActivities(queryFileActivities(), queryNetworkActivity(), queryProcessProcessActivity(), querySrcSinkActivities())
  }
}

/*
scala> sealed trait Number
defined trait Number

scala> case class Integer(x:Int) extends Number
defined class Integer

scala> case class FloatingPoint(x:Double) extends Number
defined class FloatingPoint

scala> List(Integer(1), FloatingPoint(2.2))
res117: List[Product with Serializable with Number] = List(Integer(1), FloatingPoint(2.2))

scala> List(Integer(1), FloatingPoint(2.2)):List[Number]
res118: List[Number] = List(Integer(1), FloatingPoint(2.2))


* */


/*
scala> println(1,2,3)
(1,2,3)

scala> m.map((i,j)=>println(i,j))
<console>:10: error: missing parameter type
Note: The expected type requires a one-argument function accepting a 2-Tuple.
      Consider a pattern matching anonymous function, `{ case (i, j) =>  ... }`
              m.map((i,j)=>println(i,j))
                     ^
<console>:10: error: missing parameter type
              m.map((i,j)=>println(i,j))
                       ^

scala> m.map(case(i,j)=>println(i,j))
<console>:1: error: illegal start of simple expression
       m.map(case(i,j)=>println(i,j))
             ^

scala> m.map{case(i,j)=>println(i,j)}
(a,1)
(b,2)
(c,3)
res148: scala.collection.immutable.Iterable[Unit] = List((), (), ())

* */



/*
scala> def f = {i:Int => i+1}
f: Int => Int

scala> def f = {_+1}
<console>:7: error: missing parameter type for expanded function ((x$1) => x$1.$plus(1))
       def f = {_+1}
                ^

scala> def f = {_:Int+1}
<console>:1: error: identifier expected but integer literal found.
       def f = {_:Int+1}
                      ^

scala> def f = {_+1}
<console>:7: error: missing parameter type for expanded function ((x$1) => x$1.$plus(1))
       def f = {_+1}
                ^

* */