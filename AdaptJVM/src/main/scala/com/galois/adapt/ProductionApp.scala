package com.galois.adapt

import java.io.{ByteArrayInputStream, File}
import java.util.UUID

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.galois.adapt.cdm17.{CDM17, RawCDM17Type}
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
    new File(config.getString("adapt.iforestpath")).setExecutable(true)

    implicit val system = ActorSystem("production-actor-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val ta1 = config.getString("adapt.ta1")

//    val dbFile = File.createTempFile("map_" + Random.nextLong(), ".db")
//    dbFile.delete()
    val dbFilePath = "/tmp/map_" + Random.nextLong() + ".db"
    val db = DBMaker.fileDB(dbFilePath).fileMmapEnable().make()
    new File(dbFilePath).deleteOnExit()   // TODO: consider keeping this to resume from a certain offset!

    val dbActor = system.actorOf(Props[TitanDBQueryProxy])
    val anomalyActor = system.actorOf(Props( classOf[AnomalyManager], dbActor, config))
    val statusActor = system.actorOf(Props[StatusActor])



    val srcActor = config.getString("adapt.runflow") match {
      case "database" | "db" =>
        Flow[CDM17].runWith(CDMSource(ta1).via(FlowComponents.printCounter("DB Writer", 1000)), TitanFlowComponents.titanWrites())

      case "anomalies" | "anomaly" =>
        Ta1Flows(ta1)(system.dispatcher)(db).runWith(CDMSource(ta1).via(FlowComponents.printCounter("Anomalies", 10000)), Sink.actorRef[ViewScore](anomalyActor, None))

      case _ => RunnableGraph.fromGraph(GraphDSL.create(){ implicit graph =>
        import GraphDSL.Implicits._
        val bcast = graph.add(Broadcast[CDM17](2))

        CDMSource(ta1).via(FlowComponents.printCounter("Combined", 1000)) ~> bcast.in
        bcast.out(0) ~> Ta1Flows(ta1)(system.dispatcher)(db) ~> Sink.actorRef[ViewScore](anomalyActor, None)
        bcast.out(1) ~> TitanFlowComponents.titanWrites()

        ClosedShape
      }).run()
    }


    val httpService = Await.result(Http().bindAndHandle(ProdRoutes.mainRoute(dbActor, anomalyActor, statusActor), interface, port), 10 seconds)
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
  val scenario = config.getString("adapt.scenario")

  def apply(ta1: String): Source[CDM17, _] = {
    println(s"Setting source for: $ta1")
    ta1.toLowerCase match {
      case "cadets"         => kafkaSource(config.getString("adapt.ta1kafkatopic"))
      case "clearscope"     => kafkaSource(config.getString("adapt.ta1kafkatopic"))
      case "faros"          => kafkaSource(config.getString("adapt.ta1kafkatopic"))
      case "fivedirections" => kafkaSource(config.getString("adapt.ta1kafkatopic"))
      case "theia"          => kafkaSource(config.getString("adapt.ta1kafkatopic"))
        .merge(kafkaSource(config.getString("adapt.theiaresponsetopic")).via(FlowComponents.printCounter("Theia Query Response", 1)))
      case "trace"          => kafkaSource(config.getString("adapt.ta1kafkatopic"))
      case "kafkaTest"      => kafkaSource("kafkaTest") //.throttle(500, 5 seconds, 1000, ThrottleMode.shaping)
      case _ =>
        val paths = config.getStringList("adapt.loadfiles").asScala
        println(s"Setting file sources to: ${paths.mkString(", ")}")
        paths.foldLeft(Source.empty[Try[CDM17]])((a,b) => a.concat(Source.fromIterator[Try[CDM17]](() => CDM17.readData(b, None).get._2)))
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
          }.via(FlowComponents.printCounter("File Source", 1e6.toInt)) //.throttle(1000, 1 seconds, 1500, ThrottleMode.shaping)
    }
  }


  def kafkaSource(ta1Topic: String): Source[CDM17, _] = Consumer.committableSource(
    ConsumerSettings(config.getConfig("akka.kafka.consumer"), new ByteArrayDeserializer, new ByteArrayDeserializer),
    Subscriptions.assignmentWithOffset(new TopicPartition(ta1Topic, 0), offset = config.getLong("adapt.startatoffset"))
  ).map { msg =>
    Try {
      val bais = new ByteArrayInputStream(msg.record.value())
      val offset = msg.record.offset()
      val reader = new SpecificDatumReader(classOf[com.bbn.tc.schema.avro.cdm17.TCCDMDatum])
      val decoder = DecoderFactory.get.binaryDecoder(bais, null)
      val t = Try {
        val elem: com.bbn.tc.schema.avro.cdm17.TCCDMDatum = reader.read(null, decoder)
        elem
      }
      if (t.isFailure) println(s"Couldn't read binary data at offset: $offset")
      msg.committableOffset.commitScaladsl()
      val cdm = new RawCDM17Type(t.get.getDatum)
      CDM17.parse(cdm)
    }.flatten
  }.mapConcat(c =>
    if (c.isSuccess) List(c.get)
    else List.empty
  ) //.asInstanceOf[Source[CDM17, NotUsed]]
}


object Ta1Flows {
  import FlowComponents._

  private val config = ConfigFactory.load()
  val base = config.getInt("adapt.basecleanupseconds")    // 10
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
    ).map[ViewScore]((ViewScore.apply _).tupled)  //.recover[ViewScore]{ case e: Throwable => e.printStackTrace().asInstanceOf[ViewScore] }
  }
}