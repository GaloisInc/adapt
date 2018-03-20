package com.galois.adapt

import akka.actor.Actor

import scala.collection.mutable
import scala.util.Try

class StatusActor extends Actor {

  var currentlyIngesting = false

  val generalRecords = mutable.Map.empty[String,Any]

  val populationLog = mutable.Map.empty[String, Long]


  def receive = {
    case GetStats => sender() ! StatusReport(
      currentlyIngesting,
      generalRecords.toMap.mapValues(_.toString),
      populationLog.toMap
    )

    case PopulationLog(name, position, every, counterMap, secondsThisEvery, blockEdgesCount, blockingNodes, uuidsBlocking, blockedUuidResponses, cdm2cdmSize: Long, cdm2admSize, seenNodesSize, seenEdgesSize, currentTime) =>
      counterMap.foreach{ case (k,v) =>
        val key = s"$name: $k"
        populationLog += (key -> (populationLog.getOrElse(key, 0L) + v))
      }

      generalRecords += ("every" -> every)
      generalRecords += ("secondsThisEvery" -> secondsThisEvery)
      generalRecords += ("blockEdgesCount" -> blockEdgesCount)
      generalRecords += ("blockingNodes" -> blockingNodes)
      generalRecords += ("uuidsBlocking" -> blockingNodes)
      generalRecords += ("blockedUuidResponses" -> blockingNodes)
      generalRecords += ("cdm2cdmSize" -> cdm2cdmSize)
      generalRecords += ("cdm2admSize" -> cdm2admSize)
      generalRecords += ("seenNodesSize" -> seenNodesSize)
      generalRecords += ("seenEdgesSize" -> seenEdgesSize)
      generalRecords += ("monotonicTime" -> currentTime)

    case InitMsg => currentlyIngesting = true
    case CompleteMsg => currentlyIngesting = false
  }
}

case object GetStats

case class PopulationLog(
  name: String,
  position: Long,
  every: Int,
  counter: Map[String,Long],
  secondsThisEvery: Double,
  blockEdgesCount: Long,
  blockingNodes: Long,
  uuidsBlocking: Int,
  blockedUuidResponses: Int,
  cdm2cdmSize: Long,
  cdm2admSize: Long,
  seenNodesSize: Long,
  seenEdgesSize: Long,
  currentTime: Long
)

case class IncrementCount(name: String)
case class DecrementCount(name: String)

case class StatusReport(
  currentlyIngesting: Boolean,
  generalRecords: Map[String,String],
  population: Map[String, Long]
)