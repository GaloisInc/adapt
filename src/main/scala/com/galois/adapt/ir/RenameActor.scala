package com.galois.adapt.ir

import scala.collection.mutable
import akka.actor._


class RenameActor extends MapActor[java.util.UUID /* CDM */, java.util.UUID /* IR */] with Actor with ActorLogging {
  def synthesizeValue() = java.util.UUID.randomUUID()
}

// Non-blocking key-value store with an actor interface
abstract class MapActor[K,V] extends Actor with ActorLogging {

  // How to create a new value for 'Get(<key>, true)' queries when `<key>` is not found
  def synthesizeValue(): V

  val store: mutable.Map[K,V] = mutable.Map[K,V]()

  def receive: PartialFunction[Any,Unit] = {
    case Put(key: K, value: V) => store.get(key) match {
      case None => store(key) = value
      case Some(value1) => log.error(s"Trying to write $value at key $key, but $value1 is already there")
    }
    case Get(key: K, false) => sender() ! Val(store.get(key))
    case Get(key: K, true) => store.get(key) match {
      case Some(value) => sender() ! Val(Some(value))
      case None => {
        val value = synthesizeValue()
        store(key) = value
        sender() ! SynVal(value)
      }
    }
  }
}

// Types of messages that should be sent to/from a 'MapActor'
sealed trait MapMessage[K,V]
final case class Put[K,V](key: K, value: V) extends MapMessage[K,V]
final case class Get[K,V](key: K, createIfNotFound: Boolean = false) extends MapMessage[K,V]
final case class Val[K,V](value: Option[V]) extends MapMessage[K,V]
final case class SynVal[K,V](value: V) extends MapMessage[K,V]


