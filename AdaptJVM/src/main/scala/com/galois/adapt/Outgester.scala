package com.galois.adapt

import akka.actor._

// Represents something that will subscribe to messages of any type
class Outgestor(val registry: ActorRef)
  extends Actor with ActorLogging with ServiceClient with SubscriptionActor[Nothing] { 
  
  val dependencies = "IForestAnomalyDetector" :: Nil
  lazy val subscriptions = {
    log.info("Forced subcription list")
    val ingest: ActorRef = dependencyMap("IForestAnomalyDetector").get
    Set[Subscription](Subscription(ingest, { _ => true }))
  }

  def beginService() = initialize()
  def endService() = ()  // TODO

  def process(v: Any): Unit = {
    log.info("Outgestor: " + v)
  }
}

//object Outgestor {
//  def props(subscriptions: Set[ActorRef]): Props = Props(new Outgestor(subscriptions))
//}
