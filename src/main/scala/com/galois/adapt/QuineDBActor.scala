package com.galois.adapt

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.galois.adapt.cdm17._
import com.rrwright.quine.language._
import com.rrwright.quine.runtime.GraphService

import scala.pickling.FastTypeTag



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
  implicit val d = Pickler.generate[PrincipalType]
  //  implicit val e = scala.pickling.Defaults.stringPickler  //Pickler.generate[Seq[String]]
  //  implicit val f = Pickler.generate[Option[String]]

  implicit val g = Pickler.generate[AbstractObject]
  implicit val h = Pickler.generate[FileObjectType]
  //  implicit val i = Pickler.generate[CryptographicHash]
  //  implicit val j = Pickler.generate[Some[Int]]

//  implicit def k[T: FastTypeTag] = Pickler.generate[T]


  override def receive = {
//    case p: Principal => p.create(Some(p.uuid))
    case f: FileObject => f.create(Some(f.uuid))

  }
}