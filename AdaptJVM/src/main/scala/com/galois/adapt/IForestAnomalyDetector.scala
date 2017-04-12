package com.galois.adapt

import com.galois.adapt._

import scala.util.{Failure, Success}
//import com.galois.adapt.cdm13._

import java.io.{File, FileWriter, FileReader, BufferedReader, IOException}

import scala.collection.mutable.{Set => MutableSet, Map => MutableMap, ListBuffer}
import scala.sys.process._
import scala.concurrent.Future

import akka.actor._
import akka.util.Timeout

// TODO: Figure out why Future makes it impossible to exit on RETURN
// TODO: User the right IForest code, not a Python mock 

/*
 * This actor subscribes to Maps whose values are sequences of Doubles - essentially things we can
 * project into matrices. The actor takes these Maps, converts them into matrices, runs them through
 * the external IForest algorithm, then unpacks the resulting matrix back into a Map (keyed
 * appropriately).
 */
class IForestAnomalyDetector() //val registry: ActorRef, override val subscriptions: Set[Subscription])
  extends Actor with ActorLogging { //with ServiceClient with SubscriptionActor[Map[_,Double]] with ReportsStatus {
  
//  val dependencies = List.empty
//  def beginService() = initialize()
//  def endService() = ()

  def statusReport = Map("IForestAnomalyDetector" -> "status...?")

  def receive = {

    case c: Map[_,_] =>

      // Execution context and timeout
      implicit val system = context.system
      implicit val ec = context.dispatcher

      // Perform this asynchronously
      Future {
        val rows: List[(_,_)] = c.toList

        // Make a temporary file
        val inputFile: File = File.createTempFile("input",".csv")
        val outputFile: File = File.createTempFile("output",".csv")
        inputFile.deleteOnExit()
        outputFile.deleteOnExit()

        // Write in data
        val writer: FileWriter = new FileWriter(inputFile)
        for ((_,row) <- rows) {
          row match {
            case seq: Seq[_] => seq.mkString("",",","\n");
            case prod: Product => prod.productIterator.toSeq.mkString("",",","\n")
          }
        }
        writer.close()

        // Call IForest
        val succeeded = Seq[String]("./fake_iforest.py"
                                     , "-i", inputFile.getCanonicalPath
                                     , "-o", outputFile.getCanonicalPath).! == 0
        println(s"Call to IForest ${if (succeeded) "succeeded" else "failed"}.")

        // Read out data
        val outputScores: List[Double] = {
          val buffer = ListBuffer.empty[Double]
          if (succeeded) {
            val reader: BufferedReader = new BufferedReader(new FileReader(outputFile))
            var line: String = null

            do {
              line = reader.readLine()
              if (line != null) buffer += line.toDouble
            } while (line != null)

            reader.close()
          }
          buffer.toList
        }

        // Send it off
        val output: Map[_, Double] = (rows zip outputScores).map { case ((key, _), value) => (key, value) }.toMap
        output
      }.onComplete {
        case Failure(e) => log.error(s"IForest Future computation failed with:\n$e")
        case Success(output) => log.info(s"IForest output succeeded:\n$output")
      }
  }
}

object IForestAnomalyDetector {
  def props(registry: ActorRef, inputs: Set[Subscription]): Props =
    Props(new IForestAnomalyDetector()) //registry, inputs))
}

