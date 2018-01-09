package com.galois.adapt

import java.io.{ByteArrayInputStream, File}
import java.nio.file.Paths
import java.util.UUID

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.galois.adapt.cdm17.{AbstractObject, CDM17, CryptographicHash, Event, FileObject, FileObjectType, NetFlowObject, Principal, PrincipalType, PrivilegeLevel, ProvenanceTagNode, RawCDM17Type, RegistryKeyObject, SrcSinkObject, Subject, SubjectType}
import com.typesafe.config.ConfigFactory
import akka.stream._
import akka.stream.scaladsl._
import akka.util.Timeout
import com.rrwright.quine.language.refinedBranchOf
import com.rrwright.quine.runtime._

import scala.pickling.PicklerUnpickler
//import akka.util.{ByteString, Timeout}
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
import scala.pickling.FastTypeTag



object ProductionApp {
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

    val dbActor = system.actorOf(Props[TitanDBQueryProxy])
    val anomalyActor = system.actorOf(Props( classOf[AnomalyManager], dbActor, config))
    val statusActor = system.actorOf(Props[StatusActor])

    val ta1 = config.getString("adapt.env.ta1")
    config.getString("adapt.runflow").toLowerCase match {
      case "database" | "db" | "quine" =>
//        println("Running database-only flow")
        println("running Quine flow")
        val graph = GraphService(system,
          inMemoryNodeLimit = None //Some(1000)
        , uiPort = 9090)(
//          EmptyPersistor
          MapDBMultimap()
//          ParallelBlockingJsonFilePersistor()

//          SingleActorJsonFilePersistor(system)
//          MapDbEventJsonPersistor()
//          MapDbJournalJsonPersistor()
        )
        implicit val timeout = Timeout(30.4 seconds)
        val parallelism = 16
//        val quineActor = system.actorOf(Props(classOf[QuineDBActor], graph))
//        Flow[CDM17].runWith(CDMSource(ta1).via(FlowComponents.printCounter("Quine", 1000)), Sink.actorRefWithAck(quineActor, Init, Ack, Complete, println))
        val quineRouter = system.actorOf(Props(classOf[QuineRouter], parallelism, graph))

        {
          import scala.pickling.Pickler
          import scala.pickling.Defaults._
          import com.rrwright.quine.runtime.runtimePickleFormat
          implicit val gr = graph
          implicit val b = PicklerUnpickler.generate[Option[Map[String,String]]]
          //  implicit val l = PicklerUnpickler.generate[Option[UUID]]
          implicit val t = PicklerUnpickler.generate[Option[String]]
          implicit val y = PicklerUnpickler.generate[AbstractObject]
          implicit val j = PicklerUnpickler.generate[SubjectType]
          implicit val k = PicklerUnpickler.generate[Option[PrivilegeLevel]]
          implicit val l = PicklerUnpickler.generate[Option[Seq[String]]]
          implicit val m = PicklerUnpickler.generate[Option[Int]]
          implicit val n = PicklerUnpickler.generate[Option[UUID]]
          refinedBranchOf[Subject]().standingFind(println)
        }

        CDMSource(ta1)
          .via(FlowComponents.printCounter("Quine", 10000))
          .mapAsyncUnordered(parallelism)(cdm => quineRouter ? cdm)
            .recover{ case x => println(s"\n\nFAILING AT END OF STREAM.\n\n"); x.printStackTrace()}
          .runWith(Sink.ignore)

      case "anomalies" | "anomaly" =>
        println("Running anomaly-only flow")
        Ta1Flows(ta1)(system.dispatcher)(db).runWith(CDMSource(ta1).via(FlowComponents.printCounter("Anomalies", 10000)), Sink.actorRef[ViewScore](anomalyActor, None))

      case "ui" =>
        println("Staring only the UI and doing nothing else.")

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
          bcast.out(4).collect{ case c: ProvenanceTagNode => c.getUuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "ProvenanceTagNodes.csv")
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
        println("Running the combined database ingest + anomaly calculation flow")
	
//        RunnableGraph.fromGraph(GraphDSL.create(){ implicit graph =>
//          import GraphDSL.Implicits._
//          val bcast = graph.add(Broadcast[CDM17](2))
//
//          CDMSource(ta1).via(FlowComponents.printCounter("Combined", 1000)) ~> bcast.in
//          bcast.out(0) ~> Ta1Flows(ta1)(system.dispatcher)(db) ~> Sink.actorRef[ViewScore](anomalyActor, None)
//          bcast.out(1) ~> TitanFlowComponents.titanWrites()
//
//          ClosedShape
//        }).run()
    }

//    val source = Source
//      .actorRef[Int](0, OverflowStrategy.fail)
//      .mapMaterializedValue( ref =>
//        Await.result(Http().bindAndHandle(ProdRoutes.mainRoute(dbActor, anomalyActor, ref), interface, port), 10 seconds)
//      )

    val httpService = Await.result(Http().bindAndHandle(ProdRoutes.mainRoute(dbActor, anomalyActor, statusActor), interface, port), 10 seconds)
  }
}



import akka.routing.{ ActorRefRoutee, RoundRobinRoutingLogic, Router }

class QuineRouter(count: Int, graph: GraphService) extends Actor {
  var router = {
    val routees = Vector.fill(count) {
      val r = context.actorOf(Props(classOf[QuineDBActor], graph))
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case w: CDM17 =>
      router.route(w, sender())
    case Terminated(a) =>
      router = router.removeRoutee(a)
      val r = context.actorOf(Props[QuineDBActor])
      context watch r
      router = router.addRoutee(r)
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
    ta1.toLowerCase match {
      case "cadets"         => kafkaSource(config.getString("adapt.env.ta1kafkatopic")).drop(start)
      case "clearscope"     => kafkaSource(config.getString("adapt.env.ta1kafkatopic")).drop(start)
      case "faros"          => kafkaSource(config.getString("adapt.env.ta1kafkatopic")).drop(start)
      case "fivedirections" => kafkaSource(config.getString("adapt.env.ta1kafkatopic")).drop(start)
      case "theia"          => kafkaSource(config.getString("adapt.env.ta1kafkatopic")).drop(start)
        .merge(kafkaSource(config.getString("adapt.env.theiaresponsetopic")).via(FlowComponents.printCounter("Theia Query Response", 1)))
      case "trace"          => kafkaSource(config.getString("adapt.env.ta1kafkatopic")).drop(start)
      case "kafkaTest"      => kafkaSource("kafkaTest").drop(start) //.throttle(500, 5 seconds, 1000, ThrottleMode.shaping)
      case _ =>
        val paths = config.getStringList("adapt.ingest.loadfiles").asScala
        println(s"Setting file sources to: ${paths.mkString(", ")}")
        paths.foldLeft(Source.empty[Try[CDM17]])((a,b) => a.concat(Source.fromIterator[Try[CDM17]](() => CDM17.readData(b, None).get._2)))
          .drop(start)
          .take(Application.loadLimitOpt.getOrElse(Long.MaxValue))
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
