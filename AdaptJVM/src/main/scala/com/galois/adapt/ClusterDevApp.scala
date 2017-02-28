package com.galois.adapt

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{MemberEvent, MemberJoined, MemberUp}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import com.typesafe.config.Config
import scala.collection.JavaConverters._
import ServiceRegistryProtocol._

import com.galois.adapt.feature._
import com.galois.adapt.cdm13.{CDM13, EpochMarker, Subject}


object ClusterDevApp {
  println(s"Spinning up a development cluster.")

  var nodeManager: Option[ActorRef] = None
  var registryProxy: Option[ActorRef] = None

  def run(config: Config): Unit = {
    implicit val system = ActorSystem(config.getString("adapt.systemname"))
   
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

  override def preStart() = cluster.subscribe(self, classOf[MemberEvent])
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
            Props(classOf[FileIngestActor], registryProxy),
            "FileIngestActor"
          )
        ))
      )
      val limitOpt = if (config.getInt("adapt.loadlimit") > 0) Some(config.getInt("adapt.loadlimit")) else None
      config.getStringList("adapt.loadfiles").asScala foreach (f =>
        childActors(roleName) foreach ( ingestActor =>
          ingestActor ! IngestFile(f, limitOpt)
        )
      )

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
            Props(classOf[Outgestor], registryProxy, Set()),
            "Outgestor"
          )
        ))
      )

  case "features" =>
      
      val erActor = context.actorOf(Props(classOf[ErActor]), "er-actor")

      // The two feature extractors subscribe to the CDM13 produced by the ER actor
      val featureExtractor1 = context.actorOf(FileEventsFeature.props(registryProxy, erActor), "file-events-actor")
      val featureExtractor2 = context.actorOf(NetflowFeature.props(registryProxy, erActor), "netflow-actor")
      
      // The IForest anomaly detector is going to subscribe to the output of the two feature extractors
      val ad = context.actorOf(IForestAnomalyDetector.props(registryProxy, Set(
        Subscription(featureExtractor1, (m: Any) =>
          Some(m.asInstanceOf[Map[Subject,(Int,Int,Int)]].mapValues(t => Seq(t._1.toDouble, t._2.toDouble, t._3.toDouble)))),
        Subscription(featureExtractor2, (m: Any) =>
          Some(m.asInstanceOf[Map[Subject,Seq[Double]]]))
      )), "anomaly-detector")
      
      childActors = childActors + (roleName ->
        childActors.getOrElse(roleName, Set(erActor, featureExtractor1, featureExtractor2, ad))
      )
  }

  def receive = {
    case MemberUp(m) if cluster.selfUniqueAddress == m.uniqueAddress =>
      if (cluster.selfUniqueAddress == m.uniqueAddress) {
        log.info("Message: {} will result in creating child nodes for: {}", m, m.roles)
        m.roles foreach createChild
      }

    case _: MemberUp => ()

    case _: MemberJoined => ()

    case x => log.warning("received unhandled message: {}", x)
  }
}
