package com.galois.adapt.quine

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import akka.util.Timeout
import com.galois.adapt.adm._
import com.galois.adapt.{Ack, CompleteMsg, DBNodeable, DBQueryProxyActor, InitMsg, Ready}
import com.rrwright.quine.runtime.GraphService

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import com.rrwright.quine.language.DomainNodeSetSingleton
import com.rrwright.quine.language.JavaObjectSerializationScheme._   // IntelliJ sometimes can't tell that this is used for implicits

class QuineDBActor(graphService: GraphService, idx: Int) extends DBQueryProxyActor {

  implicit val service = graphService
  lazy val graph: org.apache.tinkerpop.gremlin.structure.Graph = ???

//  log.info(s"QuineDB actor init")

  implicit class FutureAckOnComplete(f: Future[_]) extends AnyRef {
    def ackOnComplete(ackTo: ActorRef, successF: => Unit = ()): Unit = f.onComplete{
      case Success(_) => ackTo ! Ack
      case Failure(ex) => ex.printStackTrace(); ackTo ! Ack
    }
  }

  implicit val timeout = Timeout(21 seconds)

  def DBNodeableTx(cdms: Seq[DBNodeable[_]]): Try[Unit] = ??? 
  
  // TODO Make an async interface for this - the 'Await' is gross.
  def AdmTx(adms: Seq[Either[ADM,EdgeAdm2Adm]]): Try[Unit] = Try(Await.result(
    Future.sequence(adms.map {
      case Left(a: ADM) => writeAdm(a)
      case Right(e: EdgeAdm2Adm) => writeAdmEdge(e) 
    }),
    Duration.Inf
  ))

  def writeAdm(a: ADM): Future[Unit] = (a match {
    case anAdm: AdmEvent              => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
    case anAdm: AdmSubject            => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
    case anAdm: AdmPrincipal          => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
    case anAdm: AdmFileObject         => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
    case anAdm: AdmNetFlowObject      => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
    case anAdm: AdmPathNode           => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
    case anAdm: AdmPort               => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
    case anAdm: AdmAddress            => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
    case anAdm: AdmSrcSinkObject      => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
    case anAdm: AdmProvenanceTagNode  => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
    case anAdm: AdmHost               => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
    case anAdm: AdmSynthesized        => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
    case _                            => throw new Exception("Unexpected ADM")
  }).flatMap {
    case Success(_) => Future.successful(())
    case Failure(f) => Future.failed(f)
  }

  def writeAdmEdge(e: EdgeAdm2Adm): Future[Unit] = graphService.dumbOps.addEdge(e.src, e.tgt, e.label)

  def FutureTx[T](body: => T)(implicit ec: ExecutionContext): Future[T] = Future(body)

  override def receive = {

    case WithSender(s: ActorRef, Left(a: ADM))          => writeAdm(a).ackOnComplete(s)
    case WithSender(s: ActorRef, Right(e: EdgeAdm2Adm)) => writeAdmEdge(e).ackOnComplete(s)

    case InitMsg => sender() ! Ack

    case Ready => sender() ! Ack

    case CompleteMsg =>
      println(s"Data loading complete: $idx")
      sender() ! Ack

    case msg => log.warning(s"Unknown message: $msg")

  }
}

case class WithSender[A](sender: ActorRef, message: A)


class QuineRouter(count: Int, graph: GraphService) extends Actor with ActorLogging {
  var router = {
    val routees = (0 until count) map { idx =>
      val r = context.actorOf(Props(classOf[QuineDBActor], graph, idx))
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  var nextIdx: Int = count

  def receive = {
    case msg @ Terminated(a) =>
      log.warning(s"Received $msg")
      router = router.removeRoutee(a)
      val r = context.actorOf(Props(classOf[QuineDBActor], graph, nextIdx))
      nextIdx += 1
      context watch r
      router = router.addRoutee(r)
    case x => router.route(WithSender(sender(), x), sender())
  }
}

