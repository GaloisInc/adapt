package com.galois.adapt

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import akka.util.Timeout
import com.galois.adapt.cdm17._
import com.rrwright.quine.language.EdgeDirections.{-->, <--}
import com.rrwright.quine.language._
import com.rrwright.quine.runtime.GraphService
import spray.json.{JsArray, JsObject, JsString, JsValue}
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import scala.pickling.{PickleFormat, PicklerUnpickler}
import scala.pickling.shareNothing._
import shapeless._
import shapeless.syntax.singleton._
//import scala.pickling.static._        // Avoid run-time reflection


//case object ReadEventType extends NodeConstants( 'eventType ->> EVENT_READ )
//case class FileReadingEvent(predicateObject: FileObject) extends DomainNode {
//  val nodeConstants: AbstractNodeConstants = ReadEventType
//}
//case class ProcessReadsFile(pid: Int, subject: <--[FileReadingEvent]) extends NoConstantsDomainNode
case class FileEvent(eventType: EventType, predicateObject: FileObject) extends NoConstantsDomainNode
case class ProcessFileActivity(pid: Int, subject: <--[FileEvent]) extends NoConstantsDomainNode


class QuineDBActor(graph: GraphService) extends Actor with ActorLogging {

  implicit val implicitGraph = graph //GraphService(context.system, uiPort = 9090)(EmptyPersistor)
  implicit val ec = context.dispatcher

//  log.info(s"QuineDB actor init")

  implicit class FutureAckOnComplete(f: Future[_]) extends AnyRef {
    def ackOnComplete(ackTo: ActorRef, successF: => Unit = ()): Unit = f.onComplete{
      case Success(_) => ackTo ! Ack
      case Failure(ex) => ex.printStackTrace(); ackTo ! Ack
    }
  }

  import scala.pickling.Defaults._
  import com.rrwright.quine.runtime.runtimePickleFormat
  import CDM17Implicits._

  implicit val timeout = Timeout(21 seconds)


//  case class EventToObject(eventType: EventType, timestampNanos: Long, predicateObject: -->[QuineId]) extends NoConstantsDomainNode
//  case class EventToNetFlow(eventType: EventType, timestampNanos: Long, predicateObject: NetFlowObject) extends NoConstantsDomainNode
//  case class EventToObjectWithSubject(eventType: EventType, timestampNanos: Long, predicateObject: -->[QuineId], subject: -->[QuineId]) extends NoConstantsDomainNode
//


  override def receive = {

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


    case InitMsg => sender() ! Ack

    case Ready => sender() ! Ack

    case CompleteMsg =>
      println("Data loading complete.")
      sender() ! Ack
//      graph.printRandomContextSentences(100, 10, 100, Timeout(1001 seconds))



    case NodeQuery(queryString, shouldReturnJson) =>
      def go(): Future[Try[JsValue]] = {
        val idsOpt = if (queryString.nonEmpty) Try(queryString.split(",").map(s => UUID.fromString(s.trim)).toSet).toOption else None
        val uiDataF = graph.getUiData(idsOpt, None, false)
        uiDataF.map{uiData => Try {
          JsArray( uiData._1.map{ datum =>
            val uiNode = UINode(datum.id.toString, datum.label, datum.title)
            val jso = ApiJsonProtocol.uiNodeFormat.write(uiNode).asJsObject
            val props = datum.properties.map{ case (k,p) => k.name -> JsString(unpickleToString(p))}
            jso.copy(jso.fields + ("properties" -> JsObject(props)) )
//            datum.properties.mapValues(unpickleToString)
//            com.rrwright.quine.runtime.UiJsonFormat.nodeFormat.write(datum)
          }.toVector)
        } }
      }
      sender() ! go()

    case EdgeQuery(queryString, shouldReturnJson) =>
      def go(): Future[Try[JsValue]] = {
        val idsOpt = if (queryString.nonEmpty) Try(queryString.split(",").map(s => UUID.fromString(s.trim)).toSet).toOption else None
        val uiDataF = graph.getUiData(idsOpt, None, false)
        uiDataF.map{uiData => Try {
          JsArray( uiData._2.map{ edge =>
//            val uiEdge = UIEdge(edge.from.toString, edge.to.toString, edge.label.name)
//            ApiJsonProtocol.uiEdgeFormat.write(uiEdge)
            com.rrwright.quine.runtime.UiJsonFormat.edgeFormat.write(edge)
          }.toVector)
        } }
      }
      sender() ! go()


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
    val routees = Vector.fill(count) {
      val r = context.actorOf(Props(classOf[QuineDBActor], graph))
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case w: CDM17 =>
      router.route(w, sender())
    case msg @ Terminated(a) =>
      log.warning(s"Received $msg")
      router = router.removeRoutee(a)
      val r = context.actorOf(Props[QuineDBActor])
      context watch r
      router = router.addRoutee(r)
    case x =>
      router.route(x, sender())
  }
}



object CDM17Implicits {
  import scala.pickling.Defaults._

  implicit val a = PicklerUnpickler.generate[Option[Map[String, String]]]
  implicit val b = PicklerUnpickler.generate[Option[String]]
  implicit val c = PicklerUnpickler.generate[AbstractObject]

  implicit val d = PicklerUnpickler.generate[PrincipalType]
  implicit val e = PicklerUnpickler.generate[List[String]]  // canNOT also have this in scope: scala.pickling.Defaults.stringPickler

  implicit val f = PicklerUnpickler.generate[Option[Int]]
  implicit val g = PicklerUnpickler.generate[Option[Long]]
  implicit val h = PicklerUnpickler.generate[FileObjectType]
  implicit val i = PicklerUnpickler.generate[CryptographicHash]
  implicit val j = PicklerUnpickler.generate[Option[Seq[CryptographicHash]]]

  implicit val k = PicklerUnpickler.generate[EventType]
  implicit val l = PicklerUnpickler.generate[Option[List[Value]]]   // Scala pickling doesn't like Option[Seq[Value]]

  implicit val m = PicklerUnpickler.generate[SubjectType]
  implicit val n = PicklerUnpickler.generate[Option[PrivilegeLevel]]
  implicit val o = PicklerUnpickler.generate[Option[List[String]]]
  implicit val p = PicklerUnpickler.generate[Option[UUID]]

  implicit val q = PicklerUnpickler.generate[Option[TagOpCode]]
  implicit val r = PicklerUnpickler.generate[Option[List[UUID]]]
  implicit val t = PicklerUnpickler.generate[Option[IntegrityTag]]
  implicit val u = PicklerUnpickler.generate[Option[ConfidentialityTag]]

  implicit val v = PicklerUnpickler.generate[Option[Value]]
  implicit val w = PicklerUnpickler.generate[SrcSinkType]

//  implicit val aa = PicklerUnpickler.generate[EVENT_READ.type]

  private def unpicklePrimitiveToString(pickle: QuinePickleWrapper)(implicit format: PickleFormat): Try[String] = Try {
    Try( pickle.thisPickle.unpickle[String] ).getOrElse(
      Try( pickle.thisPickle.unpickle[Long] ).getOrElse(
        Try( pickle.thisPickle.unpickle[Int] ).get //OrElse(
//          Try( pickle.thisPickle.unpickle[Boolean] ).getOrElse(
//            Try( pickle.thisPickle.unpickle[Float] ).getOrElse(
//              Try( pickle.thisPickle.unpickle[Double] ).getOrElse(
//                Try( pickle.thisPickle.unpickle[Char] ).getOrElse(
//                  Try( pickle.thisPickle.unpickle[Short] ).getOrElse(
//                    Try( pickle.thisPickle.unpickle[Byte] ).getOrElse(
//                      Try( pickle.thisPickle.unpickle[Unit] ).getOrElse(
//                        Try( pickle.thisPickle.unpickle[Null] ).get
//                      )
//                    )
//                  )
//                )
//              )
//            )
//          )
//        )
      )
    )
  }.map(_.toString)

  def unpickleToString(pickle: QuinePickleWrapper)(implicit format: PickleFormat): String = {
//    Try( pickle.thisPickle.unpickle[AbstractObject] ).getOrElse(
    unpicklePrimitiveToString(pickle).getOrElse(
      Try( pickle.thisPickle.unpickle[Option[Map[String, String]]] ).getOrElse(
        Try( pickle.thisPickle.unpickle[Option[String]] ).getOrElse(
          Try( pickle.thisPickle.unpickle[PrincipalType] ).getOrElse(
            Try( pickle.thisPickle.unpickle[List[String]] ).getOrElse(
              Try( pickle.thisPickle.unpickle[Option[Int]] ).getOrElse(
                Try( pickle.thisPickle.unpickle[Option[Long]] ).getOrElse(
                  Try( pickle.thisPickle.unpickle[FileObjectType] ).getOrElse(
//                        Try( pickle.thisPickle.unpickle[CryptographicHash] ).getOrElse(
//                          Try( pickle.thisPickle.unpickle[Option[Seq[CryptographicHash]]] ).getOrElse(
                    Try( pickle.thisPickle.unpickle[EventType] ).getOrElse(
                      Try( pickle.thisPickle.unpickle[Option[List[Value]]] ).getOrElse(
                        Try( pickle.thisPickle.unpickle[SubjectType] ).getOrElse(
                          Try( pickle.thisPickle.unpickle[Option[PrivilegeLevel]] ).getOrElse(
//                                    Try( pickle.thisPickle.unpickle[Option[List[String]]] ).getOrElse(
                            Try( pickle.thisPickle.unpickle[Option[UUID]] ).getOrElse(
                              Try( pickle.thisPickle.unpickle[UUID] ).getOrElse(
//                                        Try( pickle.thisPickle.unpickle[Option[TagOpCode]] ).getOrElse(
//                                          Try( pickle.thisPickle.unpickle[Option[List[UUID]]] ).getOrElse(
//                                            Try( pickle.thisPickle.unpickle[Option[IntegrityTag]] ).getOrElse(
//                                              Try( pickle.thisPickle.unpickle[Option[ConfidentialityTag]] ).getOrElse(
//                                                Try( pickle.thisPickle.unpickle[Option[Value]] ).getOrElse(
                                Try( pickle.thisPickle.unpickle[SrcSinkType] ).getOrElse(
                                  "{--unknown_serialized_type--}"
                                )
//                                                )
//                                              )
//                                            )
//                                          )
//                                        )
                              )
//                                    )
                            )
                          )
                        )
                      )
//                          )
//                        )
                    )
                  )
                )
              )
            )
          )
        )
      )
    )
//    )
  }.toString
}