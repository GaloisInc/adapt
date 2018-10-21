package com.galois.adapt.adm

import java.util.UUID

import akka.NotUsed
import akka.event.LoggingAdapter
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Source}
import akka.stream.{FlowShape, OverflowStrategy}
import com.galois.adapt.MapSetUtils.{AlmostMap, AlmostSet}
import com.galois.adapt.adm.ERStreamComponents.{EventResolution, _}
import com.galois.adapt.adm.UuidRemapper.{JustTime, UuidRemapperInfo}
import com.galois.adapt.cdm19._
import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
import scala.concurrent.duration._
import scala.language.postfixOps
import com.galois.adapt.AdaptConfig._


object EntityResolution {

  case object EndOfStream extends LatestCDM // marker used for in-band signaling

  // We keep these as local variables in the object (as opposed to state closed over in `annotateTime`) because we want
  // easy access to them for logging/debugging purposes.
  var monotonicTime: Long = 0
  var sampledTime: Long = 0
  var nodeCount: Long = 0

  // This is just for logging state held onto by the `EventResolution` branch of `erWithoutRemaps`
  val activeChains: MutableMap[EventResolution.EventKey, EventResolution.EventMergeState] = MutableMap.empty

  // This is just for logging state held onto by `deduplicate`.
  var blockedEdgesCount: Long = 0
  val blockingNodes: MutableSet[UUID] = MutableSet.empty[UUID]


  def apply(
    config: AdmConfig,                                              // Config passed in

    cdm2cdmMaps: Array[AlmostMap[CdmUUID, CdmUUID]],                // Map from CDM to CDM
    cdm2admMaps: Array[AlmostMap[CdmUUID, AdmUUID]],                // Map from CDM to ADM
    blockedEdges: Array[MutableMap[CdmUUID, (List[Edge], Set[CdmUUID])]], // Map of things blocked
    shardCount: Array[Int],
    log: LoggingAdapter,

    seenNodesSet: AlmostSet[AdmUUID],                               // Set of nodes seen so far
    seenEdgesSet: AlmostSet[EdgeAdm2Adm]                            // Set of edges seen so far
  ): Flow[(String,LatestCDM), Either[ADM, EdgeAdm2Adm], NotUsed] = {

    val numUuidRemapperShards = config.uuidRemapperShards           // 0 means use the old remapper

    assert(numUuidRemapperShards >= 0, "Negative number of shards!")
    assert(cdm2cdmMaps.length == Math.max(numUuidRemapperShards, 1), "Wrong number of cdm2cdm maps")
    assert(cdm2admMaps.length == Math.max(numUuidRemapperShards, 1), "Wrong number of cdm2adm maps")

    val maxTimeJump: Long      = (config.maxtimejumpsecs seconds).toNanos

    val uuidExpiryTime: Time   = {
      val uuidExpiryNanos: Long  = (config.cdmexpiryseconds seconds).toNanos
      val uuidExpiryCount: Long  = config.cdmexpirycount
      Time(uuidExpiryNanos, uuidExpiryCount)
    }

    val eventExpiryTime: Time  = {
      val eventExpiryNanos: Long = (config.eventexpirysecs seconds).toNanos
      val eventExpiryCount: Long = config.eventexpirycount
      Time(eventExpiryNanos, eventExpiryCount)
    }

    val maxEventsMerged: Int   = config.maxeventsmerged

    val maxTimeMarker = ("", Timed(Time.max, EndOfStream))
    val maxTimeRemapper = Timed(Time.max, JustTime)

    val ignoreEventUuids: Boolean = config.ignoreeventremaps

    val remapper: UuidRemapper.UuidRemapperFlow = if (numUuidRemapperShards == 0) {
      UuidRemapper.apply(uuidExpiryTime, cdm2cdmMaps(0), cdm2admMaps(0), blockedEdges(0), ignoreEventUuids, log)
    } else {
      UuidRemapper.sharded(uuidExpiryTime, cdm2cdmMaps, cdm2admMaps, blockedEdges, shardCount, ignoreEventUuids, log, numUuidRemapperShards)
    }

    Flow[(String, LatestCDM)]
      .via(annotateTime(maxTimeJump))                                         // Annotate with a monotonic time
      .buffer(2000, OverflowStrategy.backpressure)
      .concat(Source.fromIterator(() => Iterator(maxTimeMarker)))             // Expire everything in UuidRemapper
      .via(erWithoutRemaps(eventExpiryTime, maxEventsMerged, activeChains))   // Entity resolution without remaps
      .concat(Source.fromIterator(() => Iterator(maxTimeRemapper)))           // Expire everything in UuidRemapper
      .buffer(2000, OverflowStrategy.backpressure)
      .via(remapper)                                                          // Remap UUIDs
      .buffer(2000, OverflowStrategy.backpressure)
      .via(deduplicate(seenNodesSet, seenEdgesSet, MutableMap.empty))         // Order nodes/edges
  }


  private type TimeFlow = Flow[(String,LatestCDM), (String,Timed[LatestCDM]), NotUsed]

  // Since TA1s cannot be trusted to have a regularly increasing time value, we can't rely on just this for expiring
  // things. The solution is to thread thorugh a node count - we're pretty sure that will increase proportionally to the
  // number of nodes ingested ;)
  case class Time(nanos: Long, count: Long) {
    def plus(other: Time): Time = Time(this.nanos + other.nanos, this.count + other.count)
  }
  object Time {
    def max: Time = Time(Long.MaxValue, Long.MaxValue)
  }

  case class Timed[+T](time: Time, unwrap: T)

  // Annotate a flow of `CDM` with a monotonic time value corresponding roughly to the time when the CDM events were
  // observed on the instrumented machine.
  private def annotateTime(maxTimeJump: Long): TimeFlow = {

    // Map a CDM onto a possible timestamp
    val timestampOf: LatestCDM => Option[Long] = {
      case s: Subject => s.startTimestampNanos
      case e: Event => Some(e.timestampNanos)
      case t: TimeMarker => Some(t.timestampNanos)
      case _ => None
    }

    Flow[(String,LatestCDM)].map { case (provider, cdm: LatestCDM) =>

      nodeCount += 1
      for (time <- timestampOf(cdm); _ = { sampledTime = time; () }; if time > monotonicTime) {
        cdm match {
          case _: TimeMarker if time > monotonicTime => monotonicTime = time

          // For things that aren't time markers, only update the time if it is larger, but not too much larger than the
          // previous time. With an exception made for old times that are much too old (looking at you ClearScope)
          case _ if time > monotonicTime && (time - monotonicTime < maxTimeJump || monotonicTime < 1400000000000000L) =>
            monotonicTime = time

          case _ => ()
        }
      }

      (provider, Timed(Time(monotonicTime, nodeCount), cdm))
    }
  }


  type ErFlow = Flow[(String,Timed[LatestCDM]), Timed[UuidRemapperInfo], NotUsed]

  // Perform entity resolution on stream of CDMs to convert them into ADMs, Edges, and general information to hand off
  // to the UUID remapping stage.
  private def erWithoutRemaps(
    eventExpiryTime: Time,
    maxEventsMerged: Int,
    activeChains: MutableMap[EventResolution.EventKey, EventResolution.EventMergeState]
  ): ErFlow =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val broadcast = b.add(Broadcast[(String,Timed[LatestCDM])](3))
      val merge = b.add(Merge[Timed[UuidRemapperInfo]](3))

      broadcast ~> EventResolution(eventExpiryTime, maxEventsMerged, activeChains) ~> merge
      broadcast ~> SubjectResolution.apply                                         ~> merge
      broadcast ~> OtherResolution.apply                                           ~> merge

      FlowShape(broadcast.in, merge.out)
    })


  private type OrderAndDedupFlow = Flow[Either[ADM, EdgeAdm2Adm], Either[ADM, EdgeAdm2Adm], NotUsed]

  // Prevents nodes/edges from being emitted twice. Also prevents edges from being emitted before both of their
  // endpoints have been emitted.
  private def deduplicate(
    seenNodes: AlmostSet[AdmUUID],
    seenEdges: AlmostSet[EdgeAdm2Adm],
    blockedEdges: MutableMap[UUID, List[EdgeAdm2Adm]]     // Edges blocked by UUIDs that haven't arrived yet
  ): OrderAndDedupFlow = Flow[Either[ADM, EdgeAdm2Adm]].statefulMapConcat[Either[ADM, EdgeAdm2Adm]](() => {

    // Try to emit an edge. If either end of the edge hasn't arrived, add it to the blocked map.
    def emitEdge(edge: EdgeAdm2Adm): Option[Either[ADM, EdgeAdm2Adm]] = edge match {
      case EdgeAdm2Adm(_, _, tgt) if !seenNodes.contains(tgt) =>
        blockedEdges(tgt) = edge :: blockedEdges.getOrElse(tgt, Nil)
        blockedEdgesCount += 1
        blockingNodes += tgt
        None

      case EdgeAdm2Adm(src, _, _) if !seenNodes.contains(src) =>
        blockedEdges(src) = edge :: blockedEdges.getOrElse(src, Nil)
        blockedEdgesCount += 1
        blockingNodes += src
        None

      case _ =>
        Some(Right(edge))
    }

    {
      case Right(edge) =>
        if (seenEdges.add(edge)) {
          // New edge
          emitEdge(edge).toList
        } else {
          // Duplicate edge
          Nil
        }

      case Left(adm) =>
        if (seenNodes.add(adm.uuid)) {
          // New node
          blockingNodes -= adm.uuid
          val emit = blockedEdges.remove(adm.uuid).getOrElse(Nil).flatMap { e =>
            blockedEdgesCount -= 1
            emitEdge(e).toList
          }
          Left(adm) :: emit
        } else {
          // Duplicate node
          Nil
        }
    }
  })
}
