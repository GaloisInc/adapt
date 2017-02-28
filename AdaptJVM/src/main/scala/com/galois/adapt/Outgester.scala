package com.galois.adapt

import akka.actor._

// Represents something that will subscribe to messages of any type
class Outgestor(val registry: ActorRef, val outputs: Set[ActorRef])
  extends Actor with ActorLogging with ServiceClient with SubscriptionActor[Any,Any] { 

  val subscriptions: Set[Subscription[Any]] = outputs.map { 
    sub => Subscription[Any](target = sub, pack = { case x: Any => Some(x) })
  }

  val dependencies = List.empty
  def beginService() = ()  // TODO
  def endService() = ()  // TODO

  initialize()
  
  def process(v: Any): Unit = {
    println("Outgestor: " + v)
  }
}

//object Outgestor {
//  def props(subscriptions: Set[ActorRef]): Props = Props(new Outgestor(subscriptions))
//}
