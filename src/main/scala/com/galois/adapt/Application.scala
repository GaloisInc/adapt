package com.galois.adapt

import java.io._

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.{Done, NotUsed}
import com.bbn.tc.schema.avro.{cdm20 => bbnCdm20}
import com.typesafe.config.ConfigFactory
import org.apache.avro.file.DataFileReader
import org.apache.avro.specific.{SpecificDatumReader, SpecificDatumWriter}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.language.postfixOps

object Application extends App {

  implicit val system = ActorSystem("adapt-kafka-producer")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  org.slf4j.LoggerFactory.getILoggerFactory  // This is here just to make SLF4j shut up and not log lots of error messages when instantiating the Kafka producer.

  def readAvroAsTCCDMDatum(filePath: String): Source[bbnCdm20.TCCDMDatum, NotUsed] = {
    val tcDatumReader = new SpecificDatumReader(classOf[bbnCdm20.TCCDMDatum])
    Source.fromIterator(() => {
      val tcFileReader: DataFileReader[bbnCdm20.TCCDMDatum] = new DataFileReader(new java.io.File(filePath), tcDatumReader)
      tcFileReader.iterator().asScala
    })
  }

  def writeTCCDatumKafka(topicName: String): Sink[bbnCdm20.TCCDMDatum, Future[Done]] = {
    val tcDatumWriter = new SpecificDatumWriter(classOf[bbnCdm20.TCCDMDatum])

    Producer
      .plainSink(
        ProducerSettings(system, new ByteArraySerializer, new ByteArraySerializer)
          .withBootstrapServers("localhost:9092")
      )
      .contramap { datum: bbnCdm20.TCCDMDatum =>
        import org.apache.avro.io.EncoderFactory

        val baos = new ByteArrayOutputStream
        val encoder = EncoderFactory.get.binaryEncoder(baos, null)
        tcDatumWriter.write(datum, encoder)
        encoder.flush()
        val elem = baos.toByteArray

        new ProducerRecord(topicName, elem)
      }
  }

  val topicName = args(0)
  println(s"The topic name is $topicName")

  args.drop(1).foldLeft[Future[Done]](Future.successful(Done)) { (acc, file) =>
    acc.flatMap { _ =>
      println(s"Ingesting $file into kafka...")
      var counter: Long = 0L

      val newAcc = readAvroAsTCCDMDatum(file)
        .via(Flow.fromFunction(cdm => {
          counter += 1
          if (counter % 10000 == 0) {
            println(s"  $counter records processed...")
          }
          cdm
        }))
        .runWith(writeTCCDatumKafka(topicName))

      newAcc.onComplete(_ => println(s"Done ingesting $file into kafka."))

      newAcc
    }
  }
}

