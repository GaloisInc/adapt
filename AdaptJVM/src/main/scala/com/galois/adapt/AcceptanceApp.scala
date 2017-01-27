package com.galois.adapt

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.util.Timeout
import com.galois.adapt.cdm13._
import com.galois.adapt.scepter._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try
import scala.io.StdIn

import java.io.File
import java.awt.Desktop;
import java.net.URI;

import scala.language.postfixOps

object AcceptanceApp {
  println(s"Spinning up an acceptance system.")

  val config = Application.config  // .withFallback(ConfigFactory.load("acceptance"))
  implicit val system = ActorSystem("acceptance-actor-system")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val askTimeout = Timeout(2 seconds)

  val dbActor = system.actorOf(Props(classOf[DevDBActor], None))
  val counterActor = system.actorOf(Props(new EventCountingTestActor()))
  val basicOpsActor = system.actorOf(Props(new BasicOpsIdentifyingActor()))
  var ta1Source: Option[InstrumentationSource] = None

  val toDisplay = scala.collection.mutable.ListBuffer.empty[String]

  def run(
    loadPaths: List[String],
    count: Option[Int] = None
  ) {

    /* Get all of the files on the load paths. Each load path should either
     *  - be itself a data file
     *  - be a directory full of data files
     */
    val filePaths: List[String] = loadPaths.map(new File(_)).flatMap(path =>
      if (path.isDirectory)
        path.listFiles.toList.map(_.getPath)
      else
        List(path.getPath)
    )

    for (filePath <- filePaths) {
      println(s"Processing $filePath...")
      Try {
        val data = CDM13.readData(filePath, count).get
        
        lazy val missingEdgeCount = Await.result(dbActor ? Shutdown, 2 seconds).asInstanceOf[Int]
        lazy val finalCount = Await.result(dbActor ? HowMany("total"), 2 seconds).asInstanceOf[Int]
        
        // General tests
        org.scalatest.run(new General_TA1_Tests(data, missingEdgeCount, count))

        println("")

        // Provider specific tests
        val providerSpecificTests = ta1Source match {
          case Some(SOURCE_ANDROID_JAVA_CLEARSCOPE) => Some(new CLEARSCOPE_Specific_Tests(finalCount))
          case Some(SOURCE_LINUX_AUDIT_TRACE) => Some(new TRACE_Specific_Tests(finalCount))
          case Some(SOURCE_FREEBSD_DTRACE_CADETS) => Some(new CADETS_Specific_Tests(finalCount))
          case Some(SOURCE_WINDOWS_DIFT_FAROS) => Some(new FAROS_Specific_Tests(finalCount))
          case Some(SOURCE_LINUX_THEIA) => Some(new THEIA_Specific_Tests(finalCount))
          case Some(SOURCE_WINDOWS_FIVEDIRECTIONS) => Some(new FIVEDIRECTIONS_Specific_Tests(finalCount))
          case Some(s) => { println(s"No tests for $s"); None }
          case None => { println("Failed to detect provider"); None }
        }
        providerSpecificTests.foreach(org.scalatest.run(_));

        println(s"Total vertices: $finalCount")
      }
      ta1Source = None
    }

    println(s"\nIf any of these test results surprise you, please email Ryan Wright and the Adapt team at: ryan@galois.com\n")
    
    if (config.getBoolean("adapt.webserver")) {
      val interface = config.getString("akka.http.server.interface")
      val port = config.getInt("akka.http.server.port")
      
      println("Opening up a webserver...")
      val httpServiceFuture = Http().bindAndHandle(Routes.mainRoute(dbActor), interface, port).map { f =>
        println("Server online at http://localhost:8080/")
       
        // Open up the failed tests
        toDisplay.foreach { query =>
          Desktop.getDesktop().browse(new URI("http://localhost:8080/#" + query))
        } 
        
        // let it run until user presses return
        StdIn.readLine()
        f
      }

      httpServiceFuture.flatMap(_.unbind()).onComplete { _ => system.terminate() }
    } else { 
      system.terminate()
    }
  }

  // Identifies all the actors who are interested in a given CDM statement
  def distributionSpec(t: CDM13): Seq[ActorRef] = t match {
    case f: FileObject =>
      if (ta1Source.isEmpty) {
        println(s"Source data from: ${f.baseObject.source}")
        ta1Source = Some(f.baseObject.source)
      }
      List(counterActor, dbActor, basicOpsActor)
    case _: Subject => List(counterActor, dbActor, basicOpsActor)
    case _: Event => List(counterActor, dbActor, basicOpsActor)
    case _ => List(counterActor, basicOpsActor)
  }

  // Sends a given CDM statement to all interested actors
  def distribute(cdm: CDM13): Unit = distributionSpec(cdm).foreach(receiver => receiver ! cdm)
}
