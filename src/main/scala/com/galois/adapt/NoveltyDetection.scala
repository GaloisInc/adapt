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

  case class NoveltyTree(filter: F, discriminators: D) {
    var children = Map.empty[ExtractedValue, NoveltyTree]
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
          val childTree = children.getOrElse(extracted, new NoveltyTree(filter, remainingDiscriminators))
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



  trait PpmTree {
    def getCount: Int
    def update(e: Event, s: Subject, o: Object): Option[Boolean]
    def getTreeReport(yourDepth: Int = 0, key: String = "", yourProbability: Float = 1F, parentGlobalProb: Float = 1F): TreeReport
  }
  case object PpmTree {
    def apply(filter: F, discriminators: D): PpmTree = new SymbolNode(filter, discriminators)
  }

  class SymbolNode(filter: F, discriminators: D) extends PpmTree {
    private var counter = 0
    def getCount: Int = counter

    var children = Map.empty[ExtractedValue, PpmTree]
    children = children + ("_?_" -> new QNode(() => children.size - 1))

    def totalChildCounts = children.values.map(_.getCount).sum

    def localChildProbability(identifier: ExtractedValue): Float =
      children.getOrElse(identifier, children("_?_")).getCount.toFloat / totalChildCounts

    def update(e: Event, s: Subject, o: Object): Option[Boolean] = if (filter(e,s,o)) {
      counter += 1
      discriminators match {
        case Nil => Some(false)
        case discriminator :: remainingDiscriminators =>
          val extracted = discriminator(e, s, o)
          val childExists = children.contains(extracted)
          val childNode = children.getOrElse(extracted, new SymbolNode(filter, remainingDiscriminators))
          if ( ! childExists) children = children + (extracted -> childNode)
          childNode.update(e, s, o)
      }
    } else None

    def getTreeReport(yourDepth: Int, key: String, yourProbability: Float, parentGlobalProb: Float): TreeReport =
      TreeReport(yourDepth, key, yourProbability, yourProbability * parentGlobalProb, getCount,
        if (children.size > 1) children.toSet[(ExtractedValue, PpmTree)].map{ case (k,v) => v.getTreeReport(yourDepth + 1, k, localChildProbability(k), yourProbability * parentGlobalProb)} else Set.empty
      )
  }

  class QNode(counterFunc: () => Int) extends PpmTree {
    def getCount = counterFunc()
    def update(e: Event, s: Subject, o: Object): Option[Boolean] = Some(true)
    def getTreeReport(yourDepth: Int, key: String, yourProbability: Float, parentGlobalProb: Float): TreeReport =
      TreeReport(yourDepth, key, yourProbability, yourProbability * parentGlobalProb, getCount, Set.empty)
  }
}

case class TreeReport(depth: Int, key: String, localProb: Float, globalProb: Float, count: Int, children: Set[TreeReport]) {
  override def toString = {
    val indent = (0 until (4 * depth)).map(_ => " ").mkString("")
    val depthString = if (depth < 10) s" $depth" else depth.toString
    val localProbString = localProb.toString + (0 until (13 - localProb.toString.length)).map(_ => " ").mkString("")
    val globalProbString = globalProb.toString + (0 until (13 - globalProb.toString.length)).map(_ => " ").mkString("")
    val countString = (0 until (9 - count.toString.length)).map(_ => " ").mkString("") + count.toString
    s"$indent### Depth: $depthString  Local Prob: $localProbString  Global Prob: $globalProbString  Counter: $countString  Key: $key" +
      children.toList.sortBy(r => 1F - r.localProb).par.map(_.toString).mkString("\n", "", "")
  }
}


class NoveltyActor extends Actor with ActorLogging {
  import NoveltyDetection._

  val f = (e: Event, s: Subject, o: Object) => e.eventType == EVENT_READ || e.eventType == EVENT_WRITE
  val ds = List(
    (e: Event, s: Subject, o: Object) => s._2.toList.map(_.path).sorted.mkString("[", ",", "]"),
    (e: Event, s: Subject, o: Object) => o._2.toList.map(_.path).sorted.mkString("[", ",", "]")
  )

  val processFileTouches = PpmTree(f, ds)
  val processesThatReadFiles = PpmTree((e,s,o) => e.eventType == EVENT_READ, ds.reverse)
  val filesExecutedByProcesses = PpmTree((e,s,o) => e.eventType == EVENT_EXECUTE, ds)

  def receive = {
    case (e: Event, Some(s: AdmSubject), subPathNodes: Set[AdmPathNode], Some(o: ADM), objPathNodes: Set[AdmPathNode]) =>
      processFileTouches.update(e, s -> subPathNodes, o -> objPathNodes)
      processesThatReadFiles.update(e, s -> subPathNodes, o -> objPathNodes)
      filesExecutedByProcesses.update(e, s -> subPathNodes, o -> objPathNodes)
      sender() ! Ack

    case InitMsg => sender() ! Ack
    case CompleteMsg => println(processFileTouches.getTreeReport().toString)
    case x => log.error(s"Received Unknown Message: $x")
  }
}
