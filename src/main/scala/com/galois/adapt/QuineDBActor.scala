package com.galois.adapt

import java.nio.ByteBuffer
import shapeless.cachedImplicit
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import akka.util.Timeout
import com.galois.adapt.adm._
import com.rrwright.quine.runtime.{GraphService, NameSpacedUuidProvider, QuineIdProvider, StandingFetchActor, StandingQueryId}
import java.util.UUID
import com.galois.adapt.cdm20._
import spray.json.{JsArray, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import com.rrwright.quine.gremlin.{GremlinQueryRunner, TypeAnnotationFieldReader}
import com.rrwright.quine.language.{DomainNode, DomainNodeSetSingleton, NoConstantsDomainNode, PickleReader, PickleScheme, Queryable, QuineId}
import com.rrwright.quine.language.EdgeDirections._
import com.rrwright.quine.runtime.{NameSpacedUuidProvider, QuineIdProvider}
import com.rrwright.quine.language.BoopickleScheme._
//import com.rrwright.quine.language.JavaObjectSerializationScheme._


object AdmUuidProvider extends QuineIdProvider[AdmUUID] {

  private val stringPickler = implicitly[PickleScheme[String]]

  // Given a namespace, get the host index
  private val namespaceIdx: Map[String, HostIdx] =
    AdaptConfig.quineConfig.hosts.zipWithIndex.flatMap {
      case (AdaptConfig.QuineHost(_, namespaces), hostIdx) => namespaces.map(_ -> hostIdx)
    }.toMap

  println(s"AdmUuidProvider namespaceIdx:\n${namespaceIdx.mkString("\n")}")


  def newCustomId(): AdmUUID = AdmUUID(UUID.randomUUID(), "synthesized")

  override def newCustomIdFromData(data: Any): AdmUUID = newCustomId().copy(namespace = data.asInstanceOf[String])

  def customIdToString(typed: AdmUUID): String = typed.rendered
  def customIdFromString(s: String): Try[AdmUUID] = Try(AdmUUID.fromRendered(s))

  def customIdToBytes(typed: AdmUUID): Array[Byte] = {
    val stringBytes = stringPickler.write(typed.namespace)
    ByteBuffer.allocate(16 + stringBytes.length)
      .putLong(typed.uuid.getMostSignificantBits).putLong(typed.uuid.getLeastSignificantBits)
      .put(stringBytes)
      .array()
  }
  def customIdFromBytes(bytes: Array[Byte]): Try[AdmUUID] = Try {
    import com.rrwright.quine.runtime.bbRemainder
    val bb = ByteBuffer.wrap(bytes)
    val uuid = new UUID(bb.getLong(), bb.getLong())
    AdmUUID(uuid, stringPickler.read(bb.remainingBytes))
  }

  def hashedCustomId(bytes: Array[Byte]): AdmUUID = AdmUUID(UUID.nameUUIDFromBytes(bytes), "synthesized")


//  def ppmTreeRootNodeId(hostName: String, treeName: String) = new QuineId(customIdToBytes(AdmUUID(UUID.nameUUIDFromBytes(treeName.getBytes), hostName)))


  override def qidDistribution(qid: QuineId): (HostIdx, LocalShardIdx) = {
    customIdFromQid(qid) match {
      case Success(admUuid) =>
        // Host index is defined with the first match of:
        //   1.) saved Idx for hostname,
        //   2.) first saved Idx for `admUuid.hostname` PREFIX,
        //   3.) random according to hashcode  (includes namespace: "synthesized")
        val h = Math.abs(admUuid.hashCode)
        lazy val prefixMatchOrHash = namespaceIdx.keys.find(k => admUuid.namespace.startsWith(k))   // TODO: Consider adding new entries in namespaceIdx to speed this up.
          .fold(h)(matchedKey => namespaceIdx(matchedKey))
        namespaceIdx.getOrElse(admUuid.namespace, prefixMatchOrHash) -> h

      case Failure(_) =>
        val randomIdx = Math.abs(ByteBuffer.wrap(hashToLength(qid.array, 4)).getInt())
        randomIdx -> randomIdx
    }
  }
}



case class ObjectWriter(did_write: <--[ESOSubject]) extends NoConstantsDomainNode
case class ObjectExecutor(did_execute: <--[ESOSubject]) extends NoConstantsDomainNode

case class LatestNetflowRead(remoteAddress: Option[String], remotePort: Option[Int], localAddress: Option[String], localPort: Option[Int], latestTimestampNanos: Long, qid: Array[Byte])
case class NetflowReadingProcess(cid: Int, path: AdmPathNode, latestNetflowRead: LatestNetflowRead) extends NoConstantsDomainNode

// TODO: More than just `cmdLine` on Subjects?!?
case class ParentProcess(cid: Int, subjectTypes: Set[SubjectType], path: AdmPathNode, startTimestampNanos: Long, hostName: String) extends NoConstantsDomainNode
case class ChildProcess(cid: Int, subjectTypes: Set[SubjectType], path: AdmPathNode, startTimestampNanos: Long, hostName: String, parentSubject: ParentProcess) extends NoConstantsDomainNode

case class ESOSubject(cid: Int, subjectTypes: Set[SubjectType], path: AdmPathNode) extends NoConstantsDomainNode

case class ESOFileObject(fileObjectType: FileObjectType, path: AdmPathNode) extends NoConstantsDomainNode
case class ESOSrcSinkObject(srcSinkType: SrcSinkType) extends NoConstantsDomainNode
case class ESONetFlowObject(remoteAddress: Option[String], localAddress: Option[String], remotePort: Option[Int], localPort: Option[Int]) extends NoConstantsDomainNode

case class ESOFileInstance(eventType: EventType, earliestTimestampNanos: Long, latestTimestampNanos: Long, hostName: String, subject: ESOSubject, predicateObject: ESOFileObject) extends NoConstantsDomainNode
case class ESOSrcSnkInstance(eventType: EventType, earliestTimestampNanos: Long, latestTimestampNanos: Long, hostName: String, subject: ESOSubject, predicateObject: ESOSrcSinkObject) extends NoConstantsDomainNode
case class ESONetworkInstance(eventType: EventType, earliestTimestampNanos: Long, latestTimestampNanos: Long, hostName: String, subject: ESOSubject, predicateObject: ESONetFlowObject) extends NoConstantsDomainNode


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
        "eventType" -> "EventType",
        "originalCdmUuids" -> "Set[CdmUUID]",
        "size" -> "Option[Long]",
        "inputType" -> "Option[String]",
        "deviceType" -> "Option[String]",
        "subjectTypes" -> "Set[SubjectType]",
        "srcSinkType" -> "SrcSinkType",
        "localAddress" -> "Option[String]",
        "localPort" -> "Option[Int]",
        "remoteAddress" -> "Option[String]",
        "remotePort" -> "Option[Int]"
      ),
      defaultTypeNames = Seq("Boolean", "Long", "Int", "List[Int]", "List[Long]", "String"),
      typeNameToPickleReader = Map(
        "Boolean"          -> PickleReader[Boolean],
        "Long"             -> PickleReader[Long],
        "Int"              -> PickleReader[Int],
        "List[Long]"       -> PickleReader[List[Long]],
        "List[Int]"        -> PickleReader[List[Int]],
        "String"           -> PickleReader[String],
        "FileObjectType"   -> PickleReader[FileObjectType],
        "EventType"        -> PickleReader[EventType],
        "Set[CdmUUID]"     -> PickleReader[Set[CdmUUID]],
        "Option[Int]"      -> PickleReader[Option[Int]],
        "Option[Long]"     -> PickleReader[Option[Long]],
        "Option[String]"   -> PickleReader[Option[String]],
        "Set[SubjectType]" -> PickleReader[Set[SubjectType]],
        "SrcSinkType"      -> PickleReader[SrcSinkType]
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



  implicit val queryableEsoSubject: Queryable[ESOSubject] = cachedImplicit
  implicit val queryableEsoFileObject: Queryable[ESOFileObject] = cachedImplicit
  implicit val queryableEsoSrcSinkObject: Queryable[ESOSrcSinkObject] = cachedImplicit
  implicit val queryableEsoNetFlow: Queryable[ESONetFlowObject] = cachedImplicit
  implicit val queryableEsoFileInstance: Queryable[ESOFileInstance] = cachedImplicit
  implicit val queryableEsoSrcSnkInstance: Queryable[ESOSrcSnkInstance] = cachedImplicit
  implicit val queryableEsoNetworkInstance: Queryable[ESONetworkInstance] = cachedImplicit
  implicit val admSubjectInstance: Queryable[AdmSubject] = cachedImplicit
  implicit val admPrincipalInstance: Queryable[AdmPrincipal] = cachedImplicit
  implicit val admFileObjectInstance: Queryable[AdmFileObject] = cachedImplicit
  implicit val admNetFlowObjectInstance: Queryable[AdmNetFlowObject] = cachedImplicit
  implicit val admPathNodeInstance: Queryable[AdmPathNode] = cachedImplicit
  implicit val admPortInstance: Queryable[AdmPort] = cachedImplicit
  implicit val admAddressInstance: Queryable[AdmAddress] = cachedImplicit
  implicit val admSrcSinkObjectInstance: Queryable[AdmSrcSinkObject] = cachedImplicit
  implicit val admProvenanceTagNodeInstance: Queryable[AdmProvenanceTagNode] = cachedImplicit
  implicit val admHostInstance: Queryable[AdmHost] = cachedImplicit
  implicit val admSynthesizedInstance: Queryable[AdmSynthesized] = cachedImplicit

  def writeAdm(a: ADM): Future[Unit] = (a match {
    case anAdm: AdmEvent =>
      anAdm.create(Some(anAdm.uuid)).map { x =>
        graphService.standingFetch[ESOFileInstance](anAdm.uuid, Some(StandingQueryId("standing-fetch_ESOFile-accumulator")))(x => {})
        graphService.standingFetch[ESOSrcSnkInstance](anAdm.uuid, Some(StandingQueryId("standing-fetch_ESOSrcSnk-accumulator")))(x => {})
        graphService.standingFetch[ESONetworkInstance](anAdm.uuid, Some(StandingQueryId("standing-fetch_ESONetwork-accumulator")))(x => {})
        x
      }
    case anAdm: AdmSubject =>
      anAdm.create(Some(anAdm.uuid)).map { x =>
        graphService.standingFetch[ChildProcess](anAdm.uuid, Some(StandingQueryId("standing-fetch_ProcessParentage")))(_ => {})
        x
      }
    case anAdm: AdmPrincipal          => anAdm.create(Some(anAdm.uuid))
    case anAdm: AdmFileObject         => anAdm.create(Some(anAdm.uuid))
    case anAdm: AdmNetFlowObject      => anAdm.create(Some(anAdm.uuid))
    case anAdm: AdmPathNode           => anAdm.create(Some(anAdm.uuid))
    case anAdm: AdmPort               => anAdm.create(Some(anAdm.uuid))
    case anAdm: AdmAddress            => anAdm.create(Some(anAdm.uuid))
    case anAdm: AdmSrcSinkObject      => anAdm.create(Some(anAdm.uuid))
    case anAdm: AdmProvenanceTagNode  => anAdm.create(Some(anAdm.uuid))
    case anAdm: AdmHost               => anAdm.create(Some(anAdm.uuid))
    case anAdm: AdmSynthesized        => anAdm.create(Some(anAdm.uuid))
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

