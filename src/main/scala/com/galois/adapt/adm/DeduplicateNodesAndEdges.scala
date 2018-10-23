package com.galois.adapt.adm

import java.util.UUID

import akka.NotUsed
import akka.stream.{FlowShape, OverflowStrategy}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, MergePreferred, Partition}
import com.galois.adapt.MapSetUtils.AlmostSet
import com.galois.adapt.adm.EntityResolution.{blockedEdgesCount, blockingNodes}

import scala.collection.mutable.{Map => MutableMap}

object DeduplicateNodesAndEdges {

  type OrderAndDedupFlow = Flow[Either[ADM, EdgeAdm2Adm], Either[ADM, EdgeAdm2Adm], NotUsed]


  /***************************************************************************************
   * Sharded variant                                                                     *
   ***************************************************************************************/

  // For the sharded variant, we need to keep track of extra info with edges: namely which endpoint have been checked
  // already.
  private sealed trait EdgeEndpointCheck
  private case class NoEndpointsChecked(edge: EdgeAdm2Adm) extends EdgeEndpointCheck
  private case class SrcEndpointChecked(edge: EdgeAdm2Adm) extends EdgeEndpointCheck
  private case class BothEndpointsChecked(edge: EdgeAdm2Adm) extends EdgeEndpointCheck

  // Figure out which shard an annotated edge or ADM should be routed to
  private def partitioner(numShards: Int): Either[ADM, EdgeEndpointCheck] => Int = {
    def uuidPartition(u: UUID): Int =
      (Math.abs(u.getLeastSignificantBits * 7 + u.getMostSignificantBits * 31) % numShards).intValue()

    {
      case Left(adm) => uuidPartition(adm.uuid.uuid)
      case Right(NoEndpointsChecked(e)) => uuidPartition(e.src)
      case Right(SrcEndpointChecked(e)) => uuidPartition(e.tgt)
      case Right(BothEndpointsChecked(e)) => uuidPartition(e.tgt) // should never occur
    }
  }

  // Prevents nodes/edges from being emitted twice. Also prevents edges from being emitted before both of their
  // endpoints have been emitted.
  def sharded(
      numShards: Int,

      seenNodesMaps: Array[AlmostSet[AdmUUID]],
      seenEdgesMaps: Array[AlmostSet[EdgeAdm2Adm]]
  ): OrderAndDedupFlow = Flow.fromGraph[Either[ADM, EdgeAdm2Adm], Either[ADM, EdgeAdm2Adm], NotUsed](GraphDSL.create() {
    implicit b =>
    import GraphDSL.Implicits._

    /*
     *
     *                 +-------------------[small buffer]------------<---------------------------+
     *                 |                                                                         |
     *                 |                                                                         |
     *                 |                                                                         |
     *                 |                        +----> oneShard(0, ...) +---+                    |
     *                 |                        |                           |                    |
     *                 v                        +----> oneShard(1, ...) +---+                    +
     * +--annotate-> loopBack > partitionShards |                           | mergeShards +-> decider +--->
     *                                          |          ...              |
     *                                          |                           |
     *                                          +----> oneShard(n, ...) +---+
     *
     */

    val annotate = b.add(Flow.fromFunction[Either[ADM, EdgeAdm2Adm], Either[ADM, EdgeEndpointCheck]] {
      case Left(a) => Left(a)
      case Right(e) => Right(NoEndpointsChecked(e))
    })

    def thisPartitioner = partitioner(numShards)

    val loopBack = b.add(MergePreferred[Either[ADM, EdgeEndpointCheck]](1))
    val partitionShards = b.add(Partition[Either[ADM, EdgeEndpointCheck]](numShards, thisPartitioner))
    val mergeShards = b.add(Merge[Either[ADM, EdgeEndpointCheck]](numShards))
    val decider = b.add(Partition[Either[ADM, EdgeEndpointCheck]](2, {
      case Left(_) => 0
      case Right(BothEndpointsChecked(_)) => 0
      case _ => 1
    }))
    val ret = b.add(Flow[Either[ADM, EdgeAdm2Adm]])

    annotate.out ~> loopBack.in(0)
    loopBack.out ~> partitionShards.in

    for (i <- 0 until numShards) {
      partitionShards.out(i) ~>
        oneShard(seenNodesMaps(i), seenEdgesMaps(i), MutableMap.empty) ~>
        mergeShards.in(i)
    }

    mergeShards.out ~> decider.in

    decider.out(1).buffer(
      1000,
      OverflowStrategy.backpressure
    ) ~> loopBack.preferred

    decider.out(0).map {
      case Left(a) => Left(a)
      case Right(BothEndpointsChecked(e)) => Right(e)
      case Right(a) =>
        throw new Exception(s"DeduplicateNodesAndEdges: decider encountered an edge that was not remapped: $a")
    } ~> ret.in

    FlowShape(annotate.in, ret.out)
  })

  private def oneShard(
      seenNodes: AlmostSet[AdmUUID],
      seenEdges: AlmostSet[EdgeAdm2Adm],

      blockedEdges: MutableMap[AdmUUID, List[EdgeEndpointCheck]]
  ): Flow[Either[ADM, EdgeEndpointCheck], Either[ADM, EdgeEndpointCheck], NotUsed] = Flow[Either[ADM, EdgeEndpointCheck]].mapConcat {
      case l @ Left(adm: ADM) =>
        val unblocked = blockedEdges.getOrElse(adm.uuid, Nil).map(Right(_))
        if (seenNodes.add(adm.uuid)) {
          l :: unblocked
        } else {
          unblocked
        }

      case Right(a @ NoEndpointsChecked(e)) =>
        if (seenNodes.contains(e.src)) {
          List(Right(SrcEndpointChecked(e)))
        } else {
          blockedEdges(e.src) = a :: blockedEdges.getOrElse(e.src, Nil)
          Nil
        }

      case Right(a @ SrcEndpointChecked(e)) =>
        if (seenNodes.contains(e.tgt)) {
          if (seenEdges.add(e)) {
            List(Right(BothEndpointsChecked(e)))
          } else {
            Nil
          }
        } else {
          blockedEdges(e.tgt) = a :: blockedEdges.getOrElse(e.tgt, Nil)
          Nil
        }

      case Right(BothEndpointsChecked(e)) =>
        throw new Exception(s"DeduplicateNodesAndEdges: shard encountered an edge that is already fully checked: $e")
    }


  /***************************************************************************************
   * Unsharded variant                                                                   *
   ***************************************************************************************/

  def apply(
    seenNodes: AlmostSet[AdmUUID],
    seenEdges: AlmostSet[EdgeAdm2Adm]
  ): OrderAndDedupFlow = Flow[Either[ADM, EdgeAdm2Adm]].statefulMapConcat[Either[ADM, EdgeAdm2Adm]](() => {

    // Edges blocked by UUIDs that haven't arrived yet
    val blockedEdges: MutableMap[UUID, List[EdgeAdm2Adm]] = MutableMap.empty

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
