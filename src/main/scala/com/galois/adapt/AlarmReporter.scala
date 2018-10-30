package com.galois.adapt

/*
*
* Alarm rreporting framework.
* Supports
*   Splunk
*   logging to file
*   printing to console
*   reporting in UI
*
*/
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

import scala.concurrent.duration._
import akka.event.{Logging, LoggingAdapter}
import com.galois.adapt.NoveltyDetection.PpmTreeNodeAlarm
//import com.galois.adapt.Application.system
//import com.galois.adapt.NoveltyDetection.{Alarm, NamespacedUuidDetails}
import spray.json._//{JsNumber, JsObject, JsString, JsValue}
import spray.json.DefaultJsonProtocol._

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future

import ApiJsonProtocol._

case class ProcessDetails(processName:String, pid:Option[Int])


//trait AlarmNamespace
//case object DefaultNamespace extends AlarmNamespace
//case object summary extends AlarmNamespace
//case object defaultNamespace extends AlarmNamespace

case class MetaData(runID:String, alarmCategory:String){
  def toJson:JsValue = {
    JsObject(
      "RunID" -> JsString(runID),
      "AlarmCategory" -> JsString(alarmCategory)
    )
  }
}

trait AlarmData{
  def toJson:JsValue
}

//summary
case class DetailedAlarmData (summary:String) extends AlarmData{
  def toJson:JsValue = {
    JsObject("Summary" -> JsString(summary))
  }
}


case class ConciseAlarmData(
  treeInfo: String,
  hostName: String,
  key: List[String],
  timestamp:Long,
  localProbThreshold:Float,
  ppmTreeNodeAlarms: List[PpmTreeNodeAlarm]
//  localProb: Float,
//  globalProb: Float,
//  count: Int,
//  siblingPop: Int,
//  parentCount: Int,
//  depthOfLocalProbabilityCalculation: Int
  )extends AlarmData{
  def toJson:JsValue = {
    val delim = "∫"

    JsObject(
      "Hostname" -> JsString(this.hostName),
      "Tree" -> JsString(this.treeInfo),
      "ShortSummary" -> JsString(this.key.reduce(_ +s" $delim "+ _)),
      "Timestamp" -> JsNumber(this.timestamp),
      "LocalProbThreshold" -> JsNumber(this.localProbThreshold),
      "ppmTreeNodeAlarms" -> this.ppmTreeNodeAlarms.toJson
    )
  }
}

case class Message(
                    data:AlarmData,
                    metadata:MetaData
                       ) {

  def toJson: JsValue = {
    JsObject(
      "Metadata" -> metadata.toJson,
      "Alarm" -> data.toJson
    )
  }
}

case object Message{
  implicit val executionContext = Application.system.dispatcher

  def conciseMessagefromAnAlarm(treeName:String, hostName:String, a: AnAlarm, setProcessDetails: Set[ProcessDetails], localProbThreshold:Float):Message = {
    //key
    val key: List[String] = a.key
    //details:(timestamp, System.currentTimeMillis, alarm, setNamespacedUuidDetails, Map.empty[String,Int]): (Long, Long, Alarm, Set[NamespacedUuidDetails], Map[String, Int])
    val ppmTreeNodeAlarms: List[NoveltyDetection.PpmTreeNodeAlarm] = a.details._3

    val summaries: Set[Future[TreeRepr]] = setProcessDetails.map(processDetails => PpmSummarizer.summarize(processDetails.processName, None, processDetails.pid))
    summaries.map { summary =>
      summary.map { tree =>
        val s = tree.toString(0)

        //if (alarmConfig.logging.enabled) reportLog(a, summary);
        //      if (alarmConfig.console.enabled) reportConsole(key.fold(treeStr)(_ + _));
        //if (alarmConfig.splunk.enabled) reportSplunk(a, summary);
        //if (alarmConfig.gui.enabled) collectAlarms(a, summary);
      }
    }
    Message(
      ConciseAlarmData(treeName, hostName, key, a.details._1, localProbThreshold, ppmTreeNodeAlarms),
      MetaData("default", "concise"))
  }

  def summaryMessagefromAnAlarm(treeName:String, a: AnAlarm, processDetails:ProcessDetails):Message = {
    //key
    val key: List[String] = a.key
    //details:(timestamp, System.currentTimeMillis, alarm, setNamespacedUuidDetails, Map.empty[String,Int]): (Long, Long, Alarm, Set[NamespacedUuidDetails], Map[String, Int])
    val ppmTreeNodeAlarms: List[NoveltyDetection.PpmTreeNodeAlarm] = a.details._3

    val summary: Future[TreeRepr] = PpmSummarizer.summarize(processDetails.processName, None, processDetails.pid)
    summary.map{tree =>
      val s = tree.toString(0)

      //if (alarmConfig.logging.enabled) reportLog(a, summary);
      //      if (alarmConfig.console.enabled) reportConsole(key.fold(treeStr)(_ + _));
      //if (alarmConfig.splunk.enabled) reportSplunk(a, summary);
      //if (alarmConfig.gui.enabled) collectAlarms(a, summary);
    }
    Message(
      DetailedAlarmData(""),
      MetaData("default", "concise"))
  }
}

case class AddAlarmSummary(a: Message)
case class FlushAlarmSummaries()

class SplunkActor(splunkHecClient:SplunkHecClient) extends Actor {

  var alarmSummaryBuffer:List[Message] = List.empty[Message]

  def receive = {
    case FlushAlarmSummaries => flush()
    case AddAlarmSummary(a) => Add(a)
  }

  val jsonAst = List(1).toJson

  def flush() = {
    //todo: retry on failure
    alarmSummaryBuffer.map(reportSplunk)
    alarmSummaryBuffer = List.empty
  }
  def Add(a:Message) = {
    alarmSummaryBuffer = a::alarmSummaryBuffer
  }

  def reportSplunk(a: Message) = {
    splunkHecClient.sendEvent(a.toJson)(Application.system.dispatcher)
  }

}



//topic name
// hostname
// alarm cateogories: realtime, batched summaries: Process name+pid,batched summaries: Process name
//experiment prefix [run number]
//summarized for process and summarized for process /\ pid
// sometimes the processname is <unnamed>, handle that!
//add data timestamp: a._3


object AlarmReporter extends LazyLogging {
  implicit val executionContext = Application.system.dispatcher
  //val log: LoggingAdapter = Logging.getLogger(system, logSource = this)
  val alarmConfig = AdaptConfig.alarmConfig
  var allAlarms = List.empty[AnAlarm]

  val splunkHecClient: SplunkHecClient = new SplunkHecClient(alarmConfig.splunk.token, alarmConfig.splunk.host, alarmConfig.splunk.port)

//  def dummyReporter(a:Alarm) = {}
//  val reporters = List(
//    if (alarmConfig.splunk.enabled) reportSplunk _ else dummyReporter _,
//    if (alarmConfig.logging.enabled) logSplunk _ else dummyReporter _
//  )

  val splunkActor = Application.system.actorOf(Props(classOf[SplunkActor], splunkHecClient))

  def splunkFlushTask = Application.system.scheduler.schedule(50 milliseconds, 50 milliseconds, splunkActor, FlushAlarmSummaries)

  def initialize() = {
    if (alarmConfig.splunk.bufferlength > 0){
      splunkFlushTask
    }
  }

  def deinit() = {
    splunkFlushTask.cancel()
  }


  //type Alarm = List[(String, Float, Float, Int, Int, Int, Int)]
  //println(alarmConfig)

  def reportLog(a: Message) = {
    //log.info(a.toString)
  }

  def reportConsole(a: Message) = {
    println(a)
  }


  //  def collectAlarms(a: AlarmSummary) = {
//    allAlarms = a::AlarmSummary
//  }


  def report(treeName:String, hostName:String, a: AnAlarm, setProcessDetails: Set[ProcessDetails], localProbThreshold:Float) = {

    val alarmSummary = Message.conciseMessagefromAnAlarm(treeName, hostName, a, setProcessDetails, localProbThreshold)

    if (alarmConfig.console.enabled) reportConsole(alarmSummary);
    if (alarmConfig.logging.enabled) reportLog(alarmSummary);
    if (alarmConfig.splunk.enabled) {
      splunkActor ! AddAlarmSummary(alarmSummary)
      if (alarmConfig.splunk.bufferlength <= 0){
        splunkActor ! FlushAlarmSummaries
      }
    }
  }
}
//system.scheduler.scheduleOnce