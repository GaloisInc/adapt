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
import akka.actor.Actor
import akka.actor.Props
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
//import com.galois.adapt.Application.system
//import com.galois.adapt.NoveltyDetection.{Alarm, NamespacedUuidDetails}
import spray.json.{JsNumber, JsObject, JsString, JsValue}

import scala.concurrent.Future

case class ProcessDetails(processName:String, pid:Option[Int])

case class AlarmSummary(
                       treeInfo: String,
                       alarmString: String
                       ) {

  def toJson:JsValue = {
    JsObject(
      "Type" -> JsString(this.treeInfo),
      "AlarmInfo" -> JsString(this.alarmString)
//      "globalProbability" -> JsNumber(this.globalProbability),
//      "count" -> JsNumber(this.count),
//      "siblingPop" -> JsNumber(this.siblingPop),
//      "parentCount" -> JsNumber(this.parentCount),
//      "depthOfLocalProbabilityCalculation" -> JsNumber(this.depthOfLocalProbabilityCalculation)
    )
  }

}

case class AddAlarmSummary(a: AlarmSummary)
case class FlushAlarmSummaries()

class SplunkActor(splunkHecClient:SplunkHecClient) extends Actor {

  var alarmSummaryBuffer = List.empty[AlarmSummary]

  def receive = {
    case FlushAlarmSummaries => flush()
    case AddAlarmSummary(a) => Add(a)
  }

  def flush() = {
    alarmSummaryBuffer.map(reportSplunk)
  }
  def Add(a:AlarmSummary) = {
    alarmSummaryBuffer = a::alarmSummaryBuffer
  }

  def reportSplunk(a: AlarmSummary) = {
    splunkHecClient.sendEvent(a.toJson)
  }

}

object AlarmReporter {

  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher

  val log: LoggingAdapter = Logging.getLogger(system, logSource = this)
  val alarmConfig = AdaptConfig.alarmConfig
  var allAlarms = List.empty[AnAlarm]

  val splunkHecClient: SplunkHecClient = new SplunkHecClient(alarmConfig.splunk.token, alarmConfig.splunk.host, alarmConfig.splunk.port)

//  def dummyReporter(a:Alarm) = {}
//  val reporters = List(
//    if (alarmConfig.splunk.enabled) reportSplunk _ else dummyReporter _,
//    if (alarmConfig.logging.enabled) logSplunk _ else dummyReporter _
//  )

  val splunkActor = system.actorOf(Props(classOf[SplunkActor], splunkHecClient))

  def splunkFlushTask = system.scheduler.schedule(50 milliseconds, 50 milliseconds, splunkActor, FlushAlarmSummaries)

  def initialize() = {
    if (alarmConfig.splunk.bufferlength > 0){
      splunkFlushTask
    }
  }

  def deinit() = {
    splunkFlushTask.cancel()
  }


  //type Alarm = List[(String, Float, Float, Int, Int, Int, Int)]
  println(alarmConfig)

  def reportLog(a: AlarmSummary) = {
    log.info(a.toString)
  }

  def reportConsole(a: AlarmSummary) = {
    println(a)
  }




  //  def collectAlarms(a: AlarmSummary) = {
//    allAlarms = a::AlarmSummary
//  }



  //topic name
  // hostname
  // alarm cateogories: realtime, batched summaries: Process name+pid,batched summaries: Process name
  //experiment prefix [run number]
  //summarized for process and summarized for process /\ pid



  def report(treeName:String, a: AnAlarm, processDetails:ProcessDetails) = {

    //key
    val key: List[String] = a.key
    //details:(timestamp, System.currentTimeMillis, alarm, setNamespacedUuidDetails, Map.empty[String,Int]): (Long, Long, Alarm, Set[NamespacedUuidDetails], Map[String, Int])
    val alarm: List[NoveltyDetection.PpmTreeNodeAlarm] = a.details._3

    val summary: Future[TreeRepr] = PpmSummarizer.summarize(processDetails.processName, None, processDetails.pid)
    summary.map{tree =>
      val s = tree.toString(0)

      //if (alarmConfig.logging.enabled) reportLog(a, summary);
//      if (alarmConfig.console.enabled) reportConsole(key.fold(treeStr)(_ + _));
      //if (alarmConfig.splunk.enabled) reportSplunk(a, summary);
      //if (alarmConfig.gui.enabled) collectAlarms(a, summary);
    }

    val alarmSummary = AlarmSummary(treeName, key.reduce(_ + ":" + _))
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