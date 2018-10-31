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
import FlowComponents._
import akka.NotUsed
import akka.event.{Logging, LoggingAdapter}
import com.galois.adapt.FilterCdm.Filter
import com.galois.adapt.MapSetUtils.{AlmostMap, AlmostSet}
import com.galois.adapt.cdm19._

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps
import scala.util.{Failure, Success}
import AdaptConfig._
import com.galois.adapt.PpmFlowComponents.CompletedESO


object Application extends App {
  org.slf4j.LoggerFactory.getILoggerFactory  // This is here just to make SLF4j shut up and not log lots of error messages when instantiating the Kafka producer.

  implicit val system = ActorSystem("production-actor-system")
  val log: LoggingAdapter = Logging.getLogger(system, this)

  // All large maps should be store in `MapProxy`
  val hostNames: List[HostName] = ingestConfig.hosts.toList.map(_.hostName)
  val hostNameForAllHosts = "BetweenHosts"
  val mapProxy: MapProxy = new MapProxy(
    fileDbPath = runFlow match { case "accept" => None; case _ => Some(admConfig.mapdb)},
    fileDbBypassChecksum = admConfig.mapdbbypasschecksum,
    fileDbTransactions = admConfig.mapdbtransactions,

    admConfig.uuidRemapperShards,
    hostNames,
    hostNameForAllHosts,
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
  val cdm2cdmMaps: Map[HostName, Array[AlmostMap[CdmUUID,CdmUUID]]] = mapProxy.cdm2cdmMapShardsMap
  val cdm2admMaps: Map[HostName, Array[AlmostMap[CdmUUID,AdmUUID]]] = mapProxy.cdm2admMapShardsMap

  // Edges blocked waiting for a target CDM uuid to be remapped.
  val blockedEdgesMaps: Map[HostName, Array[mutable.Map[CdmUUID, (List[Edge], Set[CdmUUID])]]] = mapProxy.blockedEdgesShardsMap

  val seenEdgesMaps: Map[HostName, Array[AlmostSet[EdgeAdm2Adm]]] = mapProxy.seenEdgesShardsMap
  val seenNodesMaps: Map[HostName, Array[AlmostSet[AdmUUID]]] = mapProxy.seenNodesShardsMap
  val shardCount: Array[Int] = Array.fill(admConfig.uuidRemapperShards)(0)

  // Mutable state that gets updated during ingestion
  var failedStatements: List[(Int, String)] = Nil

  val errorHandler: ErrorHandler = runFlow match {
    case "accept" => new ErrorHandler {
      override def handleError(offset: Long, error: Throwable): Unit = {
        failedStatements = (offset.toInt, error.getMessage) :: failedStatements
      }
    }
    case _ => ErrorHandler.print
  }

  val cdmSources: Map[HostName, (IngestHost, Source[(Namespace,CDM19), NotUsed])] = ingestConfig.hosts.map { host: IngestHost =>
    host.hostName -> (host, host.toCdmSource(errorHandler))
  }.toMap

  val erMap: Map[HostName, Flow[(String,CurrentCdm), Either[ADM, EdgeAdm2Adm], NotUsed]] = runFlow match {
    case "accept" => Map.empty
    case _ => ingestConfig.hosts.map { host: IngestHost =>
      host.hostName -> EntityResolution(
        admConfig,
        host,
        cdm2cdmMaps(host.hostName),
        cdm2admMaps(host.hostName),
        blockedEdgesMaps(host.hostName),
        shardCount,
        log,
        seenNodesMaps(host.hostName),
        seenEdgesMaps(host.hostName)
      )
    }.toMap
  }
  val betweenHostDedup: Flow[Either[ADM, EdgeAdm2Adm], Either[ADM, EdgeAdm2Adm], NotUsed] = if (hostNames.size <= 1) {
    Flow.apply[Either[ADM, EdgeAdm2Adm]]
  } else {
    DeduplicateNodesAndEdges.apply(
      admConfig.uuidRemapperShards,
      seenNodesMaps(hostNameForAllHosts),
      seenEdgesMaps(hostNameForAllHosts)
    )
  }

  val ppmManagerActors: Map[HostName, ActorRef] = runFlow match {
    case "accept" => Map.empty
    case _ =>
      ingestConfig.hosts.map { host: IngestHost =>
        val props = Props(classOf[PpmManager], host.hostName, host.simpleTa1Name, host.isWindows)
        val ref = system.actorOf(props, s"ppm-actor-${host.hostName}")
        ppmConfig.saveintervalseconds match {
          case Some(i) if i > 0L =>
            println(s"Saving PPM trees every $i seconds")
            val cancellable = system.scheduler.schedule(i.seconds, i.seconds, ref, SaveTrees())
            system.registerOnTermination(cancellable.cancel())
          case _ => println("Not going to periodically save PPM trees.")
        }
        host.hostName -> ref
      }.toMap + (hostNameForAllHosts -> system.actorOf(Props(classOf[PpmManager], hostNameForAllHosts, "<no-name>", false), s"ppm-actor-$hostNameForAllHosts"))
        // TODO nichole:  what instrumentation source should I give to the `hostNameForAllHosts` PpmManager? This smells bad...
  }

  // Produce a Sink which accepts any type of observation to distribute as an observation to PPM tree actors for every host.
  def ppmObservationDistributorSink[T]: Sink[T, NotUsed] = Sink.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._
    val actorList: List[ActorRef] = ppmManagerActors.toList.map(_._2)
    val broadcast = b.add(Broadcast[T](actorList.size))
    actorList.foreach { ref => broadcast ~> Sink.actorRefWithAck[T](ref, InitMsg, Ack, CompleteMsg) }
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



  def startWebServer(): Http.ServerBinding = {
    println(s"Starting the web server at: http://${runtimeConfig.webinterface}:${runtimeConfig.port}")
    val route = Routes.mainRoute(dbActor, statusActor, ppmManagerActors)
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

      val handler: ErrorHandler = new ErrorHandler {
        override def handleError(offset: Long, error: Throwable): Unit = {
          failedStatements = (offset.toInt, error.getMessage) :: failedStatements
        }
      }

      assert(cdmSources.size == 1, "Cannot run tests for more than once host at a time")
      val (host, cdmSource) = cdmSources.head._2

      assert(host.parallelIngests.size == 1, "Cannot run tests for more than one linear ingest stream")
      val li = host.parallelIngests.head

      cdmSource
        .via(printCounter("CDM events", statusActor, li.range.startInclusive))
        .recover{ case e: Throwable => e.printStackTrace(); ??? }
        .runWith(sink)


    case "database" | "db" | "ingest" =>
      val completionMsg = if (ingestConfig.quitafteringest) {
        println("Will terminate after ingest.")
        KillJVM
      } else CompleteMsg
      val writeTimeout = Timeout(30.1 seconds)

      println(s"Running database flow to ${ingestConfig.produce} with UI.")
      startWebServer()

      ingestConfig.produce match {
        case ProduceCdm =>
          RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
            import GraphDSL.Implicits._

            val sources = cdmSources.values.toSeq
            val merge = b.add(Merge[(Namespace,CDM19)](sources.size))

            for (((host, source), i) <- sources.zipWithIndex) {
              source.via(printCounter(host.hostName, statusActor, 0)) ~> merge.in(i)
            }

            merge.out ~> DBQueryProxyActor.graphActorCdm19WriteSink(dbActor, completionMsg)(writeTimeout)

            ClosedShape
          }).run()

        case ProduceAdm =>
          RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
            import GraphDSL.Implicits._

            val sources = cdmSources.values.toSeq
            val merge = b.add(Merge[Either[ADM, EdgeAdm2Adm]](sources.size))


            for (((host, source), i) <- sources.zipWithIndex) {
              source.via(printCounter(host.hostName, statusActor, 0)) ~> erMap(host.hostName) ~> merge.in(i)
            }

            merge.out ~> betweenHostDedup ~> DBQueryProxyActor.graphActorAdmWriteSink(dbActor, completionMsg)

            ClosedShape
          }).run()

        case ProduceCdmAndAdm =>
          RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
            import GraphDSL.Implicits._

            val sources = cdmSources.values.toSeq
            val mergeCdm = b.add(Merge[(Namespace, CurrentCdm)](sources.size))
            val mergeAdm = b.add(Merge[Either[ADM, EdgeAdm2Adm]](sources.size))


            for (((host, source), i) <- sources.zipWithIndex) {
              val broadcast = b.add(Broadcast[(Namespace, CurrentCdm)](2))
              source.via(printCounter(host.hostName, statusActor, 0)) ~> broadcast.in

              broadcast.out(0)                         ~> mergeCdm.in(i)
              broadcast.out(1) ~> erMap(host.hostName) ~> mergeAdm.in(i)
            }

            mergeCdm.out                     ~> DBQueryProxyActor.graphActorCdm19WriteSink(dbActor, completionMsg)(writeTimeout)
            mergeAdm.out ~> betweenHostDedup ~> DBQueryProxyActor.graphActorAdmWriteSink(dbActor, completionMsg)

            ClosedShape
          }).run()
      }

    case "e3" =>
      println(
        raw"""
Unknown runflow argument e3. Quitting. (Did you mean e4?)

                \
                 \

                        _.-;:q=._
                      .' j=""^k;:\.
                     ; .F       ";`Y
                    ,;.J_        ;'j
                  ,-;"^7F       : .F           _________________
                 ,-'-_<.        ;gj. _.,---""''               .'
                ;  _,._`\.     : `T"5,                       ;
                : `?8w7 `J  ,-'" -^q. `                     ;
                 \;._ _,=' ;   n58L Y.                     .'
                   F;";  .' k_ `^'  j'                     ;
                   J;:: ;     "y:-='                      ;
                    L;;==      |:;   jT\                  ;
                    L;:;J      J:L  7:;'       _         ;
                    I;|:.L     |:k J:.' ,  '       .     ;
                     ;J:.|     ;.I F.:      .           :
                   ;J;:L::     |.| |.J  , '   `    ;    ;
                 .' J:`J.`.    :.J |. L .    ;         ;
                ;    L :k:`._ ,',j J; |  ` ,        ; ;
              .'     I :`=.:."_".'  L J             `.'
            .'       |.:  `"-=-'    |.J              ;
        _.-'         `: :           ;:;           _ ;
    _.-'"             J: :         /.;'       ;    ;
  ='_                  k;.\.    _.;:Y'     ,     .'
     `"---..__          `Y;."-=';:='     ,      .'
              `""--..__   `"==="'    -        .'
                       ``""---...__    itz .-'
                                   ``""---'

"""
      )
      Runtime.getRuntime.halt(1)

    case "e4" =>
      startWebServer()
      statusActor ! InitMsg

      // Write out debug states
      val debug = new StreamDebugger("e4|", 30 seconds, 10 seconds)

      RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
        import GraphDSL.Implicits._

        val hostSources = cdmSources.values.toSeq
        val mergeAdm = b.add(Merge[Either[ADM, EdgeAdm2Adm]](hostSources.size))

        for (((host, source), i) <- hostSources.zipWithIndex) {
          val broadcast = b.add(Broadcast[Either[ADM, EdgeAdm2Adm]](2))
          (source.via(printCounter(host.hostName, statusActor, 0)) via debug.debugBuffer(s"[${host.hostName}] 0 before ER")) ~>
            (erMap(host.hostName) via debug.debugBuffer(s"[${host.hostName}] 1 after ER")) ~>
            broadcast.in

          (broadcast.out(0) via debug.debugBuffer(s"[${host.hostName}] 3 before PPM state accumulator")) ~>
            (PpmFlowComponents.ppmStateAccumulator via debug.debugBuffer(s"[${host.hostName}] 4 before PPM sink")) ~>
            Sink.actorRefWithAck[CompletedESO](ppmManagerActors(host.hostName), InitMsg, Ack, CompleteMsg)

          (broadcast.out(1) via debug.debugBuffer(s"[${host.hostName}] 2 before ADM merge")) ~>
            mergeAdm.in(i)
        }

        val broadcastAdm = b.add(Broadcast[Either[ADM, EdgeAdm2Adm]](2))
        (mergeAdm.out via debug.debugBuffer(s"0 after ADM merge")) ~>
          (betweenHostDedup via debug.debugBuffer(s"~ 1 after cross-host deduplicate")) ~>
          broadcastAdm.in

        (broadcastAdm.out(0) via debug.debugBuffer(s"~ 3 before cross-host PPM state accumulator")) ~>
          (PpmFlowComponents.ppmStateAccumulator via debug.debugBuffer(s"before cross-host PPM sink")) ~>
          Sink.actorRefWithAck[CompletedESO](ppmManagerActors(hostNameForAllHosts), InitMsg, Ack, CompleteMsg)

        (broadcastAdm.out(1) via debug.debugBuffer(s"~ 2 before DB sink")) ~>
          DBQueryProxyActor.graphActorAdmWriteSink(dbActor)

        ClosedShape
      }).run()

//    case "e4-no-db" =>
//      startWebServer()
//      statusActor ! InitMsg
//
//      cdmSource
//      // CDMSource.cdm19(ta1, handleError = { case (off, t) =>println(s"Error at $off: ${t.printStackTrace}") })
//        .via(printCounter("E3 (no DB)", statusActor, startingCount))
//        .via(filterFlow)
//        .via(splitToSink[(String, CDM19)](ppmObservationDistributorSink, 1000))
//        .via(er)
//        .runWith(PpmFlowComponents.ppmSink)

    case "print-cdm" =>
      var i = 0
      RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
        import GraphDSL.Implicits._

        val sources = cdmSources.values.toSeq
        val merge = b.add(Merge[(Namespace,CDM19)](sources.size))
        val sink = b.add(Sink.ignore)

        for (((host, source), i) <- sources.zipWithIndex) {
          source ~> merge.in(i)
        }

        merge.out.via(Flow.fromFunction { cdm =>
          println(s"Record $i: ${cdm.toString}")
          i += 1
        }) ~> sink

        ClosedShape
      }).run()

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

      assert(cdmSources.size == 1, "Cannot produce event matrix for more than once host at a time")
      val (host, cdmSource) = cdmSources.head._2

      assert(host.parallelIngests.size == 1, "Cannot produce event matrix for more than one linear ingest stream")
      val li = host.parallelIngests.head

      cdmSource
        .via(printCounter("CDM", statusActor, li.range.startInclusive))
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

            assert(cdmSources.size == 1, "Cannot produce CSVs for more than once host at a time")
            val (host, cdmSource) = cdmSources.head._2

            assert(host.parallelIngests.size == 1, "Cannot produce CSVs for more than one linear ingest stream")
            val li = host.parallelIngests.head

            cdmSource
              .via(printCounter("DB Writer", statusActor, li.range.startInclusive, 10000))
              .via(erMap(host.hostName))
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

      assert(cdmSources.size == 1, "Cannot get valuebytes for more than once host at a time")
      val (host, cdmSource) = cdmSources.head._2

      assert(host.parallelIngests.size == 1, "Cannot get valuebytes for more than one linear ingest stream")
      val li = host.parallelIngests.head

      cdmSource
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

      assert(cdmSources.size == 1, "Cannot check for unique UUIDs for more than once host at a time")
      val (host, cdmSource) = cdmSources.head._2

      assert(host.parallelIngests.size == 1, "Cannot check for unique UUIDs for more than one linear ingest stream")
      val li = host.parallelIngests.head

      cdmSource
        .via(printCounter("UniqueUUIDs", statusActor, li.range.startInclusive))
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

      assert(cdmSources.size == 1, "Cannot run novelty flow for more than once host at a time")
      val (host, cdmSource) = cdmSources.head._2

      assert(host.parallelIngests.size == 1, "Cannot run novelty flow for more than one linear ingest stream")
      val li = host.parallelIngests.head

      cdmSource
        .via(printCounter("Novelty", statusActor, li.range.startInclusive))
        .via(erMap(host.hostName))
        .runWith(PpmFlowComponents.ppmSink)
      startWebServer()

    case other =>
      println(s"Unknown runflow argument $other. Quitting.")
      Runtime.getRuntime.halt(1)
  }
}


/**
  * Utility for debugging which components are bottlenecks in a backpressured stream system. Instantiate one of these
  * per stream system, then place [[debugBuffer]] between stages to find out whether the bottleneck is upstream (in
  * which case the buffer will end up empty) or downstream (in which case the buffer will end up full).
  *
  * @param prefix prompt with which to start status update lines
  * @param printEvery how frequently to print out status updates
  * @param reportEvery how frequently should the debug buffers report their status to the [[StreamDebugger]]
  */
class StreamDebugger(prefix: String, printEvery: FiniteDuration, reportEvery: FiniteDuration)
                    (implicit system: ActorSystem, ec: ExecutionContext) {

  import java.util.concurrent.ConcurrentHashMap
  import java.util.concurrent.atomic.AtomicInteger
  import java.util.function.BiConsumer

  val bufferCounts: ConcurrentHashMap[String, Long] = new ConcurrentHashMap()

  system.scheduler.schedule(printEvery, printEvery, new Runnable {
    override def run(): Unit = {
      val listBuffer = mutable.ListBuffer.empty[(String, Long)]
      bufferCounts.forEach(new BiConsumer[String, Long] {
        override def accept(key: String, count: Long): Unit = listBuffer += (key -> count)
      })
      println(listBuffer
        .sortBy(_._1)
        .toList
        .map { case (stage, count) => s"$prefix $stage: $count" }
        .mkString(s"$prefix ==== START OF DEBUG REPORT ====\n", "\n", s"\n$prefix ==== END OF DEBUG REPORT ====\n")
      )
    }
  })

  /**
    * Create a new debug buffer flow. This is just like a (backpressured) buffer flow, but it keeps track of how many
    * items are in the buffer (possibly plus one) and reports this number periodically to the object on which this
    * method is called.
    *
    * @param name what label to associate with this debug buffer (should be unique per [[StreamDebugger]]
    * @param bufferSize size of the buffer being created
    * @tparam T type of thing flowing through the buffer
    * @return a buffer flow which periodically reports stats about how full its buffer is
    */
  def debugBuffer[T](name: String, bufferSize: Int = 10000): Flow[T,T,NotUsed] =
    Flow.fromGraph(GraphDSL.create() {
      implicit graph =>

        import GraphDSL.Implicits._

        val bufferCount: AtomicInteger = new AtomicInteger(0)

        // Write the count out to the buffer count map regularly
        system.scheduler.schedule(reportEvery, reportEvery, new Runnable {
          override def run(): Unit = bufferCounts.put(name, bufferCount.get())
        })

        // Increment the count when entering the buffer, decrement it when exiting
        val incrementCount = graph.add(Flow.fromFunction[T,T](x => { bufferCount.incrementAndGet(); x }))
        val buffer = graph.add(Flow[T].buffer(bufferSize, OverflowStrategy.backpressure))
        val decrementCount = graph.add(Flow.fromFunction[T,T](x => { bufferCount.decrementAndGet(); x }))

        incrementCount.out ~> buffer ~> decrementCount

        FlowShape(incrementCount.in, decrementCount.out)
    })
}
