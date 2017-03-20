package com.galois.adapt.scepter

import com.galois.adapt._
import com.galois.adapt.cdm15._

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.structure.{Edge,Vertex}
import org.apache.tinkerpop.gremlin.process.traversal.P

import akka.pattern.ask
import akka.actor.{ActorRef, ActorSystem, Props, Actor, ActorLogging}
import akka.util.Timeout

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try
import scala.language.postfixOps

import org.scalatest.FlatSpec

class General_TA1_Tests(
  failedStatements: Int,                    // Number of failed events
  failedStatementsMsgs: List[String],       // First 5 failed messages TODO: use this?
  incompleteEdgeCount: Int,                 // Number of incomplete edges
  graph: TinkerGraph,
  ta1Source: Option[InstrumentationSource],
  toDisplay: scala.collection.mutable.ListBuffer[String]
) extends FlatSpec {

  implicit val timeout = Timeout(30 second)
  
  val colors: Iterator[(String,String)] = Iterator.continually(Map(
    "000" -> Console.BLACK,
    "00F" -> Console.BLUE,
    "0F0" -> Console.GREEN,
    "0FF" -> Console.CYAN,
    "F0F" -> Console.MAGENTA,
    "FF0" -> Console.YELLOW
  )).flatten


  // Test that all data gets parsed
  "Parsing data in the file..." should "parse successfully" in {
    assert(failedStatements == 0)
    if (failedStatements != 0)
      println(failedStatementsMsgs.mkString("\n"))
  }

  // Test to assert that there are no dangling edges
  "Data" should "have all incomplete edges resolved within CDM punctuation boundaries (or by the end of the file)" in {
    assert(incompleteEdgeCount == 0)
  }

  // Test tht events of type SEND,SENDMSG,READ,etc. have a size field on them
  it should "have a non-null size field on events of type SENDTO, SENDMSG, WRITE, READ, RECVMSG, RECVFROM" in {
    val eventsShouldHaveSize: java.util.List[Vertex] = graph.traversal().V()
        .hasLabel("Event")
        .has("eventType",P.within("EVENT_SENDTO", "EVENT_SENDMSG", "EVENT_WRITE", "EVENT_READ", "EVENT_RECVMSG", "EVENT_RECVFROM"))
        .hasNot("size")
        .dedup()
        .toList()
    
    if (eventsShouldHaveSize.length <= 1) {
      assert(eventsShouldHaveSize.length <= 1)
    } else {
      val (code,color) = colors.next()
      toDisplay += s"g.V(${eventsShouldHaveSize.map(_.id().toString).mkString(",")}):$code"
      val uuidsOfEventsShouldHaveSize = eventsShouldHaveSize.take(20).map(_.value("uuid").toString).mkString("\n" + color)
      
      assert(
        eventsShouldHaveSize.length <= 1,
        s"\nSome events of type SENDTO/SEND/SENDMSG/WRITE/READ/RECVMSG/RECVFROM don't have a 'size':\n$color$uuidsOfEventsShouldHaveSize${Console.RED}\n"
      )
    }
  }

  // Test uniqueness of... UUIDs
  // Some providers have suggested that they may reuse UUIDs. That would be bad.
  it should "not have any duplicate UUIDs" in {
    val grouped: java.util.List[java.util.Map[java.util.UUID,java.lang.Long]] = graph.traversal().V()
      .values("uuid")
      .groupCount[java.util.UUID]()
      .toList()

    val offending: List[(java.util.UUID,java.lang.Long)] = grouped.get(0).toList.filter(u_c => u_c._2 <= 1).take(20)
    for ((uuid,count) <- offending) {
      assert(
        count <= 1,
        s"Multiple nodes ($count nodes int this case) should not share the same UUID $uuid"
      )
    }
  }

  // Test deduplication of PIDs
  // TODO: revist this once the issue of PIDs wrapping around has been clarified with TA1s
  it should "not have duplicate PID's in process Subjects" in {
    val pids = graph.traversal().V().hasLabel("Subject")
      .has("subjectType","SUBJECT_PROCESS")
      .dedup()
      .values("pid")

    while (pids.hasNext()) {
      val pid: Int = pids.next()

      val processesWithPID: java.util.List[Vertex] = graph.traversal().V()
        .has("pid", pid)
        .hasLabel("Subject")
        .has("subjectType","SUBJECT_PROCESS")
        .dedup()
        .toList()
      
      if (processesWithPID.length <= 1) {
        assert(processesWithPID.length <= 1)
      } else {
        val (code,color) = colors.next()
        toDisplay += s"g.V(${processesWithPID.map(_.id().toString).mkString(",")}):$code"
        val uuidsOfProcessesWithPID = processesWithPID.take(20).map(_.value("uuid").toString).mkString("\n" + color)
      
        assert(
          processesWithPID.length <= 1,
          s"\nMultiple process subjects share the PID$pid:\n$color$uuidsOfProcessesWithPID${Console.RED}\n"
        )
      }
    }
  }

  // Test deduplication of Files
  // TODO: revist this once issue of uniqueness of file objects has been clarified with TA1s
  it should "not contain separate nodes (i.e. different UUIDs) that have the same file path" in {
    val files = graph.traversal().V().hasLabel("FileObject").dedup()

    while (files.hasNext) {
      val file: Vertex = files.next()

      val urls = file.properties("url").toList
      if (urls.nonEmpty) {
        val url: String = urls.head.value()
        val version: Int = file.property("version").value()
        val filesWithUrl: java.util.List[Vertex] =
          graph.traversal().V().hasLabel("FileObject")
           .has("url",url)
           .has("version",version)
           .dedup()
           .by("uuid")
           .toList
        
        
        if (filesWithUrl.length <= 1) {
          assert(filesWithUrl.length <= 1)
        } else {
          val (code,color) = colors.next()
          toDisplay += s"g.V(${filesWithUrl.map(_.id().toString).mkString(",")}):$code"
          val uuidsOfFilesWithUrlVersion = filesWithUrl.take(20).map(_.value("uuid").toString).mkString("\n" + color)
        
          assert(
            filesWithUrl.length <= 1,
            s"\nMultiple files share the same url $url and version$version:\n$color$uuidsOfFilesWithUrlVersion${Console.RED}\n"
          )
        }
      }
    }
  } 
} 

// Provider specific test classes:

class TRACE_Specific_Tests(val graph: TinkerGraph) extends FlatSpec {
  implicit val timeout = Timeout(1 second)
  val missing = List(AbstractObject, Value)
  val minimum = 50000

  // Test that we get one of each type of statement
  (CDM15.values diff missing).foreach { typeName =>
    "This provider" should s"have at least one $typeName" in {
      assert(graph.traversal().V().hasLabel(typeName.toString).count().next() > 0)
    }
  }
  
  // Test that we have a minimum number of nodes
  "This provider" should "contain a representative number of nodes" in {
    assert(graph.traversal().V().count().next() > minimum)
  }
}

class CADETS_Specific_Tests(val graph: TinkerGraph) extends FlatSpec {
  implicit val timeout = Timeout(1 second)
  val missing = List(AbstractObject, MemoryObject, ProvenanceTagNode, RegistryKeyObject, SrcSinkObject, Value)
  val minimum = 50000  

  // Test that we get one of each type of statement
  (CDM15.values diff missing).foreach { typeName =>
    "This provider" should s"have at least one $typeName" in {
      assert(graph.traversal().V().hasLabel(typeName.toString).count().next() > 0)
    }
  }
  
  // Test that we have a minimum number of nodes
  "This provider" should "contain a representative number of nodes" in {
    assert(graph.traversal().V().count().next() > minimum)
  }
}

class FAROS_Specific_Tests(val graph: TinkerGraph) extends FlatSpec {
  implicit val timeout = Timeout(1 second)
  val missing = List(AbstractObject, MemoryObject, RegistryKeyObject, Value)
  val minimum = 50000

  // Test that we get one of each type of statement
  (CDM15.values diff missing).foreach { typeName =>
    "This provider" should s"have at least one $typeName" in {
      assert(graph.traversal().V().hasLabel(typeName.toString).count().next() > 0)
    }
  }
  
  // Test that we have a minimum number of nodes
  "This provider" should "contain a representative number of nodes" in {
    assert(graph.traversal().V().count().next() > minimum)
  }
}

class THEIA_Specific_Tests(val graph: TinkerGraph) extends FlatSpec {
  implicit val timeout = Timeout(1 second)
  val missing = List(AbstractObject, Value)
  val minimum = 50000

  // Test that we get one of each type of statement
  (CDM15.values diff missing).foreach { typeName =>
    "This provider" should s"have at least one $typeName" in {
      assert(graph.traversal().V().hasLabel(typeName.toString).count().next() > 0)
    }
  }
  
  // Test that we have a minimum number of nodes
  "This provider" should "contain a representative number of nodes" in {
    assert(graph.traversal().V().count().next() > minimum)
  }
}  

class FIVEDIRECTIONS_Specific_Tests(val graph: TinkerGraph) extends FlatSpec {
  implicit val timeout = Timeout(1 second)
  val missing = List(MemoryObject, Value)
  val minimum = 50000

  // Test that we get one of each type of statement
  (CDM15.values diff missing).foreach { typeName =>
    "This provider" should s"have at least one $typeName" in {
      assert(graph.traversal().V().hasLabel(typeName.toString).count().next() > 0)
    }
  }
  
  // Test that we have a minimum number of nodes
  "This provider" should "contain a representative number of nodes" in {
    assert(graph.traversal().V().count().next() > minimum)
  }
}

class CLEARSCOPE_Specific_Tests(val graph: TinkerGraph) extends FlatSpec {
  implicit val timeout = Timeout(1 second)
  val missing = List(AbstractObject, MemoryObject, RegistryKeyObject, Value)
  val minimum = 50000

  // Test that we get one of each type of statement
  (CDM15.values diff missing).foreach { typeName =>
    "This provider" should s"have at least one $typeName" in {
      assert(graph.traversal().V().hasLabel(typeName.toString).count().next() > 0)
    }
  }
  
  // Test that we have a minimum number of nodes
  "This provider" should "contain a representative number of nodes" in {
    assert(graph.traversal().V().count().next() > minimum)
  }
}


object Utility {
  
  // Escape backslashes (common in Window's paths)
  def escapePath(path: String): String = path.flatMap {
    case '\\' => "\\\\"
    case '\'' => "\\\'"
    case c => s"$c" 
  }

}

