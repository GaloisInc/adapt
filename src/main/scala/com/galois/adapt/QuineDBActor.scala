package com.galois.adapt

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem}
import akka.util.Timeout
import com.galois.adapt.cdm17._
import com.rrwright.quine.language._
import com.rrwright.quine.runtime.GraphService
import scala.concurrent.duration._
import scala.pickling.{FastTypeTag, Unpickler}



class QuineDBActor() extends Actor with ActorLogging {

  implicit val graph = GraphService(context.system, uiPort = 9090)


  import scala.pickling.Pickler
  import scala.pickling.Defaults._
  import scala.pickling.json.pickleFormat

  implicit val b = Pickler.generate[Option[Map[String,String]]]
//  implicit val l = Pickler.generate[Option[UUID]]
  implicit val t = Pickler.generate[Option[String]]
  implicit val g = Pickler.generate[AbstractObject]

  import com.rrwright.quine.language._


  override def receive = {

    case cdm: Principal =>
      implicit val m = Pickler.generate[PrincipalType]
      implicit val s = Pickler.generate[List[String]]  // canNOT also have this in scope: scala.pickling.Defaults.stringPickler
      implicit val x: NodeConstructor = Principal
      cdm.create(Some(cdm.uuid))

    case cdm: FileObject =>
      implicit val q = Pickler.generate[Option[Int]]
      implicit val s = Pickler.generate[Option[Long]]
      implicit val h = Pickler.generate[FileObjectType]
      implicit val i = Pickler.generate[CryptographicHash]
      implicit val n = Pickler.generate[Option[Seq[CryptographicHash]]]
      implicit val x: NodeConstructor = FileObject
      cdm.create(Some(cdm.getUuid))

    case cdm: Event =>
      implicit val j = Pickler.generate[EventType]
      implicit val s = Pickler.generate[Option[Long]]
      implicit val fp = Pickler.generate[Option[List[Value]]]   // Scala pickling doesn't like Option[Seq[Value]]
      implicit val x: NodeConstructor = Event
      cdm.create(Some(cdm.uuid))

    case cdm: Subject =>
      implicit val j = Pickler.generate[SubjectType]
      implicit val k = Pickler.generate[Option[PrivilegeLevel]]
      implicit val l = Pickler.generate[Option[Seq[String]]]
      implicit val m = Pickler.generate[Option[Int]]
      implicit val n = Pickler.generate[Option[UUID]]
      implicit val x: NodeConstructor = Subject
      cdm.create(Some(cdm.uuid))

    case cdm: NetFlowObject =>
      implicit val m = Pickler.generate[Option[Int]]
      cdm.create(Some(cdm.uuid))

    case cdm: MemoryObject =>
      implicit val s = Pickler.generate[Option[Long]]
      cdm.create(Some(cdm.uuid))

    case cdm: ProvenanceTagNode =>
      implicit val a = Pickler.generate[Option[UUID]]
      implicit val b = Pickler.generate[Option[TagOpCode]]
      implicit val c = Pickler.generate[Option[List[UUID]]]
      implicit val d = Pickler.generate[Option[IntegrityTag]]
      implicit val e = Pickler.generate[Option[ConfidentialityTag]]
      implicit val z = Pickler.generate[Option[Map[String,String]]]  // This needs to be here for some reason?!?
      cdm.create(Some(cdm.getUuid))

    case cdm: RegistryKeyObject =>
      implicit val p = Pickler.generate[Option[Value]]
      implicit val s = Pickler.generate[Option[Long]]
      cdm.create(Some(cdm.uuid))

    case cdm: SrcSinkObject =>
      implicit val q = Pickler.generate[Option[Int]]
      implicit val a = Pickler.generate[SrcSinkType]
      cdm.create(Some(cdm.uuid))

    case _ =>
      
  }
}

object DSL {
  import com.rrwright.quine.language._
  import scala.pickling.Defaults._
  import scala.pickling.json.pickleFormat

  case class TestObject(name: String) extends FreeDomainNode[TestObject] {
    val companion = TestObject
  }

  case object TestObject extends FreeNodeConstructor {
    type ClassType = TestObject
  }


  implicit val system = ActorSystem("test")
  implicit val graph = GraphService(system)
  implicit val ec = system.dispatcher
  implicit val timeout = Timeout(5 seconds)

  TestObject("test").create(None)

}