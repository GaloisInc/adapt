package com.galois.adapt

import akka.util.Timeout
import akka.pattern.ask
import com.galois.adapt.AdaptConfig.HostName
import com.galois.adapt.NoveltyDetection.ExtractedValue
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.concurrent.duration._
import cdm20._
import com.galois.adapt.adm.AdmUUID


object PpmSummarizer {
  implicit val ec: ExecutionContext = Application.system.dispatchers.lookup("quine.actor.node-dispatcher")

  sealed trait AbstractionOne {
    val events: Set[EventType]
    def doAbstraction(eventType: EventType): Option[AbstractionOne] = if (events.contains(eventType)) Some(this) else None
    def doAbstraction(eventType: String): Option[AbstractionOne] = if (events.exists(_.toString == eventType)) Some(this) else None
  }
  case object AbstractionOne {
    val abstractions = List(ReadEvents, WriteEvents, ProcessTreeEvents, ExecutionEvents, MemoryMap, DeleteEvents, AddObjectAttribute, CreateObject, FlowsTo, Update, FileSystemEvents, FileSystemEvents, NetworkManagementEvents, SystemManagementEvents, UserEvents, NonspecificEvents, DiscardedEvents)
    def go(eventType: EventType): Option[AbstractionOne] = abstractions.flatMap(_.doAbstraction(eventType)).headOption
    def go(eventType: String): Option[AbstractionOne] = abstractions.flatMap(_.doAbstraction(eventType)).headOption
  }
  case object ReadEvents extends AbstractionOne {
    val events: Set[EventType] = Set(
      EVENT_READ,
      EVENT_RECVFROM, EVENT_RECVMSG
//        EVENT_MMAP
    )
  }
  case object WriteEvents extends AbstractionOne {
    val events: Set[EventType] = Set(
      EVENT_WRITE,
      EVENT_SENDTO, EVENT_SENDMSG
//        EVENT_MMAP
    )
  }
  case object ProcessTreeEvents extends AbstractionOne {
    val events: Set[EventType] = Set(
      EVENT_FORK,
      EVENT_MODIFY_PROCESS,
      EVENT_EXIT,
      EVENT_CLONE,
      EVENT_CREATE_THREAD,
      EVENT_STARTSERVICE
    )
  }
  case object ExecutionEvents extends AbstractionOne {
    val events: Set[EventType] = Set(EVENT_EXECUTE, EVENT_LOADLIBRARY)
  }
  case object MemoryMap extends AbstractionOne {
    val events: Set[EventType] = Set(EVENT_MMAP)        // Needs further thought about the implications.)
  }
  case object DeleteEvents extends AbstractionOne {
    val events: Set[EventType] = Set(EVENT_UNLINK, EVENT_TRUNCATE)
  }
  case object AddObjectAttribute extends AbstractionOne {
    val events: Set[EventType] = Set(EVENT_ADD_OBJECT_ATTRIBUTE)
  }
  case object CreateObject extends AbstractionOne {
    val events: Set[EventType] = Set(EVENT_CREATE_OBJECT)
  }
  case object FlowsTo extends AbstractionOne {
    val events: Set[EventType] = Set(EVENT_FLOWS_TO)    // Needs further thought about the implications.
  }
  case object Update extends AbstractionOne {
    val events: Set[EventType] = Set(EVENT_UPDATE)
  }
  case object FileSystemEvents extends AbstractionOne {
    val events: Set[EventType] = Set(
      EVENT_MOUNT,
      EVENT_UMOUNT,
      EVENT_CHECK_FILE_ATTRIBUTES,
      EVENT_MODIFY_FILE_ATTRIBUTES,    // Needs further thought about the implications.
      EVENT_RENAME,
      EVENT_OPEN,
      EVENT_CLOSE,
      EVENT_LINK,
      EVENT_FCNTL, EVENT_DUP
    )
  }
  case object NetworkManagementEvents extends AbstractionOne {
    val events: Set[EventType] = Set(
      EVENT_ACCEPT,
      EVENT_CONNECT,
      EVENT_BIND,
      EVENT_READ_SOCKET_PARAMS,
      EVENT_WRITE_SOCKET_PARAMS
    )
  }
  case object SystemManagementEvents extends AbstractionOne {
    val events: Set[EventType] = Set(
      EVENT_BOOT,
      EVENT_MPROTECT,    // Needs further thought about the implications.
      EVENT_SHM,         // Needs further thought about the implications.
      EVENT_LOGCLEAR,
      EVENT_SERVICEINSTALL
    )
  }
  case object UserEvents extends AbstractionOne {
    val events: Set[EventType] = Set(
      EVENT_CHANGE_PRINCIPAL,
      EVENT_LOGIN,
      EVENT_LOGOUT
    )
  }
  case object NonspecificEvents extends AbstractionOne {
    val events: Set[EventType] = Set(
      EVENT_BLIND,
      EVENT_OTHER
    )
  }
  case object DiscardedEvents extends AbstractionOne {
    val events: Set[EventType] = Set() // Throw away everything below...
    (
      EVENT_LSEEK,
      EVENT_WAIT,
      EVENT_UNIT,
      EVENT_SIGNAL,
      EVENT_CORRELATION,
      PSEUDO_EVENT_PARENT_SUBJECT
    )
  }


  def reduceByAbstraction(tree: TreeRepr): TreeRepr = {
    def mergeChildrenByAbstraction(tree: TreeRepr): TreeRepr =
      tree.copy(children = tree.children.foldLeft(Set.empty[TreeRepr]){ case (acc, child) =>
        AbstractionOne.go(child.key).fold(acc + child){ abs => acc.find(_.key == abs.toString).fold(acc + child.copy(key = abs.toString)){ foundAbsTree =>
          (acc - foundAbsTree) + foundAbsTree.merge(child.copy(key = abs.toString)) } }
      })
    mergeChildrenByAbstraction(tree.copy(children = tree.children.map(t => reduceByAbstraction(t))))
  }

  def reduceBySameCounts(tree: TreeRepr): TreeRepr = {
    def mergeSameCountChildren(tree: TreeRepr): TreeRepr =
      tree.copy(children = tree.children.groupBy(_.count).map{
        case (_, ts) =>
          val merged = ts.foldLeft(Set.empty[ExtractedValue] -> TreeRepr.empty){ case (a,b) => (a._1 + b.key) -> b.merge(a._2, ignoreKeys = true)}
          val mergedKey =
            if (merged._1.size == 1) merged._1.head
            else if (merged._1.size <= 5)
              Try(merged._1.toList.sorted.mkString("[",", ","]")).getOrElse{
                println(s"\n\nWeird failing case with null value?!? => $merged\n\n"); "<weird_null_value!>"
              }
            else {
              val items = merged._1.toList.sorted
              s"${items.size} items like: ${Try(items.take(2).:+("...").++(items.reverse.take(2)).mkString("[",", ","]")).getOrElse{
                println(s"\n\nWeird failing case with null value?!? => $merged\n\n"); "<weird_null_value!>"
              }}"
            }
          mergedKey -> merged._2
      }.toSet[(ExtractedValue, TreeRepr)].map(c => c._2.copy(key = c._1))
      )
    mergeSameCountChildren(tree.copy(children = tree.children.map(t => reduceBySameCounts(t))))
  }

//  def summarize(tree: TreeRepr): TreeRepr =
//    reduceBySameCounts(reduceByAbstraction(tree.withoutQNodes).collapseUnitaryPaths()).collapseUnitaryPaths().renormalizeProbs
  // Consider: repeatedly call `reduceTreeBySameCounts` and `mergeChildrenByAbstraction` until no change



  def dummyTree(processName: String, hostName: Option[HostName], pid: Option[Int]) = Future.successful(
    TreeRepr(0, hostName.getOrElse("no_host"), 1, 1, 1, Set(
      TreeRepr(1, processName, 1, 1, 1, Set(
        TreeRepr(2, pid.map(p => s"PID_$p").getOrElse("NoPID"), 1, 1, 1, Set.empty)
      ))
    ))
  )


  // In the case of trying to summarize from the "BetweenHosts" PPM tree set: query all hosts and merge the results.
//  val allHostPossibilities = Application.ppmManagers.keySet - Application.hostNameForAllHosts   // AdaptConfig.ingestConfig.hosts.map(_.hostName)


  def summarize(processUuid: AdmUUID): Future[String] = {
    implicit val timeout = Timeout(180.12345 seconds)

    //  cadets_6709fef0-ffe7-3e84-91d2-65de4770dd74   did_write  did_execute

    val pidQuery = s"g.V(${processUuid.rendered}).values('cid')"
    def pidResults = (Application.uiDBInterface ? RawQuery(pidQuery))
      .mapTo[Future[Stream[Any]]].flatten.map {
      _.flatMap {
        case s: Int => List(s)
        case _ => Nil
      }.headOption
    }

    val thisProcessNameQuery = s"g.V(${processUuid.rendered}).outLimit('path', 20).values('path').limit(20)"
    def thisProcessNameResults = (Application.uiDBInterface ? RawQuery(thisProcessNameQuery))
      .mapTo[Future[Stream[Any]]].flatten.map {
      _.flatMap {
        case s: String => List(s)
        case _ => Nil
      }.toSet
    }

    val parentSubjectQuery = s"g.V(${processUuid.rendered}).outLimit('parentSubject', 1).outLimit('path', 20).values('path').limit(20)"
    def parentSubjectResults = (Application.uiDBInterface ? RawQuery(thisProcessNameQuery))
      .mapTo[Future[Stream[Any]]].flatten.map {
        _.flatMap {
          case s: String => List(s)
          case _ => Nil
        }.toSet
      }

    val childSubjectsQuery = s"g.V(${processUuid.rendered}).inLimit('parentSubject', 100).outLimit('path', 10).values('path').limit(100)"
    def childSubjectNamesResults = (Application.uiDBInterface ? RawQuery(childSubjectsQuery))
      .mapTo[Future[Stream[Any]]].flatten.map {
      _.flatMap {
        case s: String => List(s)
        case _ => Nil
      }.toSet
    }

    val writingFileQuery = s"g.V(${processUuid.rendered}).outLimit('did_write', 100).outLimit('path', 10).values('path').limit(100)"
    def writingFileResults = (Application.uiDBInterface ? RawQuery(writingFileQuery))
      .mapTo[Future[Stream[Any]]].flatten.map {
      _.flatMap {
        case s: String => List(s)
        case _ => Nil
      }.toSet
    }

    val readingFileQuery = s"g.V(${processUuid.rendered}).outLimit('did_read', 100).outLimit('path', 10).values('path').limit(100)"
    def readingFileResults = (Application.uiDBInterface ? RawQuery(readingFileQuery))
      .mapTo[Future[Stream[Any]]].flatten.map {
      _.flatMap {
        case s: String => List(s)
        case _ => Nil
      }.toSet
    }

    val writingNetFlowQuery = s"g.V(${processUuid.rendered}).outLimit('did_write', 100).valueMap('remoteAddress', 'remotePort').limit(100)"
    def writingNetFlowResults = (Application.uiDBInterface ? RawQuery(writingNetFlowQuery))
      .mapTo[Future[Stream[Map[String, Any]]]].flatten.map { stream =>
        stream.flatMap {
          case m: Map[String, Option[Any]] @unchecked => for {
            remoteAddressOpt <- m.get("remoteAddress")
            remotePortOpt <- m.get("remotePort")
          } yield {
            remoteAddressOpt.getOrElse("unknown") -> remotePortOpt.getOrElse("unknown").toString
          }
          case _ => None
        }.toList
      }

    val readingNetFlowQuery = s"g.V(${processUuid.rendered}).outLimit('did_read', 100).valueMap('remoteAddress', 'remotePort').limit(100)"
    def readingNetFlowResults = (Application.uiDBInterface ? RawQuery(readingNetFlowQuery))
      .mapTo[Future[Stream[Map[String, Any]]]].flatten.map { stream =>
        stream.flatMap {
          case m: Map[String, Option[Any]] @unchecked => for {
            remoteAddressOpt <- m.get("remoteAddress")
            remotePortOpt <- m.get("remotePort")
          } yield {
            remoteAddressOpt.getOrElse("unknown") -> remotePortOpt.getOrElse("unknown").toString
          }
          case _ => None
        }.toList
      }


    val wroteOverNetflowToProcessQuery = s"g.V(${processUuid.rendered}).inLimit('did_read_over_network', 100).as('proc').id().as('id').select('proc').outLimit('path', 10).values('path').as('pth').select('id','path').limit(100)"
    def wroteOverNetflowToProcessResults = (Application.uiDBInterface ? RawQuery(wroteOverNetflowToProcessQuery))
      .mapTo[Future[Stream[Any]]].flatten.map { stream =>
      val uniquePairs = stream.flatMap {
        case m: Map[String, Any] @unchecked => for {
          id <- m.get("id")
          procPath <- m.get("path")
        } yield { id.asInstanceOf[AdmUUID].rendered -> procPath.toString }
        case _ => Nil
      }
      uniquePairs.toSet[(String, String)].groupBy(_._1).mapValues(_.map(_._2).toList.sorted.mkString(",")).toList  // Goal: List( "id.rendered" -> "multiple,process,names,as,one,string" )
    }

    val readOverNetflowFromProcessQuery = s"g.V(${processUuid.rendered}).outLimit('did_read_over_network', 100).as('proc').id().as('id').select('proc').outLimit('path', 10).values('path').as('pth').select('id','path').limit(100)"
    def readOverNetflowFromProcessResults = (Application.uiDBInterface ? RawQuery(readOverNetflowFromProcessQuery))
      .mapTo[Future[Stream[Any]]].flatten.map { stream =>
        val uniquePairs = stream.flatMap {
          case m: Map[String, Any] @unchecked => for {
            id <- m.get("id")
            procPath <- m.get("path")
          } yield { id.asInstanceOf[AdmUUID].rendered -> procPath.toString }
          case _ => Nil
        }
        uniquePairs.toSet[(String, String)].groupBy(_._1).mapValues(_.map(_._2).toList.sorted.mkString(",")).toList  // Goal: List( "id.rendered" -> "multiple,process,names,as,one,string" )
      }


    for {
      pid <- pidResults
      thisProcName <- thisProcessNameResults
      parentSubject <- parentSubjectResults
      childSubjectNames <- childSubjectNamesResults
      readFromFiles <- readingFileResults
      wroteToFiles <- writingFileResults
      readFromNets <- readingNetFlowResults
      wroteToNets <- writingNetFlowResults
      readFromRemoteProcess <- readOverNetflowFromProcessResults
      wroteToRemoteProcess <- wroteOverNetflowToProcessResults
    } yield {
      s"""Process: ${thisProcName.toList.sorted match {case Nil => "(unnamed)"; case l => l.mkString(",")}}${pid.fold("")(p => s"  PID: $p")}
         |Child of: ${parentSubject.toList.sorted match {case Nil => "(unknown)"; case l => l.mkString(",")}}
         |Has child processes: ${childSubjectNames.toList.sorted match {case Nil => "(none)"; case l => l.mkString(", ")}}
         |Read from files: ${readFromFiles.toList.sorted match {case Nil => "(none)"; case l => l.mkString(", ")}}
         |Wrote to files: ${wroteToFiles.toList.sorted match {case Nil => "(none)"; case l => l.mkString(", ")}}
         |Read from network: ${readFromNets match {case Nil => "(none)"; case r => r.map(n => s"${n._1}:${n._2}").sorted.mkString(", ")}}
         |Wrote to network: ${wroteToNets match {case Nil => "(none)"; case r => r.map(n => s"${n._1}:${n._2}").sorted.mkString(", ")}}
         |Received from remote process: ${readFromRemoteProcess match {case Nil => "(none)"; case r => r.map(x => s"${x._1} : ${x._2}").mkString(",  ")}}
         |Sent to remote process: ${wroteToRemoteProcess match {case Nil => "(none)"; case r => r.map(x => s"${x._1} : ${x._2}").mkString(",  ")}}
       """.stripMargin
    }
  }.recoverWith{ case e => e.printStackTrace(); Future.successful(s"Failed to get process details for ${processUuid.rendered} because of error: ${e.getMessage}")}


//  def summarize(processName: String, hostName: Option[HostName], pid: Option[Int]): Future[TreeRepr] = {
//
//    dummyTree(processName, hostName, pid)
//
////    implicit val timeout = Timeout(30 seconds)
////    (hostName, hostName contains Application.hostNameForAllHosts) match {
////      case (Some(hn), false) =>
////        Application.ppmManagers(hn).ppmNodeActorBeginGetTreeRepr("SummarizedProcessActivity", List(processName) ++ pid.map(_.toString).toList)
//////          .mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity)
////          .map{r => summarize(r.repr) }
////      case x => // None or Some(BetweenHosts)
////        allHostPossibilities.foldLeft(Future.successful(List.empty[(HostName, TreeRepr)])) { case (accF, aHost) =>
////          accF.flatMap(acc =>
////            Application.ppmManagers(aHost).ppmNodeActorBeginGetTreeRepr("SummarizedProcessActivity", List(processName) ++ pid.map(_.toString).toList)
//////              .mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity)
////              .map { r => (aHost -> summarize(r.repr)) :: acc }  // Summarize before merging.
////          )
////        }.map(trees => TreeRepr.fromNamespacedChildren("SummarizedFromHosts", trees.toMap))
////    }
//  }

//  def fullTree(processName: String, hostName: Option[HostName], pid: Option[Int]): Future[TreeRepr] = {
//
//    dummyTree(processName, hostName, pid)
//
////    implicit val timeout = Timeout(30 seconds)
////    (hostName, hostName contains Application.hostNameForAllHosts) match {
////      case (Some(hn), false) =>
////        Application.ppmManagers(hn).ppmNodeActorBeginGetTreeRepr("SummarizedProcessActivity", List(processName) ++ pid.map(_.toString).toList)
//////          .mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity)
////          .map { r => r.repr }
////      case _ => // None or Some(BetweenHosts)
////        allHostPossibilities.foldLeft(Future.successful(List.empty[(HostName, TreeRepr)])) { case (accF, aHost) =>
////          accF.flatMap(acc =>
////            Application.ppmManagers(aHost).ppmNodeActorBeginGetTreeRepr("SummarizedProcessActivity", List(processName) ++ pid.map(_.toString).toList)
//////              .mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity)
////              .map { r => (aHost -> r.repr) :: acc }
////          )
////        }.map(trees => TreeRepr.fromNamespacedChildren("FullTreeFromHosts", trees.toMap))
////    }
//  }

//  def summarizableProcesses: Future[Map[HostName, TreeRepr]] = {
//
//    dummyTree("fakeprocess", Some("SomeHostName"), Some(12345)).map(t => Map(t.key -> t))
//
////    implicit val timeout = Timeout(30 seconds)
////    Future.sequence(
////      Application.ppmManagers.map { case (hostName, mgr) if hostName != Application.hostNameForAllHosts =>
////        mgr.ppmNodeActorBeginGetTreeRepr("SummarizedProcessActivity")    // TODO: Find another way!!!!!!!!
//////        .mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity)
////          .map {result => hostName -> result.repr.truncate(1).withoutQNodes}
////      }
////    ).map(_.toMap)
//  }

//  def mostNovelActions(maxCount: Int, processName: String, hostName: HostName, pid: Option[Int] = None): Future[List[String]] = {
//
//    Future.successful(List("Nothing interesting to report", "...except for this!"))
//
//
////    implicit val timeout = Timeout(30 seconds)
////    if (Application.hostNameForAllHosts == hostName)
////      allHostPossibilities.foldLeft(Future.successful(List.empty[HostName])) { case (accF, aHost) =>
////        accF.flatMap(acc =>
////          Application.ppmManagers(aHost).ppmNodeActorBeginGetTreeRepr("SummarizedProcessActivity", List(processName) ++ pid.map(_.toString).toList)
//////            .mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity)
////            .map {
////              _.repr.withoutQNodes.renormalizeProbs.mostNovelKeys(maxCount).map(ex => s"$aHost: $ex") ++ acc
////            }
////        )
////      }
////    else
////      (Application.ppmManagers(hostName).ppmNodeActorBeginGetTreeRepr("SummarizedProcessActivity", List(processName) ++ pid.map(_.toString).toList))
//////      .mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity)
////        .map {
////          _.repr.withoutQNodes.renormalizeProbs.mostNovelKeys(maxCount).map(ex => s"$hostName: $ex")
////        }
//  }


  // Overall strategy: systematically remove/collapse items from a TreeRepr into single lines until the the limit is reached, and gloss the remainder.

  /*
   * Interesting things:
   *   - Long path lengths from a leaf upward where each hop has a count = 1  (or identical counts)
   *   - Comparatively low local probability
   *   - Low global probability after renormalizing to only the branch under consideration.
   *   - Common sub-trees in different branches. (suggests pivoting the tree)
   *   - Common subset trees in different branches. (suggests collapsing?)
   *   - Alarms that were produced...?
   *   - Consider keeping timestamps.
   */

  /*
   * Reduction categories:
   *   - paths into common parent directories. Need to distinguish types for each level of the tree => unify strings iff types are all directories/path components.
   *   - known process names into "purposes"...?  (e.g. web browsing, email)
   *   - Events into "higher-level" notions:
   *     - READ / WRITE => "File IO"
   *     - SEND / RCV / ACCEPT / BIND => "Network Communication"
   *     - LOADLIBRARY / MMAP / MPROTECT => "Memory Load"
   *     - ...
   *     - everything else => Discard
   *   - Events that signal information flow:
   *     - READ / RCV / ACCEPT / LOADLIBRARY / MMAP / etc. => "incoming data"
   *     - WRITE / SEND / etc. => "outgoing data"
   *   - Shape (distribution types) of local prob. among sibling nodes. e.g. uniform dist., uniform w/ outlier, single item, power law, 2-clear-categories, 3-clear-categories.
   */

  /**
    * Types:
    *   - Event = IO | Net Comm | ...
    *   - Process = web browser | email client | server | document editing | control (shells / interpreters)  =  client program | system program
    *   - Path = directory | filename | file extension
    *   - URLs …?
    */

  /**
    * Clustering considerations:
    *   - One dimension for each level of the tree. Would need to ensure there is only one variable length discriminator and it is last. Would need to handle null values for the variable length discriminator values.
    *   - One dimension for each type of discriminator. Would need to encode types together with discriminator values.
    *   - Clustering exclusively at a single level of a tree. e.g. apply clustering to only the children of a single node at a time; probably for divining shape.
    *
    *   Consider: Mean-shift clustering (to discover centroid counts & approximate centers) + gaussian mixture models (to learn model params and cluster membership)
    */

//  trait Child
//  trait Shape { def assessScore(children: Set[Child]): Float } //  = PowerLaw,
//  val shapes: List[Shape] = ???
//  def assessShapeScores(children: Set[Child]): List[(Shape, Float)] = shapes.map(s => s -> s.assessScore(children))
//  def bestFitShape(children: Set[Child]): Shape = assessShapeScores(children).sortBy(_._2).reverse.head._1


  // TODO: Next steps:
  //  X- implement removal of _?_ nodes
  //  X- implement tree collapse function (reduces all nodes which have only one child)
  //  X- implement renormalize function
  //  X- implement extraction of single item from TreeRepr (the most novel)
  //  X- implement subtree-collapse/summarization by the "Types" mentioned above.
  //    X- Simple version: assume subtree can be summarized in a single output line.
  //    - Complex version: allow for one subtree to summarized with multiple lines.. perhaps recursively.
  //  - implement a chooser which extracts N particular items, then summarizes the remainder to fit into the X total summary parameter constraint.
}