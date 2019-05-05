package com.galois.adapt

import java.io.{File, PrintWriter}
import java.nio.ByteBuffer
import java.text.NumberFormat
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

import shapeless.cachedImplicit
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import akka.util.Timeout
import com.galois.adapt.AdaptConfig.HostName
import com.galois.adapt.NoveltyDetection.NamespacedUuidDetails
import com.galois.adapt.adm._
import com.galois.adapt.cdm20._
import spray.json.{JsArray, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._
import com.rrwright.quine.runtime.{FutureRecoverWith, GraphService, Novelty, QuineIdProvider}
import com.rrwright.quine.gremlin.{GremlinQueryRunner, TypeAnnotationFieldReader}
import com.rrwright.quine.language.{DomainNodeSetSingleton, NoConstantsDomainNode, PickleReader, PickleScheme, Queryable, QuineId}
import com.rrwright.quine.language.EdgeDirections._
import com.rrwright.quine.language.BoopickleScheme._
//import com.rrwright.quine.language.JavaObjectSerializationScheme._


object AdmUuidProvider extends QuineIdProvider[AdmUUID] {

  private val stringPickler = implicitly[PickleScheme[String]]

  // Given a namespace, get the host index
  private val namespaceIdx: Map[String, HostIdx] =
    AdaptConfig.quineConfig.hosts.zipWithIndex.flatMap {
      case (AdaptConfig.QuineHost(_, _, namespaces), hostIdx) => namespaces.map(_ -> hostIdx)
    }.toMap

  println(s"AdmUuidProvider namespaceIdx:\n${namespaceIdx.mkString("    ","\n", "")}")


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




case class PpmNetflow(
  localAddress: Option[String],
  localPort: Option[Int],
  remoteAddress: Option[String],
  remotePort: Option[Int],
  provider: String
) extends NoConstantsDomainNode

case class PpmLocalAddress(address: String, localAddressAdm: <--[PpmNetflow]) extends NoConstantsDomainNode

case class PpmLocalPort(port: Int, localPortAdm: <--[PpmNetflow]) extends NoConstantsDomainNode

case class PpmRemoteAddress(address: String, remoteAddressAdm: <--[PpmNetflow]) extends NoConstantsDomainNode

case class PpmRemotePort(port: Int, remotePortAdm: <--[PpmNetflow]) extends NoConstantsDomainNode

case class CommunicatingNetflows(
  localAddress: Option[String],
  localPort: Option[Int],
  remoteAddress: Option[String],
  remotePort: Option[Int],
  provider: String,
  localAddressAdm: PpmRemoteAddress,
  localPortAdm: PpmRemotePort,
  remoteAddressAdm: PpmLocalAddress,
  remotePortAdm: PpmLocalPort
) extends NoConstantsDomainNode {
  def doesConverge: Boolean = remoteAddressAdm.localAddressAdm.target == remotePortAdm.localPortAdm.target
  def otherNetflow: Option[PpmNetflow] = if (doesConverge) Some(remoteAddressAdm.localAddressAdm.target) else None
}





case class PpmObservation(
  treeRootQid: QuineId,
  treeName: String,
  hostName: String,
  extractedValues: List[String],
  collectedUuids: Set[NamespacedUuidDetails],
  timestamps: Set[Long],
  sendNoveltiesFunc: (HostName, Novelty[Set[NamespacedUuidDetails]]) => Unit,
  observationCount: Int
)

class QuineDBActor(graphService: GraphService[AdmUUID], idx: Int) extends DBQueryProxyActor {
  val nf = NumberFormat.getInstance()
  val hostName: String = self.path.elements.last

  implicit val service = graphService

//  implicit val ec = service.system.dispatchers.lookup("quine.actor.node-dispatcher")

//  implicit val timeout = Timeout(21 seconds)
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
        "remotePort" -> "Option[Int]",
        "path" -> "String",
        "address" -> "String",
        "port" -> "Int"
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
  )(implicitly, Timeout(21.23456 seconds))


  implicit class FutureAckOnComplete(f: Future[_]) extends AnyRef {
    def ackOnComplete(ackTo: ActorRef, successF: => Unit = ()): Unit = f.onComplete{
      case Success(_) =>
        ackTo ! Ack
        if (shouldRecordDBWriteTimes) stopWork()
      case Failure(e) =>
//        e.printStackTrace()
        ackTo ! Ack
        if (shouldRecordDBWriteTimes) stopWork()
    }
  }


  def DBNodeableTx(cdms: Seq[DBNodeable[_]]): Try[Unit] = ???

  // TODO Make an async interface for this - the 'Await' is gross.
  def AdmTx(adms: Seq[Either[ADM,EdgeAdm2Adm]]): Try[Unit] = ???
//    Try(Await.result(  // TODO WHAT?!?  don't await!  ackOnComplete
//    Future.sequence(adms.map {
//      case Left(a: ADM) => writeAdm(a)
//      case Right(e: EdgeAdm2Adm) => writeAdmEdge(e)
//    }),
//    Duration.Inf
//  ))



  implicit val queryableEsoSubject: Queryable[ESOSubject] = cachedImplicit
  implicit val queryableEsoFileObject: Queryable[ESOFileObject] = cachedImplicit
  implicit val queryableEsoSrcSinkObject: Queryable[ESOSrcSinkObject] = cachedImplicit
  implicit val queryableEsoNetFlow: Queryable[ESONetFlowObject] = cachedImplicit
  implicit val queryableEsoFileInstance: Queryable[ESOFileInstance] = cachedImplicit
  implicit val queryableEsoSrcSnkInstance: Queryable[ESOSrcSnkInstance] = cachedImplicit
  implicit val queryableEsoNetworkInstance: Queryable[ESONetworkInstance] = cachedImplicit
  implicit val queryableChildProcess: Queryable[ChildProcess] = cachedImplicit
  implicit val admSubjectInstance: Queryable[AdmSubject] = cachedImplicit
  implicit val admEventInstance: Queryable[AdmEvent] = cachedImplicit
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

  implicit val ppmNetflowInstance: Queryable[PpmNetflow] = cachedImplicit
  implicit val ppmAddressInstance: Queryable[PpmLocalAddress] = cachedImplicit
  implicit val ppmPortInstance: Queryable[PpmLocalPort] = cachedImplicit
  implicit val communicatingNetflowInstance: Queryable[CommunicatingNetflows] = cachedImplicit



  def wrongFunc(x: Any): Unit = println("This is not the function you are looking for.")


  def writeAdm(a: ADM, timeout: Timeout): Future[Long] = {
    implicit val t = timeout
    val startNanos = System.nanoTime()
    (a match {
      case anAdm: AdmEvent =>
        anAdm.create(Some(anAdm.uuid)).map { x =>
          graphService.standingFetchWithBranch[ESOFileInstance](anAdm.uuid, Application.esoFileInstanceBranch, Application.sqidFile)(wrongFunc)
          graphService.standingFetchWithBranch[ESOSrcSnkInstance](anAdm.uuid, Application.esoSrcSinkInstanceBranch, Application.sqidSrcSnk)(wrongFunc)
          graphService.standingFetchWithBranch[ESONetworkInstance](anAdm.uuid, Application.esoNetworkInstanceBranch, Application.sqidNetwork)(wrongFunc)
          x
        }

      case anAdm: AdmSubject =>
        anAdm.create(Some(anAdm.uuid)).map { x =>
          graphService.standingFetchWithBranch[ChildProcess](anAdm.uuid, Application.esoChildProcessInstanceBranch, Application.sqidParentProcess)(wrongFunc)
          x
        }

      case anAdm: AdmNetFlowObject      =>
        anAdm.create(Some(anAdm.uuid)).map { x =>
//          graphService.standingFetchWithBranch[CommunicatingNetflows](anAdm.uuid, Application.esoCommunicatingNetflowsBranch, Application.sqidCommunicatingNetflows)(wrongFunc)
          x
        }

      case anAdm: AdmPrincipal          => anAdm.create(Some(anAdm.uuid))
      case anAdm: AdmFileObject         => anAdm.create(Some(anAdm.uuid))
      case anAdm: AdmPathNode           => anAdm.create(Some(anAdm.uuid))
      case anAdm: AdmPort               => anAdm.create(Some(anAdm.uuid))
      case anAdm: AdmAddress            => anAdm.create(Some(anAdm.uuid))
      case anAdm: AdmSrcSinkObject      => anAdm.create(Some(anAdm.uuid))
      case anAdm: AdmProvenanceTagNode  => anAdm.create(Some(anAdm.uuid))
      case anAdm: AdmHost               => anAdm.create(Some(anAdm.uuid))
      case anAdm: AdmSynthesized        => anAdm.create(Some(anAdm.uuid))
      case _                            => throw new Exception("Unexpected ADM")
    }).flatMap {
      case Success(s) => Future.successful(System.nanoTime() - startNanos)
      case Failure(f) => Future.failed(f)
    }
  }

  def writeAdmEdge(e: EdgeAdm2Adm, timeout: Timeout): Future[Long] = {
    val src = AdmUuidProvider.customIdToQid(e.src)
    val dest = AdmUuidProvider.customIdToQid(e.tgt)
    val startNanos = System.nanoTime()
    graphService.dumbOps.addEdge(src, dest, e.label)(timeout)
      .map(_ => System.nanoTime() - startNanos)
  }

  def FutureTx[T](body: => T)(implicit ec: ExecutionContext): Future[T] = Future(body)


  var nodeTimes =  // Not exactly correct because it is a set instead of a list, but close enough:
    new java.util.LinkedHashMap[Long, None.type](10000, 1F, true) {
      override def removeEldestEntry(eldest: java.util.Map.Entry[Long, None.type]) = this.size() >= 10000
    }

  var edgeTimes =  // Not exactly correct because it is a set instead of a list, but close enough:
    new java.util.LinkedHashMap[Long, None.type](10000, 1F, true) {
      override def removeEldestEntry(eldest: java.util.Map.Entry[Long, None.type]) = this.size() >= 10000
    }

  var obsTimes =  // Not exactly correct because it is a set instead of a list, but close enough:
    new java.util.LinkedHashMap[Long, None.type](10000, 1F, true) {
      override def removeEldestEntry(eldest: java.util.Map.Entry[Long, None.type]) = this.size() >= 10000
    }

  var nodeCount = new AtomicLong(0L)
  var edgeCount = new AtomicLong(0L)
  var obsCount = new AtomicLong(0L)
  var lastRecv: AtomicLong = new AtomicLong(0)
  var lastSent: AtomicLong = new AtomicLong(0)
  var timeWaiting: AtomicLong = new AtomicLong(0)
  var timeWorking: AtomicLong = new AtomicLong(0)

  val myselfRef = context.self
  val shouldRecordDBWriteTimes = true
  if (shouldRecordDBWriteTimes && idx >= 0) context.system.scheduler.schedule(30 seconds, 60 seconds) {
    myselfRef ! PrintStats
  }

  def recordResults() = {
    val oldNodeTimes = nodeTimes
    val oldEdgeTimes = edgeTimes
    val oldObsTimes = obsTimes
    nodeTimes =  // Not exactly correct because it is a set instead of a list, but close enough:
      new java.util.LinkedHashMap[Long, None.type](10000, 1F, true) {
        override def removeEldestEntry(eldest: java.util.Map.Entry[Long, None.type]) = this.size() >= 10000
      }
    edgeTimes =  // Not exactly correct because it is a set instead of a list, but close enough:
      new java.util.LinkedHashMap[Long, None.type](10000, 1F, true) {
        override def removeEldestEntry(eldest: java.util.Map.Entry[Long, None.type]) = this.size() >= 10000
      }
    obsTimes =  // Not exactly correct because it is a set instead of a list, but close enough:
      new java.util.LinkedHashMap[Long, None.type](10000, 1F, true) {
        override def removeEldestEntry(eldest: java.util.Map.Entry[Long, None.type]) = this.size() >= 10000
      }
    printResults(nodeCount.get(), edgeCount.get(), obsCount.get(), timeWaiting.get(), timeWorking.get(), lastSent.get(), lastRecv.get(), oldNodeTimes, oldEdgeTimes, oldObsTimes)
    nodeCount.set(0L)
    edgeCount.set(0L)
    obsCount.set(0L)
    nodeTimes.clear()
    edgeTimes.clear()
    obsTimes.clear()
    timeWaiting.set(0L)
    timeWorking.set(0L)
    lastRecv.set(0L)
    lastSent.set(0L)
  }

  def printResults(nodeCount: Long, edgeCount: Long, obsCount: Long, timeWaiting: Long, timeWorking: Long, lastSent: Long, lastRecv: Long, oldNodeTimes: java.util.LinkedHashMap[Long, None.type], oldEdgeTimes: java.util.LinkedHashMap[Long, None.type], oldObsTimes: java.util.LinkedHashMap[Long, None.type], shouldWriteToFile: Boolean = false) = Future {
    val nodeTimes = oldNodeTimes.asScala.keys
    val edgeTimes = oldEdgeTimes.asScala.keys
    val obsTimes = oldObsTimes.asScala.keys

    if (shouldWriteToFile) {
      val nodeWriter = new PrintWriter(new File(s"stats/$idx-nodes.csv"))
      nodeWriter.write(nodeTimes.mkString("", ",", "\n"))
      nodeWriter.close()

      val edgeWriter = new PrintWriter(new File(s"stats/$idx-edges.csv"))
      edgeWriter.write(edgeTimes.mkString("", ",", "\n"))
      edgeWriter.close()

      val obsWriter = new PrintWriter(new File(s"stats/$idx-obs.csv"))
      obsWriter.write(obsTimes.mkString("", ",", "\n"))
      obsWriter.close()
    }

    val (ntotal, ncount, nmax, nmin) = nodeTimes.foldLeft((0L, 0L, 0L, Long.MaxValue)) {
      case ((total, count, max, min), next) => (total + next, count + 1, if (next > max) next else max, if (next < min) next else min)
    }
    val (etotal, ecount, emax, emin) = edgeTimes.foldLeft((0L, 0L, 0L, Long.MaxValue)) {
      case ((total, count, max, min), next) => (total + next, count + 1, if (next > max) next else max, if (next < min) next else min)
    }
    val (ototal, ocount, omax, omin) = obsTimes.foldLeft((0L, 0L, 0L, Long.MaxValue)) {
      case ((total, count, max, min), next) => (total + next, count + 1, if (next > max) next else max, if (next < min) next else min)
    }

    println(
      s"""$hostName: Work/wait ratio: ${timeWorking.toDouble / timeWaiting}         Work: ${nf.format(timeWorking)}  Wait: ${nf.format(timeWaiting)}
         |           Processed ${nf.format(nodeCount)} nodes. Avg write time for ${nf.format(ncount)} nodes: ${nf.format((ntotal.toDouble / ncount).toLong)} nanoseconds.  Min: ${nf.format(nmin.toLong)}  Max: ${nf.format(nmax.toLong)}
         |           Processed ${nf.format(edgeCount)} edges. Avg write time for ${nf.format(ecount)} edges: ${nf.format((etotal.toDouble / ecount).toLong)} nanoseconds.  Min: ${nf.format(emin.toLong)}  Max: ${nf.format(emax.toLong)}
         |           Processed ${nf.format(obsCount) } obses. Avg write time for ${nf.format(ocount)} obses: ${nf.format((ototal.toDouble / ocount).toLong)} nanoseconds.  Min: ${nf.format(omin.toLong)}  Max: ${nf.format(omax.toLong)}""".stripMargin
    )
  }.recover{ case e => e.printStackTrace() }


//  TODO: hypothesis: there are lots of remote calls being made in the cadets data. but why?


  def startWork(): Unit = if (shouldRecordDBWriteTimes) {
    val now = System.nanoTime()
    lastSent.compareAndSet(0L, now)
    timeWaiting.addAndGet(now - lastSent.get())
    lastRecv.set(now)
  }

  def stopWork(): Unit = if (shouldRecordDBWriteTimes) {
    val now = System.nanoTime()
    lastRecv.compareAndSet(0L, now)
    timeWorking.addAndGet(now - lastRecv.get())
    lastSent.set(now)
  }


  override def receive = {

    case PrintStats => recordResults()

    // Run the query without specifying what the output type will be. This is the variant used by 'cmdline_query.py'
    case StringQuery(q, shouldParse) =>
      log.debug(s"Received string query: $q")
      println(s"Received string query: $q")
      sender() ! {
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
    case NodeQuery(q, shouldParse) =>
      log.debug(s"Received node query: $q")
      println(s"Received node query: $q")
      sender() ! {
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
    case EdgeQuery(q, shouldParse) =>
      log.debug(s"Received new edge query: $q")
      println(s"Received new edge query: $q")
      sender() ! {
        gremlin.queryEitherExpecting[com.rrwright.quine.gremlin.Edge](q).map { edgesEither =>
          edgesEither.fold(qge => Failure(throw qge), Success(_)).map { edges =>
            if (shouldParse) JsArray(edges.map(ApiJsonProtocol.quineEdgeToJson).toVector)
            else edges.toList.toStream
          }
        }
      }


    case Left(a: ADM) =>
      val s = sender()
      if (shouldRecordDBWriteTimes) startWork()
      writeAdm(a, Timeout(5 seconds)) //0.3 seconds))
        .map(t => if (shouldRecordDBWriteTimes) {
          Try(nodeTimes.put(t, None)) // Not a big deal if it fails sometimes
          nodeCount.incrementAndGet()
        } else t)
        .recoveryMessage("Writing NODE failed at ID: {} for ADM Node: {}", a.uuid, a)
        .ackOnComplete(s)

    case Right(e: EdgeAdm2Adm) =>
      val s = sender()
      if (shouldRecordDBWriteTimes) startWork()
      writeAdmEdge(e, Timeout(5 seconds)) //0.5 seconds))
        .map(t => if (shouldRecordDBWriteTimes) {
          Try(edgeTimes.put(t, None)) // Not a big deal if it fails sometimes
          edgeCount.incrementAndGet()
        } else t)
        .recoveryMessage("Writing EDGE failed at IDs: {} and: {} with label: {}", e.src, e.tgt, e.label)
        .ackOnComplete(s)

    case PpmObservation(treeRootQid, treeName, hostName, extractedValues, collectedUuids, timestamps, sendNoveltiesFunc, observationCount) =>
      val s = sender()
      var startTime = 0L
      if (shouldRecordDBWriteTimes) {
        startWork()
        startTime = System.nanoTime()
      }
      implicit val timeout = Timeout(10 seconds)  //2 seconds)
      graphService.observe(treeRootQid, treeName, hostName, extractedValues, collectedUuids, timestamps, sendNoveltiesFunc, observationCount)
        .map(u => if (shouldRecordDBWriteTimes) {
          val t = System.nanoTime() - startTime
          Try(obsTimes.put(t, None))
          obsCount.incrementAndGet()
        } else u)
        .recoveryMessage("Writing PPM OBS failed at hostname: {} with tree name: {} for extracted values: {} with count: {}", hostName, treeName, extractedValues, observationCount)
        .ackOnComplete(s)

    case InitMsg => sender() ! Ack

    case Ready => sender() ! Ack

    case CompleteMsg =>
      println(s"Data loading complete: $idx")
      sender() ! Ack
    
    case Application.KillMe =>
      println(s"Killing myself :(")
      sender() ! Ack
      context.stop(self)

    case msg => log.warning(s"Unknown message: $msg")

  }
}


case object PrintStats

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
    case InitMsg => (0 until count).foreach(_ => sender() ! Ack )
    case CompleteMsg => println("CompleteMsg at QuineRouter"); sender() ! Ack
    case x => router.route(WithSender(sender(), x), sender())
  }
}

