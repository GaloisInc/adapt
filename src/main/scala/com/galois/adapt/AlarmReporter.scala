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

import scala.concurrent.duration._
import scala.util.{Failure, Success}

import akka.actor.{Actor, ActorLogging, Props}
import com.galois.adapt.NoveltyDetection.PpmTreeNodeAlarm
import spray.json._
import spray.json.DefaultJsonProtocol._

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future
import Application.system
import ApiJsonProtocol._
import com.galois.adapt.AdaptConfig.HostName

case class ProcessDetails (processName: String, pid: Option[Int], hostName: HostName)

case class AlarmMetaData (runID: String, alarmCategory: String)

trait AlarmData {
  def toJson: JsValue
}

//summary
case class DetailedAlarmData (processName: String, pid: String, details: String) extends AlarmData {
  def toJson: JsValue = {
    JsObject(
      "processName" -> JsString(processName),
      "pid" -> JsString(pid),
      "details" -> JsString(details)
    )
  }
}


case class ConciseAlarmData (
  treeInfo: String,
  hostName: HostName,
  key: List[String],
  timestamp: Long,
  localProbThreshold: Float,
  ppmTreeNodeAlarms: List[PpmTreeNodeAlarm],
  alarmID: Long) extends AlarmData {
  def toJson: JsValue = {
    val delim = "∫"

    JsObject(
      "hostName" -> JsString(this.hostName),
      "tree" -> JsString(this.treeInfo),
      "shortSummary" -> JsString(this.key.reduce(_ + s" $delim " + _)),
      "timestamp" -> JsNumber(this.timestamp),
      "localProbThreshold" -> JsNumber(this.localProbThreshold),
      "ppmTreeNodeAlarms" -> JsArray((this.ppmTreeNodeAlarms.map(_.toJson)).toVector),
      "alarmID" -> JsNumber(this.alarmID)
    )
  }
}

case class AlarmMessage (
  data: AlarmData,
  metadata: AlarmMetaData
) {

  def toJson: JsValue = {
    JsObject(
      "metadata" -> metadata.toJson,
      "alarm" -> data.toJson
    )
  }
}


case object AlarmMessage {
  implicit val executionContext = system.dispatcher

  def fromRawAlarm (receivedAlarm: ReceivedAlarm, runID: String, alarmID: Long): AlarmMessage = {
    //key
    val key: List[String] = receivedAlarm.a.key
    //details:(timestamp, System.currentTimeMillis, alarm, setNamespacedUuidDetails, Map.empty[String,Int]): (Long, Long, Alarm, Set[NamespacedUuidDetails], Map[String, Int])
    val ppmTreeNodeAlarms: List[NoveltyDetection.PpmTreeNodeAlarm] = receivedAlarm.a.details._3

    AlarmMessage(
      //todo: Fix alarmID
      ConciseAlarmData(receivedAlarm.treeName, receivedAlarm.hostName, key, receivedAlarm.a.details._1, receivedAlarm.localProbThreshold, ppmTreeNodeAlarms, alarmID),
      AlarmMetaData(runID, "raw")
    )
  }

  def fromBatchedAlarm (alarmCategory: AlarmCategory, pd: ProcessDetails, details: String, runID: String): AlarmMessage = {

    AlarmMessage(
      DetailedAlarmData(pd.processName, pd.pid.getOrElse("None").toString, details),
      AlarmMetaData(runID, alarmCategory.toString))
  }
}

sealed trait AlarmCategory

case object ProcessActivity extends AlarmCategory {
  override def toString = "processActivity"
}

case object ProcessSummary extends AlarmCategory {
  override def toString = "processSummary"
}

case object TopTwenty extends AlarmCategory {
  override def toString = "topTwenty"
}

/*
* Valid Messages for the AlarmReporterActor
*
*/

// Adds a concise alarm to the buffer
case class AddConciseAlarm (a: ReceivedAlarm)

// Skip the concise alarm alarm buffer and send the message
case class SendConciseAlarm (a: ReceivedAlarm)

// Flushes the Concise Alarm buffer
case object FlushConciseMessages

// Generate summaries and send
case object SendDetailedMessages


class AlarmReporterActor (runID: String, maxbufferlength: Long, splunkHecClient: SplunkHecClient, alarmConfig: AdaptConfig.AlarmsConfig) extends Actor with ActorLogging {
  implicit val executionContext = system.dispatcher

  var alarmSummaryBuffer: List[AlarmMessage] = List.empty[AlarmMessage]
  var processRefSet: Set[ProcessDetails] = Set.empty
  var alarmCounter: Long = 0

  def genAlarmID () = {
    alarmCounter += 1; alarmCounter
  }

  def receive = {
    case FlushConciseMessages => flush
    case SendDetailedMessages => generateSummaryAndSend
    case AddConciseAlarm(a) => addConciseAlarm(a)
    case SendConciseAlarm(a) => reportSplunk(List(AlarmMessage.fromRawAlarm(a, runID, genAlarmID())))
  }

  //todo: verify any truncation of alarms and ... in the treeRepr.toString
  //todo: handle errors if the ppmSummarizer can't find anything
  //todo: handle <no_subject_path_node>
  //todo: Should we generate summaries with and without pid?

  def generateSummaryAndSend = {

    val batchedMessages: List[Future[AlarmMessage]] = processRefSet.view.map { processDetails => {
      val pd = processDetails
      val summaries: Future[AlarmMessage] = PpmSummarizer.summarize(processDetails.processName, Some(processDetails.hostName), processDetails.pid).map { s =>
        AlarmMessage.fromBatchedAlarm(ProcessSummary, pd, s.readableString, runID)
      }

      val completeTreeRepr = PpmSummarizer.fullTree(processDetails.processName, Some(processDetails.hostName), processDetails.pid).map { a =>
        AlarmMessage.fromBatchedAlarm(ProcessActivity, pd, a.readableString, runID)
      }

      val mostNovel = PpmSummarizer.mostNovelActions(20, processDetails.processName, processDetails.hostName, processDetails.pid).map { nm =>
        AlarmMessage.fromBatchedAlarm(TopTwenty, pd, nm.mkString("\n"), runID)
      }

      (summaries, completeTreeRepr, mostNovel)
    }
    }.toList.flatMap(x => List(x._1, x._2, x._3))

    val x = Future.sequence(batchedMessages).map(reportSplunk)
    x.onComplete {
      case Success(res) => ()
      case Failure(res) => println(s"AlarmReporter: failed with $res!")
    }
    //todo: deletes the set without checking if the reportSplunk succeeded. Add error handling above
    processRefSet = Set.empty
  }

  def flush () = {
    if (alarmSummaryBuffer.nonEmpty) {
      reportSplunk(alarmSummaryBuffer)
      alarmSummaryBuffer = List.empty
    }
  }

  //todo: Process alarm instead of just add
  def addConciseAlarm (a: ReceivedAlarm) = {
    val conciseAlarm = AlarmMessage.fromRawAlarm(a, runID, genAlarmID())
    alarmSummaryBuffer = conciseAlarm :: alarmSummaryBuffer
    processRefSet |= a.processDetailsSet

    if (alarmSummaryBuffer.length > maxbufferlength) {
      flush()
    }
  }

  //todo: How to get back an exception on failure so the send can be retried
  def reportSplunk (messages: List[AlarmMessage]) = splunkHecClient.sendEvents(messages.map(_.toJson))
}

case class ReceivedAlarm (
  treeName: String,
  hostName: HostName,
  a: AnAlarm,
  processDetailsSet: Set[ProcessDetails],
  localProbThreshold: Float
)

object AlarmReporter extends LazyLogging {
  implicit val executionContext = system.dispatcher

  val runID = Application.randomIdentifier
  val alarmConfig: AdaptConfig.AlarmsConfig = AdaptConfig.alarmConfig
  val splunkConfig = AdaptConfig.alarmConfig.splunk
  val splunkHecClient: SplunkHecClient = new SplunkHecClient(splunkConfig.token, splunkConfig.host, splunkConfig.port)

  val alarmReporterActor = system.actorOf(Props(classOf[AlarmReporterActor], runID, splunkConfig.maxbufferlength, splunkHecClient, alarmConfig), "alarmReporter")

  val t1 = splunkConfig.realtimeReportingPeriodSeconds seconds
  val t2 = splunkConfig.detailedReportingPeriodSeconds seconds
  val scheduleConciseMessagesFlush = system.scheduler.schedule(t1, t1, alarmReporterActor, FlushConciseMessages)
  val scheduleDetailedMessages = system.scheduler.schedule(t1, t2, alarmReporterActor, SendDetailedMessages)

  system.registerOnTermination(scheduleDetailedMessages.cancel())
  system.registerOnTermination(scheduleConciseMessagesFlush.cancel())

  def report (treeName: String, hostName: HostName, a: AnAlarm, processDetailsSet: Set[ProcessDetails], localProbThreshold: Float) = {

    val receivedAlarm = ReceivedAlarm(treeName, hostName, a, processDetailsSet, localProbThreshold)

    if (alarmConfig.console.enabled) println(receivedAlarm.toString);
    if (alarmConfig.logging.enabled) logger.info(receivedAlarm.toString);
    if (splunkConfig.enabled) alarmReporterActor ! AddConciseAlarm(receivedAlarm)
  }
}