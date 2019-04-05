package com.galois.adapt

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props}
import spray.json._
import com.univocity.parsers.csv.{CsvParser, CsvParserSettings, CsvWriter, CsvWriterSettings}
import com.galois.adapt.NoveltyDetection._
import com.galois.adapt.adm._
import com.galois.adapt.cdm19._
import java.io.{BufferedWriter, File, FileWriter, PrintWriter}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}

import akka.pattern.ask
import akka.util.Timeout
import com.galois.adapt
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.collection.{SortedMap, mutable}
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import AdaptConfig._
import Application.hostNameForAllHosts
import spray.json._
import ApiJsonProtocol._
import com.rrwright.quine.language.QuineId
import com.rrwright.quine.runtime.GraphService
import com.rrwright.quine.runtime.Novelty

import scala.annotation.tailrec

//type AnAlarm = (List[String], (Long, Long, Alarm, Set[NamespacedUuidDetails], Map[String, Int]))
case class AnAlarm(key:List[String], details:(Set[Long], Long, Alarm, Set[NamespacedUuidDetails], Map[String, Int]))

object NoveltyDetection {
  type Event = AdmEvent
  type Subject = (AdmSubject, Option[AdmPathNode])
  type Object = (ADM, Option[AdmPathNode])

  type DataShape = (Event, Subject, Object)

  type ExtractedValue = String
  type Discriminator[DataShape] = DataShape => List[ExtractedValue]
  type Filter[DataShape] = DataShape => Boolean

  type Alarm = List[PpmTreeNodeAlarm]  // (Key, localProbability, globalProbability, count, siblingPop, parentCount, depthOfLocalProbabilityCalculation)
  case class PpmTreeNodeAlarm(key: String, localProb: Float, globalProb: Float, count: Int, siblingPop: Int, parentCount: Int, depthOfLocalProbabilityCalculation: Int)

  val writeTypes = Set[EventType](EVENT_WRITE, EVENT_SENDMSG, EVENT_SENDTO)
  val readTypes = Set[EventType](EVENT_READ, EVENT_RECVMSG, EVENT_RECVFROM)
  val readAndWriteTypes = readTypes ++ writeTypes
  val netFlowTypes = readAndWriteTypes ++ Set(EVENT_CONNECT, EVENT_ACCEPT)
  val execDeleteTypes = Set[EventType](EVENT_EXECUTE, EVENT_UNLINK)
  val march1Nanos = 1519862400000000L

  case class NamespacedUuidDetails(extendedUuid: AdmUUID, name: Option[String] = None, pid: Option[Int] = None)
}

case object AlarmExclusions {
  val cadets = Set("ld-elf.so.1", "local", "bounce", "pkg", "top", "mlock", "cleanup", "qmgr", "smtpd", "trivial-rewrite")
  val clearscope = Set("system_server", "proc", "com.android.inputmethod.latin", "com.android.camera2", "com.android.launcher3", "com.android.smspush", "com.android.quicksearchbox", "com.android.gallery3d", "android.process.media", "com.android.music")
  val fivedirections = Set("\\windows\\system32\\svchost.exe", "\\program files\\tightvnc\\tvnserver.exe", "mscorsvw.exe")
  val marple= Set()
  val theia = Set("qt-opensource-linux-x64-5.10.1.run", "/usr/lib/postgresql/9.1/bin/postgres", "whoopsie", "Qt5.10.1", "5.10.1", "/bin/dbus-daemon", "/usr/sbin/console-kit-daemon")
  val trace = Set()
  val general = Set("<no_subject_path_node>")
  val allExclusions = cadets ++ clearscope ++ fivedirections ++ marple ++ theia ++ trace ++ general
  def filter(novelty: Novelty[Set[NamespacedUuidDetails]]): Boolean = // true == allow an alarm to be reported.
    ! novelty.probabilityData.exists(level => allExclusions.contains(level._1))
}


case class PpmDefinition[DataShape](
  name: String,
  incomingFilter: Filter[DataShape],
  discriminators: List[Discriminator[DataShape]],
  uuidCollector: DataShape => Set[NamespacedUuidDetails],
  timestampExtractor: DataShape => Set[Long],
  shouldApplyThreshold: Boolean,
  noveltyFilter: Novelty[Set[NamespacedUuidDetails]] => Boolean = AlarmExclusions.filter
)(
  context: ActorContext,
  alarmActor: ActorRef,
  hostName: HostName,
  graphService: GraphService[AdmUUID]
) extends LazyLogging {

  val basePath: String = ppmConfig.basedir + name + "-" + hostName

  val inputAlarmFilePath  = basePath + ppmConfig.loadfilesuffix + "_alarm.json"
  val outputAlarmFilePath = basePath + ppmConfig.savefilesuffix + "_alarm.json"

  val treeRootQid = AdmUuidProvider.ppmTreeRootNodeId(hostName,name)//context.actorOf(Props(classOf[PpmNodeActor], name, alarmActor, startingState), name = name)

  var alarms: Map[List[ExtractedValue], (Set[Long], Long, Alarm, Set[NamespacedUuidDetails], Map[String, Int])] =
    if (ppmConfig.shouldloadalarms)
      Try {
        val content = new String(Files.readAllBytes(new File(inputAlarmFilePath).toPath), StandardCharsets.UTF_8)
        content.parseJson.convertTo[List[(List[ExtractedValue], (Set[Long], Long, Alarm, Set[NamespacedUuidDetails], Map[String, Int]))]].toMap
      } match {
        case Success(alarm) => alarm
        case Failure(e) =>
          println(s"FAILED to load alarms for tree: $name  on host: $hostName. Starting with empty tree state.")
          e.printStackTrace()
          Map.empty[List[ExtractedValue], (Set[Long], Long, Alarm, Set[NamespacedUuidDetails], Map[String, Int])]
      }
    else Map.empty

  var localProbAccumulator: SortedMap[Float,Int] = // If there are more than 2,147,483,647 alarms with a given LP; then need Long.
    if (ppmConfig.shouldloadlocalprobabilitiesfromalarms && shouldApplyThreshold) {
      val noveltyLPs =
        Try {
          val content = new String(Files.readAllBytes(new File(inputAlarmFilePath).toPath), StandardCharsets.UTF_8)
          content.parseJson.convertTo[List[(List[ExtractedValue], (Set[Long], Long, Alarm, Set[NamespacedUuidDetails], Map[String, Int]))]].map(_._2._3.last.localProb).groupBy(identity).mapValues(_.size)
        } match {
          case Success(lps) => lps
          case Failure(e) =>
            println(s"FAILED to load local probability alarms for tree: $name  on host: $hostName. Starting with empty local probability accumulator.")
            e.printStackTrace()
            Map.empty[Float, Int]
        }
      SortedMap.empty(Ordering[Float]) ++ noveltyLPs
    }
    else SortedMap.empty(Ordering[Float])

  var localProbCount: Int = localProbAccumulator.values.sum

  var localProbThreshold: Float = 1

  def insertIntoLocalProbAccumulator(alarmOpt: Option[(Alarm, Set[NamespacedUuidDetails], Set[Long])]): Unit = alarmOpt.foreach {
    case (alarm, _, _) =>
      alarm.lastOption.foreach { alarmNode =>
        val lp = alarmNode.localProb
        if (shouldApplyThreshold) {
          // Since we are only concerned with low lp novelties, we need not track large lps with great precision.
          // This serves to reduce the max size of the lpAccumulator map.
          val approxLp = if (lp >= 0.3) math.round(lp * 10) / 10F else lp
          localProbAccumulator += (approxLp -> (localProbAccumulator.getOrElse(approxLp, 0) + 1))
          localProbCount += 1
        }
      }
  }

  private def updateThreshold(percentile: Float): Unit = {
    val percentileOfTotal = percentile/100 * localProbCount

    var accLPCount = 0

    localProbThreshold = localProbAccumulator.takeWhile {
      case (_, thisLPCount) =>
        accLPCount += thisLPCount
        accLPCount <= percentileOfTotal
    }.lastOption.map(_._1).getOrElse(1F)

    println(s"LP THRESHOLD LOG: $name     $hostName: $localProbCount novelties collected.")
    println(s"LP THRESHOLD LOG: $name     $hostName: $localProbThreshold is current local probability threshold.")

  }

  if (shouldApplyThreshold) {
    val computeAlarmLpThresholdIntervalMinutes = ppmConfig.computethresholdintervalminutes
    val alarmPercentile = ppmConfig.alarmlppercentile
    if (computeAlarmLpThresholdIntervalMinutes > 0) { // Dynamic Threshold
      context.system.scheduler.schedule(computeAlarmLpThresholdIntervalMinutes minutes,
        computeAlarmLpThresholdIntervalMinutes minutes)(updateThreshold(alarmPercentile))
    }
    else updateThreshold(alarmPercentile) // Static Threshold
  }

  def observe(observation: DataShape): Unit = if (incomingFilter(observation)) {
    implicit val timeout = Timeout(6.1 seconds)
    graphService.observe(treeRootQid, name, PpmTree.prepareObservation[DataShape](observation, discriminators), uuidCollector(observation), timestampExtractor(observation), alarmActor)
  }

  //process name and pid/uuid
  def getProcessDetails(setNamespacedUuidDetails: Set[NamespacedUuidDetails]): Set[ProcessDetails] = {
    setNamespacedUuidDetails.filter { d =>
      d.pid.isDefined && d.name.isDefined
    }.map { d =>
      ProcessDetails(d.name.get, d.pid, hostName)
    }
  }

  def recordAlarm(alarmOpt: Option[(Alarm, Set[NamespacedUuidDetails], Set[Long])]): Unit = alarmOpt.foreach {
    case (alarm, setNamespacedUuidDetails, timestamps) =>

    val key: List[ExtractedValue] = alarm.map(_.key)
    if (alarms contains key) adapt.Application.statusActor ! IncrementAlarmDuplicateCount
    else {

      val alarmDetails = (timestamps, System.currentTimeMillis, alarm, setNamespacedUuidDetails, Map.empty[String,Int])
      val newAlarm = AnAlarm(key,alarmDetails)
      alarms = alarms + AnAlarm.unapply(newAlarm).get

      def thresholdAllows: Boolean = alarm.lastOption.forall( (i: PpmTreeNodeAlarm) => ! ((i.localProb > localProbThreshold) && shouldApplyThreshold) )

      val processDetails = getProcessDetails(setNamespacedUuidDetails)
      //report the alarm
      if (thresholdAllows) AlarmReporter.report(name, hostName, newAlarm, processDetails, localProbThreshold)
    }
  }

  def setAlarmRating(key: List[ExtractedValue], rating: Option[Int], namespace: String): Boolean = alarms.get(key).fold(false) { a =>
    rating match {
      case Some(number) => // set the alarm rating in this namespace
        alarms = alarms + (key -> a.copy (_5 = a._5 + (namespace -> number) ) ); true
      case None => // Unset the alarm rating.
        alarms = alarms + (key -> a.copy (_5 = a._5 - namespace) ); true
    }
  }

  val saveEveryAndNoMoreThan = 0L //ppmConfig.saveintervalseconds.getOrElse(0L) * 1000  // convert seconds to milliseconds
  val lastSaveCompleteMillis = new AtomicLong(0L)
  val isCurrentlySaving = new AtomicBoolean(false)

  def getRepr(implicit timeout: Timeout): Future[TreeRepr] = graphService.getTreeRepr(treeRootQid,name,List()).mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity).map{ _.repr }

  def prettyString: Future[String] = {
    implicit val timeout = Timeout(593 seconds)
    graphService.getTreeRepr(treeRootQid,name,List()).mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity).map(_.repr.toString)
  }
}


case object PpmTree {
  def prepareObservation[DataShape](data: DataShape, ds: List[Discriminator[DataShape]]): List[ExtractedValue] = ds.flatMap(_.apply(data))
}

case class PpmNodeActorGetTreeReprResult(repr: TreeRepr)
case class PpmNodeActorBeginGetTreeRepr(treeName: String, startingKey: List[ExtractedValue] = Nil)
case object PpmNodeActorGetTopLevelCount
case class PpmNodeActorGetTopLevelCountResult(count: Int) // We can just query the graph for properties on root node for this

class PpmManager(hostName: HostName, source: String, isWindows: Boolean, graphService: GraphService[AdmUUID]) extends Actor with ActorLogging { thisActor =>

  val (pathDelimiterRegexPattern, pathDelimiterChar) = if (isWindows) ("""\\""", """\""") else ("""/""" ,   "/")
  val sudoOrPowershellComparison: String => Boolean = if (isWindows) {
    _.toLowerCase.contains("powershell")
  } else {
    _.toLowerCase == "sudo"
  }

  import NoveltyDetection._

  lazy val esoTrees = List(
    PpmDefinition[DataShape]( "ProcessFileTouches",
      d => readAndWriteTypes.contains(d._1.eventType),
      List(
        d => List(d._2._2.map(_.path).getOrElse("<no_subject_path_node>")),
        d => List(d._3._2.map(_.path).getOrElse("<no_file_path_node>"))
      ),
      d => Set(NamespacedUuidDetails(d._1.uuid),
               NamespacedUuidDetails(d._2._1.uuid, Some(d._2._2.map(_.path).getOrElse("<no_subject_path_node>")), Some(d._2._1.cid)),
               NamespacedUuidDetails(d._3._1.uuid, Some(d._3._2.map(_.path).getOrElse("<no_file_path_node>")))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      d => Set(d._1.latestTimestampNanos),
      shouldApplyThreshold = true
    )(thisActor.context, context.self, hostName, graphService),

    PpmDefinition[DataShape]( "FilesTouchedByProcesses",
      d => readAndWriteTypes.contains(d._1.eventType),
      List(
        d => List(d._3._2.map(_.path).getOrElse("<no_file_path_node>")),
        d => List(d._2._2.map(_.path).getOrElse("<no_subject_path_node>"))
      ),
      d => Set(NamespacedUuidDetails(d._1.uuid),
        NamespacedUuidDetails(d._2._1.uuid, Some(d._2._2.map(_.path).getOrElse("<no_subject_path_node>")), Some(d._2._1.cid)),
        NamespacedUuidDetails(d._3._1.uuid, Some(d._3._2.map(_.path).getOrElse("<no_file_path_node>")))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      d => Set(d._1.latestTimestampNanos),
      shouldApplyThreshold = true
    )(thisActor.context, context.self, hostName, graphService),

    PpmDefinition[DataShape]( "FilesExecutedByProcesses",
      d => d._1.eventType == EVENT_EXECUTE,
      List(
        d => List(d._2._2.map(_.path).getOrElse("<no_subject_path_node>")),
        d => List(d._3._2.map(_.path).getOrElse("<no_file_path_node>"))
      ),
      d => Set(NamespacedUuidDetails(d._1.uuid),
        NamespacedUuidDetails(d._2._1.uuid, Some(d._2._2.map(_.path).getOrElse("<no_subject_path_node>")), Some(d._2._1.cid)),
        NamespacedUuidDetails(d._3._1.uuid, Some(d._3._2.map(_.path).getOrElse("<no_file_path_node>")))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      d => Set(d._1.latestTimestampNanos),
      shouldApplyThreshold = false
    )(thisActor.context, context.self, hostName, graphService),

    PpmDefinition[DataShape]( "ProcessesWithNetworkActivity",
      d => d._3._1.isInstanceOf[AdmNetFlowObject],
      List(
        d => List(d._2._2.map(_.path).getOrElse("<no_subject_path_node>")),
        d => {
          val nf = d._3._1.asInstanceOf[AdmNetFlowObject]
          List(nf.remoteAddress.getOrElse("NULL_value_from_CDM"), nf.remotePort.toString)
        }
      ),
      d => Set(NamespacedUuidDetails(d._1.uuid),
        NamespacedUuidDetails(d._2._1.uuid, Some(d._2._2.map(_.path).getOrElse("<no_subject_path_node>")), Some(d._2._1.cid)),
        NamespacedUuidDetails(d._3._1.uuid, Some(d._3._1.asInstanceOf[AdmNetFlowObject].remoteAddress.getOrElse("no_address_from_CDM")))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      d => Set(d._1.latestTimestampNanos),
      shouldApplyThreshold = true
    )(thisActor.context, context.self, hostName, graphService),

    PpmDefinition[DataShape]( "ProcessDirectoryReadWriteTouches",
      d => d._3._1.isInstanceOf[AdmFileObject] && d._3._2.isDefined && readAndWriteTypes.contains(d._1.eventType),
      List(
        d => List(d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)),  // Process name or UUID
        d => d._3._2.map { _.path.split(pathDelimiterRegexPattern, -1).toList match {
          case "" :: remainder => pathDelimiterChar :: remainder
          case x => x
        }}.getOrElse(List("<no_file_path_node>")).dropRight(1)
      ),
      d => Set(NamespacedUuidDetails(d._1.uuid),
        NamespacedUuidDetails(d._2._1.uuid, Some(d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)), Some(d._2._1.cid)),
        NamespacedUuidDetails(d._3._1.uuid, Some(d._3._2.map(_.path).getOrElse("<no_file_path_node>")))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      d => Set(d._1.latestTimestampNanos),
      shouldApplyThreshold = true
    )(thisActor.context, context.self, hostName, graphService),

    PpmDefinition[DataShape]( "ProcessesChangingPrincipal",
      d => d._1.eventType == EVENT_CHANGE_PRINCIPAL,
      List(
        d => List(d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)),  // Process name or UUID
        d => List(d._3._2.map(_.path + s" : ${d._3._1.getClass.getSimpleName}").getOrElse( s"${d._3._1.uuid.rendered} : ${d._3._1.getClass.getSimpleName}"))
      ),
      d => Set(NamespacedUuidDetails(d._1.uuid, Some(d._1.eventType.toString)),
        NamespacedUuidDetails(d._2._1.uuid, Some(d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)), Some(d._2._1.cid)),
        NamespacedUuidDetails(d._3._1.uuid, Some(d._3._2.map(_.path + s" : ${d._3._1.getClass.getSimpleName}").getOrElse( s"${d._3._1.uuid.rendered} : ${d._3._1.getClass.getSimpleName}")))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      d => Set(d._1.latestTimestampNanos),
      shouldApplyThreshold = false
    )(thisActor.context, context.self, hostName, graphService),

    PpmDefinition[DataShape]( "SudoIsAsSudoDoes",
      d => d._2._2.exists(p => sudoOrPowershellComparison(p.path)),
      List(
        d => List(d._1.eventType.toString),
        d => List(d._3._2.map(_.path).getOrElse(d._3._1.uuid.rendered) + " : " + d._3._1.getClass.getSimpleName)
      ),
      d => Set(NamespacedUuidDetails(d._1.uuid, Some(d._1.eventType.toString)),
        NamespacedUuidDetails(d._2._1.uuid),
        NamespacedUuidDetails(d._3._1.uuid, Some(d._3._2.map(_.path).getOrElse(d._3._1.uuid.rendered) + " : " + d._3._1.getClass.getSimpleName))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      d => Set(d._1.latestTimestampNanos),
      shouldApplyThreshold = true
    )(thisActor.context, context.self, hostName, graphService),

    PpmDefinition[DataShape]( "ParentChildProcesses",
      d => d._1.eventType == PSEUDO_EVENT_PARENT_SUBJECT && d._2._2.isDefined && d._3._2.isDefined,
      List(d => List(
        d._3._2.map(_.path).getOrElse("<no_path>"),  // Parent process first
        d._2._2.map(_.path).getOrElse("<no_path>")   // Child process second
      )),
      d => Set(NamespacedUuidDetails(d._1.uuid),
        NamespacedUuidDetails(d._2._1.uuid, Some(d._2._2.get.path), Some(d._2._1.cid)),
        NamespacedUuidDetails(d._3._1.uuid, Some(d._3._2.get.path), d._3._1 match {
                  case o: AdmSubject => Some(o.cid)
                  case _ => None
                })) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      d => Set(d._1.latestTimestampNanos),
      shouldApplyThreshold = false
    )(thisActor.context, context.self, hostName, graphService),

    PpmDefinition[DataShape]("SummarizedProcessActivity",
      d => d._2._1.subjectTypes.contains(SUBJECT_PROCESS), // is a process
      List(d => List(                // 1.) Process name
          d._2._2.map(_.path).getOrElse("{{{unnamed_process}}}"), //es_should_have_been_filtered_out"),
          d._2._1.cid.toString,      // 2.) PID, to disambiguate process instances. (collisions are assumed to be ignorably unlikely)
          d._1.eventType.toString    // 3.) Event type
        ), _._3 match {              // 4.) identifier(s) for the object, based on its type
          case (adm: AdmFileObject, pathOpt) => pathOpt.map(_.path.split(pathDelimiterRegexPattern, -1).toList match {
            case "" :: remainder => pathDelimiterChar :: remainder
            case x => x
          }).getOrElse(List(s"${adm.fileObjectType}:${adm.uuid.rendered}"))
          case (adm: AdmSubject, pathOpt) => List(pathOpt.map(_.path).getOrElse(s"{${adm.subjectTypes.toList.map(_.toString).sorted.mkString(",")}}:${adm.cid}"))
          case (adm: AdmSrcSinkObject, _) => List(s"${adm.srcSinkType}:${adm.uuid.rendered}")
          case (adm: AdmNetFlowObject, _) => List(s"${adm.remoteAddress}:${adm.remotePort}")
          case (adm, pathOpt) => List(s"UnhandledType:$adm:$pathOpt")
        }
      ),
      d => Set(NamespacedUuidDetails(d._1.uuid),
        NamespacedUuidDetails(d._2._1.uuid, d._2._2.map(_.path)),
        NamespacedUuidDetails(d._3._1.uuid, d._3._2.map(_.path))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      d => Set(d._1.latestTimestampNanos),
      shouldApplyThreshold = false,
      noveltyFilter = _ => false
    )(thisActor.context, context.self, hostName, graphService)
  ).par


//  lazy val seoesTrees = List(
//    new PpmDefinition[DataShape]("FileExecuteDelete",
//      d => d._3._1.isInstanceOf[AdmFileObject],
//      List(
//        d => List(
//          d._3._2.map(_.path).getOrElse(d._3._1.uuid.rendered), // File name or UUID
//          d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)  // Executing process name or UUID
//        ),
//        d => List(
//          d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)  // Deleting process name or UUID
//        )
//      ),
//      d => Set(NamespacedUuidDetails(d._1.uuid),
//        NamespacedUuidDetails(d._2._1.uuid,Some(d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)), Some(d._2._1.cid)),
//        NamespacedUuidDetails(d._3._1.uuid,Some(d._3._2.map(_.path).getOrElse(d._3._1.uuid.rendered)))) ++
//        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
//        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
//      d => Set(d._1.latestTimestampNanos),
//      shouldApplyThreshold = false
//    )(thisActor.context, context.self, hostName, graphService) with PartialPpm[String] {
//
//      def getJoinCondition(observation: DataShape) =
//        observation._3._2.map(_.path).orElse(Some(observation._3._1.uuid.rendered))  // File name or UUID
//
//      val partialFilters = (
//        (d: DataShape) => d._1.eventType == EVENT_EXECUTE,
//        (d: DataShape) => d._1.eventType == EVENT_UNLINK
//      )
//    },
//
//    new PpmDefinition[DataShape]("FilesWrittenThenExecuted",
//      d => d._3._1.isInstanceOf[AdmFileObject],
//      List(
//        d => List(
//          d._3._2.map(_.path).getOrElse(d._3._1.uuid.rendered), // File name or UUID
//          d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)  // Writing process name or UUID
//        ),
//        d => List(
//          d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)  // Executing process name or UUID
//        )
//      ),
//      d => Set(NamespacedUuidDetails(d._1.uuid,Some(d._1.eventType.toString)),
//        NamespacedUuidDetails(d._2._1.uuid,Some(d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)), Some(d._2._1.cid)),
//        NamespacedUuidDetails(d._3._1.uuid,Some(d._3._2.map(_.path).getOrElse(d._3._1.uuid.rendered)))) ++
//        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
//        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
//      d => Set(d._1.latestTimestampNanos),
//      shouldApplyThreshold = false
//    )(thisActor.context, context.self, hostName, graphService) with PartialPpm[String] {
//
//      def getJoinCondition(observation: DataShape) =
//        observation._3._2.map(_.path).orElse(Some(observation._3._1.uuid.rendered))  // File name or UUID
//
//      val partialFilters = (
//        (d: DataShape) => writeTypes.contains(d._1.eventType),
//        (d: DataShape) => d._1.eventType == EVENT_EXECUTE
//      )
//    },
//
//    new PpmDefinition[DataShape]("CommunicationPathThroughObject",
//      d => readAndWriteTypes.contains(d._1.eventType) ||
//        (d._3._1.isInstanceOf[AdmSrcSinkObject] && d._3._1.asInstanceOf[AdmSrcSinkObject].srcSinkType == MEMORY_SRCSINK),  // Any kind of event to a memory object.
//      List(
//        d => List(
//          d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)  // Writing subject name or UUID
//        ),
//        d => List(
//          d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered), // Reading subject name or UUID
//          d._3._2.map(_.path).getOrElse(d._3._1.uuid.rendered) + (  // Object name or UUID and type
//            d._3._1 match {
//              case o: AdmSrcSinkObject => s" : ${o.srcSinkType}"
//              case o: AdmFileObject => s" : ${o.fileObjectType}"
//              case o: AdmNetFlowObject => s"  NetFlow: ${o.remoteAddress}:${o.remotePort}"
//              case _ => ""
//            }
//          )
//        )
//      ),
//      d => Set(NamespacedUuidDetails(d._1.uuid),
//        NamespacedUuidDetails(d._2._1.uuid,Some(d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)), Some(d._2._1.cid)),
//        NamespacedUuidDetails(d._3._1.uuid,Some(d._3._2.map(_.path).getOrElse(d._3._1.uuid.rendered) + (  // Object name or UUID and type
//          d._3._1 match {
//            case o: AdmSrcSinkObject => s" : ${o.srcSinkType}"
//            case o: AdmFileObject => s" : ${o.fileObjectType}"
//            case o: AdmNetFlowObject => s"  NetFlow: ${o.remoteAddress}:${o.remotePort}"
//            case _ => ""
//          }
//          )))) ++
//        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
//        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
//      d => Set(d._1.latestTimestampNanos),
//      shouldApplyThreshold = true
//    )(thisActor.context, context.self, hostName, graphService) with PartialPpm[AdmUUID] {
//
//      def getJoinCondition(observation: DataShape) = Some(observation._3._1.uuid)   // Object UUID
//
//      val partialFilters = (
//        (d: DataShape) => writeTypes.contains(d._1.eventType) ||
//          (d._3._1.isInstanceOf[AdmSrcSinkObject] && d._3._1.asInstanceOf[AdmSrcSinkObject].srcSinkType == MEMORY_SRCSINK),     // Any kind of event to a memory object.
//        (d: DataShape) => (readTypes.contains(d._1.eventType) ||
//          (d._3._1.isInstanceOf[AdmSrcSinkObject] && d._3._1.asInstanceOf[AdmSrcSinkObject].srcSinkType == MEMORY_SRCSINK)) &&  // Any kind of event to a memory object.
//          (partialMap.contains(getJoinCondition(d).get) && d._2._2.exists(_.path != partialMap(getJoinCondition(d).get)._1.head))  // subject names are distinct
//      )
//    }
//  ).par


//  lazy val oeseoTrees = List(
//    new PpmDefinition[DataShape]("ProcessWritesFileSoonAfterNetflowRead",
//      d => readAndWriteTypes.contains(d._1.eventType),
//      List(
//        d => {
//          val nf = d._3._1.asInstanceOf[AdmNetFlowObject]
//          List(s"${nf.remoteAddress}:${nf.remotePort}")
//        },
//        d => List(
//          d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered),
//          d._3._2.map(_.path) match {
//            case Some("") | None => d._3._1.uuid.rendered
//            case Some(s) => s
//          }
//        )
//      ),
//      d => Set(NamespacedUuidDetails(d._1.uuid),
//        NamespacedUuidDetails(d._2._1.uuid, Some(d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)), Some(d._2._1.cid)),
//        NamespacedUuidDetails(d._3._1.uuid, Some(d._3._2.map(_.path).getOrElse("AdmNetFlow")))) ++
//        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
//        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
//      d => Set(d._1.latestTimestampNanos),
//      shouldApplyThreshold = true
//    )(thisActor.context, context.self, hostName, graphService) with PartialPpm[AdmUUID] {
//
//      def getJoinCondition(observation: DataShape) = observation._2._2.map(_.uuid)   // TODO: shouldn't this include some time range comparison here?????????????????????????????????????????????????????????????????
//
//      override def arrangeExtracted(extracted: List[ExtractedValue]) = extracted.tail :+ extracted.head
//
//      val partialFilters = (
//        (eso: DataShape) => eso._3._1.isInstanceOf[AdmNetFlowObject] && readTypes.contains(eso._1.eventType),
//        (eso: DataShape) => eso._3._1.isInstanceOf[AdmFileObject] && writeTypes.contains(eso._1.eventType)
//      )
//    }
//  ).par


  sealed trait SendingOrReceiving { def invert: SendingOrReceiving = this match { case Sending => Receiving; case Receiving => Sending } }
  case object Sending extends SendingOrReceiving
  case object Receiving extends SendingOrReceiving
  type LocalAddress = String
  type RemoteAddress = String
  type LocalPort = Int
  type RemotePort = Int
  type NF = (LocalAddress, LocalPort, RemoteAddress, RemotePort)
  case class CrossHostNetObs(sendRec: SendingOrReceiving, localAddress: LocalAddress, localPort: LocalPort, remoteAddress: RemoteAddress, remotePort: RemotePort) {
    def invert = CrossHostNetObs(sendRec.invert, remoteAddress, remotePort, localAddress, localPort)
  }

// /* lazy val crossHostTrees = List(
//
//    new PpmDefinition[DataShape]("CrossHostProcessCommunication",
//      d => netFlowTypes.contains(d._1.eventType) && (d._3._1 match {
//        case AdmNetFlowObject(_,Some(_),Some(_),Some(_),Some(_),_) => true   // Ignore raw sockets. ...and other sockets.
//        case _ => false
//      }),
//      List(
//        d => List(
//          d._2._2.map(_.path).getOrElse("<<unknown_process>>")), // Sending process name
//        d => List(
//          d._2._2.map(_.path).getOrElse("<<unknown_process>>"),          // Receiving process name
//          d._3._1.asInstanceOf[AdmNetFlowObject].localPort.get.toString  // Receiving port number
//        )
//      ),
//      d => Set(
//        NamespacedUuidDetails(d._1.uuid),
//        NamespacedUuidDetails(d._2._1.uuid, d._2._2.map(_.path), Some(d._2._1.cid)),
//        NamespacedUuidDetails(d._3._1.uuid, d._3._2.map(_.path))
//      ) ++ d._2._2.map(n => NamespacedUuidDetails(n.uuid, Some(n.path), None)).toSet,
////        ++ d._3._2.map(n => NamespacedUuidDetails(n.uuid, Some(n.path), None)).toSet,  // Don't need/will never be a path node from a netflow.
//      d => Set(d._1.latestTimestampNanos),
//      shouldApplyThreshold = false
//    )(thisActor.context, context.self, hostName, graphService) with PartialPpm[CrossHostNetObs] {
//
////      implicit def partialMapJson: RootJsonFormat[(Set[String], (List[ExtractedValue], Set[NamespacedUuidDetails]))] = ???
//
//      val netFlowReadTypes = writeTypes.+(EVENT_CONNECT)
//      val netFlowWriteTypes = readTypes.+(EVENT_ACCEPT)
//
//      val partialFilters = (
//        (eso: PartialShape) => netFlowReadTypes.contains(eso._1.eventType),
//        (eso: PartialShape) => netFlowWriteTypes.contains(eso._1.eventType)
//      )
//
//      def getJoinCondition(observation: DataShape): Option[CrossHostNetObs] = Try {
//        val nf = observation._3._1.asInstanceOf[AdmNetFlowObject]
//        CrossHostNetObs(
//          observation match {
//            case t if partialFilters._1(t) => Sending
//            case t if partialFilters._2(t) => Receiving
//            case _ => ???
//          },
//          nf.localAddress.get,
//          nf.localPort.get,
//          nf.remoteAddress.get,
//          nf.remotePort.get
//        )
//      }.toOption
//
//      var partialMap2 = Map.empty[HostName, mutable.Map[CrossHostNetObs, Map[List[ExtractedValue],Set[NamespacedUuidDetails]]]]
//
//      override def observe(observation: PartialShape): Unit = if (incomingFilter(observation)) {
//        getJoinCondition(observation) match {
//          case None => log.error(s"Something unexpected passed the filter for CrossHostProcessCommunication: $observation")
//          case Some(joinValue) =>
//
//            val existingThisHost = partialMap2.getOrElse(observation._1.hostName, mutable.Map.empty[CrossHostNetObs, Map[List[ExtractedValue],Set[NamespacedUuidDetails]]])
//            if ( ! partialMap2.contains(observation._2._1.hostName)) partialMap2 = partialMap2 + (observation._2._1.hostName -> existingThisHost)
//            val existingOtherHosts = (partialMap2 - observation._1.hostName).values
//              .foldLeft[Map[CrossHostNetObs, Map[List[ExtractedValue],Set[NamespacedUuidDetails]]]](Map.empty)(_ ++ _)
//
//            val previousSameDirObservations = existingThisHost.getOrElse(joinValue, Map.empty[List[ExtractedValue], Set[NamespacedUuidDetails]])
//            val observedIds = uuidCollector(observation)
//
//            val sendRecOpt = joinValue.sendRec match {
//              case Sending =>
//                val theseDiscs = discriminators(0)(observation)
//                val theseIds = previousSameDirObservations.get(theseDiscs).fold(observedIds) { previousIds => observedIds ++ previousIds }
//                val thisSendObs = Map(theseDiscs -> theseIds)
//                val allSendObs = previousSameDirObservations ++ thisSendObs
//                existingThisHost(joinValue) = allSendObs  // Update mutable Map in place
//                existingOtherHosts.get(joinValue.invert).map(thisSendObs -> _)
//              case Receiving =>
//                val theseDiscs = discriminators(1)(observation)
//                val theseIds = previousSameDirObservations.get(theseDiscs).fold(observedIds) { previousIds => observedIds ++ previousIds }
//                val thisRecObs = Map(theseDiscs -> theseIds)
//                val allRecObs = previousSameDirObservations ++ thisRecObs
//                existingThisHost(joinValue) = allRecObs  // Update mutable Map in place
//                existingOtherHosts.get(joinValue.invert).map(_ -> thisRecObs)
//            }
//
//            sendRecOpt.foreach { obs =>
//              val combinedObs = for {
//                o1 <- obs._1
//                o2 <- obs._2
//              } yield {
//                o1._1 ++ o2._1 -> (o1._2 ++ o2._2)
//              }
//              combinedObs.foreach { case (extracted, ids) =>
//                tree ! PpmNodeActorBeginObservation(
//                  name,
//                  arrangeExtracted(extracted), // Updated first, because Sending discriminators is first.
//                  ids,
//                  observation._1.latestTimestampNanos,
//                  alarmFilter
//                )
//              }
//            }
//        }
//      }
//    }
//  ).par
//*/


  lazy val admPpmTrees =
    if (hostName == hostNameForAllHosts) Nil // crossHostTrees
    else esoTrees // ++ seoesTrees ++ oeseoTrees

  val ppmList = admPpmTrees

  def ppm(name: String): Option[PpmDefinition[_]] = ppmList.find(_.name == name)

  var didReceiveInit = false
  var didReceiveComplete = false

  def alarmFromProbabilityData(probabilityData: List[(ExtractedValue,Int,Int)]): Alarm = {
    val (_,parentCount,siblingCount) = probabilityData.takeWhile(_._3 > 1).last // First novel node
    val alarmLocalProbability = (siblingCount + 1).toFloat / (parentCount + siblingCount + 1) // Alarm local prob is a function of the first novel node
    val treeObservationCount = probabilityData.head._2
    val lastAlarmListIndex = probabilityData.size - 2

    probabilityData.iterator.sliding(2).zipWithIndex.map {
      case (extractedPair, depth) =>
        val (ev, parentCount, siblingCount) = extractedPair.head
        val (_, count, _) = extractedPair(1)
        if (depth == lastAlarmListIndex)
          PpmTreeNodeAlarm(ev,alarmLocalProbability,count.toFloat/treeObservationCount,count,siblingCount+1,parentCount,depth)
        else
          PpmTreeNodeAlarm(ev,count.toFloat/parentCount.toFloat,count.toFloat/treeObservationCount,count,siblingCount+1,parentCount,depth)
    }.toList

  }

  def receive = {

    case msg @ Novelty(treeName: String, probabilityData: List[(ExtractedValue,Int,Int)], collectedUuids: Set[NamespacedUuidDetails], timestamps: Set[Long]) =>
      ppm(treeName).fold(
        log.warning(s"Could not find tree named: $treeName to record Alarm related to: $probabilityData with UUIDs: $collectedUuids, with dataTimestamps: $timestamps from: $sender")
      ){ tree =>
        val alarmOpt = if (tree.noveltyFilter(msg)) Some((alarmFromProbabilityData(probabilityData), collectedUuids, timestamps)) else None
        tree.recordAlarm(alarmOpt)
        tree.insertIntoLocalProbAccumulator(alarmOpt)
      }

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
            admPpmTrees.foreach(ppm =>
              flatEvents.foreach(e =>
                ppm.observe(e)
              )
            )
          }
        case Failure(err) =>
          log.warning(s"Cast Failed. Could not process/match message as types (Set[AdmPathNode] and Set[AdmPathNode]) due to erasure: $msg  Message: ${err.getMessage}")
          Future.successful(())
      }

      Try(
        Await.result(f, 15 seconds)
      ).failed.map(e => log.warning(s"Writing batch trees failed: ${e.getMessage}"))


    case PpmTreeAlarmQuery(treeName, queryPath, namespace, startAtTime, forwardFromStartTime, resultSizeLimit, excludeRatingBelow) =>
      val resultOpt = ppm(treeName).map( tree =>
        if (queryPath.isEmpty) tree.alarms.values.map(a => a.copy(_5 = a._5.get(namespace))).toList
        else tree.alarms.collect{ case (k,v) if k.startsWith(queryPath) => v.copy(_5 = v._5.get(namespace))}.toList
      ).map { r =>
        val filteredResults = r.filter { case (dataTimestamps, observationMillis, alarm, uuids, ratingOpt) =>
          (if (forwardFromStartTime) dataTimestamps.min >= startAtTime else dataTimestamps.max <= startAtTime) &&
            excludeRatingBelow.forall(test => ratingOpt.forall(given => given >= test))
        }
        val sortedResults = filteredResults.sortBy[Long](i => if (forwardFromStartTime) i._1.min else Long.MaxValue - i._1.max)
        resultSizeLimit.fold(sortedResults)(limit => sortedResults.take(limit))
      }
      sender() ! PpmTreeAlarmResult(resultOpt)

    case SetPpmRatings(treeName, keys, rating, namespace) =>
      sender() ! ppm(treeName).map(tree => keys.map(key => tree.setAlarmRating(key, rating match {case 0 => None; case x => Some(x)}, namespace)))

    case msg @ PpmNodeActorBeginGetTreeRepr(treeName, startingKey) =>
      implicit val timeout = Timeout(30 seconds)
      val reprFut = ppm(treeName)
        .map(d => graphService.getTreeRepr(d.treeRootQid,treeName,startingKey).mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity))
        .getOrElse(Future.failed(new NoSuchElementException(s"No tree found with name $treeName")))
      sender() ! reprFut

    case InitMsg =>
      if ( ! didReceiveInit) {
        didReceiveInit = true
      }
      sender() ! Ack

    case CompleteMsg =>
      if ( ! didReceiveComplete) {
        println(s"PPM Manager with hostName: $hostName is completing the stream.")
        didReceiveComplete = true
      }

    case x =>
      log.error(s"PPM Actor received Unknown Message: $x")
      sender() ! Ack
  }
}

case object ListPpmTrees
case class PpmTreeNames(namesAndCounts: Map[String, Int])
case class PpmTreeAlarmQuery(treeName: String, queryPath: List[ExtractedValue], namespace: String, startAtTime: Long = 0L, forwardFromStartTime: Boolean = true, resultSizeLimit: Option[Int] = None, excludeRatingBelow: Option[Int] = None)
case class PpmTreeAlarmResult(results: Option[List[(Set[Long], Long, Alarm, Set[NamespacedUuidDetails], Option[Int])]]) {
  def toUiTree: List[UiTreeElement] = results.map { l =>
    l.foldLeft(Set.empty[UiTreeElement]){ (a, b) =>
      val names = b._3.map(_.key)
      val someUiData = UiDataContainer(b._5, names.mkString("∫"), b._1.min, b._2, b._3.last.localProb, b._4.map(_.extendedUuid.rendered)) // Only shows min datatime in UI
      UiTreeElement(names, someUiData).map(_.merge(a)).getOrElse(a)
    }.toList
  }.getOrElse(List.empty).sortBy(_.title)
}
case class SetPpmRatings(treeName: String, key: List[List[String]], rating: Int, namespace: String)

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
  def merge(other: UiTreeElement): Set[UiTreeElement] = other match {
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

case class UiDataContainer(rating: Option[Int], key: String, dataTime: Long, observationTime: Long, localProb: Float, uuids: Set[String])
case object UiDataContainer { def empty = UiDataContainer(None, "", 0L, 0L, 1F, Set.empty) }


case class TreeRepr(depth: Int, key: ExtractedValue, localProb: Float, globalProb: Float, count: Int, children: Set[TreeRepr]) extends Serializable {
  def get(keys: ExtractedValue*): Option[TreeRepr] = keys.toList match {
    case Nil => Some(this)
    case x :: Nil => this.children.find(_.key == x)
    case x :: xs => this.children.find(_.key == x).flatMap(_.get(xs:_*))
  }

  def apply(keys: ExtractedValue*): TreeRepr = get(keys:_*).get

  def nodeCount: Int = if (children.isEmpty) 1 else children.foldLeft(1)((a,b) => a + b.nodeCount)

  def leafCount: Int = if (children.isEmpty) 1 else children.foldLeft(0)((a,b) => a + b.leafCount)

  override def toString: String = toString(0)
  def toString(passedDepth: Int): String = {
    val indent = (0 until (4 * passedDepth)).map(_ => " ").mkString("")
    val depthString = if (depth < 10) s" $depth" else depth.toString
    val localProbString = localProb.toString + (0 until (13 - localProb.toString.length)).map(_ => " ").mkString("")
    val globalProbString = globalProb.toString + (0 until (13 - globalProb.toString.length)).map(_ => " ").mkString("")
    val countString = (0 until (9 - count.toString.length)).map(_ => " ").mkString("") + count.toString
    s"$indent### Depth: $depthString  Local Prob: $localProbString  Global Prob: $globalProbString  Counter: $countString  Key: $key" +
      children.toList.sortBy(r => 1F - r.localProb).par.map(_.toString(passedDepth + 1)).mkString("\n", "", "")
  }

  def readableString: String = simpleStrings(-1).drop(1).mkString("\n")
  def simpleStrings(passedDepth: Int = 0): List[String] =
    s"${(0 until (4 * passedDepth)).map(_ => " ").mkString("") + (if (children.isEmpty) s"- ${count} count${if (count == 1) "" else "s"} of:" else "with:")} $key" ::
      children.toList.sortBy(r => 1F - r.localProb).flatMap(_.simpleStrings(passedDepth + 1))

  def toFlat: List[(Int, ExtractedValue, Float, Float, Int)] = (depth, key, localProb, globalProb, count) :: children.toList.flatMap(_.toFlat)

  def writeToFile(filePath: String): Unit = {
    val settings = new CsvWriterSettings
    val pw = new PrintWriter(new File(filePath))
    val writer = new CsvWriter(pw,settings)
    this.toFlat.foreach(f => writer.writeRow(TreeRepr.flatToCsvArray(f)))
    writer.close()
    pw.close()
  }


  // Combinators for summarization:

  type LocalProb = Float
  type GlobalProb = Float

  def leafNodes: List[(List[ExtractedValue], LocalProb, GlobalProb, Int)] = {
    def leafNodesRec(children: Set[TreeRepr], nameAcc: List[ExtractedValue] = Nil): List[(List[ExtractedValue], LocalProb, GlobalProb, Int)] =
      children.toList.flatMap {
        case TreeRepr(_, nextKey, lp, gp, cnt, c) if c.isEmpty => List((nameAcc :+ nextKey, lp, gp, cnt))
        case next => leafNodesRec(next.children, nameAcc :+ next.key)
      }
    leafNodesRec(children, List(key))
  }

  def truncate(depth: Int): TreeRepr = depth match {
    case 0 => this.copy(children = Set.empty)
    case _ => this.copy(children = children.map(_.truncate(depth - 1)))
  }

  def merge(other: TreeRepr, ignoreKeys: Boolean = false): TreeRepr = {
    if (!ignoreKeys) require(other.key == key, s"Keys much match to merge Trees. Tried to merge other tree with key: ${other.key} into this tree with key: $key")
    val newChildren = other.children.foldLeft( children ){
      case (childAcc, newChild) => childAcc.find(_.key == newChild.key).fold(childAcc + newChild){ case existingChild => (childAcc - existingChild) + existingChild.merge(newChild)} }
    this.copy(count = count + other.count, children = newChildren)
  }

  def mergeAndEliminateChildren(atDepth: Int = 1): TreeRepr =
    if (depth <= 0) this
    else if (depth == 1) this.copy(children = children.foldLeft(TreeRepr.empty){case (a,b) => b.merge(a, true)}.children)
    else this.copy(children = children.map(_.mergeAndEliminateChildren(depth - 1)))

  def withoutQNodes: TreeRepr = this.copy(children = this.children.collect{ case c if c.key != "_?_" => c.withoutQNodes } )

  def collapseUnitaryPaths(delimiter: String = " ∫ ", childMergeCondition: TreeRepr => Boolean = r => r.count == r.children.head.count): TreeRepr = {
    if (this.children.isEmpty) this
    else if (this.children.size == 1 && childMergeCondition(this))
      this.children.head.copy(depth = this.depth, key = this.key + delimiter + this.children.head.key).collapseUnitaryPaths(delimiter, childMergeCondition)
    else this.copy(children = this.children.map(c => c.collapseUnitaryPaths(delimiter, childMergeCondition)))
  }

  def renormalizeProbs: TreeRepr = {
    def renormalizeProbabilities(repr: TreeRepr, totalCount: Option[Float] = None): TreeRepr = {
      val normalizedChildren = repr.children.map(c => renormalizeProbabilities(c, totalCount.orElse(Some(repr.count.toFloat))))
      repr.copy(
//      localProb = ???,   // TODO!!!!!!!!!!!!!!!!!
        globalProb = repr.count / totalCount.getOrElse(repr.count.toFloat),
        children = normalizedChildren
      )
    }
    renormalizeProbabilities(this)
  }

  type PpmElement = (List[ExtractedValue], LocalProb, GlobalProb, Int)

  def extractMostNovel: (PpmElement, TreeRepr) = {
    def findMostNovel(repr: TreeRepr): PpmElement = repr.leafNodes.minBy(_._2)
    def subtractMostNovel(repr: TreeRepr, key: List[ExtractedValue], decrement: Int): TreeRepr = key match {
      case thisKey :: childKey :: remainderKeys if repr.key == thisKey =>
        require(repr.count >= decrement, s"Cannot decrement a count past zero at key: $thisKey  Attempted: ${repr.count} - $decrement")
        require(repr.children.exists(_.key == childKey), s"They key: $childKey is not a child key of: $thisKey")
        repr.copy(count = repr.count - decrement, children = repr.children.collect {
          case c if c.key != childKey => c
          case c if c.key == childKey && c.count != decrement => subtractMostNovel(c, childKey :: remainderKeys, decrement)
        })
      case thisKey :: Nil if repr.key == thisKey =>
        require(repr.count >= decrement, s"Cannot decrement a count past zero at key: $thisKey  Attempted: ${repr.count} - $decrement")
        require(repr.children.isEmpty, s"Should only decrement counts on leaf nodes!")
        repr.copy(count = repr.count - decrement)
      case k :: _ => throw new IllegalArgumentException(s"Key not found: $k")
      case Nil => throw new IllegalArgumentException(s"Cannot decrement a tree when given an empty key.")
    }
    val mostNovel = findMostNovel(this)
    mostNovel -> subtractMostNovel(this, mostNovel._1, mostNovel._4).renormalizeProbs()
  }

  @tailrec
  final def extractMostNovel(count: Int = 1, acc: List[PpmElement] = Nil): List[PpmElement] =
    Try(this.extractMostNovel) match {
      case Failure(_) => acc.reverse
      case Success(_) if count == 0 => acc.reverse
      case Success((novel, next)) => next.extractMostNovel(count - 1, novel :: acc)
    }

  @tailrec
  final def extractBelowLocalProb(localProbThreshold: Float, acc: List[PpmElement] = Nil): List[PpmElement] =
    Try(this.extractMostNovel) match {
      case Failure(_) => acc.reverse
      case Success(_) if localProb > localProbThreshold => acc.reverse
      case Success((novel, next)) => next.extractBelowLocalProb(localProbThreshold, novel :: acc)
    }

  @tailrec
  final def extractBelowGlobalProb(globalProbThreshold: Float, acc: List[PpmElement] = Nil): List[PpmElement] =
    Try(this.extractMostNovel) match {
      case Failure(_) => acc.reverse
      case Success(_) if globalProb > globalProbThreshold => acc.reverse
      case Success((novel, next)) => next.extractBelowGlobalProb(globalProbThreshold, novel :: acc)
    }

  def mostNovelKeys(count: Int = 1, delimiter: String = " ∫ "): List[ExtractedValue] = extractMostNovel(count).map(_._1.mkString(delimiter))

  def incrementdepth(additionalIncrement: Int = 1): TreeRepr = this.copy(
    depth = depth + additionalIncrement,
    children = children.map(_.incrementdepth(additionalIncrement))
  )
}

case object TreeRepr {
  val empty: TreeRepr = TreeRepr(0, "", 0F, 0F, 0, Set.empty)

  def fromChildren(topLevelKey: ExtractedValue, children: Set[TreeRepr]): TreeRepr =
    TreeRepr(0, topLevelKey, 1F, 1F, children.map(_.count).sum, children.map(_.incrementdepth()))
  def fromNamespacedChildren(topLevelKey: ExtractedValue, namespacedChildren: Map[ExtractedValue, TreeRepr]): TreeRepr = {
    val renamedChildren = namespacedChildren.map{ case (namespace, tree) => tree.copy(key = s"$namespace:${tree.key}") }.toSet[TreeRepr]
    fromChildren(topLevelKey, renamedChildren)
  }

  type Depth = Int
  type LocalProb = Float
  type GlobalProb = Float
  type ObservationCount = Int
  type CSVRow = (Depth, ExtractedValue, LocalProb, GlobalProb, ObservationCount)

  def fromFlat(repr: List[CSVRow]): TreeRepr = {
    def fromFlatRecursive(rows: List[CSVRow], atDepth: Int, accAtThisDepth: List[TreeRepr]): (Set[TreeRepr], List[CSVRow]) = {
      val thisDepthSiblings = rows.takeWhile(_._1 == atDepth) // WARNING: This `takeWhile` is important for correctness. See `throw new RuntimeException` code below.
        .map(l => TreeRepr(l._1, l._2, l._3, l._4, l._5, Set.empty))
      val remainingRows = rows.drop(thisDepthSiblings.size)
      val nextDepth = remainingRows.headOption.map(_._1)
      nextDepth match {
        case None =>  // this is the last item in the list
          (accAtThisDepth ++ thisDepthSiblings).toSet -> remainingRows
        case Some(d) if d == atDepth => // There should never be a `nextDepth` equal to `atDepth` because of the `takeWhile(_._1 == atDepth)` above
          throw new RuntimeException("THIS SHOULD NEVER HAPPEN! (because of `takeWhile(_._1 == atDepth)` above)")    // fromFlatRecursive(remainingRows, atDepth, accAtThisDepth ++ thisDepthSiblings)
        case Some(d) if d < atDepth  => // returning to parent case
          (accAtThisDepth ++ thisDepthSiblings).toSet -> remainingRows
        case Some(d) if d > atDepth  => // descending into the child case
          val (childSet, nextRemainder) = fromFlatRecursive(remainingRows, atDepth + 1, List.empty)
          val updatedThisDepthList = accAtThisDepth ++ thisDepthSiblings.dropRight(1) :+ thisDepthSiblings.last.copy(children = childSet)
          fromFlatRecursive(nextRemainder, atDepth, updatedThisDepthList)
      }
    }
    fromFlatRecursive(repr, 0, List.empty)._1.head
  }

  def flatToCsvArray(t: CSVRow): Array[String] = Array(t._1.toString,t._2,t._3.toString,t._4.toString,t._5.toString)
  def csvArrayToFlat(a: Array[String]): CSVRow = (a(0).toInt, Option(a(1)).getOrElse(""), a(2).toFloat, a(3).toFloat, a(4).toInt)

  def readFromFile(filePath: String): Option[TreeRepr] = Try {
    val fileHandle = new File(filePath)
    val parser = new CsvParser(new CsvParserSettings)
    val rows: List[Array[String]] = parser.parseAll(fileHandle).asScala.toList
    TreeRepr.fromFlat(rows.map(TreeRepr.csvArrayToFlat))
  }.toOption
}
