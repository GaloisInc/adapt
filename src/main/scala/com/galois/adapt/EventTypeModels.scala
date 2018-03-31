package com.galois.adapt

import akka.util.Timeout
import akka.pattern.ask
import java.io.{File, PrintWriter}
import java.util.UUID

import com.galois.adapt.NoveltyDetection.{ExtractedValue, ppmList}
import com.galois.adapt.cdm18.EventType
import com.univocity.parsers.csv.{CsvParser, CsvParserSettings, CsvWriter, CsvWriterSettings}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.sys.process._
import Application.ppmActor
import akka.actor.ActorSystem
import com.galois.adapt.adm.{AdmUUID, ExtendedUuid}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}


//TODO: make all the operations safe in a functional programming way
object EventTypeModels {
  type EventTypeCounts = Map[EventType,Int]
  type EventTypeAlarm = List[(String,Float,Float,Int)] // This is the process and anomaly/fca score
  case class Process(name: String,uuid: String) {
    override def toString() = {
      this.name + "_" + this.uuid.toString
    }
  }

  val modelDirIForest = Application.config.getString("adapt.ppm.eventtypemodelsdir")
  val dirIForest = new File(modelDirIForest)
  val baseDirFile = new File(Application.config.getString("adapt.ppm.basedir"))
  if ( !dirIForest.exists && baseDirFile.exists && baseDirFile.canWrite && baseDirFile.isDirectory) dirIForest.mkdir()



  val trainFileIForest = "train_iforest.csv"
  val evalFileIForest = "eval_iforest.csv"

  val outputFileIForest = "output_iforest.csv"
  val alarmFileIForestCommon = modelDirIForest + "output_iforest_common_process_alarms.csv"
  val alarmFileIForestUnCommon = modelDirIForest + "output_iforest_uncommon_process_alarms.csv"

  val alarmTreeToFile = Map("iForestCommonAlarms" -> alarmFileIForestCommon,
    "iForestUncommonAlarms"-> alarmFileIForestUnCommon)


  object EventTypeData {

    def query(treeName: String): PpmTreeCountResult = {
      implicit val timeout: Timeout = Timeout(60000 seconds)
      val future: Future[Any] = ppmActor ? PpmTreeCountQuery(treeName: String)
      val ppmTreeCountFutureResult = Await.ready(future, timeout.duration).value match {
        case Some(Success(result)) => result
        case Some(Failure(msg)) => println(s"Unable to query ProcessEventTypeCounts with failure: ${msg.getMessage}"); None
        case None => println("Unable to query IForestProcessEventTypeCounts"); None
      }
      ppmTreeCountFutureResult match {
        case ppmTreeCountResult: PpmTreeCountResult => ppmTreeCountResult
        case None => PpmTreeCountResult(None)
      }
    }

    def collect(data: Map[List[ExtractedValue], Int]): Map[Process,EventTypeCounts] = {
      // This removes all but max depth of ProcessEventType tree, which is all we need.
      val dataFiltered = data.filter {
        case (extractedValues, _) => extractedValues.lengthCompare(3)==0 && extractedValues.last != "_?_"
      }

      dataFiltered.groupBy(x => (x._1.head,x._1(1))).map { case ((name, uuid), dataMap) =>
        Process(name,uuid) -> dataMap.collect {
          case (key, value) if key.lastOption.flatMap(EventType.from).isDefined =>
            EventType.from(key.last).get -> value
          }
      }
    }

    def collectToCSVArray(row: (Process,EventTypeCounts),modelName: String = "iforest"): Array[String] = {
      if (row._1.name.isEmpty) Array("NA",row._1.uuid) ++ EventType.values.map(e => row._2.getOrElse(e,0)).map(_.toString)
      else Array(row._1.name,row._1.uuid) ++ EventType.values.map(e => row._2.getOrElse(e,0)).map(_.toString)
    }



    def writeToFile(data: Map[List[ExtractedValue], Int], filePath: String, modelName: String = "iforest"): Unit = {
      val settings = new CsvWriterSettings
      val pw = new PrintWriter(new File(filePath))
      val writer = new CsvWriter(pw, settings)
      val header = if (modelName=="FCA") {
        List("uuid")++EventType.values.map(e => e.toString).toList
      } else {
        List("process_name","uuid")++EventType.values.map(e => e.toString).toList
      }
      writer.writeHeaders(header.asJava)
      EventTypeData.collect(data).foreach(f => writer.writeRow(EventTypeData.collectToCSVArray(f,modelName)))
      writer.close()
      pw.close()
    }
  }

  object EventTypeAlarms {

    def readToAlarmMap(filePath: String): Map[List[ExtractedValue], (Long, EventTypeAlarm, Set[ExtendedUuid], Map[String, Int])] = {
      val result = Try {
        val fileHandle = new File(filePath)
        val settings = new CsvParserSettings
        settings.setNumberOfRowsToSkip(1)
        val parser = new CsvParser(settings)
        val rows: List[Array[String]] = parser.parseAll(fileHandle).asScala.toList
        val extractedRows = extractAndFilterAlarms(rows)
        extractedRows.map(r => rowToAlarmIForest(r)).toMap
      }
      result match {
        case Success(alarmList) => alarmList
        case Failure(_) => Map.empty
      }
    }

    def extractAndFilterAlarms(rows: List[Array[String]]): List[(String,String,Float)] = {
      rows.map(r => (r(0),r(1),r.last.toFloat)).sortBy(_._3).take(5000)
    }

    def rowToAlarmIForest(extractedRow: (String,String,Float)): (List[ExtractedValue], (Long, EventTypeAlarm, Set[ExtendedUuid], Map[String, Int])) = {
      List(extractedRow._1,extractedRow._2) -> (System.currentTimeMillis,
        List(
        (extractedRow._1,extractedRow._3,extractedRow._3,1),
        (extractedRow._2,extractedRow._3,extractedRow._3,1)
        ),
        Set[ExtendedUuid](AdmUUID(UUID.fromString(extractedRow._2),"")),
        Map.empty[String, Int]
      )
    }

/*
    def rowToAlarmFCA (row: Array[String]): EventTypeAlarm = {
      (Process("",row(1)).toString(),row(3).toFloat,row(7).toFloat,1)
      }
*/

  }

  object Execute {

    def iforest(iforestDirFile: File, trainFile: String, testFile: String, outFile: String): Int = {
      val s = s"./iforest.exe -t 100 -s 512 -m 1-3 -r 1 -n 0 -k 50 -z 1 -p 1 -i $trainFile -c $testFile -o $outFile"
      println(s"Executing iforest in iforest directory with command: $s")
      sys.process.Process(s, iforestDirFile) ! ProcessLogger(_ => ()) //Returns the exit code and nothing else
    }

    def fca(scoringScriptDir: File,testFile: String, testFileRCF: String, outputFile: String): Int = {

      val makeRCF = s"""Rscript -e "source('csv_to_rcf.r'); csv_to_rcf('$testFile','$testFileRCF')" """
      println(makeRCF)
      sys.process.Process(makeRCF,scoringScriptDir) ! ProcessLogger(_ => ())

      val s = s"Rscript contexts_scoring_shell.r ProcessEvent $testFile $testFileRCF $outputFile 97 97"
      println(s)
      sys.process.Process(s,scoringScriptDir) ! ProcessLogger(_ => ())
    }
  }

  // Call this function sometime in the beginning of the flow...
  // I'd probably wait ten minutes or so to get real results
  def evaluateModels(system: ActorSystem): Unit  = {
    val writeResult = EventTypeData.query("iForestProcessEventType").results match {
      case Some(data) => Try(EventTypeData.writeToFile(data,modelDirIForest+evalFileIForest))
      case _ => println("Query to IForestProcessEventType returned no data.")
        Failure(new RuntimeException("Query to IForestProcessEventType returned no data.")) //If there is no data, we want a failure (this seems hacky)
    }

    writeResult match {
      case Success(_) =>
        Try(Execute.iforest(dirIForest,trainFileIForest,evalFileIForest,outputFileIForest))
      case Failure(ex) => println(s"Unable to query or write data for IForest: ${ex.getMessage}")
    }

    alarmTreeToFile.foreach {
      case (ppmName,file) => ppmList.find(_.name == ppmName) match {
        case Some(tree) =>  Try(getAlarms(file)) match {
          case Success(alarms) => tree.alarms = alarms
          case Failure(e) => println(s"Could not read alarm file $file: ${e.getMessage}")
        }
        case None => println(s"Unable to find tree for use in IForest alarm display: $ppmName")
      }
    }

    Try(system.scheduler.scheduleOnce(15 minutes)(evaluateModels(system)))

  }


  def getAlarms(iforestAlarmFile: String): Map[List[ExtractedValue], (Long, EventTypeAlarm, Set[ExtendedUuid], Map[String, Int])]= {
    val iforestAlarms = EventTypeAlarms.readToAlarmMap(iforestAlarmFile)

    //new File(iforestAlarmFile).delete() //If file doesn't exist, returns false

    iforestAlarms
  }

}
