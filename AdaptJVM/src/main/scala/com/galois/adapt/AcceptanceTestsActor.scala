package com.galois.adapt

import akka.actor.{ActorRef, ActorSystem, Props, Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.util.Timeout
import com.galois.adapt.cdm15._
import com.galois.adapt.scepter._
import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration._
import scala.util.{Try, Failure, Success}
import scala.io.StdIn

import java.io.File
import java.awt.Desktop;
import java.net.URI;

import scala.language.postfixOps


class AcceptanceTestsActor(val registry: ActorRef, val counterActor: ActorRef, val basicOpsActor: ActorRef)
  extends Actor with ActorLogging with ServiceClient with SubscriptionActor[CDM15] {
  
  import context.dispatcher
 
  val dependencies = "DevDBActor" :: "FileIngestActor" :: "UIActor" :: Nil
  lazy val subscriptions = Set[Subscription](Subscription(dependencyMap("FileIngestActor").get, _.isInstanceOf[CDM15]))

  println(s"Spinning up an acceptance system.")

  //implicit val ec = system.dispatcher
  implicit val askTimeout = Timeout(2 seconds)

  lazy val dbActor = dependencyMap("DevDBActor").get
  var ta1Source: Option[InstrumentationSource] = None

  val colors: Iterator[(String,String)] = Iterator.continually(Map(
    "000" -> Console.BLACK,
    "00F" -> Console.BLUE,
    "0F0" -> Console.GREEN,
    "0FF" -> Console.CYAN,
    "F0F" -> Console.MAGENTA,
    "FF0" -> Console.YELLOW
  )).flatten

  def beginService(): Unit = initialize()
  def endService(): Unit = ()

  // Some stats to keep
  var failedStatements: Int = 0
  var failedStatementsMsgs: List[String] = Nil // First 5 failed messages
  var successStatements: Int = 0

  val toDisplay = scala.collection.mutable.ListBuffer.empty[String]

  override def process = {

    case ErrorReadingStatement(t) =>
      log.info("Error reading statement")
      failedStatements += 1
      if (failedStatementsMsgs.length < 10)
        failedStatementsMsgs = t.getMessage :: failedStatementsMsgs
    
    case c: CDM15 =>
      log.info("Read CDM15")
      successStatements += 1
      broadCast(c)

    case DisplayThese(q) =>
      val (code,color) = colors.next()
      log.info("Display request")
      sender() ! color
      toDisplay += q + ":" + code
    
    case DoneFile(f) => // TODO "ask" the counting actors and then start running the actual FlatSpec tests,  roughly what follows
      
      val oldFailedStatements = failedStatements
      val oldFailedStatementsMsgs = failedStatementsMsgs
      val oldSuccessStatements = successStatements
      val oldTa1Source = ta1Source

      failedStatements = 0
      failedStatementsMsgs = Nil
      successStatements = 0
      ta1Source = None

      Future { 
        log.info("Done file")
        lazy val missingEdgeCount = Await.result(dbActor ? Shutdown, 2 seconds).asInstanceOf[Int]
        lazy val finalCount = Await.result(dbActor ? HowMany("total"), 2 seconds).asInstanceOf[Int]
        
        // General tests
        org.scalatest.run(new General_TA1_Tests(
          oldFailedStatements,
          oldFailedStatementsMsgs,
          oldSuccessStatements,
          missingEdgeCount,
          dbActor,
          counterActor,
          basicOpsActor,
          self,
          oldTa1Source,
          None
        ))
        println("")

        // Provider specific tests
        val providerSpecificTests = ta1Source match {
          case Some(SOURCE_ANDROID_JAVA_CLEARSCOPE) => Some(new CLEARSCOPE_Specific_Tests(finalCount, counterActor))
          case Some(SOURCE_LINUX_AUDIT_TRACE) => Some(new TRACE_Specific_Tests(finalCount, counterActor))
          case Some(SOURCE_FREEBSD_DTRACE_CADETS) => Some(new CADETS_Specific_Tests(finalCount, counterActor))
          case Some(SOURCE_WINDOWS_DIFT_FAROS) => Some(new FAROS_Specific_Tests(finalCount, counterActor))
          case Some(SOURCE_LINUX_THEIA) => Some(new THEIA_Specific_Tests(finalCount, counterActor))
          case Some(SOURCE_WINDOWS_FIVEDIRECTIONS) => Some(new FIVEDIRECTIONS_Specific_Tests(finalCount, counterActor))
          case Some(s) => { println(s"No tests for: $s"); None }
          case None => { println("Failed to detect provider"); None }
        }
        providerSpecificTests.foreach(org.scalatest.run(_));

        println(s"Total vertices: $finalCount")

        println(s"\nIf any of these test results surprise you, please email Ryan Wright and the Adapt team at: ryan@galois.com\n")
      
        if (toDisplay.length > 0) {
          println("Opening up a webserver...")
         
          // Open up the failed tests
          Desktop.getDesktop().browse(new URI("http://localhost:8080/#" + toDisplay.mkString("&"))) 
          println("To navigate the UI, try right-clicking or double-clicking nodes")
          println("The number in the top right corner of the browser window should be the number of nodes displayed, so if you don't see anything but you have a large number, you may want to try zooming out.")
          println("")
      
          // let it run until user presses return
          println("Press CTRL^C to kill the webserver")
        }
      } onFailure { case f: Throwable => 
        println("The tests crashed in some unexpected manner! This should never happen: it is a bug.")
        println("")
        println(f.getMessage)
      }
  }
}

case class DisplayThese(query: String)
