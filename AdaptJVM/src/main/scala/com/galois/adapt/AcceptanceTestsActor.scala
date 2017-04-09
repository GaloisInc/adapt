package com.galois.adapt

import akka.actor.{ActorRef, ActorSystem, Props, Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.util.Timeout
import com.galois.adapt.cdm17._
import com.galois.adapt.scepter._
import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration._
import scala.util.{Try, Failure, Success}
import scala.io.StdIn

import java.io.File
import java.awt.Desktop
import java.net.URI

import scala.language.postfixOps


class AcceptanceTestsActor(val registry: ActorRef)
  extends Actor with ActorLogging with ServiceClient with SubscriptionActor[Nothing] {
  
  import context.dispatcher
 
  val dependencies = "DevDBActor" :: "UIActor" :: Nil
  lazy val subscriptions = Set[Subscription](Subscription(dependencyMap("DevDBActor").get, x => true))

  //implicit val ec = system.dispatcher
  implicit val askTimeout = Timeout(100 seconds)

  def beginService(): Unit = initialize()
  def endService(): Unit = ()


  def statusReport = Map()

  // Some stats to keep
  var failedStatements: Int = 0
  var failedStatementsMsgs: List[String] = Nil // First 5 failed messages
  var instrumentationSource: Option[InstrumentationSource] = None

  override def process = {

    case ErrorReadingFile(path, t) =>
      println(s"Error reading file $path")
      failedStatements += 1
      log.info(s"Error reading file $path")

    case ErrorReadingStatement(t) =>
      log.info("Error reading statement")
      failedStatements += 1
      if (failedStatementsMsgs.length < 10)
        failedStatementsMsgs = t.getMessage :: failedStatementsMsgs
    
    case BeginFile(path, source) =>

      // Set the instrumentation source and, if it is already set, check that it matches
      instrumentationSource match {
        case None => instrumentationSource = Some(source);
        case Some(source2) => assert(source == source2)
      }
      
    case DoneDevDB(graphOpt, incompleteEdges) =>

      println("Beginning to run tests.")

      val graph = graphOpt.get
      var toDisplay = scala.collection.mutable.ListBuffer.empty[String]

      log.info("Done file")
        
      // General tests
      org.scalatest.run(new General_TA1_Tests(
        failedStatements,
        failedStatementsMsgs,
        incompleteEdges,
        graph,
        instrumentationSource,
        toDisplay
      ))
      println("")

      // Provider specific tests
      val providerSpecificTests = instrumentationSource match {
        case Some(SOURCE_ANDROID_JAVA_CLEARSCOPE) => Some(new CLEARSCOPE_Specific_Tests(graph))
        case Some(SOURCE_LINUX_AUDIT_TRACE) => Some(new TRACE_Specific_Tests(graph))
        case Some(SOURCE_FREEBSD_DTRACE_CADETS) => Some(new CADETS_Specific_Tests(graph))
        case Some(SOURCE_WINDOWS_DIFT_FAROS) => Some(new FAROS_Specific_Tests(graph))
        case Some(SOURCE_LINUX_THEIA) => Some(new THEIA_Specific_Tests(graph))
        case Some(SOURCE_WINDOWS_FIVEDIRECTIONS) => Some(new FIVEDIRECTIONS_Specific_Tests(graph))
        case Some(s) => { println(s"No tests for: $s"); None }
        case None => { println("Failed to detect provider"); None }
      }
      providerSpecificTests.foreach(org.scalatest.run(_));

      println(s"Total vertices: ${graph.traversal().V().count().next()}")

      println(s"\nIf any of these test results surprise you, please email Ryan Wright and the Adapt team at: ryan@galois.com\n")
      
      if (toDisplay.nonEmpty) {
        println("Opening up a webserver...")
       
        // Open up the failed tests
        Desktop.getDesktop().browse(new URI("http://0.0.0.0:8080/graph#" + toDisplay.mkString("&")))
        println("To navigate the UI, try right-clicking or double-clicking nodes")
        println("The number in the top right corner of the browser window should be the number of nodes displayed, so if you don't see anything but you have a large number, you may want to try zooming out.")
        println("")
      
        // let it run until user kills the process
        println("Press CTRL^C to kill the webserver")
      }
    }
}

//case class DisplayThese(query: String)
