package com.galois.adapt.adm

import scala.collection.SortedMap

// Represents a store of things of type `K` on which we want to add expiry times.
class Fridge[K] {

  type TimeNanos = Long

  private var key2expiry: Map[K, TimeNanos] = Map()
  private var expiry2key: SortedMap[TimeNanos, List[K]] = SortedMap()
  var counter = 0

  // Check that the same keys and times are in the two maps
  def checkInvariants(): Unit = {
    assert(key2expiry.keySet == expiry2key.values.flatten.toSet)
    assert(key2expiry.values.toSet == expiry2key.keySet)
  }

  // Introduce or update an expiry time associated with a key
  def updateExpiryTime(key: K, newTime: TimeNanos): Unit = {
    for (oldTime <- key2expiry.get(key)) {
      expiry2key.get(oldTime) match {
        case Some(List(key1)) if key == key1 => expiry2key = expiry2key - oldTime
        case Some(keys) => expiry2key = expiry2key + (oldTime -> keys.filter(_ != key))
        case None => /* do nothing */
      }
    }

    key2expiry = key2expiry + (key -> newTime)
    expiry2key = expiry2key + (newTime -> (key :: expiry2key.getOrElse(newTime,Nil)))
  }

  // See what keys are next in line to be expired (have the smallest expiry time)
  def peekFirstToExpire: Option[(List[K], TimeNanos)] = {
    expiry2key.headOption.map { case (t,k) => (k,t) }
  }

  // Remove the keys that are next in line to be expired (have the smallest expiry time)
  def popFirstToExpire(): Option[(List[K], TimeNanos)] = {
    peekFirstToExpire match {
      case None => None
      case toReturn@Some((keys, time)) =>
        expiry2key = expiry2key - time
        for (key <- keys)
          key2expiry = key2expiry - key
        toReturn
    }
  }

  def keySet: Set[K] = {
    key2expiry.keySet
  }
}

object Fridge {
  def empty[K]: Fridge[K] = new Fridge()
}
