package com.galois.adapt.adm

import com.galois.adapt.adm.EntityResolution.Time

import scala.collection.{SortedMap, mutable}

// Represents a store of things of type `K` on which we want to add expiry times.
class Fridge[K] {

  private var key2expiry: Map[K, Time] = Map()
  private var expiryNanos2key: SortedMap[Long, mutable.Set[K]] = SortedMap()
  private var expiryCount2key: SortedMap[Long, mutable.Set[K]] = SortedMap()
  var counter = 0

  // Check that the same keys and times are in the two maps
  def checkInvariants(): Unit = {
    assert(key2expiry.keySet == expiryNanos2key.values.flatten)
    assert(key2expiry.keySet == expiryCount2key.values.flatten)
    assert(key2expiry.values.toSet == expiryNanos2key.keySet)
    assert(key2expiry.values.toSet == expiryCount2key.keySet)
  }

  // Introduce or update an expiry time associated with a key
  def updateExpiryTime(key: K, newTime: Time): Unit = {
    for (Time(oldTime, oldCount) <- key2expiry.get(key)) {
      expiryNanos2key.get(oldTime) match {
        case Some(List(key1)) if key == key1 => expiryNanos2key = expiryNanos2key - oldTime
        case Some(keys) => keys -= key
        case None => /* do nothing */
      }

      expiryCount2key.get(oldCount) match {
        case Some(List(key1)) if key == key1 => expiryCount2key = expiryCount2key - oldCount
        case Some(keys) => keys -= key
        case None => /* do nothing */
      }
    }

    key2expiry = key2expiry + (key -> newTime)
    expiryNanos2key = expiryNanos2key + (newTime.nanos -> (expiryNanos2key.getOrElse(newTime.nanos, mutable.Set.empty) += key))
    expiryCount2key = expiryCount2key + (newTime.count -> (expiryCount2key.getOrElse(newTime.count, mutable.Set.empty) += key))
  }

  // See what keys are next in line to be expired (have the smallest expiry time)
  def peekFirstNanosToExpire: Option[Long] = {
    expiryNanos2key.headOption.map { case (t,_) => t }
  }

  // See what keys are next in line to be expired (have the smallest expiry count)
  def peekFirstCountToExpire: Option[Long] = {
    expiryCount2key.headOption.map { case (t,_) => t }
  }

  // Remove the keys that are next in line to be expired (have the smallest expiry time)
  def popFirstNanosToExpire(): Option[(Iterable[K], Long)] = {
    expiryNanos2key.headOption match {
      case None => None
      case Some((time, keys)) =>
        expiryNanos2key = expiryNanos2key - time
        for (key <- keys) {
          for (Time(_, count) <- key2expiry.get(key)) {
            expiryCount2key.get(count) match {
              case Some(List(key1)) if key == key1 => expiryCount2key = expiryCount2key - count
              case Some(keys1) => keys1 -= key
              case None => /* do nothing */
            }
          }
          key2expiry = key2expiry - key
        }
        Some(keys -> time)
    }
  }

  // Remove the keys that are next in line to be expired (have the smallest expiry count)
  def popFirstCountToExpire(): Option[(Iterable[K], Long)] = {
    expiryCount2key.headOption match {
      case None => None
      case Some((count, keys)) =>
        expiryCount2key = expiryCount2key - count
        for (key <- keys) {
          for (Time(time, _) <- key2expiry.get(key)) {
            expiryNanos2key.get(time) match {
              case Some(List(key1)) if key == key1 => expiryNanos2key = expiryNanos2key - time
              case Some(keys1) => keys1 -= key
              case None => /* do nothing */
            }
          }
          key2expiry = key2expiry - key
        }
        Some(keys -> count)
    }
  }

  def keySet: Set[K] = {
    key2expiry.keySet
  }
}

object Fridge {
  def empty[K]: Fridge[K] = new Fridge()
}
