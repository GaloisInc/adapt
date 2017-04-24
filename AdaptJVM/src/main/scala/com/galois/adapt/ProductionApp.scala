package com.galois.adapt

import java.io.File

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.galois.adapt.cdm17.{CDM17, TimeMarker}
import com.typesafe.config.ConfigFactory
import akka.stream._
import akka.stream.scaladsl._
import org.mapdb.DBMaker

import scala.util.Random


object ProductionApp {
  println(s"Running the production system.")

  def run() {

    val config = Application.config  //.withFallback(ConfigFactory.load("production"))
    val interface = config.getString("akka.http.server.interface")
    val port = config.getInt("akka.http.server.port")

    implicit val system = ActorSystem("production-actor-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val dbFile = File.createTempFile("map_" + Random.nextLong(), ".db")
    val db = DBMaker.fileDB(dbFile).fileMmapEnable().make()
    dbFile.deleteOnExit()   // TODO: consider keeping this to resume from a certain offset!


    // TODO: Use an actor which distributes to external DB:
//    val dbActor = system.actorOf(Props[DevDBActor])
//    val bindingFuture = Http().bindAndHandle(Routes.mainRoute(dbActor, List.empty, List.empty), interface, port)
//    println(s"Server online at http://0.0.0.0:8080/")

    val path = "/Users/ryan/Desktop/ta1-cadets-cdm17-3.bin" // cdm17_0407_1607.bin" //  ta1-clearscope-cdm17.bin"  //
    val source = Source.fromIterator[CDM17](() => CDM17.readData(path, None).get._2.map(_.get))
    val dbSink = TitanFlowComponents.titanWrites()

    val printSink = Sink.actorRef(system.actorOf(Props[PrintActor]()), TimeMarker(0L))

    // Flow only consumes and writes to Titan
    Flow[CDM17].runWith(source, dbSink)

    // Flow calculates all streaming results.
    FlowComponents.normalizedScores(db).runWith(source, printSink)

  }
}
