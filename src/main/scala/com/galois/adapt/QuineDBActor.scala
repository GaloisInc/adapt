package com.galois.adapt

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import akka.util.Timeout
import com.galois.adapt.cdm17._
import com.rrwright.quine.language._
import com.rrwright.quine.runtime.{EmptyPersistor, GraphService}

import scala.concurrent.duration._
import scala.pickling.PicklerUnpickler
import scala.util.{Failure, Success}
import scala.pickling.shareNothing._
//import scala.pickling.static._        // Avoid run-time reflection


class QuineDBActor(gr: GraphService) extends Actor with ActorLogging {

  implicit val graph = gr //GraphService(context.system, uiPort = 9090)(EmptyPersistor)
  implicit val ec = context.dispatcher

  log.warning(s"QuineDB actor init")


  import scala.pickling.Pickler
  import scala.pickling.Defaults._
  import com.rrwright.quine.runtime.runtimePickleFormat

  implicit val b = PicklerUnpickler.generate[Option[Map[String,String]]]
//  implicit val l = PicklerUnpickler.generate[Option[UUID]]
  implicit val t = PicklerUnpickler.generate[Option[String]]
  implicit val y = PicklerUnpickler.generate[AbstractObject]


  var isSet = false

  var counter = 0


  implicit val timeout = Timeout(21 seconds)


  override def receive = {

    case cdm: Principal =>
      val s = sender()
      implicit val m = Pickler.generate[PrincipalType]
      implicit val i = Pickler.generate[List[String]]  // canNOT also have this in scope: scala.pickling.Defaults.stringPickler
      cdm.create(Some(cdm.getUuid))
        .onComplete{
          case Success(_) => s ! Ack
          case Failure(e) => e.printStackTrace(); s ! Ack
        }

    case cdm: FileObject =>
      val s = sender()
      implicit val q = Pickler.generate[Option[Int]]
      implicit val o = Pickler.generate[Option[Long]]
      implicit val h = Pickler.generate[FileObjectType]
      implicit val i = Pickler.generate[CryptographicHash]
      implicit val n = Pickler.generate[Option[Seq[CryptographicHash]]]
      cdm.create(Some(cdm.getUuid))
        .onComplete{
          case Success(_) => s ! Ack
          case Failure(e) => e.printStackTrace(); s ! Ack
        }

    case cdm: Event =>
      val s = sender()
      implicit val j = Pickler.generate[EventType]
      implicit val i = Pickler.generate[Option[Long]]
      implicit val fp = Pickler.generate[Option[List[Value]]]   // Scala pickling doesn't like Option[Seq[Value]]
//      counter += 1
//      cdm.predicateObjectPath.map(s => println(s"##$counter: $s"))
//      if (cdm.predicateObjectPath == Some("/etc/libmap.conf")) println(cdm.toBranch)
//      if (counter == 1000) {
//        implicit val t = Timeout(10 seconds)
//
////        println(EventLookup(UUID.randomUUID(), Some("test_string")).toBranch)
//        implicit val uu = Unpickler.generate[Option[String]]
//
//
//        lookup[EventLookup]( 'predicateObjectPath := Some("/etc/libmap.conf") ).onComplete{
//          case Success(list) => println(s"\nSUCCESS: $list\n")
//          case Failure(e) => println(s"\nFAILED: $e\n")
//        }
//      }
      cdm.create(Some(cdm.getUuid))
        .onComplete{
          case Success(_) => s ! Ack
          case Failure(e) => e.printStackTrace(); s ! Ack
        }

    case cdm: Subject =>
      val s = sender()
      implicit val j = PicklerUnpickler.generate[SubjectType]
      implicit val k = PicklerUnpickler.generate[Option[PrivilegeLevel]]
      implicit val l = PicklerUnpickler.generate[Option[Seq[String]]]
      implicit val m = PicklerUnpickler.generate[Option[Int]]
      implicit val n = PicklerUnpickler.generate[Option[UUID]]

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
      implicit val m = Pickler.generate[Option[Int]]
      cdm.create(Some(cdm.getUuid))
        .onComplete{
          case Success(_) => s ! Ack
          case Failure(e) => e.printStackTrace(); s ! Ack
        }

    case cdm: MemoryObject =>
      val s = sender()
      implicit val l = Pickler.generate[Option[Long]]
      cdm.create(Some(cdm.getUuid))
        .onComplete{
          case Success(_) => s ! Ack
          case Failure(e) => e.printStackTrace(); s ! Ack
        }

    case cdm: ProvenanceTagNode =>
      val s = sender()
      implicit val a = Pickler.generate[Option[UUID]]
      implicit val b = Pickler.generate[Option[TagOpCode]]
      implicit val c = Pickler.generate[Option[List[UUID]]]
      implicit val d = Pickler.generate[Option[IntegrityTag]]
      implicit val f = Pickler.generate[Option[ConfidentialityTag]]
      implicit val z = Pickler.generate[Option[Map[String,String]]]  // This needs to be here for some reason. WTF?!?
      cdm.create(Some(cdm.getUuid))
        .onComplete{
          case Success(_) => s ! Ack
          case Failure(e) => e.printStackTrace(); s ! Ack
        }

    case cdm: RegistryKeyObject =>
      val s = sender()
      implicit val p = Pickler.generate[Option[Value]]
      implicit val q = Pickler.generate[Option[Long]]
      cdm.create(Some(cdm.getUuid))
        .onComplete{
          case Success(_) => s ! Ack
          case Failure(e) => e.printStackTrace(); s ! Ack
        }

    case cdm: SrcSinkObject =>
      val s = sender()
      implicit val q = Pickler.generate[Option[Int]]
      implicit val a = Pickler.generate[SrcSinkType]
      cdm.create(Some(cdm.getUuid))
        .onComplete{
          case Success(_) => s ! Ack
          case Failure(e) => e.printStackTrace(); s ! Ack
        }

    case InitMsg => sender() ! Ack

    case CompleteMsg =>
      println("DONE.")
      sender() ! Ack
      graph.getRandomContextSentences(10, 4, Timeout(1001 seconds)).onComplete{
        case Success(c) => c.foreach(x => println(s"${x._1} ->\n${x._2.mkString("\n")}"))
        case Failure(e) => e.printStackTrace()
      }

    case _: CDM17 => sender() ! Ack

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