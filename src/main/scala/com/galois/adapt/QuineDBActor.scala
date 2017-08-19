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
  implicit val l = Pickler.generate[Option[UUID]]
  implicit val t = Pickler.generate[Option[String]]
  implicit val g = Pickler.generate[AbstractObject]

  import com.rrwright.quine.language._

  override def receive = {
    case p: Principal =>
      implicit val m = Pickler.generate[PrincipalType]
      implicit val s = Pickler.generate[List[String]]  // canNOT also have this in scope: scala.pickling.Defaults.stringPickler
      p.create(Some(p.uuid))
    case f: FileObject =>
      implicit val q = Pickler.generate[Option[Int]]
      implicit val s = Pickler.generate[Option[Long]]
      implicit val h = Pickler.generate[FileObjectType]
      implicit val i = Pickler.generate[CryptographicHash]
      implicit val n = Pickler.generate[Option[Seq[CryptographicHash]]]
      f.create(Some(f.getUuid))
    case e: Event =>
      implicit val j = Pickler.generate[EventType]
      implicit val s = Pickler.generate[Option[Long]]
      implicit val fp = Pickler.generate[Option[List[Value]]]   // Scala pickling doesn't like Option[Seq[Value]]
      e.create(Some(e.uuid))

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