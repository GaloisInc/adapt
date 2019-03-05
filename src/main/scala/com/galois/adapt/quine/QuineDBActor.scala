package com.galois.adapt.quine

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import akka.util.Timeout
import com.galois.adapt.adm._
import com.galois.adapt.{Ack, ApiJsonProtocol, CompleteMsg, DBNodeable, DBQueryProxyActor, EdgeQuery, InitMsg, NodeQuery, Ready, StringQuery}
import com.rrwright.quine.runtime.GraphService
import java.util.UUID

import spray.json.{JsArray, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import com.rrwright.quine.gremlin.{GremlinQueryRunner, TypeAnnotationFieldReader}
import com.rrwright.quine.runtime.{NameSpacedUuidProvider, QuineIdProvider}
import com.rrwright.quine.language.{DomainNodeSetSingleton, PickleReader, QuineId}
// import com.rrwright.quine.language.BoopickleScheme._
import com.rrwright.quine.language.JavaObjectSerializationScheme._

object AdmUuidProvider extends QuineIdProvider[AdmUUID] {
  val underlying = NameSpacedUuidProvider(List("synthetic"),0)
  private implicit def toNamespacedId(a: AdmUUID): (String, UUID) = (a.namespace, a.uuid)
  private implicit def fromNamespacedId(x: (String, UUID)): AdmUUID  = AdmUUID(x._2, x._1)

  def newId() = underlying.newId()
  def hashedCustomId(bytes: Array[Byte]) = underlying.hashedCustomId(bytes)
  def customIdToString(typed: AdmUUID) = typed.rendered
  def customIdToBytes(typed: AdmUUID) = underlying.customIdToBytes(typed)
  def customIdFromBytes(bytes: Array[Byte]) = underlying.customIdFromBytes(bytes).map(x => x)
  def customIdFromString(str: String) = Try(AdmUUID.fromRendered(str))
}

class QuineDBActor(graphService: GraphService[AdmUUID], idx: Int) extends DBQueryProxyActor {

  implicit val service = graphService
  implicit val timeout = Timeout(21 seconds)
  lazy val graph: org.apache.tinkerpop.gremlin.structure.Graph = ???

  val gremlin = GremlinQueryRunner(
    graph = graphService,
    fieldReader = TypeAnnotationFieldReader(
      fieldToTypeName = Map(
        "type_of" -> "String",
        "hostName" -> "String",
        "provider" -> "String",
        "fileObjectType" -> "FileObjectType",
        "originalCdmUuids" -> "Seq[CdmUUID]",
        "size" -> "Option[Long]"
      ),
      defaultTypeNames = Seq("Boolean", "Long", "Int", "List[Int]", "List[Long]", "String"),
      typeNameToPickleReader = Map(
        "Boolean"         -> PickleReader[Boolean],   
        "Long"            -> PickleReader[Long],
        "Int"             -> PickleReader[Int],
        "List[Long]"      -> PickleReader[List[Long]],
        "List[Int]"       -> PickleReader[List[Int]],
        "String"          -> PickleReader[String],
        "FileObjectType"  -> PickleReader[com.galois.adapt.cdm19.FileObjectType],
        "Seq[CdmUUID]"    -> PickleReader[List[CdmUUID]],
        "Option[Long]"    -> PickleReader[Option[Long]]
      )
    ),
    labelKey = "type_of"
  )

//  log.info(s"QuineDB actor init")

  implicit class FutureAckOnComplete(f: Future[_]) extends AnyRef {
    def ackOnComplete(ackTo: ActorRef, successF: => Unit = ()): Unit = f.onComplete{
      case Success(_) => ackTo ! Ack
      case Failure(ex) => ex.printStackTrace(); ackTo ! Ack
    }
  }


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
    case anAdm: AdmEvent              => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid))
    case anAdm: AdmSubject            => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid))
    case anAdm: AdmPrincipal          => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid))
    case anAdm: AdmFileObject         => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid))
    case anAdm: AdmNetFlowObject      => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid))
    case anAdm: AdmPathNode           => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid))
    case anAdm: AdmPort               => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid))
    case anAdm: AdmAddress            => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid))
    case anAdm: AdmSrcSinkObject      => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid))
    case anAdm: AdmProvenanceTagNode  => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid))
    case anAdm: AdmHost               => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid))
    case anAdm: AdmSynthesized        => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid))
    case _                            => throw new Exception("Unexpected ADM")
  }).flatMap {
    case Success(_) => Future.successful(())
    case Failure(f) => Future.failed(f)
  }

  def writeAdmEdge(e: EdgeAdm2Adm): Future[Unit] = graphService.dumbOps.addEdge(
    AdmUuidProvider.customIdToQid(e.src),
    AdmUuidProvider.customIdToQid(e.tgt),
    e.label
  )

  def FutureTx[T](body: => T)(implicit ec: ExecutionContext): Future[T] = Future(body)

  override def receive = {

    // Run the query without specifying what the output type will be. This is the variant used by 'cmdline_query.py'
    case WithSender(sndr, StringQuery(q, shouldParse)) =>
      log.debug(s"Received string query: $q")
      println(s"Received string query: $q")
      sndr ! {
        gremlin.queryEither(q).map { resultsEither =>
          resultsEither.fold(qge => Failure(throw qge), Success(_)).map { results =>
            if (shouldParse) {
              ApiJsonProtocol.anyToJson(results.toList)
            } else {
              JsString(results.map(r => s""""${r.toString.replace("\\", "\\\\").replace("\"", "\\\"")}"""").mkString("[", ",", "]"))
            }
          }
        }
      }

    // Run a query that returns vertices
    case WithSender(sndr, NodeQuery(q, shouldParse)) =>
      log.debug(s"Received node query: $q")
      println(s"Received node query: $q")
      sndr ! {
        // The Gremlin adapter for quine doesn't store much information on nodes, so we have to
        // actively go get that information
        gremlin.queryEither(q + s""".as('vertex')
                                   |.valueMap().as('valueMap').select('vertex')
                                   |.id().as('id')
                                   |.select('valueMap','id')""".stripMargin).map { verticesEither =>
          verticesEither.fold(qge => Failure(throw qge), Success(_)).map { vertices =>
            if (shouldParse) JsArray(vertices.map(ApiJsonProtocol.quineVertexToJson).toVector)
            else vertices.toStream
          }
        }
      }

    // Run a query that returns edges
    case WithSender(sndr, EdgeQuery(q, shouldParse)) =>
      log.debug(s"Received new edge query: $q")
      println(s"Received new edge query: $q")
      sndr ! {
        gremlin.queryEitherExpecting[com.rrwright.quine.gremlin.Edge](q).map { edgesEither =>
          edgesEither.fold(qge => Failure(throw qge), Success(_)).map { edges =>
            if (shouldParse) JsArray(edges.map(ApiJsonProtocol.quineEdgeToJson).toVector)
            else edges.toList.toStream
          }
        }
      }


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


class QuineRouter(count: Int, graph: GraphService[AdmUUID]) extends Actor with ActorLogging {
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

