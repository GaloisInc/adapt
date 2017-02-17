package com.galois.adapt

/**
  * Copyright  2015  Comcast Cable Communications Management, LLC
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

//package com.comcast.csv.akka.serviceregistry

import akka.actor._
import akka.persistence.{PersistentActor, RecoveryCompleted, SaveSnapshotSuccess, SnapshotOffer}
import com.galois.adapt.ServiceRegistryInternalProtocol.End
import com.galois.adapt.ServiceRegistryProtocol._

import scala.collection.mutable


object ServiceRegistry {
  def props = Props[ServiceRegistry]
  val identity = "serviceRegistry"
}


class ServiceRegistry extends PersistentActor with ActorLogging {

  // [aSubscriberOrPublisher]
  val subscribersPublishers = scala.collection.mutable.Set.empty[ActorRef]
  // Map[subscriber,Set[subscribedTo]]
  val subscribers = scala.collection.mutable.HashMap.empty[ActorRef, mutable.HashSet[String]]
  // Map[published,publisher]
  val publishers = scala.collection.mutable.HashMap.empty[String, ActorRef]

  log.info(s"ServiceRegistry created")

  override val persistenceId: String = ServiceRegistry.identity

  def recordSubscriberPublisher(subpub: AddSubscriberPublisher): Unit = {
    subscribersPublishers += subpub.subscriberPublisher
  }

  def considerRememberParticipant(participant: ActorRef): Unit = {
    if (!subscribersPublishers.contains(participant)) {
      val add = AddSubscriberPublisher(participant)
      persist(add)(recordSubscriberPublisher)
    }
  }

  def unrecordSubscriberPublisher(subpub: RemoveSubscriberPublisher): Unit = {
    subscribersPublishers -= subpub.subscriberPublisher
  }

  def considerForgetParticipant(participant: ActorRef): Unit = {

    def isSubscriberPublisherStillInUse(subpub: ActorRef): Boolean = {
      subscribers.contains(subpub) ||
        publishers.exists { case (serviceName, endPoint) => endPoint == subpub }
    }

    if (subscribersPublishers.contains(participant) && !isSubscriberPublisherStillInUse(participant)) {
      val remove = RemoveSubscriberPublisher(participant)
      persist(remove)(unrecordSubscriberPublisher)
    }
  }

  override def receiveRecover: Receive = {
    case add: AddSubscriberPublisher =>
      log.info(s"Received -> AddSubscriberPublisher: $add  from: ${sender()}")
      recordSubscriberPublisher(add)

    case remove: RemoveSubscriberPublisher =>
      log.info(s"Received -> RemoveSubscriberPublisher: $remove  from: ${sender()}")
      unrecordSubscriberPublisher(remove)

    case SnapshotOffer(_, snapshot: SnapshotAfterRecover) =>
      log.info(s"Received -> SnapshotOffer  from: ${sender()}")
    // do nothing

    case RecoveryCompleted =>
      log.info(s"Received -> RecoveryCompleted  from: ${sender()}")
      val registryHasRestarted = RegistryHasRestarted(self)
      subscribersPublishers.foreach(sp => sp ! registryHasRestarted)
      subscribersPublishers.clear()
      saveSnapshot(SnapshotAfterRecover())
  }

  override def receiveCommand: Receive = {

    case ps: PublishService =>
      log.info(s"Received -> PublishService: $ps  from: ${sender()}")
      publishers += (ps.serviceName -> ps.serviceEndpoint)
      subscribers.filter(p => p._2.contains(ps.serviceName))
        .foreach(p => p._1 ! ServiceAvailable(ps.serviceName, ps.serviceEndpoint))
      context.watch(ps.serviceEndpoint)
      considerRememberParticipant(ps.serviceEndpoint)

    case ups: UnPublishService =>
      log.info(s"Received -> UnPublishService: $ups  from: ${sender()}")
      val serviceEndpoint = publishers.get(ups.serviceName)
      publishers.remove(ups.serviceName)
      subscribers.filter(p => p._2.contains(ups.serviceName))
        .foreach(p => p._1 ! ServiceUnAvailable(ups.serviceName))
      serviceEndpoint.foreach(ep => considerForgetParticipant(ep))

    case ss: SubscribeToService =>
      log.info(s"Received -> SubscribeToService: $ss  from: ${sender()}")
      subscribers += (sender() -> subscribers.get(sender())
        .orElse(Some(new mutable.HashSet[String])).map(s => {
        s + ss.serviceName
      })
        .getOrElse(new mutable.HashSet[String]))
      publishers.filter(p => p._1 == ss.serviceName)
        .foreach(p => sender() ! ServiceAvailable(ss.serviceName, p._2))
      considerRememberParticipant(sender())

    case us: UnSubscribeToService =>
      log.info(s"Received -> UnSubscribeToService: $us  from: ${sender()}")
      subscribers += (sender() -> subscribers.get(sender())
        .orElse(Some(new mutable.HashSet[String])).map(s => {
        s - us.serviceName
      })
        .getOrElse(new mutable.HashSet[String]))
      considerForgetParticipant(sender())

    case rs: RequestService =>
      log.info(s"Received -> RequestService: $rs  from: ${sender()}")
      publishers.find(p => p._1 == rs.serviceName) match {
        case Some(svc) =>
          sender() ! RespondService(rs.serviceName, svc._2)
        case None =>
          sender() ! RespondServiceUnAvailable(rs.serviceName)
      }

    case terminated: Terminated =>
      log.info(s"Received -> Terminated: $terminated  from: ${sender()}")
      var toRemoveServiceName: Option[String] = None
      publishers.find(p => p._2 == terminated.getActor).foreach(p2 => {
        toRemoveServiceName = Some(p2._1)
        subscribers.filter(p3 => p3._2.contains(p2._1))
          .foreach(p4 => p4._1 ! ServiceUnAvailable(p2._1))
      })
      toRemoveServiceName.foreach(serviceName => publishers.remove(serviceName))

    case sss: SaveSnapshotSuccess =>
      log.info(s"Received -> SaveSnapshotSuccess: $sss  from: ${sender()}")

    case End =>
      log.info(s"Received -> End  from: ${sender()}")

    case msg =>
      log.warning(s"Received unknown message: $msg  from: ${sender()}")
  }
}

/**
  * Private ServiceRegistry messages.
  */
object ServiceRegistryInternalProtocol {
  case object End
}

case class AddSubscriberPublisher(subscriberPublisher: ActorRef)
case class RemoveSubscriberPublisher(subscriberPublisher: ActorRef)
case class SnapshotAfterRecover()


object ServiceProtocol {
  case class ServiceNotOnline(serviceName: String)
}


/**
  * Protocol for interacting with the Service Registry.
  */
object ServiceRegistryProtocol {
  /**
    * ServiceRegistry sends to service client when subscribed to service is now online.
    */
  case class ServiceAvailable(serviceName: String, serviceEndpoint: ActorRef)

  /**
    * ServiceRegistry sends to service client when subscribed to service is now offline.
    */
  case class ServiceUnAvailable(serviceName: String)




  /**
    * Service implementor sends to ServiceRegistry when transitions to online.
    */
  case class PublishService(serviceName: String, serviceEndpoint: ActorRef)

  /**
    * Service implementor sends to ServiceRegistry when transitions to offline.
    */
  case class UnPublishService(serviceName: String)

  /**
    * Service client sends to ServiceRegistry when requiring dependent service.
    */
  case class SubscribeToService(serviceName: String)

  /**
    * Service client sends to ServiceRegistry when no longer requiring dependent service.
    */
  case class UnSubscribeToService(serviceName: String)




  /**
    * ServiceRegistry sends to publishers and subscribers when ServiceRegistry has been restarted
    *   requiring all participants to re-subscribe and re-publish.
    */
  case class RegistryHasRestarted(registry: ActorRef)

  /**
    * Realtime request for a service.
    */
  case class RequestService(serviceName: String)

  /**
    * Realtime response of a service endpoint.
    */
  case class RespondService(serviceName: String, serviceEndpoint: ActorRef)

  /**
    * Realtime response that a service is not online.
    */
  case class RespondServiceUnAvailable(serviceName: String)

}