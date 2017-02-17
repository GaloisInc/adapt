package com.galois.adapt

import akka.actor.{ActorSystem, Props, ActorRef}

// Represents something that will subscribe to messages of any type
class Outgestor(outputs: Set[ActorRef]) extends SubscriptionActor[Any,Any] { 
  val subscriptions: Set[Subscription[Any]] = outputs.map { 
    sub => Subscription[Any](target = sub, pack = { case x: Any => Some(x) })
  }

  initialize()
 
  override def receive = { case t => process(t) }

  def process(v: Any): Unit = {
    println("Outgestor: " + v)
  }
}

object Outgestor {
  def props(subscriptions: Set[ActorRef]): Props = Props(new Outgestor(subscriptions))
}
