package com.galois.adapt

import akka.actor._
import com.galois.adapt.cdm16.CDM16

// Represents something that will subscribe to messages of any type
class Outgester(val registry: ActorRef)
  extends Actor with ActorLogging with ServiceClient with SubscriptionActor[Nothing] with ReportsStatus {
  
  val dependencies = "FileWrites" :: "ProcessWrites" :: Nil
  lazy val subscriptions = {
    val fileWrites: ActorRef = dependencyMap("FileWrites").get
    val processWrites: ActorRef = dependencyMap("ProcessWrites").get
    Set(Subscription(processWrites, { _ => true }), Subscription(fileWrites, { _ => true }))
  }

  def beginService() = initialize()
  def endService() = ()  // TODO

  def statusReport = Map()

  override def process = {
    case v => log.info("Outgestor: " + v)
  }
}

