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
import java.util.function.Consumer
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
import scala.annotation.tailrec

//type AnAlarm = (List[String], (Long, Long, Alarm, Set[NamespacedUuidDetails], Map[String, Int]))
case class AnAlarm (key:List[String], details:(Long, Long, Alarm, Set[NamespacedUuidDetails], Map[String, Int]))

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

  case class NamespacedUuidDetails(extendedUuid: NamespacedUuid, name: Option[String] = None, pid: Option[String] = None)
}

case object AlarmExclusions {
  val cadets = Set("ld-elf.so.1", "local", "bounce", "master", "pkg", "top", "mlock", "cleanup", "qmgr", "smtpd", "trivial-rewrite", "head")
  val clearscope = Set("system_server", "proc", "com.android.inputmethod.latin", "com.android.camera2", "com.android.launcher3", "com.android.smspush", "com.android.quicksearchbox", "com.android.gallery3d", "android.process.media", "com.android.music")
  val fivedirections = Set("\\windows\\system32\\svchost.exe", "\\program files\\tightvnc\\tvnserver.exe", "mscorsvw.exe")
  val marple= Set()
  val theia = Set("qt-opensource-linux-x64-5.10.1.run", "/usr/lib/postgresql/9.1/bin/postgres", "whoopsie", "Qt5.10.1", "5.10.1", "/bin/dbus-daemon", "/usr/sbin/console-kit-daemon")
  val trace = Set()
  val general = Set("<no_subject_path_node>")
  val allExclusions = cadets ++ clearscope ++ fivedirections ++ marple ++ theia ++ trace ++ general
  def filter(alarm: PpmNodeActorAlarmDetected): Boolean = // true == allow an alarm to be reported.
    ! alarm.alarmData.exists(level => allExclusions.contains(level.key))
}


case class PpmDefinition[DataShape](
  name: String,
  incomingFilter: Filter[DataShape],
  discriminators: List[Discriminator[DataShape]],
  uuidCollector: DataShape => Set[NamespacedUuidDetails],
  timestampExtractor: DataShape => Long,
  shouldApplyThreshold: Boolean,
  alarmFilter: PpmNodeActorAlarmDetected => Boolean = AlarmExclusions.filter
)(
  context: ActorContext,
  alarmActor: ActorRef,
  hostName: HostName
) extends LazyLogging {


  val inputFilePath = Try(ppmConfig.basedir + name + ppmConfig.loadfilesuffix + ".csv").toOption
  val outputFilePath =
    if (ppmConfig.shouldsave)
      Try(ppmConfig.basedir + name + ppmConfig.savefilesuffix + ".csv").toOption
    else None

  val startingState =
      if (ppmConfig.shouldload)
        inputFilePath.flatMap { s =>
          TreeRepr.readFromFile(s).map { t => println(s"Reading tree $name in from file: $s"); t }
            .orElse {println(s"Loading no data for tree: $name"); None}
        }
      else { println(s"Loading no data for tree: $name"); None }

  val tree = context.actorOf(Props(classOf[PpmNodeActor], name, alarmActor, startingState), name = name)

  val inputAlarmFilePath  = Try(ppmConfig.basedir + name + ppmConfig.loadfilesuffix + "_alarm.json").toOption
  val outputAlarmFilePath =
    if (ppmConfig.shouldsave)
      Try(ppmConfig.basedir + name + ppmConfig.savefilesuffix + "_alarm.json" + s"_$hostName").toOption
    else None
  var alarms: Map[List[ExtractedValue], (Long, Long, Alarm, Set[NamespacedUuidDetails], Map[String, Int])] =
    if (ppmConfig.shouldload)
      inputAlarmFilePath.flatMap { fp =>
        Try {
          import spray.json._
          import ApiJsonProtocol._

          val content = new String(Files.readAllBytes(new File(fp).toPath()), StandardCharsets.UTF_8)
          content.parseJson.convertTo[List[(List[ExtractedValue], (Long, Long, Alarm, Set[NamespacedUuidDetails], Map[String, Int]))]].toMap
        }.toOption orElse  {
          println(s"Did not load alarms for tree: $name. Starting with empty tree state.")
          None
        }
      }.getOrElse(Map.empty[List[ExtractedValue], (Long, Long, Alarm, Set[NamespacedUuidDetails], Map[String, Int])])
    else {
      println(s"Loading no alarms for tree: $name")
      Map.empty
    }

  def observe(observation: DataShape): Unit = if (incomingFilter(observation)) {
    tree ! PpmNodeActorBeginObservation(name, PpmTree.prepareObservation[DataShape](observation, discriminators), uuidCollector(observation), timestampExtractor(observation), alarmFilter)
  }

  //process name and pid/uuid
  def getProcessDetailsFromAlarm(a:Alarm, setNamespacedUuidDetails:Set[NamespacedUuidDetails], timestamp:Long):ProcessDetails = {
    name match {
      case default => ProcessDetails("Unknown Process", Some(0))
    }
  }

  def recordAlarm(alarmOpt: Option[(Alarm, Set[NamespacedUuidDetails], Long)], localProbThreshold: Float): Unit = alarmOpt.foreach { a =>
    //(Key, localProbability, globalProbability, count, siblingPop, parentCount, depthOfLocalProbabilityCalculation)
    //case class AnAlarm (key:List[String], alarm:(Long, Long, Alarm, Set[NamespacedUuidDetails], Map[String, Int]))

    val alarm:Alarm = a._1
    val setNamespacedUuidDetails:Set[NamespacedUuidDetails] = a._2
    val timestamp:Long = a._3
    val key: List[ExtractedValue] = alarm.map(_.key)
    if (alarms contains key) adapt.Application.statusActor ! IncrementAlarmDuplicateCount
    else {

      val alarmDetails = (timestamp, System.currentTimeMillis, alarm, setNamespacedUuidDetails, Map.empty[String,Int])
      val newAlarm = AnAlarm(key,alarmDetails)
      alarms = alarms + AnAlarm.unapply(newAlarm).get

      def thresholdAllows: Boolean = ! ( (a._1.last.localProb > localProbThreshold) && shouldApplyThreshold )

      val processDetails = (getProcessDetailsFromAlarm _).tupled(a)
      //report the alarm
      if (thresholdAllows) AlarmReporter.report(name, newAlarm, processDetails)
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

  def getAllCounts: Future[ Map[List[ExtractedValue], Int] ] = {
    implicit val timeout = Timeout(595 seconds)
    (tree ? PpmNodeActorGetAllCounts(List.empty)).mapTo[Future[PpmNodeActorGetAllCountsResult]].flatMap(identity).map(_.results)
  }

  val saveEveryAndNoMoreThan = 0L //ppmConfig.saveintervalseconds.getOrElse(0L) * 1000  // convert seconds to milliseconds
  val lastSaveCompleteMillis = new AtomicLong(0L)
  val isCurrentlySaving = new AtomicBoolean(false)

  def getRepr(implicit timeout: Timeout): Future[TreeRepr] = (tree ? PpmNodeActorBeginGetTreeRepr(name)).mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity).map{ _.repr }

  def saveStateAsync(): Future[Unit] = {
    val now = System.currentTimeMillis
    val expectedSaveCostMillis = 1000  // Allow repeated saving in subsequent attempts if total save time took no longer than this time.
    if ( ! isCurrentlySaving.get() && lastSaveCompleteMillis.get() + saveEveryAndNoMoreThan - expectedSaveCostMillis <= now ) {
      isCurrentlySaving.set(true)

      implicit val timeout = Timeout(593 seconds)
      (tree ? PpmNodeActorBeginGetTreeRepr(name)).mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity).map{ reprResult =>
        val repr = reprResult.repr
        outputFilePath.foreach(p => repr.writeToFile(p + s"_$hostName"))
        outputAlarmFilePath.foreach((fp: String) => {
          import spray.json._
          import ApiJsonProtocol._

          val content = alarms.toList.toJson.prettyPrint
          val outputFile = new File(fp)
          if ( ! outputFile.exists) outputFile.createNewFile()
          Files.write(outputFile.toPath, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING)
        })

        if (this.isInstanceOf[PartialPpm[_]]) this.asInstanceOf[PartialPpm[_]].saveStateSync(hostName)

        lastSaveCompleteMillis.set(System.currentTimeMillis)
        isCurrentlySaving.set(false)
      }.failed.map { case e =>
        println(s"Error writing to file for $name tree: ${e.getMessage}")
        isCurrentlySaving.set(false)
      }
    } else Future.successful(())
  }
  def prettyString: Future[String] = {
    implicit val timeout = Timeout(593 seconds)
    (tree ? PpmNodeActorBeginGetTreeRepr(name)).mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity).map(_.repr.toString)
  }
}

trait PartialPpm[JoinType] { myself: PpmDefinition[DataShape] =>
  type PartialShape = DataShape
  val discriminators: List[Discriminator[PartialShape]]
  require(discriminators.length == 2, "PartialPpm trees must have a discriminator length of exactly two; the first extracting from the observation written into the partialMap, and the second extracting from the observation that completes the match. NOTE: they can be reordered (mixed) by overriding the `arrangeExtracted` function.")
  implicit def partialMapJson: RootJsonFormat[(JoinType, (List[ExtractedValue],Set[NamespacedUuidDetails]))] = implicitly[RootJsonFormat[(JoinType, (List[ExtractedValue], Set[NamespacedUuidDetails]))]]
  var partialMap: mutable.Map[JoinType, (List[ExtractedValue],Set[NamespacedUuidDetails])] = (inputFilePath, ppmConfig.shouldload) match {
    case (Some(fp), true) =>
      val loadPath = fp + ".partialMap"
      Try {
        import spray.json._
        import ApiJsonProtocol._

        val toReturn = mutable.Map.empty[JoinType,(List[ExtractedValue],Set[NamespacedUuidDetails])]
        Files.lines(Paths.get(loadPath)).forEach(new Consumer[String]{
          override def accept(line: String): Unit = {
            val (k, (v1,v2)) = line.parseJson.convertTo[(JoinType, (List[ExtractedValue],Set[NamespacedUuidDetails]))]
            toReturn.put(k,(v1,v2))
          }
        })

        println(s"Read in from disk $name at $loadPath: ${toReturn.size}")
        toReturn
      } match {
        case Success(m) => m
        case Failure(e) =>
          println(s"Failed to read from disk $name at $loadPath (${e.toString})")
          mutable.Map.empty
      }
    case _ => mutable.Map.empty
  }

  def getJoinCondition(observation: PartialShape): Option[JoinType]
  def arrangeExtracted(extracted: List[ExtractedValue]): List[ExtractedValue] = extracted

  // The first filter defines whether an observation will be saved in the "partialMap" cache (after matching nothing on the joinCondition).
  // The second filter defines whether we have a successful match from the "partialMap" cache given the join condition.
  val partialFilters: (PartialShape => Boolean, PartialShape => Boolean)

  override def observe(observation: PartialShape): Unit = if (incomingFilter(observation)) {
    getJoinCondition(observation).foreach { joinValue =>
      partialMap.get(joinValue) match {
        case None =>
          if (partialFilters._1(observation)) {
            val extractedList = discriminators(0)(observation)
            if (extractedList.nonEmpty) partialMap(joinValue) = extractedList -> myself.uuidCollector(observation)
          }
        case Some((firstExtracted,uuidDetailSet)) =>
          if (partialFilters._2(observation)) {
            val newlyExtracted = discriminators(1)(observation)
            if (newlyExtracted.nonEmpty) {
              tree ! PpmNodeActorBeginObservation(name, arrangeExtracted(firstExtracted ++ newlyExtracted), uuidDetailSet ++ uuidCollector(observation), observation._1.latestTimestampNanos, myself.alarmFilter)
            }
          }
      }
    }
  }


  def saveStateSync(hostName: HostName): Unit = {
    outputFilePath.foreach { savePath =>
      Try {
        import spray.json._
        import ApiJsonProtocol._

        val outputFile = new File(savePath + ".partialMap" + s"_$hostName")
        if (!outputFile.exists) outputFile.createNewFile()

        val writer = new BufferedWriter(new FileWriter(outputFile))
        for (pair <- this.partialMap) {
          writer.write(pair.toJson.compactPrint + "\n")
        }
        writer.close()

      } getOrElse {
        println(s"Failed to save partial map to disk $name at $savePath.partialMap: ${partialMap.size}")
      }
    }
  }
}






case object PpmTree {
  def prepareObservation[DataShape](data: DataShape, ds: List[Discriminator[DataShape]]): List[ExtractedValue] = ds.flatMap(_.apply(data))
}



case class PpmNodeActorBeginObservation(treeName: String, extractedValues: List[ExtractedValue], collectedUuids: Set[NamespacedUuidDetails], dataTimestamp: Long, alarmFilter: PpmNodeActorAlarmDetected => Boolean)
case class PpmNodeActorObservation(treeName: String, extractedValues: List[ExtractedValue], collectedUuids: Set[NamespacedUuidDetails], dataTimestamp: Long, siblingPopulation: Int, parentCount: Int, parentLocalProb: Float, acc: Alarm, alarmFilter: PpmNodeActorAlarmDetected => Boolean, newLeafProb: Option[(Float,Int)], depth: Int)
case class PpmNodeActorBeginGetTreeRepr(treeName: String, startingKey: List[ExtractedValue] = Nil)
case class PpmNodeActorGetTreeRepr(yourDepth: Int, key: String, siblingPopulation: Int, parentCount: Int, parentGlobalProb: Float)
case class PpmNodeActorGetTreeReprResult(repr: TreeRepr)
case class PpmNodeActorGetAllCounts(accumulatedKey: List[ExtractedValue])
case class PpmNodeActorGetAllCountsResult(results: Map[List[ExtractedValue], Int])
case object PpmNodeActorGetTopLevelCount
case class PpmNodeActorGetTopLevelCountResult(count: Int)
case class PpmNodeActorAlarmDetected(treeName: String, alarmData: Alarm, collectedUuids: Set[NamespacedUuidDetails], dataTimestamp: Long)
case class PpmNodeActorManyAlarmsDetected(alarms: Set[PpmNodeActorAlarmDetected])

class PpmNodeActor(thisKey: ExtractedValue, alarmActor: ActorRef, startingState: Option[TreeRepr]) extends Actor with ActorLogging {
  var counter = 0

  var children: Map[ExtractedValue, ActorRef] = startingState.fold {
    Map.empty[ExtractedValue, ActorRef]
  } { thisTreeState =>
    counter = thisTreeState.count
    thisTreeState.children.filter { c =>
      c.key != "_?_"
    }.map { c =>
      c.key -> newSymbolNode(c.key, Some(c))
    }.toMap   // + ("_?_" -> new QNode(children)) // Ensure a ? node is always present, even if it isn't in the loaded data.
  }

  def childrenPopulation: Int = children.size  // + 1   // `+ 1` for the Q node

  def newSymbolNode(newNodeKey: ExtractedValue, startingState: Option[TreeRepr] = None): ActorRef =
    context.actorOf(Props(classOf[PpmNodeActor], newNodeKey, alarmActor, startingState))

  def localProbOfThisObs(parentCount: Int): Float =
    if (parentCount == 0) 1F
    else counter.toFloat / parentCount.toFloat

  def qLocalProbOfThisObs(siblingPopulation: Int, parentCount: Int): Float =
    if (parentCount == 0) 1F
    else siblingPopulation.toFloat / (siblingPopulation.toFloat + parentCount.toFloat)

  def globalProbOfThisObs(parentGlobalProb: Float, parentCount: Int): Float =
    (counter.toFloat / parentCount.toFloat) * parentGlobalProb

  /* Alternate methods of calculating local probability of a standard node and a question mark node.
  (Based on the email Anthony Williams sent on September 14, 2018)

  1. Simple Good-Turing
    def qSimpleGoodTuringLP(qNodeVal: Int, parentCount: Int): Float = {
      // qNodeVal is the number of sibling nodes with a count of 1
      if (parentCount == 0) 1F
      else qNodeVal.toFloat / parentCount.toFloat
    }

    def simpleGoodTuringLP(parentCount: Int): Float = localProbOfThisObs(parentCount)

  2. Additive Smoothing
    def qAdditiveSmoothingLP(siblingCount: Int, parentCount: Int): Float = {
      1F / (parentCount.toFloat + siblingCount + 1) // +1 to count ?-node
    }

    def additiveSmoothingLP(siblingCount: Int, parentCount: Int): Float = {
      (counter.toFloat + 1) / (parentCount.toFloat + siblingCount + 1)
      }

  3. Cleary and Witten's Method A
    def qClearyWittenMethodALP(parentCount: Int): Float = {
      1F / (parentCount.toFloat + 1)
    }

    def clearyWittenMethodALP(parentCount: Int): Float = {
      (counter.toFloat) / (parentCount.toFloat + 1)
      }

  4. Cleary and Witten's Method B
    def qClearyWittenMethodBLP(siblingCount: Int, parentCount: Int): Float = {
      if (parentCount == 0) 1F
      else siblingCount.toFloat / parentCount.toFloat
    }

    def clearyWittenMethodBLP(siblingCount: Int, parentCount: Int): Float = {
      if (parentCount == 0) 1F
      else (counter.toFloat - 1) / parentCount.toFloat
      }

  5. Cleary and Witten's Method C
    def qClearyWittenMethodCLP(siblingCount: Int, parentCount: Int): Float = {
      siblingCount.toFloat / (siblingCount.toFloat + parentCount.toFloat + 1)
    }

    def clearyWittenMethodCLP(siblingCount: Int, parentCount: Int): Float = {
      counter.toFloat / (siblingCount.toFloat + parentCount.toFloat + 1)
      }

  6. GoodTuringInspired
    def qGoodTuringInspiredLP(siblingCount: Int, parentCount: Int): Float = {
      if (parentCount == 0) 1F
      else siblingCount.toFloat / parentCount.toFloat
    }

    def goodTuringInspiredLP(parentCount: Int): Float = {
      if (parentCount == 0) 1F
      else counter.toFloat / parentCount.toFloat
      }
 */

  def receive = {

    case PpmNodeActorGetTopLevelCount => sender() ! PpmNodeActorGetTopLevelCountResult(counter)

    case PpmNodeActorBeginObservation(treeName: String, extractedValues: List[ExtractedValue], collectedUuids: Set[NamespacedUuidDetails], dataTimestamp: Long, alarmFilter) =>
      extractedValues match {
        case Nil => log.warning(s"Tried to start an observation in tree: $treeName with an empty extractedValues.")
        case extracted :: remainder =>
          /*
          If the first extracted value is new to this tree (at this level)
          then an alarm will be recorded. The local probability of the question
          mark node (at this level) will be used as the local probability of the
          leaf node in the alarm.
           */
          val newLeafProb =
            if (children.contains(extracted)) None
            else Some(qLocalProbOfThisObs(childrenPopulation, counter) -> 1)
          val childNode = children.getOrElse(extracted, {
            val newChild = newSymbolNode(extracted)
            children = children + (extracted -> newChild)
            newChild
          })
          counter += 1
          childNode ! PpmNodeActorObservation(treeName, remainder, collectedUuids, dataTimestamp, childrenPopulation, counter, 1F, List.empty, alarmFilter, newLeafProb, 1 )
      }


    case PpmNodeActorObservation(treeName, remainingExtractedValues, collectedUuids, dataTimestamp, siblingPopulation, parentCount, parentGlobalProb, alarmAcc: Alarm, alarmFilter, passNewLeafProb, depth) =>
      counter += 1
      val thisLocalProb = localProbOfThisObs(parentCount)
      val thisQLocalProb = qLocalProbOfThisObs(siblingPopulation, parentCount)
      val thisGlobalProb = globalProbOfThisObs(parentGlobalProb, parentCount)
      val thisAlarmComponent = PpmTreeNodeAlarm(thisKey, thisLocalProb, thisGlobalProb, counter, siblingPopulation, parentCount, depth)
      remainingExtractedValues match {
        case Nil if counter == 1 =>
          val alarmLocalProb = if (passNewLeafProb.isDefined && parentCount == 1) passNewLeafProb.get else thisQLocalProb -> depth // We use the newLeafProb if the parent node is new.
          val leafAlarmComponent = PpmTreeNodeAlarm(thisKey, alarmLocalProb._1, thisGlobalProb, counter, siblingPopulation, parentCount, alarmLocalProb._2)
          val alarm = PpmNodeActorAlarmDetected(treeName, alarmAcc :+ leafAlarmComponent, collectedUuids, dataTimestamp)  // Sound an alarm if the end is novel.
          if (alarmFilter(alarm)) alarmActor ! alarm
        case Nil =>
        case extracted :: remainder =>
          /*
          If passNewLeafProb is defined, we pass it on;
          if it's not defined and the newly extracted value has not been seen before
          (i.e., it is not contained in `children`), we capture the local probability of the ?-node
          (and tree depth) and pass it to the leaf (eventually) through newly defined nodes in the tree.
           */
          val newLeafProb =  if (passNewLeafProb.isDefined) passNewLeafProb else {
            if (children.contains(extracted)) None
            else Some(qLocalProbOfThisObs(childrenPopulation + 1, counter) -> (depth + 1))
          }
          val childNode = children.getOrElse(extracted, {
            val newChild = newSymbolNode(extracted)
            children = children + (extracted -> newChild)
            newChild
          })
          childNode ! PpmNodeActorObservation(treeName, remainder, collectedUuids, dataTimestamp, childrenPopulation, counter, thisGlobalProb, alarmAcc :+ thisAlarmComponent, alarmFilter, newLeafProb, depth + 1)
      }


    case PpmNodeActorBeginGetTreeRepr(treeName: String, startingKey) =>
      implicit val timeout = Timeout(599 seconds)
      val qNodeRepr = if (children.isEmpty)
        Set.empty[TreeRepr]
      else {
        val prob = if (counter == 0) 1 else childrenPopulation.toFloat / counter.toFloat   // TODO: This should reference another probability calculation instead of being hardcoded as a one-off!!!!!!!
        Set(TreeRepr(1, "_?_", prob, prob, childrenPopulation, Set.empty))
      }
      val futureResult: Future[PpmNodeActorGetTreeReprResult] = startingKey match {
        case Nil =>
          val futureRepr = Future.sequence(
            children.map { case (k,v) =>
              (v ? PpmNodeActorGetTreeRepr(1, k, childrenPopulation, counter, 1F)).mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity)
            }
          ).map { resolvedChildren =>
            PpmNodeActorGetTreeReprResult(TreeRepr(0, treeName, 1F, 1F, counter, resolvedChildren.map(_.repr).toSet ++ qNodeRepr))
          }
          futureRepr
        case childKey :: remainder =>
          children.get(childKey) match {
            case Some(childRef) => (childRef ? PpmNodeActorBeginGetTreeRepr(childKey, remainder)).mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity)
            case None =>
//              Future.failed(new IndexOutOfBoundsException(s"No child element for key $childKey"))
              Future.successful(PpmNodeActorGetTreeReprResult(TreeRepr.empty))
          }
      }

      sender() ! futureResult


    case PpmNodeActorGetTreeRepr(yourDepth: Int, key: String, siblingPopulation: Int, parentCount: Int, parentGlobalProb: Float) =>
      implicit val timeout = Timeout(599 seconds)
      val thisLocalProb = counter.toFloat / parentCount.toFloat
      val thisGlobalProb = thisLocalProb * parentGlobalProb
      val childPop = childrenPopulation
      val qNodeRepr = if (children.isEmpty)
        Set.empty[TreeRepr]
      else {
        val prob = if (counter == 0) 1 else childPop.toFloat / counter.toFloat
        Set(TreeRepr(yourDepth + 1, "_?_", prob, prob * thisGlobalProb, childPop, Set.empty))
      }
      val futureResult = Future.sequence(
        children.map { case (k,v) =>
          (v ? PpmNodeActorGetTreeRepr(yourDepth + 1, k, childPop, counter, thisGlobalProb)).mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity)
        }
      ).map { resolvedChildren =>
        PpmNodeActorGetTreeReprResult(TreeRepr(yourDepth, key, thisLocalProb, thisLocalProb * parentGlobalProb, counter, resolvedChildren.map(_.repr).toSet ++ qNodeRepr))
      }
      sender() ! futureResult


    case PpmNodeActorGetAllCounts(accumulatedKey: List[ExtractedValue]) =>
      implicit val timeout = Timeout(597 seconds)
      val qNodeCountTuple = accumulatedKey ++ List(thisKey, "_?_") -> childrenPopulation
      val childNodeCountTuple = accumulatedKey ++ List(thisKey) -> counter
      val countTuples = Map(qNodeCountTuple, childNodeCountTuple)
      val futureResult = Future.sequence(
        children.values.map(child => (child ? PpmNodeActorGetAllCounts(accumulatedKey :+ thisKey)).mapTo[Future[PpmNodeActorGetAllCountsResult]].flatMap(identity))
      ).map(x => x.foldLeft(Map.empty[List[ExtractedValue], Int])((a,b) => a ++ b.results) ++ countTuples)
      sender() ! futureResult.map(r => PpmNodeActorGetAllCountsResult(r))


    case msg => log.error(s"Received unknown message: $msg")

  }
}


class PpmManager(hostName: HostName, source: String, isWindows: Boolean) extends Actor with ActorLogging { thisActor =>

  val (pathDelimiterRegexPattern, pathDelimiterChar) = if (isWindows) ("""\\""", """\""") else ("""/""" ,   "/")
  val sudoOrPowershellComparison: String => Boolean = if (isWindows) {
    _.toLowerCase.contains("powershell")
  } else {
    _.toLowerCase == "sudo"
  }

  import NoveltyDetection._

  val cdmSanityTrees = List(
    PpmDefinition[CurrentCdm]("CDM-Event",
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
      d => Set(NamespacedUuidDetails(CdmUUID(d.asInstanceOf[cdm18.Event].uuid, source))),
      _.asInstanceOf[cdm18.Event].timestampNanos,
      shouldApplyThreshold = false
    )(thisActor.context, context.self, hostName),

    PpmDefinition[CurrentCdm]("CDM-Subject",
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
      d => Set(NamespacedUuidDetails(CdmUUID(d.asInstanceOf[cdm18.Subject].uuid, source))),
      _ => 0L,
      shouldApplyThreshold = false
    )(thisActor.context, context.self, hostName),

    PpmDefinition[CurrentCdm]("CDM-Netflow",
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
      d => Set(NamespacedUuidDetails(CdmUUID(d.asInstanceOf[cdm18.NetFlowObject].uuid, source))),
      _ => 0L,
      shouldApplyThreshold = false
    )(thisActor.context, context.self, hostName)
    ,
    PpmDefinition[CurrentCdm]("CDM-FileObject",
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
      d => Set(NamespacedUuidDetails(CdmUUID(d.asInstanceOf[cdm18.FileObject].uuid, source))),
      _ => 0L,
      shouldApplyThreshold = false
    )(thisActor.context, context.self, hostName)
  ).par


  val iforestTrees = List(
    PpmDefinition[(Event,AdmSubject,Set[AdmPathNode])]("iForestProcessEventType",
      d => d._3.nonEmpty,
      List(
        d => List(d._3.map(_.path).toList.sorted.mkString("-"),d._2.uuid.uuid.toString),
        d => List(d._1.eventType.toString)
      ),
      d => Set(NamespacedUuidDetails(d._2.uuid)),
      _._1.latestTimestampNanos,
      shouldApplyThreshold = false,
      _ => false
    )(thisActor.context, context.self, hostName),

    PpmDefinition[(Event,AdmSubject,Set[AdmPathNode])]("iForestCommonAlarms",
      d => d._3.nonEmpty,
      List(
        d => List(d._3.map(_.path).toList.sorted.mkString("-"),d._2.uuid.uuid.toString),
        d => List(d._1.eventType.toString)
      ),
      d => Set(NamespacedUuidDetails(d._2.uuid)),
      _._1.latestTimestampNanos,
      shouldApplyThreshold = false
    )(thisActor.context, context.self, hostName),

    PpmDefinition[(Event,AdmSubject,Set[AdmPathNode])]("iForestUncommonAlarms",
      d => d._3.nonEmpty,
      List(
        d => List(d._3.map(_.path).toList.sorted.mkString("-"),d._2.uuid.uuid.toString),
        d => List(d._1.eventType.toString)
      ),
      d => Set(NamespacedUuidDetails(d._2.uuid)),
      _._1.latestTimestampNanos,
      shouldApplyThreshold = false
    )(thisActor.context, context.self, hostName)
  ).par


  val esoTrees = List(
    PpmDefinition[DataShape]( "ProcessFileTouches",
      d => readAndWriteTypes.contains(d._1.eventType),
      List(
        d => List(d._2._2.map(_.path).getOrElse("<no_subject_path_node>")),
        d => List(d._3._2.map(_.path).getOrElse("<no_file_path_node>"))
      ),
      d => Set(NamespacedUuidDetails(d._1.uuid),
               NamespacedUuidDetails(d._2._1.uuid,Some(d._2._2.map(_.path).getOrElse("<no_subject_path_node>")), Some(d._2._1.cid.toString)),
               NamespacedUuidDetails(d._3._1.uuid,Some(d._3._2.map(_.path).getOrElse("<no_file_path_node>")))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      _._1.latestTimestampNanos,
      shouldApplyThreshold = true
    )(thisActor.context, context.self, hostName),

    PpmDefinition[DataShape]( "FilesTouchedByProcesses",
      d => readAndWriteTypes.contains(d._1.eventType),
      List(
        d => List(d._3._2.map(_.path).getOrElse("<no_file_path_node>")),
        d => List(d._2._2.map(_.path).getOrElse("<no_subject_path_node>"))
      ),
      d => Set(NamespacedUuidDetails(d._1.uuid),
        NamespacedUuidDetails(d._2._1.uuid,Some(d._2._2.map(_.path).getOrElse("<no_subject_path_node>")), Some(d._2._1.cid.toString)),
        NamespacedUuidDetails(d._3._1.uuid,Some(d._3._2.map(_.path).getOrElse("<no_file_path_node>")))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      _._1.latestTimestampNanos,
      shouldApplyThreshold = true
    )(thisActor.context, context.self, hostName),

    PpmDefinition[DataShape]( "FilesExecutedByProcesses",
      d => d._1.eventType == EVENT_EXECUTE,
      List(
        d => List(d._2._2.map(_.path).getOrElse("<no_subject_path_node>")),
        d => List(d._3._2.map(_.path).getOrElse("<no_file_path_node>"))
      ),
      d => Set(NamespacedUuidDetails(d._1.uuid),
        NamespacedUuidDetails(d._2._1.uuid,Some(d._2._2.map(_.path).getOrElse("<no_subject_path_node>")), Some(d._2._1.cid.toString)),
        NamespacedUuidDetails(d._3._1.uuid,Some(d._3._2.map(_.path).getOrElse("<no_file_path_node>")))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      _._1.latestTimestampNanos,
      shouldApplyThreshold = false
    )(thisActor.context, context.self, hostName),

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
        NamespacedUuidDetails(d._2._1.uuid,Some(d._2._2.map(_.path).getOrElse("<no_subject_path_node>")), Some(d._2._1.cid.toString)),
        NamespacedUuidDetails(d._3._1.uuid,Some(d._3._1.asInstanceOf[AdmNetFlowObject].remoteAddress.getOrElse("no_address_from_CDM")))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      _._1.latestTimestampNanos,
      shouldApplyThreshold = true
    )(thisActor.context, context.self, hostName),

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
        NamespacedUuidDetails(d._2._1.uuid,Some(d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)), Some(d._2._1.cid.toString)),
        NamespacedUuidDetails(d._3._1.uuid,Some(d._3._2.map(_.path).getOrElse("<no_file_path_node>")))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      _._1.latestTimestampNanos,
      shouldApplyThreshold = true
    )(thisActor.context, context.self, hostName),

    PpmDefinition[DataShape]( "ProcessesChangingPrincipal",
      d => d._1.eventType == EVENT_CHANGE_PRINCIPAL,
      List(
        d => List(d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)),  // Process name or UUID
        d => List(d._3._2.map(_.path + s" : ${d._3._1.getClass.getSimpleName}").getOrElse( s"${d._3._1.uuid.rendered} : ${d._3._1.getClass.getSimpleName}"))
      ),
      d => Set(NamespacedUuidDetails(d._1.uuid,Some(d._1.eventType.toString)),
        NamespacedUuidDetails(d._2._1.uuid,Some(d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)), Some(d._2._1.cid.toString)),
        NamespacedUuidDetails(d._3._1.uuid,Some(d._3._2.map(_.path + s" : ${d._3._1.getClass.getSimpleName}").getOrElse( s"${d._3._1.uuid.rendered} : ${d._3._1.getClass.getSimpleName}")))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      _._1.latestTimestampNanos,
      shouldApplyThreshold = false
    )(thisActor.context, context.self, hostName),

    PpmDefinition[DataShape]( "SudoIsAsSudoDoes",
      d => d._2._2.exists(p => sudoOrPowershellComparison(p.path)),
      List(
        d => List(d._1.eventType.toString),
        d => List(d._3._2.map(_.path).getOrElse(d._3._1.uuid.rendered) + " : " + d._3._1.getClass.getSimpleName)
      ),
      d => Set(NamespacedUuidDetails(d._1.uuid,Some(d._1.eventType.toString)),
        NamespacedUuidDetails(d._2._1.uuid),
        NamespacedUuidDetails(d._3._1.uuid,Some(d._3._2.map(_.path).getOrElse(d._3._1.uuid.rendered) + " : " + d._3._1.getClass.getSimpleName))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      _._1.latestTimestampNanos,
      shouldApplyThreshold = false
    )(thisActor.context, context.self, hostName),

    PpmDefinition[DataShape]( "ParentChildProcesses",
      d => d._1.eventType == PSEUDO_EVENT_PARENT_SUBJECT && d._2._2.isDefined && d._3._2.isDefined,
      List(d => List(
        d._3._2.map(_.path).getOrElse("<no_path>"),  // Parent process first
        d._2._2.map(_.path).getOrElse("<no_path>")   // Child process second
      )),
      d => Set(NamespacedUuidDetails(d._1.uuid),
        NamespacedUuidDetails(d._2._1.uuid,Some(d._2._2.get.path)),
        NamespacedUuidDetails(d._3._1.uuid,Some(d._3._2.get.path))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      _._1.latestTimestampNanos,
      shouldApplyThreshold = false
    )(thisActor.context, context.self, hostName),

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
        NamespacedUuidDetails(d._2._1.uuid,d._2._2.map(_.path)),
        NamespacedUuidDetails(d._3._1.uuid,d._3._2.map(_.path))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      _._1.latestTimestampNanos,
      shouldApplyThreshold = false,
      alarmFilter = _ => false
    )(thisActor.context, context.self, hostName)

//    PpmDefinition[DataShape]("SummarizedProcessActivityTiming",
//      d => d._2._1.subjectTypes.contains(SUBJECT_PROCESS), // is a process
//      List(d => List(                         // 1.) Process name
//        d._2._2.map(_.path).getOrElse("{{{unnamed_process}}}"), //es_should_have_been_filtered_out"),
//        d._2._1.cid.toString,                 // 2.) PID, to disambiguate process instances. (collisions are assumed to be ignorably unlikely)
//        d._1.earliestTimestampNanos.toString, // 3.) timestamp
//        d._1.eventType.toString               // 4.) Event type
//        ), _._3 match {                       // 5.) identifier(s) for the object, based on its type
//          case (adm: AdmFileObject, pathOpt) => pathOpt.map(_.path.split(pathDelimiterRegexPattern, -1).toList match {
//            case "" :: remainder => pathDelimiterChar :: remainder
//            case x => x
//          }).getOrElse(List(s"${adm.fileObjectType}:${adm.uuid.rendered}"))
//          case (adm: AdmSubject, pathOpt) => List(pathOpt.map(_.path).getOrElse(s"{${adm.subjectTypes.toList.map(_.toString).sorted.mkString(",")}}:${adm.cid}"))
//          case (adm: AdmSrcSinkObject, _) => List(s"${adm.srcSinkType}:${adm.uuid.rendered}")
//          case (adm: AdmNetFlowObject, _) => List(s"${adm.remoteAddress}:${adm.remotePort}")
//          case (adm, pathOpt) => List(s"UnhandledType:$adm:$pathOpt")
//        }
//      ),
//      d => Set(NamespacedUuidDetails(d._1.uuid),
//        NamespacedUuidDetails(d._2._1.uuid,d._2._2.map(_.path), Some(d._2._1.cid.toString)),
//        NamespacedUuidDetails(d._3._1.uuid,d._3._2.map(_.path))) ++ // TODO: Need to get child process PID (cid) from here
//        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
//        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
//      _._1.latestTimestampNanos
//    )(thisActor.context, context.self)

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
      d => Set(NamespacedUuidDetails(d._1.uuid),
        NamespacedUuidDetails(d._2._1.uuid,Some(d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)), Some(d._2._1.cid.toString)),
        NamespacedUuidDetails(d._3._1.uuid,Some(d._3._2.map(_.path).getOrElse(d._3._1.uuid.rendered)))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      _._1.latestTimestampNanos,
      shouldApplyThreshold = false
    )(thisActor.context, context.self, hostName) with PartialPpm[String] {

      def getJoinCondition(observation: DataShape) =
        observation._3._2.map(_.path).orElse(Some(observation._3._1.uuid.rendered))  // File name or UUID

      val partialFilters = (
        (d: DataShape) => d._1.eventType == EVENT_EXECUTE,
        (d: DataShape) => d._1.eventType == EVENT_UNLINK
      )

      override def partialMapJson = {
        import spray.json._
        import ApiJsonProtocol._
        implicitly[RootJsonFormat[(String,(List[ExtractedValue],Set[NamespacedUuidDetails]))]]
      }
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
      d => Set(NamespacedUuidDetails(d._1.uuid,Some(d._1.eventType.toString)),
        NamespacedUuidDetails(d._2._1.uuid,Some(d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)), Some(d._2._1.cid.toString)),
        NamespacedUuidDetails(d._3._1.uuid,Some(d._3._2.map(_.path).getOrElse(d._3._1.uuid.rendered)))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      _._1.latestTimestampNanos,
      shouldApplyThreshold = false
    )(thisActor.context, context.self, hostName) with PartialPpm[String] {

      def getJoinCondition(observation: DataShape) =
        observation._3._2.map(_.path).orElse(Some(observation._3._1.uuid.rendered))  // File name or UUID

      val partialFilters = (
        (d: DataShape) => writeTypes.contains(d._1.eventType),
        (d: DataShape) => d._1.eventType == EVENT_EXECUTE
      )

      override def partialMapJson = {
        import spray.json._
        import ApiJsonProtocol._
        implicitly[RootJsonFormat[(String,(List[ExtractedValue],Set[NamespacedUuidDetails]))]]
      }
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
      d => Set(NamespacedUuidDetails(d._1.uuid),
        NamespacedUuidDetails(d._2._1.uuid,Some(d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)), Some(d._2._1.cid.toString)),
        NamespacedUuidDetails(d._3._1.uuid,Some(d._3._2.map(_.path).getOrElse(d._3._1.uuid.rendered) + (  // Object name or UUID and type
          d._3._1 match {
            case o: AdmSrcSinkObject => s" : ${o.srcSinkType}"
            case o: AdmFileObject => s" : ${o.fileObjectType}"
            case o: AdmNetFlowObject => s"  NetFlow: ${o.remoteAddress}:${o.remotePort}"
            case _ => ""
          }
          )))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      _._1.latestTimestampNanos,
      shouldApplyThreshold = true
    )(thisActor.context, context.self, hostName) with PartialPpm[AdmUUID] {

      def getJoinCondition(observation: DataShape) = Some(observation._3._1.uuid)   // Object UUID

      val partialFilters = (
        (d: DataShape) => writeTypes.contains(d._1.eventType) ||
          (d._3._1.isInstanceOf[AdmSrcSinkObject] && d._3._1.asInstanceOf[AdmSrcSinkObject].srcSinkType == MEMORY_SRCSINK),     // Any kind of event to a memory object.
        (d: DataShape) => (readTypes.contains(d._1.eventType) ||
          (d._3._1.isInstanceOf[AdmSrcSinkObject] && d._3._1.asInstanceOf[AdmSrcSinkObject].srcSinkType == MEMORY_SRCSINK)) &&  // Any kind of event to a memory object.
          (partialMap.contains(getJoinCondition(d).get) && d._2._2.exists(_.path != partialMap(getJoinCondition(d).get)._1.head))  // subject names are distinct
      )

      override def partialMapJson = implicitly[RootJsonFormat[(AdmUUID,(List[ExtractedValue],Set[NamespacedUuidDetails]))]]
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
      d => Set(NamespacedUuidDetails(d._1.uuid),
        NamespacedUuidDetails(d._2._1.uuid, Some(d._2._2.map(_.path).getOrElse(d._2._1.uuid.rendered)), Some(d._2._1.cid.toString)),
        NamespacedUuidDetails(d._3._1.uuid, Some(d._3._2.map(_.path).getOrElse("AdmNetFlow")))) ++
        d._2._2.map(a => NamespacedUuidDetails(a.uuid)).toSet ++
        d._3._2.map(a => NamespacedUuidDetails(a.uuid)).toSet,
      _._1.latestTimestampNanos,
      shouldApplyThreshold = true
    )(thisActor.context, context.self, hostName) with PartialPpm[AdmUUID] {

      def getJoinCondition(observation: DataShape) = observation._2._2.map(_.uuid)   // TODO: shouldn't this include some time range comparison here?????????????????????????????????????????????????????????????????

      override def arrangeExtracted(extracted: List[ExtractedValue]) = extracted.tail :+ extracted.head

      val partialFilters = (
        (eso: DataShape) => eso._3._1.isInstanceOf[AdmNetFlowObject] && readTypes.contains(eso._1.eventType),
        (eso: DataShape) => eso._3._1.isInstanceOf[AdmFileObject] && writeTypes.contains(eso._1.eventType)
      )

      override def partialMapJson =
        implicitly[RootJsonFormat[(AdmUUID,(List[ExtractedValue],Set[NamespacedUuidDetails]))]]

    }
  ).par


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

  val crossHostTrees = List(

    new PpmDefinition[DataShape]("CrossHostProcessCommunication",
      d => netFlowTypes.contains(d._1.eventType) && (d._3._1 match {
        case AdmNetFlowObject(_,Some(_),Some(_),Some(_),Some(_),_) => true   // Ignore raw sockets. ...and other sockets.
        case _ => false
      }),
      List(
        d => List(
          d._2._2.map(_.path).getOrElse("<<unknown_process>>")), // Sending process name
        d => List(
          d._2._2.map(_.path).getOrElse("<<unknown_process>>"),          // Receiving process name
          d._3._1.asInstanceOf[AdmNetFlowObject].localPort.get.toString  // Receiving port number
        )
      ),
      d => Set(
        NamespacedUuidDetails(d._1.uuid),
        NamespacedUuidDetails(d._2._1.uuid, d._2._2.map(_.path), Some(d._2._1.cid.toString)),
        NamespacedUuidDetails(d._3._1.uuid, d._3._2.map(_.path))
      ) ++ d._2._2.map(n => NamespacedUuidDetails(n.uuid, Some(n.path), None)).toSet,
//        ++ d._3._2.map(n => NamespacedUuidDetails(n.uuid, Some(n.path), None)).toSet,  // Don't need/will never be a path node from a netflow.
      _._1.latestTimestampNanos,
      shouldApplyThreshold = false
    )(thisActor.context, context.self, hostName) with PartialPpm[CrossHostNetObs] {

//      implicit def partialMapJson: RootJsonFormat[(Set[String], (List[ExtractedValue], Set[NamespacedUuidDetails]))] = ???

      val netFlowReadTypes = writeTypes.+(EVENT_CONNECT)
      val netFlowWriteTypes = readTypes.+(EVENT_ACCEPT)

      val partialFilters = (
        (eso: PartialShape) => netFlowReadTypes.contains(eso._1.eventType),
        (eso: PartialShape) => netFlowWriteTypes.contains(eso._1.eventType)
      )

      def getJoinCondition(observation: DataShape): Option[CrossHostNetObs] = Try {
        val nf = observation._3._1.asInstanceOf[AdmNetFlowObject]
        CrossHostNetObs(
          observation match {
            case t if partialFilters._1(t) => Sending
            case t if partialFilters._2(t) => Receiving
            case _ => ???
          },
          nf.localAddress.get,
          nf.localPort.get,
          nf.remoteAddress.get,
          nf.remotePort.get
        )
      }.toOption

      var partialMap2 = Map.empty[HostName, mutable.Map[CrossHostNetObs, Map[List[ExtractedValue],Set[NamespacedUuidDetails]]]]

      override def observe(observation: PartialShape): Unit = if (incomingFilter(observation)) {
        getJoinCondition(observation) match {
          case None => log.error(s"Something unexpected passed the filter for CrossHostProcessCommunication: $observation")
          case Some(joinValue) =>

            val existingThisHost = partialMap2.getOrElse(observation._1.hostName, mutable.Map.empty[CrossHostNetObs, Map[List[ExtractedValue],Set[NamespacedUuidDetails]]])
            if ( ! partialMap2.contains(observation._2._1.hostName)) partialMap2 = partialMap2 + (observation._2._1.hostName -> existingThisHost)
            val existingOtherHosts = (partialMap2 - observation._1.hostName).values
              .foldLeft[Map[CrossHostNetObs, Map[List[ExtractedValue],Set[NamespacedUuidDetails]]]](Map.empty)(_ ++ _)

            val previousSameDirObservations = existingThisHost.getOrElse(joinValue, Map.empty[List[ExtractedValue], Set[NamespacedUuidDetails]])
            val observedIds = uuidCollector(observation)

            val sendRecOpt = joinValue.sendRec match {
              case Sending =>
                val theseDiscs = discriminators(0)(observation)
                val theseIds = previousSameDirObservations.get(theseDiscs).fold(observedIds) { previousIds => observedIds ++ previousIds }
                val thisSendObs = Map(theseDiscs -> theseIds)
                val allSendObs = previousSameDirObservations ++ thisSendObs
                existingThisHost(joinValue) = allSendObs  // Update mutable Map in place
                existingOtherHosts.get(joinValue.invert).map(thisSendObs -> _)
              case Receiving =>
                val theseDiscs = discriminators(1)(observation)
                val theseIds = previousSameDirObservations.get(theseDiscs).fold(observedIds) { previousIds => observedIds ++ previousIds }
                val thisRecObs = Map(theseDiscs -> theseIds)
                val allRecObs = previousSameDirObservations ++ thisRecObs
                existingThisHost(joinValue) = allRecObs  // Update mutable Map in place
                existingOtherHosts.get(joinValue.invert).map(_ -> thisRecObs)
            }

            sendRecOpt.foreach { obs =>
              val combinedObs = for {
                o1 <- obs._1
                o2 <- obs._2
              } yield {
                o1._1 ++ o2._1 -> (o1._2 ++ o2._2)
              }
              combinedObs.foreach { case (extracted, ids) =>
                tree ! PpmNodeActorBeginObservation(
                  name,
                  arrangeExtracted(extracted), // Updated first, because Sending discriminators is first.
                  ids,
                  observation._1.latestTimestampNanos,
                  alarmFilter
                )
              }
            }
        }
      }
    }
  ).par



  val iforestEnabled = ppmConfig.iforestenabled

  lazy val admPpmTrees =
    if (hostName == hostNameForAllHosts) crossHostTrees
    else esoTrees ++ seoesTrees ++ oeseoTrees

  lazy val iforestTreesToUse = if (iforestEnabled && hostName != hostNameForAllHosts) iforestTrees else Nil

  val ppmList =
    if (hostName == hostNameForAllHosts) admPpmTrees
    else cdmSanityTrees ++ admPpmTrees ++ iforestTreesToUse


  // Alarm Local Probabilities for novelty trees (not alarm trees) should be the second input to AlarmLocalProbabilityAccumulator.
  val alarmLpAccumulator = AlarmLocalProbabilityAccumulator(hostName, ppmList.filter(tree => tree.shouldApplyThreshold).flatMap(t => t.alarms.map(_._2._3.last.localProb)).toList)

//  import akka.actor.ActorSystem
  //implicit val executionContext = ActorSystem().dispatcher
  val computeAlarmLpThresholdIntervalMinutes = ppmConfig.computethresholdintervalminutes
  if (computeAlarmLpThresholdIntervalMinutes > 0) { // Dynamic Threshold
     context.system.scheduler.schedule(computeAlarmLpThresholdIntervalMinutes minutes,
      computeAlarmLpThresholdIntervalMinutes minutes)(alarmLpAccumulator.updateThreshold(ppmConfig.alarmlppercentile))
  }
  else alarmLpAccumulator.updateThreshold(ppmConfig.alarmlppercentile) // Static Threshold

  def saveIforestModel(): Unit = {
    val iForestTree = iforestTrees.find(_.name == "iForestProcessEventType")
    val iforestTrainingSaveFile = ppmConfig.iforesttrainingsavefile
    iForestTree match {
      case Some(tree) => tree.getAllCounts.map( counts =>
        EventTypeModels.EventTypeData.writeToFile(counts, EventTypeModels.modelDirIForest + iforestTrainingSaveFile)
      ).onFailure{ case e => log.warning(s"Could not getAllCounts for iForestTree, with error: $e")}
      case None => println("ProcessEventType tree for iForest is not defined.")
    }
  }


//  println(s"Setting shutdown hook to save PPM trees for $hostName, shouldsave: ${ppmConfig.shouldsave}")
////  override def postStop(): Unit = {
//    context.system.registerOnTermination{
//      println(s"Post stop: $didReceiveComplete && ${ppmConfig.shouldsave}")
//      if ( ! didReceiveComplete && ppmConfig.shouldsave) {
//        didReceiveComplete = true
//        val ppmSaveFutures = ppmList.map(_.saveStateAsync())
//        if (iforestEnabled) saveIforestModel()
//        Await.ready(Future.sequence(ppmSaveFutures.seq), 603 seconds)
//      }
//  //    super.postStop()
//    }

  def ppm(name: String): Option[PpmDefinition[_]] = ppmList.find(_.name == name)

  var didReceiveInit = false
  var didReceiveComplete = false

  def receive = {

    case PpmNodeActorAlarmDetected(treeName: String, alarmData: Alarm, collectedUuids: Set[NamespacedUuidDetails], dataTimestamp: Long) =>
      alarmLpAccumulator.insert(alarmData.last.localProb)
      ppm(treeName).fold(
        log.warning(s"Could not find tree named: $treeName to record Alarm: $alarmData with UUIDs: $collectedUuids, with dataTimestamp: $dataTimestamp from: $sender")
      )( tree => tree.recordAlarm(Some((alarmData, collectedUuids, dataTimestamp)), alarmLpAccumulator.threshold))

    case PpmNodeActorManyAlarmsDetected(setOfAlarms) =>
      setOfAlarms.headOption.flatMap(a =>
        ppm(a.treeName)
      ).fold(
        log.warning(s"Could not find tree named: ${setOfAlarms.headOption.map(_.treeName)} to record many Alarms from: $sender")
      )( tree => setOfAlarms.foreach{ case PpmNodeActorAlarmDetected(treeName, alarmData, collectedUuids, dataTimestamp) =>
        tree.recordAlarm( Some((alarmData, collectedUuids, dataTimestamp)), alarmLpAccumulator.threshold )
      })

    case ListPpmTrees =>
      implicit val timeout = Timeout(591 seconds)
      sender() ! Future.sequence(
        ppmList.map(t => (t.tree ? PpmNodeActorGetTopLevelCount).mapTo[PpmNodeActorGetTopLevelCountResult].map(c => t.name -> c.count)).seq
      ).map(s => PpmTreeNames(s.toMap))

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

      if (iforestEnabled) {
        Try(
          (e, s, subPathNodes.asInstanceOf[Set[AdmPathNode]])
        ) match {
          case Success(t) => iforestTrees.find(_.name == "iForestProcessEventType").foreach(p => p.observe(t))
          case Failure(err) => log.warning(s"Cast Failed. Could not process/match message as types (Set[AdmPathNode] and Set[AdmPathNode]) due to erasure: $msg  Message: ${err.getMessage}")
        }
      }
      Try(
        Await.result(f, 15 seconds)
      ).failed.map(e => log.warning(s"Writing batch trees failed: ${e.getMessage}"))


//      sender() ! Ack


    case (_, cdm: CurrentCdm) =>
      cdmSanityTrees.foreach( ppm =>
        ppm.observe(cdm)
      )
      sender() ! Ack


    case PpmTreeAlarmQuery(treeName, queryPath, namespace, startAtTime, forwardFromStartTime, resultSizeLimit, excludeRatingBelow) =>
      val resultOpt = ppm(treeName).map( tree =>
        if (queryPath.isEmpty) tree.alarms.values.map(a => a.copy(_5 = a._5.get(namespace))).toList
        else tree.alarms.collect{ case (k,v) if k.startsWith(queryPath) => v.copy(_5 = v._5.get(namespace))}.toList
      ).map { r =>
        val filteredResults = r.filter { case (dataTimestamp, observationMillis, alarm, uuids, ratingOpt) =>
          (if (forwardFromStartTime) dataTimestamp >= startAtTime else dataTimestamp <= startAtTime) &&
            excludeRatingBelow.forall(test => ratingOpt.forall(given => given >= test))
        }
        val sortedResults = filteredResults.sortBy[Long](i => if (forwardFromStartTime) i._1 else Long.MaxValue - i._1)
        resultSizeLimit.fold(sortedResults)(limit => sortedResults.take(limit))
      }
      sender() ! PpmTreeAlarmResult(resultOpt)

    case PpmTreeCountQuery(treeName) =>
      sender() ! ppm(treeName).map(tree => tree.getAllCounts.map(r => PpmTreeCountResult(Some(r)))).getOrElse(Future.successful(PpmTreeCountResult(None)))

    case SetPpmRatings(treeName, keys, rating, namespace) =>
      sender() ! ppm(treeName).map(tree => keys.map(key => tree.setAlarmRating(key, rating match {case 0 => None; case x => Some(x)}, namespace)))

    case msg @ PpmNodeActorBeginGetTreeRepr(treeName, startingKey) =>
      implicit val timeout = Timeout(30 seconds)
      val reprFut = ppm(treeName)
        .map(d => (d.tree ? msg).mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity))
        .getOrElse(Future.failed(new NoSuchElementException(s"No tree found with name $treeName")))
      sender() ! reprFut

    case InitMsg =>
      if ( ! didReceiveInit) {
        didReceiveInit = true
        if (iforestEnabled)   // TODO: If using iForest again in the future, come back and refactor this to use the right host name!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
          Future {
            Application.ppmManagerActors.keys.foreach(hostName => EventTypeModels.evaluateModels(context.system, hostName))
          }.recoverWith{ case e => e.printStackTrace(); throw e }
      }
      sender() ! Ack

    case SaveTrees(shouldConfirm) =>
      val s = sender()
      val saveFutures = ppmList.map(_.saveStateAsync())
      if (iforestEnabled) saveIforestModel()
      if (shouldConfirm) Future.sequence(saveFutures.toList).onComplete(_ => s ! Ack )

    case CompleteMsg =>
      if ( ! didReceiveComplete) {
        println(s"PPM Manager with hostName: $hostName is completing the stream.")
        didReceiveComplete = true
        val ppmSaveFutures = ppmList.map{ ppm => ppm.saveStateAsync() }
        if (iforestEnabled) saveIforestModel()
        Await.ready(Future.sequence(ppmSaveFutures.seq), 603 seconds)
      }

    case x =>
      log.error(s"PPM Actor received Unknown Message: $x")
      sender() ! Ack
  }
}

case object ListPpmTrees
case class PpmTreeNames(namesAndCounts: Map[String, Int])
case class PpmTreeAlarmQuery(treeName: String, queryPath: List[ExtractedValue], namespace: String, startAtTime: Long = 0L, forwardFromStartTime: Boolean = true, resultSizeLimit: Option[Int] = None, excludeRatingBelow: Option[Int] = None)
case class PpmTreeAlarmResult(results: Option[List[(Long, Long, Alarm, Set[NamespacedUuidDetails], Option[Int])]]) {
  def toUiTree: List[UiTreeElement] = results.map { l =>
    l.foldLeft(Set.empty[UiTreeElement]){ (a, b) =>
      val names = b._3.map(_.key)
      val someUiData = UiDataContainer(b._5, names.mkString("∫"), b._1, b._2, b._3.last.localProb, b._4.map(_.extendedUuid.rendered))
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
    s"${(0 until (4 * passedDepth)).map(_ => " ").mkString("") + (if (children.isEmpty) s"- ${count} count${if (count>=2)"s"} of:" else "with:")} $key" ::
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

  def mostNovelKeys(count: Int = 1, delimiter: String = " ∫ "): List[String] = extractMostNovel(count).map(_._1.mkString(delimiter))
}

case object TreeRepr {
  val empty: TreeRepr = TreeRepr(0, "", 0F, 0F, 0, Set.empty)

  type Depth = Int
  type LocalProb = Float
  type GlobalProb = Float
  type ObservationCount = Int
  type CSVRow = (Depth, ExtractedValue, LocalProb, GlobalProb, ObservationCount)

  def fromFlat(repr: List[CSVRow]): TreeRepr = {
    def fromFlatRecursive(rows: List[CSVRow], atDepth: Int, accAtThisDepth: List[TreeRepr]): (Set[TreeRepr], List[CSVRow]) = {
      val thisDepthSiblings = rows.takeWhile(_._1 == atDepth).map(l => TreeRepr(l._1, l._2, l._3, l._4, l._5, Set.empty))
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


case class AlarmLocalProbabilityAccumulator(hostname: String, initialLocalProbabilities: List[Float]) {

  var lpAccumulator: SortedMap[Float,Int] = // If there are more than 2,147,483,647 alarms with a given LP; then need Long.
    SortedMap.empty(Ordering[Float]) ++ initialLocalProbabilities.groupBy(identity).mapValues(_.size)

  var count : Int = lpAccumulator.values.sum

  var threshold: Float = 1

  // A useful function for testing
  def print : Unit = {
    println("We've seen this many alarms     ", count)
    println("The 1 percentile threshold is...", threshold)
    println("The lp map looks like ", lpAccumulator.toString())
  }

  def insert(lp: Float): Unit = {
    // Since we are only concerned with low lp novelties, we need not track large lps with great precision.
    // This serves to reduce the max size of the lpAccumulator map.
    val approxLp = if (lp >= 0.3) math.round(lp * 10) / 10F else lp
    lpAccumulator += (approxLp -> (lpAccumulator.getOrElse(approxLp,0) + 1))
    count += 1
  }

  def updateThreshold(percentile: Float): Unit = {
    val percentileOfTotal = percentile/100 * count

    var accLPCount = 0

    threshold = lpAccumulator.takeWhile {
      case (_, thisLPCount) =>
        accLPCount += thisLPCount
        accLPCount <= percentileOfTotal
    }.lastOption.map(_._1).getOrElse(1F)

  }
}
