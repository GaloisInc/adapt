package com.galois.adapt.scepter

import com.galois.adapt._
import com.galois.adapt.cdm13._

import java.nio.file.{Files, Paths}

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.structure.{Edge,Vertex}

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes._

import scala.collection.JavaConversions._
import scala.io.StdIn
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try
import scala.language.postfixOps

import org.scalatest.FlatSpec

class General_TA1_Tests(
  data: => Iterator[Try[CDM13]],            // Input CDM statements
  incompleteEdgeCount: Int,                 // Number of incomplete edges
  count: Option[Int] = None                 // Expected number of statements
) extends FlatSpec {

  implicit val timeout = Timeout(30 second)

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

  // Test to assert that there are no dangling edges
  "Data" should "have all incomplete edges resolved within CDM punctuation boundaries (or by the end of the file)" in {
    assert(incompleteEdgeCount == 0)
  }

  // Test deduplication of PIDs
  // TODO: revist this once the issue of PIDs wrapping around has been clarified with TA1s
  it should "not have duplicate PID's in process Subjects" in {
    val pids = Await.result(
      AcceptanceApp.dbActor ?
      AnyQuery("g.V().has(label,'Subject').has('subjectType','SUBJECT_PROCESS').values('pid')"),
      30 seconds
    ).asInstanceOf[Try[List[_]]].get
    
    for (pid <- pids) {
      val processesWithPID = Await.result(
        AcceptanceApp.dbActor ?
        AnyQuery(s"g.V().has('pid',$pid).has(label,'Subject').has('subjectType','SUBJECT_PROCESS').dedup().by('uuid')"),
        30 seconds
      ).asInstanceOf[Try[List[Vertex]]].get
      val uuidsOfProcessesWithPID = processesWithPID.take(20).map(_.value("uuid").toString).mkString("\n")
      
      assert(
        processesWithPID.length <= 1,
        s"Multiple process subjects share the PID $pid:\n$uuidsOfProcessesWithPID\n"
      )
    }
  }

  // Test deduplication of Files
  // TODO: revist this once issue of uniqueness of file objects has been clarified with TA1s
  it should "not have duplicate files" in {
    val files = Await.result(
      AcceptanceApp.dbActor ?
      AnyQuery("g.V().has(label,'FileObject').dedup()"),
      30 seconds
    ).asInstanceOf[Try[List[Vertex]]].get

    for (file <- files) {
      val urls = file.properties("url").toList
      if (urls.length > 0) {
        val url: String = urls(0).value()
        val version: Int = file.property("version").value()
        val filesWithUrl = Await.result(
          AcceptanceApp.dbActor ?
          AnyQuery(s"g.V().has(label,'FileObject').has('url','${Utility.escapePath(url)}').has('version',$version).dedup().by('uuid')"),
          30 seconds
        ).asInstanceOf[Try[List[Vertex]]].get
        val uuidsOfFilesWithUrlVersion = filesWithUrl.take(20).map(_.value("uuid").toString).mkString("\n")
        
        assert(
          filesWithUrl.length <= 1,
          s"Multiple files share the same url $url and version $version:\n$uuidsOfFilesWithUrlVersion\n"
        )
      }
    }
  }

} 

// Provider specific test classes:

class TRACE_Specific_Tests(val totalNodes: Int) extends FlatSpec {
  implicit val timeout = Timeout(1 second)
  val missing = List(AbstractObject, Value)
  val minimum = 50000

  // Test that we get one of each type of statement
  (CDM13.values diff missing).foreach { typeName =>
    "This provider" should s"have at least one $typeName" in {
      assert {
        Await.result(
          AcceptanceApp.counterActor ? HowMany(typeName.toString), 1 second
        ).asInstanceOf[Int] > 0
      }
    }
  }
  
  // Test that we have a minimum number of nodes
  "This provider" should "contain a representative number of nodes" in {
    assert(totalNodes > minimum)
  }

}

class CADETS_Specific_Tests(val totalNodes: Int) extends FlatSpec {
  implicit val timeout = Timeout(1 second)
  val missing = List(AbstractObject, MemoryObject, ProvenanceTagNode, RegistryKeyObject, SrcSinkObject,
  TagEntity, Value)
  val minimum = 50000  

  // Test that we get one of each type of statement
  (CDM13.values diff missing).foreach { typeName =>
    "This provider" should s"have at least one $typeName" in {
      assert {
        Await.result(
          AcceptanceApp.counterActor ? HowMany(typeName.toString), 1 second
        ).asInstanceOf[Int] > 0
      }
    }
  }
  
  // Test that we have a minimum number of nodes
  "This provider" should "contain a representative number of nodes" in {
    assert(totalNodes > minimum)
  }

}

class FAROS_Specific_Tests(val totalNodes: Int) extends FlatSpec {
  implicit val timeout = Timeout(1 second)
  val missing = List(AbstractObject, MemoryObject, RegistryKeyObject, TagEntity, Value) 
  val minimum = 50000
  
  // Test that we get one of each type of statement
  (CDM13.values diff missing).foreach { typeName =>
    "This provider" should s"have at least one $typeName" in {
      assert {
        Await.result(
          AcceptanceApp.counterActor ? HowMany(typeName.toString), 1 second
        ).asInstanceOf[Int] > 0
      }
    }
  }
  
  // Test that we have a minimum number of nodes
  "This provider" should "contain a representative number of nodes" in {
    assert(totalNodes > minimum)
  }
}

class THEIA_Specific_Tests(val totalNodes: Int) extends FlatSpec {
  implicit val timeout = Timeout(1 second)
  val missing = List(AbstractObject, Value)
  val minimum = 50000
  
  // Test that we get one of each type of statement
  (CDM13.values diff missing).foreach { typeName =>
    "This provider" should s"have at least one $typeName" in {
      assert {
        Await.result(
          AcceptanceApp.counterActor ? HowMany(typeName.toString), 1 second
        ).asInstanceOf[Int] > 0
      }
    }
  }
  
  // Test that we have a minimum number of nodes
  "This provider" should "contain a representative number of nodes" in {
    assert(totalNodes > minimum)
  }
}  

class FIVEDIRECTIONS_Specific_Tests(val totalNodes: Int) extends FlatSpec {
  implicit val timeout = Timeout(1 second)
  val missing = List(MemoryObject, TagEntity, Value)
  val minimum = 50000
   
  // Test that we get one of each type of statement
  (CDM13.values diff missing).foreach { typeName =>
    "This provider" should s"have at least one $typeName" in {
      assert {
        Await.result(
          AcceptanceApp.counterActor ? HowMany(typeName.toString), 1 second
        ).asInstanceOf[Int] > 0
      }
    }
  }
  
  // Test that we have a minimum number of nodes
  "This provider" should "contain a representative number of nodes" in {
    assert(totalNodes > minimum)
  }
}

class CLEARSCOPE_Specific_Tests(val totalNodes: Int) extends FlatSpec {
  implicit val timeout = Timeout(1 second)
  val missing = List(AbstractObject, MemoryObject, RegistryKeyObject, TagEntity, Value)
  val minimum = 50000
  
  // Test that we get one of each type of statement
  (CDM13.values diff missing).foreach { typeName =>
    "This provider" should s"have at least one $typeName" in {
      assert {
        Await.result(
          AcceptanceApp.counterActor ? HowMany(typeName.toString), 1 second
        ).asInstanceOf[Int] > 0
      }
    }
  }
  
  // Test that we have a minimum number of nodes
  "This provider" should "contain a representative number of nodes" in {
    assert(totalNodes > minimum)
  }
}


trait TestEvaluationCases
case class HowMany(name: String) extends TestEvaluationCases

object Utility {
  
  // Escape backslashes (common in Window's paths)
  def escapePath(path: String): String = path.flatMap {
    case '\\' => "\\\\"
    case '\'' => "\\\'"
    case c => s"$c" 
  }

}

