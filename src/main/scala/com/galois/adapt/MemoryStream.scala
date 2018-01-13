package com.galois.adapt

import java.util.UUID

import akka.stream.{DelayOverflowStrategy, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Source}
import com.galois.adapt.FlowComponents.predicateTypeLabeler
import com.galois.adapt.cdm18._
import com.typesafe.config.ConfigFactory
import org.mapdb.{DB, HTreeMap}

import scala.collection.mutable.{Map => MutableMap, Set => MutableSet, SortedSet => MutableSortedSet}
import scala.util.Random
import scala.concurrent.duration._


object MemoryStream {

  val config = ConfigFactory.load()

  def memoryFeatureGenerator(commandSource: Source[ProcessingCommand,_], db: DB) = {
//    val dbMap = db.hashMap("fileFeatureGenerator" + Random.nextInt()).createOrOpen().asInstanceOf[HTreeMap[UUID,MutableSet[Event]]]

//    predicateTypeLabeler(commandSource, db)
    Flow[(String, UUID, Event, CDM18)]
      .filter(x => x._1 == "MemoryObject")
      .groupBy(Int.MaxValue, _._2)
//      .merge(commandSource)
      .statefulMapConcat[(MemoryUUID, MutableSet[Event])]{ () =>
        val memoryEvents = MutableSet.empty[Event]

        var shouldStore = true
        var cleanupCounts = 0

        {
          case Tuple4("MemoryObject", uuid: MemoryUUID, event: Event, _: CDM18) =>
            if (shouldStore) {
              memoryEvents += event
              cleanupCounts = 0
            }
            if (memoryEvents.size > config.getInt("adapt.runtime.throwawaythreshold") && shouldStore) {
              println(s"MemoryObject is over the event limit: $uuid")
              shouldStore = false
              memoryEvents.clear()
            }

            List(uuid -> memoryEvents)

//          case CleanUp =>
////            if (netFlowEvents.nonEmpty) {
////              val mergedEvents = MutableMap.empty[UUID,mutable.SortedSet[Event]]
////              netFlowEvents.foreach { case (u, es) =>
////                mergedEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) ++ es
////              }
////              dbMap.putAll(mergedEvents.asJava)
////              netFlowUuids ++= netFlowEvents.keySet
////              netFlowEvents.clear()
////            }
////            if (memoryEvents.nonEmpty) {
////              val mergedEvents = MutableMap.empty[UUID,mutable.SortedSet[Event]]
////              memoryEvents.foreach { case (u, es) =>
////                mergedEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) ++ es
////              }
////
////              // This gets slower as Event Set gets larger.
////              dbMap.putAll(mergedEvents.asJava)
////
////              fileUuids ++= memoryEvents.keySet
////              memoryEvents.clear()
////            }
//            List.empty
//
//          case EmitCmd =>
////            fileUuids.foreach(u =>
////              memoryEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) ++
////                memoryEvents.getOrElse(u, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) )
////
////            netFlowUuids.foreach(u =>
////              netFlowEvents(u) = dbMap.getOrDefault(u, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) ++
////                netFlowEvents.getOrElse(u, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))) )
//
//            memoryEvents.toList
        }
      }
      .buffer(1, OverflowStrategy.dropHead)
      .delay(config.getInt("adapt.runtime.featureextractionseconds") seconds, DelayOverflowStrategy.backpressure)
      .map(t =>
          (t._1, MutableSortedSet(t._2.toList:_*)(Ordering.by(_.timestampNanos)))
      )
      .mergeSubstreams
      .via(memoryFeatures)
  }

  val memoryEventTypes = List(EVENT_CREATE_OBJECT, EVENT_DUP, EVENT_EXECUTE, EVENT_FNCTL, EVENT_LINK, EVENT_LSEEK, EVENT_MMAP, EVENT_MODIFY_FILE_ATTRIBUTES, EVENT_MPROTECT, EVENT_OPEN, EVENT_READ, EVENT_RENAME, EVENT_SHM, EVENT_TRUNCATE, EVENT_UNLINK, EVENT_UPDATE, EVENT_WRITE, EVENT_LOADLIBRARY)

  val memoryFeatures = Flow[(MemoryUUID, MutableSortedSet[Event])]
    .mapConcat[(String, MemoryUUID, MutableMap[String,Any], Set[UUID])] { case (memoryUUID, memoryEventSet) =>
    val memoryEventList = memoryEventSet.toList
    var allRelatedUUIDs = memoryEventSet.flatMap(e => List(Some(e.uuid), e.predicateObject, e.predicateObject2, e.subjectUuid).flatten)
    val m = MutableMap.empty[String,Any]
    m("writeAndMProtectToTheSameMemoryObject") = memoryEventSet.exists(_.eventType == EVENT_WRITE) && memoryEventSet.exists(_.eventType == EVENT_MPROTECT)
    m("mmapAndMProtectToTheSameMemoryObject") = memoryEventSet.exists(_.eventType == EVENT_MMAP) && memoryEventSet.exists(_.eventType == EVENT_MPROTECT)
    m("mmapFromTempDirectory") = memoryEventList.filter(_.eventType == EVENT_MMAP).flatMap(_.predicateObjectPath).exists(path => List("/tmp", "/temp", "\\temp").exists(tmp => path.toLowerCase.contains(tmp) || (path.toLowerCase.startsWith("c:\\") && ! path.drop(3).contains("\\") )))

    memoryEventTypes.foreach( t =>
      m("count_"+ t.toString) = memoryEventSet.count(_.eventType == t)
    )

    val viewDefinitions = Map(
      // An alarm must have a name beginning with "ALARM", and contain exactly one boolean feature. See the EmitCmd case in anomalyScoreCalculator.
      "ALARM: Write + MProtect A MemoryObject" -> List("writeAndMProtectToTheSameMemoryObject")
    , "ALARM: MMap + MProtect A MemoryObject" -> List("mmapAndMProtectToTheSameMemoryObject")
    , "ALARM: MMap From Temp Directory" -> List("mmapFromTempDirectory")
    )

    // TODO: remove!
//    val req = viewDefinitions.values.flatten.toSet.forall(m.keySet.contains)
//    if (! req) println("Memory Requirement failed: s" + viewDefinitions.values.flatten.toSet[String].map(x => x -> m.keySet.contains(x)).filter(x => ! x._2))

    viewDefinitions.toList.map { case (name, columnList) =>
      (name, memoryUUID, m.filter(t => columnList.contains(t._1)), allRelatedUUIDs.toSet)
    } ++ List(("All Memory Features", memoryUUID, m, allRelatedUUIDs.toSet))
  }

}
