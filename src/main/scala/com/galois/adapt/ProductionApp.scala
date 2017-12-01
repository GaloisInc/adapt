package com.galois.adapt

import java.io.{ByteArrayInputStream, File}
import java.nio.file.Paths
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.galois.adapt.cdm17.{CDM17, Event, FileObject, NetFlowObject, Principal, ProvenanceTagNode, RawCDM17Type, RegistryKeyObject, SrcSinkObject, Subject}
import com.typesafe.config.ConfigFactory
import akka.stream._
import akka.stream.scaladsl._
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import org.mapdb.{DB, DBMaker}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import org.apache.tinkerpop.gremlin.structure.{Element => VertexOrEdge}
import ApiJsonProtocol._
import akka.http.scaladsl.server.RouteResult._
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer}

import collection.JavaConverters._
import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps
import scala.util.{Random, Try}
import scala.concurrent.duration._


object ProductionApp {
//  println(s"Running the production system.")

//  println(ConfigFactory.load())

  def run() {

    // This is here just to make SLF4j shut up and not log lots of error messages when instantiating the Kafka producer.
    org.slf4j.LoggerFactory.getILoggerFactory


    val config = ConfigFactory.load()  //.withFallback(ConfigFactory.load("production"))
    val interface = config.getString("akka.http.server.interface")
    val port = config.getInt("akka.http.server.port")

//    new File(this.getClass.getClassLoader.getResource("bin/iforest.exe").getPath).setExecutable(true)
//    new File(config.getString("adapt.runtime.iforestpath")).setExecutable(true)

    implicit val system = ActorSystem("production-actor-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher


//    val dbFile = File.createTempFile("map_" + Random.nextLong(), ".db")
//    dbFile.delete()
    val dbFilePath = "/tmp/map_" + Random.nextLong() + ".db"
    val db = DBMaker.fileDB(dbFilePath).fileMmapEnable().make()
    new File(dbFilePath).deleteOnExit()   // TODO: consider keeping this to resume from a certain offset!

    val dbActor = system.actorOf(Props(classOf[Neo4jDBQueryProxy]))
    implicit val timeout = Timeout(600 seconds)
    println(s"Waiting for DB indices to become active: $timeout")
    Await.result(dbActor ? Ready, timeout.duration)
    val anomalyActor = system.actorOf(Props( classOf[AnomalyManager], dbActor, config))
    val statusActor = system.actorOf(Props[StatusActor])

    val ta1 = config.getString("adapt.env.ta1")

    config.getString("adapt.runflow").toLowerCase match {
      case "database" | "db" | "ingest" =>
        println("Running database flow with UI")
        val writeTimeout = Timeout(30.1 seconds)
        if (config.getBoolean("adapt.ingest.quitafteringest")) {
          println("Will shut down after ingesting all files.")
          CDMSource(ta1).via(FlowComponents.printCounter("Neo4j Writer", 10000)).via(Neo4jFlowComponents.neo4jActorWriteFlow(dbActor)(writeTimeout)).runForeach {
            case Success(_) => ()
            case msg @ Failure(e) => println(s"Insertion errors in batch. Continuing after exception:\n${e.printStackTrace()}")
          } onComplete {
            case Failure(e) => e.printStackTrace(); Runtime.getRuntime.halt(1)
            case Success(v) => println("shutting down..."); Runtime.getRuntime.halt(0)
          }
        } else {
          println("Will continuing running the DB and UI after ingesting all files.")
          CDMSource(ta1).via(FlowComponents.printCounter("Neo4j Writer", 10000)).runWith(Neo4jFlowComponents.neo4jActorWrite(dbActor)(writeTimeout))
        }
      val httpService = Await.result(Http().bindAndHandle(ProdRoutes.mainRoute(dbActor, anomalyActor, statusActor), interface, port), 10 seconds)

      case "anomalies" | "anomaly" =>
        println("Running anomaly-only flow")
        Ta1Flows(ta1)(system.dispatcher)(db).runWith(CDMSource(ta1).via(FlowComponents.printCounter("Anomalies", 10000)), Sink.actorRef[ViewScore](anomalyActor, None))

      case "ui" | "uionly" =>
        println("Staring only the UI and doing nothing else.")
        val httpService = Await.result(Http().bindAndHandle(ProdRoutes.mainRoute(dbActor, anomalyActor, statusActor), interface, port), 10 seconds)

      case "csvmaker" | "csv" =>
        RunnableGraph.fromGraph(GraphDSL.create(){ implicit graph =>
          import GraphDSL.Implicits._
          val bcast = graph.add(Broadcast[CDM17](8))
          val odir = if(config.hasPath("adapt.outdir")) config.getString("adapt.outdir") else "."
          CDMSource(ta1).via(FlowComponents.printCounter("File Input")) ~> bcast.in
          bcast.out(0).collect{ case c: NetFlowObject => c.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "NetFlowObjects.csv")
          bcast.out(1).collect{ case c: Event => c.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "Events.csv")
          bcast.out(2).collect{ case c: FileObject => c.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "FileObjects.csv")
          bcast.out(3).collect{ case c: RegistryKeyObject => c.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "RegistryKeyObjects.csv")
          bcast.out(4).collect{ case c: ProvenanceTagNode => c.tagIdUuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "ProvenanceTagNodes.csv")
          bcast.out(5).collect{ case c: Subject => c.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "Subjects.csv")
          bcast.out(6).collect{ case c: Principal => c.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "Principals.csv")
          bcast.out(7).collect{ case c: SrcSinkObject => c.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "SrcSinkObjects.csv")
          ClosedShape
        }).run()

      case "valuebytes" =>
        CDMSource(ta1)
          .collect{ case e: Event if e.parameters.nonEmpty => e}
          .flatMapConcat(
            (e: Event) => Source.fromIterator(
              () => e.parameters.get.flatMap( v =>
                v.valueBytes.map(b =>
                  List(akka.util.ByteString(s"<<<BEGIN_LINE\t${e.uuid}\t${new String(b)}\tEND_LINE>>>\n"))
                ).getOrElse(List.empty)).toIterator
              )
            )
          .toMat(FileIO.toPath(Paths.get("ValueBytes.txt")))(Keep.right).run()

      case _ =>
        println("Running the combined database ingest + anomaly calculation flow + UI")
        val httpService = Await.result(Http().bindAndHandle(ProdRoutes.mainRoute(dbActor, anomalyActor, statusActor), interface, port), 10 seconds)
	
        RunnableGraph.fromGraph(GraphDSL.create(){ implicit graph =>
          import GraphDSL.Implicits._
          val bcast = graph.add(Broadcast[CDM17](2))

          CDMSource(ta1).via(FlowComponents.printCounter("Combined", 1000)) ~> bcast.in
          bcast.out(0) ~> Ta1Flows(ta1)(system.dispatcher)(db) ~> Sink.actorRef[ViewScore](anomalyActor, None)
          bcast.out(1) ~> Neo4jFlowComponents.neo4jActorWrite(dbActor)(Timeout(30 seconds)) //Neo4jFlowComponents.neo4jWrites(neoGraph)

          ClosedShape
        }).run()
    }
  }
}


class StatusActor extends Actor with ActorLogging {
  def receive = {
    case x => println(s"StatusActor received: $x")
  }
}


case class ViewScore(viewName: String, keyNode: UUID, suspicionScore: Double, subgraph: Set[UUID])


object CDMSource {
  private val config = ConfigFactory.load()
  val scenario = config.getString("adapt.env.scenario")

  def apply(ta1: String): Source[CDM17, _] = {
    println(s"Setting source for: $ta1")
    val start = Try(config.getLong("adapt.ingest.startatoffset")).getOrElse(0L)
    val shouldLimit = Try(config.getLong("adapt.ingest.loadlimit")) match {
      case Success(0) => None
      case Success(i) => Some(i)
      case _ => None
    }
    ta1.toLowerCase match {
      case "cadets"         =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic")).drop(start)
        shouldLimit.fold(src)(l => src.take(l))
      case "clearscope"     =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic")).drop(start)
        shouldLimit.fold(src)(l => src.take(l))
      case "faros"          =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic")).drop(start)
        shouldLimit.fold(src)(l => src.take(l))
      case "fivedirections" =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic")).drop(start)
        shouldLimit.fold(src)(l => src.take(l))
      case "theia"          =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic")).drop(start)
        shouldLimit.fold(src)(l => src.take(l))
        .merge(kafkaSource(config.getString("adapt.env.theiaresponsetopic")).via(FlowComponents.printCounter("Theia Query Response", 1)))
      case "trace"          =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic")).drop(start)
        shouldLimit.fold(src)(l => src.take(l))
      case "kafkaTest"      =>
        val src = kafkaSource("kafkaTest").drop(start) //.throttle(500, 5 seconds, 1000, ThrottleMode.shaping)
        shouldLimit.fold(src)(l => src.take(l))
      case _ =>
        val paths = config.getStringList("adapt.ingest.loadfiles").asScala
        println(s"Setting file sources to: ${paths.mkString(", ")}")
        val startStream = paths.foldLeft(Source.empty[Try[CDM17]])((a,b) => a.concat(Source.fromIterator[Try[CDM17]](() => CDM17.readData(b, None).get._2)))
          .drop(start)
        shouldLimit.map(l => startStream.take(l)).getOrElse(startStream)
          .statefulMapConcat { () =>
            var counter = 0
            cdmTry => {
              counter = counter + 1
              if (cdmTry.isSuccess) List(cdmTry.get)
              else {
                println(s"Couldn't read binary data at offset: $counter")
                List.empty
              }
            }
          }
//          .via(FlowComponents.printCounter("File Source", 1e6.toInt)) //.throttle(1000, 1 seconds, 1500, ThrottleMode.shaping)
    }
  }


  def kafkaSource(ta1Topic: String): Source[CDM17, _] = Consumer.plainSource(  // commitableSource
    ConsumerSettings(config.getConfig("akka.kafka.consumer"), new ByteArrayDeserializer, new ByteArrayDeserializer),
    Subscriptions.assignmentWithOffset(new TopicPartition(ta1Topic, 0), offset = config.getLong("adapt.ingest.startatoffset"))
  ).map { msg =>
    Try {
      val bais = new ByteArrayInputStream(msg.value())  // msg.record.value()
      val offset = msg.offset()   // msg.record.offset()
      val reader = new SpecificDatumReader(classOf[com.bbn.tc.schema.avro.cdm17.TCCDMDatum])
      val decoder = DecoderFactory.get.binaryDecoder(bais, null)
      val t = Try {
        val elem: com.bbn.tc.schema.avro.cdm17.TCCDMDatum = reader.read(null, decoder)
        elem
      }
      if (t.isFailure) println(s"Couldn't read binary data at offset: $offset")
//      msg.committableOffset.commitScaladsl()
      val cdm = new RawCDM17Type(t.get.getDatum)
      CDM17.parse(cdm)
    }.flatten
  }.mapConcat(c =>
    if (c.isSuccess) List(c.get)
    else List.empty
  ) //.asInstanceOf[Source[CDM17, NotUsed]]
}


object Ta1Flows {
  import AnomalyStream._

  private val config = ConfigFactory.load()
  val base = config.getInt("adapt.runtime.basecleanupseconds")    // 10
  val fastEmit = base * 2 + base                          // 30
  val slowClean = fastEmit * 2                            // 60
  val slowEmit = slowClean * 2 + base                     // 130

  def apply(ta1: String)(implicit ec: ExecutionContext) = ta1.toLowerCase match {
//    case "cadets" =>
//    case "clearscope" =>
//    case "faros" =>
//    case "fivedirections" =>
//    case "theia" =>
//    case "trace" =>
    case _ => anomalyScores(_: DB,
      base,
      fastEmit,
      slowClean,
      slowEmit
    ).map[ViewScore]((ViewScore.apply _).tupled).recover[ViewScore]{ case e: Throwable => e.printStackTrace().asInstanceOf[ViewScore] }
  }
}
