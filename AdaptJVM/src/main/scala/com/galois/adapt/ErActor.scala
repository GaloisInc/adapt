package com.galois.adapt

import com.galois.adapt.cdm15._

import akka.actor._

// This actor is special: it performs entity resolution on the input, then feeds that back out. 
class ErActor(val registry: ActorRef) extends Actor with ActorLogging with ServiceClient with SubscriptionActor[CDM15] {
  
  val dependencies = "FileIngestActor" :: Nil
  lazy val subscriptions = {
    log.info("Forced subcription list")
    val ingest: ActorRef = dependencyMap("FileIngestActor").get
    Set[Subscription](Subscription(ingest, _.isInstanceOf[CDM15]))
  }

  def beginService() = {
    log.info("Begin service")
    initialize()    
  }
  def endService() = ()  // TODO

  override def process = { case c: CDM15 => broadCast(c) }
}

