package com.galois.adapt

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem}
import akka.util.Timeout
import com.galois.adapt.cdm17._
import com.rrwright.quine.language._
import com.rrwright.quine.runtime.{EmptyPersistor, GraphService}

import scala.concurrent.duration._
import scala.pickling.Unpickler
import scala.util.{Failure, Success}


class QuineDBActor(gr: GraphService) extends Actor with ActorLogging {

  implicit val graph = gr //GraphService(context.system, uiPort = 9090)(EmptyPersistor)
  implicit val ec = context.dispatcher

  log.warning(s"QuineDB actor init")


  import scala.pickling.Pickler
  import scala.pickling.Defaults._
  import scala.pickling.json.pickleFormat

  implicit val b = Pickler.generate[Option[Map[String,String]]]
//  implicit val l = Pickler.generate[Option[UUID]]
  implicit val t = Pickler.generate[Option[String]]
  implicit val y = Pickler.generate[AbstractObject]

  var counter = 0

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
      counter += 1
//      cdm.predicateObjectPath.map(s => println(s"##$counter: $s"))
//      if (cdm.predicateObjectPath == Some("/etc/libmap.conf")) println(cdm.toBranch)
      if (counter == 1000) {
        implicit val t = Timeout(10 seconds)

//        println(EventLookup(UUID.randomUUID(), Some("test_string")).toBranch)
        implicit val uu = Unpickler.generate[Option[String]]


        lookup[EventLookup]( 'predicateObjectPath := Some("/etc/libmap.conf") ).onComplete{
          case Success(list) => println(s"\nSUCCESS: $list\n")
          case Failure(e) => println(s"\nFAILED: $e\n")
        }
      }
      cdm.create(Some(cdm.getUuid))
        .onComplete{
          case Success(_) => s ! Ack
          case Failure(e) => e.printStackTrace(); s ! Ack
        }

    case cdm: Subject =>
      val s = sender()
      implicit val j = Pickler.generate[SubjectType]
      implicit val k = Pickler.generate[Option[PrivilegeLevel]]
      implicit val l = Pickler.generate[Option[Seq[String]]]
      implicit val m = Pickler.generate[Option[Int]]
      implicit val n = Pickler.generate[Option[UUID]]
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

    case Init => sender() ! Ack

    case Complete => println("DONE.")

    case _: CDM17 => sender() ! Ack

    case msg => log.warning(s"Unknown message: $msg")
      
  }
}

case object Init
case object Ack
case object Complete


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