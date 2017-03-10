package com.galois.adapt

import akka.actor._

// Represents something that will subscribe to messages of any type
class Outgestor(val registry: ActorRef)
  extends Actor with ActorLogging with ServiceClient with SubscriptionActor[Nothing] { 
  
  val dependencies = "FileWrites" :: "ProcessWrites" :: Nil
  lazy val subscriptions = {
    val fileWrites: ActorRef = dependencyMap("FileWrites").get
    val processWrites: ActorRef = dependencyMap("ProcessWrites").get
    Set(Subscription(processWrites, { _ => true }), Subscription(fileWrites, { _ => true }))
  }

  def beginService() = initialize()
  def endService() = ()  // TODO

  override def process = {
    case v => log.info("Outgestor: " + v)
  }
}

