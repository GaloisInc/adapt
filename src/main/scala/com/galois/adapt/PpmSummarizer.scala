package com.galois.adapt

import akka.util.Timeout
import akka.pattern.ask
import com.galois.adapt.AdaptConfig.HostName
import com.galois.adapt.NoveltyDetection.ExtractedValue
import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.duration._


object PpmSummarizer {
  import cdm19._
  import Application.system.dispatcher

  sealed trait AbstractionOne {
    val events: Set[EventType]
    def doAbstraction(eventType: EventType): Option[AbstractionOne] = if (events.contains(eventType)) Some(this) else None
    def doAbstraction(eventType: String): Option[AbstractionOne] = if (events.exists(_.toString == eventType)) Some(this) else None
  }
  case object AbstractionOne {
    val abstractions = List(ReadEvents, WriteEvents, ProcessTreeEvents, ExecutionEvents, MemoryMap, Unlink, AddObjectAttribute, CreateObject, FlowsTo, Update, FileSystemEvents, FileSystemEvents, NetworkManagementEvents, SystemManagementEvents, UserEvents, NonspecificEvents, DiscardedEvents)
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
      EVENT_SENDTO, EVENT_SENDMSG,
      EVENT_TRUNCATE
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
  case object Unlink extends AbstractionOne {
    val events: Set[EventType] = Set(EVENT_UNLINK)
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

  def summarize(tree: TreeRepr): TreeRepr =
    reduceBySameCounts(reduceByAbstraction(tree.withoutQNodes).collapseUnitaryPaths()).collapseUnitaryPaths().renormalizeProbs
  // Consider: repeatedly call `reduceTreeBySameCounts` and `mergeChildrenByAbstraction` until no change

  def summarize(processName: String, hostName: Option[HostName], pid: Option[Int]): Future[TreeRepr] = {
    implicit val timeout = Timeout(30 seconds)
    (Application.ppmManagerActors(hostName.getOrElse(Application.hostNameForAllHosts)) ? PpmNodeActorBeginGetTreeRepr("SummarizedProcessActivity", List(processName) ++ pid.map(_.toString).toList))
      .mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity).map{r => summarize(r.repr) }
  }

  def fullTree(processName: String, hostName: Option[HostName], pid: Option[Int]): Future[TreeRepr] = {
    implicit val timeout = Timeout(30 seconds)
    (Application.ppmManagerActors(hostName.getOrElse(Application.hostNameForAllHosts)) ? PpmNodeActorBeginGetTreeRepr("SummarizedProcessActivity", List(processName) ++ pid.map(_.toString).toList))
      .mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity).map{r => r.repr }
  }

  def summarizableProcesses: Future[Map[HostName, TreeRepr]] = {
    implicit val timeout = Timeout(30 seconds)
    Future.sequence(
      Application.ppmManagerActors.map { case (hostName, ref) => (ref ? PpmNodeActorBeginGetTreeRepr("SummarizedProcessActivity"))
        .mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity).map {result => hostName -> result.repr.truncate(1).withoutQNodes}
      }
    ).map(_.toMap)
  }

  def mostNovelActions(maxCount: Int, processName: String, hostName: HostName, pid: Option[Int] = None): Future[List[String]] = {
    implicit val timeout = Timeout(30 seconds)
    (Application.ppmManagerActors(hostName) ? PpmNodeActorBeginGetTreeRepr("SummarizedProcessActivity", List(processName) ++ pid.map(_.toString).toList))
      .mapTo[Future[PpmNodeActorGetTreeReprResult]].flatMap(identity).map{r => r.repr.withoutQNodes.renormalizeProbs.mostNovelKeys(maxCount) }
  }


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