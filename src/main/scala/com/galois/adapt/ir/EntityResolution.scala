package com.galois.adapt.ir

import java.util.UUID

import com.galois.adapt.ir.ERStreamComponents._
import com.galois.adapt.cdm17._

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import scala.concurrent.{Await, ExecutionContext, Future}
import com.galois.adapt.FlowComponents
import akka.actor.Props
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.dispatch.ExecutionContexts
import akka.util.Timeout
import akka.stream.{Attributes, DelayOverflowStrategy, FlowShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Partition, Source}
import com.galois.adapt.ir.UuidRemapper.{GetCdm2Ir, ResultOfGetCdm2Ir}

import scala.reflect._

object EntityResolution {

  def apply(uuidRemapper: ActorRef)(implicit system: ActorSystem): Flow[CDM, Either[EdgeIr2Ir, IR], _] = {

    implicit val ec: ExecutionContext = system.dispatcher
    implicit val timout: Timeout = Timeout.durationToTimeout(21474830 seconds) // Timeout(30 seconds)
    val parallelism = 10000

    erWithoutRemapsFlow(uuidRemapper)
      .via(remapEdgeUuids(uuidRemapper))
        .via(awaitAndDeduplicate(parallelism))
  }

  // Perform entity resolution on stream of CDMs to convert them into IRs
  private def erWithoutRemapsFlow(uuidRemapper: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext): Flow[CDM, Future[Either[Edge[_, _], IR]], _] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      /* The original CDM stream gets split into one stream per merged CDM type, and the rest. We merge
       *
       *     - Events
       *     - Subjects
       *
       * Each of these streams maintains the bare minimum of state is maintained to perform entity resolution. Some of
       * that state is kept implicitly via 'groupBy' operations.
       *
       * Then, every time a new IR element is being created, a message is sent to a renaming actor.
       * This actor maintains information about the mapping from old CDM to new resolved IR. However,
       * the edges emitted by this flow still have UUIDs that point to the old CDM.
       *
       *                             _--->  Event ----_
       *                            /                  \
       *                      --CDM +--- > Subjects ---+-- IR (with possibly unremapped edges) -->
       *                            \                  /
       *                             `---> `Other ----'
       *
       *                                       \
       *                                        v
       *                                      ,-'''''''''-.
       *                                     | renameActor |
       *                                      `-_________-'
       */

      // split stream into 7 components (one for each CDM type)
      val broadcast = b.add(Broadcast[CDM](3))

      // merge those components back together
      val merge = b.add(Merge[Future[Either[Edge[_, _], IR]]](3))


      // Connect the graph
      broadcast.out(0).collect({ case e: Event => e })    .via(eventResolution(uuidRemapper))    ~> merge.in(0)
      broadcast.out(1).collect({ case s: Subject => s })  .via(subjectResolution(uuidRemapper))  ~> merge.in(1)
      broadcast.out(2)                                    .via(otherResolution(uuidRemapper))    ~> merge.in(2)

      // expose ports
      FlowShape(broadcast.in, merge.out)
    })


  // Remap UUIDs in Edges from CDMs to other IRs
  private def remapEdgeUuids(uuidRemapper: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext): Flow[Future[Either[Edge[_, _], IR]], Future[Either[EdgeIr2Ir, IR]], _] = {

    def remapEdge[From, To](edge: Edge[From, To]): Future[EdgeIr2Ir] = edge match {
      case EdgeIr2Ir(src, lbl, tgt) => Future.successful(EdgeIr2Ir(src, lbl, tgt))

      case EdgeCdm2Ir(src, lbl, tgt) => for {
        src_new <- (uuidRemapper ? GetCdm2Ir(src)).mapTo[ResultOfGetCdm2Ir]
      } yield EdgeIr2Ir(src_new.target, lbl, tgt)

      case EdgeIr2Cdm(src, lbl, tgt) => for {
        tgt_new <- (uuidRemapper ? GetCdm2Ir(tgt)).mapTo[ResultOfGetCdm2Ir]
      } yield EdgeIr2Ir(src, lbl, tgt_new.target)

      case EdgeCdm2Cdm(src, lbl, tgt) => for {
        src_new <- (uuidRemapper ? GetCdm2Ir(src)).mapTo[ResultOfGetCdm2Ir]
        tgt_new <- (uuidRemapper ? GetCdm2Ir(tgt)).mapTo[ResultOfGetCdm2Ir]
      } yield EdgeIr2Ir(src_new.target, lbl, tgt_new.target)
    }

    Flow[Future[Either[Edge[_, _], IR]]].map(_.flatMap {
      case Left(edge) => remapEdge(edge).map(Left(_))
      case Right(node) => Future.successful(Right(node))
    })
  }

  // This does three things:
  //
  //   * await the Futures (processing first the Futures that finish first)
  //   * prevent nodes with the same UUID from being re-emitted
  //   * prevent Edges from being emitted before both of their endpoints have been emitted
  //
  private def awaitAndDeduplicate(parallelism: Int)(implicit timeout: Timeout, ec: ExecutionContext): Flow[Future[Either[EdgeIr2Ir, IR]], Either[EdgeIr2Ir, IR], _] =
    Flow[Future[Either[EdgeIr2Ir, IR]]]
      .mapAsyncUnordered[Either[EdgeIr2Ir, IR]](parallelism)(identity)
      .statefulMapConcat[Either[EdgeIr2Ir, IR]](() => {
        // UUIDs of nodes seen so far
        val seen: collection.mutable.Set[UUID] = collection.mutable.Set.empty[UUID]

        // Edges blocked by UUIDsthat haven't arrived yet
        val blockedEdges: collection.mutable.Map[UUID, List[EdgeIr2Ir]] = collection.mutable.Map.empty[UUID, List[EdgeIr2Ir]]

        // Try to emit an edge. If either end of the edge hasn't arrived, add it to the blocked map
        def emitEdge(edge: EdgeIr2Ir): List[Either[EdgeIr2Ir, IR]] = {
          if (!seen.contains(edge.src)) {
            blockedEdges(edge.src) = edge :: blockedEdges.getOrElse(edge.src, Nil)
            Nil
          } else if (!seen.contains(edge.tgt)) {
            blockedEdges(edge.tgt) = edge :: blockedEdges.getOrElse(edge.tgt, Nil)
            Nil
          } else {
            List(Left(edge))
          }
        }

        {
          case Left(edge) => emitEdge(edge)
          case Right(ir) if seen.contains(ir.uuid) => Nil
          case Right(ir) =>
            seen.add(ir.uuid)
            val emit = blockedEdges.remove(ir.uuid).getOrElse(Nil).flatMap(emitEdge)
            Right(ir) :: emit
        }
      })
}
