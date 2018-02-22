package com.galois.adapt

import akka.actor.Actor

import scala.collection.mutable
import scala.util.Try

class StatusActor extends Actor {

  var currentlyIngesting = false

  val generalRecords = mutable.Map.empty[String,Any]

  val populationLog = mutable.Map.empty[String, Long]

  var admFuturesCountsObserved = 0
  var admFuturesTotal = 0
  var admFuturesLastObserved = 0

  def receive = {
    case GetStats => sender() ! StatusReport(
      currentlyIngesting,
      generalRecords.toMap.mapValues(_.toString),
      populationLog.toMap,
      admFuturesLastObserved,
      Try(admFuturesTotal.toFloat / admFuturesCountsObserved.toFloat).getOrElse(0F)
    )

    case PopulationLog(name, position, every, counterMap, admFuturesCount, countOutOfAsyncBuffer, averageMillisecondsInAsyncBuffer) =>
      counterMap.foreach{ case (k,v) =>
        val key = s"$name: $k"
        populationLog += (key -> (populationLog.getOrElse(key, 0L) + v))
      }
      admFuturesCountsObserved += 1
      admFuturesTotal += admFuturesCount
      admFuturesLastObserved = admFuturesCount

      generalRecords += ("countOutOfAsyncBuffer" -> countOutOfAsyncBuffer)
      generalRecords += ("averageMillisecondsInAsyncBuffer" -> averageMillisecondsInAsyncBuffer)

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
  admFuturesCount: Int,
  countOutOfAsyncBuffer: Int,
  averageMillisInAsyncBuffer: Float
)

case class IncrementCount(name: String)
case class DecrementCount(name: String)

case class StatusReport(
  currentlyIngesting: Boolean,
  generalRecords: Map[String,String],
  population: Map[String, Long],
  admFuturesSize: Int,
  admFuturesAverageSize: Float
)