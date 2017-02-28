package com.galois.adapt

import com.galois.adapt.cdm13._

import akka.actor._

// This actor is special: it performs entity resolution on the input, then feeds that back out. 
class ErActor(val registry: ActorRef) extends Actor with ActorLogging with ServiceClient with SubscriptionActor[Any,CDM13] {
  
  val dependencies = "FileIngestActor" :: Nil
  lazy val subscriptions = {
    log.info("Forced subcription list")
    val ingest: ActorRef = dependencyMap("FileIngestActor").get
    Set[Subscription[Any]](Subscription(ingest, (c: Any) => Some(c.asInstanceOf[CDM13])))
  }

  def beginService() = {
    log.info("Begin service")
    initialize()    
  }
  def endService() = ()  // TODO

  override def receive
    = super.receive orElse ({ case a => broadCast(a.asInstanceOf[CDM13]) }: PartialFunction[Any,Unit])
}

