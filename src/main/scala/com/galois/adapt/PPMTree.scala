package com.galois.adapt

import akka.actor.{Actor, ActorLogging}
import com.galois.adapt.NoveltyDetection._
import com.galois.adapt.adm._
import com.galois.adapt.cdm18.{EVENT_EXECUTE, EVENT_READ, EVENT_WRITE}
import java.io.{File, PrintWriter}
import scala.util.{Failure, Success, Try}


object NoveltyDetection {
  type Event = AdmEvent
  type Subject = (AdmSubject, Option[AdmPathNode])
  type Object = (ADM, Option[AdmPathNode])

  type ExtractedValue = String
  type Discriminator = (Event, Subject, Object) => ExtractedValue
  type Filter = (Event, Subject, Object) => Boolean

  type Alarm = List[(String, Float, Float, Int)]

  val ppmList = List(
    PpmDefinition( "ProcessFileTouches",
      (e: Event, s: Subject, o: Object) => e.eventType == EVENT_READ || e.eventType == EVENT_WRITE,
      List(
        (e: Event, s: Subject, o: Object) => s._2.map(_.path).getOrElse("<no_path_node>"),
        (e: Event, s: Subject, o: Object) => o._2.map(_.path).getOrElse("<no_path_node>")
      )
    ),
    PpmDefinition( "FilesTouchedByProcesses",
      (e: Event, s: Subject, o: Object) => e.eventType == EVENT_READ || e.eventType == EVENT_WRITE,
      List(
        (e: Event, s: Subject, o: Object) => o._2.map(_.path).getOrElse("<no_path_node>"),
        (e: Event, s: Subject, o: Object) => s._2.map(_.path).getOrElse("<no_path_node>")
      )
    ),
    PpmDefinition( "FilesExecutedByProcesses",
      (e: Event, s: Subject, o: Object) => e.eventType == EVENT_EXECUTE,
      List(
        (e: Event, s: Subject, o: Object) => s._2.map(_.path).getOrElse("<no_path_node>"),
        (e: Event, s: Subject, o: Object) => o._2.map(_.path).getOrElse("<no_path_node>")
      )
    ),
    PpmDefinition("ProcessesWithNetworkActivity",
      (e: Event, s: Subject, o: Object) => o._1.isInstanceOf[AdmNetFlowObject],
      List(
        (e: Event, s: Subject, o: Object) => s._2.map(_.path).getOrElse("<no_path_node>")
      )
    )
  )
}


case class PpmDefinition(name: String, filter: Filter, discriminators: List[Discriminator]) {
  val inputFilePath  = Try(Application.config.getString("adapt.ppm.basedir") + Application.config.getString(s"adapt.ppm.$name.loadfile")).toOption
  val outputFilePath = Try(Application.config.getString("adapt.ppm.basedir") + Application.config.getString(s"adapt.ppm.$name.savefile")).toOption
  val tree = PpmTree(filter, discriminators, inputFilePath.map{println(s"Reading tree in from file: $inputFilePath"); TreeRepr.readFromFile})
  var alarms: Map[List[ExtractedValue], (Long, Alarm, Map[String, Int])] = Map.empty

  def observe(observation: (Event, Subject, Object)): Option[Alarm] = (tree.update _).tupled(observation)
  def recordAlarm(alarmOpt: Option[Alarm]): Unit = alarmOpt.foreach(a => alarms = alarms + (a.map(_._1) -> (System.currentTimeMillis, a, Map.empty[String,Int])))
  def setAlarmRating(key: List[ExtractedValue], rating: Option[Int], namespace: String): Boolean = alarms.get(key).map { a =>
    if (rating.isDefined) // set the alarm rating in this namespace
      alarms = alarms + (key -> a.copy(_3 = a._3 + (namespace -> rating.get)))
    else // Unset the alarm rating.
      alarms = alarms + (key -> a.copy(_3 = a._3 - namespace))
    true
  }.getOrElse(false)

  def saveState(): Unit = outputFilePath.foreach(tree.getTreeRepr(key = name).writeToFile)
  def prettyString: String = tree.getTreeRepr(key = name).toString
}


trait PpmTree {
  def getCount: Int
  def update(e: Event, s: Subject, o: Object): Option[Alarm] = this.update(e, s, o, 1F, 1F)
  def update(e: Event, s: Subject, o: Object, yourProb: Float = 1F, parentGlobalProb: Float = 1F): Option[Alarm]
  def getTreeRepr(yourDepth: Int = 0, key: String = "", yourProbability: Float = 1F, parentGlobalProb: Float = 1F): TreeRepr
}
case object PpmTree {
  def apply(filter: Filter, discriminators: List[Discriminator], serialized: Option[TreeRepr] = None): PpmTree = new SymbolNode(filter, discriminators, serialized)
}


class SymbolNode(val filter: Filter, val discriminators: List[Discriminator], repr: Option[TreeRepr] = None) extends PpmTree {
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


class PpmActor extends Actor with ActorLogging {
  import NoveltyDetection._

  def ppm(name: String): Option[PpmDefinition] = ppmList.find(_.name == name)

  def receive = {
    case ListPpmTrees => sender() ! PpmTreeNames(ppmList.map(_.name))

    case msg @ (e: Event, Some(s: AdmSubject), subPathNodes: Set[_], Some(o: ADM), objPathNodes: Set[_]) =>
      def flatten(e: Event, s: AdmSubject, subPathNodes: Set[AdmPathNode], o: ADM, objPathNodes: Set[AdmPathNode]): Set[(Event, Subject, Object)] = {
        val subjects: Set[(AdmSubject, Option[AdmPathNode])] = if (subPathNodes.isEmpty) Set(s -> None) else subPathNodes.map(p => s -> Some(p))
        val objects: Set[(ADM, Option[AdmPathNode])] = if (objPathNodes.isEmpty) Set(o -> None) else objPathNodes.map(p => o -> Some(p))
        subjects.flatMap(sub => objects.map(obj => (e, sub, obj)))
      }
      Try (
        flatten(e, s, subPathNodes.asInstanceOf[Set[AdmPathNode]], o, objPathNodes.asInstanceOf[Set[AdmPathNode]])
      ) match {
        case Success(flatEvents) => ppmList.foreach (ppm => flatEvents.foreach (e => ppm.recordAlarm (ppm.observe (e) ) ) )
        case Failure(err) => log.warning(s"Could not process/match message as types (Set[AdmPathNode] and Set[AdmPathNode]) due to erasure: $msg  Message: ${err.getMessage}")
      }
      sender() ! Ack

    case PpmTreeQuery(treeName, queryPath, namespace) =>
      val resultOpt = ppm(treeName).map(tree =>
        if (queryPath.isEmpty) tree.alarms.values.map(a => a.copy(_3 = a._3.get(namespace))).toList
        else tree.alarms.collect{ case (k,v) if k.startsWith(queryPath) => v.copy(_3 = v._3.get(namespace))}.toList
      )
      sender() ! PpmTreeResult(resultOpt)

    case SetPpmRating(treeName, key, rating, namespace) =>
      sender() ! ppm(treeName).map(tree => tree.setAlarmRating(key, rating match {case 0 => None; case x => Some(x)}, namespace))

    case InitMsg => sender() ! Ack

    case CompleteMsg =>
      ppmList.foreach { ppm =>
        ppm.saveState()
//        println(ppm.prettyString)
      }
      println("Done")

    case x =>
      log.error(s"Received Unknown Message: $x")
      sender() ! Ack
  }
}

case object ListPpmTrees
case class PpmTreeNames(names: List[String])
case class PpmTreeQuery(treeName: String, queryPath: List[ExtractedValue], namespace: String)
case class PpmTreeResult(results: Option[List[(Long, Alarm, Option[Int])]]) {
  def toUiTree: List[UiTreeElement] = results.map { l =>
    l.foldLeft(Set.empty[UiTreeElement]){ (a, b) =>
      val names = b._2.map(_._1)

      val someUiData = UiDataContainer(b._3)

      UiTreeElement(names, someUiData).map(_.merge(a)).getOrElse(a)
    }.toList
  }.getOrElse(List.empty).sortBy(_.title)
}
case class SetPpmRating(treeName: String, key: List[String], rating: Int, namespace: String)



sealed trait UiTreeElement{
  val title: String
  val data: UiDataContainer
  def merge(other: UiTreeElement): Set[UiTreeElement]
  def merge(others: Set[UiTreeElement]): Set[UiTreeElement] =
    others.find(o => o.isInstanceOf[UiTreeFolder] && o.title == this.title) match {
      case Some(existing) => others - existing ++ existing.merge(this)
      case None => if (others contains this) others else others + this
    }
}
case object UiTreeElement {
  def apply(names: List[ExtractedValue], data: UiDataContainer): Option[UiTreeElement] = names.foldRight[Option[UiTreeElement]](None){
    case (extracted, None) => Some(UiTreeNode(extracted, data))
    case (extracted, Some(n: UiTreeNode)) => Some(UiTreeFolder(extracted, children = Set(n)))
    case (extracted, Some(f: UiTreeFolder)) => Some(UiTreeFolder(extracted, children = Set(f)))
  }
}

case class UiTreeNode(title: String, data: UiDataContainer) extends UiTreeElement {
  def merge(other: UiTreeElement) = other match {
    case o: UiTreeFolder => o merge this
    case o: UiTreeElement => if (this.title == o.title) Set(this) else Set(this, o)
  }
}

case class UiTreeFolder(title: String, folder: Boolean = true, data: UiDataContainer = UiDataContainer.empty, children: Set[UiTreeElement] = Set.empty) extends UiTreeElement {
  def merge(other: UiTreeElement): Set[UiTreeElement] = other match {
    case node: UiTreeNode =>
      Set(this, node)
    case newFolder: UiTreeFolder =>
      if (newFolder.title == title) {
        // merge children into this child set.
        val newChildren = newFolder.children.foldLeft(children)((existing, newOne) =>
          existing.find(e => e.isInstanceOf[UiTreeFolder] && e.title == newOne.title) match {
            case Some(folderMatch) => existing - folderMatch ++ folderMatch.merge(newOne)
            case None => if (existing.exists(_.title == newOne.title)) existing else existing + newOne
          }
        )
        Set(this.copy(children = newChildren))
      } else Set(this, newFolder)
  }
}

case class UiDataContainer(rating: Option[Int])
case object UiDataContainer { def empty = UiDataContainer(None) }


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