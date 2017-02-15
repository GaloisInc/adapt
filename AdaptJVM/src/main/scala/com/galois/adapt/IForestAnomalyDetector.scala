package com.galois.adapt.ad

import com.galois.adapt._
import com.galois.adapt.cdm13._

import java.io.{File, FileWriter, FileReader, BufferedReader, IOException}

import scala.collection._
import scala.sys.process._

import akka.actor._

class IForestAnomalyDetector(override val subscriptions: immutable.Set[Subscription[Map[_,Seq[Double]]]])
  extends SubscriptionActor[Map[_,Seq[Double]],Map[_,Double]] {
  
  initialize()

  override def process(c: Map[_,Seq[Double]]): Unit = {

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
      val buffer = mutable.ListBuffer.empty[Double]
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

object IForestAnomalyDetector {
  def props(inputs: immutable.Set[Subscription[Map[_, Seq[Double]]]]): Props = Props(new IForestAnomalyDetector(inputs))
}
