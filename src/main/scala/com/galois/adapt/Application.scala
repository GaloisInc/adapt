package com.galois.adapt

import java.io._
import java.nio.file.Paths
import java.util
import java.util.UUID
import java.util.concurrent.{Executors, TimeUnit}

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteResult._
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.pattern.ask
import akka.stream.{ActorMaterializer, _}
import akka.stream.scaladsl._
import akka.util.{ByteString, Timeout}
import com.galois.adapt.adm._
import com.galois.adapt.cdm17.{CDM17, RawCDM17Type}
import com.galois.adapt.{cdm17 => cdm17types}
import com.galois.adapt.cdm18.{CDM18, RawCDM18Type, Cdm17to18}
import com.galois.adapt.{cdm18 => cdm18types}
import com.typesafe.config.ConfigFactory
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.tinkerpop.gremlin.structure.{Element => VertexOrEdge}
import org.mapdb.{DB, DBMaker, HTreeMap, Serializer}
import org.reactivestreams.Publisher
import FlowComponents._
import akka.NotUsed
import akka.event.{Logging, LoggingAdapter}
import bloomfilter.CanGenerateHashFrom
import bloomfilter.mutable.BloomFilter
import com.galois.adapt.FilterCdm.Filter
import com.galois.adapt.MapSetUtils.{AlmostMap, AlmostSet}
import com.galois.adapt.adm.EntityResolution.Timed
import com.galois.adapt.cdm19._
import org.mapdb.serializer.SerializerArrayTuple

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.Await
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
  val log: LoggingAdapter = Logging.getLogger(system, this)

  // All large maps should be store in `MapProxy`
  val mapProxy: MapProxy = new MapProxy(
    fileDbPath = Try(config.getString("adapt.adm.mapdb")).filter(_ => runFlow != "accept").toOption,
    fileDbBypassChecksum = config.getBoolean("adapt.adm.mapdbbypasschecksum"),
    fileDbTransactions = config.getBoolean("adapt.adm.mapdbtransactions"),

    cdm2cdmLruCacheSize = Try(config.getLong("adapt.adm.cdm2cdmlrucachesize")).getOrElse(10000000L),
    cdm2admLruCacheSize = Try(config.getLong("adapt.adm.cdm2admlrucachesize")).getOrElse(30000000L),
    dedupEdgeCacheSize = config.getInt("adapt.adm.dedupEdgeCacheSize")
  )

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

  val statusActor = system.actorOf(Props[StatusActor], name = "statusActor")
  val logFile = config.getString("adapt.logfile")
  val scheduledLogging = system.scheduler.schedule(10.seconds, 10.seconds, statusActor, LogToDisk(logFile))
  system.registerOnTermination(scheduledLogging.cancel())

  val ppmBaseDirPath = config.getString("adapt.ppm.basedir")
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

  // Get namespaces if there are any
  private val namespaces: mutable.Map[String,Boolean] = mutable.Map.empty

  // Global mutable state for figuring out what namespaces we currently have
  def addNamespace(ns: String, isWindows: Boolean): Unit = Application.namespaces(ns) = isWindows

  // Load up all of the namespaces, and then write them back out on shutdown
  val namespacesFile = new File(config.getString("adapt.runtime.neo4jfile"), "namespaces.txt")
  if (runFlow != "accept") {
    if (namespacesFile.exists && namespacesFile.canRead) {
      import scala.collection.JavaConverters._

      val in = new BufferedReader(new InputStreamReader(new FileInputStream(namespacesFile)))
      for (line <- in.lines().iterator().asScala)
        addNamespace(line, false)
      in.close()
    }
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable() {
      override def run(): Unit = {
        val out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(namespacesFile)))
        for (namespace <- namespaces.toList)
          out.write(namespace + "\n")
        out.close()
      }
    }))
  }

  def getNamespaces: List[String] = List("cdm") ++ namespaces.keySet.toList.flatMap(ns => List("cdm_" + ns, ns))

  // This only works during ingestion - it won't work when we read namespaces out of the DB
  def isWindows(ns: String): Boolean = namespaces.getOrElse(ns, false)

  // These are the maps that `UUIDRemapper` will use
  val cdm2cdmMap: AlmostMap[CdmUUID,CdmUUID] = mapProxy.cdm2cdmMap
  val cdm2admMap: AlmostMap[CdmUUID,AdmUUID] = mapProxy.cdm2admMap

  // Edges blocked waiting for a target CDM uuid to be remapped.
  val blockedEdges: mutable.Map[CdmUUID, (List[Edge], Set[CdmUUID])] = mutable.Map.empty

  val seenEdges: AlmostSet[EdgeAdm2Adm] = mapProxy.seenEdges
  val seenNodes: AlmostSet[AdmUUID] = mapProxy.seenNodes



  val er = EntityResolution(cdm2cdmMap, cdm2admMap, blockedEdges, log, seenNodes, seenEdges)

  val ppmActor: Option[ActorRef] = runFlow match {
    case "accept" => None
    case _ =>
      val ref = system.actorOf(Props(classOf[PpmActor]), "ppm-actor")
      Try(config.getLong("adapt.ppm.saveintervalseconds")) match {
        case Success(i) if i > 0L =>
          println(s"Saving PPM trees every $i seconds")
          val cancellable = system.scheduler.schedule(i.seconds, i.seconds, ref, SaveTrees())
          system.registerOnTermination(cancellable.cancel())
        case _ => println("Not going to periodically save PPM trees.")
      }
      Some(ref)
  }

  // Coarse grain filtering of the input CDM
  var filter: Option[Filterable => Boolean] = None
  var filterAst: Option[Filter] = None
  val filterFlow: Flow[(String,CDM19),(String,CDM19),_] = Flow[(String,CDM19)]
    .map[(String, Either[Filterable,CDM19])] {
      case (s, c: Event) => (s, Left(Filterable.apply(c)))
      case (s, c: FileObject) => (s, Left(Filterable.apply(c)))
      case (s, c: Host) => (s, Left(Filterable.apply(c)))
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


  var ta1 = config.getString("adapt.env.ta1")  // This gets overwritten with a single value pulled from a file--if it begins as anything other than a TA1 name from the config.  This mutability is probably a very bad idea.

  // Mutable state that gets updated during ingestion
  var instrumentationSource: String = "(not detected)"
  var failedStatements: List[(Int, String)] = Nil

  def startWebServer(): Http.ServerBinding = {
    println(s"Starting the web server at: http://$interface:$port")
    val route = Routes.mainRoute(dbActor, statusActor, ppmActor, cdm2admMap, cdm2cdmMap)
    val httpServer = Http().bindAndHandle(route, interface, port)
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
      CDMSource.cdm19(ta1, (position, msg) => failedStatements = (position, msg.getMessage) :: failedStatements)
        .via(printCounter("CDM events", statusActor))
        .recover{ case e: Throwable => e.printStackTrace(); ??? }
        .runWith(sink)


    case "database" | "db" | "ingest" =>
      val ingestCdm = config.getBoolean("adapt.ingest.producecdm")
      val ingestAdm = config.getBoolean("adapt.ingest.produceadm")
      val completionMsg = if (config.getBoolean("adapt.ingest.quitafteringest")) KillJVM else CompleteMsg
      val writeTimeout = Timeout(30.1 seconds)

      val (name, sink) = (ingestCdm, ingestAdm) match {
        case (false, false) => println("\n\nA database ingest flow which ingest neither CDM nor ADM data ingests nothing at all.\n\nExiting, so that you can ponder the emptiness of existence for a while...\n\n"); Runtime.getRuntime.halt(42); throw new RuntimeException("TreeFallsInTheWoodsException")
        case (true, false) => "CDM" -> DBQueryProxyActor.graphActorCdm19WriteSink(dbActor, completionMsg)(writeTimeout)
        case (false, true) => "ADM" -> er.to(DBQueryProxyActor.graphActorAdmWriteSink(dbActor, completionMsg))
        case (true, true) => "CDM+ADM" -> Sink.fromGraph(GraphDSL.create() { implicit b =>
          import GraphDSL.Implicits._
          val broadcast = b.add(Broadcast[(String,CDM19)](2))

          broadcast ~> DBQueryProxyActor.graphActorCdm19WriteSink(dbActor, completionMsg)(writeTimeout)
          broadcast ~> er ~> DBQueryProxyActor.graphActorAdmWriteSink(dbActor, completionMsg)

          SinkShape(broadcast.in)
        })
      }

      println(s"Running database flow for $name with UI.")
      if (config.getBoolean("adapt.ingest.quitafteringest")) println("Will terminate after ingest.")

      startWebServer()
      CDMSource.cdm19(ta1).buffer(10000, OverflowStrategy.backpressure).via(printCounter(name, statusActor)).runWith(sink)

    case "train" =>
      startWebServer()
      statusActor ! InitMsg

      CDMSource.cdm19(ta1)
        .via(printCounter("E3 Training", statusActor))
        .via(splitToSink[(String, CDM19)](Sink.actorRefWithAck(ppmActor.get, InitMsg, Ack, CompleteMsg), 1000))
        .via(er)
        .runWith(PpmComponents.ppmSink)

    case "e3" =>
      startWebServer()
      statusActor ! InitMsg

      CDMSource.cdm19(ta1)
        .via(printCounter("E3", statusActor))
        .via(filterFlow)
        .via(splitToSink[(String, CDM19)](Sink.actorRefWithAck(ppmActor.get, InitMsg, Ack, CompleteMsg), 1000))
        .via(er)
        .via(splitToSink(PpmComponents.ppmSink, 1000))
        .runWith(DBQueryProxyActor.graphActorAdmWriteSink(dbActor))

    case "e3-no-db" =>
      startWebServer()
      statusActor ! InitMsg

      CDMSource.cdm19(ta1)
      // CDMSource.cdm19(ta1, handleError = { case (off, t) =>println(s"Error at $off: ${t.printStackTrace}") })
        .via(printCounter("E3 (no DB)", statusActor))
        .via(filterFlow)
        .via(splitToSink[(String, CDM19)](Sink.actorRefWithAck(ppmActor.get, InitMsg, Ack, CompleteMsg), 1000))
        .via(er)
        .runWith(PpmComponents.ppmSink)

    case "print-cdm" =>
      var i = 0
      CDMSource.cdm19(ta1)
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

      CDMSource.cdm18(ta1)
        .via(printCounter("CDM", statusActor))
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

    case "ignore" =>

      val odir = if(config.hasPath("adapt.outdir")) config.getString("adapt.outdir") else "."
      startWebServer()
      statusActor ! InitMsg

      CDMSource.cdm19(ta1)
        .via(printCounter("File Input", statusActor))
        .via(filterFlow)
        .via(er)
        .runWith(Sink.ignore)

    case "csvmaker" | "csv" =>

      val forCdm = config.getBoolean("adapt.ingest.producecdm")
      val forAdm = config.getBoolean("adapt.ingest.produceadm")

      val odir = if(config.hasPath("adapt.outdir")) config.getString("adapt.outdir") else "."
      startWebServer()
      statusActor ! InitMsg

      if (forCdm) {
        println("Producing CSVs from CDM is no longer supported")
      } else if (!forAdm) {
        println("Generating CSVs for neither CDM not ADM - so... generating nothing!")
      } else {
        RunnableGraph.fromGraph(GraphDSL.create(){ implicit graph =>
          import GraphDSL.Implicits._

          val broadcast = graph.add(Broadcast[Any](9))

          CDMSource.cdm19(ta1)
            .via(printCounter("DB Writer", statusActor, 10000))
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

      CDMSource.cdm18(ta1)
        .collect{ case (_, e: cdm18.Event) if e.parameters.nonEmpty => e}
        .flatMapConcat(
          (e: cdm18.Event) => Source.fromIterator(
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
      CDMSource.cdm18(ta1)
        .via(printCounter("UniqueUUIDs", statusActor))
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


    case "find" =>
      println("Running FIND flow")
      CDMSource.cdm18(ta1).via(printCounter("Find", statusActor))
        .collect{ case (_, cdm: Event) if cdm.uuid == UUID.fromString("8265bd98-c015-52e9-9361-824e2ade7f4c") => cdm.toMap.toString + s"\n$cdm" }
        .runWith(Sink.foreach(println))

    case "fsox" =>
      CDMSource.cdm19(ta1)
        .via(printCounter("Novelty FSOX", statusActor))
        .via(er)
        .via(FSOX.apply)
        .runWith(Sink.foreach(println))

    case "novelty" | "novel" | "ppm" | "ppmonly" =>
      println("Running Novelty Detection Flow")
      statusActor ! InitMsg
      CDMSource.cdm19(ta1)
        .via(printCounter("Novelty", statusActor))
        .via(er)
        .runWith(PpmComponents.ppmSink)
      startWebServer()

    case _ =>
      println("Unknown runflow argument. Quitting.")
      Runtime.getRuntime.halt(1)
  }
}


object CDMSource {
  private val config = ConfigFactory.load()
//  val scenario = config.getString("adapt.env.scenario")

  type Provider = String

  def getLoadfilesOld: List[(Provider, String)] = {
    val data = config.getObject("adapt.ingest.data")

    for {
      provider <- data.keySet().asScala.toList
      providerFixed = if (provider.isEmpty) { "\"\"" } else { provider }

      paths = config.getStringList(s"adapt.ingest.data.$providerFixed").asScala.toList
      pathsPossiblyFromDirectory = if (paths.length == 1 && new File(paths.head).isDirectory) {
        new File(paths.head).listFiles().toList.collect {
          case f if ! f.isHidden => f.getCanonicalPath
        }
      } else paths

      path <- pathsPossiblyFromDirectory.sorted

      // TODO: This is an ugly hack to handle paths like ~/Documents/file.avro
      pathFixed = path.replaceFirst("^~", System.getProperty("user.home"))
    } yield (provider, pathFixed)
  }
  def getLoadfiles: List[(Provider, String)] = {

/*
    val data = config.getObjectList("adapt.ingest.data").asScala.toList.map(_.asScala.toMap)
    val data2 = data.map(_.map{
	case (key, value) if key == "provider" => (key, value.unwrapped.toString)
	case (key, value) if key == "files" => (key, value.asInstanceOf[java.util.List[com.typesafe.config.ConfigValue]].asScala.toList.map(_.unwrapped.toString))
      })
*/

    val dataMapList = config.getObjectList("adapt.ingest.data").asScala.toList
    val data: List[(Provider, List[String])] = dataMapList.map(_.toConfig).map(i=>(i.getString("provider"), i.getStringList("files").asScala.toList))

    for {
      e: (Provider, List[String]) <- data
      provider:String <- e._1
      providerFixed = if (provider == "") { "\"\"" } else { provider }

      paths: List[String] = e._2
      pathsPossiblyFromDirectory = if (paths.length == 1 && new File(paths.head).isDirectory) {
        new File(paths.head).listFiles().toList.collect {
          case f if ! f.isHidden => f.getCanonicalPath
        }
      } else paths

      path <- pathsPossiblyFromDirectory.sorted

      // TODO: This is an ugly hack to handle paths like ~/Documents/file.avro
      pathFixed = path.replaceFirst("^~", System.getProperty("user.home"))
    } yield (provider, pathFixed)
  }

  //  Make a CDM17 source
  def cdm17(ta1: String, handleError: (Int, Throwable) => Unit = (_,_) => { }): Source[(Provider, CDM17), _] = {
    println(s"Setting source for: $ta1")
    val start = Try(config.getLong("adapt.ingest.startatoffset")).getOrElse(0L)
    val shouldLimit = Try(config.getLong("adapt.ingest.loadlimit")) match {
      case Success(0) => None
      case Success(i) => Some(i)
      case _ => None
    }
    ta1.toLowerCase match {
      case "cadets"         =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic"), kafkaCdm17Parser, None)
        Application.instrumentationSource = "cadets"
        Application.addNamespace("cadets", isWindows = false)
        shouldLimit.fold(src)(l => src.take(l)).map("cadets" -> _)
      case "clearscope"     =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic"), kafkaCdm17Parser, None)
        Application.instrumentationSource = "clearscope"
        Application.addNamespace("clearscope", isWindows = false)
        shouldLimit.fold(src)(l => src.take(l)).map("clearscope" -> _)
      case "faros"          =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic"), kafkaCdm17Parser, None)
        Application.instrumentationSource = "faros"
        Application.addNamespace("faros", isWindows = true)
        shouldLimit.fold(src)(l => src.take(l)).map("faros" -> _)
      case "fivedirections" =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic"), kafkaCdm17Parser, None)
        Application.instrumentationSource = "fivedirections"
        Application.addNamespace("fivedirections", isWindows = true)
        shouldLimit.fold(src)(l => src.take(l)).map("fivedirections" -> _)
      case "theia"          =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic"), kafkaCdm17Parser, None)
        Application.instrumentationSource = "theia"
        Application.addNamespace("theia", isWindows = false)
        shouldLimit.fold(src)(l => src.take(l))
          .merge(kafkaSource(config.getString("adapt.env.theiaresponsetopic"), kafkaCdm17Parser, None).via(printCounter("Theia Query Response", Application.statusActor, 1)))
          .map("theia" -> _)
      case "trace"          =>
        val src = kafkaSource(config.getString("adapt.env.ta1kafkatopic"), kafkaCdm17Parser, None)
        Application.instrumentationSource = "trace"
        Application.addNamespace("trace", isWindows = false)
        shouldLimit.fold(src)(l => src.take(l)).map("trace" -> _)
      case "kafkaTest"      =>
        Application.addNamespace("kafkaTest", isWindows = false)
        val src = kafkaSource("kafkaTest", kafkaCdm17Parser, None)  //.throttle(500, 5 seconds, 1000, ThrottleMode.shaping)
        shouldLimit.fold(src)(l => src.take(l)).map("kafkaTest" -> _)
      case _ =>

        val paths: List[(Provider, String)] = getLoadfiles
        println(s"Setting file sources to: ${paths.mkString(", ")}")

        val startStream = paths.foldLeft(Source.empty[Try[(String,CDM17)]])((a,b) => a.concat{
          Source.fromIterator[Try[(String,CDM17)]](() => {
            val read = CDM17.readData(b._2, None)

            read.map(_._1) match {
              case Failure(_) => None
              case Success(s) =>
                Application.instrumentationSource = Ta1Flows.getSourceName(s)
                Application.addNamespace(b._1, Ta1Flows.isWindows(Application.instrumentationSource))
            }

            read.get._2.map(_.map(b._1 -> _))
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

  val start = Try(config.getLong("adapt.ingest.startatoffset")).getOrElse(0L)

  // Make a CDM18 source, possibly falling back on CDM17 for files with that version
  def cdm18(ta1: String, handleError: (Int, Throwable) => Unit = (_,_) => { }): Source[(String,CDM18), _] = {
    println(s"Setting source for CDM 18 TA1: $ta1")
    if (start > 0L) println(s"Throwing away the first $start statements.")
    val shouldLimit = Try(config.getLong("adapt.ingest.loadlimit")) match {
      case Success(0) => None
      case Success(i) => Some(i)
      case _ => None
    }

    val src = config.getStringList("adapt.env.ta1kafkatopics").asScala.map{topicNameAndLimit =>
      val (topicName, limitOpt) = topicNameAndLimit.split("∫").toList match {
        case name :: l :: Nil if Try(l.toInt).isSuccess => (name, Some(l.toInt))
        case name :: Nil => (name, None)
        case _ => throw new IllegalArgumentException(s"Cannot parse kaka topic list with inputs: $topicNameAndLimit")
      }

      val isWindows = ta1.toLowerCase match {
        case "faros" => true
        case "fivedirections" => true
        case "cadets" => false
        case "clearscope" => false
        case "theia" => false
        case "trace" => false
        case _ => false
      }
      Application.addNamespace(topicName, isWindows)

      kafkaSource(topicName, kafkaCdm18Parser, limitOpt).map(topicName -> _)
    }.fold(Source.empty)((earlierTopicSource, laterTopicSouce) => earlierTopicSource.concat(laterTopicSouce))

    Application.instrumentationSource = ta1.toLowerCase

    ta1.toLowerCase match {
      case "cadets"         => shouldLimit.fold(src)(l => src.take(l))
      case "clearscope"     => shouldLimit.fold(src)(l => src.take(l))
      case "faros"          => shouldLimit.fold(src)(l => src.take(l))
      case "fivedirections" => shouldLimit.fold(src)(l => src.take(l))
      case "theia"          =>
        val queryTopic = config.getString("adapt.env.theiaresponsetopic")
        Application.addNamespace(queryTopic, false)

        shouldLimit.fold(src)(l => src.take(l))
          .merge(kafkaSource(queryTopic, kafkaCdm18Parser, None)
            .via(printCounter("Theia Query Response", Application.statusActor, 1))
            .map(queryTopic -> _))

      case "trace"          => shouldLimit.fold(src)(l => src.take(l))
      case "kafkatest"      =>
        Application.addNamespace("kafkatest", isWindows = false)
        val kafkaTestSource = kafkaSource("kafkatest", kafkaCdm18Parser, None)  //.throttle(500, 5 seconds, 1000, ThrottleMode.shaping)
        shouldLimit.fold(kafkaTestSource)(l => kafkaTestSource.take(l)).map(ta1 -> _)
      case _ =>
        val paths: List[(Provider, String)] = getLoadfiles
        println(s"Setting file sources to: ${paths.mkString("\n", "\n", "")}")
        paths.headOption.foreach { p =>
          Application.ta1 = p._1
          println(s"Assuming a single provider from file data: ${p._1}")
        }

        val startStream = paths.foldLeft(Source.empty[Try[(String,CDM18)]])((a,b) => a.concat({

          val read = CDM18.readData(b._2, None)
          read.map(_._1) match {
            case Failure(_) => None
            case Success(s) => {
              Application.instrumentationSource = Ta1Flows.getSourceName(s)
              Application.addNamespace(b._1, Ta1Flows.isWindows(Application.instrumentationSource))
            }
          }

          // Try to read CDM18 data. If we fail, fall back on reading CDM17 data, then convert that to CDM18
          val cdm18: Iterator[Try[(String,CDM18)]] = read.map(_._2).getOrElse({
            println("Failed to read file as CDM18, trying to read it as CDM17...")

            val dummyHost: UUID = new java.util.UUID(0L,1L)

            val read = CDM17.readData(b._2, None)
            read.map(_._1) match {
              case Failure(_) => None
              case Success(s) =>
                Application.instrumentationSource = Ta1Flows.getSourceName(s)
                Application.addNamespace(b._1, Ta1Flows.isWindows(Application.instrumentationSource))
            }

            read.map(_._2).get.flatMap {
              case Failure(e) => List(Failure[CDM18](e))
              case Success(cdm17) => cdm17ascdm18(cdm17, dummyHost).toList.map(Success(_))
            }
          }).map(_.map(b._1 -> _))
          Source.fromIterator[Try[(String,CDM18)]](() => cdm18)
        })).statefulMapConcat[Try[(String,CDM18)]]{ () =>  // This drops CDMs while counting live.
            var counter = 0L
            var stillDiscarding = start > 0L;
            {
              case cdm if stillDiscarding =>
                print(s"\rSkipping past: $counter")
                counter += 1
                stillDiscarding = start > counter
                Nil
              case cdm => List(cdm)
            }
          }
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

  // Try to make a CDM18 record from a CDM17 one
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

  // Make a CDM18 source, possibly falling back on CDM17/CDM18 for files with that version
  def cdm19(ta1: String, handleError: (Int, Throwable) => Unit = (_,_) => { }): Source[(String,CDM19), _] = {
    println(s"Setting source for CDM 19 TA1: $ta1")
    if (start > 0L) println(s"Throwing away the first $start statements.")
    val shouldLimit = Try(config.getLong("adapt.ingest.loadlimit")) match {
      case Success(0) => None
      case Success(i) => Some(i)
      case _ => None
    }

    val src = config.getStringList("adapt.env.ta1kafkatopics").asScala.map{topicNameAndLimit =>
      val (topicName, limitOpt) = topicNameAndLimit.split("∫").toList match {
        case name :: l :: Nil if Try(l.toInt).isSuccess => (name, Some(l.toInt))
        case name :: Nil => (name, None)
        case _ => throw new IllegalArgumentException(s"Cannot parse kaka topic list with inputs: $topicNameAndLimit")
      }

      val isWindows = ta1.toLowerCase match {
        case "faros" => true
        case "fivedirections" => true
        case "cadets" => false
        case "clearscope" => false
        case "theia" => false
        case "trace" => false
        case _ => false
      }
      Application.addNamespace(topicName, isWindows)

      kafkaSource(topicName, kafkaCdm19Parser, limitOpt).map(topicName -> _)
    }.fold(Source.empty)((earlierTopicSource, laterTopicSouce) => earlierTopicSource.concat(laterTopicSouce))

    Application.instrumentationSource = ta1.toLowerCase

    ta1.toLowerCase match {
      case "cadets"         => shouldLimit.fold(src)(l => src.take(l))
      case "clearscope"     => shouldLimit.fold(src)(l => src.take(l))
      case "faros"          => shouldLimit.fold(src)(l => src.take(l))
      case "fivedirections" => shouldLimit.fold(src)(l => src.take(l))
      case "theia"          =>
        val queryTopic = config.getString("adapt.env.theiaresponsetopic")
        Application.addNamespace(queryTopic, false)

        shouldLimit.fold(src)(l => src.take(l))
          .merge(kafkaSource(queryTopic, kafkaCdm19Parser, None)
            .via(printCounter("Theia Query Response", Application.statusActor, 1))
            .map(queryTopic -> _))

      case "trace"          => shouldLimit.fold(src)(l => src.take(l))
      case "kafkatest"      =>
        Application.addNamespace("kafkatest", isWindows = false)
        val kafkaTestSource = kafkaSource("kafkatest", kafkaCdm19Parser, None)  //.throttle(500, 5 seconds, 1000, ThrottleMode.shaping)
        shouldLimit.fold(kafkaTestSource)(l => kafkaTestSource.take(l)).map(ta1 -> _)
      case _ =>
        val paths: List[(Provider, String)] = getLoadfiles
        println(s"Setting file sources to: ${paths.mkString("\n", "\n", "")}")
        paths.headOption.foreach { p =>
          Application.ta1 = p._1
          println(s"Assuming a single provider from file data: ${p._1}")
        }

        val startStream = paths.foldLeft(Source.empty[Try[(String,CDM19)]])((a,b) => a.concat({

          val read = CDM19.readData(b._2, None)
          read.map(_._1) match {
            case Failure(_) => None
            case Success(s) => {
              Application.instrumentationSource = Ta1Flows.getSourceName(s)
              Application.addNamespace(b._1, Ta1Flows.isWindows(Application.instrumentationSource))
            }
          }

          // Try to read CDM19 data. If we fail, fall back on reading CDM18 data, then convert that to CDM19
          val cdm19: Iterator[Try[(String,CDM19)]] = read.map(_._2).getOrElse({
            println("Failed to read file as CDM19, trying to read it as CDM18...")

            val dummyHost: UUID = new java.util.UUID(0L,1L)

            val read = CDM18.readData(b._2, None)
            read.map(_._1) match {
              case Failure(_) => None
              case Success(s) =>
                Application.instrumentationSource = Ta1Flows.getSourceName(s)
                Application.addNamespace(b._1, Ta1Flows.isWindows(Application.instrumentationSource))
            }

            read.map(_._2).getOrElse({
              println("Failed to read file as CDM18, trying to read it as CDM17...")

              val dummyHost: UUID = new java.util.UUID(0L,1L)

              val read = CDM17.readData(b._2, None)
              read.map(_._1) match {
                case Failure(_) => None
                case Success(s) =>
                  Application.instrumentationSource = Ta1Flows.getSourceName(s)
                  Application.addNamespace(b._1, Ta1Flows.isWindows(Application.instrumentationSource))
              }

              read.map(_._2).get.flatMap {
                case Failure(e) => List(Failure[CDM18](e))
                case Success(cdm17) => cdm17ascdm18(cdm17, dummyHost).toList.map(Success(_))
              }
            }).flatMap {
              case Failure(e) => List(Failure[CDM19](e))
              case Success(cdm18) => cdm18ascdm19(cdm18, dummyHost).toList.map(Success(_))
            }
          }).map(_.map(b._1 -> _))

            /*.getOrElse({
            println("Failed to read file as CDM19, trying to read it as CDM18...")

            val dummyHost: UUID = new java.util.UUID(0L,1L)

            val read = CDM18.readData(b._2, None)
            read.map(_._1) match {
              case Failure(_) => None
              case Success(s) =>
                Application.instrumentationSource = Ta1Flows.getSourceName(s)
                Application.addNamespace(b._1, Ta1Flows.isWindows(Application.instrumentationSource))
            }

            read.map(_._2).get.flatMap {
              case Failure(e) => List(Failure[CDM19](e))
              case Success(cdm18) => cdm18ascdm19(cdm18, dummyHost).toList.map(Success(_))
            }
          })*/
          Source.fromIterator[Try[(String,CDM19)]](() => cdm19)
        })).statefulMapConcat[Try[(String,CDM19)]]{ () =>  // This drops CDMs while counting live.
          var counter = 0L
          var stillDiscarding = start > 0L;
        {
          case cdm if stillDiscarding =>
            print(s"\rSkipping past: $counter")
            counter += 1
            stillDiscarding = start > counter
            Nil
          case cdm => List(cdm)
        }
        }
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

  // Try to make a CDM19 record from a CDM18 one
  def cdm18ascdm19(c: CDM18, dummyHost: UUID): Option[CDM19] = {
    implicit val _: UUID = dummyHost
    c match {
      case e: cdm18types.Event => Some(Cdm18to19.event(e))
      case f: cdm18types.FileObject => Some(Cdm18to19.fileObject(f))
      case m: cdm18types.MemoryObject => Some(Cdm18to19.memoryObject(m))
      case n: cdm18types.NetFlowObject => Some(Cdm18to19.netFlowObject(n))
      case p: cdm18types.Principal => Some(Cdm18to19.principal(p))
      case p: cdm18types.ProvenanceTagNode => Some(Cdm18to19.provenanceTagNode(p))
      case r: cdm18types.RegistryKeyObject => Some(Cdm18to19.registryKeyObject(r))
      case s: cdm18types.SrcSinkObject => Some(Cdm18to19.srcSinkObject(s))
      case s: cdm18types.Subject => Some(Cdm18to19.subject(s))
      case t: cdm18types.TimeMarker => Some(Cdm18to19.timeMarker(t))
      case u: cdm18types.UnitDependency => Some(Cdm18to19.unitDependency(u))
      case u: cdm18types.UnnamedPipeObject => Some(Cdm18to19.ipcObject(u))
      case other =>
        println(s"couldn't find a way to convert $other")
        None
    }
  }

  // Make a CDM source from a kafka topic
  def kafkaSource[C](ta1Topic: String, parser: ConsumerRecord[Array[Byte], Array[Byte]] => Try[C], takeLimit: Option[Int]): Source[C, Consumer.Control] = {
    val kafkaConsumer = Consumer.plainSource(
      ConsumerSettings(config.getConfig("akka.kafka.consumer"), new ByteArrayDeserializer, new ByteArrayDeserializer),
      Subscriptions.assignmentWithOffset(new TopicPartition(ta1Topic, 0), offset = 0) // Try(config.getLong("adapt.ingest.startatoffset")).getOrElse(0L))  // TODO: Why aren't offsets working?
    )
      .statefulMapConcat[ConsumerRecord[Array[Byte], Array[Byte]]] { () =>  // This drops CDMs while counting live.
      var counter = 0L
      var stillDiscarding = start > 0L;
    {
      case cdm if stillDiscarding =>
        if (counter % 10000 == 0) print(s"\rSkipping past: $counter")
        counter += 1
        stillDiscarding = start > counter
        Nil
      case cdm => List(cdm)
    }
    }
    takeLimit.fold(kafkaConsumer)(limit => kafkaConsumer.take(limit))
//      .drop(start)
      .map(parser)
      .mapConcat(c => if (c.isSuccess) List(c.get) else List.empty)
  }

  val reader17 = new SpecificDatumReader(classOf[com.bbn.tc.schema.avro.cdm17.TCCDMDatum])
  val reader18 = new SpecificDatumReader(classOf[com.bbn.tc.schema.avro.cdm18.TCCDMDatum])
  val reader19 = new SpecificDatumReader(classOf[com.bbn.tc.schema.avro.cdm19.TCCDMDatum])

  // Parse a `CDM17` from a kafka record
  def kafkaCdm17Parser(msg: ConsumerRecord[Array[Byte], Array[Byte]]): Try[CDM17] = Try {
    val bais = new ByteArrayInputStream(msg.value())  // msg.record.value()
    val offset = msg.offset()   // msg.record.offset()
    val decoder = DecoderFactory.get.binaryDecoder(bais, null)
    val t = Try {
      val elem: com.bbn.tc.schema.avro.cdm17.TCCDMDatum = reader17.read(null, decoder)
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
    val decoder = DecoderFactory.get.binaryDecoder(bais, null)
    val t = Try {
      val elem: com.bbn.tc.schema.avro.cdm18.TCCDMDatum = reader18.read(null, decoder)
      elem
    }
    if (t.isFailure) println(s"Couldn't read binary data at offset: $offset")
    val cdm = new RawCDM18Type(t.get.getDatum)  // throw the error inside the parent Try
    CDM18.parse(cdm)
  }.flatten

  // Parse a `CDM18` from a kafka record
  def kafkaCdm19Parser(msg: ConsumerRecord[Array[Byte], Array[Byte]]): Try[CDM19] = Try {
    val bais = new ByteArrayInputStream(msg.value())  // msg.record.value()
    val offset = msg.offset()   // msg.record.offset()
    val decoder = DecoderFactory.get.binaryDecoder(bais, null)
    val t = Try {
      val elem: com.bbn.tc.schema.avro.cdm19.TCCDMDatum = reader19.read(null, decoder)
      elem
    }
    if (t.isFailure) println(s"Couldn't read binary data at offset: $offset")
    val cdm = new RawCDM19Type(t.get.getDatum, Some(t.get.getHostId))  // throw the error inside the parent Try
    CDM19.parse(cdm)
  }.flatten
}


object Ta1Flows {
  // Get the name of the instrumentation source
  def getSourceName(a: AnyRef): String = a.toString.split("_").last.toLowerCase

  // Get whether a source is windows of not
  def isWindows(s: String): Boolean = s match {
    case "fivedirections" => true
    case "faros" => true
    case "marple" => true
    case _ => false
  }
}
