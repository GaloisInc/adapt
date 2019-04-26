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
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import com.galois.adapt.NoveltyDetection.PpmTreeNodeAlarm
import spray.json._
import java.io.{File, FileOutputStream, PrintWriter}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
//import spray.json.DefaultJsonProtocol._

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future
import Application.system
import ApiJsonProtocol._
import com.galois.adapt.AdaptConfig.HostName

case class ProcessDetails(processName: String, pid: Option[Int], hostName: HostName, dataTimestamps: Set[Long]) {
  def toJson: JsValue = {
    JsObject(
      "processName" -> JsString(processName),
      "pid" -> JsString(pid.toString),
      "hostName" -> JsString(hostName),
      "dataTimestamps" -> JsArray(dataTimestamps.toList.map(x => JsNumber(x)).toVector)
    )
  }
}

case class AlarmEventMetaData(runID: String, alarmCategory: String)

trait AlarmEventData {
  def toJson: JsValue
}

case class DetailedAlarmEvent(processName: String, pid: String, hostName: HostName, details: String, alarmIDs: Set[Long], dataTimestamps: Set[Long]) extends AlarmEventData {
  def toJson: JsValue = {
    JsObject(
      "processName" -> JsString(processName),
      "pid" -> JsString(pid),
      "hostName" -> JsString(hostName),
      "referencedRawAlarms" -> JsArray(alarmIDs.map(JsNumber(_)).toVector),
      "details" -> JsString(details),
      "alarmIDs" -> JsArray(alarmIDs.toList.map(x => JsNumber(x)).toVector),
      "dataTimestamps" -> JsArray(dataTimestamps.toList.map(x => JsNumber(x)).toVector)
    )
  }
}

case class ConciseAlarmEvent(
  treeInfo          : String,
  hostName          : HostName,
  key               : List[String],
  dataTimestamp     : Long,
  emitTimestamp     : Long,
  localProbThreshold: Float,
  ppmTreeNodeAlarms : List[PpmTreeNodeAlarm],
  alarmID           : Long,
  processDetails    : Set[ProcessDetails])
  extends AlarmEventData {
  def toJson: JsValue = {
    val delim = "∫"

    JsObject(
      "hostName" -> JsString(this.hostName),
      "tree" -> JsString(this.treeInfo),
      "shortSummary" -> JsString(this.key.reduce(_ + s" $delim " + _)),
      "dataTimestamp" -> JsNumber(this.dataTimestamp),
      "emitTimestamp" -> JsNumber(this.emitTimestamp),
      "localProbThreshold" -> JsNumber(this.localProbThreshold),
      "ppmTreeNodeAlarms" -> JsArray(this.ppmTreeNodeAlarms.map(_.toJson).toVector),
      "alarmID" -> JsNumber(this.alarmID),
      "processDetails" -> JsArray(processDetails.map(_.toJson).toVector)
    )
  }
}

case class AlarmEvent(
  data    : AlarmEventData,
  metadata: AlarmEventMetaData
) {

  def toJson: JsValue = {
    JsObject(
      "metadata" -> metadata.toJson,
      "alarm" -> data.toJson
    )
  }
}


case object AlarmEvent {
  implicit val executionContext: ExecutionContext = Application.system.dispatchers.lookup("quine.actor.node-dispatcher")

  def fromRawAlarm(alarmDetails: AlarmDetails, runID: String, alarmID: Long): AlarmEvent = {
    //key
    val key: List[String] = alarmDetails.alarm.key
    //details:(timestamp, System.currentTimeMillis, alarm, setNamespacedUuidDetails, Map.empty[String,Int]): (Long, Long, Alarm, Set[NamespacedUuidDetails], Map[String, Int])
    val ppmTreeNodeAlarms: List[NoveltyDetection.PpmTreeNodeAlarm] = alarmDetails.alarm.details._3

    AlarmEvent(
      ConciseAlarmEvent(alarmDetails.treeName, alarmDetails.hostName, key, alarmDetails.alarm.details._1.min, alarmDetails.alarm.details._2, alarmDetails.localProbThreshold, ppmTreeNodeAlarms, alarmID, alarmDetails.processDetailsSet),
      AlarmEventMetaData(runID, "raw")
    )
  }

  def fromBatchedAlarm(alarmCategory: AlarmCategory, pd: ProcessDetails, details: String, alarmIDs: Set[Long], runID: String): AlarmEvent = {

    AlarmEvent(
      DetailedAlarmEvent(pd.processName, pd.pid.getOrElse("None").toString, pd.hostName, details, alarmIDs, pd.dataTimestamps),
      AlarmEventMetaData(runID, alarmCategory.toString))
  }

  def changeCategory(alarmEvent: AlarmEvent, alarmCategory: AlarmCategory): AlarmEvent = {
    AlarmEvent(alarmEvent.data, AlarmEventMetaData(alarmEvent.metadata.runID,alarmCategory.toString))
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

case object ProcessFiltered extends AlarmCategory {
  override def toString = "processFiltered"
}
/*
* Valid Messages for the AlarmReporterActor
*
*/

// Adds a concise alarm to the buffer
case class AddConciseAlarm(a: AlarmDetails)

// Skip the concise alarm alarm buffer and send the message
//case class SendConciseAlarm (a: AlarmDetails)

// Flushes the Concise Alarm buffer
case object FlushConciseMessages

// Generate summaries and send
case object SendDetailedMessages

//log the alarm
case class LogAlarm(alarmEvents: List[AlarmEvent])

class AlarmReporterActor(runID: String, maxbufferlength: Long, splunkHecClient: SplunkHecClient, alarmConfig: AdaptConfig.AlarmsConfig, logFilenamePrefix: String) extends Actor with ActorLogging {
  implicit val executionContext: ExecutionContext = Application.system.dispatchers.lookup("quine.actor.node-dispatcher")


  val pwAllAlarms: PrintWriter = new PrintWriter(new FileOutputStream(new File(logFilenamePrefix + "All"), true))
  //val pwRawAlarm: PrintWriter = new PrintWriter(new FileOutputStream(new File(logFilenamePrefix + "Raw"),true))
  //val pwDetailedAlarm: PrintWriter = new PrintWriter(new FileOutputStream(new File(logFilenamePrefix + "DetailedAlarm"),true))
  //val pwProcessActivity: PrintWriter = new PrintWriter(new FileOutputStream(new File(logFilenamePrefix + "ProcessActivity"),true))
  //val pwProcessSummary: PrintWriter = new PrintWriter(new FileOutputStream(new File(logFilenamePrefix + "ProcessSummary"),true))
  //val pwTopTwenty: PrintWriter = new PrintWriter(new FileOutputStream(new File(logFilenamePrefix + "TopTwenty"),true))

  val pw = List(pwAllAlarms)
  pw.foreach(_.println(s"runID: $runID"))
  val numMostNovel = 20
  var alarmSummaryBuffer: List[AlarmEvent] = List.empty[AlarmEvent]
  //var processRefSet: Set[ProcessDetails] = Set.empty
  var processRefSet: Map[ProcessDetails, Set[Long]] = Map.empty
  var alarmCounter: Long = 0

  // TODO: Initialize processInstanceCounter with training data; add new function to receive.
  var processInstanceCounter = scala.collection.mutable.Map[ProcessDetails, Int]().withDefaultValue(0)
  var distinctProcessInstanceCount = processInstanceCounter.keys.size

  def genAlarmID(): Long = {
    alarmCounter += 1
    alarmCounter
  }

  def receive: PartialFunction[Any, Unit] = {
    case FlushConciseMessages => flushConciseAlarms(false)
    case SendDetailedMessages => generateSummaryAndSend(lastMessage = false)
    case AddConciseAlarm(alarmDetails) => addConciseAlarm(alarmDetails)
    //case SendConciseAlarm(alarmDetails) => reportSplunk(List(AlarmEvent.fromRawAlarm(alarmDetails, runID, genAlarmID())))
    case LogAlarm(alarmEvents: List[AlarmEvent]) => logAlarm(alarmEvents)
    case unknownMessage => log.error(s"Received unknown message: $unknownMessage")
  }

  def logAlarm(alarmEvents: List[AlarmEvent]): Unit = {
    //if (alarmConfig.logging.enabled) logger.info(alarmDetails.toString)
    val messageString = alarmEvents.map(_.toJson).mkString("\n")
    
    if (alarmConfig.splunk.enabled) reportSplunk(alarmEvents)
    if (alarmConfig.logging.enabled) pw.foreach(_.println(messageString))
    if (alarmConfig.console.enabled) println(messageString)
  }

  def handleMessage(m: List[AlarmEvent], lastMessage: Boolean = false): Unit = if (lastMessage) logAlarm(m) else self ! LogAlarm(m)

  //todo: handle <no_subject_path_node>?

  def generateSummaryAndSend(lastMessage: Boolean): Unit = {

    val numProcessInstancesToTake = math.round(distinctProcessInstanceCount * AlarmReporter.percentProcessInstancesToTake)
    val minProcessInstanceCount = processInstanceCounter.toList.sortBy(-_._2).take(numProcessInstancesToTake).lastOption.map(_._2).getOrElse(0)

    val batchedMessages: List[Future[Option[AlarmEvent]]] = processRefSet.view.map { case (pd, alarmIDs) =>

      //Suppress empty summaries
      val summaries: Future[Option[AlarmEvent]] = PpmSummarizer.summarize(pd.processName, Some(pd.hostName), pd.pid).map { s =>
        if (s == TreeRepr.empty) None
        else Some(AlarmEvent.fromBatchedAlarm(ProcessSummary, pd, s.readableString, alarmIDs, runID))
      }.recoverWith{ case e => log.error(s"Summarizing: $pd failed with error: ${e.getMessage}"); Future.failed(e)}

      // Filter out process instance summaries that don't have a lot of activity
      val filteredSummary: Future[Option[AlarmEvent]] = processInstanceCounter.getOrElse(pd, 0) match {
        case cnt if cnt >= minProcessInstanceCount =>
          summaries.map(_.map(x => AlarmEvent.changeCategory(x,ProcessFiltered)))
        case _ => Future{None}
      }

      //it is OK to have empty process Activities
      val completeTreeRepr: Future[Option[AlarmEvent]] = PpmSummarizer.fullTree(pd.processName, Some(pd.hostName), pd.pid).map { a =>
        Some(AlarmEvent.fromBatchedAlarm(ProcessActivity, pd, a.withoutQNodes.readableString, alarmIDs, runID))
      }.recoverWith{ case e => log.error(s"Getting Full Tree for: $pd failed with error: ${e.getMessage}"); Future.failed(e)}

      //Suppress empty summaries
      val mostNovel: Future[Option[AlarmEvent]] = PpmSummarizer.mostNovelActions(numMostNovel, pd.processName, pd.hostName, pd.pid).map { mn =>
        if (mn.isEmpty) None
        else Some(AlarmEvent.fromBatchedAlarm(TopTwenty, pd, mn.mkString("\n"), alarmIDs, runID))
      }.recoverWith{ case e => log.error(s"Getting Top 20 for: $pd failed with error: ${e.getMessage}"); Future.failed(e)}

      (summaries, filteredSummary, completeTreeRepr, mostNovel)
    }.toList.flatMap(x => List(x._1, x._2, x._3, x._4))

    Future.sequence(batchedMessages).map(m => handleMessage(m.flatten, lastMessage)).onFailure {
      case res => log.error(s"AlarmReporter: failed with $res.")
    }

    //todo: deletes the set without checking if the reportSplunk succeeded. Add error handling above
    processRefSet = Map.empty
  }

  def flushConciseAlarms(lastMessage: Boolean): Unit = {
    if (alarmSummaryBuffer.nonEmpty) {
      handleMessage(alarmSummaryBuffer, lastMessage)
    }
    alarmSummaryBuffer = List.empty
  }

  def addConciseAlarm(alarmDetails: AlarmDetails): Unit = {

    val alarmID = genAlarmID()
    val conciseAlarm: AlarmEvent = AlarmEvent.fromRawAlarm(alarmDetails, runID, alarmID)

    //update vars
    alarmSummaryBuffer = conciseAlarm :: alarmSummaryBuffer

    alarmDetails.processDetailsSet.foreach { pd =>
      processRefSet += (pd -> (processRefSet.getOrElse(pd, Set.empty[Long]) + alarmID))
      processInstanceCounter(pd) += 1
      distinctProcessInstanceCount =  if (processInstanceCounter.keySet.contains(pd)) distinctProcessInstanceCount else distinctProcessInstanceCount+1
    }

    // is this a cleaner solution?
    //    val tmp: Map[ProcessDetails, List[Long]] = alarmDetails.processDetailsSet.view.map{pd =>
    //      pd -> (processRefSet.getOrElse(pd, List(alarmID)) ++ processRefSet(pd))
    //    }.toMap
    // Now use scalaz to merge the maps!

    if (alarmSummaryBuffer.length > maxbufferlength) {
      flushConciseAlarms(false)
    }
  }

  def reportSplunk(messages: List[AlarmEvent]): Unit = {
    val messagesJson: List[JsValue] = messages.map(_.toJson)
    splunkHecClient.sendEvents(messagesJson)
    //summaries.foreach(pwProcessSummary.println)
    //completeTreeRepr.foreach(pwProcessActivity.println)
    //mostNovel.foreach(pwTopTwenty.println)
    //alarmSummaryBuffer.foreach(pwRawAlarm.println)
  }

  override def postStop(): Unit = {
    flushConciseAlarms(true)
    generateSummaryAndSend(true)
    //close the file
    pw.foreach(_.close)
    super.postStop()
  }
}

case class AlarmDetails(
  treeName: String,
  hostName: HostName,
  alarm: AnAlarm,
  processDetailsSet: Set[ProcessDetails],
  localProbThreshold: Float
)

object AlarmReporter extends LazyLogging {
  implicit val executionContext: ExecutionContext = Application.system.dispatchers.lookup("quine.actor.node-dispatcher")

  val runID: String = Application.randomIdentifier
  val alarmConfig: AdaptConfig.AlarmsConfig = AdaptConfig.alarmConfig
  val splunkConfig: AdaptConfig.SplunkConfig = alarmConfig.splunk
  val splunkHecClient: SplunkHecClient = SplunkHecClient(splunkConfig.token, splunkConfig.host, splunkConfig.port)

  val guiConfig: AdaptConfig.GuiConfig = alarmConfig.gui
  val consoleConfig: AdaptConfig.ConsoleConfig = alarmConfig.console

  //Assumes ppmConfig.basedir is already created
  val logFilenamePrefix: String = AdaptConfig.ppmConfig.basedir + alarmConfig.logging.fileprefix

  val percentProcessInstancesToTake = splunkConfig.percentProcessInstancesToTake

  val alarmReporterActor: ActorRef = system.actorOf(Props(classOf[AlarmReporterActor], runID, splunkConfig.maxbufferlength, splunkHecClient, alarmConfig, logFilenamePrefix), "alarmReporter")

  val t1: FiniteDuration = splunkConfig.realtimeReportingPeriodSeconds seconds
  val t2: FiniteDuration = splunkConfig.detailedReportingPeriodSeconds seconds
  val scheduleConciseMessagesFlush: Cancellable = system.scheduler.schedule(t1, t1, alarmReporterActor, FlushConciseMessages)
  val scheduleDetailedMessages: Cancellable = system.scheduler.schedule(t1, t2, alarmReporterActor, SendDetailedMessages)

  system.registerOnTermination(scheduleDetailedMessages.cancel())
  system.registerOnTermination(scheduleConciseMessagesFlush.cancel())

  def report(treeName: String, hostName: HostName, alarm: AnAlarm, processDetailsSet: Set[ProcessDetails], localProbThreshold: Float): Unit = {
    if (splunkConfig.enabled || consoleConfig.enabled || guiConfig.enabled)
      alarmReporterActor ! AddConciseAlarm(AlarmDetails(treeName, hostName, alarm, processDetailsSet, localProbThreshold))
  }
}