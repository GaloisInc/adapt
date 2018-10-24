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
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import spray.json.{JsObject, JsValue, JsString, JsNumber}

case class Alarm (Key:String, localProbability:Float, globalProbability:Float, count:Int, siblingPop:Int, parentCount:Int, depthOfLocalProbabilityCalculation:Int){
  def toJson:JsValue = {
    JsObject(
      "Key" -> JsString(this.Key),
      "localProbability" -> JsNumber(this.localProbability),
      "globalProbability" -> JsNumber(this.globalProbability),
      "count" -> JsNumber(this.count),
      "siblingPop" -> JsNumber(this.siblingPop),
      "parentCount" -> JsNumber(this.parentCount),
      "depthOfLocalProbabilityCalculation" -> JsNumber(this.depthOfLocalProbabilityCalculation)
    )
  }
}

object AlarmReporter {
  implicit val system = ActorSystem()
  val log: LoggingAdapter = Logging.getLogger(system, logSource = this)
  val alarmConfig = AdaptConfig.alarmConfig
  var allAlarms = List.empty[Alarm]

  val splunkHecClient = new SplunkHecClient(alarmConfig.splunk.token, alarmConfig.splunk.host, alarmConfig.splunk.port)

//  def dummyReporter(a:Alarm) = {}
//  val reporters = List(
//    if (alarmConfig.splunk.enabled) reportSplunk _ else dummyReporter _,
//    if (alarmConfig.logging.enabled) logSplunk _ else dummyReporter _
//  )


  def reportSplunk(a: Alarm) = {
    splunkHecClient.sendEvent(a.toJson)
  }

  //type Alarm = List[(String, Float, Float, Int, Int, Int, Int)]
  println(alarmConfig)

  def reportLog(a: Alarm) = {
    log.info(a.toString)
  }

  def reportConsole(a: Alarm) = {
    println(a)
  }

  def collectAlarms(a: Alarm) = {
    allAlarms = a::allAlarms
  }

  def report(alarms: List[Alarm]) = {
    //reporters.map(_.apply(alarm))

    alarms.map { a =>
      if (alarmConfig.logging.enabled) reportLog(a);
      if (alarmConfig.console.enabled) reportConsole(a);
      if (alarmConfig.splunk.enabled) reportSplunk(a);
      if (alarmConfig.gui.enabled) collectAlarms(a);
    }
  }
}