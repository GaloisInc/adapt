package com.galois.adapt.scepter

import akka.actor.{ActorRef, ActorSystem, Props, Actor, ActorLogging}
import com.galois.adapt.{ServiceClient, SubscriptionActor, Subscription}
import com.galois.adapt.cdm15._

/* This actor counts all of the CDM statements it receives and sends back these counts every time it
 * receives a 'HowMany' message
 */
class EventCountingTestActor(val registry: ActorRef) extends
  Actor with ActorLogging with ServiceClient with SubscriptionActor[Nothing] {

  val dependencies = "AcceptanceTestsActor" :: Nil
  lazy val subscriptions =  Set[Subscription](Subscription(dependencyMap("AcceptanceTestsActor").get, _ => true))
  
  def beginService() = initialize()
  def endService() = ()
  
  // Map of statement-name to its current count
  var typeCounter = Map.empty[String,Int]

  private def incrementTypeCount(name: String): Unit = {
    typeCounter = typeCounter.updated(name, typeCounter.getOrElse(name, 0) + 1)
  }

  override def process = {
    // Receive CDM statements to count
    case _: AbstractObject => incrementTypeCount("AbstractObject")
    case _: Event => incrementTypeCount("Event")
    case _: FileObject => incrementTypeCount("FileObject")
    case _: MemoryObject => incrementTypeCount("MemoryObject")
    case _: NetFlowObject => incrementTypeCount("NetFlowObject")
    case _: Principal => incrementTypeCount("Principal")
    case _: ProvenanceTagNode => incrementTypeCount("ProvenanceTagNode")
    case _: RegistryKeyObject => incrementTypeCount("RegistryKeyObject")
    case _: SrcSinkObject => incrementTypeCount("SrcSinkObject")
    case _: Subject => incrementTypeCount("Subject")
    case _: Value => incrementTypeCount("Value")

    // Receive a query asking about the counts stored
    case HowMany(name) =>
      sender() ! (name match {
        case "total" => typeCounter.values.sum
        case "each" => typeCounter
        case _ => typeCounter.getOrElse(name,0)
      })
  }
}
