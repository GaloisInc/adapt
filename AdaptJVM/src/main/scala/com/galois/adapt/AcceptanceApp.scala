package com.galois.adapt

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.galois.adapt.cdm13.CDM13
import com.galois.adapt.scepter.{EventCountingTestActor, HowMany, ParsingTests}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try


object AcceptanceApp {
  println(s"Spinning up an acceptance system.")

  val config = Application.config  // .withFallback(ConfigFactory.load("acceptance"))

  val system = ActorSystem("acceptance-actor-system")
  implicit val ec = system.dispatcher
  implicit val askTimeout = Timeout(2 seconds)


  val dbActor = system.actorOf(Props(classOf[DatabaseActor]))
  val counterActor = system.actorOf(Props(new EventCountingTestActor(dbActor)))


  def run(
    filePath: String,
    count: Option[Int] = None
  ) {
    val data = CDM13.readData(filePath, count).get

    org.scalatest.run(new ParsingTests(data, count))

    val finalCount = Await.result(dbActor ? HowMany("total"), 2 seconds)
    println(s"Total vertices: $finalCount")

    Await.result(dbActor ? Shutdown, 2 seconds)

    system.terminate()

  }


  def distributionSpec(t: CDM13): Seq[ActorRef] = t match {
    case _ => List(counterActor)
  }

  def distribute(cdm: CDM13): Unit = distributionSpec(cdm).foreach(receiver => receiver ! cdm)
}
