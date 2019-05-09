package com.galois.adapt.adm

import java.util.{Date, UUID}

import akka.NotUsed
import akka.event.LoggingAdapter
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Source}
import akka.stream.{FlowShape, OverflowStrategy}
import com.galois.adapt.MapSetUtils.{AlmostMap, AlmostSet}
import com.galois.adapt.adm.ERStreamComponents.{EventResolution, _}
import com.galois.adapt.adm.UuidRemapper.{JustTime, UuidRemapperInfo}
import com.galois.adapt.cdm20._

import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.duration._
import scala.language.postfixOps
import com.galois.adapt.AdaptConfig._
import com.galois.adapt.CurrentCdm


object EntityResolution {

  case object EndOfStream extends CurrentCdm // marker used for in-band signalling

  // We keep these as local variables in the object (as opposed to state closed over in `annotateTime`) because we want
  // easy access to them for logging/debugging purposes. This also makes them global across all parallel ER flows.
  var monotonicTime: Long = 0
  var sampledTime: Long = 0

  def apply(
    config: AdmConfig,                                              // Config passed in
    host: IngestHost,

    cdm2cdmMaps: Array[AlmostMap[CdmUUID, CdmUUID]],                // Map from CDM to CDM
    cdm2admMaps: Array[AlmostMap[CdmUUID, AdmUUID]],                // Map from CDM to ADM
    blockedEdges: Array[MutableMap[CdmUUID, (List[Edge], Set[CdmUUID])]], // Map of things blocked
    remapperShardCount: Array[Long],
    log: LoggingAdapter,

    seenNodesSets: Array[AlmostSet[AdmUUID]],                       // Set of nodes seen so far
    seenEdgesSets: Array[AlmostSet[EdgeAdm2Adm]],                   // Set of edges seen so far
    deduplicateShardCount: Array[Long]
  ): Flow[(String, CurrentCdm), Either[ADM, EdgeAdm2Adm], NotUsed] = {

    val numUuidRemapperShards = config.uuidRemapperShards           // 0 means use the old remapper

    assert(numUuidRemapperShards >= 0, "Negative number of shards!")
    assert(cdm2cdmMaps.length == Math.max(numUuidRemapperShards, 1), "Wrong number of cdm2cdm maps")
    assert(cdm2admMaps.length == Math.max(numUuidRemapperShards, 1), "Wrong number of cdm2adm maps")
    assert(seenNodesSets.length == Math.max(numUuidRemapperShards, 1), "Wrong number of seenNodes sets")
    assert(seenEdgesSets.length == Math.max(numUuidRemapperShards, 1), "Wrong number of seenEdges sets")

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
      UuidRemapper.sharded(uuidExpiryTime, cdm2cdmMaps, cdm2admMaps, blockedEdges, remapperShardCount, ignoreEventUuids, log, numUuidRemapperShards)
    }

//    val deduplicate: DeduplicateNodesAndEdges.OrderAndDedupFlow =
//      DeduplicateNodesAndEdges.apply(numUuidRemapperShards, seenNodesSets, seenEdgesSets, deduplicateShardCount)

    // CDM nodes with this CDM UUID (or edges to/from this CDM UUID) should be ignored
    val badUuid = new UUID(0L, 0L)

    val goodEventTypes = {
      import com.galois.adapt.StandingFetches._

      readTypes.union(writeTypes).union(execTypes).union(deleteTypes)
    }

    val tue7Afternoon  = new Date(2019, 4, 6,  18, 0)
    val wed8Afternoon  = new Date(2019, 4, 7,  18, 0)
    val thu9Afternoon  = new Date(2019, 4, 8,  18, 0)
    val fri10Afternoon = new Date(2019, 4, 9,  18, 0)
    val mon13Afternoon = new Date(2019, 4, 12, 18, 0)
    val tue14Afternoon = new Date(2019, 4, 13, 18, 0)
    val wed15Afternoon = new Date(2019, 4, 14, 18, 0)
    val thu16Afternoon = new Date(2019, 4, 15, 18, 0)

    val wed8Morning  = new Date(2019, 4, 7,  8, 0)
    val thu9Morning  = new Date(2019, 4, 8,  8, 0)
    val fri10Morning = new Date(2019, 4, 9,  8, 0)
    val mon13Morning = new Date(2019, 4, 12, 8, 0)
    val tue14Morning = new Date(2019, 4, 13, 8, 0)
    val wed15Morning = new Date(2019, 4, 14, 8, 0)
    val thu16Morning = new Date(2019, 4, 15, 8, 0)
    val fri17Morning = new Date(2019, 4, 16, 8, 0)

    Flow[(String, CurrentCdm)]
      .filter {
        case (_, cdm: Host) => cdm.uuid != badUuid
        case (_, cdm: Principal) => cdm.uuid != badUuid
        case (_, cdm: Subject) => cdm.uuid != badUuid
        case (_, cdm: FileObject) => cdm.uuid != badUuid
        case (_, cdm: IpcObject) => cdm.uuid != badUuid
        case (_, cdm: RegistryKeyObject) => cdm.uuid != badUuid
        case (_, cdm: PacketSocketObject) => cdm.uuid != badUuid
        case (_, cdm: NetFlowObject) => cdm.uuid != badUuid
        case (_, cdm: MemoryObject) => cdm.uuid != badUuid
        case (_, cdm: SrcSinkObject) => cdm.uuid != badUuid && cdm.srcSinkType != SRCSINK_UNKNOWN
        case (_, cdm: Event) => cdm.uuid != badUuid && goodEventTypes.contains(cdm.eventType)
        case _ => true
      }
      .via(annotateTime(maxTimeJump))                                         // Annotate with a monotonic time
      .filter {                                                               // Filter out events outside of business hours
        case timed @ (_, Timed(t, cdm: Event)) =>
          val date = new Date(t.nanos / (1000 * 1000))

          val afterHours =
            date.after(tue7Afternoon)  && date.before(wed8Morning)  || // after tues 18:00, before wed 8:00
            date.after(wed8Afternoon)  && date.before(thu9Morning)  || // after wed 18:00, before thurs 8:00
            date.after(thu9Afternoon)  && date.before(fri10Morning) || // after thurs 18:00, before fri 8:00
            date.after(fri10Afternoon) && date.before(mon13Morning) || // after fri 18:00, before mon 8:00
            date.after(mon13Afternoon) && date.before(tue14Morning) || // after mon 18:00, before tues 8:00
            date.after(tue14Afternoon) && date.before(wed15Morning) || // after tues 18:00, before wed 8:00
            date.after(wed15Afternoon) && date.before(thu16Morning) || // after wed 18:00, before thurs 8:00
            date.after(thu16Afternoon) && date.before(fri17Morning)    // after thurs 18:00, before fri 8:00

          ! afterHours

        case _ => true
      }
      .buffer(2000, OverflowStrategy.backpressure)
      .concat(Source.fromIterator(() => Iterator(maxTimeMarker)))             // Expire everything in UuidRemapper
      .via(erWithoutRemaps(eventExpiryTime, maxEventsMerged, host)) // Entity resolution without remaps
      .filter {
        case Timed(_, UuidRemapper.AnEdge(EdgeCdm2Cdm(cdm1, _, cdm2))) => cdm1.uuid != badUuid && cdm2.uuid != badUuid
        case Timed(_, UuidRemapper.AnEdge(EdgeCdm2Adm(cdm1, _, _))) => cdm1.uuid != badUuid
        case Timed(_, UuidRemapper.AnEdge(EdgeAdm2Cdm(_, _, cdm2))) => cdm2.uuid != badUuid
        case _ => true
      }
      .concat(Source.fromIterator(() => Iterator(maxTimeRemapper)))           // Expire everything in UuidRemapper
      .buffer(2000, OverflowStrategy.backpressure)
      .via(remapper)                                                          // Remap UUIDs
//      .buffer(2000, OverflowStrategy.backpressure)
//      .via(deduplicate)                                                       // Order nodes/edges
  }


  private type TimeFlow = Flow[(String,CurrentCdm), (String,Timed[CurrentCdm]), NotUsed]

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

    var nodeCount: Long = 0

    // Map a CDM onto a possible timestamp
    val timestampOf: CurrentCdm => Option[Long] = {
      case s: Subject => s.startTimestampNanos
      case e: Event => Some(e.timestampNanos)
      case t: TimeMarker => Some(t.timestampNanos)
      case _ => None
    }

    /// This tries to convert seconds and microseconds to nanoseconds
    def adjustTimeUnits(x: Long): Long = {
      if        (x > 1450000000000000000L && x < 1600000000000000000L) {
        // Probably nanoseconds, since that range corresponds to (December 13, 2015 - September 13, 2020)
        x
      } else if (x > 1450000000000000L    && x < 1600000000000000L) {
        // Probably microseconds, since that range corresponds to (December 13, 2015 - September 13, 2020)
        x * 1000
      } else if (x > 1450000000000L       && x < 1600000000000L) {
        // Probably milliseconds, since that range corresponds to (December 13, 2015 - September 13, 2020)
        x * 1000000
      } else if (x > 1450000000L          && x < 1600000000L) {
        // Probably seconds, since that range corresponds to (December 13, 2015 - September 13, 2020)
        x * 1000000000
      } else {
        // Seriously, WTF TA1.
        x
      }
    }

    Flow[(String,CurrentCdm)].map { case (provider, cdm: CurrentCdm) =>

      nodeCount += 1
      for (time <- timestampOf(cdm).map(adjustTimeUnits); _ = { sampledTime = time; () }; if time > monotonicTime) {
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


  type ErFlow = Flow[(String,Timed[CurrentCdm]), Timed[UuidRemapperInfo], NotUsed]

  // Perform entity resolution on stream of CDMs to convert them into ADMs, Edges, and general information to hand off
  // to the UUID remapping stage.
  private def erWithoutRemaps(
    eventExpiryTime: Time,
    maxEventsMerged: Int,
    host: IngestHost
  ): ErFlow =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val broadcast = b.add(Broadcast[(String,Timed[CurrentCdm])](3))
      val merge = b.add(Merge[Timed[UuidRemapperInfo]](3))

      broadcast ~> EventResolution(host, eventExpiryTime, maxEventsMerged) ~> merge
      broadcast ~> SubjectResolution(host)                                 ~> merge
      broadcast ~> OtherResolution(host)                                   ~> merge

      FlowShape(broadcast.in, merge.out)
    })
}
