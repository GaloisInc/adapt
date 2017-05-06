package com.galois.adapt

import java.util.UUID

import akka.stream.scaladsl.{Flow, Source}
import com.galois.adapt.FlowComponents.predicateTypeLabeler
import com.galois.adapt.cdm17.{CDM17, EVENT_ACCEPT, EVENT_CONNECT, EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG, Event, EventType, NetFlowObject}
import org.mapdb.{DB, HTreeMap}
import FlowComponents._
import cdm17._
import scala.collection.mutable
import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
import scala.util.Random

object ProcessStream {

  def processFeatureGenerator(commandSource: Source[ProcessingCommand,_], db: DB) = {
    val dbMap = db.hashMap("fileFeatureGenerator_" + Random.nextLong()).createOrOpen().asInstanceOf[HTreeMap[UUID,mutable.SortedSet[Event]]]

    predicateTypeLabeler(commandSource, db)
      //      .filter(x => List("NetFlowObject", "FileObject", "Subject").contains(x._1))
      .groupBy(Int.MaxValue, _._3.subjectUuid)
      .merge(commandSource)
      .statefulMapConcat[((UUID, mutable.SortedSet[Event]), Set[(UUID, mutable.SortedSet[(Event,NetFlowObject)])], Set[(UUID, mutable.SortedSet[Event])])] { () =>
      var processUuidOpt: Option[UUID] = None
      val eventsToThisProcess = mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))
      //        val allEventsByThisProcess = mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))
      //        val fileUuids = MutableSet.empty[UUID]
      val fileEvents = MutableMap.empty[FileUUID, mutable.SortedSet[Event]]
      //        val netFlowUuids = MutableSet.empty[UUID]
      val netFlowEvents = MutableMap.empty[NetFlowUUID, mutable.SortedSet[(Event,NetFlowObject)]]

    {
      case Tuple4("Subject", uuid: ProcessUUID, event: Event, _: CDM17) =>
        if (processUuidOpt.isEmpty) processUuidOpt = Some(uuid)
        //            if (event.subject == processUuidOpt.get) allEventsByThisProcess += event
        eventsToThisProcess += event
        List.empty

      case Tuple4("NetFlowObject", uuid: NetFlowUUID, event: Event, cdmOpt: CDM17) =>
        if (processUuidOpt.isEmpty) processUuidOpt = Some(event.subjectUuid)
        netFlowEvents(uuid) = netFlowEvents.getOrElse(uuid, mutable.SortedSet.empty[(Event,NetFlowObject)](Ordering.by(_._1.timestampNanos))) +
          (event -> cdmOpt.asInstanceOf[NetFlowObject])
        List.empty

      case Tuple4("FileObject", uuid: FileUUID, event: Event, _: CDM17) =>
        if (processUuidOpt.isEmpty) processUuidOpt = Some(event.subjectUuid)
        fileEvents(uuid) = fileEvents.getOrElse(uuid, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) + event
        List.empty

      case CleanUp =>
        //            if (fileEvents.nonEmpty) {
        //              val mergedEvents = MutableMap.empty[UUID,mutable.SortedSet[Event]]
        //              fileEvents.foreach { case (u, es) =>
        //                mergedEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) ++ es
        //              }
        //              dbMap.putAll(mergedEvents.asJava)
        //              fileUuids ++= fileEvents.keySet
        //              fileEvents.clear()
        //            }
        //            if (netFlowEvents.nonEmpty) {
        //              val mergedEvents = MutableMap.empty[UUID,mutable.SortedSet[Event]]
        //              netFlowEvents.foreach { case (u, es) =>
        //                mergedEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) ++ es
        //              }
        //              dbMap.putAll(mergedEvents.asJava)
        //              netFlowUuids ++= netFlowEvents.keySet
        //              netFlowEvents.clear()
        //            }
        //            if (eventsToThisProcess.nonEmpty) {
        //              val mergedEvents = dbMap.getOrDefault(processUuidOpt.get, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) ++ eventsToThisProcess
        //              dbMap.put(processUuidOpt.get, mergedEvents)
        //              eventsToThisProcess.clear()
        //            }
        //            and similar for all events BY this process
        List.empty

      case EmitCmd =>
        //            fileUuids.foreach(u =>
        //              fileEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) ++
        //                fileEvents.getOrElse(u, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) )
        //
        //            netFlowUuids.foreach(u =>
        //              netFlowEvents(u) = //dbMap.getOrDefault(u, mutable.SortedSet.empty[(Event,NetFlowObject)](Ordering.by(_._1.timestampNanos))) ++
        //                netFlowEvents.getOrElse(u, mutable.SortedSet.empty[(Event,NetFlowObject)](Ordering.by(_._1.timestampNanos))) )

        //            eventsToThisProcess ++= dbMap.getOrDefault(processUuidOpt.get, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos)))

        List(((processUuidOpt.get, eventsToThisProcess), netFlowEvents.toSet, fileEvents.toSet))
    }
    }
      .mergeSubstreams
      .via(processFeatureExtractor)
  }



  val processEventTypes = EventType.values.toList

  val processFeatureExtractor = Flow[((ProcessUUID, mutable.SortedSet[Event]), Set[(NetFlowUUID, mutable.SortedSet[(Event,NetFlowObject)])], Set[(FileUUID, mutable.SortedSet[Event])])]
    .mapConcat[(String, ProcessUUID, MutableMap[String,Any], Set[EventUUID])] { case ((processUuid, eventsDoneToThisProcessSet), netFlowEventSets, fileEventSets) =>
    val eventsDoneToThisProcessList = eventsDoneToThisProcessSet.toList
    val processEventSet: mutable.SortedSet[Event] = eventsDoneToThisProcessSet ++ netFlowEventSets.flatMap(_._2.map(_._1)) ++ fileEventSets.flatMap(_._2)
    val processEventList: List[Event] = processEventSet.toList

    var allRelatedUUIDs = eventsDoneToThisProcessSet.flatMap(e => List(Some(e.uuid), e.predicateObject, e.predicateObject2, Some(e.subjectUuid)).flatten)

    val m = MutableMap.empty[String, Any]
    //      m("countOfImmediateChildProcesses") = "TODO"                                        // TODO: needs process tree
    //      m("countOfAllChildProcessesInTree") = "TODO"                                        // TODO: needs process tree
    //      m("countOfUniquePortAccesses") = "TODO"                                             // TODO: needs pairing with NetFlows —— not just events!
    // TODO: consider emitting the collected Process Tree
    m("countOfDistinctMemoryObjectsMProtected") = processEventSet.collect { case e if e.eventType == EVENT_MPROTECT && e.predicateObject.isDefined => e.predicateObject }.size
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
        _._2.dropWhile(write =>
          write.eventType != EVENT_WRITE && write.timestampNanos > netFlowReadTime
        ).exists(ex => ex.eventType == EVENT_EXECUTE)
      )
    )

    m("changesFilePermissionsThenExecutesIt") = processEventList.dropWhile(_.eventType != EVENT_MODIFY_FILE_ATTRIBUTES).exists(_.eventType == EVENT_EXECUTE)
    m("executedThenImmediatelyDeletedAFile") = processEventList.groupBy(_.predicateObject).-(None).values.exists(l => l.sortBy(_.timestampNanos).dropWhile(_.eventType != EVENT_EXECUTE).drop(1).headOption.exists(_.eventType == EVENT_UNLINK))
    m("readFromNetFlowThenDeletedFile") = netFlowEventSets.map(i => i._1 -> i._2.map(_._1)).flatMap(
      _._2.collect{ case e if e.subjectUuid == processUuid && List(EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG).contains(e.eventType) => e.timestampNanos }   // TODO: revisit these event types
    ).toList.sorted.headOption.exists(netFlowReadTime =>
      fileEventSets.exists(
        _._2.exists(delete =>
          delete.eventType != EVENT_UNLINK && delete.timestampNanos > netFlowReadTime
        )
      )
    )

    // TODO: consider: process takes any local action after reading from NetFlow

    m("countOfDistinctFileWrites") = processEventSet.collect { case e if e.eventType == EVENT_WRITE && e.predicateObject.isDefined => e.predicateObject }.size
    //      m("countOfFileUploads") = "TODO"                                                    // TODO: needs pairing with Files (to ensure reads are from Files)
    //      m("countOfFileDownloads") = "TODO"                                                  // TODO: needs pairing with Files (to ensure writes are to Files)
    m("isAccessingTempDirectory") = processEventList.flatMap(e => List(e.predicateObjectPath, e.predicateObject2Path).flatten).exists(path => List("/tmp", "/temp", "\\temp").exists(tmp => path.toLowerCase.contains(tmp) || (path.toLowerCase.startsWith("c:\\") && ! path.drop(3).contains("\\") ) ))  // TODO: revisit the list of temp locations.
    m("thisProcessIsTheObjectOfACHANGE_PRINCIPALEvent") = eventsDoneToThisProcessList.exists(e => e.eventType == EVENT_CHANGE_PRINCIPAL)
    m("thisProcessIsTheObjectOfAMODIFY_PROCESSEvent") = eventsDoneToThisProcessList.exists(e => e.eventType == EVENT_MODIFY_PROCESS)
    m("totalBytesSentToNetFlows") = processEventList.collect { case e if e.eventType == EVENT_SENDTO => e.size.getOrElse(0L)}.sum
    m("totalBytesReceivedFromNetFlows") = processEventList.collect { case e if e.eventType == EVENT_RECVFROM => e.size.getOrElse(0L)}.sum

    m("totalUniqueCheckFileEvents") = processEventSet.filter(_.eventType == EVENT_CHECK_FILE_ATTRIBUTES).flatMap(_.predicateObject).size
    processEventTypes.foreach( t =>
      m("count_"+ t.toString) = processEventSet.count(_.eventType == t)
    )

    // TODO: alarms: "executedThenImmediatelyDeletedAFile", "readsFromNetFlowThenWritesAFileThenExecutesTheFile", "changesFilePermissionsThenExecutesIt", "thisProcessIsTheObjectOfACHANGE_PRINCIPALEvent", others?
    // todo: alarms for executing: net.exe, ipconfig.exe,  netstat.exe, nmap, whoami.exe, hostname.exe, powershell.exe, cmd.exe
    // todo: alarm for opening a file with the substring: 'git' and 'commit'

    val viewDefinitions = Map(
      "Process Exec from Network" -> List("count_EVENT_EXECUTE", "readsFromNetFlowThenWritesAFileThenExecutesTheFile","executedThenImmediatelyDeletedAFile", "changesFilePermissionsThenExecutesIt")
    , "Process Directory Scan" -> List("count_EVENT_CHECK_FILE_ATTRIBUTES", "count_EVENT_OPEN", "totalUniqueCheckFileEvents")
    , "Process Netflow Events" -> List("count_EVENT_RECVFROM", "count_EVENT_RECVMSG", "count_EVENT_SENDMSG", "count_EVENT_SENDTO", "count_EVENT_WRITE", "count_EVENT_READ", "totalBytesReceivedFromNetFlows", "totalBytesSentToNetFlows")
    , "Process File Events" -> List("count_EVENT_CHECK_FILE_ATTRIBUTES", "count_EVENT_DUP", "count_EVENT_EXECUTE", "count_EVENT_LINK", "count_EVENT_LOADLIBRARY", "count_EVENT_LSEEK", "count_EVENT_MMAP", "count_EVENT_MODIFY_FILE_ATTRIBUTES", "count_EVENT_READ", "count_EVENT_WRITE", "count_EVENT_RENAME", "count_EVENT_TRUNCATE", "count_EVENT_UNLINK", "count_EVENT_UPDATE", "changesFilePermissionsThenExecutesIt", "countOfDistinctFileWrites", "isAccessingTempDirectory")
    , "Process Memory Events" -> List("countOfDistinctMemoryObjectsMProtected", "count_EVENT_LOADLIBRARY", "count_EVENT_MMAP", "count_EVENT_MPROTECT", "count_EVENT_UPDATE")
    , "Process Process Events" -> List("count_EVENT_CHANGE_PRINCIPAL", "thisProcessIsTheObjectOfACHANGE_PRINCIPALEvent", "count_EVENT_CLONE", "count_EVENT_FORK", "count_EVENT_LOGCLEAR", "count_EVENT_LOGIN", "count_EVENT_LOGOUT", "count_EVENT_MODIFY_PROCESS", "count_EVENT_SHM", "count_EVENT_SIGNAL", "count_EVENT_STARTSERVICE", "isAccessingTempDirectory")
    )

    // TODO: REMOVE:
    val req = viewDefinitions.values.flatten.toSet.forall(m.keySet.contains)
    if (! req) println("DANGER: " + viewDefinitions.values.flatten.toSet[String].map(x => x -> m.keySet.contains(x)).filter(x => ! x._2))

    viewDefinitions.toList.map { case (name, columnList) =>
      (name, processUuid, m.filter(t => columnList.contains(t._1)), allRelatedUUIDs.toSet)
    } ++ List(("All Process Features", processUuid, m, allRelatedUUIDs.toSet))
  }

  //  def testProcessFeatureExtractor(commandSource: Source[ProcessingCommand,_], db: DB) = {
  //    val processEventsDBMap = db.hashMap("ProcessEventsByPredicate").createOrOpen().asInstanceOf[HTreeMap[UUID, mutable.SortedSet[Event]]]
  //    Flow[CDM17]
  //      .via(eventsGroupedByKey(commandSource, processEventsDBMap, SubjectKey(None)).mergeSubstreams)
  //      .via(processFeatureExtractor)
  //  }

}
