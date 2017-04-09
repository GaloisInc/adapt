package com.galois.adapt

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{MemberEvent, MemberJoined, MemberUp, InitialStateAsEvents}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import com.typesafe.config.Config
import scala.collection.JavaConverters._
import ServiceRegistryProtocol._
import scala.concurrent.duration._
import com.galois.adapt.feature._
import com.galois.adapt.scepter._
import com.galois.adapt.cdm17.{CDM17, EpochMarker, Subject}

import java.io.File
import scala.collection.JavaConversions._

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

    Thread.sleep(5000)

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
  
  case "devfeatures" =>
    val fileWrites = context.actorOf(Props(classOf[FileWrites], registryProxy), "FileWrites")
    val processWrites = context.actorOf(Props(classOf[ProcessWrites], registryProxy), "ProcessWrites")
    val processWritesFile = context.actorOf(Props(classOf[ProcessWritesFile], registryProxy), "ProcessWritesFile")
    childActors = childActors + (roleName -> childActors.getOrElse(roleName, Set(fileWrites, processWrites, processWritesFile)))   // TODO: These sets are not used correctly

  case "rankedui" =>
    val rankedDataActor = context.actorOf(Props(classOf[RankedDataActor], registryProxy), "RankedDataActor")
    childActors = childActors + (roleName -> childActors.getOrElse(roleName, Set(rankedDataActor)))   // TODO: These sets are not used correctly

  case "features" =>
    val erActor = context.actorOf(Props(classOf[ErActor], registryProxy), "er-actor")

    // The two feature extractors subscribe to the CDM produced by the ER actor
    val featureExtractor1 = context.actorOf(FileEventsFeature.props(registryProxy, erActor), "file-events-actor")
    val featureExtractor2 = context.actorOf(NetflowFeature.props(registryProxy, erActor), "netflow-actor")

    // The IForest anomaly detector is going to subscribe to the output of the two feature extractors
    val ad = context.actorOf(IForestAnomalyDetector.props(registryProxy, Set(
      Subscription(featureExtractor1, { _ => true }),
      Subscription(featureExtractor2, { _ => true })
    )), "IForestAnomalyDetector")

    childActors = childActors + (roleName ->
      childActors.getOrElse(roleName, Set(erActor, featureExtractor1, featureExtractor2, ad))
    )
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
}

case class UIStatusReport(reports: List[StatusReport])