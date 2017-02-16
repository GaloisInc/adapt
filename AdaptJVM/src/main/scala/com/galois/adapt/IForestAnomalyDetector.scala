package com.galois.adapt

import com.galois.adapt._
import com.galois.adapt.cdm13._

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
class IForestAnomalyDetector(override val subscriptions: Set[Subscription[Map[_,Seq[Double]]]])
  extends SubscriptionActor[Map[_,Seq[Double]],Map[_,Double]] {
  
  initialize()

  override def process(c: Map[_,Seq[Double]]): Unit = {

    // Execution context and timeout
    val system = akka.actor.ActorSystem("system")
    import system.dispatcher

    // Perform this asynchronously
    Future {
      val rows: List[(_,Seq[Double])] = c.toList

      // Make a temporary file
      val inputFile: File = File.createTempFile("input",".csv")
      val outputFile: File = File.createTempFile("output",".csv")
      inputFile.deleteOnExit()
      outputFile.deleteOnExit()
    
      // Write in data
      val writer: FileWriter = new FileWriter(inputFile)
      for ((_,row) <- rows)
        writer.write(row.mkString("",",","\n"))
      writer.close()

      // Call IForest
      val suceeded = Seq[String]("./fake_iforest.py"
                                   , "-i", inputFile.getCanonicalPath
                                   , "-o", outputFile.getCanonicalPath).! == 0
      println(s"Call to IForest ${if (suceeded) "succeeded" else "failed"}.")

      // Read out data
      val outputScores: List[Double] = {
        val buffer = ListBuffer.empty[Double]
        if (suceeded) {
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
      broadCast(output)
    }
  }
}

object IForestAnomalyDetector {
  def props(inputs: Set[Subscription[Map[_, Seq[Double]]]]): Props = Props(new IForestAnomalyDetector(inputs))
}
