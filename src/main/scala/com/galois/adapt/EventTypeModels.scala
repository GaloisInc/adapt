package com.galois.adapt

import akka.util.Timeout
import akka.pattern.ask
import java.io.{File, PrintWriter}

import com.galois.adapt.NoveltyDetection.ExtractedValue
import com.galois.adapt.cdm18.EventType
import com.univocity.parsers.csv.{CsvParser, CsvParserSettings, CsvWriter, CsvWriterSettings}

import scala.collection.JavaConverters._
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

  val modelDir = Application.config.getString("adapt.ppm.eventtypemodelsdir")
  val modelDirIForest = modelDir + "iforest/"
  val modelDirFCA = modelDir + "AR_Miner/"

  val trainFileIForest = "train_iforest.csv"

  val evalFileIForest = "eval_iforest.csv"
  val evalFileFCA = modelDirFCA + "eval_fca.csv"
  val evalFileFCARCF = modelDirFCA + "eval_fca.rcf"

  val outputFileIForest = "output_iforest.csv"
  val outputFileFCA = modelDirFCA + "output_fca.csv"

  object EventTypeData {

    //TODO: what if there's a timeout? What is returned?
    def query(treeName: String): PpmTreeCountResult = {
      implicit val timeout: Timeout = Timeout(60000 seconds)
      val future = ppmActor ? PpmTreeCountQuery(treeName: String)
      Await.ready(future, timeout.duration).value.get match {
        case Success(result) => result.asInstanceOf[PpmTreeCountResult]
        case Failure(msg) => PpmTreeCountResult(None)
      }
    }

    def collect(data: Map[List[ExtractedValue], Int]): Map[Process,EventTypeCounts] = {
      // This removes all but max depth of ProcessEventType tree, which is all we need.
      val dataFiltered = data.filter {
        case (extractedValues, _) => extractedValues.lengthCompare(3)==0 && extractedValues.last != "_?_"
      }

      dataFiltered.groupBy(x => (x._1.head,x._1(1))).map{ case ((name, uuid), dataMap) =>
           Process(name,uuid) ->
        dataMap.map(e => EventType.from(e._1.last).get->e._2)
        }
    }

    def collectToCSVArray(row: (Process,EventTypeCounts),modelName: String = "iforest"): Array[String] = {
      if (modelName=="FCA") Array(row._1.uuid) ++ EventType.values.map(e => row._2.getOrElse(e,0)).map(_.toString)
      else Array(row._1.name,row._1.uuid) ++ EventType.values.map(e => row._2.getOrElse(e,0)).map(_.toString)
    }



    def writeToFile(data: Map[List[ExtractedValue], Int], filePath: String, modelName: String = "iforest"): Unit = {
      val settings = new CsvWriterSettings
      val pw = new PrintWriter(new File(filePath))
      val writer = new CsvWriter(pw, settings)
      val header = if (modelName=="FCA") List("uuid")++EventType.values.map(e => e.toString).toList
                    else List("process_name","uuid")++EventType.values.map(e => e.toString).toList
      writer.writeHeaders(header.asJava)
      EventTypeData.collect(data).foreach(f => writer.writeRow(EventTypeData.collectToCSVArray(f,modelName)))
      writer.close()
      pw.close()
    }
  }

  object EventTypeAlarms {
    def readToTree(filePath: String, modelName: String,rowToAlarm: Array[String] => EventTypeAlarm): TreeRepr = {
      val fileHandle = new File(filePath)
      val parser = new CsvParser(new CsvParserSettings)
      val rows: List[Array[String]] = parser.parseAll(fileHandle).asScala.toList
      TreeRepr.fromFlat(List((0,modelName,0F,0F,1)) ++ rows.map(rowToAlarm))
    }

    def rowToAlarmIForest(row: Array[String]): EventTypeAlarm = {
        (1,Process(row(0),row(1)).toString(),row.last.toFloat,0F,1)
    }

    def rowToAlarmFCA (row: Array[String]): EventTypeAlarm = {
        (1,Process("",row(0)).toString(),row(1).toFloat,0F,1)
    }
  }

  object Execute {

    def iforest(iforestExecutablePath: String, trainFile: String, testFile: String, outFile: String): Int = {
      val s = s"${iforestExecutablePath}iforest.exe -t 100 -s 512 -m 1-3 -r 1 -n 0 -k 50 -z 1 -p 1 -i $trainFile -c $testFile -o $outFile"
      sys.process.Process(s,new File(modelDir)) ! ProcessLogger(_ => ()) //Returns the exit code and nothing else
    }

    def fca(scoringScriptPath: String,testFile: String, testFileRCF: String, outputFile: String): Int = {

      val makeRCF = s"""Rscript -e "source(csv_to_rcf.r); csv_to_rcf($testFile,$testFileRCF)" """
      sys.process.Process(makeRCF,new File(scoringScriptPath)) ! ProcessLogger(_ => ())

      val s = s"Rscript context_scoring_shell.r 'ProcessEvent' $testFile $testFileRCF $outputFile '97' '97'"
      "Rscript contexts_scoring_shell.r" ! ProcessLogger(_ => ())
    }
  }

  // Call this function sometime in the beginning of the flow...
  // I'd probably wait ten minutes or so to get real results
  def evaluateModels(): Unit /*Map[String,List[EventTypeAlarm]]*/ = {
    val writeResult = EventTypeData.query("ProcessEventType").results match {
      case Some(data) => Try(EventTypeData.writeToFile(data,modelDirIForest+evalFileIForest))
        Try(EventTypeData.writeToFile(data,modelDirFCA+evalFileFCA,modelName = "FCA"))
      case _ => Failure(new RuntimeException) //If there is no data, we want a failure (this seems hacky)
    }

    writeResult match {
      case Success(_) =>
        Try(Execute.iforest(modelDir,trainFileIForest,evalFileIForest,outputFileIForest))
        Try(Execute.fca(modelDirFCA,evalFileFCA,evalFileFCARCF,outputFileFCA))
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

}
