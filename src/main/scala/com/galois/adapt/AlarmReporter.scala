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
//import com.galois.adapt.Application.system
//import com.galois.adapt.NoveltyDetection.{Alarm, NamespacedUuidDetails}
import spray.json.{JsNumber, JsObject, JsString, JsValue}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future

case class ProcessDetails(processName:String, pid:Option[Int])


//trait AlarmNamespace
//case object DefaultNamespace extends AlarmNamespace
//case object summary extends AlarmNamespace
//case object defaultNamespace extends AlarmNamespace

case class AlarmSummary(
                       treeInfo: String,
                       alarmString: String,
                       namespace: String = "default"
                       ) {

  def toJson:JsValue = {
    JsObject(
      "Metadata" -> JsObject(
        "Namespace" -> JsString(namespace.toString)
      ),
      "Alarm" -> JsObject(
        "Type" -> JsString(this.treeInfo),
        "AlarmInfo" -> JsString(this.alarmString)
        //      "globalProbability" -> JsNumber(this.globalProbability),
        //      "count" -> JsNumber(this.count),
        //      "siblingPop" -> JsNumber(this.siblingPop),
        //      "parentCount" -> JsNumber(this.parentCount),
        //      "depthOfLocalProbabilityCalculation" -> JsNumber(this.depthOfLocalProbabilityCalculation)
      )
    )
  }

}

case class AddAlarmSummary(a: AlarmSummary)
case class FlushAlarmSummaries()

class SplunkActor(splunkHecClient:SplunkHecClient) extends Actor {

  var alarmSummaryBuffer:List[AlarmSummary] = List.empty[AlarmSummary]

  def receive = {
    case FlushAlarmSummaries => flush()
    case AddAlarmSummary(a) => Add(a)
  }

  def flush() = {
    //todo: retry on failure
    alarmSummaryBuffer.map(reportSplunk)
    alarmSummaryBuffer = List.empty
  }
  def Add(a:AlarmSummary) = {
    alarmSummaryBuffer = a::alarmSummaryBuffer
  }

  def reportSplunk(a: AlarmSummary) = {
    splunkHecClient.sendEvent(a.toJson)(Application.system.dispatcher)
  }

}



//topic name
// hostname
// alarm cateogories: realtime, batched summaries: Process name+pid,batched summaries: Process name
//experiment prefix [run number]
//summarized for process and summarized for process /\ pid
// sometimes the processname is <unnamed>, handle that!



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

  def reportLog(a: AlarmSummary) = {
    //log.info(a.toString)
  }

  def reportConsole(a: AlarmSummary) = {
    println(a)
  }




  //  def collectAlarms(a: AlarmSummary) = {
//    allAlarms = a::AlarmSummary
//  }



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