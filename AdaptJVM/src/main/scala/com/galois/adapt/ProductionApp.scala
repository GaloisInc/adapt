package com.galois.adapt

import java.io.File
import java.util.UUID

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.galois.adapt.cdm17.{CDM17, TimeMarker}
import com.typesafe.config.ConfigFactory
import akka.stream._
import akka.stream.scaladsl._
import akka.util.Timeout
import org.mapdb.{DB, DBMaker}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import org.apache.tinkerpop.gremlin.structure.{Element => VertexOrEdge}
import ApiJsonProtocol._
import akka.http.scaladsl.server.RouteResult._

import scala.concurrent.Await
import scala.language.postfixOps
import scala.util.Random
import scala.concurrent.duration._


object ProductionApp extends App {
  println(s"Running the production system.")

  run()

  def run() {

    val config = ConfigFactory.load()  //.withFallback(ConfigFactory.load("production"))
    val interface = config.getString("akka.http.server.interface")
    val port = config.getInt("akka.http.server.port")

    implicit val system = ActorSystem("production-actor-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val ta1 = config.getString("adapt.ta1")

//    val dbFile = File.createTempFile("map_" + Random.nextLong(), ".db")
//    dbFile.delete()
//    println(s"DB file: $dbFile")
    val dbFilePath = "/tmp/map_" + Random.nextLong() + ".db"
    val db = DBMaker.fileDB(dbFilePath).fileMmapEnable().make()
    new File(dbFilePath).deleteOnExit()   // TODO: consider keeping this to resume from a certain offset!

    val dbActor = system.actorOf(Props[FakeDbActor])
    val anomalyActor = system.actorOf(Props[AnomalyManager])
    val statusActor = system.actorOf(Props[StatusActor])

    val httpService = Await.result(Http().bindAndHandle(ProdRoutes.mainRoute(dbActor, anomalyActor, statusActor), interface, port), 10 seconds)

    // Flow only consumes and writes to Titan
//    Flow[CDM17].runWith(CDMSource(ta1).via(FlowComponents.printCounter("DB Writes", 10000)), TitanFlowComponents.titanWrites())

    // Flow calculates all streaming results.
    Ta1Flows(ta1)(db).runWith(CDMSource(ta1), Sink.actorRef[RankingCard](anomalyActor, None))
  }
}


class StatusActor extends Actor with ActorLogging {
  def receive = {
    case x => println(s"StatusActor received: $x")
  }
}

class FakeDbActor extends Actor {
  def receive = {
    case x => println(s"FakeDbActor received: $x")
  }
}


case class RankingCard(name: String, keyNode: UUID, suspicionScore: Double, subgraph: Set[UUID])


object CDMSource {
  def apply(ta1: String): Source[CDM17, NotUsed] = ta1.toLowerCase match {
//    case "cadets" =>
//    case "clearscope" =>
//    case "faros" =>
//    case "fivedirections" =>
//    case "theia" =>
//    case "trace" =>
    case _ =>
      val path = "/Users/ryan/Desktop/ta1-cadets-cdm17-3.bin" // cdm17_0407_1607.bin" //  ta1-clearscope-cdm17.bin"  //
      Source.fromIterator[CDM17](() => CDM17.readData(path, None).get._2.map(_.get))
        .concat(Source.fromIterator[CDM17](() => CDM17.readData(path + ".1", None).get._2.map(_.get)))
        .concat(Source.fromIterator[CDM17](() => CDM17.readData(path + ".2", None).get._2.map(_.get)))
        .via(FlowComponents.printCounter("CDM Source", 1e6.toInt))
  }
}


object Ta1Flows {
  import FlowComponents._

  def apply(ta1: String) = ta1.toLowerCase match {
//    case "cadets" =>
//    case "clearscope" =>
//    case "faros" =>
//    case "fivedirections" =>
//    case "theia" =>
//    case "trace" =>
    case _ => normalizedScores(_: DB, 1, 2, 3, 6).map((RankingCard.apply _).tupled)
  }
}