package com.galois.adapt

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.file.Paths
import java.util.UUID
import java.io._
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import com.galois.adapt.cdm17._
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.Producer
import akka.kafka.scaladsl.Consumer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.apache.avro.specific.{SpecificDatumReader, SpecificDatumWriter}
import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
import GraphDSL.Implicits._
import akka.util.ByteString
import ch.qos.logback.classic.LoggerContext
import org.mapdb.{DB, DBMaker, HTreeMap}
import collection.JavaConverters._
import scala.collection.mutable
import scala.sys.process._
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success, Try}
import com.thinkaurelius.titan.core._
import com.thinkaurelius.titan.graphdb.database.management.ManagementSystem
import com.thinkaurelius.titan.core.schema.{SchemaAction, SchemaStatus}
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.tinkerpop.gremlin.structure.Vertex
import scala.concurrent.{ExecutionContext, Future}
import scala.io.{Source => FileSource}
import NetFlowStream._
import FileStream._
import ProcessStream._
import MemoryStream._


object FlowComponents {

  sealed trait EventsKey
  case object PredicateObjectKey extends EventsKey
  case class SubjectKey(t: Option[EventType]) extends EventsKey


  def eventsGroupedByKey(commandSource: Source[ProcessingCommand, _], dbMap: HTreeMap[UUID, mutable.SortedSet[Event]], key: EventsKey) = {
    val keyPredicate = key match {
      case PredicateObjectKey => Flow[CDM17]
        .collect { case e: Event if e.predicateObject.isDefined => e }
        .mapConcat(e =>
          if (e.predicateObject2.isDefined) List((e.predicateObject.get, e), (e.predicateObject2.get, e))
          else List((e.predicateObject.get, e)))
      case SubjectKey(Some(t)) => Flow[CDM17]
        .collect { case e: Event if e.eventType == t => e.subjectUuid -> e }
      case SubjectKey(None) => Flow[CDM17]
        .collect { case e: Event => e.subjectUuid -> e }
    }
    keyPredicate
      .filter(_._2.timestampNanos != 0L)
      .filterNot { tup =>
        val excluded = List("00000000-0000-0000-0000-000000000000", "071fbdeb-131c-11e7-bfbf-f55a9065b18e", "19f119de-131b-11e7-bfbf-f55a9065b18e").map(UUID.fromString)
        excluded.contains(tup._1)
      } // TODO: why are there these special cases?!?!?!?!?
      .groupBy(Int.MaxValue, _._1) // TODO: Limited to ~4 billion unique UUIDs!!!
      .merge(commandSource)
      .statefulMapConcat { () =>
        var uuid: Option[UUID] = None
        val events = mutable.SortedSet.empty[Event](Ordering.by[Event, Long](_.timestampNanos))

        {
          case EmitCmd =>
            val existingSet = dbMap.getOrDefault(uuid.get, mutable.SortedSet.empty[Event](Ordering.by[Event, Long](_.timestampNanos)))
            existingSet ++= events
            dbMap.put(uuid.get, existingSet)
            events.clear()
            List(uuid.get -> existingSet)

          case CleanUp =>
            if (events.nonEmpty) {
              val existingSet = dbMap.getOrDefault(uuid.get, mutable.SortedSet.empty[Event](Ordering.by[Event, Long](_.timestampNanos)))
              existingSet ++= events
              dbMap.put(uuid.get, existingSet)
              events.clear()
            }
            List.empty

          case Tuple2(u: UUID, e: Event) =>
            if (uuid.isEmpty) uuid = Some(u)
            //            val emptySet = mutable.SortedSet.empty[Event](Ordering.by[Event, Long](_.timestampNanos))
            //            dbMap.put(u, emptySet)
            events += e
            List.empty
        }
      }
  }


  def sortedEventAccumulator[K](groupBy: ((UUID,Event,CDM17)) => K, commandSource: Source[ProcessingCommand,_], db: DB) = {
    val dbMap = db.hashMap("sortedEventAccumulator" + Random.nextInt()).createOrOpen().asInstanceOf[HTreeMap[UUID, mutable.SortedSet[Event]]]
    Flow[(UUID,Event,CDM17)]
      .groupBy(Int.MaxValue, groupBy) // TODO: Limited to ~4 billion unique UUIDs!!!
      .merge(commandSource)
      .statefulMapConcat { () =>
        var uuid: Option[UUID] = None
        val events = mutable.SortedSet.empty[Event](Ordering.by[Event, Long](_.timestampNanos))

        {
          case EmitCmd =>
            val existingSet = dbMap.getOrDefault(uuid.get, mutable.SortedSet.empty[Event](Ordering.by[Event, Long](_.timestampNanos)))
            existingSet ++= events
            dbMap.put(uuid.get, existingSet)
            events.clear()
            List(uuid.get -> existingSet)

          case CleanUp =>
            if (events.nonEmpty) {
              val existingSet = dbMap.getOrDefault(uuid.get, mutable.SortedSet.empty[Event](Ordering.by[Event, Long](_.timestampNanos)))
              existingSet ++= events
              dbMap.put(uuid.get, existingSet)
              events.clear()
            }
            List.empty

          case Tuple3(u: UUID, e: Event, _: CDM17) =>
            if (uuid.isEmpty) uuid = Some(u)
            events += e
            List.empty
        }
      }.mergeSubstreams
  }



  def predicateTypeLabeler(commandSource: Source[ProcessingCommand,_], db: DB) = {
    val dbMap = db.hashMap("typeSorter_" + Random.nextLong()).createOrOpen().asInstanceOf[HTreeMap[UUID,mutable.SortedSet[Event]]]
    Flow[CDM17]
      .mapConcat[(UUID, String, CDM17)] {
        case e: Event if e.predicateObject.isDefined =>
          if (e.predicateObject2.isDefined) List((e.predicateObject.get, "Event", e), (e.predicateObject2.get, "Event", e))
          else List((e.predicateObject.get, "Event", e))
        case n: NetFlowObject => List((n.uuid, "NetFlowObject", n))
        case f: FileObject => List((f.uuid, "FileObject", f))
        case s: Subject => List((s.uuid, "Subject", s))
        case _ => List.empty }
//        case msg @ => List(msg) }
      .filterNot {
        case (uuid, e, _) =>
          val excluded = List("00000000-0000-0000-0000-000000000000", "071fbdeb-131c-11e7-bfbf-f55a9065b18e", "19f119de-131b-11e7-bfbf-f55a9065b18e").map(UUID.fromString)
          excluded.contains(uuid) // TODO: why are there these special cases in cadets data?!?!?!?!?
        case _ => false }
      .groupBy(Int.MaxValue, _._1)
      .merge(commandSource)
      .statefulMapConcat[(String, UUID, Event, CDM17)] { () =>
        var idOpt: Option[(UUID,String,CDM17)] = None
        val events = mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos))

        {
          case Tuple3(predicateUuid: UUID, "Event", e: Event) =>
            if (idOpt.isDefined)
              List((idOpt.get._2, predicateUuid, e, idOpt.get._3))
            else {
              events += e
              List.empty
            }
  //          List(labelOpt.map(label => (label, predicateUuid, e))).flatten   // TODO: interesting. maybe this _should_ throw away events for Objects we never see.

          case Tuple3(objectUuid: UUID, labelName: String, cdm: CDM17) =>
            if (idOpt.isEmpty) {
              idOpt = Some((objectUuid, labelName, cdm))
//              val existingSet = dbMap.getOrDefault(objectUuid, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos)))
//              events ++= existingSet
//              dbMap.remove(objectUuid)
            }
            val toSend = events.toList.map(event => (labelName, objectUuid, event, cdm))
            events.clear()
            toSend

          case CleanUp =>
//            if (events.nonEmpty) {
//              val existingSet = dbMap.getOrDefault(uuidOpt.get, mutable.SortedSet.empty[Event](Ordering.by(_.timestampNanos)))
//              events ++= existingSet
//  //            println(s"UNMATCHED: ${uuidOpt}  size: ${events.size}    ${events.map(_.eventType)}")  // TODO
//              dbMap.put(uuidOpt.get, events)
//              events.clear()
//            }
            List.empty

          case EmitCmd => List.empty
        }
      }.mergeSubstreams
  }

//case class LabeledPredicateType(labelName: String, predicateObjectUuid: UUID, event: Event, cdm: CDM17)


  def printCounter[T](name: String, every: Int = 10000) = Flow[T].statefulMapConcat { () =>
    var counter = 0L
    var originalStartTime = 0L
    var lastTimestampNanos = 0L

    { item: T =>  // Type annotation T is a compilation hack! No runtime effect because it's generic.
        if (lastTimestampNanos == 0L) {
          originalStartTime = System.nanoTime()
          lastTimestampNanos = System.nanoTime()
        }
        counter = counter + 1
        if (counter % every == 0) {
          val nowNanos = System.nanoTime()
          val durationSeconds = (nowNanos - lastTimestampNanos) / 1e9
          println(s"$name ingested: $counter   Elapsed for this $every: ${f"$durationSeconds%.3f"} seconds.  Rate for this $every: ${(every / durationSeconds).toInt} items/second.  Rate since beginning: ${(counter / ((nowNanos - originalStartTime) / 1e9)).toInt} items/second")
          lastTimestampNanos = System.nanoTime()
        }
        List(item)
    }
  }


  val uuidMapToCSVPrinterSink = Flow[(UUID, mutable.Map[String,Any])]
    .map{ case (u, m) =>
      s"$u,${m.toList.sortBy(_._1).map(_._2).mkString(",")}"
    }.toMat(Sink.foreach(println))(Keep.right)


  def csvFileSink(path: String) = Flow[(UUID, mutable.Map[String,Any])]
    .statefulMapConcat{ () =>
      var needsHeader = true

      { case Tuple2(u: UUID, m: mutable.Map[String,Any]) =>
        val row = List(ByteString(s"$u,${m.toList.sortBy(_._1).map(_._2).mkString(",")}\n"))
        if (needsHeader) {
          needsHeader = false
          List(ByteString(s"uuid,${m.toList.sortBy(_._1).map(_._1).mkString(",")}\n")) ++ row
        } else row
      }
    }.toMat(FileIO.toPath(Paths.get(path)))(Keep.right)


  def anomalyScoreCalculator(commandSource: Source[ProcessingCommand,_]) = Flow[(String, UUID, mutable.Map[String,Any], Set[UUID])]
    .merge(commandSource)
    .statefulMapConcat[(String, UUID, Double, Set[UUID])] { () =>
      var matrix = MutableMap.empty[UUID, (String, Set[UUID])]
      var headerOpt: Option[String] = None
      var nameOpt: Option[String] = None

      {
        case Tuple4(name: String, uuid: UUID, featureMap: mutable.Map[String,Any], relatedUuids: Set[UUID]) =>
          if (nameOpt.isEmpty) nameOpt = Some(name)
          if (headerOpt.isEmpty) headerOpt = Some(s"uuid,${featureMap.toList.sortBy(_._1).map(_._1).mkString(",")}\n")
          val csvFeatures = s"${featureMap.toList.sortBy(_._1).map(_._2).mkString(",")}\n"
          val row = csvFeatures -> relatedUuids
          matrix(uuid) = row
          List.empty

        case CleanUp => List.empty

        case EmitCmd =>
          if (nameOpt.isEmpty) List.empty
          else if (nameOpt.get.startsWith("ALARM")) {
            matrix.toList.map{row =>
              val alarmValue = if (row._2._1.trim == "true") 1D else 0D
              (nameOpt.get, row._1, alarmValue, row._2._2)
            }
          } else {
            val randomNum = Random.nextLong()
            val inputFile = new File(s"/Users/ryan/Desktop/intermediate_csvs/temp.in_${nameOpt.get}_$randomNum.csv") // TODO
            val outputFile = new File(s"/Users/ryan/Desktop/intermediate_csvs/temp.out_${nameOpt.get}_$randomNum.csv") // TODO
            //          val inputFile  = File.createTempFile(s"input_${nameOpt.get}_$randomNum",".csv")
            //          val outputFile = File.createTempFile(s"output_${nameOpt.get}_$randomNum",".csv")
            inputFile.deleteOnExit()
            outputFile.deleteOnExit()
            val writer: FileWriter = new FileWriter(inputFile)
            writer.write(headerOpt.get)
            matrix.map(row => s"${row._1},${row._2._1}").foreach(writer.write)
            writer.close()

            Try(Seq[String](
              this.getClass.getClassLoader.getResource("bin/iforest.exe").getPath, // "../ad/osu_iforest/iforest.exe",
              "-i", inputFile.getCanonicalPath, // input file
              "-o", outputFile.getCanonicalPath, // output file
              "-m", "1", // ignore the first column
              "-t", "250" // number of trees
            ).!!) match {
              case Success(output) => //println(s"AD output: $randomNum\n$output")
              case Failure(e) => println(s"AD failure: $randomNum"); e.printStackTrace()
            }

            //          val normalizedFile = File.createTempFile(s"normalized_${nameOpt.get}_$randomNum", ".csv")
            //          val normalizedFile = new File(s"/Users/ryan/Desktop/intermediate_csvs/normalized_${nameOpt.get}_$randomNum.csv")
            //          normalizedFile.createNewFile()
            //          normalizedFile.deleteOnExit()

            //          val normalizationCommand = Seq(
            //            "Rscript",
            //            this.getClass.getClassLoader.getResource("bin/NormalizeScore.R").getPath,
            //            "-i", outputFile.getCanonicalPath,       // input file
            //            "-o", normalizedFile.getCanonicalPath)   // output file
            //
            //          val normResultTry = Try(normalizationCommand.!!) match {
            //            case Success(output) => println(s"Normalization output: $randomNum\n$output")
            //            case Failure(e)      => e.printStackTrace()
            //          }


            //          val fileLines = FileSource.fromFile(normalizedFile).getLines()
            val fileLines = FileSource.fromFile(outputFile).getLines()
            if (fileLines.hasNext) fileLines.next() // Throw away the header row
            fileLines
              .toSeq.map { l =>
              val columns = l.split(",")
              val uuid = UUID.fromString(columns.head)
              (nameOpt.get, uuid, columns.last.toDouble, matrix(uuid)._2)
            }.toList
          }
      }
    }


  def commandSource(cleanUpSeconds: Int, emitSeconds: Int) =
    Source.tick[ProcessingCommand](cleanUpSeconds seconds, cleanUpSeconds seconds, CleanUp).buffer(1, OverflowStrategy.backpressure)
      .merge(Source.tick[ProcessingCommand](emitSeconds seconds, emitSeconds seconds, EmitCmd).buffer(1, OverflowStrategy.backpressure))


  def anomalyScores(db: DB, fastClean: Int = 6, fastEmit: Int = 20, slowClean: Int = 30, slowEmit: Int = 50) = Flow.fromGraph(
    GraphDSL.create(){ implicit graph =>
      val bcast = graph.add(Broadcast[CDM17](4))
      val merge = graph.add(Merge[(String,UUID,Double, Set[UUID])](4))

      val fastCommandSource = commandSource(fastClean, fastEmit)   // TODO
      val slowCommandSource = commandSource(slowClean, slowEmit)   // TODO

      bcast.out(0) ~> netFlowFeatureGenerator(fastCommandSource, db).groupBy(100, _._1).via(anomalyScoreCalculator(slowCommandSource)).mergeSubstreams ~> merge
      bcast.out(1) ~> fileFeatureGenerator(fastCommandSource, db).groupBy(100, _._1).via(anomalyScoreCalculator(slowCommandSource)).mergeSubstreams ~> merge
      bcast.out(2) ~> processFeatureGenerator(fastCommandSource, db).groupBy(100, _._1).via(anomalyScoreCalculator(slowCommandSource)).mergeSubstreams ~> merge
      bcast.out(3) ~> memoryFeatureGenerator(fastCommandSource, db).groupBy(100, _._1).via(anomalyScoreCalculator(slowCommandSource)).mergeSubstreams ~> merge
      merge.out

      FlowShape(bcast.in, merge.out)
    }
  )


  def kafkaSource(consumerSettings: ConsumerSettings[Array[Byte], Array[Byte]], topic: String) = Source.fromGraph(
    GraphDSL.create() { implicit graph =>
      val kafkaSource = Consumer.committableSource(consumerSettings, Subscriptions.topics(topic)).map{ msg =>
        val bais = new ByteArrayInputStream(msg.record.value())
        val reader = new SpecificDatumReader(classOf[com.bbn.tc.schema.avro.cdm17.TCCDMDatum])
        val decoder = DecoderFactory.get.binaryDecoder(bais, null)
        val elem: com.bbn.tc.schema.avro.cdm17.TCCDMDatum = reader.read(null, decoder)
        val cdm = new RawCDM17Type(elem.getDatum)
        msg.committableOffset.commitScaladsl()
        cdm
      }.map(CDM17.parse)
      SourceShape(kafkaSource.shape.out)
    }
  )


  type MilliSeconds = Long
  type NanoSeconds = Long

  implicit class EventCollection(es: Iterable[Event]) {
    def timeBetween(first: Option[EventType], second: Option[EventType]): NanoSeconds = {
      val foundFirst = if (first.isDefined) es.dropWhile(_.eventType != first.get) else es
      val foundSecond = if (second.isDefined) foundFirst.drop(1).find(_.eventType == second.get) else es.lastOption
      foundFirst.headOption.flatMap(f => foundSecond.map(s => s.timestampNanos - f.timestampNanos)).getOrElse(0L)
    }

    def sizePerSecond(t: EventType): Float = {
      val events = es.filter(_.eventType == t)
      val lengthOpt = events.headOption.flatMap(h => events.lastOption.map(l => l.timestampNanos / 1e9 - (h.timestampNanos / 1e9)))
      val totalSize = events.toList.map(_.size.getOrElse(0L)).sum
      lengthOpt.map(l => if (l > 0D) totalSize / l else 0D).getOrElse(0D).toFloat
    }
  }
}



trait ProcessingCommand extends CDM17
case class AdaptProcessingInstruction(id: Long) extends ProcessingCommand
case object EmitCmd extends ProcessingCommand
case object CleanUp extends ProcessingCommand




object TestGraph extends App {
  implicit val system = ActorSystem("test")
  implicit val ec = system.dispatcher
  implicit val mat = ActorMaterializer()


  val path = "/Users/erin/Documents/proj/adapt/git/adapt/data/ta1-theia-bovia-cdm17.bin" // cdm17_0407_1607.bin" //ta1-clearscope-cdm17.bin"  //
  val data = CDM17.readData(path, None).get._2.map(_.get)
  val source = Source.fromIterator[CDM17](() => CDM17.readData(path, None).get._2.map(_.get))
//    .concat(Source.fromIterator[CDM17](() => CDM17.readData(path + ".1", None).get._2.map(_.get)))
//    .concat(Source.fromIterator[CDM17](() => CDM17.readData(path + ".2", None).get._2.map(_.get)))
    .via(FlowComponents.printCounter("CDM Source", 1e6.toInt))
//    .via(Streams.titanWrites(graph))

  println("Total CDM statements: " + data.length)


//  // TODO: this should be a single source (instead of multiple copies) that broadcasts into all the necessary places.
//  val fastCommandSource = Source.tick[ProcessingCommand](6 seconds, 6 seconds, CleanUp).buffer(1, OverflowStrategy.backpressure)
//    .merge(Source.tick[ProcessingCommand](20 seconds, 20 seconds, Emit).buffer(1, OverflowStrategy.backpressure))
////    .via(FlowComponents.printCounter("Command Source", 1))
//
//  val slowCommandSource = Source.tick[ProcessingCommand](30 seconds, 30 seconds, CleanUp).buffer(1, OverflowStrategy.backpressure)
//    .merge(Source.tick[ProcessingCommand](50 seconds, 50 seconds, Emit).buffer(1, OverflowStrategy.backpressure))


  val dbFilePath = "/tmp/map.db"
  val db = DBMaker.fileDB(dbFilePath).fileMmapEnable().make()
  new File(dbFilePath).deleteOnExit()  // Only meant as ephemeral on-disk storage.


//  TitanFlowComponents.titanWrites(TitanFlowComponents.graph)
//    .runWith(source.via(FlowComponents.printCounter("titan write count", 1)), Sink.ignore)

//  FlowComponents.testNetFlowFeatureExtractor(commandSource, db)
//    .runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/netFlowFeatures.csv"))

//  FlowComponents.fileFeatureGenerator(commandSource, db)
//    .runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/fileFeatures.csv"))



//  Flow[CDM17].collect{ case e: Event => e }.groupBy(Int.MaxValue, _.toString).mergeSubstreams.via(FlowComponents.printCounter[Event]("Event counter", 100)).recover{ case e: Throwable => e.printStackTrace()}.runWith(source, Sink.ignore)



  // Print out all unique property/edge keys and their corresponding types. Should be done for all 6 TA1 teams in the same source!
//  source.statefulMapConcat{ () =>
//    val seen = mutable.Set.empty[String];
//    { case d: DBNodeable =>
//      val keys: List[String] = (d.asDBKeyValues.grouped(2).map(t => t.head.toString + " : " + t(1).getClass.getCanonicalName) ++ d.asDBEdges.map(_._1 + " : -[Edge]->")).toList
//      val newOnes = keys.filter(s => !seen.contains(s))
//      newOnes.map { n => seen += n; n } }
//  }.recover{ case e: Throwable => e.printStackTrace() }.runForeach(println)


    Flow[CDM17].runWith(source, TitanFlowComponents.titanWrites(TitanFlowComponents.graph))





  //  FlowComponents.normalizedScores(db).statefulMapConcat { () =>
//    val rankedPendingQueries = mutable.SortedSet.empty[(String, UUID, Double)](Ordering.by(1D - _._3))
//
//    {
//      case msg @ ("File", _, _) => List.empty
//      case msg @ ("NetFlow", _, _) => List.empty
//      case msg @ ("Process", _, _) => List.empty
//    }
//  }.recover{ case e: Throwable => e.printStackTrace()}.runWith(source, Sink.foreach(println))


//      .recover{ case e: Throwable => e.printStackTrace() } ~> printSink

//    FlowComponents.testNetFlowFeatureExtractor(fastCommandSource, db)
//      .via(FlowComponents.anomalyScoreCalculator(slowCommandSource))
//      .runWith(source, printSink)

//  FlowComponents.testNetFlowFeatureExtractor(fastCommandSource, db)
//    .via(FlowComponents.anomalyScoreCalculator(slowCommandSource))
//    .recover{ case e: Throwable => e.printStackTrace() }
//    .runWith(source, printSink)

//  FlowComponents.fileFeatureGenerator(fastCommandSource, db)
//    .via(FlowComponents.anomalyScoreCalculator(slowCommandSource))
//    .recover{ case e: Throwable => e.printStackTrace() }
//    .runWith(source, printSink)


//  FlowComponents.processFeatureGenerator(fastCommandSource, db)
//    .via(FlowComponents.anomalyScoreCalculator(slowCommandSource))
//    .recover{ case e: Throwable => e.printStackTrace() }
//    .runWith(source.take(3000000), printSink)

//    .runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/processFeatures.csv"))

//  FlowComponents.testFileFeatureExtractor(commandSource, db)
//    .runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/fileFeatures.csv"))
//




//  FlowComponents.testFileFeatureExtractor(commandSource, db)
//    .runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/fileFeatures.csv"))

//  FlowComponents.testNetFlowFeatureExtractor(commandSource, db)
//    .runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/netflowFeatures.csv"))

//  FlowComponents.testProcessFeatureExtractor(commandSource, db)
//    .runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/processFeatures.csv"))

//  FlowComponents.test(commandSource, db).runWith(source, FlowComponents.csvFileSink("/Users/ryan/Desktop/netflowFeatures9000.csv"))



//  FlowComponents.normalizedScores(db, 2, 4, 8, 20)
//    .via(Flow.fromGraph(QueryCollector(0.5D, 10000)))
//    .throttle(1, 2 seconds, 1, ThrottleMode.shaping)
//    .recover { case e: Throwable => e.printStackTrace() }
//    .runWith(source, Sink.foreach(println))

}


object KafkaStreams {

  //  val producerSettings = ProducerSettings(config.getConfig("akka.kafka.producer"), new ByteArraySerializer, new ByteArraySerializer)

  def kafkaProducer(file: String, producerSettings: ProducerSettings[Array[Byte], Array[Byte]], topic: String) = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit graph =>
      //      val datums: Iterator[com.bbn.tc.schema.avro.cdm17.TCCDMDatum] = CDM17.readAvroAsTCCDMDatum(file)
      Source.fromIterator(() => CDM17.readAvroAsTCCDMDatum(file)).map(elem => {
        val baos = new ByteArrayOutputStream
        val writer = new SpecificDatumWriter(classOf[com.bbn.tc.schema.avro.cdm17.TCCDMDatum])
        val encoder = EncoderFactory.get.binaryEncoder(baos, null)
        writer.write(elem, encoder)
        encoder.flush()
        baos.toByteArray
      }).map(elem => new ProducerRecord[Array[Byte], Array[Byte]](topic, elem)) ~> Producer.plainSink(producerSettings)

      ClosedShape
    }
  )

  def kafkaIngest(consumerSettings: ConsumerSettings[Array[Byte], Array[Byte]], topic: String) = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit graph =>
      Consumer.committableSource(consumerSettings, Subscriptions.topics(topic)).map{ msg =>
        val bais = new ByteArrayInputStream(msg.record.value())
        val reader = new SpecificDatumReader(classOf[com.bbn.tc.schema.avro.cdm17.TCCDMDatum])
        val decoder = DecoderFactory.get.binaryDecoder(bais, null)
        val elem: com.bbn.tc.schema.avro.cdm17.TCCDMDatum = reader.read(null, decoder)
        val cdm = new RawCDM17Type(elem.getDatum)
        msg.committableOffset.commitScaladsl()
        cdm
      }.map(CDM17.parse) ~> Sink.foreach[Try[CDM17]](x => println(s"kafka ingest end of the line for: $x")) //.map(println) ~> Sink.ignore
      ClosedShape
    }
  )
}


case class QueryCollector(scoreThreshhold: Double, queueLimit: Int) extends GraphStage[FlowShape[(String,UUID,Double,Set[UUID]),(String,UUID,Double,Set[UUID])]] {

  val in = Inlet[(String,UUID,Double,Set[UUID])]("Name -> UUID -> Score -> Subgraph")
  val out = Outlet[(String,UUID,Double,Set[UUID])]("Graph Query Package")
  val shape = FlowShape.of(in, out)
  def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {

    var priorityQueue = mutable.SortedSet.empty[(String, UUID, Double, Set[UUID])](Ordering.by[(String, UUID, Double, Set[UUID]), Double](_._3).reverse)
    val alreadySent = mutable.Set.empty[UUID]

    def popAndSend() = if (priorityQueue.nonEmpty) {
      println(s"Queue size: ${priorityQueue.size}")
      if ( ! alreadySent.contains(priorityQueue.head._2)) {
        push(out, priorityQueue.head)
        alreadySent += priorityQueue.head._2
      }
      priorityQueue = priorityQueue.tail
    }

    setHandler(in, new InHandler {
      def onPush() = {
        val scored = grab(in)
        if (scored._3 >= scoreThreshhold && ! alreadySent.contains(scored._2)) {  // TODO: revisit how to handle updated scores.
          priorityQueue += scored
          priorityQueue = priorityQueue.take(queueLimit)
        }
        if (isAvailable(out)) popAndSend()
        pull(in)
      }
    })

    setHandler(out, new OutHandler {
      def onPull() = {
        popAndSend()
        if ( ! hasBeenPulled(in)) pull(in)
      }
    })

  }

}





sealed trait JoinMultiplicity
case object One extends JoinMultiplicity
case object Many extends JoinMultiplicity

case class Join[A,B,K](
  in0Key: A => K,
  in1Key: B => K,
  in0Multiplicity: JoinMultiplicity = Many, // will there be two elements streamed for which 'in0Key' produces the same value
  in1Multiplicity: JoinMultiplicity = Many  // will there be two elements streamed for which 'in1Key' produces the same value
) extends GraphStage[FanInShape2[A, B, (K,A,B)]] {
  val shape = new FanInShape2[A, B, (K,A,B)]("Join")
  
  def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {

    val in0Stored = MutableMap.empty[K,Set[A]]
    val in1Stored = MutableMap.empty[K,Set[B]]

    setHandler(shape.in0, new InHandler {
      def onPush() = {
        val a: A = grab(shape.in0)
        val k: K = in0Key(a)

        in1Stored.getOrElse(k,Set()) match {
          case s if s.isEmpty =>
            in0Stored(k) = in0Stored.getOrElse(k,Set[A]()) + a

          case bs =>
            for (b <- bs)
              push(shape.out, (k,a,b))

            if (in0Multiplicity == One)
              in0Stored -= k

            if (in1Multiplicity == One)
              in1Stored -= k
        }
      }
    })

    setHandler(shape.in1, new InHandler {
      def onPush() = {
        val b: B = grab(shape.in1)
        val k: K = in1Key(b)

        in0Stored.getOrElse(k,Set()) match {
          case s if s.isEmpty =>
            in1Stored(k) = in1Stored.getOrElse(k,Set[B]()) + b

          case as =>
            for (a <- as)
              push(shape.out, (k,a,b))

            if (in0Multiplicity == One)
              in0Stored -= k

            if (in1Multiplicity == One)
              in1Stored -= k
        }
      }
    })

    setHandler(shape.out, new OutHandler {
      def onPull() = {
        if (!hasBeenPulled(shape.in0)) pull(shape.in0)
        if (!hasBeenPulled(shape.in1)) pull(shape.in1)
      }
    })
  }
}
