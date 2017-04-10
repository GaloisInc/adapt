package com.galois.adapt

import java.util.UUID

import akka.actor._
import com.galois.adapt.cdm17.{Event, FileObject, Subject, TimeMarker}


class RankedDataActor(/*val registry: ActorRef*/) extends Actor with ActorLogging /*with ServiceClient with SubscriptionActor[List[(String,Set[UUID],Float)]] with ReportsStatus*/ {
//  val dependencies = List("ProcessWritesFile", "FileIngestActor")

//  def beginService() = initialize()
//  def endService() = ()

  def statusReport = Map("items_length" -> items.length)

//  def subscriptions = Set(
////    Subscription(dependencyMap("FileWrites").get, _ => true),
//    Subscription(dependencyMap("FileIngestActor").get, {   // TODO: the whole pipeline needs to pass the time marker through the system!
//      case t: TimeMarker => true
//      case _ => false
//    }),
//    Subscription(dependencyMap("ProcessWritesFile").get, _ => true)
//  )


  var items: List[(String, Set[UUID], List[Float])] = List.empty

  val name = "ProcessWritesFile"

  def receive = {
    case (s:Subject, e:Event, f:FileObject) =>
      items = (name, Set(s.uuid, e.uuid, f.uuid), List[Float](
        s.cmdLine.map(_.length).getOrElse(0).toFloat,
        e.size.getOrElse(0L).toFloat,
        f.size.getOrElse(0L).toFloat
      ) ) :: items

    case t: TimeMarker =>
      val scored = items.map(i => (i._1, i._2, i._3.sum / i._3.length))
      val sorted = scored.sortBy(_._3).reverse
      println(s"\nSorted: ${sorted.length}\n")
      sorted.take(20).foreach(println)
      println(s"\nUnique: ${sorted.map(_._2).toSet.size}\n")
  }
}
