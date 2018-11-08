package com.galois.adapt

import java.io.{FileOutputStream, PrintWriter}

import akka.actor.Actor
import com.galois.adapt.AdaptConfig.HostName
import com.galois.adapt.adm.{DeduplicateNodesAndEdges, EntityResolution}
import spray.json.JsObject

import scala.collection.mutable
import scala.util.Try

class StatusActor extends Actor {

  var currentlyIngesting = false
  var duplicateAlarmCount = 0L
  val recentPopulationLogs: mutable.Map[String, PopulationLog] = mutable.Map.empty
  val totalPopulationLogs: mutable.Map[String, PopulationLog] = mutable.Map.empty

  // This is aggrgeated over all dedup stages in the akka-streams graph we are running
  def blockEdgesCount: Long = DeduplicateNodesAndEdges.blockedEdgesCount
  def blockingNodes: Long = DeduplicateNodesAndEdges.blockingNodes.size

  // Per host info about possibly sharded large maps/sets
  def uuidRemapperSize: Map[HostName, ShardedInfo] =
    Application.hostNames.map(h => h -> ShardedInfo.fromCounts(
      (Application.cdm2cdmMaps(h) zip Application.cdm2admMaps(h)).map { case (m1, m2) => m1.size() + m2.size() },
      Application.uuidRemapperShardCounts(h)
    )).toMap
  def deduplicateSize: Map[HostName, ShardedInfo] =
    (Application.hostNameForAllHosts :: Application.hostNames).map(h => h -> ShardedInfo.fromCounts(
      (Application.seenNodesMaps(h) zip Application.seenEdgesMaps(h)).map { case (n, e) => n.size() + e.size() },
      Application.dedupShardCounts(h)
    )).toMap

  // UuidRemapper related stats
  def uuidsBlocking: Int = Application.blockedEdgesMaps.values.map(_.map(_.size).sum).sum
  def blockedUuidResponses: Int = Application.blockedEdgesMaps.values.map(_.map(_.values.map(x => x._1.length).sum).sum).sum


  // Info about time in entity resolution stream
  def currentTime: Long = EntityResolution.monotonicTime
  def sampledTime: Long = EntityResolution.sampledTime


  def calculateStats(): StatusReport = StatusReport(
    currentlyIngesting,
    recentPopulationLogs.values.toList,
    uuidRemapperSize,
    deduplicateSize,
    uuidsBlocking,
    blockedUuidResponses,
    currentTime,
    sampledTime,
    duplicateAlarmCount
  )


  def receive = {
    case GetStats => sender() ! calculateStats()

    case LogToDisk(p: String) => Try {
      import ApiJsonProtocol._
      import spray.json._

      val logFile = new PrintWriter(new FileOutputStream(new java.io.File(p), true))
      logFile.append(calculateStats().toJson.compactPrint + "\n")
      logFile.close()
    }

    case IncrementAlarmDuplicateCount => duplicateAlarmCount += 1
    case p: PopulationLog => recentPopulationLogs.put(p.name, p)
    case InitMsg => currentlyIngesting = true
    case CompleteMsg => currentlyIngesting = false
  }
}

case object GetStats
case class LogToDisk(path: String)

case class ShardedInfo(
  totalSize: Long,                     // total number of entries stored
  totalVisit: Long,                    // total number of visits
  shardSizeDistribution: Array[Long],  // percentage of entries stored per shard
  shardVisitDistribution: Array[Long]  // percentage of visits per shard
)
object ShardedInfo {
  def fromCounts(sizeCounts: Array[Long], visitCounts: Array[Long]): ShardedInfo = {
    val (totalSize, shardSizeDistribution) = shardCountDistribution(sizeCounts)
    val (totalVisit, shardVisitDistribution) = shardCountDistribution(visitCounts)
    ShardedInfo(totalSize, totalVisit, shardSizeDistribution, shardVisitDistribution)
  }

  private def shardCountDistribution(counts: Array[Long]): (Long, Array[Long]) = {
    val totalCount = counts.sum
    val distribution = counts.map(i => Math.round(i.toDouble * 100 / totalCount.toDouble))
    (totalCount, distribution)
  }
}

case class PopulationLog(
  name: String,
  position: Long,
  every: Int,
  counter: Map[String,Long],
  totalCounter: Map[String,Long],
  secondsThisEvery: Double
)

case class IncrementCount(name: String)
case class DecrementCount(name: String)

case class StatusReport(
  currentlyIngesting: Boolean,
  recentPopulationLogs: List[PopulationLog],
  uuidRemapperSize: Map[HostName, ShardedInfo],
  deduplicateSize: Map[HostName, ShardedInfo],
  uuidsBlocking: Int,
  blockedUuidResponses: Int,
  currentTime: Long,
  sampledTime: Long,
  duplicateAlarmCount: Long
)

case object IncrementAlarmDuplicateCount