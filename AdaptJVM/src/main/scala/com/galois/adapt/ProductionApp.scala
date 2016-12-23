package com.galois.adapt

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

object ProductionApp {
  println(s"Running the production system.")

  def run() {

    val config = Application.config  //.withFallback(ConfigFactory.load("production"))
    val interface = config.getString("akka.http.server.interface")
    val port = config.getInt("akka.http.server.port")

    implicit val system = ActorSystem("production-actor-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    // TODO: Use an actor which distributes to external DB:
    val dbActor = system.actorOf(Props[DatabaseActor])

    val bindingFuture = Http().bindAndHandle(Routes.mainRoute(dbActor), interface, port)

    println(s"Server online at http://localhost:8080/")
  }
}
