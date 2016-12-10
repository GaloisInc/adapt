package com.galois.adapt

import com.bbn.tc.schema.avro.{Event, ProvenanceTagNode, Subject}
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._


//object IngestTest extends App {
//  val config = ConfigFactory.load("test")
//  println(config.getInt("foo.bar"))
//}

import org.scalacheck.Properties
import org.scalacheck.Prop.forAll

//object StringSpecification extends Properties("String") {
//
//  property("startsWith") = forAll { (a: String, b: String) =>
//    (a+b).startsWith(a)
//  }
//
////  property("concatenate") = forAll { (a: String, b: String) =>
////    (a+b).length > a.length && (a+b).length > b.length
////  }
//
//  property("substring") = forAll { (a: String, b: String, c: String) =>
//    (a+b+c).substring(a.length, a.length+b.length) == b
//  }
//
//}


import org.scalatest.FlatSpec

class IngestSpec extends FlatSpec {
  "Ingesting infoleak_small_units.avro" should "successfully ingest X events" in {
    val results = Ingest.readAvroFile("src/test/resources/Avro_datasets_with_units_CDM13/infoleak_small_units.avro").asScala
//    println(results.map(_.get("datum")).toSet)
    assert(results.count(_.isInstanceOf[Event]) == 3764)
  }
}

class SetSpec extends FlatSpec {

  "An empty Set" should "have size 0" in {
    assert(Set.empty.size == 0)
  }

  it should "produce NoSuchElementException when head is invoked" in {
    assertThrows[NoSuchElementException] {
      Set.empty.head
    }
  }
}

object Run extends App {
  val results = Ingest.readAvroFile("src/test/resources/Avro_datasets_with_units_CDM13/infoleak_small_units.avro").asScala
  println(results.head.get("datum"))
}