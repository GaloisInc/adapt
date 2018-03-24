package com.galois.adapt

import akka.actor.{Actor, ActorLogging}
import spray.json.DefaultJsonProtocol._
import spray.json._
import com.univocity.parsers.csv.{CsvParser, CsvParserSettings, CsvWriter, CsvWriterSettings}
import com.galois.adapt.NoveltyDetection._
import com.galois.adapt.adm._
import com.galois.adapt.cdm18.{EVENT_EXECUTE, EVENT_READ, EVENT_WRITE}
import java.io.{File, PrintWriter}
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}


object NoveltyDetection {
  type Event = AdmEvent
  type Subject = (AdmSubject, Option[AdmPathNode])
  type Object = (ADM, Option[AdmPathNode])

  type ExtractedValue = String
  type Discriminator = (Event, Subject, Object) => List[ExtractedValue]
  type Filter = (Event, Subject, Object) => Boolean

  type Alarm = List[(String, Float, Float, Int)]

  val ppmList = List(
    PpmDefinition( "ProcessFileTouches",
      (e: Event, s: Subject, o: Object) => e.eventType == EVENT_READ || e.eventType == EVENT_WRITE,
      List(
        (e: Event, s: Subject, o: Object) => List(s._2.map(_.path).getOrElse("<no_path_node>")),
        (e: Event, s: Subject, o: Object) => List(o._2.map(_.path).getOrElse("<no_path_node>"))
      )
    ),
    PpmDefinition( "FilesTouchedByProcesses",
      (e: Event, s: Subject, o: Object) => e.eventType == EVENT_READ || e.eventType == EVENT_WRITE,
      List(
        (e: Event, s: Subject, o: Object) => List(o._2.map(_.path).getOrElse("<no_path_node>")),
        (e: Event, s: Subject, o: Object) => List(s._2.map(_.path).getOrElse("<no_path_node>"))
      )
    ),
    PpmDefinition( "FilesExecutedByProcesses",
      (e: Event, s: Subject, o: Object) => e.eventType == EVENT_EXECUTE,
      List(
        (e: Event, s: Subject, o: Object) => List(s._2.map(_.path).getOrElse("<no_path_node>")),
        (e: Event, s: Subject, o: Object) => List(o._2.map(_.path).getOrElse("<no_path_node>"))
      )
    ),
    PpmDefinition("ProcessesWithNetworkActivity",
      (e: Event, s: Subject, o: Object) => o._1.isInstanceOf[AdmNetFlowObject],
      List(
        (e: Event, s: Subject, o: Object) => List(s._2.map(_.path).getOrElse("<no_path_node>"))
      )
    ),
//    PpmDefinition("DirectoryStructure",
//      (e: Event, s: Subject, o: Object) => o._1.isInstanceOf[AdmFileObject],
//      List(
//        (e: Event, s: Subject, o: Object) => o._2.map(_.path.split("/").toList).getOrElse(Nil)
//      )
//    ),
    PpmDefinition( "ProcessDirectoryTouchesV1",
      (e: Event, s: Subject, o: Object) => e.eventType == EVENT_READ || e.eventType == EVENT_WRITE,
      List(
        (e: Event, s: Subject, o: Object) => List(s._2.map(_.path).getOrElse("<no_path_node>")),
        (e: Event, s: Subject, o: Object) => o._2.map { _.path.split("/").toList match {
          case "" :: remainder => "/" :: remainder
          case x => x
        }
        }.getOrElse(Nil).dropRight(1)
      )
    ),
    PpmDefinition("ProcessDirectoryTouches",
      (e: Event, s: Subject, o: Object) => ProcessDirectoryTouchesAux.dirFilter(e, s, o),
      List(
        (e: Event, s: Subject, o: Object) => List(s._2.map(_.path).getOrElse("<no_path_node>")),
        (e: Event, s: Subject, o: Object) => List(ProcessDirectoryTouchesAux.dirAtDepth(s._2.get.path,o._2.get.path))
      )
    )
  )
}


case class PpmDefinition(name: String, filter: Filter, discriminators: List[Discriminator]) {
  val inputFilePath  = Try(Application.config.getString("adapt.ppm.basedir") + Application.config.getString(s"adapt.ppm.$name.loadfile")).toOption
  val outputFilePath = Try(Application.config.getString("adapt.ppm.basedir") + Application.config.getString(s"adapt.ppm.$name.savefile")).toOption
  val tree = PpmTree(inputFilePath.map{println(s"Reading tree in from file: $inputFilePath"); TreeRepr.readFromFile})
  var alarms: Map[List[ExtractedValue], (Long, Alarm, Map[String, Int])] = Map.empty

  def observe(observation: (Event, Subject, Object)): Option[Alarm] = if (filter(observation._1, observation._2, observation._3))
    tree.observe(PpmTree.prepareObservation(observation._1, observation._2, observation._3, discriminators)) else None
  def recordAlarm(alarmOpt: Option[Alarm]): Unit = alarmOpt.foreach(a => alarms = alarms + (a.map(_._1) -> (System.currentTimeMillis, a, Map.empty[String,Int])))
  def setAlarmRating(key: List[ExtractedValue], rating: Option[Int], namespace: String): Boolean = alarms.get(key).map { a =>
    if (rating.isDefined) // set the alarm rating in this namespace
      alarms = alarms + (key -> a.copy(_3 = a._3 + (namespace -> rating.get)))
    else // Unset the alarm rating.
      alarms = alarms + (key -> a.copy(_3 = a._3 - namespace))
    true
  }.getOrElse(false)
  def getAllCounts: Map[List[ExtractedValue], Int] = tree.getAllCounts()

  def saveState(): Unit = outputFilePath.foreach(tree.getTreeRepr(key = name).writeToFile)
  def prettyString: String = tree.getTreeRepr(key = name).toString
}


trait PpmTree {
  def getCount: Int
  def observe(ds: List[ExtractedValue], thisLocalProb: Float = 1F, parentGlobalProb: Float = 1F): Option[Alarm]
  def getTreeRepr(yourDepth: Int = 0, key: String = "", yourProbability: Float = 1F, parentGlobalProb: Float = 1F): TreeRepr
  var children: Map[ExtractedValue, PpmTree]
  override def equals(obj: scala.Any) = obj.isInstanceOf[PpmTree] && {
    val o = obj.asInstanceOf[PpmTree]
    o.getCount == getCount && o.children == children
  }
  def getAllCounts(accumulatedKey: List[ExtractedValue] = Nil): Map[List[ExtractedValue], Int]
}
case object PpmTree {
  def apply(serialized: Option[TreeRepr] = None): PpmTree = new SymbolNode(serialized)
  def prepareObservation(e: Event, s: Subject, o: Object, ds: List[Discriminator]): List[ExtractedValue] = ds.flatMap(_.apply(e, s, o))
}


class SymbolNode(repr: Option[TreeRepr] = None) extends PpmTree {
  private var counter = 0
  def getCount: Int = counter

  var children = Map.empty[ExtractedValue, PpmTree]

  repr.fold[Unit] {
    children = children + ("_?_" -> new QNode(children))
  } { thisTree =>
    counter = thisTree.count
    children = thisTree.children.map {
      case c if c.key == "_?_" => c.key -> new QNode(children)
      case c => c.key -> new SymbolNode(Some(c))
    }.toMap + ("_?_" -> new QNode(children)) // Ensure a ? node is always present, even if it isn't in the loaded data.
  }

  def totalChildCounts = children.values.map(_.getCount).sum

  def localChildProbability(identifier: ExtractedValue): Float =
    children.getOrElse(identifier, children("_?_")).getCount.toFloat / totalChildCounts

  def observe(extractedValues: List[ExtractedValue], thisLocalProb: Float = 1F, parentGlobalProb: Float = 1F) = {
    counter += 1
    extractedValues match {
      case Nil => None
      case extracted :: remainder =>
        val childExists = children.contains(extracted)
        val childNode = children.getOrElse(extracted, new SymbolNode())
        val childLocalProb = localChildProbability(extracted)
        val thisGlobalProb = thisLocalProb * parentGlobalProb
        if (childExists) childNode.observe(remainder, childLocalProb, thisGlobalProb).map { alarmList =>
          // Append information about the child:
          (extracted, childLocalProb, thisGlobalProb * childLocalProb, childNode.getCount) :: alarmList
        } else {
          children = children + (extracted -> childNode)
          childNode.observe(remainder, childLocalProb, thisGlobalProb)  // This reflects a choice to throw away all sub-child alerts which occur as a result of the parent alert
          // Begin reporting information about the child:
          Some(List((extracted, childLocalProb, thisGlobalProb * childLocalProb, children("_?_").getCount)))
        }
    }
  }

  def getTreeRepr(yourDepth: Int, key: String, yourProbability: Float, parentGlobalProb: Float): TreeRepr =
    TreeRepr(yourDepth, key, yourProbability, yourProbability * parentGlobalProb, getCount,
      if (children.size > 1) children.toSet[(ExtractedValue, PpmTree)].map{
        case (k,v) => v.getTreeRepr(yourDepth + 1, k, localChildProbability(k), yourProbability * parentGlobalProb)
      } else Set.empty
    )

  def getAllCounts(accumulatedKey: List[ExtractedValue]): Map[List[ExtractedValue], Int] =
    children.foldLeft(Map(accumulatedKey -> getCount)){
      case (acc, (childKey, child)) => acc ++ child.getAllCounts(accumulatedKey :+ childKey)
    }
}


class QNode(siblings: => Map[ExtractedValue, PpmTree]) extends PpmTree {
  def getCount = siblings.size - 1
  var children = Map.empty[ExtractedValue, PpmTree]
  def observe(extractedValues: List[ExtractedValue], thisLocalProb: Float = 1F, parentGlobalProb: Float = 1F): Option[Alarm] = Some(Nil)
  def getTreeRepr(yourDepth: Int, key: String, yourProbability: Float, parentGlobalProb: Float): TreeRepr =
    TreeRepr(yourDepth, key, yourProbability, yourProbability * parentGlobalProb, getCount, Set.empty)
  override def equals(obj: scala.Any) = obj.isInstanceOf[QNode] && obj.asInstanceOf[QNode].getCount == getCount

  def getAllCounts(accumulatedKey: List[ExtractedValue]): Map[List[ExtractedValue], Int] = if (getCount > 0) Map(accumulatedKey -> getCount) else Map.empty
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

    case PpmTreeAlarmQuery(treeName, queryPath, namespace) =>
      val resultOpt = ppm(treeName).map(tree =>
        if (queryPath.isEmpty) tree.alarms.values.map(a => a.copy(_3 = a._3.get(namespace))).toList
        else tree.alarms.collect{ case (k,v) if k.startsWith(queryPath) => v.copy(_3 = v._3.get(namespace))}.toList
      )
      sender() ! PpmTreeAlarmResult(resultOpt)

    case PpmTreeCountQuery(treeName) =>
      sender() ! PpmTreeCountResult(ppm(treeName).map(tree => tree.getAllCounts))

    case SetPpmRating(treeName, key, rating, namespace) =>
      sender() ! ppm(treeName).map(tree => tree.setAlarmRating(key, rating match {case 0 => None; case x => Some(x)}, namespace))

    case InitMsg => sender() ! Ack

    case CompleteMsg =>
      ppmList.foreach { ppm =>
        ppm.saveState()
//        println(ppm.prettyString)
        println(ppm.getAllCounts.toList.sortBy(_._1.mkString("/")).mkString("\n" + ppm.name, "\n", "\n\n"))
      }
      println("Done")

    case x =>
      log.error(s"Received Unknown Message: $x")
      sender() ! Ack
  }
}

case object ListPpmTrees
case class PpmTreeNames(names: List[String])
case class PpmTreeAlarmQuery(treeName: String, queryPath: List[ExtractedValue], namespace: String)
case class PpmTreeAlarmResult(results: Option[List[(Long, Alarm, Option[Int])]]) {
  def toUiTree: List[UiTreeElement] = results.map { l =>
    l.foldLeft(Set.empty[UiTreeElement]){ (a, b) =>
      val names = b._2.map(_._1)
      val someUiData = UiDataContainer(b._3)
      UiTreeElement(names, someUiData).map(_.merge(a)).getOrElse(a)
    }.toList
  }.getOrElse(List.empty).sortBy(_.title)
}
case class SetPpmRating(treeName: String, key: List[String], rating: Int, namespace: String)

case class PpmTreeCountQuery(treeName: String)
case class PpmTreeCountResult(results: Option[Map[List[ExtractedValue], Int]])



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
    val settings = new CsvWriterSettings
    val pw = new PrintWriter(new File(filePath))
    val writer = new CsvWriter(pw,settings)
    this.toFlat.foreach(f => writer.writeRow(TreeRepr.flatToCsvArray(f)))
    writer.close()
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

  def flatToCsvArray(t: (Int, ExtractedValue, Float, Float, Int)): Array[String] = Array(t._1.toString,t._2,t._3.toString,t._4.toString,t._5.toString)
  def csvArrayToFlat(a: Array[String]): (Int, ExtractedValue, Float, Float, Int) = {
    (a(0).toInt, a(1), a(2).toFloat, a(3).toFloat, a(4).toInt)
  }

  def readFromFile(filePath: String): TreeRepr = {
    val fileHandle = new File(filePath)
    val parser = new CsvParser(new CsvParserSettings)
    val rows: List[Array[String]] = parser.parseAll(fileHandle).asScala.toList
    TreeRepr.fromFlat(rows.map(TreeRepr.csvArrayToFlat))
  }
}

case object ProcessDirectoryTouchesAux {
  def readJsonFile(filePath: String): Map[String,Int] = {
    case class ProcessDepth(name: String, depth: Int)
    implicit val ProcessToDepthFormat = jsonFormat2(ProcessDepth)

    val bufferedSource = scala.io.Source.fromFile(filePath)
    val jsonString = bufferedSource.getLines.mkString
    bufferedSource.close()

    val processDepthList = jsonString.parseJson.asJsObject.getFields("processes") match {
      case Seq(processes) => processes.convertTo[List[ProcessDepth]]
      case x => deserializationError("Do not understand how to deserialize " + x)
    }
    processDepthList.map(x => x.name -> x.depth).toMap
  }

  val auxFilePath: Option[String] = Try(Application.config.getString("adapt.ppm.basedir") +
    Application.config.getString(s"adapt.ppm.ProcessDirectoryTouches.processdirectorytouchesauxfile")).toOption
  val processToDepth: Map[String, Int] = auxFilePath.map(readJsonFile).getOrElse(Map.empty[String, Int])

  def dirFilter(e: Event, s: Subject, o: Object): Boolean = {
    o._2.map(_.path).isDefined && o._2.map(_.path).get.length > 1 && // file path must exist and have length greater than 1
      Set('/', '\\').contains(o._2.map(_.path).get.toString.head) && // file path must be absolute (or as close as we can get to forcing that)
      s._2.map(_.path).isDefined && s._2.map(_.path).get.length > 0 // process name must exist and be a non-empty string
  }

  def dirAtDepth(process: String, path: String): String = {
    val sepChar = if (path.head.toString=="/") "/" else "\\\\"
    val depth = processToDepth.getOrElse(process, -1) // default depth is directory file is contained in
    depth match {
      case -1 => path.split(sepChar).init.mkString("", sepChar, sepChar)
      case _ if path.count(_ == sepChar) < depth => path.split(sepChar).init.mkString("", sepChar, sepChar)
      case _ => path.take(depth).mkString("", sepChar, sepChar)
    }
  }
}
