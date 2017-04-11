package com.galois.adapt

import akka.actor._
import com.bbn.tc.schema.avro.cdm17.TCCDMDatum
import org.apache.avro.file.DataFileReader
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.util.Utf8

class KafkaWriter(val files: List[String]) extends Actor with ActorLogging {
  files.foreach { readAvroFile }

  def readAvroFile(file: String): Unit = {
    val tcDatumReader = new SpecificDatumReader(classOf[com.bbn.tc.schema.avro.cdm17.TCCDMDatum])
    val tcFileReader: DataFileReader[com.bbn.tc.schema.avro.cdm17.TCCDMDatum] = new DataFileReader(new java.io.File(file), tcDatumReader)
  }

  def receive = {
    case (file: String) =>
      readAvroFile(file)
  }
}
