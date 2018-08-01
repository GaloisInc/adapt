package com.galois.adapt

import java.io.{BufferedWriter, File, FileReader, FileWriter}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.util.function.Consumer

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink}
import com.galois.adapt.adm._
import com.galois.adapt.cdm18._
import spray.json.{JsonReader, JsonWriter}

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

object PpmComponents {

  import ApiJsonProtocol._
  import spray.json._

  type SubjectPathNodeKey = AdmUUID
  type ObjectPathNodeKey = AdmUUID
  type DelayedESO = (NoveltyDetection.Event, ADM, SubjectPathNodeKey, ADM, ObjectPathNodeKey)
  type CompletedESO = (NoveltyDetection.Event, ADM, Set[AdmPathNode], ADM, Set[AdmPathNode])
  type AdmUUIDReferencingPathNode = AdmUUID

  val config = Application.config

  def ppmSink(implicit system: ActorSystem, ec: ExecutionContext) = Flow[Either[ADM, EdgeAdm2Adm]]
    .statefulMapConcat[CompletedESO]{ () =>

      import ApiJsonProtocol._
      import spray.json._

      val eventsSavePath: String = Application.config.getString("adapt.ppm.components.events")
      val everythingSavePath: String = Application.config.getString("adapt.ppm.components.everything")
      val pathNodesSavePath: String = Application.config.getString("adapt.ppm.components.pathnodes")
      val pathNodeUsesSavePath: String = Application.config.getString("adapt.ppm.components.pathnodeuses")
      val releaseQueueSavePath: String = Application.config.getString("adapt.ppm.components.releasequeue")

      // Load these maps from disk on startup
      val events:       mutable.Map[AdmUUID, (AdmEvent, Option[ADM], Option[ADM])] = loadMapFromDisk("events", eventsSavePath)

      val everything:   mutable.Map[AdmUUID, ADM]                                  = loadMapFromDisk("everything", everythingSavePath)
      val pathNodes:    mutable.Map[AdmUUID, AdmPathNode]                          = loadMapFromDisk("pathNodes", pathNodesSavePath)
      val pathNodeUses: mutable.Map[AdmUUIDReferencingPathNode, Set[AdmUUID]]      = loadMapFromDisk("pathNodeUses", pathNodeUsesSavePath)

      val releaseQueue: mutable.Map[Long, DelayedESO]                              = loadMapFromDisk("releaseQueue", releaseQueueSavePath)

      // Shutdown hook to save the maps above back to disk when we shutdown
      val runnable = new Runnable() {
        override def run(): Unit = {
          println(s"Saving state in PpmComponents...")

          saveMapToDisk("events", events, eventsSavePath)
          saveMapToDisk("everything", everything, everythingSavePath)
          saveMapToDisk("pathNodes", pathNodes, pathNodesSavePath)
          saveMapToDisk("pathNodeUses", pathNodeUses, pathNodeUsesSavePath)
          saveMapToDisk("releaseQueue", releaseQueue, releaseQueueSavePath)
        }
      }

      Runtime.getRuntime.addShutdownHook(new Thread(runnable))
      system.scheduler.schedule(20.minutes, 20.minutes, runnable)

      val eventsWithPredObj2: Set[EventType] = Set(EVENT_RENAME, EVENT_MODIFY_PROCESS, EVENT_ACCEPT, EVENT_EXECUTE,
        EVENT_CREATE_OBJECT, EVENT_RENAME, EVENT_OTHER, EVENT_MMAP, EVENT_LINK, EVENT_UPDATE, EVENT_CREATE_THREAD)

      var counter: Long = 0L
      val ppmPluckingDelay = Try(Application.config.getInt("adapt.ppm.pluckingdelay")).getOrElse(20)
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
  }.to(Sink.actorRefWithAck(Application.ppmActor.get, InitMsg, Ack, CompleteMsg))


  // Load a mutable map from disk
  def loadMapFromDisk[T, U](name: String, fp: String)
                           (implicit l: JsonReader[(T,U)]): mutable.Map[T,U] =
    if (Application.config.getBoolean("adapt.ppm.shouldload"))
      Try {
        val toReturn = mutable.Map.empty[T,U]
        Files.lines(Paths.get(fp)).forEach(new Consumer[String]{
          override def accept(line: String): Unit = {
            val (k, v) = line.parseJson.convertTo[(T, U)]
            toReturn.put(k,v)
          }
        })

        println(s"Read in from disk $name at $fp: ${toReturn.size}")
        toReturn
      } match {
        case Success(m) => m
        case Failure(e) =>
          println(s"Failed to read from disk $name at $fp (${e.toString})")
          mutable.Map.empty
      }
    else mutable.Map.empty

  // Write a mutable map to disk
  def saveMapToDisk[T, U](name: String, map: mutable.Map[T,U], fp: String)
                         (implicit l: JsonWriter[(T,U)]): Unit =
    if (Application.config.getBoolean("adapt.ppm.shouldsave")) {
      import sys.process._
      Try {
        val outputFile = new File(fp)
        if ( ! outputFile.exists) outputFile.createNewFile()
        else {
          val rotateScriptPath = Try(Application.config.getString("adapt.ppm.rotatescriptpath")).getOrElse("")
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
