package com.galois.adapt

import akka.actor.{Actor, ActorLogging}
import com.galois.adapt.adm._
import com.galois.adapt.cdm18._


object NoveltyDetection {
  type Event   = AdmEvent
  type Subject = (AdmSubject, Set[AdmPathNode])
  type Object  = (ADM, Set[AdmPathNode])

  type ExtractedValue = String
  type Discriminator = (Event, Subject, Object) => ExtractedValue
  type D = List[Discriminator]
  type F = (Event, Subject, Object) => Boolean

  val noveltyThreshold: Float = 0.01F
  type IsNovel = Option[List[(String, Float)]]

  class Tree(filter: F, discriminators: D) {
    var children = Map.empty[ExtractedValue, Tree]
    var counter = 0

    def globalNoveltyRate = ???

    def localNovelty: Float = if (counter == 0) 1F else children.size / counter.toFloat
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
      val historicalNoveltyRate = localNovelty
      counter += 1
      discriminators match {
        case Nil => None

        case discriminator :: remainingDiscriminators =>
          val extracted = discriminator(e, s, o)
          val childExists = children.contains(extracted)
          val childTree = children.getOrElse(extracted, new Tree(filter, remainingDiscriminators))
          children = children + (extracted -> childTree)
          val childUpdateResult = childTree.update(e, s, o)

          if (childUpdateResult.isDefined)
            childUpdateResult.map(childPaths => (extracted -> historicalNoveltyRate) :: childPaths)
          else if ( ! childExists && historicalNoveltyRate < noveltyThreshold)  // TODO: consider if we should ALSO emit another detection if this still matches here. => List[List[String]]
            Some(List(extracted -> historicalNoveltyRate))
          else None
      }
    } else None
  }
}


class NoveltyActor extends Actor with ActorLogging {
  import NoveltyDetection._

  val f = (e: Event, s: Subject, o: Object) => e.eventType == EVENT_READ || e.eventType == EVENT_WRITE
  val ds = List(
    (e: Event, s: Subject, o: Object) => s._2.toList.map(_.path).sorted.mkString("[", ",", "]"),
    (e: Event, s: Subject, o: Object) => o._2.toList.map(_.path).sorted.mkString("[", ",", "]")
  )

  val root = new Tree(f, ds)

  def receive = {
    case (e: Event, Some(s: AdmSubject), subPathNodes: Set[AdmPathNode], Some(o: ADM), objPathNodes: Set[AdmPathNode]) =>
      root.update(e, s -> subPathNodes, o -> objPathNodes).foreach(println)
      sender() ! Ack

    case InitMsg => sender() ! Ack
    case CompleteMsg => root.print(0, "")
    case x => log.error(s"Received Unknown Message: $x")
  }
}