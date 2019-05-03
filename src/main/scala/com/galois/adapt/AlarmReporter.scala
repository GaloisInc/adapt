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

import com.galois.adapt.adm.NamespacedUuid

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
//import spray.json.DefaultJsonProtocol._

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future
import Application.system
import ApiJsonProtocol._
import com.galois.adapt.AdaptConfig.HostName

case class ProcessDetails(processName: String, pid: Option[Int], hostName: HostName, dataTimestamps: Set[Long], uuid: NamespacedUuid) {
  def toJson: JsValue = {
    JsObject(
      "processName" -> JsString(processName),
      "pid" -> JsString(pid.toString),
      "hostName" -> JsString(hostName),
      "dataTimestamps" -> JsArray(dataTimestamps.toList.map(x => JsNumber(x)).toVector),
      "uuid" -> JsString(uuid.rendered)
    )
  }
}

case class AlarmEventMetaData(runID: String, alarmCategory: String)

trait AlarmEventData {
  def toJson: JsValue
}

case class DetailedAlarmEvent(processName: String, pid: String, hostName: HostName, details: String, alarmIDs: Set[Long], dataTimestamps: Set[Long], emitTimestamp: Long) extends AlarmEventData {
  def toJson: JsValue = {
    JsObject(
      "processName" -> JsString(processName),
      "pid" -> JsString(pid),
      "hostName" -> JsString(hostName),
      "referencedRawAlarms" -> JsArray(alarmIDs.map(JsNumber(_)).toVector),
      "details" -> JsString(details),
      "dataTimestamps" -> JsArray(dataTimestamps.toList.map(x => JsNumber(x)).toVector),
      "emitTimestamp" -> JsNumber(this.emitTimestamp)
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

  def fromBatchedAlarm(alarmCategory: AlarmCategory, pd: ProcessDetails, details: String, alarmIDs: Set[Long], runID: String, emitTimestamp: Long): AlarmEvent = {

    AlarmEvent(
      DetailedAlarmEvent(pd.processName, pd.pid.getOrElse("None").toString, pd.hostName, details, alarmIDs, pd.dataTimestamps, emitTimestamp),
      AlarmEventMetaData(runID, alarmCategory.toString))
  }

  def changeCategory(alarmEvent: AlarmEvent, alarmCategory: AlarmCategory): AlarmEvent = {
    AlarmEvent(alarmEvent.data, AlarmEventMetaData(alarmEvent.metadata.runID,alarmCategory.toString))
  }
}

sealed trait AlarmCategory

case object PrioritizedAlarm extends AlarmCategory {
  override def toString = "prioritizedAlarm"
}

case object AggregatedAlarm extends AlarmCategory {
  override def toString = "aggregatedAlarm"
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
  var alarmProcessRefSet: Map[ProcessDetails, Set[(Long, String)]] = Map.empty[ProcessDetails, Set[(Long, String)]]
  var noveltyProcessRefSet: Map[ProcessDetails, Set[(Long, String)]] = Map.empty[ProcessDetails, Set[(Long, String)]]
  var alarmCounter: Long = 0

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

    val numProcessInstancesSeen = (noveltyProcessRefSet.keySet ++ alarmProcessRefSet.keySet).size

    // We take the ceiling so that we always return at least one prioritized process from the novelty process set.
    // If we don't want this, use math.round instead.
    val numProcessInstancesToTake = math.ceil(numProcessInstancesSeen * AlarmReporter.percentProcessInstancesToTake / 100).toInt

    // For each tree that produced at least one alarm, we count the number alarms produced by that tree.
    // Note that a single tree alarm may be counted twice (or more), if two (or more) processes were implicated in that single alarm.
    val treeCountsForBatch = (alarmProcessRefSet.values ++ noveltyProcessRefSet.values)
      .flatMap(alarmIdsAndTreeNames => alarmIdsAndTreeNames.toList.map(_._2))
      .groupBy(identity)
      .mapValues(_.size)

    // We take the top k process instances ranked in descending order by weight.
    // Weight is defined to be the sum over all trees of the proportion of tree alarms raised per process instance.
    val priorityProcessesFromNovelties = noveltyProcessRefSet.toList.map {
      case (pd: ProcessDetails, alarmIdsAndTreeNames: Set[(Long, String)]) =>
        val treeCounts = alarmIdsAndTreeNames.toList.map(_._2).groupBy(identity).mapValues(_.size)
        val rankValue = treeCounts.map { case (tree: String, count: Int) =>
          count * 1.0 / treeCountsForBatch.getOrElse(tree, 1)
        }.sum
      pd -> rankValue
    }.sortBy(-_._2).take(numProcessInstancesToTake).map(_._1)

    // Send all processes implicated by novelty trees to splunk as aggregated alarms.
    val aggregatedAlarmEventsFromNovelties: List[Future[Option[AlarmEvent]]] = noveltyProcessRefSet.map { case (pd, alarmIDsAndTreeNames) =>

      // Include alarmID references to alarm trees here too.
      val alarmIDs: Set[Long] = alarmIDsAndTreeNames.map(_._1)
        .union(alarmProcessRefSet.getOrElse(pd, Set.empty[(Long, String)]).map(_._1))

      Future.successful(Some(AlarmEvent.fromBatchedAlarm(AggregatedAlarm, pd, "", alarmIDs, runID, System.currentTimeMillis)))
    }.toList

    // Send only processes implicated by novelty trees that are highly ranked to as prioritized alarms to Splunk.
    val priorityAlarmEventsFromNovelties: List[Future[Option[AlarmEvent]]] = noveltyProcessRefSet
      .filterKeys(priorityProcessesFromNovelties.contains(_))
      .map { case (pd, alarmIDsAndTreeNames) =>
        // Include references to alarmIDs from alarm trees here too.
        val alarmIDs: Set[Long] = alarmIDsAndTreeNames.map(_._1)
          .union(alarmProcessRefSet.getOrElse(pd, Set.empty[(Long, String)]).map(_._1))

        val prioritizedEvent: Future[Option[AlarmEvent]] = {
          PpmSummarizer.summarize(pd.processName, Some(pd.hostName), pd.pid).map { s =>
            if (s == TreeRepr.empty) None
            else Some(AlarmEvent.fromBatchedAlarm(PrioritizedAlarm, pd, s.readableString, alarmIDs, runID, System.currentTimeMillis))
          }.recoverWith { case e => log.error(s"Summarizing: $pd failed with error: ${e.getMessage}"); Future.failed(e) }
        }

        prioritizedEvent
      }.toList

    // Send all processes implicated from alarm trees to Splunk as `AggregatedAlarm`s and `Prioritized` alarms.
    // Only include processes that we weren't already implicated by novelty trees.
    val alarmEventsFromAlarms: List[Future[Option[AlarmEvent]]] = alarmProcessRefSet
        .filterKeys(!priorityProcessesFromNovelties.contains(_))
        .map { case (pd, alarmIDsAndTreeNames) =>

          val alarmIDs: Set[Long] = alarmIDsAndTreeNames.map(_._1)
            .union(alarmProcessRefSet.getOrElse(pd, Set.empty[(Long, String)]).map(_._1))

          // Don't send two aggregated alarms for the same process
          val aggregatedEvent = if (!noveltyProcessRefSet.contains(pd)) {
            Future.successful(Some(AlarmEvent.fromBatchedAlarm(AggregatedAlarm, pd, "", alarmIDs, runID, System.currentTimeMillis)))
          } else {Future.successful(None)}

          val prioritizedEvent: Future[Option[AlarmEvent]] = {
            PpmSummarizer.summarize(pd.processName, Some(pd.hostName), pd.pid).map { s =>
              if (s == TreeRepr.empty) None
              else Some(AlarmEvent.fromBatchedAlarm(PrioritizedAlarm, pd, s.readableString, alarmIDs, runID, System.currentTimeMillis))
            }.recoverWith{ case e => log.error(s"Summarizing: $pd failed with error: ${e.getMessage}"); Future.failed(e)}
          }

          (aggregatedEvent, prioritizedEvent)
        }.toList.flatMap( x => List(x._1, x._2))


      Future.sequence(aggregatedAlarmEventsFromNovelties ++ priorityAlarmEventsFromNovelties ++ alarmEventsFromAlarms)
        .map(m => handleMessage(m.flatten, lastMessage)).onFailure { case res =>
          log.error(s"AlarmReporter: failed with $res.")
        }

    //todo: deletes the set without checking if the reportSplunk succeeded. Add error handling above
    noveltyProcessRefSet = Map.empty
    alarmProcessRefSet = Map.empty
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
    val tree = alarmDetails.treeName

    //update vars
    alarmSummaryBuffer = conciseAlarm :: alarmSummaryBuffer

    alarmDetails.processDetailsSet.foreach { pd =>
      if (alarmDetails.shouldApplyThreshold) {
        noveltyProcessRefSet += (pd -> (noveltyProcessRefSet.getOrElse(pd, Set.empty[(Long, String)]) + ((alarmID, tree))))
      } else {
        alarmProcessRefSet += (pd -> (alarmProcessRefSet.getOrElse(pd, Set.empty[(Long, String)]) + ((alarmID, tree)) ))
      }
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
  localProbThreshold: Float,
  shouldApplyThreshold: Boolean
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

  def report(treeName: String, hostName: HostName, alarm: AnAlarm, processDetailsSet: Set[ProcessDetails], localProbThreshold: Float, shouldApplyThreshold: Boolean): Unit = {
    if (splunkConfig.enabled || consoleConfig.enabled || guiConfig.enabled)
      alarmReporterActor ! AddConciseAlarm(AlarmDetails(treeName, hostName, alarm, processDetailsSet, localProbThreshold, shouldApplyThreshold))
  }
}