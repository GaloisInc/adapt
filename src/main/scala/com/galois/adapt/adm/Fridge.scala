package com.galois.adapt.adm

import com.galois.adapt.adm.EntityResolution.Time

import scala.collection.SortedMap

// Represents a store of things of type `K` on which we want to add expiry times.
class Fridge[K] {

  private var key2expiry: Map[K, Time] = Map()
  private var expiryNanos2key: SortedMap[Long, List[K]] = SortedMap()
  private var expiryCount2key: SortedMap[Long, List[K]] = SortedMap()
  var counter = 0

  // Check that the same keys and times are in the two maps
  def checkInvariants(): Unit = {
    assert(key2expiry.keySet == expiryNanos2key.values.flatten.toSet)
    assert(key2expiry.keySet == expiryCount2key.values.flatten.toSet)
    assert(key2expiry.values.toSet == expiryNanos2key.keySet)
    assert(key2expiry.values.toSet == expiryCount2key.keySet)
  }

  // Introduce or update an expiry time associated with a key
  def updateExpiryTime(key: K, newTime: Time): Unit = {
    for (Time(oldTime, oldCount) <- key2expiry.get(key)) {
      expiryNanos2key.get(oldTime) match {
        case Some(List(key1)) if key == key1 => expiryNanos2key = expiryNanos2key - oldTime
        case Some(keys) => expiryNanos2key = expiryNanos2key + (oldTime -> keys.filter(_ != key))
        case None => /* do nothing */
      }

      expiryCount2key.get(oldCount) match {
        case Some(List(key1)) if key == key1 => expiryCount2key = expiryCount2key - oldCount
        case Some(keys) => expiryCount2key = expiryCount2key + (oldCount -> keys.filter(_ != key))
        case None => /* do nothing */
      }
    }

    key2expiry = key2expiry + (key -> newTime)
    expiryNanos2key = expiryNanos2key + (newTime.nanos -> (key :: expiryNanos2key.getOrElse(newTime.nanos,Nil)))
    expiryCount2key = expiryCount2key + (newTime.count -> (key :: expiryCount2key.getOrElse(newTime.count,Nil)))
  }

  // See what keys are next in line to be expired (have the smallest expiry time)
  def peekFirstNanosToExpire: Option[(List[K], Long)] = {
    expiryNanos2key.headOption.map { case (t,k) => (k,t) }
  }

  // See what keys are next in line to be expired (have the smallest expiry count)
  def peekFirstCountToExpire: Option[(List[K], Long)] = {
    expiryCount2key.headOption.map { case (t,k) => (k,t) }
  }

  // Remove the keys that are next in line to be expired (have the smallest expiry time)
  def popFirstNanosToExpire(): Option[(List[K], Long)] = {
    peekFirstNanosToExpire match {
      case None => None
      case toReturn@Some((keys, time)) =>
        expiryNanos2key = expiryNanos2key - time
        for (key <- keys) {
          for (Time(_, count) <- key2expiry.get(key)) {
            expiryCount2key.get(count) match {
              case Some(List(key1)) if key == key1 => expiryCount2key = expiryCount2key - count
              case Some(keys1) => expiryCount2key = expiryCount2key + (count -> keys1.filter(_ != key))
              case None => /* do nothing */
            }
          }
          key2expiry = key2expiry - key
        }
        toReturn
    }
  }

  // Remove the keys that are next in line to be expired (have the smallest expiry count)
  def popFirstCountToExpire(): Option[(List[K], Long)] = {
    peekFirstCountToExpire match {
      case None => None
      case toReturn@Some((keys, count)) =>
        expiryCount2key = expiryCount2key - count
        for (key <- keys) {
          for (Time(time, _) <- key2expiry.get(key)) {
            expiryNanos2key.get(time) match {
              case Some(List(key1)) if key == key1 => expiryNanos2key = expiryNanos2key - time
              case Some(keys1) => expiryNanos2key = expiryNanos2key + (time -> keys1.filter(_ != key))
              case None => /* do nothing */
            }
          }
          key2expiry = key2expiry - key
        }
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
