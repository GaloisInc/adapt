package com.galois.adapt

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberEvent, MemberJoined, MemberUp}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import com.typesafe.config.Config

import scala.collection.JavaConverters._
import ServiceRegistryProtocol._

import scala.concurrent.duration._
import com.galois.adapt.feature._
import com.galois.adapt.scepter._
import com.galois.adapt.cdm17.{CDM17, EVENT_READ, EVENT_WRITE, EpochMarker, Event, EventType, FILE_OBJECT_FILE, FileObject, SUBJECT_PROCESS, Subject, TimeMarker}
import java.io.File

import akka.NotUsed
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source}
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer}
import akka.kafka._

import scala.collection.JavaConversions._
import scala.util.Try

object ClusterDevApp {
  println(s"Spinning up a development cluster.")

  var nodeManager: Option[ActorRef] = None
  var registryProxy: Option[ActorRef] = None

  def run(config: Config): Unit = {
    implicit val system = ActorSystem(config.getString("adapt.systemname"), config)

    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = Props(classOf[ServiceRegistry]),
        terminationMessage = Terminated,
        settings = ClusterSingletonManagerSettings(system)),
      name = "registry")

    registryProxy = Some(system.actorOf(
      ClusterSingletonProxy.props(
          singletonManagerPath = "/user/registry",
        settings = ClusterSingletonProxySettings(system)),
      name = "registryProxy"))

    nodeManager = Some(
      system.actorOf(Props(classOf[ClusterNodeManager], config, registryProxy.get), "mgr")
    )
  }
}



class ClusterNodeManager(config: Config, val registryProxy: ActorRef) extends Actor with ActorLogging {
  val cluster = Cluster(context.system)

  log.info("ClusterNodeManager started")
  log.info(s"Cluster has roles ${cluster.selfRoles}")

  override def preStart() = cluster.subscribe(self, InitialStateAsEvents, classOf[MemberEvent])
  override def postStop() = cluster.unsubscribe(self)

  var childActors: Map[String, Set[ActorRef]] = Map.empty

  def createChild(roleName: String): Unit = roleName match {
    case "db" =>
      childActors = childActors + (roleName ->
        childActors.getOrElse(roleName, Set(
          context.actorOf(
            Props(classOf[DevDBActor], registryProxy, None),
            "DevDBActor"
          )
        ))
      )

    case "ingest" =>
      childActors = childActors + (roleName ->
        childActors.getOrElse(roleName, Set(
          context.actorOf(
            Props(classOf[FileIngestActor], registryProxy, config.getInt("adapt.ingest.minsubscribers")),
            "FileIngestActor"
          )
        ))
      )
    
      /* Get all of the files on the load paths. Each load path should either
       *  - be itself a data file
       *  - be a directory full of data files
       */
      val loadPaths: List[String] = config.getStringList("adapt.loadfiles").asScala.toList
      val filePaths: List[String] = loadPaths.map(new File(_)).flatMap(path =>
        if (path.isDirectory)
          path.listFiles.toList.map(_.getPath)
        else
          List(path.getPath)
      )

      // Non-positive limit indicates no limit
      val limitOpt: Option[Int] = if (config.getInt("adapt.loadlimit") > 0)
          Some(config.getInt("adapt.loadlimit"))
        else
          None
      
      filePaths foreach { file =>
        childActors(roleName) foreach { _ ! IngestFile(file, limitOpt) }
      }

    case "ui" =>
      childActors = childActors + (roleName ->
        childActors.getOrElse(roleName, Set(
          context.actorOf(
            Props(
              classOf[UIActor],
              registryProxy,
              config.getString("akka.http.server.interface"),
              config.getInt("akka.http.server.port")
            ), "UIActor")
        ))
      )

   case "outgestor" =>
      childActors = childActors + (roleName ->
        childActors.getOrElse(roleName, Set(
          context.actorOf(
            Props(classOf[Outgester], registryProxy),
            "Outgestor"
          )
        ))
      )

  case "accept" =>
    val accept = context.actorOf(Props(classOf[AcceptanceTestsActor], registryProxy), "AcceptanceTestsActor")
    childActors = childActors + (roleName -> childActors.getOrElse(roleName, Set(accept)))   // TODO: These sets are not used correctly
  
//  case "devfeatures" =>
//    val fileWrites = context.actorOf(Props(classOf[FileWrites], registryProxy), "FileWrites")
//    val processWrites = context.actorOf(Props(classOf[ProcessEvents], registryProxy), "ProcessWrites")
//    val processWritesFile = context.actorOf(Props(classOf[ProcessWritesFile], registryProxy), "ProcessWritesFile")
//    childActors = childActors + (roleName -> childActors.getOrElse(roleName, Set(fileWrites, processWrites, processWritesFile)))   // TODO: These sets are not used correctly

    case "kafkaProducer" =>
      val file = config.getStringList("adapt.loadfiles").head
      val producerSettings = ProducerSettings(config.getConfig("akka.kafka.producer"), new ByteArraySerializer, new ByteArraySerializer)
      val streamActor = context.actorOf(Props(classOf[GraphRunner], KafkaStreams.kafkaProducer(file, producerSettings, config.getString("adapt.kafka-cdm-source"))))
      childActors = childActors + (roleName -> Set(streamActor))

    case "kafkaIngest" =>
      val consumerSettings: ConsumerSettings[Array[Byte], Array[Byte]] = ConsumerSettings(config.getConfig("akka.kafka.consumer"), new ByteArrayDeserializer, new ByteArrayDeserializer)
      val streamActor = context.actorOf(Props(classOf[GraphRunner], KafkaStreams.kafkaIngest(consumerSettings, config.getString("adapt.kafka-cdm-source"))))
      childActors = childActors + (roleName -> Set(streamActor))

    case s => throw new IllegalArgumentException(s"Unknown role: $s")
  }

  implicit val ec = context.dispatcher
  var statusCounter = 0
  var statusResults: Map[Int, (Int, List[StatusReport])] = Map.empty
  val cancellableStatusHeartbeat = context.system.scheduler.schedule(5 seconds, 5 seconds){
    val totalSent = childActors.map(_._2.size).sum
    childActors.foreach(_._2.foreach { ref =>
      ref ! StatusRequest(statusCounter, totalSent)
    })
    statusCounter = statusCounter + 1
  }

  def receive = {
    case MemberUp(m) if cluster.selfUniqueAddress == m.uniqueAddress =>
      log.info("Message: {} will result in creating child nodes for: {}", m, m.roles)
      m.roles foreach createChild

    case msg @ StatusReport(id, total, fromActor, dependencies, subscribers, measurements) =>
      val results = statusResults.getOrElse(id, total -> List.empty[StatusReport])
      val newList = msg :: results._2
      statusResults = statusResults + (id -> (results._1 -> newList))
      if (newList.length == results._1) {
        childActors.get("ui").foreach(_.foreach(_ ! UIStatusReport(newList)))
        statusResults = statusResults - id
      }

    case x => log.warning("received unhandled message: {}", x)
  }






//  import GraphDSL.Implicits._
//  val pwfRG = RunnableGraph.fromGraph(GraphDSL.create(){ implicit graph =>
//    val bcast = graph.add(Broadcast[CDM17](3))
//    val eventBroadcast = graph.add(Broadcast[Event](2))
//
//    val (ta1, cdmData) = CDM17.readData("/Users/ryan/Desktop/ta1-cadets-cdm17-3.bin", None).get
//    val cdmSource: Source[Try[CDM17], NotUsed] = Source.fromIterator(() => cdmData)
//
//    val rankedDataSink = Sink.actorRef(context.actorOf(Props(classOf[RankedDataActor])), TimeMarker(System.nanoTime))
//
//    val processFilter = Flow[CDM17].filter(s => s.isInstanceOf[Subject] && s.asInstanceOf[Subject].subjectType == SUBJECT_PROCESS).map(_.asInstanceOf[Subject])
//    def eventFilter(eType: EventType) = Flow[CDM17].filter(e => e.isInstanceOf[Event] && e.asInstanceOf[Event].eventType == eType).map(_.asInstanceOf[Event])
//
//    val fileFilter = Flow[CDM17].filter(f => f.isInstanceOf[FileObject] && f.asInstanceOf[FileObject].fileObjectType == FILE_OBJECT_FILE).map(_.asInstanceOf[FileObject])
//
//
//    val processEvents = graph.add(ProcessEvents())
//    val fileEvents = graph.add(FileWrites())
//    val processEventsFile = graph.add(ProcessWritesFile())
//
//    cdmSource.map(_.get) ~> bcast.in
//    eventBroadcast.out(0) ~> processEvents.in1
//    bcast.out(0) ~> processFilter ~> processEvents.in0
//    bcast.out(2) ~> eventFilter(EVENT_WRITE) ~> eventBroadcast.in
//    processEvents.out ~> processEventsFile.in0
//    bcast.out(1) ~> fileFilter ~> fileEvents.in0
//    eventBroadcast.out(1) ~> fileEvents.in1
//    fileEvents.out ~>    processEventsFile.in1
//    processEventsFile.out ~> rankedDataSink
//    ClosedShape
//  })


}

case class UIStatusReport(reports: List[StatusReport])


class GraphRunner(graph: RunnableGraph[_]) extends Actor {
  implicit val ec = context.dispatcher
  implicit val materializer = ActorMaterializer()(context)

  graph.run()

  def receive = {
    case _ => ()
  }

}
