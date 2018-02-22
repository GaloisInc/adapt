package com.galois.adapt.adm

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntUnaryOperator

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Source}
import akka.util.Timeout
import com.galois.adapt.adm.ERStreamComponents._
import com.galois.adapt.adm.UuidRemapper.{ExpireEverything, GetCdm2Adm, ResultOfGetCdm2Adm}
import com.galois.adapt.cdm18._
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.Random


object EntityResolution {

  val config: Config = ConfigFactory.load()

  def apply(uuidRemapper: ActorRef, synthesizedSource: Source[ADM, _])(implicit system: ActorSystem): Flow[CDM, Either[EdgeAdm2Adm, ADM], NotUsed] = {

    implicit val ec: ExecutionContext = system.dispatcher
    implicit val timeout: Timeout = Timeout.durationToTimeout(config.getLong("adapt.adm.timeoutSeconds") seconds)
    val parallelism: Int = config.getInt("adapt.adm.parallelism")
    val delay: FiniteDuration = 5 seconds

    // TODO: this is a hack
    def endHack(cdm: CDM): CDM = {
      cdm match {
        case EndMarker(session, counts) =>

          println(s"Reached end of session with session number: $session: ")
          for ((k,v) <- counts) {
            println(s"  $k: $v")
          }

          println(s"Waiting for $delay seconds...")
          Thread.sleep(delay.toMillis)
          uuidRemapper ! ExpireEverything

        case _ => /* do nothing */
      }

      cdm
    }

    Flow[CDM]
      .concat(Source.fromIterator[CDM](() => Iterator(TimeMarker(Long.MaxValue), EndMarker(-1,Map()))))
      .map(endHack)
      .via(annotateTime)
      .via(erWithoutRemapsFlow(uuidRemapper))
      .via(remapEdgeUuids(uuidRemapper))
      .merge(synthesizedSource.map(adm => Future.successful(Right(adm))))
      .via(asyncDeduplicate(parallelism))
  }


  type ErFlow = Flow[Timed[CDM], Future[Either[Edge[_, _], ADM]], NotUsed]

  // Perform entity resolution on stream of CDMs to convert them into ADMs
  private def erWithoutRemapsFlow(uuidRemapper: ActorRef)(implicit t: Timeout, ec: ExecutionContext): ErFlow =
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

      val broadcast = b.add(Broadcast[Timed[CDM]](3))
      val merge = b.add(Merge[Future[Either[Edge[_, _], ADM]]](3))

      broadcast ~> EventResolution(uuidRemapper, (config.getInt("adapt.adm.eventexpirysecs") seconds).toNanos) ~> merge
      broadcast ~> SubjectResolution(uuidRemapper)                                                             ~> merge
      broadcast ~> OtherResolution(uuidRemapper)                                                               ~> merge

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

    val id = Random.nextLong()

    Flow[Future[Either[Edge[_, _], ADM]]].map(_.flatMap {
      case Left(edge) => remapEdge(edge).map(Left(_))
      case Right(node) => Future.successful(Right(node))
    })
  }


  type OrderAndDedupFlow = Flow[Future[Either[EdgeAdm2Adm, ADM]], Either[EdgeAdm2Adm, ADM], NotUsed]

//  var inAsyncBuffer: AtomicInteger = new AtomicInteger(0)
  val asyncTime = new ConcurrentHashMap[Long, Either[Long, Long]]()
  val blockedEdgesCount: AtomicInteger = new AtomicInteger(0)
  val blockingNodes: MutableSet[UUID] = MutableSet.empty[UUID]

  // This does several things:
  //
  //   - prevent nodes with the same UUID from being re-emitted
  //   - prevent duplicate edges from being re-emitted
  //   - prevent Edges from being emitted before both of their endpoints have been emitted
  //
  private def asyncDeduplicate(parallelism: Int)(implicit t: Timeout, ec: ExecutionContext): OrderAndDedupFlow =
    Flow[Future[Either[EdgeAdm2Adm, ADM]]]
      .map(x => { val id = Random.nextLong(); asyncTime.put(id, Left(System.currentTimeMillis)); x.map(e => id -> e) })
      .mapAsyncUnordered[(Long, Either[EdgeAdm2Adm, ADM])](parallelism)(identity)
      .map(x => { asyncTime.put(x._1, Right(System.currentTimeMillis - asyncTime.get(x._1).left.get)); x._2 })
      .statefulMapConcat[Either[EdgeAdm2Adm, ADM]](() => {

        val seenNodes = MutableSet.empty[UUID]                       // UUIDs of nodes seen (and emitted) so far
        val seenEdges = MutableSet.empty[EdgeAdm2Adm]                // edges seen (not necessarily emitted)
        val blockedEdges = MutableMap.empty[UUID, List[EdgeAdm2Adm]] // Edges blocked by UUIDs that haven't arrived yet

        // Try to emit an edge. If either end of the edge hasn't arrived, add it to the blocked map
        def emitEdge(edge: EdgeAdm2Adm): List[Either[EdgeAdm2Adm, ADM]] = {
          if (!seenNodes.contains(edge.src)) {
            blockedEdges(edge.src) = edge :: blockedEdges.getOrElse(edge.src, Nil)
            blockingNodes += edge.src
            Nil
          } else if (!seenNodes.contains(edge.tgt)) {
            blockedEdges(edge.tgt) = edge :: blockedEdges.getOrElse(edge.tgt, Nil)
            blockingNodes += edge.tgt
            Nil
          } else {
            blockedEdgesCount.decrementAndGet()
            List(Left(edge))
          }
        }

        {
          case Left(edge) if seenEdges.contains(edge) => Nil      // Duplicate edge - ignore
          case Right(adm) if seenNodes.contains(adm.uuid) => Nil  // Duplicate ADM - ignore
          case Left(edge) =>
            seenEdges.add(edge)
            blockedEdgesCount.incrementAndGet()
            emitEdge(edge)                                   // Edge - check if we can emit
          case Right(adm) =>                                 // New ADM - record and emit
            seenNodes.add(adm.uuid)
            blockingNodes -= adm.uuid
            val emit = blockedEdges.remove(adm.uuid).getOrElse(Nil).flatMap(emitEdge)
            Right(adm) :: emit
        }
      })


  // Map a CDM onto a possible timestamp
  def timestampOf: CDM => Option[Long] = {
    case s: Subject => Some(s.startTimestampNanos)
    case e: Event => Some(e.timestampNanos)
    case t: TimeMarker => Some(t.timestampNanos)
    case _ => None
  }

  case class Timed[+T](time: Long, unwrap: T) {
    def map[U](f: T => U): Timed[U] = Timed(time, f(unwrap))
  }

  var currentTime: Long = 0

  def annotateTime: Flow[CDM, Timed[CDM], _] = Flow[CDM].statefulMapConcat{ () =>
//    var currentTime: Long = 0
    val maxTimeJump: Long = (config.getInt("adapt.adm.maxtimejumpsecs") seconds).toNanos

    (cdm: CDM) => {
      for (time <- timestampOf(cdm); if time > currentTime) {
        cdm match {
          case _: TimeMarker if time > currentTime => currentTime = time
          case _ if time > currentTime && (time - currentTime < maxTimeJump || currentTime < 1400000000000000L) => currentTime = time
          case _ => { }
        }

      }
      List(Timed(currentTime, cdm))
    }
  }

}
