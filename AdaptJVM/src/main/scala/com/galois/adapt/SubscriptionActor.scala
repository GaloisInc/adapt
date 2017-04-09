package com.galois.adapt

import akka.actor._

// An actor that produces output of type `U`
trait SubscriptionActor[U] extends BaseActorBehavior { s: Actor with ActorLogging =>

  // These are going to be the sources of messages
  def subscriptions: Set[Subscription]
  
  // This is a list that will grow to be the places to send
  var subscribers: Set[Subscription] = Set.empty

  // On initialization, send subscription request to upstream producers
  def initialize(): Unit = subscriptions.foreach { s => s.target ! s.copy(target = self) }


  
  // Accordingly, an AdActor must be prepared to recieve subscription requests too...
  override def receive: PartialFunction[Any,Unit] = ({
    case s: Subscription => subscribers += s
  }: PartialFunction[Any,Unit]) orElse process orElse super.receive

  // What to do with a message. This is where calls to `broadCast` will be made
  def process: PartialFunction[Any, Unit]

  // AdActors usually don't send messages to articular actors - they broadcast to their subscribers
  def broadCast(msg: U): Unit = for (
    Subscription(target, interested) <- subscribers; if interested(msg)
  ) target ! msg

  def broadCastUnsafe(msg: Any): Unit = for (
    Subscription(target, _) <- subscribers
  ) target ! msg
}

// A subscription aggregates all the information needed on the _sender_ side to pass a message along
// to a receiver.
case class Subscription(
  target: ActorRef,       // Subscribing actor
  interested: Any => Boolean // Determines whether the actor is interested in a particular message
)


trait ReportsStatus extends BaseActorBehavior { myself: Actor with ActorLogging with ServiceClient with SubscriptionActor[_] =>

  def statusReport: Map[String,Any]

  override def receive: PartialFunction[Any,Unit] = ({
    case StatusRequest(id, total) =>
//      log.warning(s"Got StatusRequest($id)")
      sender() ! StatusReport(id, total, context.self, dependencyMap.toMap, subscribers.map(_.target), statusReport)
  }: PartialFunction[Any,Unit]) orElse super.receive
}


case class StatusRequest(id: Int, total: Int)
case class StatusReport(id: Int, total: Int, from: ActorRef, dependencies: Map[String,Option[ActorRef]], subscribers: Set[ActorRef], measurements: Map[String,Any]) {
  def stringify: StatusReportString = StatusReportString(id, total, from.toString, dependencies.mapValues(_.map(_.toString)), subscribers.map(_.toString), measurements.mapValues(_.toString))
}

case class StatusReportString(id: Int, total: Int, from: String, dependencies: Map[String,Option[String]], subscribers: Set[String], measurements: Map[String,String])