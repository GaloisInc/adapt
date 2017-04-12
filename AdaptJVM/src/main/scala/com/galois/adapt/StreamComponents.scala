package com.galois.adapt

import java.util.UUID
import akka.actor.Actor
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.galois.adapt.cdm17._
import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
import GraphDSL.Implicits._

import scala.concurrent.duration._

case class SubjectEventCount(
  subjectUuid: UUID,
  filesExecuted: Int,
  netflowsConnected: Int,
  eventCounts: Map[EventType, Int]
)

// TODO: put this somewhere else
case class AdaptProcessingInstruction(id: Long) extends CDM17

object Streams {

  def processWritesFile(source: Source[CDM17,_], sink: Sink[Any,_]) = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit graph =>
      val bcast = graph.add(Broadcast[CDM17](3))
      val eventBroadcast = graph.add(Broadcast[Event](2))

      val processFilter = Flow[CDM17].filter(s => s.isInstanceOf[Subject] && s.asInstanceOf[Subject].subjectType == SUBJECT_PROCESS).map(_.asInstanceOf[Subject])
      def eventFilter(eType: EventType) = Flow[CDM17].filter(e => e.isInstanceOf[Event] && e.asInstanceOf[Event].eventType == eType).map(_.asInstanceOf[Event])
      val fileFilter = Flow[CDM17].filter(f => f.isInstanceOf[FileObject] && f.asInstanceOf[FileObject].fileObjectType == FILE_OBJECT_FILE).map(_.asInstanceOf[FileObject])

      val processEvents = graph.add(ProcessEvents())
      val fileEvents = graph.add(FileWrites())
      val processEventsFile = graph.add(ProcessWritesFile())

      source ~> bcast.in;                                   eventBroadcast.out(0) ~> processEvents.in1
                bcast.out(0) ~> processFilter ~>                                     processEvents.in0
                bcast.out(1) ~> eventFilter(EVENT_WRITE) ~> eventBroadcast.in;       processEvents.out ~> processEventsFile.in0
                bcast.out(2) ~> fileFilter ~>                                        fileEvents.in0
                                                            eventBroadcast.out(1) ~> fileEvents.in1
                                                                                     fileEvents.out  ~>   processEventsFile.in1
                                                                                                          processEventsFile.out ~> sink
      ClosedShape
    }
  )


  def processCheckOpenGraph(source: Source[CDM17,_], sink: Sink[Any,_]) = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit graph =>
      source.filter(cdm =>
        (cdm.isInstanceOf[Subject] && cdm.asInstanceOf[Subject].subjectType == SUBJECT_PROCESS) ||
        (cdm.isInstanceOf[Event] && cdm.asInstanceOf[Event].eventType == EVENT_OPEN) ||
        (cdm.isInstanceOf[Event] && cdm.asInstanceOf[Event].eventType == EVENT_CHECK_FILE_ATTRIBUTES)
      ) ~> sink
      ClosedShape
    }
  )

  def processEventCounter(source: Source[CDM17,_], sink: Sink[Any,_]) = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit graph =>

      source
        .collect{ case cdm: Event => cdm }
        .groupBy(1000000, _.subject)   // TODO: pull this number out to somewhere else
        .fold(MutableMap.empty[EventType,Int]) { (a, b) =>
        a += (b.eventType -> (a.getOrElse(b.eventType, 0) + 1)); a
      }
      .mergeSubstreams ~> sink

      ClosedShape
    }
  )


  def processUsedNetFlow(source: Source[CDM17,_], sink: Sink[Any,_]) = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit graph =>
      val bcast = graph.add(Broadcast[CDM17](2))
      val processUsedNetFlow = graph.add(ProcessUsedNetFlow())

      val eventTypesWeCareAbout = List(EVENT_ACCEPT, EVENT_CONNECT, EVENT_OPEN, EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG, EVENT_SENDTO, EVENT_SENDMSG, EVENT_WRITE)

      source ~> bcast.in
      bcast.out(0).collect{ case n: NetFlowObject => n} ~> processUsedNetFlow.in0
      bcast.out(1).collect{ case e: Event if eventTypesWeCareAbout.contains(e.eventType) => e} ~> processUsedNetFlow.in1
      processUsedNetFlow.out ~> sink

      ClosedShape
    }
  )


  // Aggregate some statistics for each process. See 'SubjectEventCount' for specifically what is
  // aggregated. Emit downstream a stream of these records every time a 'AdaptProcessingInstruction'
  // is recieved.
  def processEventCount(source: Source[CDM17,_], sink: Sink[SubjectEventCount,_]) = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit graph =>
      val netflowEventTypes = List(
        EVENT_ACCEPT, EVENT_CONNECT, EVENT_OPEN, EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG, EVENT_SENDTO, 
        EVENT_SENDMSG, EVENT_WRITE
      )

      // TODO remove this
      val timestamps = Source.tick[AdaptProcessingInstruction](10 seconds, 10 seconds, AdaptProcessingInstruction(0))
      
      source
        .collect[CDM17]{
            case e: Event => e
            case t: AdaptProcessingInstruction => t
        }
        .merge(timestamps)   // TODO remove this
        .statefulMapConcat[SubjectEventCount]{ () =>

          // Note these map is closed over by the following function
          val filesExecuted     = MutableMap[/* Subject */UUID, Set[/* FileObject */UUID]]()
          val netflowsConnected = MutableMap[/* Subject */UUID, Set[/* Netflow    */UUID]]()
          val typesOfEvents     = MutableMap[/* Subject */UUID, Map[EventType, Int]]()
          val subjectUuids      = MutableSet[/* Subject */UUID]()

          // This is the 'flatMap'ing function that gets called for every CDM passed
          {
            case e: Event  =>

              if (EVENT_EXECUTE == e.eventType)
                filesExecuted(e.subject) = filesExecuted.getOrElse(e.subject, Set()) + e.predicateObject.get

              if (netflowEventTypes contains e.eventType)
                netflowsConnected(e.subject) = netflowsConnected.getOrElse(e.subject, Set()) + e.predicateObject.get

              typesOfEvents(e.subject) = {
                val v = typesOfEvents.getOrElse(e.subject,Map())
                v.updated(e.eventType, 1 + v.getOrElse(e.eventType, 0))
              }

              subjectUuids += e.subject

              Nil

            case t: AdaptProcessingInstruction =>
              subjectUuids.toList.map { uuid =>
                SubjectEventCount(
                  uuid,
                  filesExecuted.get(uuid).map(_.size).getOrElse(0),
                  netflowsConnected.get(uuid).map(_.size).getOrElse(0),
                  typesOfEvents.get(uuid).map(_.toMap).getOrElse(Map())
                )
              }
          }

        } ~> sink
      
      ClosedShape
    }
  )


}


class PrintActor extends Actor {
  def receive = {
    case t: TimeMarker => println("DONE!!!")
    case x => println(x)
  }
}



case class ProcessUsedNetFlow() extends GraphStage[FanInShape2[NetFlowObject, Event, UUID]] {
  val shape = new FanInShape2[NetFlowObject, Event, UUID]("ProcessUsedNetFlow")
  def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    type ProcessUUID = UUID
    type EventUUID = UUID
    type NetFlowUUID = UUID

    val netflows = MutableSet.empty[NetFlowUUID]
    val events = MutableSet.empty[Event]
    val alreadySent = MutableSet.empty[ProcessUUID]

    setHandler(shape.in0, new InHandler {
      def onPush() = {
        val n = grab(shape.in0)
        netflows += n.uuid
        events.collect {
          case e if e.predicateObject.isDefined && e.predicateObject.get == n.uuid => e
        }.headOption.fold(
          pull(shape.in0)
        ) { e =>
          events -= e
//          if ( ! alreadySent.contains(e.subject))
          push(shape.out, e.subject)
          alreadySent += e.subject
        }
      }
    })

    setHandler(shape.in1, new InHandler {
      def onPush() = {
        val e = grab(shape.in1)
        e.predicateObject match {
          case None =>
            pull(shape.in1)
          case Some(netflowUuid) =>
            if (netflows.contains(netflowUuid)) {
              if (alreadySent contains e.subject) {
                pull(shape.in1)
              } else {
                push(shape.out, e.subject)
                alreadySent += e.subject
              }
            } else {
              events += e
              pull(shape.in1)
            }
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


class ProcessCheckOpenActor() extends Actor {
  type ProcessUUID = UUID
  val processes = MutableMap.empty[ProcessUUID, Subject]
  val opens = MutableMap.empty[ProcessUUID, List[Event]]
  val checks = MutableMap.empty[ProcessUUID, List[Event]]
  def receive = {
    case e: Event if e.eventType == EVENT_OPEN =>
      opens(e.subject) = opens.getOrElse(e.subject, List.empty[Event]) :+ e
    case e: Event if e.eventType == EVENT_CHECK_FILE_ATTRIBUTES =>
      checks(e.subject) = checks.getOrElse(e.subject, List.empty[Event]) :+ e
      println("check")
    case p: Subject =>
      processes(p.uuid) = p
    case t: TimeMarker =>
      println(s"Opens: ${opens.size}\nChecks: ${checks.size}")
      val ratio = opens.map{ case (k,v) => k -> (checks.getOrElse(k, List.empty).length / v.length) }.filterNot(_._2 == 0)
      val sorted = ratio.toList.sortBy(_._2).reverse
      sorted foreach println
  }
}


case class ProcessEvents() extends GraphStage[FanInShape2[Subject, Event, (Subject,Event)]] {
  val shape = new FanInShape2[Subject, Event, (Subject,Event)]("ProcessEvents")
  def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    type ProcessUUID = UUID
    val processes = MutableMap.empty[ProcessUUID, Subject]
    val unmatchedWrites = MutableMap.empty[ProcessUUID, Set[Event]]
    var sendingBuffer = List.empty[(Subject,Event)]

    setHandler(shape.in0, new InHandler {
      def onPush() = {
        val p = grab(shape.in0)
        processes(p.uuid) = p
        unmatchedWrites.get(p.uuid) match {
          case None =>
            pull(shape.in0)
          case Some(writes) => {
            unmatchedWrites -= p.uuid
            val wList = writes.toList
            wList.headOption foreach { w =>
              push(shape.out, p -> w)
            }
            if (wList.size > 1) sendingBuffer = sendingBuffer ++ wList.tail.map(p -> _)
          }
        }
      }
    })

    setHandler(shape.in1, new InHandler {
      def onPush() = {
        val e = grab(shape.in1)
        val processUuid = e.subject
        processes.get(processUuid) match {
          case None =>
            unmatchedWrites(processUuid) = unmatchedWrites.getOrElse(processUuid, Set.empty[Event]) + e
            pull(shape.in1)
          case Some(p) =>
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
        unmatchedWrites.get(f.uuid)match {
          case None =>
            pull(shape.in0)
          case Some(writes) => {
            unmatchedWrites -= f.uuid
            val wList = writes.toList
            wList.headOption foreach { w =>
              push(shape.out, f -> w)
            }
            if (wList.size > 1) sendingBuffer = sendingBuffer ++ wList.tail.map(f -> _)
          }
        }
      }
    })

    setHandler(shape.in1, new InHandler {
      def onPush() = {
        val e = grab(shape.in1)
        val fileUuids = List(e.predicateObject, e.predicateObject2).flatten
        fileUuids foreach { fu =>
          files.get(fu) match {
            case None =>
              unmatchedWrites(fu) = unmatchedWrites.getOrElse(fu, Set.empty[Event]) + e
              pull(shape.in1)
            case Some(file) =>
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

    //    val unmatchedProcesses = MutableMap.empty[Subject,Set[Event]]   // ???
    //    val unmatchedFiles = MutableMap.empty[FileObject, Set[Event]]   // ???
    val unmatchesProcesses = MutableMap.empty[Event, Subject]  // todo: probably inefficient copies of Subject when Event differs, but Subject doesn't.
    val unmatchedFiles = MutableMap.empty[Event, FileObject]

    setHandler(shape.in0, new InHandler {
      def onPush() = {
        val (p, e) = grab(shape.in0)
        unmatchedFiles.get(e) match {
          case None =>
            unmatchesProcesses(e) = p
            pull(shape.in0)
          case Some(file) =>
            push(shape.out, (p, e, file))
             unmatchedFiles -= e
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
