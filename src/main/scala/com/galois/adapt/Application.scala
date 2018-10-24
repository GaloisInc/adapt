package com.galois.adapt

import java.io._
import java.nio.file.Paths
import java.util.UUID
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteResult._
import akka.pattern.ask
import akka.stream.{ActorMaterializer, _}
import akka.stream.scaladsl._
import akka.util.{ByteString, Timeout}
import com.galois.adapt.adm._
import scala.collection.parallel.ParMap
import FlowComponents._
import akka.event.{Logging, LoggingAdapter}
import com.galois.adapt.FilterCdm.Filter
import com.galois.adapt.MapSetUtils.{AlmostMap, AlmostSet}
import com.galois.adapt.cdm19._
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.postfixOps
import scala.util.{Failure, Success}


object Application extends App {
  org.slf4j.LoggerFactory.getILoggerFactory  // This is here just to make SLF4j shut up and not log lots of error messages when instantiating the Kafka producer.

  import AdaptConfig._

  implicit val system = ActorSystem("production-actor-system")
  val log: LoggingAdapter = Logging.getLogger(system, this)

  // All large maps should be store in `MapProxy`
  val mapProxy: MapProxy = new MapProxy(
    fileDbPath = runFlow match { case "accept" => None; case _ => Some(admConfig.mapdb)},
    fileDbBypassChecksum = admConfig.mapdbbypasschecksum,
    fileDbTransactions = admConfig.mapdbtransactions,

    admConfig.uuidRemapperShards,
    cdm2cdmLruCacheSize = admConfig.cdm2cdmlrucachesize,
    cdm2admLruCacheSize = admConfig.cdm2admlrucachesize,
    dedupEdgeCacheSize = admConfig.dedupEdgeCacheSize
  )

//    new File(this.getClass.getClassLoader.getResource("bin/iforest.exe").getPath).setExecutable(true)
//    new File(config.getString("adapt.runtime.iforestpath")).setExecutable(true)

  val quitOnError = runtimeConfig.quitonerror
  val streamErrorStrategy: Supervision.Decider = {
    case e: Throwable =>
      e.printStackTrace()
      if (quitOnError) Runtime.getRuntime.halt(1)
      Supervision.Resume
  }
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system).withSupervisionStrategy(streamErrorStrategy))
  implicit val executionContext = system.dispatcher

  val statusActor = system.actorOf(Props[StatusActor], name = "statusActor")
  val logFile = runtimeConfig.logfile
  val scheduledLogging = system.scheduler.schedule(10.seconds, 10.seconds, statusActor, LogToDisk(logFile))
  system.registerOnTermination(scheduledLogging.cancel())

  val ppmBaseDirPath = ppmConfig.basedir
  val ppmBaseDirFile = new File(ppmBaseDirPath)
  if ( ! ppmBaseDirFile.exists()) ppmBaseDirFile.mkdir()

  // Start up the database
  val dbActor: ActorRef = runFlow match {
    case "accept" => system.actorOf(Props(classOf[TinkerGraphDBQueryProxy]))
    case _ => system.actorOf(Props(classOf[Neo4jDBQueryProxy], statusActor))
  }
  val dbStartUpTimeout = Timeout(600 seconds)  // Don't make this implicit.
  println(s"Waiting for DB indices to become active: $dbStartUpTimeout")
  Await.result(dbActor.?(Ready)(dbStartUpTimeout), dbStartUpTimeout.duration)

  // These are the maps that `UUIDRemapper` will use
  val cdm2cdmMaps: Array[AlmostMap[CdmUUID,CdmUUID]] = mapProxy.cdm2cdmMapShards
  val cdm2admMaps: Array[AlmostMap[CdmUUID,AdmUUID]] = mapProxy.cdm2admMapShards

  // Edges blocked waiting for a target CDM uuid to be remapped.
  val blockedEdgesMaps: Array[mutable.Map[CdmUUID, (List[Edge], Set[CdmUUID])]] = mapProxy.blockedEdgesShards

  val seenEdges: Array[AlmostSet[EdgeAdm2Adm]] = mapProxy.seenEdgesShards
  val seenNodes: Array[AlmostSet[AdmUUID]] = mapProxy.seenNodesShards
  val shardCount: Array[Int] = Array.fill(admConfig.uuidRemapperShards)(0)

  val singleIngestHost = ingestConfig.asSingleHost
  val instrumentationSource: String = singleIngestHost.simpleTa1Name
  val startingCount = {
    val List(li: LinearIngest) = singleIngestHost.parallelIngests.toList
    li.range.startInclusive
  }
  val er = EntityResolution(
    admConfig,
    singleIngestHost.isWindows,
    cdm2cdmMaps,
    cdm2admMaps,
    blockedEdgesMaps,
    shardCount,
    log,
    seenNodes,
    seenEdges
  )

  val hostNameForAllHosts = HostName("BetweenHosts")
  val ppmManagerActors: Map[HostName, ActorRef] = runFlow match {
    case "accept" => Map.empty
    case _ =>
      val hostNames = ingestConfig.hosts.map(_.hostName)
      hostNames.map { hostName =>
        val ref = system.actorOf(Props(classOf[PpmManager], hostName), s"ppm-actor-$hostName")
        ppmConfig.saveintervalseconds match {
          case Some(i) if i > 0L =>
            println(s"Saving PPM trees every $i seconds")
            val cancellable = system.scheduler.schedule(i.seconds, i.seconds, ref, SaveTrees())
            system.registerOnTermination(cancellable.cancel())
          case _ => println("Not going to periodically save PPM trees.")
        }
        hostName -> ref
      }.toMap + (hostNameForAllHosts -> system.actorOf(Props(classOf[PpmManager], hostNameForAllHosts.hostname), s"ppm-actor-${hostNameForAllHosts.hostname}"))
  }
  def ppmDistributorSink[T] = Sink.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._
    val actorList: List[ActorRef] = ppmManagerActors.toList.map(_._2)
    val broadcast = b.add(Broadcast[T](actorList.size))
    actorList.foreach { ref => broadcast ~> Sink.actorRefWithAck(ref, InitMsg, Ack, CompleteMsg) }
    SinkShape(broadcast.in)
  })

  // Coarse grain filtering of the input CDM
  var filter: Option[Filterable => Boolean] = None
  var filterAst: Option[Filter] = None
  val filterFlow: Flow[(String,CDM19),(String,CDM19),_] = Flow[(String,CDM19)]
    .map[(String, Either[Filterable,CDM19])] {
      case (s, c: Event) => (s, Left(Filterable.apply(c)))
      case (s, c: FileObject) => (s, Left(Filterable.apply(c)))
//      case (s, c: Host) => (s, Left(Filterable.apply(c)))
      case (s, c: MemoryObject) => (s, Left(Filterable.apply(c)))
      case (s, c: NetFlowObject) => (s, Left(Filterable.apply(c)))
      case (s, c: PacketSocketObject) => (s, Left(Filterable.apply(c)))
      case (s, c: Principal) => (s, Left(Filterable.apply(c)))
      case (s, c: ProvenanceTagNode) => (s, Left(Filterable.apply(c)))
      case (s, c: RegistryKeyObject) => (s, Left(Filterable.apply(c)))
      case (s, c: SrcSinkObject) => (s, Left(Filterable.apply(c)))
      case (s, c: Subject) => (s, Left(Filterable.apply(c)))
      case (s, c: TagRunLengthTuple) => (s, Left(Filterable.apply(c)))
      case (s, c: UnitDependency) => (s, Left(Filterable.apply(c)))
      case (s, c: IpcObject) => (s, Left(Filterable.apply(c)))
      case (s, other) => (s, Right(other))
    }
    .filter {
      case (s, Left(f)) => filter.fold(true)(func => func(f))
      case (s, right) => true
    }
    .map[(String, CDM19)] {
      case (s, Left(f)) => (s, f.underlying)
      case (s, Right(cdm)) => (s, cdm)
    }




  // Mutable state that gets updated during ingestion
  var failedStatements: List[(Int, String)] = Nil

  def startWebServer(): Http.ServerBinding = {
    println(s"Starting the web server at: http://${runtimeConfig.webinterface}:${runtimeConfig.port}")
    val route = Routes.mainRoute(dbActor, statusActor, ppmManagerActors, cdm2admMaps, cdm2cdmMaps)
    val httpServer = Http().bindAndHandle(route, runtimeConfig.webinterface, runtimeConfig.port)
    Await.result(httpServer, 10 seconds)
  }

  runFlow match {

    case "accept" =>
      println("Running acceptance tests")

      val writeTimeout = Timeout(30.1 seconds)

      val sink = Sink.fromGraph(GraphDSL.create() { implicit b =>
        import GraphDSL.Implicits._
        val broadcast = b.add(Broadcast[(String,CDM19)](1))

        broadcast ~> DBQueryProxyActor.graphActorCdm19WriteSink(dbActor, CdmDone)(writeTimeout)
     //   broadcast ~> EntityResolution(uuidRemapper) ~> Neo4jFlowComponents.neo4jActorAdmWriteSink(dbActor, AdmDone)(writeTimeout)
        SinkShape(broadcast.in)
      })

      startWebServer()

      val handler = new ErrorHandler {
        override def handleError(offset: Long, error: Throwable): Unit = {
          failedStatements = (offset.toInt, error.getMessage) :: failedStatements
        }
      }

      singleIngestHost.toCdmSource(handler)
        .via(printCounter("CDM events", statusActor, startingCount))
        .recover{ case e: Throwable => e.printStackTrace(); ??? }
        .runWith(sink)


    case "database" | "db" | "ingest" =>
      val completionMsg = if (ingestConfig.quitafteringest) {
        println("Will terminate after ingest.")
        KillJVM
      } else CompleteMsg
      val writeTimeout = Timeout(30.1 seconds)

      val (name, sink) = ingestConfig.produce match {
        case ProduceCdm => "CDM" -> DBQueryProxyActor.graphActorCdm19WriteSink(dbActor, completionMsg)(writeTimeout)
        case ProduceAdm => "ADM" -> er.to(DBQueryProxyActor.graphActorAdmWriteSink(dbActor, completionMsg))
        case ProduceCdmAndAdm => "CDM+ADM" -> Sink.fromGraph(GraphDSL.create() { implicit b =>
          import GraphDSL.Implicits._
          val broadcast = b.add(Broadcast[(String,CDM19)](2))

          broadcast ~> DBQueryProxyActor.graphActorCdm19WriteSink(dbActor, completionMsg)(writeTimeout)
          broadcast ~> er ~> DBQueryProxyActor.graphActorAdmWriteSink(dbActor, completionMsg)

          SinkShape(broadcast.in)
        })
      }

      println(s"Running database flow for $name with UI.")
      startWebServer()
      singleIngestHost.toCdmSource()
        .buffer(10000, OverflowStrategy.backpressure)
        .via(printCounter(name, statusActor, startingCount))
        .runWith(sink)

    case "train" =>
      startWebServer()
      statusActor ! InitMsg

      singleIngestHost.toCdmSource()
        .via(printCounter("E3 Training", statusActor, startingCount))
        .via(splitToSink[(String, CDM19)](ppmDistributorSink, 1000))
        .via(er)
        .runWith(PpmFlowComponents.ppmSink)

    case "e3" =>
      startWebServer()
      statusActor ! InitMsg

      singleIngestHost.toCdmSource()
        .via(printCounter("E3", statusActor, startingCount))
        .via(filterFlow)
        .via(splitToSink[(String, CDM19)](ppmDistributorSink, 1000))
        .via(er)
        .via(splitToSink(PpmFlowComponents.ppmSink, 1000))
        .runWith(DBQueryProxyActor.graphActorAdmWriteSink(dbActor))

    case "e3-no-db" =>
      startWebServer()
      statusActor ! InitMsg

      singleIngestHost.toCdmSource()
      // CDMSource.cdm19(ta1, handleError = { case (off, t) =>println(s"Error at $off: ${t.printStackTrace}") })
        .via(printCounter("E3 (no DB)", statusActor, startingCount))
        .via(filterFlow)
        .via(splitToSink[(String, CDM19)](ppmDistributorSink, 1000))
        .via(er)
        .runWith(PpmFlowComponents.ppmSink)

    case "print-cdm" =>
      var i = 0
      singleIngestHost.toCdmSource()
        .map(cdm => println(s"Record $i: ${cdm.toString}"))
        .runWith(Sink.ignore)

    case "event-matrix" =>
      // Produce a CSV of which fields in a CDM event are filled in

      startWebServer()
      statusActor ! InitMsg

      def getKeys(e: Event): Set[String] = List.concat(
        if (e.sequence.isDefined) List("sequence") else Nil,
        if (e.threadId.isDefined) List("threadId") else Nil,
        if (e.subjectUuid.isDefined) List("subjectUuid") else Nil,
        if (e.predicateObject.isDefined) List("predicateObject") else Nil,
        if (e.predicateObjectPath.isDefined) List("predicateObjectPath") else Nil,
        if (e.predicateObject2.isDefined) List("predicateObject2") else Nil,
        if (e.predicateObject2Path.isDefined) List("predicateObject2Path") else Nil,
        if (e.names.nonEmpty) List("name") else Nil,
        if (e.parameters.isDefined) List("parameters") else Nil,
        if (e.location.isDefined) List("location") else Nil,
        if (e.size.isDefined) List("size") else Nil,
        if (e.programPoint.isDefined) List("programPoint") else Nil,
        e.properties.getOrElse(Map.empty).keys.toList
      ).toSet

      var keysSeen: Set[String] = Set.empty
      var currentKeys: List[String] = List.empty

      val tempFile = File.createTempFile("event-matrix","csv")
      tempFile.deleteOnExit()
      val tempPath: String = tempFile.getPath

      singleIngestHost.toCdmSource()
        .via(printCounter("CDM", statusActor, startingCount))
        .collect { case (_, e: Event) => getKeys(e) }
        .map((keysHere: Set[String]) => {
          val newKeys: Set[String] = keysHere.diff(keysSeen)
          currentKeys ++= newKeys
          keysSeen ++= newKeys
          ByteString(currentKeys.map(k => if (keysHere.contains(k)) "1" else "").mkString(",") + "\n")
        })
        .runWith(FileIO.toPath(Paths.get(tempPath)))
        .onComplete {
          case Failure(f) =>
            println("Failed to write out 'event-matrix.csv")
            f.printStackTrace()

          case Success(t) =>
            t.status.map { _ =>
              val tempFileInputStream = new FileInputStream(tempFile)
              val eventFileOutputStream = new FileOutputStream(new File("event-matrix.csv"))
              eventFileOutputStream.write((currentKeys.mkString(",") + "\n").getBytes)
              org.apache.commons.io.IOUtils.copy(tempFileInputStream, eventFileOutputStream)
              println("Done")
            }
        }

    case "csvmaker" | "csv" =>
      val odir = pureconfig.loadConfig[String]("adapt.outdir").right.getOrElse(".")

      startWebServer()
      statusActor ! InitMsg

      ingestConfig.produce match {
        case ProduceCdm | ProduceCdmAndAdm => println("Producing CSVs from CDM is no longer supported")
        case ProduceAdm =>
          RunnableGraph.fromGraph(GraphDSL.create(){ implicit graph =>
            import GraphDSL.Implicits._

            val broadcast = graph.add(Broadcast[Any](9))

            singleIngestHost.toCdmSource()
              .via(printCounter("DB Writer", statusActor, startingCount, 10000))
              .via(er)
              .via(Flow.fromFunction {
                case Left(e) => e
                case Right(ir) => ir
              }) ~> broadcast.in

            broadcast.out(0).collect{ case EdgeAdm2Adm(AdmUUID(src,n), lbl, tgt) =>  src -> Map("src-name" -> n, "label" -> lbl, "target" -> tgt.rendered) } ~> FlowComponents.csvFileSink(odir + File.separator + "AdmEdges.csv")
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
      }

    case "ui" | "uionly" =>
      println("Staring only the UI and doing nothing else.")
      startWebServer()

    case "valuebytes" =>
      println("NOTE: this will run using CDM")

      singleIngestHost.toCdmSource()
        .collect{ case (_, e: Event) if e.parameters.nonEmpty => e}
        .flatMapConcat(
          (e: Event) => Source.fromIterator(
            () => e.parameters.get.flatMap( v =>
              v.valueBytes.map(b =>
                List(akka.util.ByteString(s"<<<BEGIN_LINE\t${e.uuid}\t${new String(b)}\tEND_LINE>>>\n"))
              ).getOrElse(List.empty)).toIterator
          )
        )
        .toMat(FileIO.toPath(Paths.get("ValueBytes.txt")))(Keep.right).run()


    case "uniqueuuids" =>
      println("Running unique UUID test")
      statusActor ! InitMsg
      singleIngestHost.toCdmSource()
        .via(printCounter("UniqueUUIDs", statusActor, startingCount))
        .statefulMapConcat[(UUID,Boolean)] { () =>
        import scala.collection.mutable.{Map => MutableMap}
        val firstObservation = MutableMap.empty[UUID, CDM19]
        val ignoreUuid = new UUID(0L,0L);
        {
          case (name, c: CDM19 with DBNodeable[_]) if c.getUuid == ignoreUuid => List()
          case (name, c: CDM19 with DBNodeable[_]) if firstObservation.contains(c.getUuid) =>
            val comparison = firstObservation(c.getUuid) == c
            if ( ! comparison) println(s"Match Failure on UUID: ${c.getUuid}\nOriginal: ${firstObservation(c.getUuid)}\nThis:     $c\n")
            List()
          case (name, c: CDM19 with DBNodeable[_]) =>
            firstObservation += (c.getUuid -> c)
            List()
          case _ => List.empty
        }
      }.runWith(Sink.ignore)

    case "novelty" | "novel" | "ppm" | "ppmonly" =>
      println("Running Novelty Detection Flow")
      statusActor ! InitMsg
      singleIngestHost.toCdmSource()
        .via(printCounter("Novelty", statusActor, startingCount))
        .via(er)
        .runWith(PpmFlowComponents.ppmSink)
      startWebServer()

    case _ =>
      println("Unknown runflow argument. Quitting.")
      Runtime.getRuntime.halt(1)
  }
}
