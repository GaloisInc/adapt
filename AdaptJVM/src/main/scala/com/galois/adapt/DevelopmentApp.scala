package com.galois.adapt

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.pattern.ask
import com.galois.adapt.cdm13.{CDM13, EpochMarker}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn
import collection.JavaConversions._

import java.io.File
import java.nio.file.{Files, Paths}

import scala.language.postfixOps
import scala.concurrent.Future

import com.thinkaurelius.titan.core.TitanFactory
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONMapper
import com.thinkaurelius.titan.graphdb.tinkerpop.TitanIoRegistry
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.structure.{Edge, Graph, Vertex}
import org.apache.tinkerpop.gremlin.structure.io.IoCore
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONWriter

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
    val db = config.getString("adapt.dev.db")
    
    implicit val system = ActorSystem("development-actor-system")
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher  // needed for the future flatMap/onComplete in the end

    // Setup the right graph and database actor
    val (graph, writer, dbActor): (Graph, GraphSONWriter, ActorRef)  = db match {
      case "tinkerpop" =>
        val graph = TinkerGraph.open()     
        graph.createIndex("uuid", classOf[Vertex])
        graph.createIndex("pid", classOf[Vertex])
    
        val writer = GraphSONWriter.build().create()
        val dbActor = system.actorOf(Props(classOf[DevDBActor], graph))
        (graph, writer, dbActor)
     
      case "titan-inmemory" | "titan-cassandra" =>
        val graph = db match {
          case "titan-inmemory" => TitanFactory.build.set("storage.backend", "inmemory").open
          case "titan-cassandra" => TitanFactory.build.set("storage.backend", "cassandra").set("storage.hostname","localhost").open
        }
        val management = graph.openManagement()
        management.buildIndex("uuid", classOf[Vertex]).unique()
        management.buildIndex("pid", classOf[Vertex])
        management.commit()

        val mapper = GraphSONMapper.build().addRegistry(TitanIoRegistry.INSTANCE).create()
        val writer = GraphSONWriter.build().mapper(mapper).create()
        val dbActor = system.actorOf(Props(classOf[TitanDBActor], graph))
        (graph, writer, dbActor)
      case _ => throw new Exception("Unsupported")
    }
    
    
    // Optionally load in existing data:
    localStorage.foreach( path =>
      if (Files.exists(Paths.get(path))) graph.io(IoCore.graphson()).readGraph(path)
    )

    val queryActor = system.actorOf(Props(classOf[QueryDBActor], graph, writer))

    for (path <- loadPaths) {
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

    val httpServiceFuture = Http().bindAndHandle(Routes.mainRoute(queryActor), interface, port)
      .map { f =>
      val saveString = localStorage.fold("...")(s => s", and save in-memory database to: $s")
      println(s"\n    Server online at http://localhost:8080/\n\nPress RETURN to stop$saveString\n")
      StdIn.readLine() // let it run until user presses return
      f
    }

    httpServiceFuture
      .flatMap(_.unbind())
      .onComplete { _ =>
        localStorage.foreach(s => println(s"Writing in-memory database to file: $s"))
        implicit val askDuration = Timeout(10 seconds)
        Await.result(Future {
  //      println(s"Incomplete Edge count: ${missingFromUuid.size + missingToUuid.size}")
          localStorage.fold()(path => graph.io(IoCore.graphson()).writeGraph(path))
          //sender() ! (missingFromUuid.size + missingToUuid.size)
        }, 10 seconds)
        system.terminate()
      }

  }
}
