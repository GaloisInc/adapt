package com.galois.adapt.adm

import java.util.UUID

import akka.actor.ActorRef
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.galois.adapt.adm.EntityResolution.Timed
import com.galois.adapt.cdm18._

import scala.collection.immutable.SortedMap
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

// This object contains all of the logic for resolving individual CDM types into their corresponding ADM ones.
object ERStreamComponents {

  import ERRules._

  type CDM = CDM18

  object EventResolution {

    // In order to consider merging two events, we need them to have the same key, which consists of the subject UUID,
    // predicate object UUID, and predicate object 2 UUID.
    private type EventKey = (Option[UUID], Option[UUID], Option[UUID])
    private def key(e: Event): EventKey = (e.subjectUuid, e.predicateObject, e.predicateObject2)

    // This is the state we maintain per 'EventKey'
    private case class EventMergeState(
      wipAdmEvent: AdmEvent,                       // ADM event built up so far
      lastCdmUuid: CdmUUID,                        // Latest CDM UUID that went into the AdmEvent
      remaps: List[UuidRemapper.PutCdm2Cdm],       // The remaps that need to be sequence before the AdmEvent
      dependent: Stream[Either[Edge[_, _], ADM]],  // The path and edges that should be created with the AdmEvent
      merged: Int                                  // The number of CDM events that have been folded in so far
    )

    def apply(uuidRemapper: ActorRef, expireInNanos: Long)(implicit timeout: Timeout, ec: ExecutionContext): Flow[Timed[Event], Future[Either[Edge[_, _], ADM]], _] = Flow[Timed[Event]]

      .statefulMapConcat { () =>

        // INVARIANT: the keys in the fridge should match the keys of the active chains
        val activeChains: mutable.Map[EventKey, EventMergeState] = mutable.Map.empty
        var expiryTimes: Fridge[EventKey] = Fridge.empty

        {
          case Timed(currentTime, e) =>

//            assert(activeChains.keySet == expiryTimes.keySet, "keysets are different")

            val eKey = key(e)

//            println("The current time is " + currentTime.toString)

//            println("Map size: " + activeChains.size.toString)

            // Write in the new information
            val toReturnChain = activeChains.get(eKey) match {

              // We already have an active chain for this EventKey
              case Some(EventMergeState(wipAdmEvent, lastCdmUuid, remaps, dependent, merged)) =>

                collapseEvents(e, wipAdmEvent, lastCdmUuid, merged) match {

                  // Merged event in => update the new WIP
                  case Left((remap, newLastCdmUuid, newWipEvent)) =>

                    activeChains(eKey) = EventMergeState(
                      newWipEvent,
                      newLastCdmUuid,
                      remap :: remaps,
                      dependent,
                      merged + 1
                    )
                    expiryTimes.updateExpiryTime(eKey, currentTime + expireInNanos)

                    Stream.empty

                  // Didn't merge event in
                  case Right(_) =>

                    val (newWipAdmEvent, subject, predicateObject, predicateObject2, path1, path2, path3, path4) = resolveEventAndPaths(e)

                    val final_remap = UuidRemapper.PutCdm2Adm(lastCdmUuid, wipAdmEvent.uuid)
                    val rs = Future.sequence((final_remap :: remaps).map(r => uuidRemapper ? r))
                    val toReturn = Stream.concat(                            // Emit the old WIP
                      Some(Right(wipAdmEvent)),
                      dependent
                    ).map(elem => rs.map(_ => elem))

                    activeChains(eKey) = EventMergeState(
                      newWipAdmEvent,
                      CdmUUID(e.getUuid),
                      List(),
                      extractPathsAndEdges(path1) ++
                      extractPathsAndEdges(path2) ++
                      extractPathsAndEdges(path3) ++
                      extractPathsAndEdges(path4) ++
                      Stream.concat(
                        subject.map(Left(_)),
                        predicateObject.map(Left(_)),
                        predicateObject2.map(Left(_))
                      ),
                      1
                    )
                    expiryTimes.updateExpiryTime(eKey, currentTime + expireInNanos)

                    toReturn
                }

              // We don't already have an active chain for this EventKey
              case None =>

                // Create a new WIP from e
                val (newWipAdmEvent, subject, predicateObject, predicateObject2, path1, path2, path3, path4) = resolveEventAndPaths(e)

                activeChains(eKey) = EventMergeState(
                  newWipAdmEvent,
                  CdmUUID(e.getUuid),
                  List(),
                  extractPathsAndEdges(path1) ++
                  extractPathsAndEdges(path2) ++
                  extractPathsAndEdges(path3) ++
                  extractPathsAndEdges(path4) ++
                  Stream.concat(
                    subject.map(Left(_)),
                    predicateObject.map(Left(_)),
                    predicateObject2.map(Left(_))
                  ),
                  1
                )
                expiryTimes.updateExpiryTime(eKey, currentTime + expireInNanos)

                Stream.empty
            }

//            assert(activeChains.keySet == expiryTimes.keySet, "keysets are different 2")

            // Expire old events
            var toReturnExpired = Stream[Future[Either[Edge[_, _], ADM]]]()
            while (expiryTimes.peekFirstToExpire.exists { case (_,t) => t <= currentTime }) {
//              assert(activeChains.keySet == expiryTimes.keySet, "keysets are different 3")
              val (keysToExpire, _) = expiryTimes.popFirstToExpire().get
//              println("keys to expire: " + keysToExpire.toString)

              for (keyToExpire <- keysToExpire) {
//                println("expiring: " + keyToExpire.toString)
//                println("activeChains: " + activeChains.toString)
                val EventMergeState(wipAdmEvent, lastCdmUuid, remaps, dependent, _) = activeChains.remove(keyToExpire).get
                val final_remap = UuidRemapper.PutCdm2Adm(lastCdmUuid, wipAdmEvent.uuid)
                val rs = Future.sequence((final_remap :: remaps).map(r => uuidRemapper ? r))
                val toReturn = Stream.concat(
                  Some(Right(wipAdmEvent)),
                  dependent
                ).map(elem => rs.map(_ => elem))

                toReturnExpired = toReturn ++ toReturnExpired
              }

//              println("Expired ADM (uuid: " + wipAdmEvent.uuid.toString + ", originalUuids: " + wipAdmEvent.originalCdmUuids.toString + ")")
            }

            toReturnChain ++ toReturnExpired
        }
      }

  }
/*
    // Group events that have the same subject and predicate objects
    .groupBy(Int.MaxValue, e => (e.subjectUuid, e.predicateObject, e.predicateObject2))

    // Identify sequences of events
    .statefulMapConcat( () => {
      var wipAdmEventOpt: Option[AdmEvent] = None
      var remaps: List[UuidRemapper.PutCdm2Adm] = Nil
      var dependent: Stream[Either[Edge[_, _], ADM]] = Stream.empty

      // NOTE: we keep track here of how many events have contributed to the 'wipAdmEventOpt'. This allows us to cap the
      //       number of CDM events that get ER'd into an ADM event. Not doing this slows everything to a crawl.
      var merged: Int = 0

      (e: Event) => {

        wipAdmEventOpt match {
          case Some(wipAdmEvent) => collapseEvents(e, wipAdmEvent, merged) match {

            // Merged event in
            case Left((remap, newWipEvent)) =>
              // Update the new WIP
              wipAdmEventOpt = Some(newWipEvent)
              merged += 1
              remaps = remap :: remaps
              Stream.empty

            // Didn't merge event in
            case Right((e, wipAdmEvent)) =>

              // Create a new WIP from e

              val (newWipAdmEvent, remap, subject, predicateObject, predicateObject2, path1, path2, path3, path4) = resolveEventAndPaths(e)
              merged = 1

              val r1 = Future.sequence(remaps.map(r => uuidRemapper ? r))
              val toReturn = Stream.concat(                            // Emit the old WIP
                Some(Right(wipAdmEvent)),
                dependent
              ).map(elem => r1.map(_ => elem))

              wipAdmEventOpt = Some(newWipAdmEvent)
              remaps = List(remap)
              dependent =
                extractPathsAndEdges(path1) ++
                extractPathsAndEdges(path2) ++
                extractPathsAndEdges(path3) ++
                extractPathsAndEdges(path4) ++
                Stream.concat(
                  subject.map(Left(_)),
                  predicateObject.map(Left(_)),
                  predicateObject2.map(Left(_))
                )

              toReturn

          }
          case None =>
            // Create a new WIP from e
            val (newWipAdmEvent, remap, subject, predicateObject, predicateObject2, path1, path2, path3, path4) = resolveEventAndPaths(e)
            merged = 1

            wipAdmEventOpt = Some(newWipAdmEvent)
            remaps = List(remap)
            dependent =
              extractPathsAndEdges(path1) ++
              extractPathsAndEdges(path2) ++
              extractPathsAndEdges(path3) ++
              extractPathsAndEdges(path4) ++
              Stream.concat(
                subject.map(Left(_)),
                predicateObject.map(Left(_)),
                predicateObject2.map(Left(_))
              )

            Stream.empty
        }
      }
    })

    // Un-group events
    .mergeSubstreams */

  def extractPathsAndEdges(path: Option[(Edge[_, _], AdmPathNode)])(implicit timeout: Timeout, ec: ExecutionContext): Stream[Either[Edge[_, _], ADM]] = path match {
    case Some((edge, path)) => Stream(Right(path), Left(edge))
    case None => Stream()
  }

  def subjectResolution(uuidRemapper: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext): Flow[Subject, Future[Either[Edge[_, _], ADM]], _] = Flow[Subject]
    .mapConcat[Future[Either[Edge[_, _], ADM]]] { s =>
      ERRules.resolveSubject(s) match {

        // Don't merge the Subject (it isn't a UNIT)
        case Left((irSubject, remap, localPrincipal, parentSubjectOpt, path)) =>
          Stream.concat(
            Some(Right(irSubject)),
            extractPathsAndEdges(path),
            Some(Left(localPrincipal)),
            parentSubjectOpt.map(Left(_))
          ).map(elem => (uuidRemapper ? remap).map(_ => elem))

        // Merge the Subject (it was a UNIT)
        case Right((path, remap)) =>
          Stream.concat(
            extractPathsAndEdges(path)
          ).map(elem => (uuidRemapper ? remap).map(_ => elem))
      }
    }

  def otherResolution(uuidRemapper: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext): Flow[CDM, Future[Either[Edge[_, _], ADM]], _] = {

    Flow[CDM].mapConcat[Future[Either[Edge[_, _], ADM]]] {
      case ptn: ProvenanceTagNode =>

        val (irPtn, remap, flowObjectEdge, subjectEdge, prevTagIdEdge, tagIdsEgdes) = ERRules.resolveProvenanceTagNode(ptn)

        Stream.concat(
          Some(Right(irPtn)),
          flowObjectEdge.map(Left(_)),
          Some(Left(subjectEdge)),
          prevTagIdEdge.map(Left(_)),
          tagIdsEgdes.map(Left(_))
        ).map(elem => (uuidRemapper ? remap).map(_ => elem))


      case p: Principal =>

        val (irP, remap) = resolvePrincipal(p)

        Stream(Right(irP)).map(elem => (uuidRemapper ? remap).map(_ => elem))


      case s: SrcSinkObject =>

        val (sP, remap) = resolveSrcSink(s)

        Stream(Right(sP)).map(elem => (uuidRemapper ? remap).map(_ => elem))


      case n: NetFlowObject =>

        val (nP, remap) = resolveNetflow(n)

        Stream(Right(nP)).map(elem => (uuidRemapper ? remap).map(_ => elem))


      case fo: FileObject =>

        val (irFileObject, remap, localPrincipalOpt, path) = resolveFileObject(fo)

        Stream.concat(
          Some(Right(irFileObject)),
          extractPathsAndEdges(path),
          localPrincipalOpt.map(Left(_))
        ).map(elem => (uuidRemapper ? remap).map(_ => elem))


      case rk: RegistryKeyObject =>
        val (irFileObject, remap, path) = resolveRegistryKeyObject(rk)

        Stream.concat(
          Some(Right(irFileObject)),
          extractPathsAndEdges(Some(path))
        ).map(elem => (uuidRemapper ? remap).map(_ => elem))


      case _ => Nil
    }
  }
}
