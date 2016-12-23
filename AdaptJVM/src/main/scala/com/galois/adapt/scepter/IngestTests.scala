package com.galois.adapt.scepter

import java.nio.file.{Files, Paths}

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.galois.adapt.{AcceptanceApp, DatabaseActor, Node, NodeQuery, Routes, Shutdown}
import com.galois.adapt.cdm13.CDM13
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes._

import scala.io.StdIn


//object IngestTest extends App {
//  val config = ConfigFactory.load("test")
//  println(config.getInt("foo.bar"))
//}

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

//class IngestSpec extends FlatSpec {
//  "Ingesting infoleak_small_units.avro" should "successfully ingest X events" in {
//    val results = Ingest.readAvroFile("src/test/resources/Avro_datasets_with_units_CDM13/infoleak_small_units.avro").asScala
////    println(results.map(_.get("datum")).toSet)
//    assert(results.count(_.isInstanceOf[Event]) == 3764)
//  }
//}
//
//class SetSpec extends FlatSpec {
//
//  "An empty Set" should "have size 0" in {
//    assert(Set.empty.size == 0)
//  }
//
//  it should "produce NoSuchElementException when head is invoked" in {
//    assertThrows[NoSuchElementException] {
//      Set.empty.head
//    }
//  }
//}


class ParsingTests(data: => Iterator[Try[CDM13]], count: Option[Int] = None) extends FlatSpec {
  implicit val timeout = Timeout(1 second)

  "Parsing data in the file..." should
  "parse successfully" in {
    var counter = 0
    val parseTry = Try(data.foreach { d =>
      counter += 1
      print(s"Reading data: $counter \r")
      AcceptanceApp.distribute(d.get)
    })
    println(s"Read events: $counter")
    assert(parseTry.isSuccess)
  }

  if (count.isDefined) {
    it should s"have a known statement count of ${count.get}" in {
      assert(Await.result(AcceptanceApp.counterActor ? HowMany("total"), 1 second) == count.get)
    }
  }

  CDM13.values.foreach { typeName =>
    it should s"have at least one $typeName" in {
      assert {
        Await.result(
          AcceptanceApp.counterActor ? HowMany(typeName.toString), 1 second
        ).asInstanceOf[Int] > 0
      }
    }
  }
}

trait TestEvaluationCases
case class HowMany(name: String) extends TestEvaluationCases

