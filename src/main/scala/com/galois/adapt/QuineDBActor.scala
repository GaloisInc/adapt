package com.galois.adapt

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import akka.util.Timeout
import com.galois.adapt.cdm17._
import com.rrwright.quine.language.EdgeDirections.{-->, <--}
import com.rrwright.quine.language._
import com.rrwright.quine.runtime.{EmptyPersistor, GraphService}

import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.pickling.shareNothing._
//import scala.pickling.static._        // Avoid run-time reflection


class QuineDBActor(gr: GraphService) extends Actor with ActorLogging {

  implicit val graph = gr //GraphService(context.system, uiPort = 9090)(EmptyPersistor)
  implicit val ec = context.dispatcher

  log.warning(s"QuineDB actor init")


  import scala.pickling.PicklerUnpickler
  import scala.pickling.Defaults._
  import com.rrwright.quine.runtime.runtimePickleFormat

  implicit val a = PicklerUnpickler.generate[Option[Map[String,String]]]
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

  var isSet = false

  var counter = 0


  implicit val timeout = Timeout(21 seconds)


  override def receive = {

    case cdm: Principal =>
      val s = sender()
      cdm.create(Some(cdm.getUuid))
        .onComplete{
          case Success(_) => s ! Ack
          case Failure(e) => e.printStackTrace(); s ! Ack
        }

    case cdm: FileObject =>
      val s = sender()
      cdm.create(Some(cdm.getUuid))
        .onComplete{
          case Success(_) => s ! Ack
          case Failure(e) => e.printStackTrace(); s ! Ack
        }

    case cdm: Event =>
      val s = sender()
////        println(EventLookup(UUID.randomUUID(), Some("test_string")).toBranch)
//        lookup[EventLookup]( 'predicateObjectPath := Some("/etc/libmap.conf") ).onComplete{
//          case Success(list) => println(s"\nSUCCESS: $list\n")
//          case Failure(e) => println(s"\nFAILED: $e\n")
//        }

      cdm.create(Some(cdm.getUuid)).onComplete{
        case Success(_) => s ! Ack
        case Failure(e) => e.printStackTrace(); s ! Ack
      }

    case cdm: Subject =>
      val s = sender()

      if (! isSet) {
        isSet = true
//        refinedBranchOf[Subject]().standingFind(println)
      }

      cdm.create(Some(cdm.getUuid))
        .onComplete{
          case Success(_) => s ! Ack
          case Failure(e) => e.printStackTrace(); s ! Ack
        }

    case cdm: NetFlowObject =>
      val s = sender()
      cdm.create(Some(cdm.getUuid))
        .onComplete{
          case Success(_) => s ! Ack
          case Failure(e) => e.printStackTrace(); s ! Ack
        }

    case cdm: MemoryObject =>
      val s = sender()
      cdm.create(Some(cdm.getUuid))
        .onComplete{
          case Success(_) => s ! Ack
          case Failure(e) => e.printStackTrace(); s ! Ack
        }

    case cdm: ProvenanceTagNode =>
      val s = sender()
      cdm.create(Some(cdm.getUuid))
        .onComplete{
          case Success(_) => s ! Ack
          case Failure(e) => e.printStackTrace(); s ! Ack
        }

    case cdm: RegistryKeyObject =>
      val s = sender()
      cdm.create(Some(cdm.getUuid))
        .onComplete{
          case Success(_) => s ! Ack
          case Failure(e) => e.printStackTrace(); s ! Ack
        }

    case cdm: SrcSinkObject =>
      val s = sender()
      cdm.create(Some(cdm.getUuid))
        .onComplete{
          case Success(_) => s ! Ack
          case Failure(e) => e.printStackTrace(); s ! Ack
        }

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