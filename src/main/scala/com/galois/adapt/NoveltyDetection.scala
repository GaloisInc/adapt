package com.galois.adapt

import akka.actor.{Actor, ActorLogging}
import com.galois.adapt.NoveltyDetection._
import com.galois.adapt.adm._
import com.galois.adapt.cdm18.{EVENT_EXECUTE, EVENT_READ, EVENT_WRITE}
import java.io.{PrintWriter, File}


object NoveltyDetection {
  type Event = AdmEvent
  type Subject = (AdmSubject, Option[AdmPathNode])
  type Object = (ADM, Option[AdmPathNode])

  type ExtractedValue = String
  type Discriminator = (Event, Subject, Object) => ExtractedValue
  type D = List[Discriminator]
  type F = (Event, Subject, Object) => Boolean

  val noveltyThreshold: Float = 0.01F
  type Alarm = List[(String, Float, Float, Int)]
}


trait PpmTree {
  def getCount: Int
  def update(e: Event, s: Subject, o: Object): Option[Alarm] = this.update(e, s, o, 1F, 1F)
  def update(e: Event, s: Subject, o: Object, yourProb: Float = 1F, parentGlobalProb: Float = 1F): Option[Alarm]
  def getTreeRepr(yourDepth: Int = 0, key: String = "", yourProbability: Float = 1F, parentGlobalProb: Float = 1F): TreeRepr
}
case object PpmTree {
//  def apply(filter: F, discriminators: D): PpmTree = new SymbolNode(filter, discriminators)
  def apply(filter: F, discriminators: D, serialized: Option[TreeRepr] = None):PpmTree =  new SymbolNode(filter, discriminators, serialized)
}


class SymbolNode(val filter: F, val discriminators: D, repr: Option[TreeRepr] = None) extends PpmTree {
  private var counter = 0
  def getCount: Int = counter

  var children = Map.empty[ExtractedValue, PpmTree]

  repr.fold[Unit] {
    children = children + ("_?_" -> new QNode(children))
  } { thisTree =>
    counter = thisTree.count
    children = thisTree.children.map {
      case c if c.key == "_?_" => c.key -> new QNode(children)
      case c => c.key -> new SymbolNode(filter, discriminators.tail, Some(c))
    }.toMap
  }

  def totalChildCounts = children.values.map(_.getCount).sum

  def localChildProbability(identifier: ExtractedValue): Float =
    children.getOrElse(identifier, children("_?_")).getCount.toFloat / totalChildCounts

  def update(e: Event, s: Subject, o: Object, thisLocalProb: Float, parentGlobalProb: Float): Option[Alarm] = if (filter(e,s,o)) {
    counter += 1
    discriminators match {
      case Nil => None
      case discriminator :: remainingDiscriminators =>
        val extracted = discriminator(e, s, o)
//        if (extracted.size > 1) counter += (extracted.size - 1) // ensure the parent counts equal to the size of the flattened set. `- 1` because the counter is always incremented by one (even if no discriminators match the children)
//        extracted.flatMap { extracted =>

        val childExists = children.contains(extracted)  // Merge suffixes? Would have to alter existing child nodes in the case of a child key being the suffix of the incoming key.
        // What to do about a suffix merged into a full path, followed by another full path which could have had the suffix applied to it?
        // Perhaps suffix nodes should accumulate in their own nodes, and counted in aggregate on read. This would almost require parent counts to be computed lazily.
        // ...or skip suffix folding entirely.

        val childNode = children.getOrElse(extracted, new SymbolNode(filter, remainingDiscriminators))
        val childLocalProb = localChildProbability(extracted)
        val thisGlobalProb = thisLocalProb * parentGlobalProb
        if (childExists) childNode.update(e, s, o, childLocalProb, thisGlobalProb).map { alarmList =>
          // Append information about the child:
          (extracted, childLocalProb, thisGlobalProb * childLocalProb, childNode.getCount) :: alarmList
        } else {
          children = children + (extracted -> childNode)
          childNode.update(e, s, o, childLocalProb, thisGlobalProb)  // This reflects a choice to throw away all sub-child alerts which occur as a result of the parent alert
          // Begin reporting information about the child:
          Some(List((extracted, childLocalProb, thisGlobalProb * childLocalProb, children("_?_").getCount /*childNode.getCount*/ )))
        }
//        }
    }
  } else None

  def getTreeRepr(yourDepth: Int, key: String, yourProbability: Float, parentGlobalProb: Float): TreeRepr =
    TreeRepr(yourDepth, key, yourProbability, yourProbability * parentGlobalProb, getCount,
      if (children.size > 1) children.toSet[(ExtractedValue, PpmTree)].map{ case (k,v) => v.getTreeRepr(yourDepth + 1, k, localChildProbability(k), yourProbability * parentGlobalProb)} else Set.empty
    )

  override def equals(obj: scala.Any) = obj.isInstanceOf[SymbolNode] && {
    val o = obj.asInstanceOf[SymbolNode]
    o.filter == filter && o.discriminators == discriminators && o.getCount == getCount && o.children == children
  }
}


class QNode(siblings: => Map[ExtractedValue, PpmTree]) extends PpmTree {
  def getCount = siblings.size - 1
  def update(e: Event, s: Subject, o: Object, yourProb: Float, parentGlobalProb: Float): Option[Alarm] = Some(Nil)
  def getTreeRepr(yourDepth: Int, key: String, yourProbability: Float, parentGlobalProb: Float): TreeRepr =
    TreeRepr(yourDepth, key, yourProbability, yourProbability * parentGlobalProb, getCount, Set.empty)
  override def equals(obj: scala.Any) = obj.isInstanceOf[QNode] && obj.asInstanceOf[QNode].getCount == getCount
}





class NoveltyActor extends Actor with ActorLogging {
  import NoveltyDetection._

  val f = (e: Event, s: Subject, o: Object) => (e.eventType == EVENT_READ || e.eventType == EVENT_WRITE) //&& s._2.map(_.path).intersect(Set("bounce","master","local","qmgr")).isEmpty
  val ds = List(
    (e: Event, s: Subject, o: Object) => s._2.map(_.path).getOrElse("<no_path_node>"), //.toList.sorted.mkString("[", " | ", "]"),
    (e: Event, s: Subject, o: Object) => o._2.map(_.path).getOrElse("<no_path_node>") //.suffixFold.toList.sorted //.mkString("[", " | ", "]")
  )

  val repr = Some(TreeRepr.readFromFile("/Users/ryan/Desktop/ppm_experiments/cadets-bovia_processFileTouches.csv"))

  val processFileTouches = PpmTree(f, ds, repr)
  println(processFileTouches.getTreeRepr().toString)

//  val processesThatReadFiles = PpmTree((e,s,o) => e.eventType == EVENT_READ, ds.reverse)
//  val filesExecutedByProcesses = PpmTree((e,s,o) => e.eventType == EVENT_EXECUTE, ds)


  def flatten(e: Event, s: AdmSubject, subPathNodes: Set[AdmPathNode], o: ADM, objPathNodes: Set[AdmPathNode]): Set[(Event, Subject, Object)] = {
    val subjects: Set[(AdmSubject, Option[AdmPathNode])] = if (subPathNodes.isEmpty) Set(s -> None) else subPathNodes.map(p => s -> Some(p))
    val objects: Set[(ADM, Option[AdmPathNode])] = if (objPathNodes.isEmpty) Set(o -> None) else objPathNodes.map(p => o -> Some(p))
    val combined = subjects.flatMap(sub => objects.map(obj => (e, sub, obj)))
    combined
  }


  def receive = {
    case (e: Event, Some(s: AdmSubject), subPathNodes: Set[AdmPathNode], Some(o: ADM), objPathNodes: Set[AdmPathNode]) =>
      val flatEvents = flatten(e, s, subPathNodes, o, objPathNodes)

      val processFileTouchAlarms: Set[Alarm] = flatEvents.map((processFileTouches.update _).tupled).flatten
      processFileTouchAlarms.foreach(println)

//      processesThatReadFiles.update(e, s -> subPathNodes, o -> objPathNodes)
//      filesExecutedByProcesses.update(e, s -> subPathNodes, o -> objPathNodes)
      sender() ! Ack

    case InitMsg =>
      sender() ! Ack
    case CompleteMsg =>
      println(processFileTouches.getTreeRepr().toString)
//      println(processFileTouches.getTreeRepr().leafNodes().filter(_._1.last == "_?_").sortBy(t => 1F - t._2).mkString("\n"))
//      println("")
//      println(processesThatReadFiles.getTreeRepr().leafNodes().filter(_._1.last == "_?_").sortBy(t => 1F - t._2).mkString("\n"))
//      println("")
//      println(filesExecutedByProcesses.getTreeRepr().leafNodes().filter(_._1.last == "_?_").sortBy(t => 1F - t._2).mkString("\n"))


//      processFileTouches.getTreeRepr().writeToFile("/Users/ryan/Desktop/ppm_experiments/cadets-bovia_processFileTouches.csv")
      

//      println(s"EQUALS: ${processFileTouches.getTreeRepr() == TreeRepr.readFromFile("/Users/ryan/Desktop/foo2.csv")}")
      println("Done")

    case x => log.error(s"Received Unknown Message: $x")
  }
}




case class TreeRepr(depth: Int, key: ExtractedValue, localProb: Float, globalProb: Float, count: Int, children: Set[TreeRepr]) extends Serializable {
  override def toString = {
    val indent = (0 until (4 * depth)).map(_ => " ").mkString("")
    val depthString = if (depth < 10) s" $depth" else depth.toString
    val localProbString = localProb.toString + (0 until (13 - localProb.toString.length)).map(_ => " ").mkString("")
    val globalProbString = globalProb.toString + (0 until (13 - globalProb.toString.length)).map(_ => " ").mkString("")
    val countString = (0 until (9 - count.toString.length)).map(_ => " ").mkString("") + count.toString
    s"$indent### Depth: $depthString  Local Prob: $localProbString  Global Prob: $globalProbString  Counter: $countString  Key: $key" +
      children.toList.sortBy(r => 1F - r.localProb).par.map(_.toString).mkString("\n", "", "")
  }

//  def toString[A](sortFunc: TreeRepr => A)(implicit ordering: Ordering[A]) = ???

  def leafNodes(nameAcc: List[ExtractedValue] = Nil): List[(List[ExtractedValue], Float, Float, Int)] =
    children.toList.flatMap {
      case TreeRepr(_, nextKey, lp, gp, cnt, c) if c.isEmpty => List((nameAcc ++ List(key, nextKey), lp, gp, cnt))
      case next => next.leafNodes(nameAcc :+ key)
    }

  def toFlat: List[(Int, ExtractedValue, Float, Float, Int)] = (depth, key, localProb, globalProb, count) :: children.toList.flatMap(_.toFlat)

  def writeToFile(filePath: String): Unit = {
    val pw = new PrintWriter(new File(filePath))
    this.toFlat.foreach(f => pw.write(TreeRepr.flatToCsvString(f)+"\n"))
    pw.close()
  }
}
case object TreeRepr {
  def fromFlat(repr: List[(Int,ExtractedValue,Float,Float,Int)]): TreeRepr = {
    def fromFlatRecursive(repr: List[(Int,ExtractedValue,Float,Float,Int)], atDepth: Int, accAtThisDepth: List[TreeRepr]): (Set[TreeRepr], List[(Int,ExtractedValue,Float,Float,Int)]) = {
      val thisDepthList = repr.takeWhile(_._1 == atDepth).map(l => TreeRepr(l._1, l._2, l._3, l._4, l._5, Set.empty))
      val remainder = repr.drop(thisDepthList.size)
      val nextDepth = remainder.headOption.map(_._1)
      nextDepth match {
        case None =>  // this is the last item in the list
          (accAtThisDepth ++ thisDepthList).toSet -> remainder
        case Some(d) if d == atDepth => // resuming after the decent case
          fromFlatRecursive(remainder, atDepth, accAtThisDepth ++ thisDepthList)
        case Some(d) if d < atDepth  => // returning to parent case
          (thisDepthList.dropRight(1) ++ thisDepthList.lastOption.map(_.copy(children = accAtThisDepth.toSet))).toSet -> remainder
        case Some(d) if d > atDepth  => // descending into the child case
          val (childSet, nextRemainder) = fromFlatRecursive(remainder, atDepth + 1, List.empty)
          val updatedThisDepthList = accAtThisDepth ++ thisDepthList.dropRight(1) :+ thisDepthList.last.copy(children = childSet)
          fromFlatRecursive(nextRemainder, atDepth, updatedThisDepthList)
      }
    }
    fromFlatRecursive(repr, 0, List.empty)._1.head
  }

  def flatToCsvString(t: (Int, ExtractedValue, Float, Float, Int)): String = s"${t._1},${t._2},${t._3},${t._4},${t._5}"
  def csvStringToFlat(s: String): (Int, ExtractedValue, Float, Float, Int) = {
    val a = s.split(",")
    (a(0).toInt, a(1), a(2).toFloat, a(3).toFloat, a(4).toInt)
  }

  def readFromFile(filePath: String): TreeRepr = TreeRepr.fromFlat(scala.io.Source.fromFile(filePath).getLines().map(TreeRepr.csvStringToFlat).toList)
}