package com.galois.adapt

import com.galois.adapt.cdm17._

import akka.actor._

// This actor is special: it performs entity resolution on the input, then feeds that back out. 
class ErActor(val registry: ActorRef) extends Actor with ActorLogging with ServiceClient with SubscriptionActor[CDM17] {

  val dependencies = "FileIngestActor" :: Nil
  lazy val subscriptions = {
    log.info("Forced subcription list")
    val ingest: ActorRef = dependencyMap("FileIngestActor").get
    Set[Subscription](Subscription(ingest, _.isInstanceOf[CDM17]))
  }

  def beginService() = {
    log.info("Begin service")
    initialize()    
  }
  def endService() = ()  // TODO

  override def process = { case c: CDM17 => broadCast(c) }

  def statusReport = Map("ER Actor" -> "INSERT STATUS HERE")
}

