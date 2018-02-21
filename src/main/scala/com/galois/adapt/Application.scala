package com.galois.adapt

import java.io.{ByteArrayInputStream, File}
import java.nio.file.Paths
import java.util.UUID

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteResult._
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.pattern.ask
import akka.stream.{ActorMaterializer, _}
import akka.stream.scaladsl._
import akka.util.Timeout
import com.galois.adapt.Application.config
import com.galois.adapt.adm.UuidRemapper.GetStillBlocked
import com.galois.adapt.adm._
import com.galois.adapt.cdm17.{CDM17, RawCDM17Type}
import com.galois.adapt.{cdm17 => cdm17types}
import com.galois.adapt.cdm18._
import com.typesafe.config.ConfigFactory
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.tinkerpop.gremlin.structure.{Element => VertexOrEdge}
import org.mapdb.{DB, DBMaker}
import org.reactivestreams.Publisher

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Random, Success, Try}


object Application extends App {

  // This is here just to make SLF4j shut up and not log lots of error messages when instantiating the Kafka producer.
  org.slf4j.LoggerFactory.getILoggerFactory
  val config = ConfigFactory.load()  //.withFallback(ConfigFactory.load("production"))
  val runFlow = config.getString("adapt.runflow").toLowerCase

  val interface = config.getString("akka.http.server.interface")
  val port = config.getInt("akka.http.server.port")
  implicit val system = ActorSystem("production-actor-system")

//    new File(this.getClass.getClassLoader.getResource("bin/iforest.exe").getPath).setExecutable(true)
//    new File(config.getString("adapt.runtime.iforestpath")).setExecutable(true)

  val quitOnError = config.getBoolean("adapt.runtime.quitonerror")
  val streamErrorStrategy: Supervision.Decider = {
    case e: Throwable =>
      e.printStackTrace()
      if (quitOnError) Runtime.getRuntime.halt(1)
      Supervision.Resume
  }
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system).withSupervisionStrategy(streamErrorStrategy))
  implicit val executionContext = system.dispatcher
//    val dbFile = File.createTempFile("map_" + Random.nextLong(), ".db")
//    dbFile.delete()
  val dbFilePath = "/tmp/map_" + Random.nextLong() + ".db"


  val db = DBMaker.fileDB(dbFilePath).fileMmapEnable().make()
  new File(dbFilePath).deleteOnExit()   // TODO: consider keeping this to resume from a certain offset!

  // Start up the database
  val dbActor: ActorRef = runFlow match {
    case "accept" => system.actorOf(Props(classOf[TinkerGraphDBQueryProxy]))
    case _ => system.actorOf(Props(classOf[Neo4jDBQueryProxy]))
  }
  val dbStartUpTimeout = Timeout(600 seconds)  // Don't make this implicit.
  println(s"Waiting for DB indices to become active: $dbStartUpTimeout")
  Await.result(dbActor.?(Ready)(dbStartUpTimeout), dbStartUpTimeout.duration)


  val anomalyActor = system.actorOf(Props(classOf[AnomalyManager], dbActor, config))
  val statusActor = system.actorOf(Props[StatusActor], name = "statusActor")

  // Akka-streams makes this _way_ more difficult than I feel it ought to be, but here it is:
  //
  //   * an actor ref to which you send messages of type `ADM`
  //   * a source where those `ADM` magically appear
  //
  // To make the source complete, we have to send the actor a `akka.actor.Status.Success(())`.
  val (synActor: ActorRef, synSource: Source[ADM, _]) = {
    // If we support fanout, we would have to buffer everything sent to the sink, forever. Thankfully, the odds of
    // someone making another source from this publisher are low since the publisher's scope is limited to this block.
    val fanOut: Boolean = false
    val sink: Sink[ADM, Publisher[ADM]] = Sink.asPublisher[ADM](fanOut)

    val (ref, synPublisher) = Source.actorRef(Int.MaxValue, OverflowStrategy.fail).toMat(sink)(Keep.both).run()
    val source: Source[ADM, _] = Source.fromPublisher(synPublisher)

    (ref, source)
  }

  val uuidRemapper: ActorRef = system.actorOf(Props(classOf[UuidRemapper], synActor, (10 minutes).toNanos), name = "uuidRemapper")


  val ta1 = config.getString("adapt.env.ta1")

  // Mutable state that gets updated during ingestion
  var instrumentationSource: String = "(not detected)"
  var failedStatements: List[(Int, String)] = Nil

  def startWebServer(): Http.ServerBinding = {
    println(s"Starting the web server at: http://$interface:$port")
    val route = ProdRoutes.mainRoute(dbActor, anomalyActor, statusActor)
    val httpServer = Http().bindAndHandle(route, interface, port)
    Await.result(httpServer, 10 seconds)
  }

  runFlow match {

    case "accept" =>
      println("Running acceptance tests")

      val writeTimeout = Timeout(30.1 seconds)

      val sink = Sink.fromGraph(GraphDSL.create() { implicit b =>
        import GraphDSL.Implicits._
        val broadcast = b.add(Broadcast[CDM18](1))

        broadcast ~> Neo4jFlowComponents.neo4jActorCdmWriteSink(dbActor, CdmDone)(writeTimeout)
     //   broadcast ~> EntityResolution(uuidRemapper) ~> Neo4jFlowComponents.neo4jActorAdmWriteSink(dbActor, AdmDone)(writeTimeout)
        SinkShape(broadcast.in)
      })

      startWebServer()
      CDMSource.cdm18(ta1, (position, msg) => failedStatements = (position, msg.getMessage) :: failedStatements)
        .via(FlowComponents.printCounter("CDM events"))
        .recover{ case e: Throwable => e.printStackTrace(); ??? }
        .runWith(sink)


    case "database" | "db" | "ingest" =>
      val ingestCdm = config.getBoolean("adapt.ingest.producecdm")
      val ingestAdm = config.getBoolean("adapt.ingest.produceadm")
      val completionMsg = if (config.getBoolean("adapt.ingest.quitafteringest")) KillJVM else CompleteMsg
      val writeTimeout = Timeout(30.1 seconds)

      val (name, sink) = (ingestCdm, ingestAdm) match {
        case (false, false) => println("\n\nA database ingest flow which ingest neither CDM nor ADM data ingests nothing at all.\n\nExiting, so that you can ponder the emptiness of existence for a while...\n\n"); Runtime.getRuntime.halt(42); throw new RuntimeException("TreeFallsInTheWoodsException")
        case (true, false) => "CDM" -> Neo4jFlowComponents.neo4jActorCdmWriteSink(dbActor, completionMsg)(writeTimeout)
        case (false, true) => "ADM" -> EntityResolution(uuidRemapper, synSource).to(Neo4jFlowComponents.neo4jActorAdmWriteSink(dbActor, completionMsg)(writeTimeout))   // TODO: Alec, why doesn't the ER flow pass along termination messages? (I suspect existential type parameters.)
        case (true, true) => "CDM+ADM" -> Sink.fromGraph(GraphDSL.create() { implicit b =>
          import GraphDSL.Implicits._
          val broadcast = b.add(Broadcast[CDM18](2))

          broadcast ~> Neo4jFlowComponents.neo4jActorCdmWriteSink(dbActor, completionMsg)(writeTimeout)
          broadcast ~> EntityResolution(uuidRemapper, synSource) ~> Neo4jFlowComponents.neo4jActorAdmWriteSink(dbActor, completionMsg)(writeTimeout)

          SinkShape(broadcast.in)
        })
      }

      println(s"Running database flow for $name with UI.")
      if (config.getBoolean("adapt.ingest.quitafteringest")) println("Will terminate after ingest.")

      startWebServer()
      CDMSource.cdm18(ta1).via(FlowComponents.printCounter(name)).runWith(sink)

    case "csvmaker" | "csv" =>

      val forCdm = config.getBoolean("adapt.ingest.producecdm")
      val forAdm = config.getBoolean("adapt.ingest.produceadm")

      val odir = if(config.hasPath("adapt.outdir")) config.getString("adapt.outdir") else "."

      // CSV generation
      //
      // TODO: Alec find a way to have this exit on completion
      (forCdm, forAdm) match {
        case (true, false) =>
          RunnableGraph.fromGraph(GraphDSL.create(){ implicit graph =>
            import GraphDSL.Implicits._

            val broadcast = graph.add(Broadcast[CDM18](8))

            CDMSource.cdm18(ta1).via(FlowComponents.printCounter("File Input")) ~> broadcast.in

            broadcast.out(0).collect{ case c: cdm17.NetFlowObject => c.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "NetFlowObjects.csv")
            broadcast.out(1).collect{ case c: cdm17.Event => c.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "Events.csv")
            broadcast.out(2).collect{ case c: cdm17.FileObject => c.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "FileObjects.csv")
            broadcast.out(3).collect{ case c: cdm17.RegistryKeyObject => c.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "RegistryKeyObjects.csv")
            broadcast.out(4).collect{ case c: cdm17.ProvenanceTagNode => c.tagIdUuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "ProvenanceTagNodes.csv")
            broadcast.out(5).collect{ case c: cdm17.Subject => c.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "Subjects.csv")
            broadcast.out(6).collect{ case c: cdm17.Principal => c.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "Principals.csv")
            broadcast.out(7).collect{ case c: cdm17.SrcSinkObject => c.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "SrcSinkObjects.csv")

            ClosedShape
          }).run()

        case (false, true) =>

          // TODO: Alec find a better way to get the "blocked" CSV information
          system.scheduler.schedule(0 seconds, 1 minutes, uuidRemapper, GetStillBlocked)

          RunnableGraph.fromGraph(GraphDSL.create(){ implicit graph =>
            import GraphDSL.Implicits._

            val broadcast = graph.add(Broadcast[Any](9))

            CDMSource.cdm18(ta1).via(EntityResolution(uuidRemapper, synSource))
              .via(FlowComponents.printCounter("DB Writer", 1000))
              .via(Flow.fromFunction {
                case Left(e) => e
                case Right(ir) => ir
              }) ~> broadcast.in

            broadcast.out(0).collect{ case EdgeAdm2Adm(AdmUUID(src), lbl, AdmUUID(tgt)) =>  src -> Map("label" -> lbl, "target" -> tgt) } ~> FlowComponents.csvFileSink(odir + File.separator + "AdmEdges.csv")
            broadcast.out(1).collect{ case c: AdmNetFlowObject => c.uuid.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "AdmNetFlowObjects.csv")
            broadcast.out(2).collect{ case c: AdmEvent => c.uuid.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "AdmEvents.csv")
            broadcast.out(3).collect{ case c: AdmFileObject => c.uuid.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "AdmFileObjects.csv")
            broadcast.out(4).collect{ case c: AdmProvenanceTagNode => c.uuid.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "AdmProvenanceTagNodes.csv")
            broadcast.out(5).collect{ case c: AdmSubject => c.uuid.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "AdmSubjects.csv")
            broadcast.out(6).collect{ case c: AdmPrincipal => c.uuid.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "AdmPrincipals.csv")
            broadcast.out(7).collect{ case c: AdmSrcSinkObject => c.uuid.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "AdmSrcSinkObjects.csv")
            broadcast.out(8).collect{ case c: AdmPathNode => c.uuid.uuid -> c.toMap } ~> FlowComponents.csvFileSink(odir + File.separator + "AdmPathNodes.csv")

            ClosedShape
          }).run()


        case (false, false) =>
          println("Generting CSVs for neither CDM not ADM - so... generating nothing!")
          Source.empty

        case (true, true) =>
          println("This isn't implemented yet. TODO: Alec")
          ???

      }

    case "ui" | "uionly" =>
      println("Staring only the UI and doing nothing else.")
      startWebServer()

    case "valuebytes" =>
      println("NOTE: this will run using CDM")

      CDMSource.cdm17(ta1)
        .collect{ case e: cdm17.Event if e.parameters.nonEmpty => e}
        .flatMapConcat(
          (e: cdm17.Event) => Source.fromIterator(
            () => e.parameters.get.flatMap( v =>
              v.valueBytes.map(b =>
                List(akka.util.ByteString(s"<<<BEGIN_LINE\t${e.uuid}\t${new String(b)}\tEND_LINE>>>\n"))
              ).getOrElse(List.empty)).toIterator
          )
        )
        .toMat(FileIO.toPath(Paths.get("ValueBytes.txt")))(Keep.right).run()


    case "uniqueuuids" =>
      println("Running unique UUID test")
      CDMSource.cdm17(ta1)
        .statefulMapConcat[(UUID,Boolean)] { () =>
        import scala.collection.mutable.{Map => MutableMap}
        val firstObservation = MutableMap.empty[UUID, CDM17]
        val ignoreUuid = new UUID(0L,0L);
        {
          case c: CDM17 with DBNodeable[_] if c.getUuid == ignoreUuid => List()
          case c: CDM17 with DBNodeable[_] if firstObservation.contains(c.getUuid) =>
            val comparison = firstObservation(c.getUuid) == c
            if ( ! comparison) println(s"Match Failure on UUID: ${c.getUuid}\nOriginal: ${firstObservation(c.getUuid)}\nThis:     $c\n")
            List()
          case c: CDM17 with DBNodeable[_] =>
            firstObservation += (c.getUuid -> c)
            List()
        }
      }.runWith(Sink.ignore)


    case "find" =>
      println("Running FIND flow")
      CDMSource.cdm18(ta1).via(FlowComponents.printCounter("Find"))
        .collect{ case cdm: Event if cdm.uuid == UUID.fromString("8265bd98-c015-52e9-9361-824e2ade7f4c") => cdm.toMap.toString + s"\n$cdm" }
        .runWith(Sink.foreach(println))


    case "novelty" | "novel" =>
      println("Running Novelty Detection Flow")
      val noveltyActor = system.actorOf(Props(classOf[NoveltyActor]), "novelty")
      CDMSource.cdm18(ta1)
        .via(FlowComponents.printCounter("Novelty"))
        .via(EntityResolution(uuidRemapper, synSource))
        .statefulMapConcat[(NoveltyDetection.Event, Option[ADM], Set[AdmPathNode], Option[ADM], Set[AdmPathNode])]{ () =>

          val events = collection.mutable.Map.empty[AdmUUID, (AdmEvent, Option[ADM], Option[ADM])]
          val everything = collection.mutable.Map.empty[AdmUUID, ADM]

          type AdmUUIDReferencingPathNodes = AdmUUID
          val pathNodeUses = collection.mutable.Map.empty[AdmUUIDReferencingPathNodes, Set[AdmUUID]]
          val pathNodes = collection.mutable.Map.empty[AdmUUID, AdmPathNode]

          val eventsWithPredObj2: Set[EventType] = Set(EVENT_RENAME, EVENT_MODIFY_PROCESS, EVENT_ACCEPT, EVENT_EXECUTE,
            EVENT_CREATE_OBJECT, EVENT_RENAME, EVENT_OTHER, EVENT_MMAP, EVENT_LINK, EVENT_UPDATE, EVENT_CREATE_THREAD)


//        type NodeType = _
//        type EdgeOpt = _
//        val l: List[(NodeType, EdgeOpt)] = List.empty
//        trait Direction
//        object Forward extends Direction
//        object Reverse extends Direction
//
//        class Aggregated(left: Either[ADM, Aggregated], right: ADM)
//
//        val leftThings = Map.empty[AdmUUID, Aggregated]
//        val rightThings = Map.empty[AdmUUID, ADM]
//        def foo(edge: EdgeAdm2Adm, labelFilter: String, dir: Direction = Forward) = if (edge.label == labelFilter) {
//          val (left, right) = if (dir == Forward) edge.src -> edge.tgt else edge.tgt -> edge.src
//          val t = (rightThings(left), rightThings(right))
//        }


          {
            case Left(EdgeAdm2Adm(src, "subject", tgt)) => everything.get(tgt)
              .fold(List.empty[(AdmEvent, Option[ADM], Set[AdmPathNode], Option[ADM], Set[AdmPathNode])]) { sub =>
                val e = events(src)   // EntityResolution flow step guarantees that the event nodes will arrive before the edge that references it.
                val t = (e._1, Some(sub), e._3)
                if (t._3.isDefined) {
                  if ( ! eventsWithPredObj2.contains(e._1.eventType)) events -= src
                  val subPathNodes = pathNodeUses.getOrElse(t._2.get.uuid, Set.empty).map(pathNodes.apply)
                  val objPathNodes = pathNodeUses.getOrElse(t._3.get.uuid, Set.empty).map(pathNodes.apply)
                  List((t._1, t._2, subPathNodes, t._3, objPathNodes))
                } else {
                  events += (src -> t)
                  Nil
                }
              }
            case Left(EdgeAdm2Adm(src, "predicateObject", tgt)) => everything.get(tgt)
              .fold(List.empty[(AdmEvent, Option[ADM], Set[AdmPathNode], Option[ADM], Set[AdmPathNode])]) { obj =>
                val e = events(src)   // EntityResolution flow step guarantees that the event nodes will arrive before the edge that references it.
                val t = (e._1, e._2, Some(obj))
                if (t._2.isDefined) {
                  if ( ! eventsWithPredObj2.contains(e._1.eventType)) events -= src
                  val subPathNodes = pathNodeUses.getOrElse(t._2.get.uuid, Set.empty).map(pathNodes.apply)
                  val objPathNodes = pathNodeUses.getOrElse(t._3.get.uuid, Set.empty).map(pathNodes.apply)
                  List((t._1, t._2, subPathNodes, t._3, objPathNodes))
                } else {
                  events += (src -> t)
                  Nil
                }
              }
            case Left(EdgeAdm2Adm(src, "predicateObject2", tgt)) => everything.get(tgt)
              .fold(List.empty[(AdmEvent, Option[ADM], Set[AdmPathNode], Option[ADM], Set[AdmPathNode])]) { obj =>
                val e = events(src)   // EntityResolution flow step guarantees that the event nodes will arrive before the edge that references it.
              val t = (e._1, e._2, Some(obj))
                if (t._2.isDefined) {
                  if ( ! eventsWithPredObj2.contains(e._1.eventType)) events -= src
                  val subPathNodes = pathNodeUses.getOrElse(t._2.get.uuid, Set.empty).map(pathNodes.apply)
                  val objPathNodes = pathNodeUses.getOrElse(t._3.get.uuid, Set.empty).map(pathNodes.apply)
                  List((t._1, t._2, subPathNodes, t._3, objPathNodes))
                } else {
                  events += (src -> t)
                  Nil
                }
              }
            case Left(EdgeAdm2Adm(subObj, label, pathNode)) if List("cmdLine", "(cmdLine)", "exec", "path", "(path)").contains(label) =>
              // TODO: What about Events which contain a new AdmPathNode definition/edge which arrives _just_ after the edge.
              val newSet: Set[AdmUUID] = pathNodeUses.getOrElse(subObj, Set.empty[AdmUUID]).+(pathNode)
              pathNodeUses += (subObj -> newSet)
              Nil

  //          case Left(edge) =>
  //              edge.label match { // throw away the referenced UUIDs!
  //                case "subject" | "flowObject" => everything -= edge.src
  //                case "eventExec" | "cmdLine" | "(cmdLine)" | "exec" | "localPrincipal" | "principal" | "path" | "(path)" => everything -= edge.tgt
  //                case "tagIds" | "prevTagId" => everything -= edge.src; everything -= edge.tgt
  //                case _ => ()  // "parentSubject"
  //              }
  //            List()
            case Right(adm: AdmEvent) =>
              events += (adm.uuid -> (adm, None, None))
              Nil
            case Right(adm: AdmSubject) =>
              everything += (adm.uuid -> adm)
              Nil
            case Right(adm: AdmFileObject) =>
              everything += (adm.uuid -> adm)
              Nil
            case Right(adm: AdmNetFlowObject) =>
              everything += (adm.uuid -> adm)
              Nil
            case Right(adm: AdmSrcSinkObject) =>
              everything += (adm.uuid -> adm)
              Nil
            case Right(adm: AdmPathNode) =>
              pathNodes += (adm.uuid -> adm)
              Nil
            case _ => Nil
          }
        }
        .runWith(
          Sink.actorRefWithAck(noveltyActor, InitMsg, Ack, CompleteMsg)
        )

    case _ =>
      println("Unknown runflow argument. Quitting.")
      Runtime.getRuntime.halt(1)
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

  //  Make a CDM17 source
  def cdm17(ta1: String, handleError: (Int, Throwable) => Unit = (_,_) => { }): Source[CDM17, _] = {
    println(s"Setting source for: $ta1")
    val start = Try(config.getLong("adapt.ingest.startatoffset")).getOrElse(0L)
    val shouldLimit = Try(config.getLong("adapt.ingest.loadlimit")) match {
      case Success(0) => None
      case Success(i) => Some(i)
      case _ => None
    }
    ta1.toLowerCase match {
      case "cadets"         =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic"), kafkaCdm17Parser).drop(start)
        Application.instrumentationSource = "cadets"
        shouldLimit.fold(src)(l => src.take(l))
      case "clearscope"     =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic"), kafkaCdm17Parser).drop(start)
        Application.instrumentationSource = "clearscope"
        shouldLimit.fold(src)(l => src.take(l))
      case "faros"          =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic"), kafkaCdm17Parser).drop(start)
        Application.instrumentationSource = "faros"
        shouldLimit.fold(src)(l => src.take(l))
      case "fivedirections" =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic"), kafkaCdm17Parser).drop(start)
        Application.instrumentationSource = "fivedirections"
        shouldLimit.fold(src)(l => src.take(l))
      case "theia"          =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic"), kafkaCdm17Parser).drop(start)
        Application.instrumentationSource = "theia"
        shouldLimit.fold(src)(l => src.take(l))
          .merge(kafkaSource(config.getString("adapt.env.theiaresponsetopic"), kafkaCdm17Parser).via(FlowComponents.printCounter("Theia Query Response", 1)))
      case "trace"          =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic"), kafkaCdm17Parser).drop(start)
        Application.instrumentationSource = "trace"
        shouldLimit.fold(src)(l => src.take(l))
      case "kafkaTest"      =>
        val src = kafkaSource("kafkaTest", kafkaCdm17Parser).drop(start) //.throttle(500, 5 seconds, 1000, ThrottleMode.shaping)
        shouldLimit.fold(src)(l => src.take(l))
      case _ =>

        // TODO: This is an ugly hack to handle paths like ~/Documents/file.avro
        val paths = config.getStringList("adapt.ingest.loadfiles").asScala.map { path =>
          path.replaceFirst("^~",System.getProperty("user.home"));
        }
        println(s"Setting file sources to: ${paths.mkString(", ")}")

        val startStream = paths.foldLeft(Source.empty[Try[CDM17]])((a,b) => a.concat{
          Source.fromIterator[Try[CDM17]](() => {
            val read = CDM17.readData(b, None)

            read.map(_._1) match {
              case Failure(_) => None
              case Success(s) => Application.instrumentationSource = Ta1Flows.getSourceName(s)
            }

            read.get._2
          })
        }).drop(start)
        shouldLimit.fold(startStream)(l => startStream.take(l))
          .statefulMapConcat { () =>
            var counter = 0
            cdmTry => {
              counter = counter + 1
              cdmTry match {
                case Success(cdm) => List(cdm)
                case Failure(err) =>
                  println(s"Couldn't read binary data at offset: $counter")
                  handleError(counter, err)
                  List.empty
              }
            }
          }
    }
  }

  // Make a CDM18 source, possibly falling back on CDM17 for files with that version
  def cdm18(ta1: String, handleError: (Int, Throwable) => Unit = (_,_) => { }): Source[CDM18, _] = {
    println(s"Setting source for: $ta1")
    val start = Try(config.getLong("adapt.ingest.startatoffset")).getOrElse(0L)
    val shouldLimit = Try(config.getLong("adapt.ingest.loadlimit")) match {
      case Success(0) => None
      case Success(i) => Some(i)
      case _ => None
    }
    ta1.toLowerCase match {
      case "cadets"         =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic"), kafkaCdm18Parser).drop(start)
        Application.instrumentationSource = "cadets"
        shouldLimit.fold(src)(l => src.take(l))
      case "clearscope"     =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic"), kafkaCdm18Parser).drop(start)
        Application.instrumentationSource = "clearscope"
        shouldLimit.fold(src)(l => src.take(l))
      case "faros"          =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic"), kafkaCdm18Parser).drop(start)
        Application.instrumentationSource = "faros"
        shouldLimit.fold(src)(l => src.take(l))
      case "fivedirections" =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic"), kafkaCdm18Parser).drop(start)
        Application.instrumentationSource = "fivedirections"
        shouldLimit.fold(src)(l => src.take(l))
      case "theia"          =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic"), kafkaCdm18Parser).drop(start)
        Application.instrumentationSource = "theia"
        shouldLimit.fold(src)(l => src.take(l))
      case "trace"          =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic"), kafkaCdm18Parser).drop(start)
        Application.instrumentationSource = "trace"
        shouldLimit.fold(src)(l => src.take(l))
      case "kafkaTest"      =>
        val src = kafkaSource("kafkaTest", kafkaCdm18Parser).drop(start) //.throttle(500, 5 seconds, 1000, ThrottleMode.shaping)
        shouldLimit.fold(src)(l => src.take(l))
      case _ =>
        // TODO: This is an ugly hack to handle paths like ~/Documents/file.avro
        val paths = config.getStringList("adapt.ingest.loadfiles").asScala.map { path =>
          path.replaceFirst("^~",System.getProperty("user.home"));
        }
        println(s"Setting file sources to: ${paths.mkString(", ")}")
        val startStream = paths.foldLeft(Source.empty[Try[CDM18]])((a,b) => a.concat({

          val read = CDM18.readData(b, None)
          read.map(_._1) match {
            case Failure(_) => None
            case Success(s) => Application.instrumentationSource = Ta1Flows.getSourceName(s)
          }

          // Try to read CDM18 data. If we fail, fall back on reading CDM17 data, then convert that to CDM18
          val cdm18: Iterator[Try[CDM18]] = read.map(_._2).getOrElse({
            println("Failed to read file as CDM18, trying to read it as CDM17...")

            val dummyHost: UUID = new java.util.UUID(0L,1L)

            val read = CDM17.readData(b, None)
            read.map(_._1) match {
              case Failure(_) => None
              case Success(s) => Application.instrumentationSource = Ta1Flows.getSourceName(s)
            }

            read.map(_._2).get.flatMap {
              case Failure(e) => List(Failure[CDM18](e))
              case Success(cdm17) => cdm17ascdm18(cdm17, dummyHost).toList.map(Success(_))
            }
          })
          Source.fromIterator[Try[CDM18]](() => cdm18)
        })).drop(start)
        shouldLimit.fold(startStream)(l => startStream.take(l))
          .statefulMapConcat { () =>
            var counter = 0
            cdmTry => {
              counter = counter + 1
              cdmTry match {
                case Success(cdm) => List(cdm)
                case Failure(err) =>
                  println(s"Couldn't read binary data at offset: $counter")
                  handleError(counter, err)
                  List.empty
              }
            }
          }
    }
  }

  // Make a CDM18 source from a CDM17 one
  def cdm17ascdm18(c: CDM17, dummyHost: UUID): Option[CDM18] = {
    implicit val _: UUID = dummyHost
    c match {
      case e: cdm17types.Event => Some(Cdm17to18.event(e))
      case f: cdm17types.FileObject => Some(Cdm17to18.fileObject(f))
      case m: cdm17types.MemoryObject => Some(Cdm17to18.memoryObject(m))
      case n: cdm17types.NetFlowObject => Some(Cdm17to18.netFlowObject(n))
      case p: cdm17types.Principal => Some(Cdm17to18.principal(p))
      case p: cdm17types.ProvenanceTagNode => Some(Cdm17to18.provenanceTagNode(p))
      case r: cdm17types.RegistryKeyObject => Some(Cdm17to18.registryKeyObject(r))
      case s: cdm17types.SrcSinkObject => Some(Cdm17to18.srcSinkObject(s))
      case s: cdm17types.Subject => Some(Cdm17to18.subject(s))
      case t: cdm17types.TimeMarker => Some(Cdm17to18.timeMarker(t))
      case u: cdm17types.UnitDependency => Some(Cdm17to18.unitDependency(u))
      case u: cdm17types.UnnamedPipeObject => Some(Cdm17to18.unnamedPipeObject(u))
      case other =>
        println(s"couldn't find a way to convert $other")
        None
    }
  }

  // Make a CDM source from a kafka topic
  def kafkaSource[C](ta1Topic: String, parser: ConsumerRecord[Array[Byte], Array[Byte]] => Try[C]): Source[C, _] =
    Consumer.plainSource(
      ConsumerSettings(config.getConfig("akka.kafka.consumer"), new ByteArrayDeserializer, new ByteArrayDeserializer),
      Subscriptions.assignmentWithOffset(new TopicPartition(ta1Topic, 0), offset = config.getLong("adapt.ingest.startatoffset"))
    ) .map(parser)
      .mapConcat(c => if (c.isSuccess) List(c.get) else List.empty)

  // Parse a `CDM17` from a kafka record
  def kafkaCdm17Parser(msg: ConsumerRecord[Array[Byte], Array[Byte]]): Try[CDM17] = Try {
    val bais = new ByteArrayInputStream(msg.value())  // msg.record.value()
    val offset = msg.offset()   // msg.record.offset()
    val reader = new SpecificDatumReader(classOf[com.bbn.tc.schema.avro.cdm17.TCCDMDatum])
    val decoder = DecoderFactory.get.binaryDecoder(bais, null)
    val t = Try {
      val elem: com.bbn.tc.schema.avro.cdm17.TCCDMDatum = reader.read(null, decoder)
      elem
    }
    if (t.isFailure) println(s"Couldn't read binary data at offset: $offset")
    val cdm = new RawCDM17Type(t.get.getDatum)
    CDM17.parse(cdm)
  }.flatten

  // Parse a `CDM18` from a kafka record
  def kafkaCdm18Parser(msg: ConsumerRecord[Array[Byte], Array[Byte]]): Try[CDM18] = Try {
    val bais = new ByteArrayInputStream(msg.value())  // msg.record.value()
    val offset = msg.offset()   // msg.record.offset()
    val reader = new SpecificDatumReader(classOf[com.bbn.tc.schema.avro.cdm18.TCCDMDatum])
    val decoder = DecoderFactory.get.binaryDecoder(bais, null)
    val t = Try {
      val elem: com.bbn.tc.schema.avro.cdm18.TCCDMDatum = reader.read(null, decoder)
      elem
    }
    if (t.isFailure) println(s"Couldn't read binary data at offset: $offset")
    val cdm = new RawCDM18Type(t.get.getDatum)
    CDM18.parse(cdm)
  }.flatten
}


object Ta1Flows {
  // Get the name of the instrumentation source
  def getSourceName(a: AnyRef): String = a.toString.split("_").last.toLowerCase
}
