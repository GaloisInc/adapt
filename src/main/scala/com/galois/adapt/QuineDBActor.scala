package com.galois.adapt

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import akka.util.Timeout
import com.galois.adapt.cdm17._
import com.rrwright.quine.language.EdgeDirections.{-->, <--}
import com.rrwright.quine.language._
import com.rrwright.quine.runtime.{EmptyPersistor, GraphService}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.pickling.PicklerUnpickler
import scala.pickling.shareNothing._
//import scala.pickling.static._        // Avoid run-time reflection


object CDM17Implicits {
  import scala.pickling.Defaults._

  implicit val a = PicklerUnpickler.generate[Option[Map[String, String]]]
  implicit val b = PicklerUnpickler.generate[Option[String]]
  implicit val c = PicklerUnpickler.generate[AbstractObject]

  implicit val d = PicklerUnpickler.generate[PrincipalType]
  implicit val e = PicklerUnpickler.generate[List[String]]  // canNOT also have this in scope: scala.pickling.Defaults.stringPickler

  implicit val f = PicklerUnpickler.generate[Option[Int]]
  implicit val g = PicklerUnpickler.generate[Option[Long]]
  implicit val h = PicklerUnpickler.generate[FileObjectType]
  implicit val i = PicklerUnpickler.generate[CryptographicHash]
  implicit val j = PicklerUnpickler.generate[Option[Seq[CryptographicHash]]]

  implicit val k = PicklerUnpickler.generate[EventType]
  implicit val l = PicklerUnpickler.generate[Option[List[Value]]]   // Scala pickling doesn't like Option[Seq[Value]]

  implicit val m = PicklerUnpickler.generate[SubjectType]
  implicit val n = PicklerUnpickler.generate[Option[PrivilegeLevel]]
  implicit val o = PicklerUnpickler.generate[Option[List[String]]]
  implicit val p = PicklerUnpickler.generate[Option[UUID]]

  implicit val q = PicklerUnpickler.generate[Option[TagOpCode]]
  implicit val r = PicklerUnpickler.generate[Option[List[UUID]]]
  implicit val t = PicklerUnpickler.generate[Option[IntegrityTag]]
  implicit val u = PicklerUnpickler.generate[Option[ConfidentialityTag]]

  implicit val v = PicklerUnpickler.generate[Option[Value]]
  implicit val w = PicklerUnpickler.generate[SrcSinkType]
}


class QuineDBActor(graph: GraphService) extends Actor with ActorLogging {

  implicit val implicitGraph = graph //GraphService(context.system, uiPort = 9090)(EmptyPersistor)
  implicit val ec = context.dispatcher

  log.info(s"QuineDB actor init")

  implicit class FutureAckOnComplete(f: Future[Any]) {
    def ackOnComplete(ackTo: ActorRef) = f.onComplete{
      case Success(_) => ackTo ! Ack
      case Failure(ex) => ex.printStackTrace(); ackTo ! Ack
    }
  }

  import scala.pickling.Defaults._
  import com.rrwright.quine.runtime.runtimePickleFormat
  import CDM17Implicits._

  implicit val timeout = Timeout(21 seconds)


  case class EventToObject(eventType: EventType, timestampNanos: Long, predicateObject: -->[QuineId]) extends FreeDomainNode[EventToObject] { val companion = EventToObject }
  case object EventToObject extends FreeNodeConstructor { type ClassType = EventToObject }


  override def receive = {

    case cdm: Principal =>
      val s = sender()
      cdm.create(Some(cdm.getUuid)).ackOnComplete(s)

    case cdm: FileObject =>
      val s = sender()
      cdm.create(Some(cdm.getUuid)).ackOnComplete(s)

    case cdm: Event =>
      val s = sender()

      cdm.create(Some(cdm.getUuid)).onComplete {
        case Failure(ex) => ex.printStackTrace(); s ! Ack
        case Success(_) =>
          // Mutations over time:
          cdm.eventType match {
            case EVENT_CLOSE =>
              // FileObject: Maybe delete events with preceding sequence numbers until you get to an EVENT_OPEN
              // SrcSinkObject: Maybe delete events with preceding sequence numbers (there is no EVENT_OPEN)
              // NetFlowObject: Delete Node and events to the the NetFlow.  (Close is essentially like UNLINK.)

//              println(s"Received EVENT_CLOSE")

            case EVENT_UNLINK =>
              // A file is deleted. The file node and all its events should be deleted.
              val objectUuid: QuineId = cdm.predicateObject.get.target
              val branch = branchOf[EventToObject]().refine('predicateObject --> objectUuid )
              graph.deleteFromBranch(branch).map(l => println(s"Deleted from EVENT_UNLINK branch: ${l.foldLeft(Set.empty[QuineId])((a,b) => a ++ b.allIds)}"))

            case EVENT_EXIT =>
              // A process exits. The process node and all its events should be deleted.
              val objectUuid: QuineId = cdm.predicateObject.get.target
              val branch = branchOf[EventToObject]().refine('predicateObject --> objectUuid )
              graph.deleteFromBranch(branch).map(l => println(s"Deleted from EVENT_EXIT branch: ${l.foldLeft(Set.empty[QuineId])((a,b) => a ++ b.allIds)}"))

    //        case EVENT_RENAME => // A file is renamed. Nothing to do here. There is only one predicate edge in Cadets data.

            case _ => ()
          }

          s ! Ack
      }





//      Event types in the Cadets Bovia CDM data:
//        [
//          "EVENT_READ",
//          "EVENT_SENDTO",
//          "EVENT_RECVFROM",
//          "EVENT_OPEN",
//          "EVENT_MMAP",
//          "EVENT_CLOSE",
//          "EVENT_LSEEK",
//          "EVENT_FCNTL",
//          "EVENT_WRITE",
//          "EVENT_EXIT",
//          "EVENT_RENAME",
//          "EVENT_SIGNAL",
//          "EVENT_FORK",
//          "EVENT_CREATE_OBJECT",
//          "EVENT_OTHER",
//          "EVENT_MPROTECT",
//          "EVENT_MODIFY_PROCESS",
//          "EVENT_CHANGE_PRINCIPAL",
//          "EVENT_CONNECT",
//          "EVENT_ACCEPT",
//          "EVENT_LOGIN",
//          "EVENT_EXECUTE",
//          "EVENT_UNLINK",
//          "EVENT_MODIFY_FILE_ATTRIBUTES",
//          "EVENT_SENDMSG",
//          "EVENT_RECVMSG",
//          "EVENT_LINK",
//          "EVENT_BIND"
//        ]



    case cdm: Subject =>
      val s = sender()
      cdm.create(Some(cdm.getUuid)).ackOnComplete(s)

    case cdm: NetFlowObject =>
      val s = sender()
      cdm.create(Some(cdm.getUuid)).ackOnComplete(s)

    case cdm: MemoryObject =>
      val s = sender()
      cdm.create(Some(cdm.getUuid)).ackOnComplete(s)

    case cdm: ProvenanceTagNode =>
      val s = sender()
      cdm.create(Some(cdm.getUuid)).ackOnComplete(s)

    case cdm: RegistryKeyObject =>
      val s = sender()
      cdm.create(Some(cdm.getUuid)).ackOnComplete(s)

    case cdm: SrcSinkObject =>
      val s = sender()
      cdm.create(Some(cdm.getUuid)).ackOnComplete(s)

    case cdm: TimeMarker =>  // Don't care about these.
      sender() ! Ack

    case cdm: CDM17 =>
      log.info(s"Unhandled CDM: $cdm")
      sender() ! Ack


    case InitMsg => sender() ! Ack

    case CompleteMsg =>
      println("DONE.")
      sender() ! Ack
//      graph.getRandomContextSentences(10, 4, Timeout(1001 seconds)).onComplete{
//        case Success(c) => c.foreach(x => println(s"${x._1} ->\n${x._2.mkString("\n")}"))
//        case Failure(e) => e.printStackTrace()
//      }

    case msg => log.warning(s"Unknown message: $msg")

  }
}


case class EventLookup(
  uuid: UUID,
//  sequence: Long = 0,
//  eventType: EventType,
//  threadId: Int,
//  subjectUuid: -->[UUID],
//  timestampNanos: Long,
//  predicateObject: Option[-->[UUID]] = None,
  predicateObjectPath: Option[String]
//  predicateObject2: Option[-->[UUID]] = None,
//  predicateObject2Path: Option[String] = None,
//  name: Option[String] = None,
//  parameters: Option[List[Value]] = None,
//  location: Option[Long] = None,
//  size: Option[Long] = None,
//  programPoint: Option[String] = None,
//  properties: Option[Map[String,String]] = None
) extends FreeDomainNode[EventLookup] {
  val companion = EventLookup
}

case object EventLookup extends FreeNodeConstructor {
  type ClassType = EventLookup
}



class QuineRouter(count: Int, graph: GraphService) extends Actor {
  var router = {
    val routees = Vector.fill(count) {
      val r = context.actorOf(Props(classOf[QuineDBActor], graph))
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case w: CDM17 =>
      router.route(w, sender())
    case Terminated(a) =>
      router = router.removeRoutee(a)
      val r = context.actorOf(Props[QuineDBActor])
      context watch r
      router = router.addRoutee(r)
    case x =>
      router.route(x, sender())
  }
}