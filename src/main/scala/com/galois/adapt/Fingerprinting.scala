package com.galois.adapt.fingerprinting

import akka.actor.{Actor, ActorLogging}
import com.galois.adapt.NoveltyDetection.Event
import com.galois.adapt.{Ack, CompleteMsg, InitMsg}
import com.galois.adapt.adm._

import scala.io.Source
import scala.util.parsing.json.JSON


object Fingerprinting {
  type M = String // needs to be filetouch or network


  trait FingerprintModel {
    def getModel: String

    def evaluate(p: Set[String], v: String): Option[Boolean]

    //def getFinalStats(yourDepth: Int = 0, key: String = "", yourProbability: Float = 1F, parentGlobalProb: Float = 1F): TreeReport
  }

  case object FingerprintModel {
    def apply(model: M, modelFilePath: String): FingerprintModel = model match {
      case "filetouch" => new FiletouchModelEvaluator(modelFilePath)
    }

    class FiletouchModelEvaluator(modelFilePath: String) extends FingerprintModel {

      class CC[T] {
        def unapply(a: Any): Option[T] = Some(a.asInstanceOf[T])
      }

      object M extends CC[Map[String, Any]]

      object N extends CC[List[Any]]

      object S extends CC[String]

      object P extends CC[List[String]]

      object D extends CC[Double]


      // Example data holding selected model params
      //val jsonString = "{\"processes\": [{\"name\": \"postgres\", \"depth\": -1, \"paths\": [\"/usr/local/pgsql/data/base/12730/\", \"/tmp/\", \"/usr/local/pgsql/data/pg_stat_tmp/\", \"/usr/local/pgsql/data/\", \"/usr/local/pgsql/data/base/16384/\", \"/usr/local/pgsql/data/global/\"]}, {\"name\":\"atrun,cron,sh\",\"depth\": 2, \"paths\": [\"/etc/\", \"/libexec/\", \"/\", \"/lib/\", \"/dev/\", \"/bin/\", \"/var/\", \"/usr/\"]}]}"
      case class depthsAndPaths(depth: Int, paths: List[String])

      val filePathModel = (jsonString: String) => for {
        Some(M(map)) <- List(JSON.parseFull(jsonString))
        N(processes) = map("processes")
        M(process) <- processes
        S(name) = process("name")
        D(depth) = process("depth")
        P(paths) = process("paths")
      } yield {
        (name.split(",").toSet, depthsAndPaths(depth.toInt, paths))
      }

      val bufferedSource = Source.fromFile(modelFilePath) // "test_file_path_model.json")
      val filePathModelMap = filePathModel(bufferedSource.getLines.mkString).toMap
      bufferedSource.close

      def getModel(): String = {
        filePathModelMap.toString()
      }

      // Function returns True if the FilePath is common to processName
      // otherwise it returns False and should raise an alarm.
      def evaluate(processName: Set[String], admPathNodePath: String): Option[Boolean] = {
        //println(processName+" "+admPathNodePath)
        filePathModelMap.get(processName) match {
          case Some(depthToPath) => {
            val depth = filePathModelMap(processName).depth
            val splitPath = admPathNodePath.split("/")
            val pathInput = depth match {
              case -1 => splitPath.init.mkString("/") + "/"
              case _ => splitPath.take(depth).mkString("/") + "/"
            }
            println(processName+" "+ admPathNodePath)
            println( "       " +pathInput+" "+ filePathModelMap(processName).paths.contains(pathInput).toString)
            Some(depthToPath.paths.contains(pathInput))
          }
          case None => None
        }
        }
      }
    }
}

class FingerprintActor extends Actor with ActorLogging {
  import Fingerprinting._

  val filePathModel = FingerprintModel("filetouch", "/Users/nls/repos/adapt/src/main/resources/model_file_touch_cadets.json")
  println(filePathModel.getModel)

  def receive = {
    //need to filter out the paths that don't start with /; also, only want one path per eval call; also, don't need e,s,o
    // getting several size 0, size 2 sets
    // getting path nodes that don't start with /
    case (e: Event, Some(s: AdmSubject), subPathNodes: Set[AdmPathNode], Some(o: ADM), objPathNodes: Set[AdmPathNode]) =>
      filePathModel.evaluate(subPathNodes.map(_.path), objPathNodes.toList.map(_.path).mkString(","))
      sender() ! Ack

    case InitMsg => sender() ! Ack
    case CompleteMsg => println("All done!")
    case x => log.error(s"Received Unknown Message: $x")
  }
}





