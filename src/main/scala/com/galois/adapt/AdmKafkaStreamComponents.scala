package com.galois.adapt

import com.galois.adapt.adm.{ADM, EdgeAdm2Adm}

import java.io.{ByteArrayInputStream, File, FileNotFoundException}
import java.util.UUID

import akka.kafka.scaladsl._
import akka.Done
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import com.typesafe.config.ConfigFactory
import akka.kafka._
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization._
import scala.concurrent.Future

import com.rrwright.quine.language.BoopickleScheme._
import com.rrwright.quine.language.PickleScheme

object AdmKafkaStreamComponents {
  private val eitherAdmOrEdgeScheme = implicitly[PickleScheme[Either[ADM, EdgeAdm2Adm]]]

  private val kafkaProducerJavaConfig = ConfigFactory.load().getConfig("akka.kafka.producer")
  def kafkaAdmSink(topicName: String): Sink[Either[ADM, EdgeAdm2Adm], Future[Done]] =
    Producer
      .plainSink(
        ProducerSettings(kafkaProducerJavaConfig, new ByteArraySerializer, new ByteArraySerializer)
      )
      .contramap { e: Either[ADM, EdgeAdm2Adm] =>
        new ProducerRecord(topicName, eitherAdmOrEdgeScheme.write(e))
      }

  private val kafkaConsumerJavaConfig = ConfigFactory.load().getConfig("akka.kafka.consumer")
  def kafkaAdmSource(topicName: String): Source[Either[ADM, EdgeAdm2Adm], Consumer.Control] =
    Consumer                                                                
      .plainSource(
        ConsumerSettings(kafkaConsumerJavaConfig, new ByteArrayDeserializer, new ByteArrayDeserializer),
        Subscriptions.assignmentWithOffset(new TopicPartition(topicName, 0), offset = 0)
      )
      .map { c: ConsumerRecord[Array[Byte], Array[Byte]] =>
        eitherAdmOrEdgeScheme.read(c.value())
      }
}
