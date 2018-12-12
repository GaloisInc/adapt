package com.galois.adapt

import java.util.UUID
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import akka.util.Timeout
import com.galois.adapt.cdm17._
import com.rrwright.quine.language.EdgeDirections.{-->, <--}
import com.rrwright.quine.language._
import com.rrwright.quine.language._
import com.rrwright.quine.runtime.GraphService
import spray.json.{JsArray, JsObject, JsString, JsValue}
import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import shapeless._
import shapeless.syntax.singleton._
import com.rrwright.quine.language.JavaObjectSerializationScheme._
// import scala.pickling.{PickleFormat, PicklerUnpickler}
import com.galois.adapt.adm._
import scala.util.{Try, Success, Failure}
//import scala.pickling.shareNothing._
//import scala.pickling.static._        // Avoid run-time reflection


//case object ReadEventType extends NodeConstants( 'eventType ->> EVENT_READ )
//case class FileReadingEvent(predicateObject: FileObject) extends DomainNode {
//  val nodeConstants: AbstractNodeConstants = ReadEventType
//}
//case class ProcessReadsFile(pid: Int, subject: <--[FileReadingEvent]) extends NoConstantsDomainNode
case class FileEvent(eventType: EventType, predicateObject: FileObject) extends NoConstantsDomainNode
case class ProcessFileActivity(pid: Int, subject: <--[FileEvent]) extends NoConstantsDomainNode


class QuineDBActor(graphService: GraphService, idx: Int) extends DBQueryProxyActor {

  implicit val service = graphService
  val graph: org.apache.tinkerpop.gremlin.structure.Graph = ???

//  log.info(s"QuineDB actor init")

  implicit class FutureAckOnComplete(f: Future[_]) extends AnyRef {
    def ackOnComplete(ackTo: ActorRef, successF: => Unit = ()): Unit = f.onComplete{
      case Success(_) => ackTo ! Ack
      case Failure(ex) => ex.printStackTrace(); ackTo ! Ack
    }
  }

  import com.rrwright.quine.language._

  implicit val timeout = Timeout(21 seconds)

  override def DBNodeableTx(cdms: Seq[DBNodeable[_]]): Try[Unit] = ???
  def AdmTx(adms: Seq[Either[ADM,EdgeAdm2Adm]]): Try[Unit] = Try(Await.result(
    Future.sequence(adms.map {
     //   case Left(anAdm: ADM) => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
        case Left(anAdm: AdmEvent) => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
        case Left(anAdm: AdmSubject) => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
        case Left(anAdm: AdmPrincipal) => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
        case Left(anAdm: AdmFileObject) => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
        case Left(anAdm: AdmNetFlowObject) => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
        case Left(anAdm: AdmPathNode) => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
        case Left(anAdm: AdmPort) => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
        case Left(anAdm: AdmAddress) => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
        case Left(anAdm: AdmSrcSinkObject) => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
        case Left(anAdm: AdmProvenanceTagNode) => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
        case Left(anAdm: AdmHost) => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
        case Left(anAdm: AdmSynthesized) => DomainNodeSetSingleton(anAdm).create(Some(anAdm.uuid.uuid))
        case Right(EdgeAdm2Adm(src, label, tgt)) => graphService.dumbOps.addEdge(src, tgt, label)
    }),
    Duration.Inf
  )).map(_ => ())

  def FutureTx[T](body: => T)(implicit ec: ExecutionContext): Future[T] = Future(body)

  override def receive = {
/*
    case cdm: Event =>
      val s = sender()
      val now = System.currentTimeMillis
      cdm.create(Some(cdm.getUuid)).onComplete {
        case Failure(ex) => ex.printStackTrace(); s ! Ack
        case Success(_) =>

          // Mutations over time:
          val doneF = cdm.eventType match {
            case EVENT_CLOSE =>
              // FileObject: Maybe delete events with same subject and preceding sequence numbers until you get to an EVENT_OPEN
              // SrcSinkObject: Maybe delete events with same subject and preceding sequence numbers (there is no EVENT_OPEN) Should delete the SrcSink?
              // NetFlowObject: Delete Node and events (with same subject?) to the the NetFlow.  (Close is essentially like UNLINK.)

//              val objectUuid: QuineId = cdm.predicateObject.get.target
//              val b = branchOf[FileObject]()
//              val fileObjectBranch: DomainGraphBranch = b.identifyRoot(objectUuid).refine(
//                'predicateObject <-- branchOf[Event]().refine( 'subjectUuid --> cdm.subjectUuid.target )
//              )
//              val f = graph.deleteFromBranch(fileObjectBranch).flatMap[Unit]{ x =>
//                val fileObjectIds = x.foldLeft(Set.empty[QuineId])((a,b) => a ++ b.allIds)
//                if (fileObjectIds.nonEmpty) Future.successful( log.info(s"Deleted ${fileObjectIds.size} nodes from a FileObject's EVENT_CLOSE: $objectUuid") )
//                else {
//                  val srcSinkBranch: DomainGraphBranch = branchOf[SrcSinkObject]().identifyRoot(objectUuid).refine(
//                    'predicateObject <-- branchOf[Event]().refine('subjectUuid --> cdm.subjectUuid.target)
//                  )
//                  graph.deleteFromBranch(srcSinkBranch).flatMap[Unit]{ x =>
//                    val srcSinkIds = x.foldLeft(Set.empty[QuineId])((a,b) => a ++ b.allIds)
//                    if (srcSinkIds.nonEmpty) Future.successful( log.info(s"Deleted ${srcSinkIds.size} nodes from a SrcSinkObject's EVENT_CLOSE: $objectUuid") )
//                    else {
//                      val netFlowBranch: DomainGraphBranch = branchOf[NetFlowObject]().identifyRoot(objectUuid).refine(
//                        'predicateObject <-- branchOf[Event]().refine('subjectUuid --> cdm.subjectUuid.target)
//                      )
//                      graph.deleteFromBranch(netFlowBranch).map[Unit] { x =>
//                        val netFlowIds = x.foldLeft(Set.empty[QuineId])((a, b) => a ++ b.allIds)
//                        if (netFlowIds.nonEmpty) log.info(s"Deleted ${netFlowIds.size} nodes from a NetFlowObject's EVENT_CLOSE: $objectUuid")
//                        else log.warning(s"Zero deleted from an EVENT_CLOSE after trying all three kinds")
//                      }
//                    }
//                  }
//                }
//              }

//              val branch = DomainGraphBranch.empty.identifyRoot(objectUuid).refine(
//                'predicateObject <-- branchOf[Event]().refine('subjectUuid --> cdm.subjectUuid.target)
//              )
//              graph.deleteFromBranch(branch).map { x =>
//                val ids = x.foldLeft(Set.empty[QuineId])((a, b) => a ++ b.allIds)
//                log.info(s"Deleted ${ids.size} nodes from an EVENT_CLOSE: $objectUuid")
//              }
              Future.successful(())

//            case EVENT_UNLINK =>
//              // A file is deleted. The file node and all its events should be deleted.
//              val objectId: QuineId = cdm.predicateObject.get.target
//              val branch = branchOf[FileObject]().identifyRoot(objectId).refine( 'predicateObject <-- branchOf[Event]() )
//              graph.deleteFromBranch(branch).flatMap { x =>
//                val ids = x.foldLeft(Set.empty[QuineId])((a,b) => a ++ b.allIds)
//                log.info(s"Deleted ${ids.size} nodes from EVENT_UNLINK after time: $now")
////                if (ids.size == 0) log.warning(s"Zero deleted from 'predicateObject on EVENT_UNLINK ${cdm.getUuid}")
//                graph.dumbOps.deleteNode(objectId)
//              }
//
//            case EVENT_EXIT =>
//              // A process exits. The process node and all its events should be deleted.
//              val objectId: QuineId = cdm.predicateObject.get.target
//              val branch = branchOf[Subject]().identifyRoot(objectId).refine( 'subjectUuid <-- branchOf[Event]() )
//              graph.deleteFromBranch(branch).map { x =>
//                val size = x.foldLeft(0)((a,b) => a + b.allIds.size)
//                if (size > 0) log.info(s"Deleted $size nodes from EVENT_EXIT 'subjectUuid after time: $now")
////                  if (size == 0) log.warning(s"Zero deleted from 'subjectUuid on EVENT_EXIT ${cdm.getUuid}")
//              }

  //        case EVENT_RENAME => // A file is renamed. Nothing to do here. There is only one predicate edge in Cadets data.
            case _ => Future.successful(())
          }
        doneF.ackOnComplete(s)
      }
//      Event types in the Cadets Bovia CDM data:
//        [
//          "EVENT_READ",
//          "EVENT_SENDTO",
//          "EVENT_RECVFROM",
//          "EVENT_OPEN",
//          "EVENT_MMAP",
//          "EVENT_CLOSE",
//          "EVENT_LSEEK",
//          "EVENT_FCNTL",
//          "EVENT_WRITE",
//          "EVENT_EXIT",
//          "EVENT_RENAME",
//          "EVENT_SIGNAL",
//          "EVENT_FORK",
//          "EVENT_CREATE_OBJECT",
//          "EVENT_OTHER",
//          "EVENT_MPROTECT",
//          "EVENT_MODIFY_PROCESS",
//          "EVENT_CHANGE_PRINCIPAL",
//          "EVENT_CONNECT",
//          "EVENT_ACCEPT",
//          "EVENT_LOGIN",
//          "EVENT_EXECUTE",
//          "EVENT_UNLINK",
//          "EVENT_MODIFY_FILE_ATTRIBUTES",
//          "EVENT_SENDMSG",
//          "EVENT_RECVMSG",
//          "EVENT_LINK",
//          "EVENT_BIND"
//        ]

    case cdm: Principal =>
      val s = sender()
      cdm.create(Some(cdm.getUuid)).ackOnComplete(s)

    case cdm: FileObject =>
      val s = sender()
      cdm.create(Some(cdm.getUuid)).ackOnComplete(s)

    case cdm: Subject =>
      val s = sender()
      cdm.create(Some(cdm.getUuid)).ackOnComplete(s)

    case cdm: NetFlowObject =>
      val s = sender()
      cdm.create(Some(cdm.getUuid)).ackOnComplete(s)

    case cdm: MemoryObject =>
      val s = sender()
      cdm.create(Some(cdm.getUuid)).ackOnComplete(s)

    case cdm: ProvenanceTagNode =>
      val s = sender()
      cdm.create(Some(cdm.getUuid)).ackOnComplete(s)

    case cdm: RegistryKeyObject =>
      val s = sender()
      cdm.create(Some(cdm.getUuid)).ackOnComplete(s)

    case cdm: SrcSinkObject =>
      val s = sender()
      cdm.create(Some(cdm.getUuid)).ackOnComplete(s)

    case cdm: TimeMarker =>  // Don't care about these.
      sender() ! Ack

    case cdm: CDM17 =>
      log.info(s"Unhandled CDM: $cdm")
      sender() ! Ack
*/

    case InitMsg => sender() ! Ack

    case Ready => sender() ! Ack

    case CompleteMsg =>
      println(s"Data loading complete: $idx")
      sender() ! Ack


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
) extends NoConstantsDomainNode



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
    case w: CDM17 =>
      router.route(w, sender())
    case msg @ Terminated(a) =>
      log.warning(s"Received $msg")
      router = router.removeRoutee(a)
      val r = context.actorOf(Props(classOf[QuineDBActor], graph, nextIdx))
      nextIdx += 1
      context watch r
      router = router.addRoutee(r)
    case x =>
      router.route(x, sender())
  }
}

