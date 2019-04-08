package com.galois.adapt

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.{Files, Paths}
import java.util.function.Consumer
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink}
import com.galois.adapt.adm._
import com.galois.adapt.cdm20._
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._


object PpmFlowComponents {
  import AdaptConfig._

  import ApiJsonProtocol._
  import spray.json._

  type Event = AdmEvent
  type Subject = (AdmSubject, Option[AdmPathNode])
  type Object = (ADM, Option[AdmPathNode])

  type SubjectPathNodeKey = AdmUUID
  type ObjectPathNodeKey = AdmUUID
  type DelayedESO = (Event, ADM, SubjectPathNodeKey, ADM, ObjectPathNodeKey)
  type CompletedESO = (Event, ADM, Set[AdmPathNode], ADM, Set[AdmPathNode])
  type AdmUUIDReferencingPathNode = AdmUUID

  def ppmSink(implicit system: ActorSystem, ec: ExecutionContext): Sink[Either[ADM, EdgeAdm2Adm], NotUsed] =
    ppmStateAccumulator.to(
      Application.ppmObservationDistributorSink // [(Event, ADM, Set[AdmPathNode], ADM, Set[AdmPathNode])]
    )

  def ppmStateAccumulator(implicit system: ActorSystem, ec: ExecutionContext): Flow[Either[ADM, EdgeAdm2Adm], CompletedESO, NotUsed] = Flow[Either[ADM, EdgeAdm2Adm]].statefulMapConcat[CompletedESO]{ () =>
      // Load these maps from disk on startup
      val events:       mutable.Map[AdmUUID, (AdmEvent, Option[ADM], Option[ADM])] = loadMapFromDisk("events", ppmConfig.components.events)
      val everything:   mutable.Map[AdmUUID, ADM]                                  = loadMapFromDisk("everything", ppmConfig.components.everything)
      val pathNodes:    mutable.Map[AdmUUID, AdmPathNode]                          = loadMapFromDisk("pathNodes", ppmConfig.components.pathnodes)
      val pathNodeUses: mutable.Map[AdmUUIDReferencingPathNode, Set[AdmUUID]]      = loadMapFromDisk("pathNodeUses", ppmConfig.components.pathnodeuses)

      val releaseQueue: mutable.Map[Long, DelayedESO]                              = loadMapFromDisk("releaseQueue", ppmConfig.components.releasequeue)

      // Shutdown hook to save the maps above back to disk when we shutdown
      if (ppmConfig.shouldsaveppmpartialobservationaccumulators) {
        throw new RuntimeException("TODO: Saving of PPM partial observation accumulators is not implemented on shutdown.")
//        val runnable = new Runnable() {
//          override def run(): Unit = {
//            println(s"Saving state in PpmComponents...")
//            saveMapToDisk("events", events, ppmConfig.components.events)
//            saveMapToDisk("everything", everything, ppmConfig.components.everything)
//            saveMapToDisk("pathNodes", pathNodes, ppmConfig.componescheduler.schedulescheduler.schedulescheduler.schedulents.pathnodes)
//            saveMapToDisk("pathNodeUses", pathNodeUses, ppmConfig.components.pathnodeuses)
//            saveMapToDisk("releaseQueue", releaseQueue, ppmConfig.components.releasequeue)
//          }
//        }
//        Runtime.getRuntime.addShutdownHook(new Thread(runnable))
//        ppmConfig.saveintervalseconds.foreach( interval =>
//          system.scheduler.schedule(interval seconds, interval seconds, runnable)
//        )
      }

      val eventsWithPredObj2: Set[EventType] = Set(EVENT_RENAME, EVENT_MODIFY_PROCESS, EVENT_ACCEPT, EVENT_EXECUTE,
        EVENT_CREATE_OBJECT, EVENT_RENAME, EVENT_OTHER, EVENT_MMAP, EVENT_LINK, EVENT_UPDATE, EVENT_CREATE_THREAD)

      var counter: Long = 0L
      def release(item: Option[DelayedESO]): List[CompletedESO] = {
        counter += 1
        item.foreach( i =>  // Add to release queue at X items in the future
          releaseQueue(counter + ppmConfig.pluckingdelay) = i
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
              everything.get(parent).map { p =>
                val provider = Try(p.asInstanceOf[AdmSubject].provider).getOrElse("")
                val hostName = Try(p.asInstanceOf[AdmSubject].hostName).getOrElse("")
                val admParentTime = Try(p.asInstanceOf[AdmSubject].startTimestampNanos).getOrElse(0L)
                val admChildTime = Try(c.asInstanceOf[AdmSubject].startTimestampNanos).getOrElse(0L)
                (AdmEvent(Set.empty, PSEUDO_EVENT_PARENT_SUBJECT, admParentTime, admChildTime, None, None, hostName, provider), c, c.uuid, p, p.uuid)
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
    }


  // Load a mutable map from disk
  def loadMapFromDisk[T, U](name: String, fp: String)(implicit l: JsonReader[(T,U)]): mutable.Map[T,U] =
    if (ppmConfig.shouldloadppmpartialobservationaccumulators) {
      val toReturn = mutable.Map.empty[T, U]
      Try {
        Files.lines(Paths.get(fp)).forEach(new Consumer[String] {
          override def accept(line: String): Unit = {
            val (k, v) = line.parseJson.convertTo[(T, U)]
            toReturn.put(k, v)
          }
        })
      } match {
        case Failure(e) =>
          println(s"Failed to read from disk $name at $fp (${e.toString}) - only got to ${toReturn.size}")
        case _ =>
          println(s"Read in from disk $name at $fp: ${toReturn.size}")
      }
      toReturn
    } else mutable.Map.empty

  // Write a mutable map to disk
  def saveMapToDisk[T, U](name: String, map: mutable.Map[T,U], fp: String)(implicit l: JsonWriter[(T,U)]): Unit =
    if (ppmConfig.shouldsaveppmpartialobservationaccumulators) {
      import sys.process._
      Try {
        val outputFile = new File(fp)
        if ( ! outputFile.exists) outputFile.createNewFile()
        else {
          val rotateScriptPath = ppmConfig.rotatescriptpath
          if (rotateScriptPath.nonEmpty) Try( List(rotateScriptPath, fp).! ).getOrElse(println(s"Could not execute rotate script: $rotateScriptPath for file: $fp"))
        }

        val writer = new BufferedWriter(new FileWriter(outputFile))
        for (pair <- map) {
          writer.write(pair.toJson.compactPrint + "\n")
        }
        writer.close()

        println(s"Saved to disk $name at $fp: ${map.size}")
      }.getOrElse {
        println(s"Failed to save to disk $name at $fp: ${map.size}")
      }
    }

}
