package com.galois.adapt.scepter

import java.util.UUID

import akka.util.Timeout
import com.galois.adapt.cdm17._
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.scalatest.FlatSpec

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Success, Try}

trait SignalledTests {
  val signalStatus: Boolean => Unit

  def assert(b: Boolean, msg: String = ""): Unit = {
    signalStatus(b)
    if (msg.isEmpty) Predef.assert(b) else Predef.assert(b,msg)
  }
}

class General_TA1_Tests(
  failedStatements: List[(Int,String)],     // position, failed event
  incompleteEdges: Map[UUID, List[(Vertex,String)]],
  graph: TinkerGraph,
  ta1Source: String,
  toDisplay: scala.collection.mutable.ListBuffer[String],
  override val signalStatus: Boolean => Unit
) extends FlatSpec with SignalledTests {

  implicit val timeout = Timeout(30 second)
  
  val colors: Iterator[(String,String)] = Iterator.continually(Map(
    "000" -> Console.BLACK,
    "00F" -> Console.BLUE,
    "0FF" -> Console.CYAN,
    "F0F" -> Console.MAGENTA,
    "FF0" -> Console.YELLOW
  )).flatten


  // Test that all data gets parsed
  "Parsing data in the file..." should "parse successfully" in {
    if (failedStatements.isEmpty)
      assert(failedStatements.length == 0)
    else {
      val toShow: String = failedStatements
        .map { case (i, msg) => s"  offset: $i, message: $msg" }
        .mkString("\n")
      val msg = s"The following CDM messages failed to parse:\n$toShow"
      assert(failedStatements.length == 0, msg)
    }
  }

  // Test to assert that there are no dangling edges
  "Data in this data set" should "have all incomplete edges resolved within CDM punctuation boundaries (or by the end of the file)" in {
    val incompleteEdgesExcludingZeros = incompleteEdges.filterNot(_._1 == new UUID(0L,0L))
    val incompleteCount = incompleteEdgesExcludingZeros.size
    if (incompleteCount == 0) assert(incompleteCount == 0)
    else {
      val (code, color) = colors.next()
      val edgesToPrint = incompleteEdgesExcludingZeros.toList.flatMap { case (u,l) => l map { case (o,s) => s"${o.value("uuid")} --> $u"}}.mkString("\n" + color) + "\n"
      toDisplay += s"g.V(${incompleteEdgesExcludingZeros.take(20).flatMap(_._2.map(_._1.id().toString)).mkString(",")}):$code"
      val message = s"\nThe following edges (ExistingUUID --> MissingUUID) are all referenced in this data set, but nodes with these MissingUUIDs do not exist in this dataset:\n$color$edgesToPrint${Console.RED}\n"
      assert(incompleteCount == 0, message)
    }
  }

  // Test that events of type SEND,SENDMSG,READ,etc. have a size field on them
  if ( ! List("fivedirections", "faros").contains(ta1Source)) {
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
        toDisplay += s"g.V(${eventsShouldHaveSize.map(_.id().toString).take(20).mkString(",")}):$code"
        val uuidsOfEventsShouldHaveSize = eventsShouldHaveSize.map(_.value("uuid").toString).take(20).mkString("\n" + color)

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
    val grouped: java.util.List[java.util.Map[String,java.lang.Long]] = if (ta1Source == "clearscope") {
      graph.traversal().V()
        .not(__.hasLabel("FileObject")) // FileObjects can duplicate UUIDs
        .values("uuid")
        .groupCount[String]()
        .toList
    } else if (ta1Source == "theia") {
      graph.traversal().V()
        .not(__.hasLabel("FileObject", "NetFlowObject", "Principal")) // TODO: check structural equality
        .values("uuid")
        .groupCount[String]()
        .toList
    } else {
      graph.traversal().V()
        .values("uuid")
        .groupCount[String]()
        .toList
    }


    val offending: List[(String,java.lang.Long)] = grouped.get(0).toList.filter(u_c => u_c._2 > 1).take(20)
    var assertMsg: String = "\n"
    for ((uuid,count) <- offending) {
      val (code, color) = colors.next()
      toDisplay += s"g.V().has('uuid',${uuid}).limit(20):$code"
      assertMsg += s"Multiple nodes ($count nodes in this case) should not share the same UUID $color$uuid${Console.RED}\n"
    }

    assert(
      offending.length <= 0,
      assertMsg
    )
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

  // Test that all subjects have a "parentSubject" edge
  // TODO Alec: check that parent subject uuid != uuid
  if (ta1Source != "cadets") {
    "Subjects" should "have a 'parentSubject'" in {
      val subjectsWithoutParents: java.util.List[Vertex] = graph.traversal().V()
        .hasLabel("Subject")
        .hasNot("parentSubjectUuid")
        .toList

      if (subjectsWithoutParents.isEmpty) {
        assert(subjectsWithoutParents.length <= 0)
      } else {
        val (code, color) = colors.next()
        toDisplay += s"g.V(${subjectsWithoutParents.map(_.id().toString).take(20).mkString(",")}):$code"
        val uuidsOfSubjectsWithoutParent = subjectsWithoutParents.map(_.value("uuid").toString).take(20).mkString("\n" + color)

        assert(
          subjectsWithoutParents.length <= 0,
          s"\nSome subjects don't have a 'parentSubject':\n$color$uuidsOfSubjectsWithoutParent${Console.RED}\n"
        )
      }
    }
  }

  // Test that events have a "subjectUuid" field (unless they are EVENT_ADD_OBJECT_ATTRIBUTE)
  "Events" should "have a 'subjectUuid' (unless they have type 'EVENT_ADD_OBJECT_ATTRIBUTE')" in {
    val eventsWithoutSubjectUuid: java.util.List[Vertex] = graph.traversal().V()
      .hasLabel("Event")
      .hasNot("subjectUuid")
      .has("eventType", P.neq("EVENT_ADD_OBJECT_ATTRIBUTE"))
      .toList

    if (eventsWithoutSubjectUuid.isEmpty) {
      assert(eventsWithoutSubjectUuid.length <= 0)
    } else {
      val (code, color) = colors.next()
      toDisplay += s"g.V(${eventsWithoutSubjectUuid.map(_.id().toString).take(20).mkString(",")}):$code"
      val uuidsOfEventsWithoutSubjectUuid = eventsWithoutSubjectUuid.map(_.value("uuid").toString).take(20).mkString("\n" + color)

      assert(
        eventsWithoutSubjectUuid.length <= 0,
        s"\nSome (non 'EVENT_UPDATE') events don't have a 'subjectUuid':\n$color$uuidsOfEventsWithoutSubjectUuid${Console.RED}\n"
      )
    }
  }

  if ( ! List("theia", "clearscope").contains(ta1Source)) {  // Exclusions go in this list.
    it should "demonstrate using the type: EVENT_ADD_OBJECT_ATTRIBUTE (contact us if you plan not to use this)" in {
      assert(graph.traversal().V().hasLabel("Event").has("eventType", "EVENT_ADD_OBJECT_ATTRIBUTE").count().next() > 0L)
    }
  }

  if ( ! List("faros", "theia", "clearscope").contains(ta1Source)) {  // Exclusions go in this list.
    it should "demonstrate using the type: EVENT_FLOWS_TO (contact us if you plan not to use this)" in {
      assert(graph.traversal().V().hasLabel("Event").has("eventType", "EVENT_FLOWS_TO").count().next() > 0L)
    }
  }

  if ( ! List("theia", "clearscope", "cadets").contains(ta1Source)) {  // Exclusions go in this list.
    it should "demonstrate using the type: EVENT_UPDATE (contact us if you plan not to use this)" in {
      assert(graph.traversal().V().hasLabel("Event").has("eventType", "EVENT_UPDATE").count().next() > 0L)
    }
  }

  // Test that events have a "threadId" field (unless they are EVENT_ADD_OBJECT_ATTRIBUTE or EVENT_FLOWS_TO)
  it should "have a 'threadId' (unless they have type 'EVENT_ADD_OBJECT_ATTRIBUTE' or 'EVENT_FLOWS_TO')" in {
    val eventsWithoutThreadId: java.util.List[Vertex] = graph.traversal().V()
      .hasLabel("Event")
      .hasNot("threadId")
      .has("eventType", P.not(P.within("EVENT_ADD_OBJECT_ATTRIBUTE", "EVENT_FLOWS_TO")))
      .toList

    if (eventsWithoutThreadId.isEmpty) {
      assert(eventsWithoutThreadId.length <= 0)
    } else {
      val (code, color) = colors.next()
      toDisplay += s"g.V(${eventsWithoutThreadId.map(_.id().toString).take(20).mkString(",")}):$code"
      val uuidsOfEventsWithoutThreadId = eventsWithoutThreadId.map(_.value("uuid").toString).take(20).mkString("\n" + color)

      assert(
        eventsWithoutThreadId.length <= 0,
        s"\nSome (non 'EVENT_ADD_OBJECT_ATTRIBUTE'/'EVENT_FLOWS_TO') events don't have a 'threadId':\n$color$uuidsOfEventsWithoutThreadId${Console.RED}\n"
      )
    }
  }

  // Test that EVENT_ADD_OBJECT_ATTRIBUTE events have two predicate objects
  // I (Alec) have been assuming the semantics of this event is that predicateObject is an updated variant of predicateObject2
  it should "have two predicate objects that are 'NetFlowObject's when they are 'EVENT_ADD_OBJECT_ATTRIBUTE's" in {
    val malformedAddObjectEvents: java.util.List[Vertex] = graph.traversal().V()
      .hasLabel("Event")
      .has("eventType", "EVENT_ADD_OBJECT_ATTRIBUTE")
      .where(__.not(__.and(
        __.has("predicateObjectUuid"), __.out("predicateObject").hasLabel("NetFlowObject"),
        __.has("predicateObject2Uuid"), __.out("predicateObject2").hasLabel("NetFlowObject")
      )))
      .toList

    if (malformedAddObjectEvents.isEmpty) {
      assert(malformedAddObjectEvents.length <= 0)
    } else {
      val (code, color) = colors.next()
      toDisplay += s"g.V(${malformedAddObjectEvents.map(_.id().toString).take(20).mkString(",")}):$code"
      val uuidsOfMalformedAddObjectEvents = malformedAddObjectEvents.map(_.value("uuid").toString).take(20).mkString("\n" + color)

      assert(
        malformedAddObjectEvents.length <= 0,
        s"\nSome 'EVENT_ADD_OBJECT_ATTRIBUTE' events don't have two predicate objects that are 'NetFlowObject's:\n$color$uuidsOfMalformedAddObjectEvents${Console.RED}\n"
      )
    }
  }

  // Test that we have non 'EVENT_OTHER' event types (aimed at Faros)
  it should "have at least one non-'EVENT_OTHER'" in {
    val eventsNotOther: java.util.List[Vertex] = graph.traversal().V()
      .hasLabel("Event")
      .where(__.and(
        __.has("eventType"),
        __.not(__.has("eventType", "EVENT_OTHER"))
      ))
      .toList

    if (eventsNotOther.nonEmpty) {
      assert(eventsNotOther.nonEmpty)
    } else {
      assert(
        eventsNotOther.length <= 0,
        s"\nDidn't find any events with type not 'EVENT_UPDATE'\n"
      )
    }
  }

  // Test that EVENT_WRITE and EVENT_READ have predicate objects that are 'FileObject', 'SrcSinkObject', 'RegistryKeyObject', 'UnnamedPipeObject', 'NetFlowObject'
  "Read and write events" should "have predicate objects that are exclusively: 'FileObject', 'SrcSinkObject', 'RegistryKeyObject', 'IpcObject', 'NetFlowObject'" in {
    val malformedReadWriteEvents: java.util.List[Vertex] = if ( ! List("clearscope").contains(ta1Source) ) {
      graph.traversal().V()
        .hasLabel("Event")
        .has("eventType", P.within("EVENT_READ","EVENT_WRITE"))
        .as("e")
        .out("predicateObject","predicateObject2")
        .where(__.not(__.hasLabel("FileObject", "SrcSinkObject", "RegistryKeyObject", "UnnamedPipeObject", "NetFlowObject", "IpcObject")))
        .select[Vertex]("e")
        .toList
    } else {
      // Clearscope represents getuid and such with a read event that has predicate object and subject that are the same node
      graph.traversal().V()
        .hasLabel("Event")
        .has("eventType", P.within("EVENT_READ","EVENT_WRITE"))
        .as("e")
        .out("predicateObject","predicateObject2")
        .where(__.not(__.hasLabel("FileObject", "Subject", "SrcSinkObject", "RegistryKeyObject", "UnnamedPipeObject", "NetFlowObject", "IpcObject")))
        .select[Vertex]("e")
        .toList
    }

    if (malformedReadWriteEvents.isEmpty) {
      assert(malformedReadWriteEvents.length <= 0)
    } else {
      val (code, color) = colors.next()
      toDisplay += s"g.V(${malformedReadWriteEvents.map(_.id().toString).take(20).mkString(",")}):$code"
      val uuidsOfMalformedReadWriteEvents = malformedReadWriteEvents.map(_.value("uuid").toString).take(20).mkString("\n" + color)

      assert(
        malformedReadWriteEvents.length <= 0,
        s"\nSome 'EVENT_WRITE' or 'EVENT_READ' have unexpected predicate objects':\n$color$uuidsOfMalformedReadWriteEvents${Console.RED}\n"
      )
    }
  }

//  // Test that EVENT_MODIFY_FILE_ATTRIBUTES events have files as predicate objects
//  it should "have 'FileObject' predicates when the event type is 'EVENT_MODIFY_FILE_ATTRIBUTES'" in {
//    val
//  }

} 

// Provider specific test classes:

class TRACE_Specific_Tests(val graph: TinkerGraph, override val signalStatus: Boolean => Unit) extends FlatSpec with SignalledTests {
  implicit val timeout = Timeout(1 second)
  val missing = List(AbstractObject, TagRunLengthTuple, UnnamedPipeObject, CryptographicHash, Value, UnitDependency, TimeMarker)
  val minimum = 50000

  // Test that we have a minimum number of nodes
  "TRACE data" should "contain a representative number of nodes (or else we cannot ensure that other tests behave correctly)" in {
    assert(graph.traversal().V().count().next() > minimum)
  }

  // Test that we get one of each type of statement
  (CDM17.values diff missing).foreach { typeName =>
    it should s"have at least one $typeName" in {
      assert(graph.traversal().V().hasLabel(typeName.toString).count().next() > 0)
    }
  }
}

class CADETS_Specific_Tests(val graph: TinkerGraph, override val signalStatus: Boolean => Unit) extends FlatSpec with SignalledTests {
  implicit val timeout = Timeout(1 second)
  val missing = List(AbstractObject, MemoryObject, CryptographicHash, UnnamedPipeObject, TagRunLengthTuple, ProvenanceTagNode, RegistryKeyObject, SrcSinkObject, Value, UnitDependency, TimeMarker)
  val minimum = 50000

  // Test that we have a minimum number of nodes
  "CADETS data" should "contain a representative number of nodes (or else we cannot ensure that other tests behave correctly)" in {
    assert(graph.traversal().V().count().next() > minimum)
  }

  // Test that we get one of each type of statement
  (CDM17.values diff missing).foreach { typeName =>
    it should s"have at least one $typeName" in {
      assert(graph.traversal().V().hasLabel(typeName.toString).count().next() > 0)
    }
  }
}

class FAROS_Specific_Tests(val graph: TinkerGraph, override val signalStatus: Boolean => Unit) extends FlatSpec with SignalledTests {
  implicit val timeout = Timeout(1 second)
  val missing = List(AbstractObject, TagRunLengthTuple, CryptographicHash, UnnamedPipeObject, MemoryObject, UnitDependency, RegistryKeyObject, Value, TimeMarker, SrcSinkObject)
  val minimum = 50000
  // Test that we have a minimum number of nodes
  "FAROS data" should "contain a representative number of nodes (or else we cannot ensure that other tests behave correctly)" in {
    assert(graph.traversal().V().count().next() > minimum)
  }

  // Test that we get one of each type of statement
  (CDM17.values diff missing).foreach { typeName =>
    it should s"have at least one $typeName" in {
      assert(graph.traversal().V().hasLabel(typeName.toString).count().next() > 0)
    }
  }

}

class THEIA_Specific_Tests(val graph: TinkerGraph, override val signalStatus: Boolean => Unit) extends FlatSpec with SignalledTests {
  implicit val timeout = Timeout(1 second)
  val missing = List(AbstractObject, Value, TagRunLengthTuple, CryptographicHash, UnnamedPipeObject, SrcSinkObject, UnitDependency, TimeMarker, RegistryKeyObject)
  val minimum = 50000

  // Test that we have a minimum number of nodes
  "THEIA data" should "contain a representative number of nodes (or else we cannot ensure that other tests behave correctly)" in {
    assert(graph.traversal().V().count().next() > minimum)
  }

  // Test that we get one of each type of statement
  (CDM17.values diff missing).foreach { typeName =>
    it should s"have at least one $typeName" in {
      assert(graph.traversal().V().hasLabel(typeName.toString).count().next() > 0)
    }
  }
}  

class FIVEDIRECTIONS_Specific_Tests(val graph: TinkerGraph, override val signalStatus: Boolean => Unit) extends FlatSpec with SignalledTests {
  implicit val timeout = Timeout(1 second)
  val missing = List(MemoryObject, Value, TagRunLengthTuple, UnnamedPipeObject, SrcSinkObject, UnitDependency, AbstractObject, UnnamedPipeObject, CryptographicHash, TimeMarker)
  val minimum = 50000

  // Test that we have a minimum number of nodes
  "FIVE DIRECTIONS data" should "contain a representative number of nodes (or else we cannot ensure that other tests behave correctly)" in {
    assert(graph.traversal().V().count().next() > minimum)
  }

  // Test that we get one of each type of statement
  (CDM17.values diff missing).foreach { typeName =>
    it should s"have at least one $typeName" in {
      assert(graph.traversal().V().hasLabel(typeName.toString).count().next() > 0)
    }
  }
}

class CLEARSCOPE_Specific_Tests(val graph: TinkerGraph, override val signalStatus: Boolean => Unit) extends FlatSpec with SignalledTests {
  implicit val timeout = Timeout(1 second)
  val missing = List(AbstractObject, TagRunLengthTuple, CryptographicHash, MemoryObject, UnitDependency, UnnamedPipeObject, RegistryKeyObject, Value, TimeMarker)
  val minimum = 50000

  // Test that we have a minimum number of nodes
  "CLEARSCOPE data" should "contain a representative number of nodes (or else we cannot ensure that other tests behave correctly)" in {
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

