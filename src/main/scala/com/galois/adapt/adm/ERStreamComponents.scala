package com.galois.adapt.adm

import java.util.UUID

import akka.stream.scaladsl.Flow
import com.galois.adapt.adm.EntityResolution.{CDM, ErFlow, Time, Timed}
import com.galois.adapt.adm.UuidRemapper.{AnAdm, AnEdge, UuidRemapperInfo}
import com.galois.adapt.cdm18._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

// This object contains all of the logic for resolving individual CDM types into their corresponding ADM ones.
object ERStreamComponents {

  import ERRules._

  // All flows in this object have this type
  type TimedCdmToFutureAdm = Flow[(String,Timed[CDM]), Timed[UuidRemapperInfo], _]


  def extractPathsAndEdges(pathEdge: Option[(Edge, AdmPathNode)]): List[UuidRemapperInfo] =
    pathEdge match {
      case Some((edge, path)) => List(AnAdm(path), AnEdge(edge))
      case None => List.empty
    }


  object EventResolution {

    // In order to consider merging two events, we need them to have the same key, which consists of the subject UUID,
    // predicate object UUID, and predicate object 2 UUID.
    type EventKey = (String, Option[UUID], Option[UUID], Option[UUID])
    private def key(e: Event, p: String): EventKey = (p, e.subjectUuid, e.predicateObject, e.predicateObject2)

    // This is the state we maintain per 'EventKey'
    case class EventMergeState(
      wipAdmEvent: AdmEvent,                // ADM event built up so far
      dependent: List[UuidRemapperInfo],  // The path and edges that should be created with the AdmEvent
      merged: Int                           // The number of CDM events that have been folded in so far
    )

    def apply(
      eventExpiryTime: Time, // how long to wait before expiring an event chain
      maxEventsMerged: Int,  // maximum number of events to put into an event chain

      activeChains: mutable.Map[EventKey, EventMergeState]
    ): ErFlow = Flow[(String,Timed[CDM])]

      .statefulMapConcat { () =>

        // INVARIANT: the keys in the fridge should match the keys of the active chains
        val expiryTimes: Fridge[EventKey] = Fridge.empty

        // Expire one single event key
        def expireKey(keyToExpire: EventKey, currentTime: Time, expireInto: ListBuffer[Timed[UuidRemapperInfo]]): Unit = {
          val EventMergeState(wipAdmEvent, dependent, _) = activeChains.remove(keyToExpire).get
          expireInto += Timed(currentTime, AnAdm(wipAdmEvent))
          expireInto ++= dependent.map(elem => Timed(currentTime, elem))
        }

        // Expire old events based on the current time
        def expireOldChains(currentTime: Time, expireInto: ListBuffer[Timed[UuidRemapperInfo]]): Unit = {

          // Expire based on nanosecond timestamps
          var keepExpiringNanos: Boolean = true
          while (keepExpiringNanos) {
            expiryTimes.popFirstNanosToExpireIf(_ <= currentTime.nanos) match {
              case None => keepExpiringNanos = false
              case Some((keysToExpire, _)) => keysToExpire.foreach(k => expireKey(k, currentTime, expireInto))
            }
          }

          // Expire based on node counts
          var keepExpiringCounts: Boolean = true
          while (keepExpiringCounts) {
            expiryTimes.popFirstCountToExpireIf(_ <= currentTime.count) match {
              case None => keepExpiringCounts = false
              case Some((keysToExpire, _)) => keysToExpire.foreach(k => expireKey(k, currentTime, expireInto))
            }
          }
        }

        {
          case (provider, Timed(currentTime, e: Event)) =>

            val toReturn = ListBuffer.empty[Timed[UuidRemapperInfo]]

            val eKey = key(e, provider)

            // Write in the new information
            activeChains.get(eKey) match {

              // We already have an active chain for this EventKey
              case Some(EventMergeState(wipAdmEvent, dependent, merged)) =>

                collapseEvents(provider, e, wipAdmEvent, merged, maxEventsMerged) match {

                  // Merged event in => update the new WIP
                  case Left(newWipEvent) =>

                    activeChains(eKey) = EventMergeState(
                      newWipEvent,
                      dependent,
                      merged + 1
                    )
                    expiryTimes.updateExpiryTime(eKey, currentTime.plus(eventExpiryTime))

                  // Didn't merge event in
                  case Right(_) =>

                    val (newWipAdmEvent, subject, predicateObject, predicateObject2, path1, path2, path3, path4) = resolveEventAndPaths(provider, e)

                    toReturn += Timed(currentTime, AnAdm(wipAdmEvent))
                    toReturn ++= dependent.map(elem => Timed(currentTime, elem))

                    activeChains(eKey) = EventMergeState(
                      newWipAdmEvent,
                      List.concat(
                        extractPathsAndEdges(path1),
                        extractPathsAndEdges(path2),
                        extractPathsAndEdges(path3),
                        extractPathsAndEdges(path4),
                        subject.map(AnEdge(_)),
                        predicateObject.map(AnEdge(_)),
                        predicateObject2.map(AnEdge(_))
                      ),
                      1
                    )
                    expiryTimes.updateExpiryTime(eKey, currentTime.plus(eventExpiryTime))
                }

              // We don't already have an active chain for this EventKey
              case None =>

                // Create a new WIP from e
                val (newWipAdmEvent, subject, predicateObject, predicateObject2, path1, path2, path3, path4) = resolveEventAndPaths(provider, e)

                activeChains(eKey) = EventMergeState(
                  newWipAdmEvent,
                  List.concat(
                    extractPathsAndEdges(path1),
                    extractPathsAndEdges(path2),
                    extractPathsAndEdges(path3),
                    extractPathsAndEdges(path4),
                    subject.map(AnEdge(_)),
                    predicateObject.map(AnEdge(_)),
                    predicateObject2.map(AnEdge(_))
                  ),
                  1
                )
                expiryTimes.updateExpiryTime(eKey, currentTime.plus(eventExpiryTime))
            }

            expireOldChains(currentTime, toReturn)

            toReturn.toList

          case (_, Timed(t, TimeMarker(_))) =>

            val toReturn = ListBuffer.empty[Timed[UuidRemapperInfo]]
            expireOldChains(t, toReturn)
            toReturn.toList

          case _ => List.empty
        }
      }
  }


  object SubjectResolution {
    def apply: TimedCdmToFutureAdm =
      Flow[(String,Timed[CDM])]
        .mapConcat[Timed[UuidRemapperInfo]] {

          // We are solely interested in subjects
          case (provider, Timed(t, s: Subject)) =>
            ERRules.resolveSubject(provider, s) match {

              // Don't merge the Subject (it isn't a UNIT)
              case Left((irSubject, localPrincipal, parentSubjectOpt, path)) =>

                List.concat(
                  Some(AnAdm(irSubject)),
                  extractPathsAndEdges(path),
                  Some(AnEdge(localPrincipal)),
                  parentSubjectOpt.map(AnEdge(_))
                ).map(elem => Timed(t, elem))

              // Merge the Subject (it was a UNIT)
              case Right((path, merge)) =>

                List.concat(
                  extractPathsAndEdges(path),
                  Some(merge)
                ).map(elem => Timed(t, elem))
            }

          case _ => List.empty
        }
  }


  object OtherResolution {
    def apply: TimedCdmToFutureAdm = {

      Flow[(String,Timed[CDM])].mapConcat[Timed[UuidRemapperInfo]] {
        case (provider, Timed(t, ptn: ProvenanceTagNode)) =>

          val (irPtn, flowObjEdge, subjEdge, prevTagEdge, tagIdsEdges) = ERRules.resolveProvenanceTagNode(provider, ptn)

          List.concat(
            Some(AnAdm(irPtn)),
            flowObjEdge.map(AnEdge(_)),
            Some(AnEdge(subjEdge)),
            prevTagEdge.map(AnEdge(_)),
            tagIdsEdges.map(AnEdge(_))
          ).map(elem => Timed(t, elem))


        case (provider, Timed(t, p: Principal)) =>

          val irP = resolvePrincipal(provider, p)

          List(AnAdm(irP)).map(elem => Timed(t, elem))


        case (provider, Timed(t, s: SrcSinkObject)) =>

          val sP = resolveSrcSink(provider, s)

          List(AnAdm(sP)).map(elem => Timed(t, elem))


        case (provider, Timed(t, n: NetFlowObject)) =>

          val (nP, (raEdge, ra), (laEdge, la), remotePort, localPort) = resolveNetflow(provider, n)

          List.concat(
            List(AnAdm(nP)),
            List(AnEdge(raEdge)),
            List(AnAdm(ra)),
            List(AnEdge(laEdge)),
            List(AnAdm(la)),
            remotePort.toList.flatMap { case (rpEdge, rp) => List(AnEdge(rpEdge), AnAdm(rp)) },
            localPort.toList.flatMap { case (lpEdge, lp) => List(AnEdge(lpEdge), AnAdm(lp)) }
          ).map(elem => Timed(t, elem))


        case (provider, Timed(t, fo: FileObject)) =>

          val (irFileObject, localPrincipalOpt, path) = resolveFileObject(provider, fo)

          List.concat(
            Some(AnAdm(irFileObject)),
            extractPathsAndEdges(path),
            localPrincipalOpt.map(AnEdge(_))
          ).map(elem => Timed(t, elem))


        case (provider, Timed(t, rk: RegistryKeyObject)) =>

          val (irFileObject, path) = resolveRegistryKeyObject(provider, rk)

          List.concat(
            Some(AnAdm(irFileObject)),
            extractPathsAndEdges(path)
          ).map(elem => Timed(t, elem))

        case (provider, Timed(t, u: UnnamedPipeObject)) =>

          val irFileObject = resolveUnnamedPipeObject(provider, u)

          List(AnAdm(irFileObject)).map(elem => Timed(t, elem))

        case (provider, Timed(t, m: MemoryObject)) =>

          val irSrcSink = resolveMemoryObject(provider, m)

          List(AnAdm(irSrcSink)).map(elem => Timed(t, elem))

        case (provider, Timed(t, h: Host)) =>

          val irHost = resolveHost(provider, h)

          List(AnAdm(irHost)).map(elem => Timed(t, elem))

        case _ => Nil
      }
    }
  }
}
