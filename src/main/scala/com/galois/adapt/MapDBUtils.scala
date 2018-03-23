package com.galois.adapt

import java.util.function.BiConsumer

import bloomfilter.mutable.BloomFilter
import org.mapdb.BTreeMapJava.KeySet
import org.mapdb.{BTreeMap, BTreeMapJava, HTreeMap}

import scala.collection.mutable

/* This object contains utilities for wrapping MapDB map and set types into more palatable scala ones.
 *
 * Turn to this if you are finding that you have huge maps and you want an easy way to switch between having these on
 * disk, in memory, etc.
 */
object MapDBUtils {

  // Subset of the `Map` trait. I'm too lazy to implement more of it. Can be replaced with `mutable.Map` for debugging.
  trait AlmostMap[K,V] {
    def contains(key: K): Boolean
    def apply(key: K): V
    def update(key: K, value: V): Unit
    def get(key: K): Option[V]
    def foreach(func: (K, V) => Unit): Unit
    def size(): Long
  }

  // Subset of the `Set` trait. I'm too lazy to implement more of it. Can be replaced with `mutable.Set` for debugging.
  trait AlmostSet[V] {
    def contains(value: V): Boolean
    def add(value: V): Unit
    def size(): Long
  }

  // Wrap a MapDB map into an `AlmostMap`
  def almostMap[K1,K2,V1,V2](
    map: HTreeMap[K1,V1],
    intoKey: K2 => K1, outKey: K1 => K2,      // better be inverses
    intoValue: V2 => V1, outValue: V1 => V2   // better be inverses
  ): AlmostMap[K2,V2] = new AlmostMap[K2,V2] {

    def contains(k2: K2): Boolean = map.containsKey(intoKey(k2))

    def apply(k2: K2): V2 = outValue(map.get(intoKey(k2)))

    def update(k2: K2, v2: V2): Unit = map.put(intoKey(k2), intoValue(v2))

    def get(k2: K2): Option[V2] = Option(map.get(intoKey(k2))).map(outValue)

    def foreach(func: (K2, V2) => Unit): Unit = map.forEach(new BiConsumer[K1, V1] {
      override def accept(k1: K1, v1: V1): Unit = func(outKey(k1), outValue(v1))
    })

    def size(): Long = map.sizeLong()
  }

  // Wrap a mutable map into an `AlmostMap`
  def almostMap[K,V](map: mutable.Map[K,V]): AlmostMap[K,V] = new AlmostMap[K,V] {
    def contains(k: K): Boolean = map.contains(k)

    def apply(k: K): V = map.apply(k)

    def update(k: K, v: V): Unit = map.update(k, v)

    def get(k: K): Option[V] = map.get(k)

    def foreach(func: (K, V) => Unit): Unit = map.foreach { case (k,v) => func(k,v) }

    def size(): Long = map.size
  }

  // Wrap a MapDB set into an `AlmostSet`
  def almostSet[V1,V2](
    set: HTreeMap.KeySet[V1],
    intoValue: V2 => V1, outValue: V1 => V2   // better be inverses
  ): AlmostSet[V2] = new AlmostSet[V2] {

    def contains(v2: V2): Boolean = set.contains(intoValue(v2))

    def add(v2: V2): Unit = set.add(intoValue(v2))

    def size(): Long = set.getMap.sizeLong()
  }

  // Wrap a mutable set into an `AlmostSet`
  def almostSet[V](set: mutable.Set[V]): AlmostSet[V] = new AlmostSet[V] {
    def contains(v: V): Boolean = set.contains(v)

    def add(v: V): Unit = set.add(v)

    def size(): Long = set.size
  }

  def almostSet[V](bf: BloomFilter[V]): AlmostSet[V] = new AlmostSet[V] {
    private var count: Long = 0

    def contains(v: V): Boolean = bf.mightContain(v)

    def add(v: V): Unit = if (!bf.mightContain(v)) {
      count += 1
      bf.add(v)
    }

    def size(): Long = count
  }

}
