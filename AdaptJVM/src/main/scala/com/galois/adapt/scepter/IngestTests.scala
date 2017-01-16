package com.galois.adapt.scepter

import java.nio.file.{Files, Paths}

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.galois.adapt.{AcceptanceApp, DevDBActor, Node, NodeQuery, Routes, Shutdown}
import com.galois.adapt.cdm13.{AbstractObject, CDM13}
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import com.galois.adapt.cdm13._

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

import scala.language.postfixOps

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


class General_TA1_Tests(
  data: => Iterator[Try[CDM13]],            // Input CDM statements
  count: Option[Int] = None                 // Expected number of statements
) extends FlatSpec {

  implicit val timeout = Timeout(1 second)

  // Test that all data gets parsed
  "Parsing data in the file..." should
  "parse successfully" in {
    var counter = 0
    val parseTry = Try(data.foreach { d =>
      counter += 1
      AcceptanceApp.distribute(d.get)
    })
    println(s"Read events: $counter")
    assert(parseTry.isSuccess)
  }

  // Test that we get the right number of statements out
  count.foreach { count => 
    it should s"have a known statement count of $count" in {
      assert(Await.result(AcceptanceApp.counterActor ? HowMany("total"), 1 second) == count)
    }
  }
  
  // Test that we get one of each type of statement
  val missing = AcceptanceApp.TA1Source match {
    case Some(SOURCE_ANDROID_JAVA_CLEARSCOPE) =>
      List(AbstractObject, MemoryObject, RegistryKeyObject, TagEntity, Value);
    case Some(SOURCE_LINUX_AUDIT_TRACE) =>
      List(AbstractObject, Value);
    case Some(SOURCE_FREEBSD_DTRACE_CADETS) =>
      List(AbstractObject, MemoryObject, ProvenanceTagNode, RegistryKeyObject, SrcSinkObject, TagEntity, Value); 
    case Some(SOURCE_WINDOWS_DIFT_FAROS) =>
      List(AbstractObject, MemoryObject, RegistryKeyObject, TagEntity, Value); 
    case Some(SOURCE_LINUX_THEIA) => 
      List(AbstractObject, Value); 
    case Some(SOURCE_WINDOWS_FIVEDIRECTIONS) => 
      List(AbstractObject, MemoryObject, TagEntity, Value);
    case _ =>
      List(AbstractObject);
  }
  (CDM13.values diff missing).foreach { typeName =>
    it should s"have at least one $typeName" in {
      assert {
        Await.result(
          AcceptanceApp.counterActor ? HowMany(typeName.toString), 1 second
        ).asInstanceOf[Int] > 0
      }
    }
  }
 
  // Tests for 'BasicOps.sh'
  "Looking for BasicOps events.." should "either find almost no events, or all events" in {
    
    val basicOps = Await.result(
      AcceptanceApp.basicOpsActor ? IsBasicOps, 1 second
    ).asInstanceOf[Option[Map[String,Boolean]]]
  
    basicOps.foreach { missing =>
      missing.foreach { case (msg,found) =>
        assert(found, msg)
      }
    }
  }
}


class CLEARSCOPE_Specific_Tests(totalNodes: Int, incompleteEdgeCount: Int) extends FlatSpec {
  "Analyzed data" should
    "contain a representative number of nodes" in {
      assert(totalNodes > 50000)
    }

  it should "have all incomplete edges resolved within CDM punctuation boundaries (or by the end of the file)" in {
    assert(incompleteEdgeCount == 0)
  }
}


trait TestEvaluationCases
case class HowMany(name: String) extends TestEvaluationCases

