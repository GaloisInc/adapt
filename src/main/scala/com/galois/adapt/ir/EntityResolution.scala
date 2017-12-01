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
import com.galois.adapt.ir.UuidRemapper.GetCdm2Ir
import scala.reflect._

import scala.reflect.ClassTag

object EntityResolution {

  type CdmUUID = java.util.UUID
  type IrUUID = java.util.UUID
  

  def apply()(implicit system: ActorSystem): Flow[CDM, Either[Edge[IR, IR], IR], _] = {

    implicit val ec: ExecutionContext = system.dispatcher
    implicit val timout: Timeout = Timeout(30 seconds)
    val parallelism = 10000

    val uuidRemapper: ActorRef = system.actorOf(Props[UuidRemapper])
   
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
  private def remapEdgeUuids(uuidRemapper: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext): Flow[Future[Either[Edge[_, _], IR]], Future[Either[Edge[IR, IR], IR]], _] = {

    def remapEdge[From: ClassTag, To: ClassTag](edge: Edge[From, To]): Future[Edge[IR,IR]] = for {
      // Remap source
      newSrc <- if (classTag[From] != classTag[IR]) {
        (uuidRemapper ? GetCdm2Ir(edge.src)).mapTo[UUID]
      } else {
        Future.successful(edge.src)
      }

      // Remap destination
      newTgt <- if (classTag[To] != classTag[IR]) {
        (uuidRemapper ? GetCdm2Ir(edge.src)).mapTo[UUID]
      } else {
        Future.successful(edge.tgt)
      }
    } yield Edge[IR, IR](newSrc, edge.label, newTgt)

    Flow[Future[Either[Edge[_, _], IR]]].map(_.flatMap {
      case Left(edge) => remapEdge(edge).map(Left(_))
      case Right(node) => Future.successful(Right(node))
    })
  }

  private def awaitAndDeduplicate(parallelism: Int)(implicit timeout: Timeout, ec: ExecutionContext): Flow[Future[Either[Edge[IR, IR], IR]], Either[Edge[IR, IR], IR], _] =
    Flow[Future[Either[Edge[IR, IR], IR]]]
      .mapAsyncUnordered[Either[Edge[IR, IR], IR]](parallelism)(identity)
      .statefulMapConcat[Either[Edge[IR, IR], IR]](() => {
        val seen: collection.mutable.Set[UUID] = collection.mutable.Set.empty[UUID]

        {
          case Left(e) => List(Left(e))
          case Right(ir) if seen.contains(ir.uuid) => Nil
          case Right(ir) =>
            seen.add(ir.uuid)
            List(Right(ir))
        }
      })


  }
