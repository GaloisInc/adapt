package com.galois.adapt

import akka.util.Timeout
import akka.pattern.ask
import java.io.{File, PrintWriter}

import com.galois.adapt.NoveltyDetection.ExtractedValue
import com.galois.adapt.cdm18.EventType
import com.univocity.parsers.csv.{CsvParser, CsvParserSettings, CsvWriter, CsvWriterSettings}

import collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.sys.process._
import Application.ppmActor


//TODO: make all the operations safe in a functional programming way
object EventTypeModels {
  type EventTypeCounts = Map[EventType,Int]
  type EventTypeAlarm = (Process,Float) // This is the process and anomaly score
  case class Process(name: String,uuid: String)

  object EventTypeData {

    //TODO: what if there's a timeout? What is returned
    def query(treeName: String): PpmTreeCountResult = {
      val timeout = Timeout(5 seconds)
      val future = ppmActor ? PpmTreeCountQuery(treeName: String)
      Await.result(future, timeout.duration).asInstanceOf[PpmTreeCountResult]
    } //Note: the output of this function .results is an Option

    def collect(data: Map[List[ExtractedValue], Int]): Map[Process,EventTypeCounts] = {
      val dataFiltered = data.filter(_._1 match {
        case Nil => false
        case process :: Nil => false
        case process :: rest => true
        }
      )
      dataFiltered.groupBy(_._1.head).map{y =>
          val process = y._1.split(",")
          Process(process(0),process(1)) ->
          y._2.map(z => z._1.last.asInstanceOf[EventType] -> z._2) //TODO: what if the last ExtractedValue is not EventType?
        }                        //Will this turn a string into an enum?
    }

    def collectToCSVArray(row: (Process,EventTypeCounts)): Array[String] = {
      Array(row._1.name,row._1.uuid) ++ EventType.values.map(row._2.apply).map(toString).toArray
    }

    //TODO: What if this fails?
    def writeToFile(data: Map[List[ExtractedValue], Int], filePath: String): Unit = {
      val settings = new CsvWriterSettings
      val pw = new PrintWriter(new File(filePath))
      val writer = new CsvWriter(pw, settings)
      val header = List("process_name","uuid")++EventType.values.map(e => e.toString).toList
      writer.writeHeaders(header.asJava)
      EventTypeData.collect(data).foreach(f => writer.writeRow(EventTypeData.collectToCSVArray(f)))
      writer.close()
      pw.close()
    }
  }

  object EventTypeAlarms {
    //TODO: What if this fails?
    def read(filePath: String): List[Array[String]] = {
      val fileHandle = new File(filePath)
      val parser = new CsvParser(new CsvParserSettings)
      parser.parseAll(fileHandle).asScala.toList
    }

    object IForest {
      def rowToAlarm(row: Array[String]): EventTypeAlarm = {
        (Process(row(0),row(1)),row.last.toFloat)
      }
    }

    object FCA {
      def rowToAlarm (row: Array[String]): EventTypeAlarm = {
        (Process("",row(0)),row(1).toFloat)
      }
    }
  }

  object Execute {
    //TODO: What if this fails?
    def iforest(trainFile: String, testFile: String, outFile: String): Int = {
      val s = s"./iforest.exe -t 100 -s 512 -m 1-3 -r 1 -n 0 -k 50 -z 1 -p 1 -i $trainFile -c $testFile -o $outFile"
      s ! ProcessLogger(_ => ()) //Returns the exit code and nothing else
    }
  }


//TODO: When the PPMTree actor gets `CompleteMsg`, call
//  EventTypeData.writeToFile on final ProcessEventType PPMTree

}
