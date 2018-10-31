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
      "runID" -> JsString(runID),
      "alarmCategory" -> JsString(alarmCategory)
    )
  }
}

trait AlarmData{
  def toJson:JsValue
}

//summary
case class DetailedAlarmData (summary:String) extends AlarmData{
  def toJson:JsValue = {
    JsObject("summary" -> JsString(summary))
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
      "hostName" -> JsString(this.hostName),
      "tree" -> JsString(this.treeInfo),
      "shortSummary" -> JsString(this.key.reduce(_ +s" $delim "+ _)),
      "timestamp" -> JsNumber(this.timestamp),
      "localProbThreshold" -> JsNumber(this.localProbThreshold),
      "ppmTreeNodeAlarms" -> JsArray((this.ppmTreeNodeAlarms.map(_.toJson)).toVector)
    )
  }
}

case class Message(
                    data:AlarmData,
                    metadata:MetaData
                  ) {

  def toJson: JsValue = {
    JsObject(
      "metadata" -> metadata.toJson,
      "alarm" -> data.toJson
    )
  }
}

case object Message{
  implicit val executionContext = Application.system.dispatcher

  def conciseMessagefromAnAlarm(treeName:String, hostName:String, a: AnAlarm, setProcessDetails: Set[ProcessDetails], localProbThreshold:Float, runID: String):Message = {
    //key
    val key: List[String] = a.key
    //details:(timestamp, System.currentTimeMillis, alarm, setNamespacedUuidDetails, Map.empty[String,Int]): (Long, Long, Alarm, Set[NamespacedUuidDetails], Map[String, Int])
    val ppmTreeNodeAlarms: List[NoveltyDetection.PpmTreeNodeAlarm] = a.details._3

    val summaries: Set[Future[TreeRepr]] = setProcessDetails.map(processDetails => PpmSummarizer.summarize(processDetails.processName, None, processDetails.pid))
    summaries.map { summary =>
      summary.map { tree =>
        val s = tree.toString(0)
      }
    }
    Message(
      ConciseAlarmData(treeName, hostName, key, a.details._1, localProbThreshold, ppmTreeNodeAlarms),
      MetaData(runID, "concise"))
  }

  def summaryMessagefromAnAlarm(treeName:String, a: AnAlarm, processDetails:ProcessDetails, runID: String):Message = {
    val key: List[String] = a.key
    //details:(timestamp, System.currentTimeMillis, alarm, setNamespacedUuidDetails, Map.empty[String,Int]): (Long, Long, Alarm, Set[NamespacedUuidDetails], Map[String, Int])
    val ppmTreeNodeAlarms: List[NoveltyDetection.PpmTreeNodeAlarm] = a.details._3

    val summary: Future[TreeRepr] = PpmSummarizer.summarize(processDetails.processName, None, processDetails.pid)
    summary.map{tree =>
      val s = tree.toString(0)
    }
    Message(
      DetailedAlarmData(""),
      MetaData(runID, "summary"))
  }
}

// alarm cateogories: realtime, batched summaries: Process name+pid,batched summaries: Process name
// summarized for process and summarized for process /\ pid
// sometimes the processname is <unnamed>, handle that!


/*
* Valid Messages for the AlarmReporterActor
*
*/

// Adds a concise alarm to the buffer
case class AddConciseAlarm(a: Message)
// Skip the concise alarm alarm buffer and send the message
case class SendConciseAlarm(a: Message)
// Flushes the Concise Alarm buffer
case object FlushConciseMessages
// Generate summaries and send
case object SendDetailedMessages


//todo: Make this an object??
class AlarmReporterActor(maxBufferLen:Int, splunkHecClient:SplunkHecClient, alarmConfig: AdaptConfig.AlarmsConfig) extends Actor with ActorLogging{
  implicit val executionContext = Application.system.dispatcher
  var alarmSummaryBuffer:List[Message] = List.empty[Message]

  def receive = {
    case FlushConciseMessages => flush
    case SendDetailedMessages => generateSummaryAndSend
    case AddConciseAlarm(a) => addConciseAlarm(a)
    case SendConciseAlarm(a) => reportSplunk(List(a))
  }


  def generateSummaryAndSend = {
    ???
  }

  def flush() = {
    if (!alarmSummaryBuffer.isEmpty) {
      reportSplunk(alarmSummaryBuffer)
      alarmSummaryBuffer = List.empty
    }
  }

  //todo: Process alarm instead of just add
  def addConciseAlarm(a:Message) = {
    alarmSummaryBuffer = a::alarmSummaryBuffer

    if(alarmSummaryBuffer.length > maxBufferLen){
      flush()
    }
  }

  //todo: How to get back an exception on failure so the send can be retried
  def reportSplunk(messages: List[Message]) = splunkHecClient.sendEvents(messages.map(_.toJson))

}

object AlarmReporter extends LazyLogging {
  implicit val executionContext = Application.system.dispatcher

  //todo: push this into application.conf
  val t1 = 1 second
  val t2 = 10 seconds
  val maxBufferLen = 100
  val runID = "testRun"

  val alarmConfig: AdaptConfig.AlarmsConfig = AdaptConfig.alarmConfig
  val splunkHecClient: SplunkHecClient = new SplunkHecClient(alarmConfig.splunk.token, alarmConfig.splunk.host, alarmConfig.splunk.port)

  val alarmReporterActor = Application.system.actorOf(Props(classOf[AlarmReporterActor], maxBufferLen, splunkHecClient, alarmConfig), "alarmReporter")

  //val scheduleDetailedMessages = Application.system.scheduler.schedule(t1, t1, alarmReporterActor, SendDetailedMessages)
  val scheduleConciseMessagesFlush = Application.system.scheduler.schedule(t2, t2, alarmReporterActor, FlushConciseMessages)

  //Application.system.registerOnTermination(scheduleDetailedMessages.cancel())
  Application.system.registerOnTermination(scheduleConciseMessagesFlush.cancel())

  def report(treeName:String, hostName:String, a: AnAlarm, setProcessDetails: Set[ProcessDetails], localProbThreshold:Float) = {

    val alarmSummary = Message.conciseMessagefromAnAlarm(treeName, hostName, a, setProcessDetails, localProbThreshold, runID)

    if (alarmConfig.console.enabled) println(alarmSummary.toString);
    if (alarmConfig.logging.enabled) logger.info(alarmSummary.toString);
    if (alarmConfig.splunk.enabled) alarmReporterActor ! AddConciseAlarm(alarmSummary)
  }
}
