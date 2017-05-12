package com.galois.adapt

import java.util.UUID

import akka.stream.scaladsl.{Flow, Source}
import com.galois.adapt.FlowComponents.{predicateTypeLabeler, sortedEventAccumulator}
import com.galois.adapt.cdm17.{CDM17, EVENT_ACCEPT, EVENT_CLOSE, EVENT_CONNECT, EVENT_OPEN, EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG, EVENT_SENDMSG, EVENT_SENDTO, EVENT_WRITE, Event}
import org.mapdb.DB
import scala.collection.mutable.{Map => MutableMap, SortedSet => MutableSortedSet}
import FlowComponents._

object NetFlowStream {

  val netflowEventTypes = List(EVENT_ACCEPT, EVENT_CONNECT, EVENT_OPEN, EVENT_CLOSE, EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG, EVENT_SENDTO, EVENT_SENDMSG, EVENT_WRITE)

  def netFlowFeatureGenerator(commandSource: Source[ProcessingCommand,_], db: DB) =
    predicateTypeLabeler(commandSource, db)
      .collect{ case Tuple4("NetFlowObject", predUuid, event, netFlow: CDM17) => (predUuid, event, netFlow) }
      .via(sortedEventAccumulator(_._1, commandSource, db))
      .via(netFlowFeatureExtractor)


  def netFlowFeatureExtractor = Flow[(UUID, MutableSortedSet[Event])]
    .mapConcat[(String, UUID, MutableMap[String,Any], Set[UUID])] { case (netFlowUuid, eSet) =>
    val eList = eSet.toList
    val m = MutableMap.empty[String, Any]
    var allRelatedUUIDs = eSet.flatMap(e => List(Some(e.uuid), e.predicateObject, e.predicateObject2, Some(e.subjectUuid)).flatten)
    m("lifetimeWriteRateBytesPerSecond") = eSet.sizePerSecond(EVENT_WRITE)
    m("lifetimeReadRateBytesPerSecond") = eSet.sizePerSecond(EVENT_READ)
    m("duration-SecondsBetweenFirstAndLastEvent") = eSet.timeBetween(None, None) / 1e9
    m("countOfDistinctSubjectsWithEventToThisNetFlow") = eSet.map(_.subjectUuid).size
//      m("distinctFileReadCountByProcessesWritingToThisNetFlow") = "TODO"                                // TODO: needs pairing with Files (and join on Process UUID)
    m("totalBytesRead") = eList.collect { case e if List(EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG).contains(e.eventType) => e.size.getOrElse(0L) }.sum
    m("totalBytesWritten") = eList.collect { case e if List(EVENT_WRITE, EVENT_SENDTO, EVENT_SENDMSG).contains(e.eventType) => e.size.getOrElse(0L) }.sum
    m("stdDevBetweenNetFlowWrites") = {
      val writeList = eList.filter(e => List(EVENT_SENDTO, EVENT_SENDMSG, EVENT_WRITE) contains e.eventType)
      if (writeList.length >= 2) {
        val differences = writeList.sliding(2).map(pair => pair.last.timestampNanos - pair.head.timestampNanos).toList
        val mean: Double = differences.sum.toDouble / differences.length
        val dmms = differences.map(d => Math.pow(d - mean, 2)).sum / differences.length
        val stdev = Math.sqrt(dmms)
        if (stdev > 0D) stdev else 0D
      } else 0D
    }
    m("averageWriteSize") = {
      val writes = eList.filter(e => List(EVENT_SENDTO, EVENT_SENDMSG, EVENT_WRITE) contains e.eventType)
      if (writes.nonEmpty) writes.flatMap(_.size).sum.toDouble / writes.length else 0D
    }
    // TODO: Alarm: port 1337 ???
    //    m("ALARM: Port 1337") =

    netflowEventTypes.foreach(t =>
      m("count_" + t.toString) = eSet.count(_.eventType == t)
    )


    val viewDefinitions = Map(
      "Netflow Read Write Rate Lifetime" -> List("duration-SecondsBetweenFirstAndLastEvent", "lifetimeReadRateBytesPerSecond", "lifetimeWriteRateBytesPerSecond")
    , "Netflow Write Stats" -> List("lifetimeWriteRateBytesPerSecond", "totalBytesWritten", "count_EVENT_SENDMSG", "count_EVENT_SENDTO", "count_EVENT_WRITE")
    , "Netflow Read Stats" -> List("lifetimeReadRateBytesPerSecond", "totalBytesRead", "count_EVENT_READ", "count_EVENT_RECVFROM", "count_EVENT_RECVMSG")
    , "Beaconing Behavior" -> List("stdDevBetweenNetFlowWrites", "duration-SecondsBetweenFirstAndLastEvent", "averageWriteSize")
//    , "ALARM: Port 1337" -> List()
    )

    // TODO: remove
//    val req = viewDefinitions.values.flatten.toSet.forall(m.keySet.contains)
//    if (!req) println("WARNING: " + viewDefinitions.values.flatten.toSet[String].map(x => x -> m.keySet.contains(x)).filter(x => !x._2))

    viewDefinitions.toList.map { case (name, columnList) =>
      (name, netFlowUuid, m.filter(t => columnList.contains(t._1)), allRelatedUUIDs.toSet)
    } ++ List(("All NetFlow Features", netFlowUuid, m, allRelatedUUIDs.toSet))
  }
}
