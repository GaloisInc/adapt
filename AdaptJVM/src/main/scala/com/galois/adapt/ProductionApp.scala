package com.galois.adapt

import java.io.{ByteArrayInputStream, File}
import java.util.UUID

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.galois.adapt.cdm17.{CDM17, RawCDM17Type, TimeMarker}
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

import scala.concurrent.Await
import scala.language.postfixOps
import scala.util.Random
import scala.concurrent.duration._


object ProductionApp extends App {
  println(s"Running the production system.")

  println(ConfigFactory.load())
  run()

  def run() {

    // This is here just to make SLF4j shut up and not log lots of error messages when instantiating the Kafka producer.
    org.slf4j.LoggerFactory.getILoggerFactory

    new File(this.getClass.getClassLoader.getResource("bin/iforest.exe").getPath).setExecutable(true)

    val config = ConfigFactory.load()  //.withFallback(ConfigFactory.load("production"))
    val interface = config.getString("akka.http.server.interface")
    val port = config.getInt("akka.http.server.port")

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
    val anomalyActor = system.actorOf(Props( classOf[AnomalyManager], dbActor))
    val statusActor = system.actorOf(Props[StatusActor])

    val httpService = Await.result(Http().bindAndHandle(ProdRoutes.mainRoute(dbActor, anomalyActor, statusActor), interface, port), 10 seconds)

    // Flow only consumes and writes to Titan
//    Flow[CDM17].runWith(CDMSource(ta1).via(FlowComponents.printCounter("DB Writes", 1000)), TitanFlowComponents.titanWrites())

    // Flow calculates all streaming results.
//    Ta1Flows(ta1)(db).runWith(CDMSource(ta1), Sink.actorRef[RankingCard](anomalyActor, None))

    val combined = RunnableGraph.fromGraph(GraphDSL.create(){ implicit graph =>
      import GraphDSL.Implicits._
      val bcast = graph.add(Broadcast[CDM17](2))

      CDMSource(ta1).via(FlowComponents.printCounter("Combined", 1000)) ~> bcast.in
      bcast.out(0) ~> Ta1Flows(ta1)(db) ~> Sink.actorRef[ViewScore](anomalyActor, None)
      bcast.out(1) ~> TitanFlowComponents.titanWrites()

      ClosedShape
    })
    combined.run()

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

  def apply(ta1: String): Source[CDM17, NotUsed] = {
    println(s"setting source for: $ta1")
    ta1.toLowerCase match {
      case "cadets"         => kafkaSource(s"ta1-cadets-$scenario-cdm17")
      case "clearscope"     => kafkaSource(s"ta1-clearscope-$scenario-cdm17")
      case "faros"          => kafkaSource(s"ta1-faros-$scenario-cdm17")
      case "fivedirections" => kafkaSource(s"ta1-fivedirections-$scenario-cdm17")
      case "theia"          => kafkaSource(s"ta1-theia-$scenario-cdm17")
      case "trace"          => kafkaSource(s"ta1-trace-$scenario-cdm17")
      case "kafkaTest"      => kafkaSource("kafkaTest").throttle(500, 5 seconds, 1000, ThrottleMode.shaping)
      case _ =>
        val path = "/Users/ryan/Desktop/ta1-clearscope-cdm17.bin" // cdm17_0407_1607.bin" //  ta1-clearscope-cdm17.bin"  //
        Source.fromIterator[CDM17](() => CDM17.readData(path, None).get._2.map(_.get))
          .concat(Source.fromIterator[CDM17](() => CDM17.readData(path + ".1", None).get._2.map(_.get)))
          .concat(Source.fromIterator[CDM17](() => CDM17.readData(path + ".2", None).get._2.map(_.get)))
          .via(FlowComponents.printCounter("CDM Source", 1e6.toInt)).throttle(500, 5 seconds, 1000, ThrottleMode.shaping)
    }
  }


  private def kafkaSource(ta1Topic: String): Source[CDM17, NotUsed] = Consumer.committableSource(
    ConsumerSettings(config.getConfig("akka.kafka.consumer"), new ByteArrayDeserializer, new ByteArrayDeserializer),
    Subscriptions.assignmentWithOffset(new TopicPartition(ta1Topic, 0), offset = 0L)
  ).map { msg =>
    val bais = new ByteArrayInputStream(msg.record.value())
    val reader = new SpecificDatumReader(classOf[com.bbn.tc.schema.avro.cdm17.TCCDMDatum])
    val decoder = DecoderFactory.get.binaryDecoder(bais, null)
    val elem: com.bbn.tc.schema.avro.cdm17.TCCDMDatum = reader.read(null, decoder)
    val cdm = new RawCDM17Type(elem.getDatum)
    msg.committableOffset.commitScaladsl()
    cdm
  }.map(CDM17.parse)
  .map(_.get).asInstanceOf[Source[CDM17, NotUsed]]   // TODO: handle errors!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
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
    case _ => anomalyScores(_: DB, 1, 2, 3, 6).map[ViewScore]((ViewScore.apply _).tupled).recover[ViewScore]{ case e: Throwable => e.printStackTrace().asInstanceOf[ViewScore] }
  }
}