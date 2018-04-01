package com.galois.adapt

import akka.actor.{Actor, ActorLogging}
import spray.json.DefaultJsonProtocol._
import spray.json._
import com.univocity.parsers.csv.{CsvParser, CsvParserSettings, CsvWriter, CsvWriterSettings}
import com.galois.adapt.NoveltyDetection._
import com.galois.adapt.adm._
import com.galois.adapt.cdm18.{EVENT_CHANGE_PRINCIPAL, EVENT_EXECUTE, EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG, EVENT_SENDMSG, EVENT_SENDTO, EVENT_UNLINK, EVENT_WRITE, EventType, MEMORY_SRCSINK, PSEUDO_EVENT_PARENT_SUBJECT}
import java.io.{File, PrintWriter}
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.{Files, StandardOpenOption}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}

import com.galois.adapt.adm.EntityResolution.CDM

import scala.collection.JavaConverters._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.collection.mutable
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global


object NoveltyDetection {
  type Event = AdmEvent
  type Subject = (AdmSubject, Option[AdmPathNode])
  type Object = (ADM, Option[AdmPathNode])

  type DataShape = (Event, Subject, Object)

  type ExtractedValue = String
  type Discriminator[DataShape] = DataShape => List[ExtractedValue]
  type Filter[DataShape] = DataShape => Boolean

  type Alarm = List[(String, Float, Float, Int)]


  val writeTypes = Set[EventType](EVENT_WRITE, EVENT_SENDMSG, EVENT_SENDTO)
  val readTypes = Set[EventType](EVENT_READ, EVENT_RECVMSG, EVENT_RECVFROM)
  val readAndWriteTypes = readTypes ++ writeTypes
  val execDeleteTypes = Set[EventType](EVENT_EXECUTE, EVENT_UNLINK)
  val march1Nanos = 1519862400000000L
  val (pathDelimiterRegexPattern, pathDelimiterChar) = Application.ta1 match {
    case "faros" | "fivedirections" => ("""\\""", """\""")
    case _                          => ("""/""" ,   "/")
  }
  val sudoOrPowershellComparison: String => Boolean = Application.ta1 match {
    case "faros" | "fivedirections" => (s: String) => s.contains("powershell")
    case _                          => (s: String) => s == "sudo"
  }


  var cdmSanityTrees = List(
    PpmDefinition[CDM]("CDM-Event",
      d => d.isInstanceOf[cdm18.Event],
      List(
        d => List(d.asInstanceOf[cdm18.Event].eventType.toString),
        d => List({
          val e = d.asInstanceOf[cdm18.Event]
          e match {
            case cdm18.Event(_,_,_,_,_,subUuid,timestampNanos, po1, pop1, po2, pop2, name,_,_,_,programPoint,_) =>
              val tests = List(subUuid.isDefined, timestampNanos > march1Nanos, po1.isDefined, pop1.isDefined, po2.isDefined, pop2.isDefined, name.isDefined, programPoint.isDefined)
              tests.mkString(",")
          }
        })
      ),
      d => Set(CdmUUID(d.asInstanceOf[cdm18.Event].uuid, Application.ta1))
    ),
    PpmDefinition[CDM]("CDM-Subject",
      d => d.isInstanceOf[cdm18.Subject],
      List(
        d => List(d.asInstanceOf[cdm18.Subject].subjectType.toString),
        d => List({
          val e = d.asInstanceOf[cdm18.Subject]
          e match {
            case cdm18.Subject(_, _, _, _, startTimestampNanos, parentSubject, _, unitId, _, _, cmdLine, privilegeLevel, importedLibraries, exportedLibraries, _) =>
              val tests = List(startTimestampNanos > march1Nanos, parentSubject.isDefined, unitId.isDefined, cmdLine.isDefined, privilegeLevel.isDefined, importedLibraries.isDefined, exportedLibraries.isDefined)
              tests.mkString(",")
          }
        })
      ),
      d => Set(CdmUUID(d.asInstanceOf[cdm18.Subject].uuid, Application.ta1))
    ),
    PpmDefinition[CDM]("CDM-Netflow",
      d => d.isInstanceOf[cdm18.NetFlowObject],
      List(
        d => List({
          val e = d.asInstanceOf[cdm18.NetFlowObject]
          e match {
            case cdm18.NetFlowObject(_, _, localAddress, localPort, remoteAddress, remotePort, ipProtocol, fileDescriptor) =>
              val tests = List(localAddress != "", localPort != -1, remoteAddress != "", remotePort != -1, ipProtocol.isDefined, fileDescriptor.isDefined)
              tests.mkString(",")
          }
        })
      ),
      d => Set(CdmUUID(d.asInstanceOf[cdm18.NetFlowObject].uuid, Application.ta1))
    ),
    PpmDefinition[CDM]("CDM-FileObject",
      d => d.isInstanceOf[cdm18.FileObject],
      List(
        d => List(d.asInstanceOf[cdm18.FileObject].fileObjectType.toString),
        d => List({
          val e = d.asInstanceOf[cdm18.FileObject]
          e match {
            case cdm18.FileObject(_, _, _, fileDescriptor, localPrincipal, size, peInfo, hashes) =>
              val tests = List(fileDescriptor.isDefined, localPrincipal.isDefined, size.isDefined, peInfo.isDefined, hashes.isDefined)
              tests.mkString(",")
          }
        })
      ),
      d => Set(CdmUUID(d.asInstanceOf[cdm18.FileObject].uuid, Application.ta1))
    )
  ).par


  val iforestTrees = List(
    PpmDefinition[(Event,AdmSubject,Set[AdmPathNode])]("iForestProcessEventType",
      d => d._3.nonEmpty,
      List(
        d => List(d._3.map(_.path).toList.sorted.mkString("-"),d._2.uuid.uuid.toString),
        d => List(d._1.eventType.toString)
      ),
      d => Set(d._2.uuid)
    ),
    PpmDefinition[(Event,AdmSubject,Set[AdmPathNode])]("iForestCommonAlarms",
      d => d._3.nonEmpty,
      List(
        d => List(d._3.map(_.path).toList.sorted.mkString("-"),d._2.uuid.uuid.toString),
        d => List(d._1.eventType.toString)
      ),
      d => Set(d._2.uuid)
    ),
    PpmDefinition[(Event,AdmSubject,Set[AdmPathNode])]("iForestUncommonAlarms",
      d => d._3.nonEmpty,
      List(
        d => List(d._3.map(_.path).toList.sorted.mkString("-"),d._2.uuid.uuid.toString),
        d => List(d._1.eventType.toString)
      ),
      d => Set(d._2.uuid)
    )
  ).par


  val esoTrees = List(
    PpmDefinition[DataShape]( "ProcessFileTouches",
      d => readAndWriteTypes.contains(d._1.eventType),
      List(
        d => List(d._2._2.map(_.path).getOrElse("<no_subject_path_node>")),
        d => List(d._3._2.map(_.path).getOrElse("<no_file_path_node>"))
      ),
      d => Set(d._1.uuid, d._2._1.uuid, d._3._1.uuid)
    ),
    PpmDefinition[DataShape]( "FilesTouchedByProcesses",
      d => readAndWriteTypes.contains(d._1.eventType),
      List(
        d => List(d._3._2.map(_.path).getOrElse("<no_file_path_node>")),
        d => List(d._2._2.map(_.path).getOrElse("<no_subject_path_node>"))
      ),
      d => Set(d._1.uuid, d._2._1.uuid, d._3._1.uuid)
    ),
    PpmDefinition[DataShape]( "FilesExecutedByProcesses",
      d => d._1.eventType == EVENT_EXECUTE,
      List(
        d => List(d._2._2.map(_.path).getOrElse("<no_subject_path_node>")),
        d => List(d._3._2.map(_.path).getOrElse("<no_file_path_node>"))
      ),
      d => Set(d._1.uuid, d._2._1.uuid, d._3._1.uuid)
    ),
    PpmDefinition[DataShape]( "ProcessesWithNetworkActivity",
      d => d._3._1.isInstanceOf[AdmNetFlowObject],
      List(
        d => List(d._2._2.map(_.path).getOrElse("<no_subject_path_node>")),
        d => {
          val nf = d._3._1.asInstanceOf[AdmNetFlowObject]
          List(nf.remoteAddress, nf.remotePort.toString)
        }
      ),
      d => Set(d._1.uuid, d._2._1.uuid, d._3._1.uuid)
    ),
    PpmDefinition[DataShape]( "ProcessDirectoryReadWriteTouches",
      d => d._3._1.isInstanceOf[AdmFileObject] && d._3._2.isDefined && readAndWriteTypes.contains(d._1.eventType),
      List(
        d => List(d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)),  // Process name or UUID
        d => d._3._2.map { _.path.split(pathDelimiterRegexPattern, -1).toList match {
          case "" :: remainder => pathDelimiterChar :: remainder
          case x => x
        }}.getOrElse(List("<no_file_path_node>")).dropRight(1)
      ),
      d => Set(d._1.uuid, d._2._1.uuid, d._3._1.uuid)
    ),
    PpmDefinition[DataShape]( "ProcessesChangingPrincipal",
      d => d._1.eventType == EVENT_CHANGE_PRINCIPAL,
      List(
        d => List(d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)),  // Process name or UUID
        d => List(d._3._2.map(_.path + s" : ${d._3._1.getClass.getSimpleName}").getOrElse( s"${d._3._1.uuid.rendered} : ${d._3._1.getClass.getSimpleName}"))
      ),
      d => Set(d._1.uuid, d._2._1.uuid, d._3._1.uuid)
    ),
    PpmDefinition[DataShape]( "SudoIsAsSudoDoes",
      d => d._2._2.exists(p => sudoOrPowershellComparison(p.path)),
      List(
        d => List(d._1.eventType.toString),
        d => List(d._3._2.map(_.path).getOrElse(d._3._1.uuid.rendered) + " : " + d._3._1.getClass.getSimpleName)
      ),
      d => Set(d._1.uuid, d._2._1.uuid, d._3._1.uuid)
    ),
    PpmDefinition[DataShape]( "ParentChildProcesses",
      d => d._1.eventType == PSEUDO_EVENT_PARENT_SUBJECT && d._2._2.isDefined && d._3._2.isDefined,
      List(d => List(
        d._3._2.get.path,  // Parent process first
        d._2._2.get.path   // Child process second
      )),
      d => Set(d._2._1.uuid, d._2._2.get.uuid, d._3._1.uuid, d._3._2.get.uuid)
    )
  ).par


  val seoesTrees = List(
    new PpmDefinition[DataShape]("FileExecuteDelete",
      d => d._3._1.isInstanceOf[AdmFileObject],
      List(
        d => List(
          d._3._2.map(_.path).getOrElse(d._3._1.uuid.rendered), // File name or UUID
          d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)  // Executing process name or UUID
        ),
        d => List(
          d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)  // Deleting process name or UUID
        )
      ),
      d => Set(d._1.uuid, d._2._1.uuid, d._3._1.uuid) ++ d._2._2.map(_.uuid).toSet[ExtendedUuid] ++ d._3._2.map(_.uuid).toSet[ExtendedUuid]
    ) with PartialPpm[String] {

      def getJoinCondition(observation: DataShape) =
        observation._3._2.map(_.path).orElse(Some(observation._3._1.uuid.rendered))  // File name or UUID

      val partialFilters = (
        (d: DataShape) => d._1.eventType == EVENT_EXECUTE,
        (d: DataShape) => d._1.eventType == EVENT_UNLINK
      )
    },

    new PpmDefinition[DataShape]("FilesWrittenThenExecuted",
      d => d._3._1.isInstanceOf[AdmFileObject],
      List(
        d => List(
          d._3._2.map(_.path).getOrElse(d._3._1.uuid.rendered), // File name or UUID
          d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)  // Writing process name or UUID
        ),
        d => List(
          d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)  // Executing process name or UUID
        )
      ),
      d => Set(d._1.uuid, d._2._1.uuid, d._3._1.uuid) ++ d._2._2.map(_.uuid).toSet[ExtendedUuid] ++ d._3._2.map(_.uuid).toSet[ExtendedUuid]
    ) with PartialPpm[String] {

      def getJoinCondition(observation: DataShape) =
        observation._3._2.map(_.path).orElse(Some(observation._3._1.uuid.rendered))  // File name or UUID

      val partialFilters = (
        (d: DataShape) => writeTypes.contains(d._1.eventType),
        (d: DataShape) => d._1.eventType == EVENT_EXECUTE
      )
    },

    new PpmDefinition[DataShape]("CommunicationPathThroughObject",
      d => readAndWriteTypes.contains(d._1.eventType) ||
        (d._3._1.isInstanceOf[AdmSrcSinkObject] && d._3._1.asInstanceOf[AdmSrcSinkObject].srcSinkType == MEMORY_SRCSINK),  // Any kind of event to a memory object.
      List(
        d => List(
          d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)  // Writing subject name or UUID
        ),
        d => List(
          d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered), // Reading subject name or UUID
          d._3._2.map(_.path).getOrElse(d._3._1.uuid.rendered) + (  // Object name or UUID and type
            d._3._1 match {
              case o: AdmSrcSinkObject => s" : ${o.srcSinkType}"
              case o: AdmFileObject => s" : ${o.fileObjectType}"
              case o: AdmNetFlowObject => s"  NetFlow: ${o.remoteAddress}:${o.remotePort}"
              case _ => ""
            }
          )
        )
      ),
      d => Set(d._1.uuid, d._2._1.uuid, d._3._1.uuid)
    ) with PartialPpm[AdmUUID] {

      def getJoinCondition(observation: DataShape) = Some(observation._3._1.uuid)   // Object UUID

      val partialFilters = (
        (d: DataShape) => writeTypes.contains(d._1.eventType) ||
          (d._3._1.isInstanceOf[AdmSrcSinkObject] && d._3._1.asInstanceOf[AdmSrcSinkObject].srcSinkType == MEMORY_SRCSINK),     // Any kind of event to a memory object.
        (d: DataShape) => (readTypes.contains(d._1.eventType) ||
          (d._3._1.isInstanceOf[AdmSrcSinkObject] && d._3._1.asInstanceOf[AdmSrcSinkObject].srcSinkType == MEMORY_SRCSINK)) &&  // Any kind of event to a memory object.
          (partialMap.contains(getJoinCondition(d).get) && d._2._2.exists(_.path != partialMap(getJoinCondition(d).get).head))  // subject names are distinct
      )
    }
  ).par


  val oeseoTrees = List(
    new PpmDefinition[DataShape]("ProcessWritesFileSoonAfterNetflowRead",
      d => readAndWriteTypes.contains(d._1.eventType),
      List(
        d => {
          val nf = d._3._1.asInstanceOf[AdmNetFlowObject]
          List(s"${nf.remoteAddress}:${nf.remotePort}")
        },
        d => List(
          d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered),
          d._3._2.map(_.path) match {
            case Some("") | None => d._3._1.uuid.rendered
            case Some(s) => s
          }
        )
      ),
      d => Set(d._1.uuid, d._2._1.uuid, d._3._1.uuid)
    ) with PartialPpm[AdmUUID] {

      def getJoinCondition(observation: DataShape) = observation._2._2.map(_.uuid)

      override def arrangeExtracted(extracted: List[ExtractedValue]) = extracted.tail :+ extracted.head

      val partialFilters = (
        (eso: DataShape) => eso._3._1.isInstanceOf[AdmNetFlowObject] && readTypes.contains(eso._1.eventType),
        (eso: DataShape) => eso._3._1.isInstanceOf[AdmFileObject] && writeTypes.contains(eso._1.eventType)
      )
    }
  ).par



//  TODO: consider a(n updatable?) alarm filter for every tree?



  val admPpmTrees = esoTrees ++ seoesTrees ++ oeseoTrees
  val ppmList = cdmSanityTrees ++ admPpmTrees ++ iforestTrees

}


case class PpmDefinition[DataShape](name: String, filter: Filter[DataShape], discriminators: List[Discriminator[DataShape]], uuidCollector: DataShape => Set[ExtendedUuid]) {

  val inputFilePath  = Try(Application.config.getString("adapt.ppm.basedir") + name + Application.config.getString("adapt.ppm.loadfilesuffix") + ".csv").toOption
  val outputFilePath =
    if (Application.config.getBoolean("adapt.ppm.shouldsave"))
      Try(Application.config.getString("adapt.ppm.basedir") + name + Application.config.getString("adapt.ppm.savefilesuffix") + ".csv").toOption
    else None
  val tree = PpmTree(
    if (Application.config.getBoolean("adapt.ppm.shouldload"))
      inputFilePath.flatMap { s =>
        TreeRepr.readFromFile(s).map{ t => println(s"Reading tree $name in from file: $s"); t}
          .orElse { println(s"Loading no data for tree: $name"); None }
      }
    else { println(s"Loading no data for tree: $name"); None }
  )

  val inputAlarmFilePath  = Try(Application.config.getString("adapt.ppm.basedir") + name + Application.config.getString("adapt.ppm.loadfilesuffix") + "_alarm.json").toOption
  val outputAlarmFilePath =
    if (Application.config.getBoolean("adapt.ppm.shouldsave"))
      Try(Application.config.getString("adapt.ppm.basedir") + name + Application.config.getString("adapt.ppm.savefilesuffix") + "_alarm.json").toOption
    else None
  var alarms: Map[List[ExtractedValue], (Long, Alarm, Set[ExtendedUuid], Map[String, Int])] =
    if (Application.config.getBoolean("adapt.ppm.shouldload"))
      inputAlarmFilePath.flatMap { fp =>
        Try {
          import spray.json._
          import ApiJsonProtocol._

          val content = new String(Files.readAllBytes(new File(fp).toPath()), StandardCharsets.UTF_8)
          content.parseJson.convertTo[List[(List[ExtractedValue], (Long, Alarm, Set[ExtendedUuid], Map[String, Int]))]].toMap
        }.toOption orElse  {
          println(s"Failed to load alarms for tree: $name")
          None
        }
      }.getOrElse(Map.empty[List[ExtractedValue], (Long, Alarm, Set[ExtendedUuid], Map[String, Int])])
    else {
      println(s"Loading no alarms for tree: $name")
      Map.empty
    }

  def observe(observation: DataShape): Option[Alarm] = if (filter(observation))
    tree.observe(PpmTree.prepareObservation[DataShape](observation, discriminators)) else None
  def recordAlarm(alarmOpt: Option[(Alarm, Set[ExtendedUuid])]): Unit = alarmOpt.foreach(a => alarms = alarms + (a._1.map(_._1) -> (System.currentTimeMillis, a._1, a._2, Map.empty[String,Int])))
  def setAlarmRating(key: List[ExtractedValue], rating: Option[Int], namespace: String): Boolean = alarms.get(key).fold(false) { a =>
    rating match {
      case Some(number) => // set the alarm rating in this namespace
        alarms = alarms + (key -> a.copy (_4 = a._4 + (namespace -> number) ) ); true
      case None => // Unset the alarm rating.
        alarms = alarms + (key -> a.copy (_4 = a._4 - namespace) ); true
    }
  }
  def getAllCounts: Map[List[ExtractedValue], Int] = tree.getAllCounts()


  val saveEveryAndNoMoreThan = Try(Application.config.getLong("adapt.ppm.saveintervalseconds")).getOrElse(0L)
  val lastSaveCompleteMillis = new AtomicLong(0L)
  val currentlySaving = new AtomicBoolean(false)

  def saveStateAsync(): Unit = {
    val now = System.currentTimeMillis
    val expectedSaveCostMillis = 1000  // Allow repeated saving in subsequent attempts if total save time took no longer than this time.
    if ( ! currentlySaving.get() && lastSaveCompleteMillis.get() + saveEveryAndNoMoreThan - expectedSaveCostMillis <= now ) {
      currentlySaving.set(true)
      Future {
        val repr = //time(s"get tree REPR for $name with count: ${tree.getCount}")(
          tree.getTreeRepr(key = name)
//        )
        outputFilePath.foreach(p =>
//          time(s"write repr to disk for $name")(
            repr.writeToFile(p)
//          )
        )
        outputAlarmFilePath.foreach((fp: String) => {
          import spray.json._
          import ApiJsonProtocol._

          val content = alarms.toList.toJson.prettyPrint
          val outputFile = new File(fp)
          if ( ! outputFile.exists) outputFile.createNewFile()
          Files.write(outputFile.toPath, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING)
        })
        lastSaveCompleteMillis.set(System.currentTimeMillis)
        currentlySaving.set(false)
      }
    }
  }
  def prettyString: String = tree.getTreeRepr(key = name).toString
}

trait PartialPpm[JoinType] { myself: PpmDefinition[DataShape] =>
  type PartialShape = DataShape
  val discriminators: List[Discriminator[PartialShape]]
  require(discriminators.length == 2)
  var partialMap = mutable.Map.empty[JoinType, List[ExtractedValue]] //, Set[ExtendedUuid])]

  def getJoinCondition(observation: PartialShape): Option[JoinType]
  def arrangeExtracted(extracted: List[ExtractedValue]): List[ExtractedValue] = extracted
  val partialFilters: Tuple2[PartialShape => Boolean, PartialShape => Boolean]

  override def observe(observation: PartialShape): Option[Alarm] = if (filter(observation)) {
    getJoinCondition(observation).flatMap { joinValue =>
      partialMap.get(joinValue) match {
        case None =>
          if (partialFilters._1(observation)) {
            val extractedList = discriminators(0)(observation)
            if (extractedList.nonEmpty) partialMap(joinValue) = extractedList //-> myself.uuidCollector(observation)
          }
          None
        case Some(firstExtracted) =>
          if (partialFilters._2(observation)) {
            val newlyExtracted = discriminators(1)(observation)
            if (newlyExtracted.nonEmpty) tree.observe(arrangeExtracted(firstExtracted ++ newlyExtracted))
            else None
          }
          else None
      }
    }
  } else None
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
  def prepareObservation[DataShape](data: DataShape, ds: List[Discriminator[DataShape]]): List[ExtractedValue] = ds.flatMap(_.apply(data))
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
      case Nil if counter == 1 => Some(Nil)  // Start an alarm if the end is novel.
      case Nil => None
      case extracted :: remainder =>
        val childLocalProb = localChildProbability(extracted)
        val thisGlobalProb = thisLocalProb * parentGlobalProb
        val childNode = children.getOrElse(extracted, new SymbolNode())
        if ( ! children.contains(extracted)) children = children + (extracted -> childNode)
        childNode.observe(remainder, childLocalProb, thisGlobalProb).map {
          // Begin reporting information about the child:
          case Nil => List((extracted, childLocalProb, thisGlobalProb * childLocalProb, children("_?_").getCount - 1))  // Subtract 1 because we just added the child
          // Append information about the child:
          case alarmList => (extracted, childLocalProb, thisGlobalProb * childLocalProb, childNode.getCount) :: alarmList
        }
    }
  }

  def getTreeRepr(yourDepth: Int, key: String, yourProbability: Float, parentGlobalProb: Float): TreeRepr =
    TreeRepr(yourDepth, key, yourProbability, yourProbability * parentGlobalProb, getCount,
      if (children.-("_?_").nonEmpty) children.toSet[(ExtractedValue, PpmTree)].map {
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
  NoveltyDetection.ppmList.foreach(_ => ())  // Reference the DelayedInit of this object just to instantiate before stream processing begins

  def saveIforestModel(): Unit = {
    val iForestTree = iforestTrees.find(_.name == "iForestProcessEventType")
    iForestTree match {
      case Some(tree) => EventTypeModels.EventTypeData.writeToFile(tree.getAllCounts, EventTypeModels.modelDirIForest + "train_iforest.csv")
      case None => println("ProcessEventType tree for iForest is not defined.")
    }
  }

  override def postStop(): Unit = {
    if (Application.config.getBoolean("adapt.ppm.shouldsave")) {
      ppmList.foreach(_.saveStateAsync())
      saveIforestModel()
    }
    super.postStop()
  }

  def ppm(name: String): Option[PpmDefinition[_]] = ppmList.find(_.name == name)

  def receive = {
    case ListPpmTrees => sender() ! PpmTreeNames(ppmList.map(_.name).toList)

    case msg @ (e: Event, s: AdmSubject, subPathNodes: Set[_], o: ADM, objPathNodes: Set[_]) =>

      def flatten(e: Event, s: AdmSubject, subPathNodes: Set[AdmPathNode], o: ADM, objPathNodes: Set[AdmPathNode]): Set[(Event, Subject, Object)] = {
        val subjects: Set[(AdmSubject, Option[AdmPathNode])] = if (subPathNodes.isEmpty) Set(s -> None) else subPathNodes.map(p => s -> Some(p))
        val objects: Set[(ADM, Option[AdmPathNode])] = if (objPathNodes.isEmpty) Set(o -> None) else objPathNodes.map(p => o -> Some(p))
        subjects.flatMap(sub => objects.map(obj => (e, sub, obj)))
      }

      val f = Try(
        flatten(e, s, subPathNodes.asInstanceOf[Set[AdmPathNode]], o, objPathNodes.asInstanceOf[Set[AdmPathNode]])
      ) match {
        case Success(flatEvents) =>
          Future {
//            if (flatEvents.size > 20) log.warning(s"path node SIZES: ${flatEvents.size}")
            admPpmTrees.foreach(ppm =>
              flatEvents.foreach(e =>
                ppm.recordAlarm(ppm.observe(e).map(o => o -> ppm.uuidCollector(e)))
              )
            )
          }
        case Failure(err) =>
          log.warning(s"Cast Failed. Could not process/match message as types (Set[AdmPathNode] and Set[AdmPathNode]) due to erasure: $msg  Message: ${err.getMessage}")
          Future.successful(())
      }

      Try(
        (e, s, subPathNodes.asInstanceOf[Set[AdmPathNode]])
      ) match {
        case Success(t) =>
//          Future(
            iforestTrees.find(_.name == "iForestProcessEventType").foreach(p => p.observe(t))
//          )
        case Failure(err) => log.warning(s"Cast Failed. Could not process/match message as types (Set[AdmPathNode] and Set[AdmPathNode]) due to erasure: $msg  Message: ${err.getMessage}")
      }
      Try(
        Await.result(f, 2 seconds)
      ).failed.map(e => log.warning(s"Writing batch trees failed: ${e.getMessage}"))

      sender() ! Ack


    case (_, cdm: CDM) =>
      cdmSanityTrees.foreach( ppm =>
        ppm.recordAlarm(ppm.observe(cdm).map(o => o -> ppm.uuidCollector(cdm)))
      )
      sender() ! Ack


    case PpmTreeAlarmQuery(treeName, queryPath, namespace) =>
      val resultOpt = ppm(treeName).map( tree =>
        if (queryPath.isEmpty) tree.alarms.values.map(a => a.copy(_4 = a._4.get(namespace))).toList
        else tree.alarms.collect{ case (k,v) if k.startsWith(queryPath) => v.copy(_4 = v._4.get(namespace))}.toList
      )
      sender() ! PpmTreeAlarmResult(resultOpt)

    case PpmTreeCountQuery(treeName) =>
      sender() ! PpmTreeCountResult(ppm(treeName).map(tree => tree.getAllCounts))

    case SetPpmRatings(treeName, keys, rating, namespace) =>
      sender() ! ppm(treeName).map(tree => keys.map(key => tree.setAlarmRating(key, rating match {case 0 => None; case x => Some(x)}, namespace)))

    case InitMsg =>  /*Future { EventTypeModels.evaluateModels(context.system)};*/ sender() ! Ack;

    case SaveTrees(shouldConfirm) =>
      ppmList.foreach(_.saveStateAsync())
      saveIforestModel()
      if (shouldConfirm) sender ! Ack

    case CompleteMsg =>
      ppmList.foreach { ppm =>
        ppm.saveStateAsync()
//        println(ppm.prettyString)
//        println(ppm.getAllCounts.toList.sortBy(_._1.mkString("/")).mkString("\n" + ppm.name + ":\n", "\n", "\n\n"))
      }
      saveIforestModel()
      println("Done")

    case x =>
      log.error(s"PPM Actor received Unknown Message: $x")
      sender() ! Ack
  }
}

case object ListPpmTrees
case class PpmTreeNames(names: List[String])
case class PpmTreeAlarmQuery(treeName: String, queryPath: List[ExtractedValue], namespace: String)
case class PpmTreeAlarmResult(results: Option[List[(Long, Alarm, Set[ExtendedUuid], Option[Int])]]) {
  def toUiTree: List[UiTreeElement] = results.map { l =>
    l.foldLeft(Set.empty[UiTreeElement]){ (a, b) =>
      val names = b._2.map(_._1)
      val someUiData = UiDataContainer(b._4, names.mkString("∫"), b._1, b._2.last._2, b._3.map(_.rendered))
      UiTreeElement(names, someUiData).map(_.merge(a)).getOrElse(a)
    }.toList
  }.getOrElse(List.empty).sortBy(_.title)
}
case class SetPpmRatings(treeName: String, key: List[List[String]], rating: Int, namespace: String)

case class PpmTreeCountQuery(treeName: String)
case class PpmTreeCountResult(results: Option[Map[List[ExtractedValue], Int]])
case class SaveTrees(shouldConfirm: Boolean = false)


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
    case node: UiTreeNode => Set(this, node)
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

case class UiDataContainer(rating: Option[Int], key: String, observationTime: Long, localProb: Float, uuids: Set[String])
case object UiDataContainer { def empty = UiDataContainer(None, "", 0L, 1F, Set.empty) }


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

  def readFromFile(filePath: String): Option[TreeRepr] = Try {
    val fileHandle = new File(filePath)
    val parser = new CsvParser(new CsvParserSettings)
    val rows: List[Array[String]] = parser.parseAll(fileHandle).asScala.toList
    TreeRepr.fromFlat(rows.map(TreeRepr.csvArrayToFlat))
  }.toOption
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

  def dirFilter(e: NoveltyDetection.Event, s: NoveltyDetection.Subject, o: NoveltyDetection.Object): Boolean = {
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
