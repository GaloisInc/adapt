package com.galois.adapt

import akka.actor._

// An actor that receives input of type `T` and produces output of type `U`
trait SubscriptionActor[T,U] extends Actor {

  def subscriptions: Set[Subscription[T]]          // These are going to be the sources of messages
  var subscribers: Set[Subscription[_]] = Set[Subscription[_]]() // This is a list that will grow to be the places to send


  def initialize(): Unit = {
    // On initialization, send subscription request to upstream producers
    subscriptions.foreach { s => s.target ! s.copy(target = self) }
  }
  
  // Accordingly, an AdActor must be prepared to recieve subscription requests too...
  def receive = {
    case s @ Subscription(_,_) => subscribers += s
    case t: T => process(t.asInstanceOf[T])
  }

  // What to do with a message. This is where calls to `broadCast` will be made
  def process(msg: T): Unit

  // AdActors usually don't send messages to articular actors - they broadcast to their subscribers
  def broadCast(msg: U): Unit = for (Subscription(target, pack) <- subscribers)
    pack(msg) foreach { m => target ! m }
    
}

// A subscription aggregates all the information needed on the _sender_ side to pass a message along
// to a receiver.
case class Subscription[T](
  target: ActorRef,        // Subscribing actor
  pack: Any => Option[T]     // Determines whether the actor is interested in a particular message
                           // and packs the message into a format the actor is expecting
)

