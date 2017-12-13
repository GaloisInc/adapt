package com.galois.adapt.adm

import java.util.UUID

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge}
import akka.util.Timeout
import com.galois.adapt.adm.ERStreamComponents._
import com.galois.adapt.adm.UuidRemapper.{GetCdm2Adm, ResultOfGetCdm2Adm}
import com.galois.adapt.cdm17._
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps


object EntityResolution {

  val config: Config = ConfigFactory.load()

  def apply(uuidRemapper: ActorRef)(implicit system: ActorSystem): Flow[CDM, Either[EdgeAdm2Adm, ADM], _] = {

    implicit val ec: ExecutionContext = system.dispatcher
    implicit val timeout: Timeout = Timeout.durationToTimeout(config.getLong("adapt.adm.timeoutSeconds") seconds)
    val parallelism: Int = config.getInt("adapt.adm.parallelism")

    erWithoutRemapsFlow(uuidRemapper)
      .via(remapEdgeUuids(uuidRemapper))
        .via(awaitAndDeduplicate(parallelism))
  }

  // Perform entity resolution on stream of CDMs to convert them into ADMs
  private def erWithoutRemapsFlow(uuidRemapper: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext): Flow[CDM, Future[Either[Edge[_, _], ADM]], _] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      /* The original CDM stream gets split into one stream per merged CDM type, and the rest. We merge
       *
       *     - Events
       *     - Subjects
       *
       * Each of these streams maintains the bare minimum of state to perform entity resolution. Some of that state is
       * kept implicitly via 'groupBy' operations.
       *
       * Then, every time a new ADM element is being created, a message is sent to the 'uuidRemapper' actor. This actor
       * maintains information about the mapping from old CDM to new resolved ADM. However, the edges emitted by this
       * flow still have UUIDs that point to the old CDM.
       *
       *                             _--->  Event ----_
       *                            /                  \
       *                      --CDM +--- > Subjects ---+-- ADM (with possibly unremapped edges) -->
       *                            \                  /
       *                             `---> `Other ----'
       *
       *                                       \
       *                                        v
       *                                      ,-''''''''''-.
       *                                     | uuidRemapper |
       *                                      `-__________-'
       */

      val broadcast = b.add(Broadcast[CDM](3))
      val merge = b.add(Merge[Future[Either[Edge[_, _], ADM]]](3))

      broadcast.out(0).collect({ case e: Event => e })    .via(eventResolution(uuidRemapper))    ~> merge.in(0)
      broadcast.out(1).collect({ case s: Subject => s })  .via(subjectResolution(uuidRemapper))  ~> merge.in(1)
      broadcast.out(2)                                    .via(otherResolution(uuidRemapper))    ~> merge.in(2)

      FlowShape(broadcast.in, merge.out)
    })


  type RemapEdgeFlow = Flow[Future[Either[Edge[_, _], ADM]], Future[Either[EdgeAdm2Adm, ADM]], _]

  // Remap UUIDs in Edges from CDMs to other ADMs
  private def remapEdgeUuids(uuidRemapper: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext): RemapEdgeFlow = {

    def remapEdge[From, To](edge: Edge[From, To]): Future[EdgeAdm2Adm] = edge match {
      case EdgeAdm2Adm(src, lbl, tgt) => Future.successful(EdgeAdm2Adm(src, lbl, tgt))

      case EdgeCdm2Adm(src, lbl, tgt) => for {
        src_new <- (uuidRemapper ? GetCdm2Adm(src)).mapTo[ResultOfGetCdm2Adm]
      } yield EdgeAdm2Adm(src_new.target, lbl, tgt)

      case EdgeAdm2Cdm(src, lbl, tgt) => for {
        tgt_new <- (uuidRemapper ? GetCdm2Adm(tgt)).mapTo[ResultOfGetCdm2Adm]
      } yield EdgeAdm2Adm(src, lbl, tgt_new.target)

      case EdgeCdm2Cdm(src, lbl, tgt) => for {
        src_new <- (uuidRemapper ? GetCdm2Adm(src)).mapTo[ResultOfGetCdm2Adm]
        tgt_new <- (uuidRemapper ? GetCdm2Adm(tgt)).mapTo[ResultOfGetCdm2Adm]
      } yield EdgeAdm2Adm(src_new.target, lbl, tgt_new.target)
    }

    Flow[Future[Either[Edge[_, _], ADM]]].map(_.flatMap {
      case Left(edge) => remapEdge(edge).map(Left(_))
      case Right(node) => Future.successful(Right(node))
    })
  }


  type OrderAndDedupFlow = Flow[Future[Either[EdgeAdm2Adm, ADM]], Either[EdgeAdm2Adm, ADM], _]

  // This does several things:
  //
  //   - await the Futures (processing first the Futures that finish first)
  //   - prevent nodes with the same UUID from being re-emitted
  //   - prevent duplicate edges from being re-emitted
  //   - prevent Edges from being emitted before both of their endpoints have been emitted
  //
  private def awaitAndDeduplicate(parallelism: Int)(implicit t: Timeout, ec: ExecutionContext): OrderAndDedupFlow =
    Flow[Future[Either[EdgeAdm2Adm, ADM]]]
      .mapAsyncUnordered[Either[EdgeAdm2Adm, ADM]](parallelism)(identity)
      .statefulMapConcat[Either[EdgeAdm2Adm, ADM]](() => {

        val seenNodes = MutableSet.empty[UUID]                       // UUIDs of nodes seen (and emitted) so far
        val seenEdges = MutableSet.empty[EdgeAdm2Adm]                // edges seen (not necessarily emitted)
        val blockedEdges = MutableMap.empty[UUID, List[EdgeAdm2Adm]] // Edges blocked by UUIDs that haven't arrived yet

        // Try to emit an edge. If either end of the edge hasn't arrived, add it to the blocked map
        def emitEdge(edge: EdgeAdm2Adm): List[Either[EdgeAdm2Adm, ADM]] = {
          if (!seenNodes.contains(edge.src)) {
            blockedEdges(edge.src) = edge :: blockedEdges.getOrElse(edge.src, Nil)
            Nil
          } else if (!seenNodes.contains(edge.tgt)) {
            blockedEdges(edge.tgt) = edge :: blockedEdges.getOrElse(edge.tgt, Nil)
            Nil
          } else {
            List(Left(edge))
          }
        }

        {
          case Left(edge) if seenEdges.contains(edge) => Nil      // Duplicate edge - ignore
          case Right(adm) if seenNodes.contains(adm.uuid) => Nil  // Duplicate ADM - ignore
          case Left(edge) =>
            seenEdges.add(edge)
            emitEdge(edge)                                   // Edge - check if we can emit
          case Right(adm) =>                                 // New ADM - record and emit
            seenNodes.add(adm.uuid)
            val emit = blockedEdges.remove(adm.uuid).getOrElse(Nil).flatMap(emitEdge)
            Right(adm) :: emit
        }
      })
}
