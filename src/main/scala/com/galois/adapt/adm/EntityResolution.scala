package com.galois.adapt.adm

import java.util.UUID

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Source}
import com.galois.adapt.MapDBUtils.{AlmostMap, AlmostSet}
import com.galois.adapt.adm.ERStreamComponents.{EventResolution, _}
import com.galois.adapt.adm.UuidRemapper.{JustTime, UuidRemapperInfo}
import com.galois.adapt.cdm18._
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
import scala.concurrent.duration._
import scala.language.postfixOps


object EntityResolution {

  type CDM = CDM18

  // We keep these as local variables in the object (as opposed to state closed over in `annotateTime`) because we want
  // easy access to them for logging/debugging purposes.
  var monotonicTime: Long = 0
  var sampledTime: Long = 0
  var nodeCount: Long = 0

  // This is just for logging state held onto by the `EventResolution` branch of `erWithoutRemaps`
  var activeChains: MutableMap[EventResolution.EventKey, EventResolution.EventMergeState] = MutableMap.empty

  // This is just for logging state held onto by `asyncDeduplicate` .
  var blockedEdgesCount: Long = 0
  val blockingNodes: MutableSet[UUID] = MutableSet.empty[UUID]


  def apply(
    cdm2cdmMap: AlmostMap[CdmUUID, CdmUUID],                    // Map from CDM to CDM
    cdm2admMap: AlmostMap[CdmUUID, AdmUUID],                    // Map from CDM to ADM
    blocking: MutableMap[CdmUUID, (List[Edge], Set[CdmUUID])],  // Map of things blocked

    seenNodesSet: AlmostSet[AdmUUID],                           // Set of nodes seen so far
    seenEdgesSet: AlmostSet[EdgeAdm2Adm]                        // Set of edges seen so far
  ): Flow[(String,CDM), Either[ADM, EdgeAdm2Adm], NotUsed] = {

    val config: Config = ConfigFactory.load()

    val maxTimeJump: Long      = (config.getInt("adapt.adm.maxtimejumpsecs")  seconds).toNanos
    val uuidExpiryNanos: Long  = (config.getInt("adapt.adm.cdmexpiryseconds") seconds).toNanos
    val uuidExpiryCount: Long  = config.getLong("adapt.adm.cdmexpirycount")
    val eventExpiryNanos: Long = (config.getInt("adapt.adm.eventexpirysecs")  seconds).toNanos
    val eventExpiryCount: Long = config.getLong("adapt.adm.eventexpirycount")
    val maxEventsMerged: Int   = config.getInt("adapt.adm.maxeventsmerged")

    val maxTimeMarker = ("", Timed(Time.max, TimeMarker(Long.MaxValue)))
    lazy val maxTimeRemapper = Timed(Time.max, JustTime)

    Flow[(String, CDM)]
      .via(annotateTime(maxTimeJump))                                         // Annotate with a monotonic time
      .concat(Source.fromIterator(() => Iterator(maxTimeMarker)))             // Expire everything in UuidRemapper
      .via(erWithoutRemaps(eventExpiryNanos, eventExpiryCount, maxEventsMerged, activeChains))   // Entity resolution without remaps
      .concat(Source.fromIterator(() => Iterator(maxTimeRemapper)))           // Expire everything in UuidRemapper
      .via(UuidRemapper(uuidExpiryNanos, uuidExpiryCount, cdm2cdmMap, cdm2admMap, blocking))    // Remap UUIDs
      .via(asyncDeduplicate(seenNodesSet, seenEdgesSet, MutableMap.empty))    // Order nodes/edges
  }


  private type TimeFlow = Flow[(String,CDM), (String,Timed[CDM]), NotUsed]

  // Since TA1s cannot be trusted to have a regularly increasing time value, we can't rely on just this for expiring
  // things. The solution is to thread thorugh a node count - we're pretty sure that will increase proportionally to the
  // number of nodes ingested ;)
  case class Time(nanos: Long, count: Long)
  object Time {
    def max: Time = Time(Long.MaxValue, Long.MaxValue)
  }

  case class Timed[+T](time: Time, unwrap: T)

  // Annotate a flow of `CDM` with a monotonic time value corresponding roughly to the time when the CDM events were
  // observed on the instrumented machine.
  private def annotateTime(maxTimeJump: Long): TimeFlow = {

    // Map a CDM onto a possible timestamp
    def timestampOf: CDM => Option[Long] = {
      case s: Subject => Some(s.startTimestampNanos)
      case e: Event => Some(e.timestampNanos)
      case t: TimeMarker => Some(t.timestampNanos)
      case _ => None
    }

    Flow[(String,CDM)].map { case (provider, cdm: CDM) =>

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


  private type ErFlow = Flow[(String,Timed[CDM]), Timed[UuidRemapperInfo], NotUsed]

  // Perform entity resolution on stream of CDMs to convert them into ADMs, Edges, and general information to hand of to
  // the UUID remapping stage.
  private def erWithoutRemaps(
    eventExpiryTime: Long,
    eventExpiryCount: Long,
    maxEventsMerged: Int,
    activeChains: MutableMap[EventResolution.EventKey, EventResolution.EventMergeState]
  ): ErFlow =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val broadcast = b.add(Broadcast[(String,Timed[CDM])](3))
      val merge = b.add(Merge[Timed[UuidRemapperInfo]](3))

      broadcast ~> EventResolution(eventExpiryTime, eventExpiryCount, maxEventsMerged, activeChains) ~> merge
      broadcast ~> SubjectResolution.apply                                         ~> merge
      broadcast ~> OtherResolution.apply                                           ~> merge

      FlowShape(broadcast.in, merge.out)
    })


  private type OrderAndDedupFlow = Flow[Either[ADM, EdgeAdm2Adm], Either[ADM, EdgeAdm2Adm], NotUsed]

  // Prevents nodes/edges from being emitted twice. Also prevents edges from being emitted before both of their
  // endpoints have been emitted.
  private def asyncDeduplicate(
    seenNodes: AlmostSet[AdmUUID],
    seenEdges: AlmostSet[EdgeAdm2Adm],
    blockedEdges: MutableMap[UUID, List[EdgeAdm2Adm]]     // Edges blocked by UUIDs that haven't arrived yet
  ): OrderAndDedupFlow = Flow[Either[ADM, EdgeAdm2Adm]].statefulMapConcat[Either[ADM, EdgeAdm2Adm]](() => {

    // Try to emit an edge. If either end of the edge hasn't arrived, add it to the blocked map.
    def emitEdge(edge: EdgeAdm2Adm): Option[Either[ADM, EdgeAdm2Adm]] = edge match {
      case EdgeAdm2Adm(src, _, _) if !seenNodes.contains(src) =>
        blockedEdges(src) = edge :: blockedEdges.getOrElse(src, Nil)
        blockingNodes += src
        None

      case EdgeAdm2Adm(_, _, tgt) if !seenNodes.contains(tgt) =>
        blockedEdges(tgt) = edge :: blockedEdges.getOrElse(tgt, Nil)
        blockingNodes += tgt
        None

      case _ =>
        blockedEdgesCount -= 1
        Some(Right(edge))
    }

    {
      // Duplicate node or edge
      case Left(adm) if seenNodes.contains(adm.uuid) => Nil
      case Right(edge) if seenEdges.contains(edge) => Nil

      // New node
      case Left(adm) =>
        seenNodes.add(adm.uuid)
        blockingNodes -= adm.uuid
        val emit = blockedEdges.remove(adm.uuid).getOrElse(Nil).flatMap(e => emitEdge(e).toList)
        Left(adm) :: emit

      // New edge
      case Right(edge) =>
        seenEdges.add(edge)
        blockedEdgesCount += 1
        emitEdge(edge).toList
    }
  })
}
