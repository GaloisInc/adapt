package com.galois.adapt

import java.util.UUID

import akka.stream.scaladsl.{Flow, Source}
import com.galois.adapt.FlowComponents.predicateTypeLabeler
import com.galois.adapt.cdm17.{CDM17, EVENT_ACCEPT, EVENT_CONNECT, EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG, Event, EventType, NetFlowObject}
import org.mapdb.{DB, HTreeMap}
import FlowComponents._
import akka.stream.{DelayOverflowStrategy, OverflowStrategy}
import cdm17._
import com.typesafe.config.ConfigFactory

import scala.collection.immutable.SortedSet
import scala.collection.mutable.{Map => MutableMap, Set => MutableSet, SortedSet => MutableSortedSet}
import scala.util.Random
import scala.concurrent.duration._

object ProcessStream {

  val config = ConfigFactory.load()

  def processFeatureGenerator(commandSource: Source[ProcessingCommand,_], db: DB) = {
//    val dbMap = db.hashMap("fileFeatureGenerator_" + Random.nextLong()).createOrOpen().asInstanceOf[HTreeMap[UUID, MutableSet[Event]]]

    Flow[(String, UUID, Event, CDM17)]
      .filter(x => List("NetFlowObject", "FileObject", "Subject", "MemoryObject").contains(x._1))
      .groupBy(Int.MaxValue, _._3.subjectUuid)
//      .merge(commandSource)
      .statefulMapConcat[((UUID, MutableSet[Event]), MutableMap[NetFlowUUID, MutableSet[(Event,NetFlowObject)]], MutableMap[FileUUID, MutableSet[(Event, FileObject)]], MutableMap[MemoryUUID, MutableSet[(Event,MemoryObject)]])] { () =>
        var processUuidOpt: Option[UUID] = None
        val eventsToThisProcess = MutableSet.empty[Event]
//        val allEventsByThisProcess = mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))
//        val fileUuids = MutableSet.empty[UUID]
        val fileEvents = MutableMap.empty[FileUUID, MutableSet[(Event, FileObject)]]
//        val netFlowUuids = MutableSet.empty[UUID]
        val netFlowEvents = MutableMap.empty[NetFlowUUID, MutableSet[(Event,NetFlowObject)]]
        val memoryEvents = MutableMap.empty[MemoryUUID, MutableSet[(Event,MemoryObject)]]

        var shouldStore = true
        var cleanupCounts = 0


        {
          case Tuple4("Subject", uuid: ProcessUUID, event: Event, _: CDM17) =>
            if (processUuidOpt.isEmpty) processUuidOpt = Some(uuid)
//            if (event.subject == processUuidOpt.get) allEventsByThisProcess += event
            if (shouldStore) {
              eventsToThisProcess += event
              cleanupCounts = 0
            }
            if (eventsToThisProcess.size > config.getInt("adapt.runtime.throwawaythreshold") && shouldStore) {
              println(s"Subject in process collector is over the event limit: $uuid")
              shouldStore = false
              eventsToThisProcess.clear()
            }
//            List.empty
            List(((processUuidOpt.get, eventsToThisProcess), netFlowEvents, fileEvents, memoryEvents))

          case Tuple4("NetFlowObject", uuid: NetFlowUUID, event: Event, cdm: CDM17) =>
            if (processUuidOpt.isEmpty) processUuidOpt = Some(event.subjectUuid)
//            netFlowEvents(uuid) = netFlowEvents.getOrElse(uuid, MutableSortedSet.empty[(Event,NetFlowObject)](Ordering.by(_._1.timestampNanos))) +
//              (event -> cdm.asInstanceOf[NetFlowObject])
            if (shouldStore) {
              netFlowEvents.getOrElse(uuid, MutableSet.empty[(Event,NetFlowObject)]) += (event -> cdm.asInstanceOf[NetFlowObject])
              cleanupCounts = 0
            }
            if (netFlowEvents.values.map(_.size).sum > config.getInt("adapt.runtime.throwawaythreshold") && shouldStore) {
              println(s"NetFlowObject in process collector is over the event limit: $uuid")
              shouldStore = false
              netFlowEvents.clear()
            }
//            List.empty
            List(((processUuidOpt.get, eventsToThisProcess), netFlowEvents, fileEvents, memoryEvents))

          case Tuple4("MemoryObject", uuid: MemoryUUID, event: Event, cdm: CDM17) =>
            if (processUuidOpt.isEmpty) processUuidOpt = Some(event.subjectUuid)
//            memoryEvents(uuid) = memoryEvents.getOrElse(uuid, MutableSortedSet.empty[(Event,MemoryObject)](Ordering.by(_._1.timestampNanos))) +
//              (event -> cdm.asInstanceOf[MemoryObject])
            if (shouldStore) {
              memoryEvents.getOrElse(uuid, MutableSet.empty[(Event,MemoryObject)]) += (event -> cdm.asInstanceOf[MemoryObject])
              cleanupCounts = 0
            }
            if (memoryEvents.values.map(_.size).sum > config.getInt("adapt.runtime.throwawaythreshold") && shouldStore) {
              println(s"MemoryObject in process collector is over the event limit: $uuid")
              shouldStore = false
              memoryEvents.clear()
            }
//            List.empty
            List(((processUuidOpt.get, eventsToThisProcess), netFlowEvents, fileEvents, memoryEvents))

          case Tuple4("FileObject", uuid: FileUUID, event: Event, cdm: CDM17) =>
            if (processUuidOpt.isEmpty) processUuidOpt = Some(event.subjectUuid)
//            fileEvents(uuid) = fileEvents.getOrElse(uuid, MutableSortedSet.empty[(Event,FileObject)](Ordering.by(_._1.timestampNanos))) +
//              (event -> cdm.asInstanceOf[FileObject])
            if (shouldStore) {
              fileEvents.getOrElse(uuid, MutableSet.empty[(Event,FileObject)]) += (event -> cdm.asInstanceOf[FileObject])
              cleanupCounts = 0
            }
            if (fileEvents.values.map(_.size).sum > config.getInt("adapt.runtime.throwawaythreshold") && shouldStore) {
              println(s"FileObject in process collector is over the event limit: $uuid")
              shouldStore = false
              fileEvents.clear()
            }
//            List.empty
            List(((processUuidOpt.get, eventsToThisProcess), netFlowEvents, fileEvents, memoryEvents))

//          case CleanUp =>
//            List.empty
//
//          case EmitCmd =>
//            List.empty

//          case CleanUp =>
////            if (fileEvents.nonEmpty) {
////              val mergedEvents = MutableMap.empty[UUID,mutable.SortedSet[Event]]
////              fileEvents.foreach { case (u, es) =>
////                mergedEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) ++ es
////              }
////              dbMap.putAll(mergedEvents.asJava)
////              fileUuids ++= fileEvents.keySet
////              fileEvents.clear()
////            }
////            if (netFlowEvents.nonEmpty) {
////              val mergedEvents = MutableMap.empty[UUID,mutable.SortedSet[Event]]
////              netFlowEvents.foreach { case (u, es) =>
////                mergedEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) ++ es
////              }
////              dbMap.putAll(mergedEvents.asJava)
////              netFlowUuids ++= netFlowEvents.keySet
////              netFlowEvents.clear()
////            }
////            if (eventsToThisProcess.nonEmpty) {
////              val mergedEvents = dbMap.getOrDefault(processUuidOpt.get, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) ++ eventsToThisProcess
////              dbMap.put(processUuidOpt.get, mergedEvents)
////              eventsToThisProcess.clear()
////            }
////            and similar for all events BY this process
//            List.empty
//
//          case EmitCmd =>
////            fileUuids.foreach(u =>
////              fileEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) ++
////                fileEvents.getOrElse(u, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) )
////
////            netFlowUuids.foreach(u =>
////              netFlowEvents(u) = //dbMap.getOrDefault(u, mutable.SortedSet.empty[(Event,NetFlowObject)](Ordering.by(_._1.timestampNanos))) ++
////                netFlowEvents.getOrElse(u, mutable.SortedSet.empty[(Event,NetFlowObject)](Ordering.by(_._1.timestampNanos))) )
//
////            eventsToThisProcess ++= dbMap.getOrDefault(processUuidOpt.get, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos)))
//
//            List(((processUuidOpt.get, eventsToThisProcess), netFlowEvents.toSet, fileEvents.toSet, memoryEvents.toSet))
        }
      }
      .buffer(1, OverflowStrategy.dropHead)
      .delay(config.getInt("adapt.runtime.featureextractionseconds") seconds, DelayOverflowStrategy.backpressure)
      .map(t => (
        (t._1._1, MutableSortedSet(t._1._2.toList:_*)(Ordering.by(_.timestampNanos))),
        t._2.mapValues(s => SortedSet(s.toList:_*)(Ordering.by[(Event, NetFlowObject), Long](_._1.timestampNanos))).toSet,
        t._3.mapValues(s => SortedSet(s.toList:_*)(Ordering.by[(Event, FileObject), Long](_._1.timestampNanos))).toSet,
        t._4.mapValues(s => SortedSet(s.toList:_*)(Ordering.by[(Event, MemoryObject), Long](_._1.timestampNanos))).toSet))
      .mergeSubstreams
      .via(processFeatureExtractor)
  }



  val processEventTypes = EventType.values.toList

  val processFeatureExtractor = Flow[((ProcessUUID, MutableSortedSet[Event]), Set[(NetFlowUUID, SortedSet[(Event,NetFlowObject)])], Set[(FileUUID, SortedSet[(Event, FileObject)])], Set[(MemoryUUID, SortedSet[(Event,MemoryObject)])])]
    .mapConcat[(String, ProcessUUID, MutableMap[String,Any], Set[EventUUID])] { case ((processUuid, eventsDoneToThisProcessSet), netFlowEventSets, fileEventSets, memoryEventSets) =>
    val eventsDoneToThisProcessList = eventsDoneToThisProcessSet.toList
    val allProcessEventSet: MutableSortedSet[Event] = eventsDoneToThisProcessSet ++ netFlowEventSets.flatMap(_._2.map(_._1)) ++= fileEventSets.flatMap(_._2.map(_._1)) ++= memoryEventSets.flatMap(_._2.map(_._1))
    val allProcessEventList: List[Event] = allProcessEventSet.toList

    var allRelatedUUIDs = eventsDoneToThisProcessSet.flatMap(e => List(Some(e.uuid), e.predicateObject, e.predicateObject2, Some(e.subjectUuid)).flatten)


    val m = MutableMap.empty[String, Any]
//      m("countOfImmediateChildProcesses") = "TODO"                                        // TODO: needs process tree
//      m("countOfAllChildProcessesInTree") = "TODO"                                        // TODO: needs process tree
//      m("countOfUniquePortAccesses") = "TODO"                                             // TODO: needs pairing with NetFlows —— not just events!
    // TODO: consider emitting the collected Process Tree
    m("countOfDistinctMemoryObjectsMProtected") = allProcessEventSet.collect { case e if e.eventType == EVENT_MPROTECT && e.predicateObject.isDefined => e.predicateObject }.size
//      m("isProcessRunning_cmd.exe_or-powershell.exe_whileParentRunsAnotherExe") = "TODO"  // TODO: needs process tree
    m("countOfAllConnect+AcceptEventsToPorts22or443") =
      netFlowEventSets.toList.map(s => s._2.toList.collect{
        case (e,n) if List(EVENT_CONNECT, EVENT_ACCEPT).contains(e.eventType) &&
          (List(22,443).contains(n.localPort) || List(22,443).contains(n.remotePort)) => 1
      }.sum).sum
    m("countOfAllConnect+AcceptEventsToPortsOtherThan22or443") =
      netFlowEventSets.toList.map(s => s._2.toList.collect{
        case (e,n) if List(EVENT_CONNECT, EVENT_ACCEPT).contains(e.eventType) &&
          ( ! List(22,443).contains(n.localPort) || ! List(22,443).contains(n.remotePort)) => 1
      }.sum).sum

//      m("touchesAPasswordFile") = "TODO"                                                  // TODO: needs pairing with Files. Or does it? Path is probably on events.
    m("readsFromNetFlowThenWritesAFileThenExecutesTheFile") = netFlowEventSets.map(i => i._1 -> i._2.map(_._1)).flatMap(
      _._2.collect{ case e if e.subjectUuid == processUuid && List(EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG).contains(e.eventType) => e.timestampNanos }   // TODO: revisit these event types
    ).toList.sorted.headOption.exists(netFlowReadTime =>
      fileEventSets.exists(
        _._2.map(_._1).dropWhile(write =>
          write.eventType != EVENT_WRITE && write.timestampNanos > netFlowReadTime
        ).exists(ex => ex.eventType == EVENT_EXECUTE)
      )
    ): Boolean

    m("changesFilePermissionsThenExecutesIt") = allProcessEventList.dropWhile(_.eventType != EVENT_MODIFY_FILE_ATTRIBUTES).exists(_.eventType == EVENT_EXECUTE): Boolean
    m("executedThenImmediatelyDeletedAFile") = allProcessEventList.groupBy(_.predicateObject).-(None).values.exists(l => l.sortBy(_.timestampNanos).dropWhile(_.eventType != EVENT_EXECUTE).drop(1).headOption.exists(_.eventType == EVENT_UNLINK)): Boolean
    m("readFromNetFlowThenDeletedFile") = netFlowEventSets.map(i => i._1 -> i._2.map(_._1)).flatMap(
      _._2.collect{ case e if e.subjectUuid == processUuid && List(EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG).contains(e.eventType) => e.timestampNanos }   // TODO: revisit these event types
    ).toList.sorted.headOption.exists(netFlowReadTime =>
      fileEventSets.exists(
        _._2.map(_._1).exists(delete =>
          delete.eventType != EVENT_UNLINK && delete.timestampNanos > netFlowReadTime
        )
      )
    )

    // TODO: consider: process takes any local action after reading from NetFlow

    m("countOfDistinctFileWrites") = allProcessEventSet.collect { case e if e.eventType == EVENT_WRITE && e.predicateObject.isDefined => e.predicateObject }.size
//      m("countOfFileUploads") = "TODO"                                                    // TODO: needs pairing with Files (to ensure reads are from Files)
//      m("countOfFileDownloads") = "TODO"                                                  // TODO: needs pairing with Files (to ensure writes are to Files)
    m("isAccessingTempDirectory") = allProcessEventList.flatMap(e => List(e.predicateObjectPath, e.predicateObject2Path).flatten).exists(path => List("/tmp", "/temp", "\\temp").exists(tmp => path.toLowerCase.contains(tmp) || (path.toLowerCase.startsWith("c:\\") && ! path.drop(3).contains("\\") ) ))  // TODO: revisit the list of temp locations.
    m("thisProcessIsTheObjectOfACHANGE_PRINCIPALEvent") = eventsDoneToThisProcessList.exists(e => e.eventType == EVENT_CHANGE_PRINCIPAL): Boolean
    m("thisProcessIsTheObjectOfAMODIFY_PROCESSEvent") = eventsDoneToThisProcessList.exists(e => e.eventType == EVENT_MODIFY_PROCESS)
    m("totalBytesSentToNetFlows") = allProcessEventList.collect { case e if e.eventType == EVENT_SENDTO => e.size.getOrElse(0L)}.sum
    m("totalBytesReceivedFromNetFlows") = allProcessEventList.collect { case e if e.eventType == EVENT_RECVFROM => e.size.getOrElse(0L)}.sum

    m("commandLineStringOfInterest") = allProcessEventList.flatMap(e => List(e.predicateObjectPath, e.predicateObject2Path, e.name).flatten).exists(path =>
      List("putfile", "getfile", "execfile", "shell", "elevate", "refload", "screengrab", "netrecon", "recordaudio", "imagegrab",
        "keylogger", "kudu", "dropper", "dropbear", "net.exe", "ipconfig", "ifconfig", "netstat", "nmap", "whoami", "hostname", "powershell.exe", "cmd.exe")
        .exists(name => path.toLowerCase.contains(name) || (path.toLowerCase.contains("git") && path.toLowerCase.contains("commit")))
    ): Boolean

    m("mProtectWithAcceptOrConnect") = allProcessEventSet.exists(_.eventType == EVENT_MPROTECT) &&
      allProcessEventSet.exists(e => e.eventType == EVENT_ACCEPT || e.eventType == EVENT_CONNECT)

    m("powershellAndInboundNetflow") = allProcessEventSet.flatMap(e => List(e.predicateObjectPath, e.predicateObject2Path, e.name).flatten).exists(path =>
      path.toLowerCase.contains("powershell")
    ) && allProcessEventSet.exists(e => List(EVENT_ACCEPT, EVENT_RECVMSG, EVENT_RECVFROM).contains(e.eventType))

    m("totalUniqueCheckFileEvents") = allProcessEventSet.filter(_.eventType == EVENT_CHECK_FILE_ATTRIBUTES).flatMap(_.predicateObject).size
    processEventTypes.foreach( t =>
      m("count_"+ t.toString) = allProcessEventSet.count(_.eventType == t)
    )

    val viewDefinitions = Map(
      "Process Exec from Network" -> List("count_EVENT_EXECUTE", "readsFromNetFlowThenWritesAFileThenExecutesTheFile","executedThenImmediatelyDeletedAFile", "changesFilePermissionsThenExecutesIt")
    , "Process Directory Scan" -> List("count_EVENT_CHECK_FILE_ATTRIBUTES", "count_EVENT_OPEN", "totalUniqueCheckFileEvents")
    , "Process Netflow Events" -> List("count_EVENT_RECVFROM", "count_EVENT_RECVMSG", "count_EVENT_SENDMSG", "count_EVENT_SENDTO", "count_EVENT_WRITE", "count_EVENT_READ", "totalBytesReceivedFromNetFlows", "totalBytesSentToNetFlows")
    , "Process File Events" -> List("count_EVENT_CHECK_FILE_ATTRIBUTES", "count_EVENT_DUP", "count_EVENT_EXECUTE", "count_EVENT_LINK", "count_EVENT_LOADLIBRARY", "count_EVENT_LSEEK", "count_EVENT_MMAP", "count_EVENT_MODIFY_FILE_ATTRIBUTES", "count_EVENT_READ", "count_EVENT_WRITE", "count_EVENT_RENAME", "count_EVENT_TRUNCATE", "count_EVENT_UNLINK", "count_EVENT_UPDATE", "changesFilePermissionsThenExecutesIt", "countOfDistinctFileWrites", "isAccessingTempDirectory")
    , "Process Memory Events" -> List("countOfDistinctMemoryObjectsMProtected", "count_EVENT_LOADLIBRARY", "count_EVENT_MMAP", "count_EVENT_MPROTECT", "count_EVENT_UPDATE")
    , "Process Process Events" -> List("count_EVENT_CHANGE_PRINCIPAL", "thisProcessIsTheObjectOfACHANGE_PRINCIPALEvent", "count_EVENT_CLONE", "count_EVENT_FORK", "count_EVENT_LOGCLEAR", "count_EVENT_LOGIN", "count_EVENT_LOGOUT", "count_EVENT_MODIFY_PROCESS", "count_EVENT_SHM", "count_EVENT_SIGNAL", "count_EVENT_STARTSERVICE", "isAccessingTempDirectory")

    // An alarm must have a name beginning with "ALARM", and contain exactly one boolean feature. See the EmitCmd case in anomalyScoreCalculator.
    , "ALARM: Process Executed Then Deleted a File" -> List("executedThenImmediatelyDeletedAFile")
    , "ALARM: Process Read NetFlow Wrote and Exec File" -> List("readsFromNetFlowThenWritesAFileThenExecutesTheFile")
    , "ALARM: Process Changed File Perms Then Exec" -> List("changesFilePermissionsThenExecutesIt")
    , "ALARM: Changed Principal" -> List("thisProcessIsTheObjectOfACHANGE_PRINCIPALEvent")
    , "ALARM: Command Line String of Interest" -> List("commandLineStringOfInterest")
    , "ALARM: MProtect with Accept or Connect" -> List("mProtectWithAcceptOrConnect")
    , "ALARM: Powershell with Inbound NetFlow" -> List("powershellAndInboundNetflow")
    )

//    // TODO: REMOVE:
//    val req = viewDefinitions.values.flatten.toSet.forall(m.keySet.contains)
//    if (! req) println("DANGER: " + viewDefinitions.values.flatten.toSet[String].map(x => x -> m.keySet.contains(x)).filter(x => ! x._2))

    viewDefinitions.toList.map { case (name, columnList) =>
      (name, processUuid, m.filter(t => columnList.contains(t._1)), allRelatedUUIDs.toSet)
    } ++ List(("All Process Features", processUuid, m, allRelatedUUIDs.toSet))
  }
}
