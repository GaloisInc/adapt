package com.galois.adapt

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{MemberEvent, MemberUp}

import scala.concurrent.Await
import scala.concurrent.duration._


object ClusterDevApp {
  println(s"Spinning up a development cluster.")

  val config = Application.config
//  val interface = config.getString("akka.http.server.interface")
//  val port = config.getInt("akka.http.server.port")

  implicit val system = ActorSystem(config.getString("adapt.systemname"))
  var nodeManager: Option[ActorRef] = None

  def run(): Unit = {
    nodeManager = Some(
      system.actorOf(Props[ClusterNodeManager])
    )
  }
}


class ClusterNodeManager extends Actor with ActorLogging {
  val cluster = Cluster(context.system)

  override def preStart() = cluster.subscribe(self, classOf[MemberEvent])
  override def postStop() = cluster.unsubscribe(self)

  var childActors: Map[String, Set[ActorRef]] = Map.empty

  def createChild(roleName: String): Unit = roleName match {
    case "db"     => childActors = childActors + (roleName ->
      (childActors.getOrElse(roleName, Set.empty[ActorRef]) + context.actorOf(Props(classOf[DevDBActor], None), "db-actor")) )
    case "ingest" => childActors = childActors + (roleName ->
      (childActors.getOrElse(roleName, Set.empty[ActorRef]) + context.actorOf(Props[FileIngestActor], "file-ingest-actor")) )
  }

  def receive = {
    case MemberUp(m) =>
      if (cluster.selfUniqueAddress == m.uniqueAddress) {
        log.info("Message: {} will result in creating child nodes for: {}", m, m.roles)
        m.roles foreach createChild
      } else if (m.hasRole("ingest") /*&& childActors.keySet.contains("db")*/) {
        val selection = Await.result(context.actorSelection("*/file-ingest-actor").resolveOne( 5 seconds ), 5 seconds)
        log.info("Message: {} will result subscribing to: {}", m, selection)
        selection ! Subscription
      }
    case x => log.warning("received message: {}", x)
  }
}