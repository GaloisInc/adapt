package com.galois.adapt

import akka.stream.scaladsl.{Flow, Sink}
import com.galois.adapt.adm._
import com.galois.adapt.cdm18._
import scala.collection.mutable
import scala.util.Try


object PpmComponents {

  type SubjectPathNodeKey = AdmUUID
  type ObjectPathNodeKey = AdmUUID
  type DelayedESO = (NoveltyDetection.Event, ADM, SubjectPathNodeKey, ADM, ObjectPathNodeKey)
  type CompletedESO = (NoveltyDetection.Event, ADM, Set[AdmPathNode], ADM, Set[AdmPathNode])
  type AdmUUIDReferencingPathNode = AdmUUID

  val ppmSink = Flow[Either[ADM, EdgeAdm2Adm]]
    .statefulMapConcat[CompletedESO]{ () =>

      val events = mutable.Map.empty[AdmUUID, (AdmEvent, Option[ADM], Option[ADM])]
      val everything = mutable.Map.empty[AdmUUID, ADM]

      val pathNodeUses = mutable.Map.empty[AdmUUIDReferencingPathNode, Set[AdmUUID]]
      val pathNodes = mutable.Map.empty[AdmUUID, AdmPathNode]

      val eventsWithPredObj2: Set[EventType] = Set(EVENT_RENAME, EVENT_MODIFY_PROCESS, EVENT_ACCEPT, EVENT_EXECUTE,
        EVENT_CREATE_OBJECT, EVENT_RENAME, EVENT_OTHER, EVENT_MMAP, EVENT_LINK, EVENT_UPDATE, EVENT_CREATE_THREAD)

      var counter: Long = 0L
      val ppmPluckingDelay = Try(Application.config.getInt("adapt.ppm.pluckingdelay")).getOrElse(20)
      val releaseQueue = mutable.Map.empty[Long, DelayedESO]
      def release(item: Option[DelayedESO]): List[CompletedESO] = {
        counter += 1
        item.foreach( i =>  // Add to release queue at X items in the future
          releaseQueue(counter + ppmPluckingDelay) = i
        )
        releaseQueue.remove(counter).map { i =>  // release the item queued for this point, after resolving path node UUIDs seen so far.
          i.copy(
            _3 = pathNodeUses.getOrElse(i._3, Set.empty).flatMap(pathNodes.get),
            _5 = pathNodeUses.getOrElse(i._5, Set.empty).flatMap(pathNodes.get)
          )
        }.toList
      }

    {
      case Right(EdgeAdm2Adm(src, "subject", tgt)) =>
        release(
          everything.get(tgt).flatMap { sub =>
            events.get(src).flatMap { e =>    // EntityResolution flow step guarantees that the event nodes will arrive before the edge that references it.
              if (e._3.isDefined) {
                if ( ! eventsWithPredObj2.contains(e._1.eventType)) events -= src
                Some((e._1, sub, sub.uuid, e._3.get, e._3.get.uuid))
              } else {
                events(src) = (e._1, Some(sub), e._3)
                None
              }
            }
          }
        )

      case Right(EdgeAdm2Adm(src, "predicateObject", tgt)) =>
        release(
          everything.get(tgt).flatMap { obj =>
            events.get(src).flatMap { e =>    // EntityResolution flow step guarantees that the event nodes will arrive before the edge that references it.
              if (e._2.isDefined) {
                if ( ! eventsWithPredObj2.contains(e._1.eventType)) events -= src
                Some((e._1, e._2.get, e._2.get.uuid, obj, obj.uuid))
              } else {
                events(src) = (e._1, e._2, Some(obj))
                None
              }
            }
          }
        )

      case Right(EdgeAdm2Adm(src, "predicateObject2", tgt)) =>
        release(
          everything.get(tgt).flatMap { obj =>
            events.get(src).flatMap { e =>    // EntityResolution flow step guarantees that the event nodes will arrive before the edge that references it.
              if (e._2.isDefined) {
                if ( ! eventsWithPredObj2.contains(e._1.eventType)) events -= src
                Some((e._1, e._2.get, e._2.get.uuid, obj, obj.uuid))
              } else {
                events(src) = (e._1, e._2, Some(obj))
                None
              }
            }
          }
        )

      case Right(EdgeAdm2Adm(child, "parentSubject", parent)) =>
        release(
          everything.get(child).flatMap { c =>
            everything.get(parent).map{ p =>
              (AdmEvent(Seq.empty, PSEUDO_EVENT_PARENT_SUBJECT, 0L, 0L, ""), c, c.uuid, p, p.uuid)
            }
          }
        )

      case Right(EdgeAdm2Adm(subObj, label, pathNode)) if List("cmdLine", "(cmdLine)", "exec", "path", "(path)").contains(label) =>
        val newSet: Set[AdmUUID] = pathNodeUses.getOrElse(subObj, Set.empty[AdmUUID]).+(pathNode)
        pathNodeUses += (subObj -> newSet)
        release(None)


  //      case Left(edge) =>
  //          edge.label match { // throw away the referenced UUIDs!
  //            case "subject" | "flowObject" => everything -= edge.src
  //            case "eventExec" | "cmdLine" | "(cmdLine)" | "exec" | "localPrincipal" | "principal" | "path" | "(path)" => everything -= edge.tgt
  //            case "tagIds" | "prevTagId" => everything -= edge.src; everything -= edge.tgt
  //            case _ => ()  // "parentSubject"
  //          }
  //        List()
      case Left(adm: AdmEvent) =>
        events += (adm.uuid -> (adm, None, None))
        release(None)
      case Left(adm: AdmSubject) =>
        everything += (adm.uuid -> adm)
        release(None)
      case Left(adm: AdmFileObject) =>
        everything += (adm.uuid -> adm)
        release(None)
      case Left(adm: AdmNetFlowObject) =>
        everything += (adm.uuid -> adm)
        release(None)
      case Left(adm: AdmSrcSinkObject) =>
        everything += (adm.uuid -> adm)
        release(None)
      case Left(adm: AdmPathNode) =>
        pathNodes += (adm.uuid -> adm)
        release(None)
      case _ =>
        release(None)
    }
  }.to(Sink.actorRefWithAck(Application.ppmActor, InitMsg, Ack, CompleteMsg))

}
