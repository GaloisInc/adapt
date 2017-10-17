package com.galois.adapt.ir

import java.util.UUID

import scala.concurrent.duration._
import akka.stream.scaladsl.{Flow, Source}
import akka.actor.ActorRef
import com.galois.adapt.cdm17._
import scala.collection.mutable

// TODO: There should be no calls to randomUUID here, only 'Get(<cdmUUID>, true)' requests to the
// RenameActor

/* This object contains all of the logic for resolving individual CDM types into their corresponding
 * IR ones.
 */
object ERStreamComponents {

  type CDM = CDM17

  /* Transform CDM events into ER events. This accumulates events and tries to group them together
   * when possible.
   *
   * The following patterns are supported:
   *   
   *    - (WRITE | LSEEK)*         is transformed into    WRITE
   *    - (RECVFROM | RECVMSG)*    is transformed into    RECVMSG
   *    - SENDMSG*                 is transformed into    SENDMSG
   *
   * Other EVENT_OPEN and EVENT_CLOSE are simply discarded.
   *
   * TODO: fill earliest/latest timestamps
   */
  def eventResolution(tickTimeout: Long): Flow[Event, IrEvent, _] = Flow[Event]
    
    // Group events that have the same subject and predicate objects
    .groupBy(Int.MaxValue, e => (e.subjectUuid, e.predicateObject, e.predicateObject2))
    
    // Merge in a stream of 'Tick' (if a substream receives two consequtive ticks without any events
    // in between, it releases the events it was holding on to
    .merge(Source.tick(tickTimeout.seconds, tickTimeout.seconds, Tick))
    
    // Identify sequences of events
    .statefulMapConcat( () => {
      var ticked: Boolean = false
      var wipEvent: Option[IrEvent] = None

      // Reset to the initial state
      def dumpState(): Option[IrEvent] = {
        val toReturn = wipEvent
        
        ticked = false
        wipEvent = None
        toReturn
      }
      
      // Set wip event
      def setWip(wip: IrEvent) = {
        wipEvent = Some(wip)
      }

      // Handler
      {
        // Release all state
        case Tick if ticked => dumpState().toList

        // Set the tick counter
        case Tick => {
          ticked = true
          Nil
        }

        // Try to follow the patterns
        case e: Event if e.eventType == EVENT_OPEN || e.eventType == EVENT_CLOSE => dumpState().toList
        case e: Event => {
          ticked = false
          wipEvent match {
            case None => setWip(resolveEvent(e)); Nil
            case Some(wip) => 
              (e.eventType, wip.eventType) match {
                // merging WRITE and LSEEK
                case (EVENT_WRITE, EVENT_WRITE) => setWip(resolveEvent(e)); Nil
                case (EVENT_WRITE, EVENT_LSEEK) => setWip(resolveEvent(e)); Nil
                case (EVENT_LSEEK, EVENT_WRITE) => setWip(wip);             Nil
                case (EVENT_LSEEK, EVENT_LSEEK) => setWip(resolveEvent(e)); Nil

                // merging RECVFROM and RECVMSG
                case (EVENT_RECVMSG,  EVENT_RECVMSG)  => setWip(resolveEvent(e)); Nil
                case (EVENT_RECVMSG,  EVENT_RECVFROM) => setWip(resolveEvent(e)); Nil
                case (EVENT_RECVFROM, EVENT_RECVMSG)  => setWip(wip);             Nil
                case (EVENT_RECVFROM, EVENT_RECVFROM) => setWip(resolveEvent(e)); Nil

                // merge SENDMSG
                case (EVENT_SENDMSG,  EVENT_SENDMSG)  => setWip(resolveEvent(e)); Nil

                // everything else causes the previous event to be flushed out and replaced with the
                // new event received
                case _ => {
                  val toReturn = dumpState()
                  setWip(resolveEvent(e))
                  toReturn.toList
                }
            }
          }
        }
      }

    })

    // Un-group events
    .mergeSubstreams 

  // Make a resolved event from an event by throwing away the fields that arent used.
  def resolveEvent(e: Event): IrEvent = IrEvent(
    UUID.randomUUID(),
    Seq(e.uuid),
    e.eventType,
    e.subjectUuid,
    e.timestampNanos,
    e.timestampNanos,
    e.properties.flatMap(_.get("exec")),
    e.predicateObject,
    e.predicateObject2
  )


  /* Units get merged into their closest subject ancestor. This requires accumulating state until we
   * see an event close for that subject.
   *
   * TODO: drop state once we see a CLOSE(?) event on the subject
   * TODO: collect fork events and look for an 'exec'
   */
  def subjectResolution(tickTimeout: Long, erRenameActor: ActorRef): Flow[CDM, IrSubject, _] = Flow[CDM]
    
    // Merge units with their closest subject ancestor
    .statefulMapConcat[Either[IrSubject,Event]]( () => {

      // Given the (CDM) UUID of a subject, find its closest non-unit ancestor subject
      var ancestorSubject: mutable.Map[UUID,UUID] = mutable.Map[UUID,UUID]();

      {
        // Try to merge UNIT subjects with their greatest parent ancestor
        case Subject(uuid, SUBJECT_UNIT, _, _, _, Some(parent), _, _, _, _, _, _, _, _)
        if ancestorSubject contains parent => { 
          val ancestor = ancestorSubject(parent)

          ancestorSubject(uuid) = ancestor 
          erRenameActor ! Put(uuid, ancestor)

          Nil
        }

        // Other subjects pass through undisturbed - we just throw away fields we don't care about
        case Subject(uuid, ty, _, principal, timestamp, parent, _, _, _, cmd, _, _, _, _) => {

          val newUuid = UUID.randomUUID()
          erRenameActor ! Put(uuid, newUuid)

          List(Left(IrSubject(newUuid, Seq(uuid), ty, principal, timestamp, cmd, parent)))
        }

        // Event that will may be interesting
        case e: Event if e.eventType == EVENT_FORK && !e.predicateObject.isEmpty => List(Right(e))

        case _ => Nil
      }
    })

    // Attach event from FORK onto the subject
    .groupBy(Int.MaxValue, {
      case Left(subj: IrSubject) => subj.originalEntities(0)
      case Right(evnt: Event) => evnt.predicateObject.get
    })
    .merge(Source.tick(tickTimeout.seconds, tickTimeout.seconds, Tick))
    .statefulMapConcat[IrSubject]( () => {
      var subject: Option[IrSubject] = None
      var execOpt: Option[String] = None
      var subjectSent: Boolean = false
      var tickReceived: Boolean = false

      // Tries to make a subject (if it succeeds, the subject is returned and subjectsent/subject are updated)
      def makeSubject(): Option[IrSubject] = {
        (subject, execOpt) match {
          case (Some(s), _) if !s.cmdLine.isEmpty => {
            subjectSent = true
            subject
          }
          case (Some(s), Some(e)) => {
            subject = Some(s.copy(cmdLine = Some(e)))
            subjectSent = true
            subject
          }
          case _ => None
        }
      }

      {
        // Don't do anything if we have already sent along the subject
        case _ if subjectSent => Nil

        // When receiving a Tick, only send off the file if there is a partial subject and we have
        // already received one tick before
        case Tick => {
          val toReturn = if (!subject.isEmpty && tickReceived) {
            subjectSent = true
            subject.toList
          } else { 
            Nil
          }
          tickReceived = true
          toReturn
        }

        // Receiving the subject
        case s: IrSubject => {
          subject = Some(s)
          makeSubject().toList
        }

        // Receiving a relevant event
        case e: Event => {
          execOpt = e.properties.flatMap(_.get("exec"))
          makeSubject().toList
        }

        // Anything else
        case _ => Nil 
      }
    })
    .mergeSubstreams



  /* Transform a stream of CDM into resolved file objects or other CDM
   *
   * We include _all_ CDM because sometimes other types will tell us more about the files. In
   * particular:
   *   
   *    - if an event has a 'predicateObjectPath', attach that to the path of the file object
   *
   * Most importantly, dedup files by their path/principal
   */
  def fileObjectResolution(tickTimeout: Long, erRenameActor: ActorRef): Flow[CDM, IrFileObject, _] = Flow[CDM]
 
    // Group subjects with events that are possibly interesting to the subject. As soon as the file
    // object has a path attached to it, we pass the file object downstream. We wait at most one
    // 'Tick' to find the path before giving up.
    .collect({
      case file: FileObject => file
      case event: Event if !event.predicateObject.isEmpty => event
    })
    .groupBy(Int.MaxValue, {
      case file: FileObject => file.uuid
      case event: Event => event.predicateObject.get
    })
    .merge(Source.tick(tickTimeout.seconds, tickTimeout.seconds, Tick))
    .statefulMapConcat( () => {
      var file: Option[IrFileObject] = None
      var pathOpt: Option[String] = None
      var fileSent: Boolean = false
      var tickReceived: Boolean = false

      // Tries to make a file (if it succeeds, the file is returned and filesent/file are updated)
      def makeFile(): Option[IrFileObject] = {
        (file, pathOpt) match {
          case (Some(f), Some(p)) => {
            file = Some(f.copy(path = Some(p)))
            fileSent = true
            file
          }
          case _ => None
        }
      }

      {
        // Don't do anything if we have already sent along the file
        case _ if fileSent => Nil

        // When receiving a Tick, only send off the file if there is a partial file and we have
        // already received one tick before
        case Tick => {
          val toReturn = if (!file.isEmpty && tickReceived) {
            fileSent = true
            file.toList
          } else { 
            Nil
          }
          tickReceived = true
          toReturn
        }

        // Receiving the file object
        case FileObject(uuid, _, ty, _, principal, _, path, _) => {
          file = Some(IrFileObject(UUID.randomUUID(), Seq(uuid), path, ty, principal))
          makeFile().toList
        }

        // Receiving a relevant event
        case e: Event if !e.properties.isEmpty => {
          pathOpt = e.properties.flatMap(_.get("predicateObjectPath"))
          makeFile().toList
        }

        // Anything else
        case _ => Nil 
      }
    })
    .mergeSubstreams
 
    // Group by file name and keep only the first of each file with that path and principal
    .groupBy(Int.MaxValue, file => (file.path, file.localPrincipal))
    .statefulMapConcat( () => {
      
      // Invariant: if `file` is not `None`, then it must have a path
      var file: Option[IrFileObject] = None

      {
        // We have already passed along the canonical file object for this path
        case f: IrFileObject if !file.isEmpty => {
          erRenameActor ! (f.uuid, file.get.uuid)
          Nil
        }
        
        // This is the first path we have seen
        case f: IrFileObject if !f.path.isEmpty => {
          file = Some(f)
          List(f)
        }

        // We are in the substream of file objects still lacking paths
        case f => List(f)
      }
    })
    .mergeSubstreams


  /* Deduplicate netflows according to local/remote address/port.
   */
  def netflowResolution(erRenameActor: ActorRef): Flow[NetFlowObject, IrNetFlowObject, _] = Flow[NetFlowObject]
    .groupBy(Int.MaxValue, n => (n.localAddress, n.localPort, n.remoteAddress, n.remotePort))
    .statefulMapConcat( () => {
      // UUID of canonical netflow for the substream
      var netflowUuid: Option[UUID] = None

      {
        // If we have already got the canonical netflow, hold onto it
        case netflow if !netflowUuid.isEmpty => {
          erRenameActor ! Put(netflow.uuid, netflowUuid)
          Nil
        }

        // If we don't yet have a canonical netflow
        case NetFlowObject(uuid, _, lclAddr, lclPort, rteAddr, rtePort, _, _) => {
          val newUuid = UUID.randomUUID()
          netflowUuid = Some(newUuid)
          List(IrNetFlowObject(newUuid, Seq(uuid), lclAddr, lclPort, rteAddr, rtePort))
        }
      }
    })
    .mergeSubstreams


  /* Unfortunately, we have no way of merging source/sink objects, so ER for these is just mapping
   * them into a new case class
   *
   * TODO: consider merging these with 'IrFileObject' with empty paths
   */
  def srcSinkResolution(erRenameActor: ActorRef): Flow[SrcSinkObject, IrSrcSinkObject, _] = Flow[SrcSinkObject]
    .map(srcSink => {
      val resolvedSrcSink = resolveSrcSink(srcSink)
      erRenameActor ! Put(srcSink.uuid, resolvedSrcSink.uuid)
      resolvedSrcSink
    })

  // Make a resolved src-sink from a src-sink by throwing away the fields that arent used.
  def resolveSrcSink(s: SrcSinkObject): IrSrcSinkObject = IrSrcSinkObject(
    UUID.randomUUID(),
    Seq(s.uuid),
    s.srcSinkType
  )


  /* Principals don't need any ER. There already aren't many of them anyways
   */
  def principalResolution(erRenameActor: ActorRef): Flow[Principal, IrPrincipal, _] = Flow[Principal]
    .map(principal => {
      val resolvedPrincipal = resolvePrincipal(principal) 
      erRenameActor ! Put(principal.uuid, resolvedPrincipal.uuid)
      resolvedPrincipal
    })

  // Make a resolved principal from a principal by throwing away the fields that arent used.
  def resolvePrincipal(p: Principal): IrPrincipal = IrPrincipal(
    UUID.randomUUID(),
    Seq(p.uuid),
    p.userId,
    p.groupIds,
    p.principalType,
    p.username
  )


  /* Provenance tag nodes don't need any ER. There already aren't many of them anyways
   */
  def provenanceTagNodeResolution(erRenameActor: ActorRef): Flow[ProvenanceTagNode,IrProvenanceTagNode, _] = Flow[ProvenanceTagNode]
    .map(provenance => {
      val resolvedProvenance = resolveProvenanceTagNode(provenance) 
      erRenameActor ! Put(provenance.tagIdUuid, resolvedProvenance.uuid)
      resolvedProvenance 
    })
  
  // Make a resolved provenance tag node from a provenance tag node by throwing away the fields that arent used.
  def resolveProvenanceTagNode(p: ProvenanceTagNode): IrProvenanceTagNode = IrProvenanceTagNode(
    UUID.randomUUID(),
    Seq(p.tagIdUuid),
    p.subjectUuid,
    p.flowObject,
    p.programPoint,
    p.prevTagId, 
    p.tagIds.getOrElse(Seq())
  )
 
}


// Object passed around as (time-based) indicator to flush state
object Tick


// 
object EventDFA {


}
