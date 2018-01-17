package com.galois.adapt.scepter

import java.util.UUID

import akka.util.Timeout
import com.galois.adapt.cdm17._
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.scalatest.FlatSpec

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Success, Try}


class General_TA1_Tests(
  failedStatements: Int,                    // Number of failed events
  failedStatementsMsgs: List[String],       // First 5 failed messages
  incompleteEdges: Map[UUID, List[(Vertex,String)]],
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
  "Data in this data set" should "have all incomplete edges resolved within CDM punctuation boundaries (or by the end of the file)" in {
    val incompleteCount = incompleteEdges.size
    if (incompleteCount == 0) assert(incompleteCount == 0)
    else {
      val (code, color) = colors.next()
      val edgesToPrint = incompleteEdges.toList.flatMap { case (u,l) => l map { case (o,s) => s"${o.value("uuid")} --> $u"}}.mkString("\n" + color) + "\n"
      toDisplay += s"g.V(${incompleteEdges.flatMap(_._2.map(_._1.id().toString)).mkString(",")}):$code"
      val message = s"\nThe following edges (ExistingUUID --> MissingUUID) are all referenced in this data set, but nodes with these MissingUUIDs do not exist in this dataset:\n$color$edgesToPrint${Console.RED}\n"
      assert(incompleteCount == 0, message)
    }
  }

  // Test that all subjects have a "parentSubject" edge
  "Subjects" should "have a 'parentSubject'" in {
    val subjectsWithoutParents: java.util.List[Vertex] = graph.traversal().V()
        .hasLabel("Subject")
        .hasNot("parentSubjectUuid")
        .toList

    if (subjectsWithoutParents.isEmpty) {
      assert(subjectsWithoutParents.length == 0)
    } else {
      val (code, color) = colors.next()
      toDisplay += s"g.V(${subjectsWithoutParents.map(_.id().toString).mkString(",")}):$code"
      val uuidsOfSubjectsWithoutParent = subjectsWithoutParents.take(20).map(_.value("uuid").toString).mkString("\n" + color)

      assert(
        subjectsWithoutParents.length == 0,
        s"\nSome subjects don't have a 'parentSubject':\n$color$uuidsOfSubjectsWithoutParent${Console.RED}\n"
      )
    }
  }



  // Test that events of type SEND,SENDMSG,READ,etc. have a size field on them
  if ( ! ta1Source.contains(SOURCE_WINDOWS_FIVEDIRECTIONS)) {
    it should "have a non-null size field on events of type SENDTO, SENDMSG, WRITE, READ, RECVMSG, RECVFROM" in {
      val eventsShouldHaveSize: java.util.List[Vertex] = graph.traversal().V()
        .hasLabel("Event")
        .has("eventType", P.within("EVENT_SENDTO", "EVENT_SENDMSG", "EVENT_WRITE", "EVENT_READ", "EVENT_RECVMSG", "EVENT_RECVFROM"))
        .hasNot("size")
        .dedup()
        .toList

      if (eventsShouldHaveSize.length <= 1) {
        assert(eventsShouldHaveSize.length <= 1)
      } else {
        val (code, color) = colors.next()
        toDisplay += s"g.V(${eventsShouldHaveSize.map(_.id().toString).mkString(",")}):$code"
        val uuidsOfEventsShouldHaveSize = eventsShouldHaveSize.take(20).map(_.value("uuid").toString).mkString("\n" + color)

        assert(
          eventsShouldHaveSize.length <= 1,
          s"\nSome events of type SENDTO/SEND/SENDMSG/WRITE/READ/RECVMSG/RECVFROM don't have a 'size':\n$color$uuidsOfEventsShouldHaveSize${Console.RED}\n"
        )
      }
    }
  }

  // Test uniqueness of... UUIDs
  // Some providers have suggested that they may reuse UUIDs. That would be bad.
  it should "not have any duplicate UUIDs" in {
    val grouped: java.util.List[java.util.Map[java.util.UUID,java.lang.Long]] = graph.traversal().V()
      .values("uuid")
      .groupCount[java.util.UUID]()
      .toList

    val offending: List[(java.util.UUID,java.lang.Long)] = grouped.get(0).toList.filter(u_c => u_c._2 > 1).take(20)
    for ((uuid,count) <- offending) {
      assert(
        count <= 1,
        s"Multiple nodes ($count nodes int this case) should not share the same UUID $uuid"
      )
    }
  }

  if (graph.traversal().V().hasLabel("UnitDependency").count().next() > 0L) {
    it should "contain UnitDependency statements which connect only SUBJECT_UNITs that have a common parent, and not as general edges" in {
      val unitDepIds = graph.traversal().V().hasLabel("UnitDependency").asScala.toSet[Vertex].map(v => v.id().asInstanceOf[Long] -> (v.value("unit").asInstanceOf[UUID], v.value("dependentUnit").asInstanceOf[UUID]))
      val unitEvals = unitDepIds map { case (id,uuids) =>
        val unitTry = Try(graph.traversal().V().has("uuid",uuids._1).next())
        val dependentUnitTry = Try(graph.traversal().V().has("uuid",uuids._2).next())
        val isValid = for {
          unit <- unitTry
          dependentUnit <- dependentUnitTry
        } yield {
          val areUnitTypes = Try(unit.value("subjectType").asInstanceOf[SubjectType]) == Success(SUBJECT_UNIT) && Try(dependentUnit.value("subjectType").asInstanceOf[SubjectType]) == Success(SUBJECT_UNIT)
          val l = Try(unit.value("parentSubject"))
          val r = Try(dependentUnit.value("parentSubject"))
          val vsHaveSameParent = l.isSuccess && l == r
          areUnitTypes && vsHaveSameParent
        }
        (id, uuids) -> isValid.getOrElse(false)
      }
      val offendingUnits = unitEvals.toList.collect { case (ids,b) if ! b => ids}
      val onlyValidUnitDependencies = offendingUnits.isEmpty
      if (onlyValidUnitDependencies) {
        assert(onlyValidUnitDependencies)
      } else {
        val (code,color) = colors.next()
        val depsToPrint = offendingUnits.take(20).map { case (id, (u1, u2)) => s"$color$u1 -[dependentUnit]-> $u2" }.mkString("\n")
        val uuidList = offendingUnits.flatMap(t => List(t._1, t._2))
        val orListString = uuidList.take(20).map(u => s"_.has('uuid',$u)").mkString(",")
        toDisplay += s"g.V().or($orListString).values('uuid').dedup():$code"
        toDisplay += s"g.V(${offendingUnits.take(20).map(_._1).mkString(",")}):$code"
        assert(onlyValidUnitDependencies, s"\nNot all UnitDependencies connect SUBJECT_UNIT types with a common parent:\n$depsToPrint\n${Console.RED}")
      }
    }
  }
} 

// Provider specific test classes:

class TRACE_Specific_Tests(val graph: TinkerGraph) extends FlatSpec {
  implicit val timeout = Timeout(1 second)
  val missing = List(AbstractObject, Value)
  val minimum = 50000

  // Test that we have a minimum number of nodes
  "This data set" should "contain a representative number of nodes (or else we cannot ensure that other tests behave correctly)" in {
    assert(graph.traversal().V().count().next() > minimum)
  }

  // Test that we get one of each type of statement
  (CDM17.values diff missing).foreach { typeName =>
    it should s"have at least one $typeName" in {
      assert(graph.traversal().V().hasLabel(typeName.toString).count().next() > 0)
    }
  }
}

class CADETS_Specific_Tests(val graph: TinkerGraph) extends FlatSpec {
  implicit val timeout = Timeout(1 second)
  val missing = List(AbstractObject, MemoryObject, ProvenanceTagNode, RegistryKeyObject, SrcSinkObject, Value)
  val minimum = 50000

  // Test that we have a minimum number of nodes
  "This data set" should "contain a representative number of nodes (or else we cannot ensure that other tests behave correctly)" in {
    assert(graph.traversal().V().count().next() > minimum)
  }

  // Test that we get one of each type of statement
  (CDM17.values diff missing).foreach { typeName =>
    it should s"have at least one $typeName" in {
      assert(graph.traversal().V().hasLabel(typeName.toString).count().next() > 0)
    }
  }
}

class FAROS_Specific_Tests(val graph: TinkerGraph) extends FlatSpec {
  implicit val timeout = Timeout(1 second)
  val missing = List(AbstractObject, MemoryObject, RegistryKeyObject, Value)
  val minimum = 50000
  // Test that we have a minimum number of nodes
  "This data set" should "contain a representative number of nodes (or else we cannot ensure that other tests behave correctly)" in {
    assert(graph.traversal().V().count().next() > minimum)
  }

  // Test that we get one of each type of statement
  (CDM17.values diff missing).foreach { typeName =>
    it should s"have at least one $typeName" in {
      assert(graph.traversal().V().hasLabel(typeName.toString).count().next() > 0)
    }
  }

}

class THEIA_Specific_Tests(val graph: TinkerGraph) extends FlatSpec {
  implicit val timeout = Timeout(1 second)
  val missing = List(AbstractObject, Value, TagRunLengthTuple, CryptographicHash, UnnamedPipeObject, SrcSinkObject, UnitDependency)
  val minimum = 50000

  // Test that we have a minimum number of nodes
  "This data set" should "contain a representative number of nodes (or else we cannot ensure that other tests behave correctly)" in {
    assert(graph.traversal().V().count().next() > minimum)
  }

  // Test that we get one of each type of statement
  (CDM17.values diff missing).foreach { typeName =>
    it should s"have at least one $typeName" in {
      assert(graph.traversal().V().hasLabel(typeName.toString).count().next() > 0)
    }
  }
}  

class FIVEDIRECTIONS_Specific_Tests(val graph: TinkerGraph) extends FlatSpec {
  implicit val timeout = Timeout(1 second)
  val missing = List(MemoryObject, Value, TagRunLengthTuple, UnnamedPipeObject, SrcSinkObject, UnitDependency, AbstractObject, CryptographicHash)  // This list is largely from an email Ryan got from Allen Chung: https://mail.google.com/mail/u/0/#inbox/15aa5d58c25c2a53
  val minimum = 50000

  // Test that we have a minimum number of nodes
  "This data set" should "contain a representative number of nodes (or else we cannot ensure that other tests behave correctly)" in {
    assert(graph.traversal().V().count().next() > minimum)
  }

  // Test that we get one of each type of statement
  (CDM17.values diff missing).foreach { typeName =>
    it should s"have at least one $typeName" in {
      assert(graph.traversal().V().hasLabel(typeName.toString).count().next() > 0)
    }
  }
}

class CLEARSCOPE_Specific_Tests(val graph: TinkerGraph) extends FlatSpec {
  implicit val timeout = Timeout(1 second)
  val missing = List(AbstractObject, MemoryObject, RegistryKeyObject, Value)
  val minimum = 50000

  // Test that we have a minimum number of nodes
  "This data set" should "contain a representative number of nodes (or else we cannot ensure that other tests behave correctly)" in {
    assert(graph.traversal().V().count().next() > minimum)
  }

  // Test that we get one of each type of statement
  (CDM17.values diff missing).foreach { typeName =>
    it should s"have at least one $typeName" in {
      assert(graph.traversal().V().hasLabel(typeName.toString).count().next() > 0)
    }
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

