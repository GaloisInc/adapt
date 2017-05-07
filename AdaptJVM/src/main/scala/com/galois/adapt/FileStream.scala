package com.galois.adapt

import java.util.UUID

import akka.stream.scaladsl.{Flow, Source}
import com.galois.adapt.FlowComponents.predicateTypeLabeler
import com.galois.adapt.cdm17.{CDM17, EVENT_CHECK_FILE_ATTRIBUTES, EVENT_CLOSE, EVENT_CREATE_OBJECT, EVENT_DUP, EVENT_EXECUTE, EVENT_FNCTL, EVENT_LINK, EVENT_LOADLIBRARY, EVENT_LSEEK, EVENT_MMAP, EVENT_MODIFY_FILE_ATTRIBUTES, EVENT_OPEN, EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG, EVENT_RENAME, EVENT_SENDMSG, EVENT_SENDTO, EVENT_TRUNCATE, EVENT_UNLINK, EVENT_UPDATE, EVENT_WRITE, Event}
import org.mapdb.{DB, HTreeMap}
import FlowComponents._
import scala.collection.mutable
import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
import scala.util.Random

object FileStream {

  def fileFeatureGenerator(commandSource: Source[ProcessingCommand,_], db: DB) = {
    val dbMap = db.hashMap("fileFeatureGenerator" + Random.nextInt()).createOrOpen().asInstanceOf[HTreeMap[UUID,mutable.SortedSet[Event]]]

    predicateTypeLabeler(commandSource, db)
      .filter(x => x._1 == "FileObject") // || x._1 == "NetFlowObject")
      .groupBy(Int.MaxValue, _._2)
      .merge(commandSource)
      .statefulMapConcat[(FileUUID, mutable.SortedSet[Event])]{ () => //, Set[(NetFlowUUID, mutable.SortedSet[Event])])]{ () =>
        var processUuidOpt: Option[ProcessUUID] = None
  //        var fileUuids = MutableSet.empty[UUID]
        val fileEvents = MutableMap.empty[FileUUID, mutable.SortedSet[Event]]
  //        val netFlowUuids = MutableSet.empty[NetFlowUUID]
        val netFlowEvents = MutableMap.empty[NetFlowUUID, mutable.SortedSet[Event]]


        {
//          case Tuple4("NetFlowObject", uuid: NetFlowUUID, event: Event, _: CDM17) =>
//            if (processUuidOpt.isEmpty) processUuidOpt = Some(event.subjectUuid)
//            netFlowEvents(uuid) = netFlowEvents.getOrElse(uuid, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) + event
//            List.empty

          case Tuple4("FileObject", uuid: FileUUID, event: Event, _: CDM17) =>
            if (processUuidOpt.isEmpty) processUuidOpt = Some(event.subjectUuid)
            fileEvents(uuid) = fileEvents.getOrElse(uuid, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) + event
            List.empty

          case CleanUp =>
    //            if (netFlowEvents.nonEmpty) {
    //              val mergedEvents = MutableMap.empty[UUID,mutable.SortedSet[Event]]
    //              netFlowEvents.foreach { case (u, es) =>
    //                mergedEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) ++ es
    //              }
    //              dbMap.putAll(mergedEvents.asJava)
    //              netFlowUuids ++= netFlowEvents.keySet
    //              netFlowEvents.clear()
    //            }
    //            if (fileEvents.nonEmpty) {
    //              val mergedEvents = MutableMap.empty[UUID,mutable.SortedSet[Event]]
    //              fileEvents.foreach { case (u, es) =>
    //                mergedEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) ++ es
    //              }
    //
    //              // This gets slower as Event Set gets larger.
    //              dbMap.putAll(mergedEvents.asJava)
    //
    //              fileUuids ++= fileEvents.keySet
    //              fileEvents.clear()
    //            }
            List.empty

          case EmitCmd =>
    //            fileUuids.foreach(u =>
    //              fileEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) ++
    //                fileEvents.getOrElse(u, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) )
    //
    //            netFlowUuids.foreach(u =>
    //              netFlowEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) ++
    //                netFlowEvents.getOrElse(u, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) )

            fileEvents.toList //.map { case (u, fes) => ((u, fes), netFlowEvents.toSet) }
        }
      }
      .mergeSubstreams
      .via(fileFeatures)
  }




  val fileEventTypes = List(EVENT_CHECK_FILE_ATTRIBUTES, EVENT_CLOSE, EVENT_CREATE_OBJECT, EVENT_DUP, EVENT_EXECUTE, EVENT_FNCTL, EVENT_LINK, EVENT_LSEEK, EVENT_MMAP, EVENT_MODIFY_FILE_ATTRIBUTES, EVENT_OPEN, EVENT_READ, EVENT_RENAME, EVENT_TRUNCATE, EVENT_UNLINK, EVENT_UPDATE, EVENT_WRITE)

  val fileFeatures = Flow[(FileUUID, mutable.SortedSet[Event])] ///, Set[(NetFlowUUID, mutable.SortedSet[Event])])]
    .mapConcat[(String, FileUUID, MutableMap[String,Any], Set[EventUUID])] { case (fileUuid, fileEventSet) => //, netFlowEventsFromIntersectingProcesses) =>
    val fileEventList = fileEventSet.toList
    var allRelatedUUIDs = fileEventSet.flatMap(e => List(Some(e.uuid), e.predicateObject, e.predicateObject2, Some(e.subjectUuid)).flatten)
    val m = MutableMap.empty[String,Any]
//    m("execAfterWriteByNetFlowReadingProcess") = {
//      var remainder = fileEventList.dropWhile(_.eventType != EVENT_WRITE)
//      var found = false
//      while (remainder.nonEmpty && remainder.exists(_.eventType == EVENT_EXECUTE)) {
//        val execOpt = remainder.find(_.eventType == EVENT_WRITE).flatMap(w => remainder.find(x => x.eventType == EVENT_EXECUTE && w.subjectUuid == x.subjectUuid))
//        found = execOpt.exists(x => netFlowEventsFromIntersectingProcesses.exists(p => p._2.exists(e => List(EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG).contains(e.eventType))))
//        if ( ! found) remainder = remainder.drop(1).dropWhile(_.eventType != EVENT_WRITE)
//      }
//      found
//    }: Boolean
    m("execSoonAfterWrite") = {
      val write = fileEventList.find(_.eventType != EVENT_WRITE)
      val exec = fileEventList.dropWhile(_.eventType != EVENT_WRITE).find(_.eventType == EVENT_EXECUTE)
      val answer = for {
        w <- write
        e <- exec
      } yield (e.timestampNanos - w.timestampNanos) <= 3e10  // within 30 seconds. This is the interpretation of "soon after"
      answer.getOrElse(false)
    }
    m("execAfterPermissionChangeToExecutable") = fileEventList.dropWhile(_.eventType != EVENT_MODIFY_FILE_ATTRIBUTES).exists(_.eventType == EVENT_EXECUTE)
    m("deletedAfterExec") = fileEventList.dropWhile(_.eventType != EVENT_EXECUTE).drop(1).exists(_.eventType == EVENT_UNLINK): Boolean
    m("deletedRightAfterExec") = fileEventList.dropWhile(_.eventType != EVENT_EXECUTE).drop(1).headOption.exists(_.eventType == EVENT_UNLINK): Boolean
//    m("deletedRightAfterProcessWithOpenNetFlowsWrites") =
//      (if (fileEventList.exists(_.eventType == EVENT_UNLINK)) {
//        fileEventList.collect { case writeEvent if writeEvent.eventType == EVENT_WRITE =>
//          val deleteAfterWriteOpt = fileEventList.find(deleteEvent =>
//            deleteEvent.eventType == EVENT_UNLINK &&
//              (deleteEvent.timestampNanos - writeEvent.timestampNanos >= 0) && // delete happened AFTER the write
//              (deleteEvent.timestampNanos - writeEvent.timestampNanos <= 3e10) // within 30 seconds. This is the interpretation of "right after"
//          )
//          deleteAfterWriteOpt.exists { deleteAfterWriteEvent => // IFF we found a Delete after WRITE...
//            netFlowEventsFromIntersectingProcesses.exists(t => t._2 // t._2 is a process's events, in order.
//              .dropWhile(_.eventType != EVENT_OPEN)
//              .takeWhile(_.eventType != EVENT_CLOSE)
//              .exists(testEvent => // in the events between OPEN and CLOSE...
//                testEvent.subjectUuid == deleteAfterWriteEvent.subjectUuid && // event by the same process as the UNLINK?
//                  t._2.find(_.eventType == EVENT_CLOSE).exists(closeEvent => // If so, get the CLOSE event and
//                    deleteAfterWriteEvent.timestampNanos <= closeEvent.timestampNanos // test if the UNLINK occurred before the CLOSE
//                  )
//              )
//            )
//          }
//        }.foldLeft(false)(_ || _) // is there a single `true`?
//      } else false): Boolean
    m("deleteSoonAfterWrite") = {
      val write = fileEventList.find(_.eventType != EVENT_WRITE)
      val delete = fileEventList.dropWhile(_.eventType != EVENT_WRITE).find(_.eventType == EVENT_UNLINK)
      val answer = for {
        w <- write
        d <- delete
      } yield (d.timestampNanos - w.timestampNanos) <= 3e10  // within 30 seconds. This is the interpretation of "soon after"
      answer.getOrElse(false)
    }

//    m("isReadByAProcessWritingToNetFlows") = fileEventList
//      .collect{ case e if e.eventType == EVENT_READ => e.subjectUuid}
//      .flatMap( processUuid =>
//        netFlowEventsFromIntersectingProcesses.toList.map(_._2.exists(ne =>
//          ne.subjectUuid == processUuid &&
//            List(EVENT_SENDTO, EVENT_SENDMSG, EVENT_WRITE).contains(ne.eventType)
//        ))
//      ).foldLeft(false)(_ || _)
    m("isInsideTempDirectory") = fileEventList.flatMap(_.predicateObjectPath).exists(path => List("/tmp", "/temp", "\\temp").exists(tmp => path.toLowerCase.contains(tmp) || (path.toLowerCase.startsWith("c:\\") && ! path.drop(3).contains("\\") )))
    m("execDeleteGapNanos") = fileEventList.timeBetween(Some(EVENT_EXECUTE), Some(EVENT_UNLINK))
    m("attribChangeEventThenExecuteGapNanos") = fileEventList.timeBetween(Some(EVENT_MODIFY_FILE_ATTRIBUTES), Some(EVENT_EXECUTE))
    m("writeExecutionGapNanos") = fileEventList.timeBetween(Some(EVENT_WRITE), Some(EVENT_EXECUTE))
    m("readDeletionGapNanos") = fileEventList.timeBetween(Some(EVENT_READ), Some(EVENT_UNLINK))
    m("countDistinctProcessesHaveEventToFile") = fileEventSet.map(_.subjectUuid).size
    m("totalBytesRead") = fileEventList.filter(_.eventType == EVENT_READ).flatMap(_.size).sum
    m("totalBytesWritten") = fileEventList.filter(_.eventType == EVENT_WRITE).flatMap(_.size).sum

    m("writeToLoadLibraryGapNanos") = fileEventList.timeBetween(Some(EVENT_WRITE), Some(EVENT_LOADLIBRARY))
    m("writeToMMapLibraryGapNanos") = fileEventList.timeBetween(Some(EVENT_WRITE), Some(EVENT_MMAP))

    m("stringsInFilePathOrName") =  fileEventSet.flatMap(e => List(e.predicateObjectPath, e.predicateObject2Path, e.name).flatten)
      .exists(path =>
        List("screengrab", "netrecon", "recordaudio", "imagegrab", "keylogger", "kudu", "samplelib", "accutrack", "flexispy",
          "gpspy", "ackposts", "godwon", "antares", "antammi", "pjapps", "sndapps", "snadapps", "placeraider", "dropper", "dropbear"
        ).exists(name => path.toLowerCase.contains(name))
    ): Boolean

    fileEventTypes.foreach( t =>
      m("count_"+ t.toString) = fileEventSet.count(_.eventType == t)
    )


    val viewDefinitions = Map(
      "Downloaded File Execution" -> List(/*"execAfterWriteByNetFlowReadingProcess",*/ "count_EVENT_EXECUTE", "count_EVENT_MODIFY_FILE_ATTRIBUTES", "deletedAfterExec", /*"deletedRightAfterProcessWithOpenNetFlowsWrites",*/ "execAfterPermissionChangeToExecutable", "isInsideTempDirectory")
    , "NetFlow-related File Anomaly" -> List("count_EVENT_MODIFY_FILE_ATTRIBUTES", "count_EVENT_EXECUTE", "count_EVENT_MMAP", "count_EVENT_UNLINK", "deletedAfterExec", /*"deletedRightAfterProcessWithOpenNetFlowsWrites", "isReadByAProcessWritingToNetFlows",*/ "isInsideTempDirectory")
    , "File Executed Stats" -> List("count_EVENT_MODIFY_FILE_ATTRIBUTES", "count_EVENT_EXECUTE", "count_EVENT_MMAP", "count_EVENT_UNLINK", "deletedAfterExec", /*"deletedRightAfterProcessWithOpenNetFlowsWrites",*/ "isInsideTempDirectory", "attribChangeEventThenExecuteGapNanos", "execAfterPermissionChangeToExecutable", "execDeleteGapNanos")
    , "File MMap Stats" -> List("count_EVENT_MMAP", "count_EVENT_LSEEK", "count_EVENT_READ", "countDistinctProcessesHaveEventToFile")
    , "File Permission Event" -> List("attribChangeEventThenExecuteGapNanos", "count_EVENT_CHECK_FILE_ATTRIBUTES", "count_EVENT_MODIFY_FILE_ATTRIBUTES")
    , "File Modify Event" -> List("attribChangeEventThenExecuteGapNanos", "count_EVENT_DUP", "count_EVENT_MODIFY_FILE_ATTRIBUTES", "count_EVENT_RENAME", "count_EVENT_TRUNCATE", "count_EVENT_UPDATE", "count_EVENT_WRITE", "totalBytesWritten")
//    , "File Affected By NetFlow" -> List(/*"deletedRightAfterProcessWithOpenNetFlowsWrites", "isReadByAProcessWritingToNetFlows",*/ "deletedAfterExec", /*"execAfterWriteByNetFlowReadingProcess"*/)
    , "Exfil Staging File" -> List("count_EVENT_OPEN", "count_EVENT_WRITE", "count_EVENT_READ", "count_EVENT_UNLINK")

    // An alarm must have a name beginning with "ALARM", and contain exactly one boolean feature. See the EmitCmd case in anomalyScoreCalculator.
    , "ALARM: Deleted After Exec" -> List("deletedAfterExec")
    , "ALARM: Deleted Immediately After Exec" -> List("deletedRightAfterExec")
//    , "ALARM: Deleted Right After Process With Open NetFlow Writes" -> List("deletedRightAfterProcessWithOpenNetFlowsWrites")
//    , "ALARM: Download Then Execute" -> List("execAfterWriteByNetFlowReadingProcess")
    , "ALARM: Suspicious Name in File Path" -> List("stringsInFilePathOrName")
    , "ALARM: Exec Soon After Write" -> List("execSoonAfterWrite")
    , "ALARM: Delete Soon After Write" -> List("deleteSoonAfterWrite")
    )

    val req = viewDefinitions.values.flatten.toSet.forall(m.keySet.contains)
    if (! req) println("Requirement failed: s" + viewDefinitions.values.flatten.toSet[String].map(x => x -> m.keySet.contains(x)).filter(x => ! x._2))

    viewDefinitions.toList.map { case (name, columnList) =>
      (name, fileUuid, m.filter(t => columnList.contains(t._1)), allRelatedUUIDs.toSet)
    } ++ List(("All File Features", fileUuid, m, allRelatedUUIDs.toSet))
  }


  //  def testFileFeatureEventAccumulator(commandSource: Source[ProcessingCommand,_], db: DB) = {
  //    val fileEventsDBMap = db.hashMap("FileEventsByPredicate_" + Random.nextLong()).createOrOpen().asInstanceOf[HTreeMap[UUID, mutable.SortedSet[Event]]]
  //    Flow[CDM17]
  //      .collect{ case e: Event if FlowComponents.fileEventTypes.contains(e.eventType) => e}
  //      .via(eventsGroupedByKey(commandSource, fileEventsDBMap, PredicateObjectKey).mergeSubstreams)
  //  }


}
