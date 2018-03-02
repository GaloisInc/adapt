package com.galois.adapt.adm

import java.util.UUID

import akka.actor.ActorRef
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.galois.adapt.adm.EntityResolution.Timed
import com.galois.adapt.cdm18._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

// This object contains all of the logic for resolving individual CDM types into their corresponding ADM ones.
object ERStreamComponents {

  import ERRules._

  type CDM = CDM18

  // All flows in this object have this type
  type TimedCdmToFutureAdm = Flow[(String,Timed[CDM]), Future[Either[Edge[_, _], ADM]], _]


  def extractPathsAndEdges(pathEdge: Option[(Edge[_, _], AdmPathNode)])
                          (implicit timeout: Timeout, ec: ExecutionContext): Stream[Either[Edge[_, _], ADM]] =
    pathEdge match {
      case Some((edge, path)) => Stream(Right(path), Left(edge))
      case None => Stream()
    }


  object EventResolution {

    // In order to consider merging two events, we need them to have the same key, which consists of the subject UUID,
    // predicate object UUID, and predicate object 2 UUID.
    private type EventKey = (String, Option[UUID], Option[UUID], Option[UUID])
    private def key(e: Event, p: String): EventKey = (p, e.subjectUuid, e.predicateObject, e.predicateObject2)

    // This is the state we maintain per 'EventKey'
    private case class EventMergeState(
      wipAdmEvent: AdmEvent,                       // ADM event built up so far
      lastCdmUuid: CdmUUID,                        // Latest CDM UUID that went into the AdmEvent
      remaps: List[UuidRemapper.PutCdm2Cdm],       // The remaps that need to be sequence before the AdmEvent
      dependent: Stream[Either[Edge[_, _], ADM]],  // The path and edges that should be created with the AdmEvent
      merged: Int                                  // The number of CDM events that have been folded in so far
    )

    def apply(uuidRemapper: ActorRef, expireInNanos: Long)
             (implicit timeout: Timeout, ec: ExecutionContext): TimedCdmToFutureAdm = Flow[(String,Timed[CDM])]

      .statefulMapConcat { () =>

        // INVARIANT: the keys in the fridge should match the keys of the active chains
        val activeChains: mutable.Map[EventKey, EventMergeState] = mutable.Map.empty
        val expiryTimes: Fridge[EventKey] = Fridge.empty

        // Expire old events based on the current time
        def expireOldChains(currentTime: Long): Stream[Future[Either[Edge[_, _], ADM]]] = {
          var toReturnExpired = Stream[Future[Either[Edge[_, _], ADM]]]()
          while (expiryTimes.peekFirstToExpire.exists { case (_,t) => t <= currentTime }) {
            val (keysToExpire, _) = expiryTimes.popFirstToExpire().get

            for (keyToExpire <- keysToExpire) {
              val EventMergeState(wipAdmEvent, lastCdmUuid, remaps, dependent, _) = activeChains.remove(keyToExpire).get
              val final_remap = UuidRemapper.PutCdm2Adm(lastCdmUuid, wipAdmEvent.uuid, Some(currentTime))
              val rs = Future.sequence((final_remap :: remaps).map(r => uuidRemapper ? r))
              val toReturn = Stream.concat(
                Some(Right(wipAdmEvent)),
                dependent
              ).map(elem => rs.map(_ => elem))

              toReturnExpired = toReturn ++ toReturnExpired
            }
          }
          toReturnExpired
        }

        {
          case (provider, Timed(currentTime, e: Event)) =>

            val eKey = key(e, provider)

            // Write in the new information
            val toReturnChain = activeChains.get(eKey) match {

              // We already have an active chain for this EventKey
              case Some(EventMergeState(wipAdmEvent, lastCdmUuid, remaps, dependent, merged)) =>

                collapseEvents(provider, e, wipAdmEvent, lastCdmUuid, merged) match {

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

                    val (newWipAdmEvent, subject, predicateObject, predicateObject2, path1, path2, path3, path4) = resolveEventAndPaths(provider, e)

                    val final_remap = UuidRemapper.PutCdm2Adm(lastCdmUuid, wipAdmEvent.uuid, Some(currentTime))
                    val rs = Future.sequence((final_remap :: remaps).map(r => uuidRemapper ? r))
                    val toReturn = Stream.concat(                            // Emit the old WIP
                      Some(Right(wipAdmEvent)),
                      dependent
                    ).map(elem => rs.map(_ => elem))

                    activeChains(eKey) = EventMergeState(
                      newWipAdmEvent,
                      CdmUUID(e.getUuid, provider),
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
                val (newWipAdmEvent, subject, predicateObject, predicateObject2, path1, path2, path3, path4) = resolveEventAndPaths(provider, e)

                activeChains(eKey) = EventMergeState(
                  newWipAdmEvent,
                  CdmUUID(e.getUuid, provider),
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

            toReturnChain ++ expireOldChains(currentTime)

          case (_, Timed(_, TimeMarker(t))) => expireOldChains(t)

          case _ => Stream.empty
        }
      }
  }


  object SubjectResolution {
    def apply(uuidRemapper: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext): TimedCdmToFutureAdm =
      Flow[(String,Timed[CDM])]
        .mapConcat[Future[Either[Edge[_, _], ADM]]] {

          // We are solely interested in subjects
          case (provider, Timed(t, s: Subject)) =>
            ERRules.resolveSubject(provider, s) match {

              // Don't merge the Subject (it isn't a UNIT)
              case Left((irSubject, remap, localPrincipal, parentSubjectOpt, path)) =>
                val remapTimed = remap.copy(time = Some(t))
                Stream.concat(
                  Some(Right(irSubject)),
                  extractPathsAndEdges(path),
                  Some(Left(localPrincipal)),
                  parentSubjectOpt.map(Left(_))
                ).map(elem => (uuidRemapper ? remapTimed).map(_ => elem))

              // Merge the Subject (it was a UNIT)
              case Right((path, remap)) =>
                val remapTimed = remap.copy(time = Some(t))
                Stream.concat(
                  extractPathsAndEdges(path)
                ).map(elem => (uuidRemapper ? remapTimed).map(_ => elem))
            }

          case _ => Stream.empty
        }
  }


  object OtherResolution {
    def apply(uuidRemapper: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext): TimedCdmToFutureAdm = {

      Flow[(String,Timed[CDM])].mapConcat[Future[Either[Edge[_, _], ADM]]] {
        case (provider, Timed(t, ptn: ProvenanceTagNode)) =>

          val (irPtn, remap, flowObjectEdge, subjectEdge, prevTagIdEdge, tagIdsEgdes) = ERRules.resolveProvenanceTagNode(provider, ptn)
          val remapTimed = remap.copy(time = Some(t))

          Stream.concat(
            Some(Right(irPtn)),
            flowObjectEdge.map(Left(_)),
            Some(Left(subjectEdge)),
            prevTagIdEdge.map(Left(_)),
            tagIdsEgdes.map(Left(_))
          ).map(elem => (uuidRemapper ? remapTimed).map(_ => elem))


        case (provider, Timed(t, p: Principal)) =>

          val (irP, remap) = resolvePrincipal(provider, p)
          val remapTimed = remap.copy(time = Some(t))

          Stream(Right(irP)).map(elem => (uuidRemapper ? remapTimed).map(_ => elem))


        case (provider, Timed(t, s: SrcSinkObject)) =>

          val (sP, remap) = resolveSrcSink(provider, s)
          val remapTimed = remap.copy(time = Some(t))

          Stream(Right(sP)).map(elem => (uuidRemapper ? remapTimed).map(_ => elem))


        case (provider, Timed(t, n: NetFlowObject)) =>

          val (nP, remap, (ra, raEdge), (la, laEdge), remotePort, localPort) = resolveNetflow(provider, n)
          val remapTimed = remap.copy(time = Some(t))

          Stream.concat(
            Stream(Right(nP)),
            Stream(Left(ra)),
            Stream(Right(raEdge)),
            Stream(Left(la)),
            Stream(Right(laEdge)),
            remotePort.toList.flatMap { case (rp,rpEdge) => Stream(Left(rp), Right(rpEdge)) },
            localPort.toList.flatMap { case (lp,lpEdge) => Stream(Left(lp), Right(lpEdge)) }
          ).map(elem => (uuidRemapper ? remapTimed).map(_ => elem))


        case (provider, Timed(t, fo: FileObject)) =>

          val (irFileObject, remap, localPrincipalOpt, path) = resolveFileObject(provider, fo)
          val remapTimed = remap.copy(time = Some(t))

          Stream.concat(
            Some(Right(irFileObject)),
            extractPathsAndEdges(path),
            localPrincipalOpt.map(Left(_))
          ).map(elem => (uuidRemapper ? remapTimed).map(_ => elem))


        case (provider, Timed(t, rk: RegistryKeyObject)) =>
          val (irFileObject, remap, path) = resolveRegistryKeyObject(provider, rk)
          val remapTimed = remap.copy(time = Some(t))

          Stream.concat(
            Some(Right(irFileObject)),
            extractPathsAndEdges(path)
          ).map(elem => (uuidRemapper ? remapTimed).map(_ => elem))

        case (provider, Timed(t, u: UnnamedPipeObject)) =>
          val (irFileObject, remap) = resolveUnnamedPipeObject(provider, u)
          val remapTimed = remap.copy(time = Some(t))

          Stream(Right(irFileObject)).map(elem => (uuidRemapper ? remapTimed).map(_ => elem))

        case (provider, Timed(t, m: MemoryObject)) =>
          val (irSrcSink, remap) = resolveMemoryObject(provider, m)
          val remapTimed = remap.copy(time = Some(t))

          Stream(Right(irSrcSink)).map(elem => (uuidRemapper ? remapTimed).map(_ => elem))

        case _ => Nil
      }
    }
  }
}
