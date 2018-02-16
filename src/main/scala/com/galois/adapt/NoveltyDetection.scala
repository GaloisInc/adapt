package com.galois.adapt

import akka.actor.{Actor, ActorLogging}
import com.galois.adapt.adm.{ADM, _}
import com.galois.adapt.cdm18.EVENT_READ


object NoveltyDetection {
  type Event = AdmEvent
  type Subject = ADM // (AdmSubject, AdmPathNode)
  type Object = ADM  // (ADM, AdmPathNode)

  type ExtractedValue = String
  type Discriminator = (Event, Subject, Object) => ExtractedValue
  type D = List[Discriminator]
  type F = (Event, Subject, Object) => Boolean

  val noveltyThreshold: Float = 0.01F
  type IsNovel = Option[List[String]]

  class Tree(filter: F, discriminators: D) {
    var children = Map.empty[ExtractedValue, Tree]
    var counter = 0

    def localNovelty: Float = if (counter == 0) 0F else children.size / counter.toFloat
//    def getChildCount: Int = counter  // if (children.isEmpty) counter else children.map(_._2.getChildCount).sum
//    def subtreeNovelty: Float = Try[Float](children.size / getChildCount.toFloat).getOrElse(1F)

    def print(yourDepth: Int, key: String): Unit = {
      val prefix = (0 until (4 * yourDepth)).map(_ => " ").mkString("")
      println("\t")
      println(s"$prefix### Tree at depth: $yourDepth  for key: $key")
      println(s"${prefix}Counter: $counter  Children keys: ${children.keys}")
      children.foreach{ case (k,v) => v.print(yourDepth + 1, k)}
    }

    def update(e: Event, s: Subject, o: Object): IsNovel = if (filter(e, s, o)) {
      counter += 1
      discriminators match {
        case Nil =>
          if (counter == 1) Some(List.empty) else None
        case discriminator :: remainingDiscriminators =>
          val extracted = discriminator(e, s, o)
          val childTree = children.getOrElse(extracted, new Tree(filter, remainingDiscriminators))
          children = children + (extracted -> childTree)
          val childUpdateResult = childTree.update(e, s, o)
          if (childUpdateResult.isDefined && localNovelty < noveltyThreshold)
            Some(extracted :: childUpdateResult.getOrElse(List.empty))
          else None
      }
    } else None
  }
}


class NoveltyActor extends Actor with ActorLogging {
  import NoveltyDetection._

  val f = (e: Event, s: Subject, o: Object) => e.eventType == EVENT_READ
  val ds = List(
    (e: Event, s: Subject, o: Object) => math.abs(s.uuid.getLeastSignificantBits % 8).toString,
    (e: Event, s: Subject, o: Object) => o.toMap.get("fileObjectType").toString
  )

  val root = new Tree(f, ds)

  def receive = {
    case (e: Event, Some(s: ADM), Some(o: ADM)) =>
      root.update(e, s, o).foreach(println)
      sender() ! Ack

    case InitMsg => sender() ! Ack
    case CompleteMsg => root.print(0, "")
    case x => log.error(s"Received Unknown Message: $x")

  }
}