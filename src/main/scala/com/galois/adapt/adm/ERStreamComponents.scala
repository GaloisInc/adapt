package com.galois.adapt.adm

import akka.actor.ActorRef
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.galois.adapt.cdm17._

import scala.concurrent.{ExecutionContext, Future}

// This object contains all of the logic for resolving individual CDM types into their corresponding ADM ones.
object ERStreamComponents {

  import ERRules._

  type CDM = CDM17

  def eventResolution(uuidRemapper: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext): Flow[Event, Future[Either[Edge[_, _], ADM]], _] = Flow[Event]

    // Group events that have the same subject and predicate objects
    .groupBy(Int.MaxValue, e => (e.subjectUuid, e.predicateObject, e.predicateObject2))

    // Identify sequences of events
    .statefulMapConcat( () => {
      var wipAdmEventOpt: Option[AdmEvent] = None
      var remaps: List[UuidRemapper.PutCdm2Adm] = Nil
      var dependent: Stream[Either[Edge[_, _], ADM]] = Stream.empty

      (e: Event) => {

        wipAdmEventOpt match {
          case Some(wipAdmEvent) => collapseEvents(e, wipAdmEvent) match {

            // Merged event in
            case Left((remap, newWipEvent)) =>
              // Update the new WIP
              wipAdmEventOpt = Some(newWipEvent)
              remaps = remap :: remaps
              Stream.empty

            // Didn't merge event in
            case Right((e, wipAdmEvent)) =>

              // Create a new WIP from e

              val (newWipAdmEvent, remap, subject, predicateObject, predicateObject2, path1, path2, path3) = resolveEventAndPaths(e)

              val toReturn = Stream.concat(                            // Emit the old WIP
                Some(Right(wipAdmEvent)),
                dependent
              ).map(elem => Future.sequence(remaps.map(r => uuidRemapper ? r)).map(_ => elem))

              wipAdmEventOpt = Some(newWipAdmEvent)
              remaps = List(remap)
              dependent = extractPathsAndEdges(path1) ++ extractPathsAndEdges(path2) ++ extractPathsAndEdges(path3) ++ Stream.concat(
                  Some(Left(subject)),
                  predicateObject.map(Left(_)),
                  predicateObject2.map(Left(_))
                )

              toReturn

          }
          case None =>
            // Create a new WIP from e
            val (newWipAdmEvent, remap, subject, predicateObject, predicateObject2, path1, path2, path3) = resolveEventAndPaths(e)

            wipAdmEventOpt = Some(newWipAdmEvent)
            remaps = List(remap)
            dependent = extractPathsAndEdges(path1) ++ extractPathsAndEdges(path2) ++ extractPathsAndEdges(path3) ++ Stream.concat(
              Some(Left(subject)),
              predicateObject.map(Left(_)),
              predicateObject2.map(Left(_))
            )

            Stream.empty
        }
      }
    })

    // Un-group events
    .mergeSubstreams

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
 

// 
object EventDFA {


}
