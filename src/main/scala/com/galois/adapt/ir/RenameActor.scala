package com.galois.adapt.ir

import scala.collection.mutable
import akka.actor._

// Non-blocking key-value store with an actor interface
class MapActor[K,V] extends Actor with ActorLogging {
  val store: mutable.Map[K,V] = mutable.Map[K,V]()

  def receive: PartialFunction[Any,Unit] = {
    case Put(key: K, value: V) => store(key) = value
    case Get(key: K) => sender() ! Val(store.get(key))
  }
}

// Types of messages that should be sent to/from a 'MapActor'
sealed trait MapMessage[K,V]
final case class Put[K,V](key: K, value: V) extends MapMessage[K,V]
final case class Get[K,V](key: K)           extends MapMessage[K,V]
final case class Val[K,V](value: Option[V]) extends MapMessage[K,V]


