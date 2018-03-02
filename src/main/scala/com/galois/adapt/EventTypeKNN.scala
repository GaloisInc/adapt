package com.galois.adapt

import akka.actor.{Actor, ActorLogging}
import com.galois.adapt.adm.{AdmPathNode, AdmSubject}
import com.galois.adapt.cdm18.EventType
import smile.classification._
import smile.math.distance.{JaccardDistance,HammingDistance,Distance}
import smile.validation.CrossValidation
import java.io.{ObjectOutputStream,FileInputStream}

object EventTypeKNN {
  type ProcessName = Set[String]
  type EventCounts = Map[EventType,Int]
  type EventVec = Array[Int]
  type ConfusionTuple = (Int,Int,Int,Int) //tp,fp,fn,tn


  class EventTypeKNNTrain {
    var dataMap: Map[ProcessName,Array[EventCounts]] = Map.empty

    def collect(s: AdmSubject, subPaths: Set[String], eventMap: EventCounts): Unit = {
      val newArray = dataMap.getOrElse(subPaths,Array(Map.empty[EventType,Int])) :+ eventMap
      dataMap += (subPaths -> newArray)
    }

    def transformData(data: Map[ProcessName,Array[EventCounts]]): (Array[EventVec],Array[ProcessName]) = {
    val dataPairs = data.flatMap( x => x._2
      .map(emap => (x._1,EventType.values
        .map(e => emap
          .getOrElse(e,0))
          .toArray)))
    (dataPairs.map(_._2).toArray,dataPairs.map(_._1).toArray)
    }

    def transformDataAndSplit(data: Map[ProcessName,Array[EventCounts]],trainPercent: Int = 70):
    (Array[EventVec],Array[ProcessName],Array[EventVec],Array[ProcessName]) = {
      val dataPairs = data.flatMap( x => x._2
        .map(emap => (x._1,EventType.values
          .map(e => emap
            .getOrElse(e,0))
          .toArray)))
      val idx = math.round(dataPairs.size*trainPercent/100.0).toInt
      val X = dataPairs.map(_._2).toArray.splitAt(idx)
      val y = dataPairs.map(_._1).toArray.splitAt(idx)
      (X._1,y._1,X._2,y._2)
    }

    def transformLabels(processNameArray: Array[ProcessName],processName: ProcessName): Array[Int] = {
      processNameArray.map(x => if (x==processName) 1 else 0)
    }

    def confusionArray(y_actual: Array[Int], y_predict: Array[Int]): ConfusionTuple = { // tn, fp, fn, tp
      val predictions = y_predict.zipWithIndex
      predictions.foldLeft((0,0,0,0))((acc,x) =>
        x._1 match {
          case 1 if y_actual(x._2)==1 => (acc._1,acc._2,acc._3,acc._4 +1)
          case 1 => (acc._1,acc._2+1,acc._3,acc._4)
          case 0 if y_actual(x._2)==1 => (acc._1,acc._2,acc._3+1,acc._4 )
          case 0 => (acc._1+1,acc._2,acc._3,acc._4 +1)
        })
    }

    def getKNNValidationStats(X: Array[EventVec], y: Array[Int], distance: Distance[EventVec], k: Int): (Distance[EventVec],Int,ConfusionTuple) = { //tp,fp,fn,tn returned
      val cv = new CrossValidation(y.length, 3)

      val testDataWithIndices = (X.zipWithIndex, y.zipWithIndex)

      val trainingDPSets = cv.train
        .map(indexList => indexList
          .map(index => testDataWithIndices
            ._1.collectFirst { case (dp, `index`) => dp }.get))

      val trainingClassifierSets = cv.train
        .map(indexList => indexList
          .map(index => testDataWithIndices
            ._2.collectFirst { case (dp, `index`) => dp }.get))

      val testingDPSets = cv.test
        .map(indexList => indexList
          .map(index => testDataWithIndices
            ._1.collectFirst { case (dp, `index`) => dp }.get))

      val testingClassifierSets = cv.test
        .map(indexList => indexList
          .map(index => testDataWithIndices
            ._2.collectFirst { case (dp, `index`) => dp }.get))


      val validationRoundRecords = trainingDPSets
        .zipWithIndex.map(x => (x._1,
        trainingClassifierSets(x._2),
        testingDPSets(x._2),
        testingClassifierSets(x._2)
      )
      )

      val confusionTuple = validationRoundRecords
        .foldLeft((0,0,0,0)) { //tn,fp,fn,tp
          (acc:ConfusionTuple, record: (Array[EventVec],Array[Int],Array[EventVec],Array[Int])) =>

          val model = knn[EventVec](record._1, record._2, distance, k)

          //And for each test data point make a prediction with the model
          val predictions = record
            ._3
            .map(x => model.predict(x))

           val confusionTuple = confusionArray(record._4,predictions)

            (acc._1+confusionTuple._1,acc._2+confusionTuple._2,acc._3+confusionTuple._3,acc._4+confusionTuple._4)
        }
      (distance,k,confusionTuple)
    }

    def selectBestModel(modelStats: List[(Distance[EventVec],Int,ConfusionTuple)]): (Distance[EventVec],Int,ConfusionTuple) = {
      modelStats.sortBy(x => x._3._3)
        .take(3) //Take the three model parameters that produce the fewest false alarms.
        .maxBy(x => (x._3._1 + x._3._4) / (x._3._1 + x._3._2 + x._3._3 + x._3._4))  //Of those, take the one with the highest accuracy.
    }

    def bestModelIfExists(model: KNN[EventVec], X: Array[EventVec],y: Array[Int], distance: Distance[EventVec],k: Int,falseAlarmThreshold: Int):
    (Option[KNN[EventVec]],Distance[EventVec],Int,ConfusionTuple) = {
      val predictions = X.map(x => model.predict(x))
      val confusionTuple = confusionArray(y,predictions)
      if (confusionTuple._3 <= falseAlarmThreshold) (Some(model),distance,k,confusionTuple)
      else (None,distance,k,confusionTuple)
    }


    def testSelectWriteModels(falseAlarmThreshold: Int): Unit = {
      //val data = transformData(dataMap)
      val data = transformDataAndSplit(dataMap)
      val processNames = dataMap.keys
      val processModels: Map[ProcessName,(Option[KNN[EventVec]],Distance[EventVec],Int,ConfusionTuple)] = processNames.map { processName =>
        val X_train = data._1
        val y_train = transformLabels(data._2, processName)
        //val distances = List(JaccardDistance[EventVec])
        val numNeighbors = List(1, 3, 5)
        val modelChoices = for (/*d <- distances;*/ k <- numNeighbors) yield getKNNValidationStats(X_train, y_train, new JaccardDistance[EventVec], k)
        val bestParams = selectBestModel(modelChoices)
        val bestModel = knn[EventVec](X_train, y_train, bestParams._1, bestParams._2)
        processName ->
          bestModelIfExists(bestModel, data._3, transformLabels(data._4, processName), bestParams._1, bestParams._2, falseAlarmThreshold)
      }.toMap

      processModels.foreach(x => println(x._1+" "+x._2._4.toString()))

      val toStore: Map[ProcessName,KNN[EventVec]] = processModels
        .filter(x => x._2._1.isDefined)
        .map(x => x._1 -> x._2._1)
        .flatten.toMap

      val oos = new ObjectOutputStream(new java.io.FileOutputStream("knnmodels.ser",true))
      oos.writeObject(toStore)
      oos.close()
    }


  }


}

class KNNActor extends Actor with ActorLogging {
  import EventTypeKNN._
  val eventTypeKNNTrain = new EventTypeKNNTrain()

  def receive = {

    case (s: AdmSubject, subPathNodes: Set[AdmPathNode], eventMap: EventCounts) =>
      eventTypeKNNTrain.collect(s,subPathNodes.map(_.path),eventMap)
      //println(s.toString+" "+subPathNodes.map(_.path).mkString(",")+" "+eventMap.toString())
      sender() ! Ack

    case InitMsg => sender() ! Ack
    case CompleteMsg => eventTypeKNNTrain.testSelectWriteModels(3) //upon completion save trained models and print summary stats
    case x => log.error(s"Received Unknown Message: $x")
  }
}
