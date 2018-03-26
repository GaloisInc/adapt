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

import scala.util.{Failure, Success, Try}


//TODO: make all the operations safe in a functional programming way
object EventTypeModels {
  type EventTypeCounts = Map[EventType,Int]
  type EventTypeAlarm = (Int,String,Float,Float,Int) // This is the process and anomaly/fca score
  case class Process(name: String,uuid: String) {
    override def toString() = {
      this.name + "_" + this.uuid.toString
    }
  }

  object EventTypeData {

    //TODO: what if there's a timeout? What is returned?
    def query(treeName: String): PpmTreeCountResult = {
      val timeout = Timeout(5 seconds)
      val future = ppmActor ? PpmTreeCountQuery(treeName: String)
      Await.result(future, timeout.duration).asInstanceOf[PpmTreeCountResult]
    }

    def collect(data: Map[List[ExtractedValue], Int]): Map[Process,EventTypeCounts] = {
      // This removes all but max depth of ProcessEventType tree, which is all we need
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
    def readToTree(filePath: String, modelName: String,rowToAlarm: Array[String] => EventTypeAlarm): TreeRepr = {
      val fileHandle = new File(filePath)
      val parser = new CsvParser(new CsvParserSettings)
      val rows: List[Array[String]] = parser.parseAll(fileHandle).asScala.toList
      TreeRepr.fromFlat(List((0,modelName,0,0,1)) ++ rows.map(rowToAlarm))
    }

    def rowToAlarmIForest(row: Array[String]): EventTypeAlarm = {
        (1,Process(row(0),row(1)).toString(),row.last.toFloat,0,1)
    }

    def rowToAlarmFCA (row: Array[String]): EventTypeAlarm = {
        (1,Process("",row(0)).toString(),row(1).toFloat,0,1)
    }
  }

  object Execute {

    def iforest(iforestExecutablePath: String, trainFile: String, testFile: String, outFile: String): Int = {
      val s = s"./"+iforestExecutablePath+"iforest.exe -t 100 -s 512 -m 1-3 -r 1 -n 0 -k 50 -z 1 -p 1 -i $trainFile -c $testFile -o $outFile"
      s ! ProcessLogger(_ => ()) //Returns the exit code and nothing else
    }

    def fca(fcaScoringScriptPath: String): Int = {
      "./"+ fcaScoringScriptPath + "Context_scoring_From_CSV.sh" ! ProcessLogger(_ => ())
    }
  }

  // Call this function sometime in the beginning of the flow...
  // I'd probably wait ten minutes or so to get real results
  def evaluateModels(): Unit /*Map[String,List[EventTypeAlarm]]*/ = {
    val writeResult = EventTypeData.query("ProcessEventType").results match {
      case Some(data) => Try(EventTypeData.writeToFile(data,"/a/great/place/to/put/a/trainfile"))
      case _ => Failure(RuntimeException) //If there is no data, we want a failure (this seems hacky)
    }

    writeResult match {
      case Success(_) =>
        Try(Execute.iforest("/path/to/exe/","/a/great/place/to/put/a/trainfile","/a/great/place/to/put/a/testfile","alarmFile"))
        Try(Execute.fca("/path/to/fca/script")) //What if FCA takes forever?!!....we could delete the script it needs
      case Failure(_) =>
    }

    /*getAlarms("alarmFile","fcaAlarmFile")*/
    evaluateModels()

  }

  // When the UI wants new alarm data, call this function
  def getAlarms(iforestAlarmFile: String,fcaAlarmFile: String): Map[String,Option[TreeRepr]] ={
    val iforestAlarms = Try(EventTypeAlarms.readToTree(iforestAlarmFile,"iforest",EventTypeAlarms.rowToAlarmIForest)).toOption
    val fcaAlarms = Try(EventTypeAlarms.readToTree(fcaAlarmFile,"fca",EventTypeAlarms.rowToAlarmFCA)).toOption

    val alarmMap = Map("iforest" -> iforestAlarms,
      "fca" -> fcaAlarms)

    new File(iforestAlarmFile).delete() //If file doesn't exist, returns false
    new File(fcaAlarmFile).delete()

    alarmMap
  }

//TODO: When the PPMTree actor gets `CompleteMsg`, call
//  EventTypeData.writeToFile on final ProcessEventType PPMTree

}
