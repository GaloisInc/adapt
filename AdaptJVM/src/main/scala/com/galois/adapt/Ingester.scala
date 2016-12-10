package com.galois.adapt

import cdm13._
import com.bbn.tc.schema.avro.TCCDMDatum
import com.thinkaurelius.titan.core.TitanFactory
import org.apache.avro.file.DataFileReader
import org.apache.avro.specific.SpecificDatumReader

import scala.collection.JavaConverters._
import scala.util.{Success, Try}


case object Ingester extends App {
  val graph = TitanFactory.build.set("storage.backend", "inmemory").open
  val cdmData = readAvroFile("Engagement1DataCDM13/pandex/ta1-clearscope-cdm13_pandex.bin").get

  var x: Try[_] = Success()
  var counter = 0
  val start = System.currentTimeMillis
  while (x.isSuccess && cdmData.hasNext) {
    val data = cdmData.next
    x = parse(data)
    if (x.isFailure) println(s"ERROR:  ${data.o}")
    counter = counter + 1
  }
  val end = System.currentTimeMillis

  if (!cdmData.hasNext) println("COMPLETE")
  println(s"$counter, in ${end-start} milliseconds:   $x")
  println(s"...that's ${counter / ((end-start)/1000)} per second!")



  def readAvroFile(filePath: String) = Try {
    val tcDatumReader = new SpecificDatumReader(classOf[TCCDMDatum])
    val tcFileReader: DataFileReader[TCCDMDatum] = new DataFileReader(new java.io.File(filePath), tcDatumReader)
    tcFileReader.iterator.asScala.map(cdm => new RawCDM13Type(cdm.getDatum))
  }

  sys.exit()
}
