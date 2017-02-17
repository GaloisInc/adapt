package com.galois.adapt

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.galois.adapt.ServiceRegistryProtocol._
import scala.collection.mutable.{Map => MutableMap}


trait BaseActorBehavior { self: Actor with ActorLogging =>
  def receive: PartialFunction[Any,Unit] = {
    case m => log.error("Received unhandled message: {}\nFrom sender: {}", m, sender)
  }
}


trait ServiceClient extends BaseActorBehavior { self: Actor with ActorLogging =>
  lazy val thisName = this.getClass.getSimpleName
  val registry: ActorRef
  def localReceive: PartialFunction[Any,Unit]

  val dependencies: List[String]

  def beginService(): Unit
  def endService(): Unit

  override def receive = handleServiceManagerMessages orElse localReceive orElse super.receive

  lazy val dependencyMap: MutableMap[String, Option[ActorRef]] = MutableMap(dependencies.map(_ -> None):_*)

  def startIfReady() = {
    val depsSatisfied = dependencyMap forall (_._2.isDefined)
    if (depsSatisfied) {
      beginService()
      registry ! PublishService(thisName, context.self)
    } else {
      log.info(s"$thisName is waiting to satisfy more dependencies before publing service availability: $dependencyMap")
    }
  }

  def handleServiceManagerMessages: PartialFunction[Any,Unit] = {

    case ServiceAvailable(serviceName, actorRef) =>
      log.info(s"$serviceName has announced its availability to $thisName")
      if (dependencyMap.keySet contains serviceName) {
        dependencyMap(serviceName) foreach ( s =>
          log.warning(s"Dependency is already available at actor: $s Replacing with: $actorRef")
        )
        dependencyMap += (serviceName -> Some(actorRef))
        startIfReady()
      } else {
        log.warning(s"Unplanned `Dependency Available` notice: $serviceName at: $actorRef")
      }

    case ServiceUnAvailable(serviceName) =>
      log.info(s"$serviceName has announced its UNavailability to $thisName")
      if (dependencyMap.keySet contains serviceName) {
        dependencyMap += (serviceName -> None)
//        registry ! UnSubscribeToService()
        registry ! UnPublishService(thisName)
        endService()
      } else {
        log.warning(s"Unplanned `Dependency Unavailable` notice: $serviceName")
      }

    case DoSubscriptions =>
      log.info(s"Announcing dependencies to Service Manager: ${dependencies.mkString(",")}")
      dependencies.foreach(d => registry ! SubscribeToService(d))
      startIfReady()
  }

  context.self ! DoSubscriptions
}

case object DoSubscriptions