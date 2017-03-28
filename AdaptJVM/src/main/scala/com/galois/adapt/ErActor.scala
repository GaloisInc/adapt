package com.galois.adapt

import com.galois.adapt.cdm16._

import akka.actor._

// This actor is special: it performs entity resolution on the input, then feeds that back out. 
class ErActor(val registry: ActorRef) extends Actor with ActorLogging with ServiceClient with SubscriptionActor[CDM16] with ReportsStatus {
  
  val dependencies = "FileIngestActor" :: Nil
  lazy val subscriptions = {
    log.info("Forced subcription list")
    val ingest: ActorRef = dependencyMap("FileIngestActor").get
    Set[Subscription](Subscription(ingest, _.isInstanceOf[CDM16]))
  }

  def beginService() = {
    log.info("Begin service")
    initialize()    
  }
  def endService() = ()  // TODO


  def statusReport = Map("ER Actor" -> "INSERT STATUS HERE")

  override def process = { case c: CDM16 => broadCast(c) }
}

