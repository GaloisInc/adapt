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
//  val dependencies = "FileIngestActor" :: Nil

//  def beginService() = initialize()

//  def endService() = ()

//  def subscriptions = dependencies.toSet[String].map(d => Subscription(dependencyMap(d).get, _.isInstanceOf[DomainNode]))

  implicit val graph = GraphService(context.system)


  import scala.pickling.Pickler
  import scala.pickling.Defaults._
  import scala.pickling.json.pickleFormat

  implicit val a = Pickler.generate[None.type]
  implicit val c = Pickler.generate[Some[Map[String,String]]]
  implicit val b = Pickler.generate[Option[Map[String,String]]]
  implicit val k = Pickler.generate[Some[UUID]]
  implicit val l = Pickler.generate[Option[UUID]]
  implicit val o = Pickler.generate[Some[Seq[CryptographicHash]]]
  implicit val n = Pickler.generate[Option[Seq[CryptographicHash]]]
  implicit val m = Pickler.generate[PrincipalType]
  implicit val p = Pickler.generate[Some[Int]]
  implicit val q = Pickler.generate[Option[Int]]
  implicit val r = Pickler.generate[Some[Long]]
  implicit val s = Pickler.generate[Option[Long]]
  //  implicit val e = scala.pickling.Defaults.stringPickler  //Pickler.generate[Seq[String]]
  implicit val t = Pickler.generate[Option[String]]
  implicit val u = Pickler.generate[Some[String]]
//  implicit val v = Pickler.generate[UUID]

  implicit val g = Pickler.generate[AbstractObject]
  implicit val h = Pickler.generate[FileObjectType]
    implicit val i = Pickler.generate[CryptographicHash]
    implicit val j = Pickler.generate[String]

    implicit val w = Unpickler.generate[String]



  import com.rrwright.quine.language._
  import com.galois.adapt.DSL._



  override def receive = {
//    case p: Principal => p.create(Some(p.uuid))
    case f: FileObject =>
//      f.create(Some(f.getUuid))

//    case
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