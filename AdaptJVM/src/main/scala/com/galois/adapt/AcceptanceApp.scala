package com.galois.adapt

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.galois.adapt.cdm13._
import com.galois.adapt.scepter._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try
import java.io.File

import scala.language.postfixOps

object AcceptanceApp {
  println(s"Spinning up an acceptance system.")

  val config = Application.config  // .withFallback(ConfigFactory.load("acceptance"))
  val system = ActorSystem("acceptance-actor-system")
  implicit val ec = system.dispatcher
  implicit val askTimeout = Timeout(2 seconds)

  val dbActor = system.actorOf(Props(classOf[DevDBActor], None))
  val counterActor = system.actorOf(Props(new EventCountingTestActor(dbActor)))
  var TA1Source: Option[InstrumentationSource] = None

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
        org.scalatest.run(new General_TA1_Tests(data, count))

        val finalCount = Await.result(dbActor ? HowMany("total"), 2 seconds).asInstanceOf[Int]
        val missingEdgeCount = Await.result(dbActor ? Shutdown, 2 seconds).asInstanceOf[Int]

        println("")

        if (TA1Source contains SOURCE_ANDROID_JAVA_CLEARSCOPE)
          org.scalatest.run(new CLEARSCOPE_Specific_Tests(finalCount, missingEdgeCount))

        println(s"Total vertices: $finalCount")
      }
    }

    println(s"\nIf any of these test results surprise you, please email Ryan Wright and the Adapt team at: ryan@galois.com\n")
    system.terminate()
  }

  def distributionSpec(t: CDM13): Seq[ActorRef] = t match {
    case f: FileObject =>
      if (TA1Source.isEmpty) {
        println(s"Source data from: ${f.baseObject.source}")
        TA1Source = Some(f.baseObject.source)
      }
      List(counterActor)
    case _ => List(counterActor)
  }

  def distribute(cdm: CDM13): Unit = distributionSpec(cdm).foreach(receiver => receiver ! cdm)
}
