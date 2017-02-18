package com.galois.adapt

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{MemberEvent, MemberJoined, MemberUp}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import com.typesafe.config.Config
import scala.collection.JavaConverters._
import ServiceRegistryProtocol._


object ClusterDevApp {
  println(s"Spinning up a development cluster.")

  val config = Application.config

  implicit val system = ActorSystem(config.getString("adapt.systemname"))
  var nodeManager: Option[ActorRef] = None
  var registryProxy: Option[ActorRef] = None

  def run(): Unit = {
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
            "db-actor"
          )
        ))
      )

    case "ingest" =>
      childActors = childActors + (roleName ->
        childActors.getOrElse(roleName, Set(
          context.actorOf(
            Props(classOf[FileIngestActor], registryProxy),
            "ingest-actor"
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
            ), "ui-actor")
        ))
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