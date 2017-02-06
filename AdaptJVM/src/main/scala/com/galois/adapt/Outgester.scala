package com.galois.adapt

import akka.actor.{ActorSystem, Props, ActorRef}

// Represents something that will subscribe to messages of any type
class Outgestor(subscriptions: Set[ActorRef]) extends AdActor[Any,Any](
  subscriptions.map { sub => Subscription[Any,Any](target = sub, pack = { case x: Any => Some(x) }) } 
) {
  
  override def receive = { case t => process(t) }

  def process(v: Any): Unit = {
    println("Outgestor received a message")
    println(v)
  }
}

object Outgestor {
  def props(subscriptions: Set[ActorRef]): Props = Props(new Outgestor(subscriptions))
}
