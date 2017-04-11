package com.galois.adapt

import akka.actor._


class KafkaWriter(val files: List[String]) extends Actor with ActorLogging {
  files.foreach { readAvroFile }

  def readAvroFile(file: String): Unit {}

  def receive = {
    case (file: String) =>
      readAvroFile(file)
  }
}
