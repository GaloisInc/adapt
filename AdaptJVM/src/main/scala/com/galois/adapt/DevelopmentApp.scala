package com.galois.adapt

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.actor._
import akka.pattern.ask
import com.galois.adapt.cdm13.{CDM13, EpochMarker, Subject}
import com.galois.adapt.feature._
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn
import collection.JavaConversions._

import java.io.File

import scala.language.postfixOps
import scala.language.existentials

object DevelopmentApp {
  println(s"Spinning up a development system.")

  def run(
    loadPaths: List[String] = List(),
    limitLoad: Option[Int] = None,
    localStorage: Option[String] = None
  ) {

    val config = Application.config  //.withFallback(ConfigFactory.load("development"))
    val interface = config.getString("akka.http.server.interface")
    val port = config.getInt("akka.http.server.port")

    implicit val system = ActorSystem(config.getString("adapt.systemname"))
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher  // needed for the future flatMap/onComplete in the end
    val dbActor = system.actorOf(Props(classOf[DevDBActor], localStorage))
    
    val erActor: ActorRef = system.actorOf(Props(classOf[ErActor]))
    
    // The two feature extractors subscribe to the CDM13 produced by the ER actor
    val featureExtractor1 = system.actorOf(FileEventsFeature.props(erActor))
    val featureExtractor2 = system.actorOf(NetflowFeature.props(erActor))

    // The IForest anomaly detector is going to subscribe to the output of the two feature extractors
    val ad = system.actorOf(IForestAnomalyDetector.props(Set(
      Subscription(featureExtractor1, (m: Any) =>
        Some(m.asInstanceOf[Map[Subject,(Int,Int,Int)]].mapValues(t => Seq(t._1.toDouble, t._2.toDouble, t._3.toDouble)))),
      Subscription(featureExtractor2, (m: Any) =>
        Some(m.asInstanceOf[Map[Subject,Seq[Double]]]))
    )))

    // As a debugging tool, the Outgestor will print out what the anomaly detector produced
    val out = system.actorOf(Outgestor.props(Set(ad)))

    for (path <- loadPaths) {
      val data = CDM13.readData(path, limitLoad).get
      var counter = 0
      data.foreach { d =>
        dbActor ! d.get
        erActor ! d.get
        counter += 1
        print(s"Reading data from $path: $counter\r")
      }
      dbActor ! EpochMarker
      erActor ! EpochMarker
      print("\n")
    }

    val httpServiceFuture = Http().bindAndHandle(Routes.mainRoute(dbActor), interface, port)
      .map { f =>
      val saveString = localStorage.fold("...")(s => s", and save in-memory database to: $s")
      println(s"\n    Server online at http://$interface:$port/\n\nPress RETURN to stop$saveString\n")
      StdIn.readLine() // let it run until user presses return
      f
    }

    httpServiceFuture
      .flatMap(_.unbind())
      .onComplete { _ =>
        localStorage.foreach(s => println(s"Writing in-memory database to file: $s"))
        implicit val askDuration = Timeout(10 seconds)
        Await.result(dbActor ? Shutdown, 10 seconds)
        system.terminate()
      }

  }


  def dev(loadFilePath: Option[String] = None, limitLoad: Option[Int] = Some(100000)) = {
    val config = ConfigFactory.load()  //.withFallback(ConfigFactory.load("development"))
    val interface = config.getString("akka.http.server.interface")
    val port = config.getInt("akka.http.server.port")

    implicit val system = ActorSystem("development-actor-system")
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher  // needed for the future flatMap/onComplete in the end
    val dbActor = system.actorOf(Props(classOf[DevDBActor], None))

    loadFilePath.fold() { path =>
      val data = CDM13.readData(path, limitLoad).get
      var counter = 0
      data.foreach { d =>
        dbActor ! d.get
        counter += 1
        print(s"Reading data from $path: $counter\r")
      }
      dbActor ! EpochMarker
      print("\n")
    }

    implicit val timeout = Timeout(10 seconds)
    val graph = Await.result((dbActor ? GiveMeTheGraph).mapTo[org.apache.tinkerpop.gremlin.structure.Graph], 10 seconds)

    val httpServiceFuture = Http().bindAndHandle(Routes.mainRoute(dbActor), interface, port)
    (system, dbActor, graph, httpServiceFuture)
  }
}
