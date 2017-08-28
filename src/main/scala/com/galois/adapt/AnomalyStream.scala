package com.galois.adapt

import java.util.UUID
import java.io._

import akka.stream._
import akka.stream.scaladsl._

import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
import GraphDSL.Implicits._
import org.mapdb.DB

import scala.collection.mutable
import scala.sys.process._
import scala.util.{Failure, Random, Success, Try}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.{Source => FileSource}
import NetFlowStream._
import FileStream._
import ProcessStream._
import MemoryStream._
import FlowComponents._
import com.galois.adapt.cdm17.{CDM17, Event}
import com.typesafe.config.ConfigFactory


object AnomalyStream {

  def anomalyScoreCalculator(commandSource: Source[ProcessingCommand,_])(implicit ec: ExecutionContext) = Flow[(String, UUID, mutable.Map[String,Any], Set[UUID])]
    .merge(commandSource)
    .statefulMapConcat[Future[List[(String, UUID, Double, Set[UUID])]]] { () =>
//    var matrix = Map.empty[UUID, (String, Set[UUID])]
    var featureCollection = Map.empty[UUID, (Map[String,Any], Set[UUID])]
    var headerOpt: Option[String] = None
    var nameOpt: Option[String] = None
    val config = ConfigFactory.load()

    {
      case (name: String, uuid: UUID, fm: mutable.Map[_,_], ru: Set[_]) =>
        val featureMap = fm.asInstanceOf[mutable.Map[String,Any]]  // shut up compiler warnings.
        val relatedUuids = ru.asInstanceOf[Set[UUID]]

        if (nameOpt.isEmpty) nameOpt = Some(name)
        if (headerOpt.isEmpty) headerOpt = Some(s"uuid,${featureMap.toList.sortBy(_._1).map(_._1).mkString(",")}\n")

//        val csvFeatures = s"${featureMap.toList.sortBy(_._1).map(_._2 match {
//          case true => 1
//          case false => 0
//          case other => other
//        }).mkString(",")}\n"

//        val row = csvFeatures -> relatedUuids
//        featureCollection = featureCollection + (uuid -> row)

        featureCollection = featureCollection + (uuid -> (featureMap.toMap -> relatedUuids))
        List.empty

      case CleanUp => List.empty

      case EmitCmd =>
        if (nameOpt.isEmpty)
          List.empty
        else if (nameOpt.get.startsWith("ALARM")) List(Future{
          featureCollection.toList.map { row =>
  //              val alarmValue = if (row._2._1.trim == "true") 1D else 0D
            val alarmValue = if (row._2._1.values.head.asInstanceOf[Boolean]) 1D else 0D
            (nameOpt.get, row._1, alarmValue, row._2._2)
          }
        }) else List(
          Future{
            val randomNum = Random.nextLong()
            
            var (inputFile, outputFile, normalizedFile) = if (config.getBoolean("adapt.runtime.intermediate-csvs.keep")) {
              val directory = new File(config.getString("adapt.runtime.intermediate-csvs.path"))
              val inpFile   = File.createTempFile(s"input_${nameOpt.get}_$randomNum",  ".csv", directory)
              var outFile   = File.createTempFile(s"output_${nameOpt.get}_$randomNum", ".csv", directory)
              var normFile  = File.createTempFile(s"normalized_${nameOpt.get}_$randomNum", ".csv", directory)
              (inpFile, outFile, normFile)
            } else { 
              val inpFile  = File.createTempFile(s"input_${nameOpt.get}_$randomNum",".csv")
              var outFile  = File.createTempFile(s"output_${nameOpt.get}_$randomNum",".csv")
              var normFile = File.createTempFile(s"normalized_${nameOpt.get}_$randomNum", ".csv")
              inpFile.deleteOnExit()
              outFile.deleteOnExit()
              normFile.deleteOnExit()
              (inpFile, outFile, normFile)
            }

            val writer: FileWriter = new FileWriter(inputFile)
            writer.write(headerOpt.get)

            // normalize counts per row:
            val normalizedFeatures = featureCollection.mapValues { case (features, subgraph) =>
              val total = features.collect { case (k,v) if k.startsWith("count_") => v.asInstanceOf[Int] }.sum
              val normalized = if (total > 0) features.map {
                case (k,v) => if (k.startsWith("count_")) k -> (v.asInstanceOf[Int].toDouble / total) else (k,v)
              } else features
              normalized -> subgraph
            }

//            // normalize each column of counts:
//            var normalizedFeatures = MutableMap(featureCollection.mapValues(t => MutableMap(t._1.toList:_*) -> t._2).toList:_*)
//            val countKeys = featureCollection.headOption.map(_._2._1.keySet.filter(_.startsWith("count_"))).getOrElse(Set.empty[String])
//            countKeys.foreach { key =>
//              val total = normalizedFeatures.values.map(_._1(key).asInstanceOf[Int]).sum.toDouble
//              normalizedFeatures.transform { case (uuid, (features, subgraph)) =>
//                val normalized = if (total > 0) features(key).asInstanceOf[Int].toDouble / total else 0D
//                features += (key -> normalized)
//                features -> subgraph
//              }
//            }

            normalizedFeatures.map { case (uuid, (features, subgraph)) =>
              val rowFeatures = features.toList.sortBy(_._1).map(x => x._2 match {  // TODO: should only sort once instead of on each iteration
                case true => 1
                case false => 0
                case other => other
              }).mkString(",")
              s"$uuid,$rowFeatures\n"
            }.foreach(writer.write)

            writer.close()

            Try(Seq[String](
              config.getString("adapt.runtime.iforestpath"),
//              this.getClass.getClassLoader.getResource("bin/iforest.exe").getPath, // "../ad/osu_iforest/iforest.exe",
              "-i", inputFile.getCanonicalPath, // input file
              "-o", outputFile.getCanonicalPath, // output file
              "-m", "1", // ignore the first column
              "-t", "250" // number of trees
            ).!!) match {
              case Success(output) => //println(s"AD output: $randomNum\n$output")
              case Failure(e) => println(s"AD failure: $randomNum"); e.printStackTrace()
            }

            val shouldNormalize = config.getBoolean("adapt.runtime.shouldnoramlizeanomalyscores")

            val fileLines = if (shouldNormalize) {
              val normalizationCommand = Seq(
                "Rscript",
                this.getClass.getClassLoader.getResource("bin/NormalizeScore.R").getPath,
                "-i", outputFile.getCanonicalPath, // input file
                "-o", normalizedFile.getCanonicalPath) // output file

              val normResultTry = Try(normalizationCommand.!!) match {
                case Success(output) => //println(s"Normalization output: $randomNum\n$output")
                case Failure(e) => e.printStackTrace()
              }

              FileSource.fromFile(normalizedFile).getLines()
            } else FileSource.fromFile(outputFile).getLines()

            if (fileLines.hasNext) fileLines.next() // Throw away the header row

            val toSend = fileLines
              .toSeq.map { l =>
              val columns = l.split(",")
              val uuid = UUID.fromString(columns.head)
              (nameOpt.get, uuid, columns.last.toDouble, normalizedFeatures(uuid)._2)
            }.toList
            
            if (!config.getBoolean("adapt.runtime.intermediate-csvs.keep")) {
              inputFile.delete()
              outputFile.delete()
              normalizedFile.delete()
            }

            toSend
          }
        )
    }
  }.mapAsyncUnordered(ConfigFactory.load().getInt("adapt.runtime.iforestparallelism"))(identity)
    .mapConcat(identity)


  def anomalyScores(db: DB, fastClean: Int = 6, fastEmit: Int = 20, slowClean: Int = 30, slowEmit: Int = 50)(implicit ec: ExecutionContext) = Flow.fromGraph(
    GraphDSL.create(){ implicit graph =>
      val bcast = graph.add(Broadcast[(String, UUID, Event, CDM17)](4))
      val merge = graph.add(Merge[(String,UUID,Double, Set[UUID])](4))
      val start = graph.add(Flow[CDM17])

      val fastCommandSource = commandSource(fastClean, fastEmit)   // TODO
      val slowCommandSource = commandSource(slowClean, slowEmit)   // TODO

      start ~> predicateTypeLabeler(fastCommandSource, db) ~> bcast.in
      bcast.out(0) ~> netFlowFeatureGenerator(fastCommandSource, db).groupBy(100, _._1).via(anomalyScoreCalculator(slowCommandSource)).mergeSubstreams ~> merge
      bcast.out(1) ~> memoryFeatureGenerator(fastCommandSource, db).groupBy(100, _._1).via(anomalyScoreCalculator(slowCommandSource)).mergeSubstreams ~> merge
      bcast.out(2) ~> fileFeatureGenerator(fastCommandSource, db).groupBy(100, _._1).via(anomalyScoreCalculator(slowCommandSource)).mergeSubstreams ~> merge
      bcast.out(3) ~> processFeatureGenerator(fastCommandSource, db).groupBy(100, _._1).via(anomalyScoreCalculator(slowCommandSource)).mergeSubstreams ~> merge
      merge.out

      FlowShape(start.in, merge.out)
    }
  ) 
}
