package com.galois.adapt

import com.galois.adapt.cdm13._

import akka.actor._

// This actor is special: it performs entity resolution on the input, then feeds that back out. 
class ErActor() extends SubscriptionActor[Any,CDM13](Set()) {
  def process(a: Any) = broadCast(a.asInstanceOf[CDM13])
}

