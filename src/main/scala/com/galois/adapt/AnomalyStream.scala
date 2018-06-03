package com.galois.adapt

import java.util.UUID
import java.io._
import scala.sys.process._
import scala.util.{Failure, Random, Success, Try}
import scala.io.{Source => FileSource}
import com.typesafe.config.ConfigFactory


object AnomalyStream {

    var featureCollection = Map.empty[UUID, (Map[String,Any], Set[UUID])]
    var headerOpt: Option[String] = None
    var nameOpt: Option[String] = None

          {
            val randomNum = Random.nextLong()
//            val inputFile = new File(s"/Users/ryan/Desktop/intermediate_csvs/temp.in_${nameOpt.get}_$randomNum.csv") // TODO
//            var outputFile = new File(s"/Users/ryan/Desktop/intermediate_csvs/temp.out_${nameOpt.get}_$randomNum.csv") // TODO
            val inputFile  = File.createTempFile(s"input_${nameOpt.get}_$randomNum",".csv")
            var outputFile = File.createTempFile(s"output_${nameOpt.get}_$randomNum",".csv")
            inputFile.deleteOnExit()
            outputFile.deleteOnExit()
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

            val config = ConfigFactory.load()

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
              val normalizedFile = File.createTempFile(s"normalized_${nameOpt.get}_$randomNum", ".csv")
//              val normalizedFile = new File(s"/Users/ryan/Desktop/intermediate_csvs/normalized_${nameOpt.get}_$randomNum.csv")
//              normalizedFile.createNewFile()
              normalizedFile.deleteOnExit()

              val normalizationCommand = Seq(
                "Rscript",
                this.getClass.getClassLoader.getResource("bin/NormalizeScore.R").getPath,
                "-i", outputFile.getCanonicalPath, // input file
                "-o", normalizedFile.getCanonicalPath) // output file

              val normResultTry = Try(normalizationCommand.!!) match {
                case Success(output) => //println(s"Normalization output: $randomNum\n$output")
                case Failure(e) => e.printStackTrace()
              }

              outputFile.delete()
              outputFile = normalizedFile

              FileSource.fromFile(normalizedFile).getLines()
            } else FileSource.fromFile(outputFile).getLines()

            if (fileLines.hasNext) fileLines.next() // Throw away the header row

            val toSend = fileLines
              .toSeq.map { l =>
              val columns = l.split(",")
              val uuid = UUID.fromString(columns.head)
              (nameOpt.get, uuid, columns.last.toDouble, normalizedFeatures(uuid)._2)
            }.toList
            inputFile.delete()
            outputFile.delete()
            toSend
          }
//  }.mapAsyncUnordered(ConfigFactory.load().getInt("adapt.runtime.iforestparallelism"))(identity)
}
