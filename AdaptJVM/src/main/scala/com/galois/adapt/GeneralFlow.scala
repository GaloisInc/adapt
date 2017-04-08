package com.galois.adapt

import java.util.UUID

import akka.NotUsed
import akka.actor.{Actor, ActorSystem, Props}
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.galois.adapt.cdm17._

import scala.collection.mutable.{Map => MutableMap}
import scala.collection.immutable
import scala.concurrent.duration._
import scala.util.Try
import GraphDSL.Implicits._
import FanInShape._


object GeneralFlow extends App {

  implicit val system = ActorSystem("test")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val (ta1, cdmData) = CDM17.readData("/Users/ryan/Desktop/ta1-cadets-cdm17-2.bin", None).get
  val cdmSource: Source[Try[CDM17], NotUsed] = Source.fromIterator(() => cdmData)

  val printSink = Sink.foreach(println)
  val actorPrintSink = Sink.actorRef(system.actorOf(Props(classOf[TestSinkActor])), DoneMessage)

  val g = RunnableGraph.fromGraph(GraphDSL.create(){ implicit buildBlock =>
    val bcast = buildBlock.add(Broadcast[CDM17](3))
    val eventBroadcast = buildBlock.add(Broadcast[Event](2))

    val processWrites = buildBlock.add(ProcessWrites())
    val fileWrites = buildBlock.add(FileWrites())
    val processWritesFile = buildBlock.add(ProcessWritesFile())

    val processFilter = Flow[CDM17].filter(s => s.isInstanceOf[Subject] && s.asInstanceOf[Subject].subjectType == SUBJECT_PROCESS).map(_.asInstanceOf[Subject])
    def eventFilter(eType: EventType) = Flow[CDM17].filter(e => e.isInstanceOf[Event] && e.asInstanceOf[Event].eventType == eType).map(_.asInstanceOf[Event])
    val fileFilter = Flow[CDM17].filter(f => f.isInstanceOf[FileObject] && f.asInstanceOf[FileObject].fileObjectType == FILE_OBJECT_FILE).map(_.asInstanceOf[FileObject])

    cdmSource.map(_.get) ~> bcast.in
    bcast.out(0) ~> processFilter ~> processWrites.in0
    bcast.out(1) ~> fileFilter ~> fileWrites.in0
    bcast.out(2) ~> eventFilter(EVENT_WRITE) ~> eventBroadcast.in
    eventBroadcast.out(0) ~> processWrites.in1
    eventBroadcast.out(1) ~> fileWrites.in1
    processWrites.out ~> processWritesFile.in0
    fileWrites.out ~> processWritesFile.in1
    processWritesFile.out ~> actorPrintSink

    ClosedShape
  })


//  cdmSource.map(_.get).runWith(actorPrintSink)

//  val e = cdmSource.runForeach(println)
//  val f = cdmSource.runFold(0){(a,b) => println(s"$a  $b"); a + 1}
//  f.map(i => println(s"count: $i"))

  g.run()

  println("should have run")
}

case object DoneMessage

class TestSinkActor extends Actor {
  def receive = {
    case DoneMessage => println("DONE!!!")
    case x => println(x)
  }
}




case class ProcessWrites() extends GraphStage[FanInShape2[Subject, Event, (Subject,Event)]] {
  val shape = new FanInShape2[Subject, Event, (Subject,Event)]("ProcessWrites")
  def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    type ProcessUUID = UUID
    val processes = MutableMap.empty[ProcessUUID, Subject]
    val unmatchedWrites = MutableMap.empty[ProcessUUID, Set[Event]]
    var sendingBuffer = List.empty[(Subject,Event)]

    setHandler(shape.in0, new InHandler {
      def onPush() = {
        val p = grab(shape.in0)
        processes(p.uuid) = p
        unmatchedWrites.get(p.uuid).fold(
          pull(shape.in0)
        ) { writes =>
          unmatchedWrites -= p.uuid
          val wList = writes.toList
          wList.headOption foreach { w =>
            push(shape.out, p -> w)
          }
          if (wList.size > 1) sendingBuffer = sendingBuffer ++ wList.tail.map(p -> _)
        }
      }
    })

    setHandler(shape.in1, new InHandler {
      def onPush() = {
        val e = grab(shape.in1)
        val processUuid = e.subject
        processes.get(processUuid).fold {
          unmatchedWrites(processUuid) = unmatchedWrites.getOrElse(processUuid, Set.empty[Event]) + e
          pull(shape.in1)
        } { p =>
          push(shape.out, p -> e)
        }
      }
    })

    setHandler(shape.out, new OutHandler {
      def onPull() = {
        if (sendingBuffer.nonEmpty) {
          push(shape.out, sendingBuffer.head)
          sendingBuffer = sendingBuffer.drop(1)
        } else {
          if ( ! hasBeenPulled(shape.in0)) pull(shape.in0)
          if ( ! hasBeenPulled(shape.in1)) pull(shape.in1)
        }
      }
    })
  }
}



case class FileWrites() extends GraphStage[FanInShape2[FileObject, Event, (FileObject,Event)]] {
  val shape = new FanInShape2[FileObject, Event, (FileObject,Event)]("FileWrites")

  def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    type FileUUID = UUID
    val files = MutableMap.empty[FileUUID, FileObject]
    val unmatchedWrites = MutableMap.empty[FileUUID, Set[Event]]
    var sendingBuffer = List.empty[(FileObject, Event)]

    setHandler(shape.in0, new InHandler {
      def onPush() = {
        val f = grab(shape.in0)
        files(f.uuid) = f
        unmatchedWrites.get(f.uuid).fold(
          pull(shape.in0)
        ) { writes =>
          unmatchedWrites -= f.uuid
          val wList = writes.toList
          wList.headOption foreach { w =>
            push(shape.out, f -> w)
          }
          if (wList.size > 1) sendingBuffer = sendingBuffer ++ wList.tail.map(f -> _)
        }
      }
    })

    setHandler(shape.in1, new InHandler {
      def onPush() = {
        val e = grab(shape.in1)
        val fileUuids = List(e.predicateObject, e.predicateObject2).flatten
        fileUuids foreach { fu =>
          files.get(fu).fold {
            unmatchedWrites(fu) = unmatchedWrites.getOrElse(fu, Set.empty[Event]) + e
            pull(shape.in1)
          } { file =>
            push(shape.out, file -> e)
          }
        }
      }
    })

    setHandler(shape.out, new OutHandler {
      def onPull() = {
        if (sendingBuffer.nonEmpty) {
          push(shape.out, sendingBuffer.head)
          sendingBuffer = sendingBuffer.drop(1)
        } else {
          if ( ! hasBeenPulled(shape.in0)) pull(shape.in0)
          if ( ! hasBeenPulled(shape.in1)) pull(shape.in1)
        }
      }
    })
  }
}




case class ProcessWritesFile() extends GraphStage[FanInShape2[(Subject, Event), (FileObject, Event), (Subject, Event, FileObject)]] {
  val shape = new FanInShape2[(Subject, Event), (FileObject, Event), (Subject, Event, FileObject)]("ProcessWritesFile")
  def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {

    //    val unmatchedProcesses = MutableMap.empty[Subject,Set[Event]]
    //    val unmatchedFiles = MutableMap.empty[FileObject, Set[Event]]
    val unmatchesProcesses = MutableMap.empty[Event, Subject]
    val unmatchedFiles = MutableMap.empty[Event, FileObject]
    //    var sendingBuffer = List.empty[(Subject, Event, FileObject)]

    setHandler(shape.in0, new InHandler {
      def onPush() = {
        val (p, e) = grab(shape.in0)
        unmatchedFiles.get(e) match {
          case None =>
            unmatchesProcesses(e) = p
            pull(shape.in0)
          case Some(file) =>
            push(shape.out, (p, e, file))
          //            unmatchedFiles -= e  // Should this hold files forever? Yes, probably so.
        }
      }
    })

    setHandler(shape.in1, new InHandler {
      def onPush() = {
        val (f, e) = grab(shape.in1)
        unmatchesProcesses.get(e) match {
          case None =>
            unmatchedFiles(e) = f
            pull(shape.in1)
          case Some(process) =>
            push(shape.out, (process, e, f))
        }
      }
    })

    setHandler(shape.out, new OutHandler {
      def onPull() = {
        if ( ! hasBeenPulled(shape.in0)) pull(shape.in0)
        if ( ! hasBeenPulled(shape.in1)) pull(shape.in1)
      }
    })
  }
}




//case class JoinerShape[L, R](leftIn: Inlet[L], rightIn: Inlet[R], joinedOut: Outlet[(L,R)]) extends Shape {
//  def inlets = leftIn :: rightIn :: Nil
//  def outlets = joinedOut :: Nil
//  def deepCopy() = JoinerShape(leftIn.carbonCopy(), rightIn.carbonCopy(), joinedOut.carbonCopy())
//  def copyFromPorts(inlets: immutable.Seq[Inlet[_]], outlets: immutable.Seq[Outlet[_]]) = {
//    assert(inlets.size == this.inlets.size)
//    assert(outlets.size == this.outlets.size)
//    JoinerShape(inlets(0).as[L], inlets(1).as[R], outlets(0).as[(L,R)])
//  }
//}
//
//case class Joiner[L, R](joinFunc: (L, R) => Boolean, cleanFunc: () => Unit) extends GraphStage[JoinerShape[L,R]] {
//  val leftIn = Inlet[L]("leftIn")
//  val rightIn = Inlet[R]("rightIn")
//  val joinedOut = Outlet[(L,R)]("joinedOut")
//  def shape = JoinerShape(leftIn, rightIn, joinedOut)
//  def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
//
//    setHandler(leftIn, new InHandler {
//      def onPush() = {
//
//      }
//    })
//  }
//}




//case class Pairer() extends GraphStage[FlowShape[CDM17, (CDM17, CDM17)]] {
//  val in = Inlet[CDM17]("Pairer.in")
//  val out = Outlet[(CDM17, CDM17)]("Pairer.out")
//  def shape = FlowShape.of(in, out)
//
//  def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
//    var internalState: Option[CDM17] = None
//
//    setHandler(in, new InHandler {
//      def onPush() = {
//        if (internalState.isEmpty) {
//          internalState = Some(grab(in))
//          pull(in)
//        } else {
//          push(out, (internalState.get, grab(in)))
//          internalState = None
//        }
//      }
//    })
//
//    setHandler(out, new OutHandler {
//      def onPull() = pull(in)
//    })
//  }
//}


