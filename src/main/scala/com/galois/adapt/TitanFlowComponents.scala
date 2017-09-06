package com.galois.adapt

import java.util.UUID
import java.util.concurrent.Executors

import akka.stream.scaladsl.{Flow, Keep, Sink}
import com.galois.adapt.cdm17.CDM17
import com.thinkaurelius.titan.core._
import com.thinkaurelius.titan.core.schema.{SchemaAction, SchemaStatus}
import com.thinkaurelius.titan.graphdb.database.management.ManagementSystem
import com.typesafe.config.ConfigFactory
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.structure.{Direction, Vertex}

import scala.concurrent.duration._
import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


object TitanFlowComponents {

  val config = ConfigFactory.load()

  val threadPool = config.getInt("adapt.ingest.parallelism")

  def addIndex(graph: TitanGraph, management: ManagementSystem, propKey: String, clazz: Class[_], indexName: String) = {

    var idKey = if (management.getPropertyKey(propKey) != null) {
      management.getPropertyKey(propKey)
    } else {
      management.makePropertyKey(propKey).dataType(clazz).make()
    }
    management.buildIndex(indexName, classOf[Vertex]).addKey(idKey).buildCompositeIndex()

    idKey = management.getPropertyKey(propKey)
    val idx = management.getGraphIndex(indexName)
    if (idx.getIndexStatus(idKey).equals(SchemaStatus.INSTALLED)) {
      ManagementSystem.awaitGraphIndexStatus(graph, indexName).status(SchemaStatus.REGISTERED).call()
    }

    management.updateIndex(
      management.getGraphIndex(indexName),
      SchemaAction.ENABLE_INDEX
    )
  }
  /* Open a Cassandra-backed Titan graph. If this is failing, make sure you've run something like
   * the following first:
   *
   *   $ rm -rf /usr/local/var/lib/cassandra/data/*            # clear information from previous run */
   *   $ rm -rf /usr/local/var/lib/cassandra/commitlog/*       # clear information from previous run */
   *   $ /usr/local/opt/cassandra@2.1/bin/cassandra -f         # start Cassandra
   *
   * The following also sets up a key index for UUIDs.
   */
  val graph = {
    val graph = Try(
      TitanFactory.build
        .set("storage.backend","cassandra")
        .set("storage.hostname","localhost")
        .set("storage.read-time",120000)
        .set("storage.cassandra.keyspace", config.getString("adapt.runtime.titankeyspace"))
        .open
    ) match {
      case Success(g) => g
      case Failure(e) =>
        val msg = s"Could not connect to the Cassandra database. Please make sure it is running and try again."
        println("\n\n  ***  " + msg + "  ***\n\n")
        class HeyDummyYouForgotToStartTheDatabaseException(s: String) extends RuntimeException
        throw new HeyDummyYouForgotToStartTheDatabaseException(msg)
    }

    val management = graph.openManagement().asInstanceOf[ManagementSystem]

    val vertexLabels = List("Event", "FileObject", "MemoryObject", "NetFlowObject", "Principal", "ProvenanceTagNode", "RegistryKeyObject", "SrcSinkObject", "Subject", "TagRunLengthTuple", "UnitDependency", "UnnamedPipeObject", "Value")
    for (vertexLabel <- vertexLabels)
      if ( ! management.containsVertexLabel(vertexLabel) ) management.makeVertexLabel(vertexLabel)

    // This allows multiple edges when they are labelled 'tagId'
    if ( ! management.containsEdgeLabel("tagId"))
      management.makeEdgeLabel("tagId").multiplicity(Multiplicity.SIMPLE).make()

    val edgeLabels = List("localPrincipal", "subject", "predicateObject", "predicateObject2",
      "parameterTagId", "flowObject", "prevTagId", "parentSubject", "dependentUnit", "unit", "tag")
    for (edgeLabel <- edgeLabels)
      if ( ! management.containsEdgeLabel(edgeLabel)) management.makeEdgeLabel(edgeLabel).make()

    val propertyKeys = List(
      ("cid", classOf[Integer]),
      ("cmdLine", classOf[String]),
      ("count", classOf[Integer]),
      //      ("ctag", classOf[ConfidentialityTag]),
      ("ctag", classOf[String]),
      //      ("components", classOf[Seq[Value]]),
      ("compoents", classOf[String]),
      ("dependentUnitUuid", classOf[UUID]),
      ("epoch", classOf[Integer]),
      //      ("exportedLibraries", classOf[Seq[String]]),
      ("exportedLibraries", classOf[String]),
      //      ("eventType", classOf[EventType]),
      ("eventType", classOf[String]),
      ("fileDescriptor", classOf[Integer]),
      //      ("fileObjectType", classOf[FileObjectType]),
      ("fileObjectType", classOf[String]),
      ("flowObjectUuid", classOf[UUID]),
      //      ("groupIds", classOf[Seq[String]]),
      ("groupIds", classOf[String]),
      ("hash", classOf[String]),
      //      ("hashes", classOf[Seq[CryptographicHash]]),
      ("hashes", classOf[String]),
      //      ("importedLibraries", classOf[Seq[String]]),
      ("importedLibraries", classOf[String]),
      ("ipProtocol", classOf[Integer]),
      ("isNull", classOf[java.lang.Boolean]),
      //      ("itag", classOf[IntegrityTag]),
      ("itag", classOf[String]),
      ("iteration", classOf[Integer]),
      
      ("keyFromProperties", classOf[java.lang.Long]),
      ("localAddress", classOf[String]),
      ("localPort", classOf[Integer]),
      ("localPrincipalUuid", classOf[UUID]),
      ("location", classOf[java.lang.Long]),
      ("memoryAddress", classOf[java.lang.Long]),
      ("name", classOf[String]),
      ("numValueElements", classOf[Integer]),
//      ("opcode", classOf[TagOpCode]),
      ("opcode", classOf[String]),
      ("pageNumber", classOf[java.lang.Long]),
      ("pageOffset", classOf[java.lang.Long]),
//      ("parameters", classOf[Seq[Value]]),
      ("parameters", classOf[String]),
      ("parentSubjectUuid", classOf[UUID]),
      ("peInfo", classOf[String]),
//      ("permission", classOf[FixedShort]),
      ("permission", classOf[String]),
      ("predicateObjectPath", classOf[String]),
      ("predicateObjectUuid", classOf[UUID]),
      ("predicateObject2Path", classOf[String]),
      ("predicateObject2Uuid", classOf[UUID]),
      ("prevTagIdUuid", classOf[UUID]),
//      ("principalType", classOf[PrincipalType]),
      ("principalType", classOf[String]),
//      ("privilegeLevel", classOf[PrivilegeLevel]),
      ("privilegeLevel", classOf[String]),
      ("programPoint", classOf[String]),
      ("registryKeyOrPath", classOf[String]),
      ("remoteAddress", classOf[String]),
      ("remotePort", classOf[Integer]),
      ("runtimeDataType", classOf[String]),
      ("sequence", classOf[java.lang.Long]),
      ("shmflg", classOf[java.lang.Long]),
      ("shmid", classOf[java.lang.Long]),
      ("sinkFileDescriptor", classOf[Integer]),
      ("size", classOf[java.lang.Long]),
      ("sizeFromProperties", classOf[java.lang.Long]),
      ("sourceFileDescriptor", classOf[Integer]),
      //      ("srcSinkType", classOf[SrcSinkType]),
      ("srcSinkType", classOf[String]),
      ("startTimestampNanos", classOf[java.lang.Long]),
      //      ("subjectType", classOf[SubjectType]),
      ("subjectType", classOf[String]),
      ("subjectUuid", classOf[UUID]),
      ("systemCall", classOf[String]),
      //      ("tag", classOf[Seq[TagRunLengthTuple]]),
      ("tagRunLengthTuples", classOf[String]),
      ("tagIds", classOf[UUID], Cardinality.LIST),
      ("threadId", classOf[Integer]),
      ("timestampNanos", classOf[java.lang.Long]),
      //      ("type", classOf[CryptoHashType]),
      ("titanType", classOf[String]),
      ("type", classOf[String]),
      ("unitId", classOf[Integer]),
      ("unitUuid", classOf[UUID]),
      ("userId", classOf[String]),
      ("username", classOf[String]),
      ("uuid", classOf[UUID]),
      //      ("value", classOf[Value]),
      ("value", classOf[String]),
      //      ("valueBytes", classOf[Array[Byte]]),
      ("valueBytes", classOf[String]),
      //      ("valueDataType", classOf[ValueDataType]),
      ("valueDataType", classOf[String]),
      //      ("valueType", classOf[ValueType])
      ("valueType", classOf[String])
    )
    for (propertyKey <- propertyKeys)
      propertyKey match {
        case (name: String, pClass: Class[_]) if ! management.containsPropertyKey(name) =>
          management.makePropertyKey(name).dataType(pClass).cardinality(Cardinality.SINGLE).make()
        case (name: String, pClass: Class[_], cardinality: Cardinality) if ! management.containsPropertyKey(name) =>
          management.makePropertyKey(name).dataType(pClass).cardinality(cardinality).make()
        case _ => ()
      }

    // This makes a unique index for 'uuid'
    val uuidIndex = management.getGraphIndex("byUuidUnique")
    if (null == uuidIndex) {

      var idKey = if (management.getPropertyKey("uuid") != null) {
        management.getPropertyKey("uuid")
      } else {
        management.makePropertyKey("uuid").dataType(classOf[UUID]).make()
      }
      management.buildIndex("byUuidUnique", classOf[Vertex]).addKey(idKey).unique().buildCompositeIndex()

      idKey = management.getPropertyKey("uuid")
      val idx = management.getGraphIndex("byUuidUnique")
      if (idx.getIndexStatus(idKey).equals(SchemaStatus.INSTALLED)) {
        ManagementSystem.awaitGraphIndexStatus(graph, "byUuidUnique").status(SchemaStatus.REGISTERED).call()
      }

      management.updateIndex(
        management.getGraphIndex("byUuidUnique"),
        SchemaAction.ENABLE_INDEX
      )
    }

    // This makes an index for the object type since Titan is bloody annoying and won't let us do it using the label
    val titanTypeIndex = management.getGraphIndex("byTitanType")
    if (null == titanTypeIndex) {
      addIndex(graph, management, "titanType", classOf[java.lang.String], "byTitanType")
    }

    // And this makes an index over both titan type and subject type since the Edinburgh team uses that frequently
    val titanAndSubjectTypeIndex = management.getGraphIndex("byTitanAndSubjectTypes")
    if( null == titanAndSubjectTypeIndex) {
      var typeKey = if (management.getPropertyKey("titanType") != null) {
        management.getPropertyKey("titanType")
      } else {
        management.makePropertyKey("titanType").dataType(classOf[java.lang.String]).make()
      }
      var subjectTypeKey = if (management.getPropertyKey("subjectType") != null) {
        management.getPropertyKey("subjectType")
      } else {
        management.makePropertyKey("subjecType").dataType(classOf[java.lang.String]).make()
      }
      management.buildIndex("byTitanAndSubjectTypes", classOf[Vertex]).addKey(typeKey).addKey(subjectTypeKey).buildCompositeIndex()

      typeKey = management.getPropertyKey("titanType")
      subjectTypeKey = management.getPropertyKey("subjectType")
      val idx = management.getGraphIndex("byTitanAndSubjectTypes")
      if (idx.getIndexStatus(typeKey).equals(SchemaStatus.INSTALLED) & idx.getIndexStatus(subjectTypeKey).equals(SchemaStatus.INSTALLED)) {
        ManagementSystem.awaitGraphIndexStatus(graph, "byTitanAndSubjectTypes").status(SchemaStatus.REGISTERED).call()
      }

      management.updateIndex(
        management.getGraphIndex("byTitanAndSubjectTypes"),
        SchemaAction.ENABLE_INDEX
      )
    }

    // Edge index on titanType
    var typeKey = if (management.getPropertyKey("titanType") != null) {
      management.getPropertyKey("titanType")
    } else {
      management.makePropertyKey("titanType").dataType(classOf[java.lang.String]).make()
    }
    val allEdges: List[String] = "tagId" :: edgeLabels
    for (edge <- allEdges) {
      val titanEdge = management.getEdgeLabel(edge)
      if(! management.containsRelationIndex(titanEdge, "titanTypeEdgeIndex")) {
        management.buildEdgeIndex(titanEdge, "titanTypeEdgeIndex", Direction.BOTH, Order.incr, typeKey)
      }
    }

    // This makes an index for 'timestampNanos'
    val timestampIndex = management.getGraphIndex("byTimestampNanos")
    if (null == timestampIndex) {
      addIndex(graph, management, "timestampNanos", classOf[java.lang.Long], "byTimestampNanos")
    }

    // This makes an index for 'predicateObjectPath'
    val predicateObjectPathIndex = management.getGraphIndex("byPredicateObjectPath")
    if (null == predicateObjectPathIndex) {
      addIndex(graph, management, "predicateObjectPath", classOf[java.lang.String], "byPredicateObjectPath")
    }

    // Index for 'name'
    val nameIndex = management.getGraphIndex("byName")
    if(null == nameIndex) {
      addIndex(graph, management, "name", classOf[java.lang.String], "byName")
    }

    // Index for 'remoteAddress'
    val remoteAddressIndex = management.getGraphIndex("byRemoteAddress")
    if(null == remoteAddressIndex) {
      addIndex(graph, management, "remoteAddress", classOf[java.lang.String], "byRemoteAddress")
    }

    // Index for 'registryKeyOrPath'
    val registeryKeyOrPathIndex = management.getGraphIndex("byRegistryKeyOrPath")
    if(null == registeryKeyOrPathIndex) {
      addIndex(graph, management, "registeryKeyOrPath", classOf[java.lang.String], "byRegistryKeyOrPath")
    }

    // Index for 'cid'
    val cidIndex = management.getGraphIndex("byCid")
    if(null == cidIndex) {
      addIndex(graph, management, "cid", classOf[java.lang.Integer], "byCid")
    }

    val cmdLineIndex = management.getGraphIndex("byCmdLine")
    if(null == cmdLineIndex) {
      addIndex(graph, management, "cmdLine", classOf[java.lang.String], "byCmdLine")
    }

    management.commit()
    if (uuidIndex == null)
      ManagementSystem.awaitGraphIndexStatus(graph, "byUuidUnique").status(SchemaStatus.ENABLED).call()
    if (titanTypeIndex == null)
      ManagementSystem.awaitGraphIndexStatus(graph, "byTitanType").status(SchemaStatus.ENABLED).call()
    if (timestampIndex == null)
      ManagementSystem.awaitGraphIndexStatus(graph, "byTimestampNanos").status(SchemaStatus.ENABLED).call()
    if (predicateObjectPathIndex == null)
      ManagementSystem.awaitGraphIndexStatus(graph, "byPredicateObjectPath").status(SchemaStatus.ENABLED).call()
    if (nameIndex == null)
      ManagementSystem.awaitGraphIndexStatus(graph, "byName").status(SchemaStatus.ENABLED).call()
    if (remoteAddressIndex == null)
      ManagementSystem.awaitGraphIndexStatus(graph, "byRemoteAddress").status(SchemaStatus.ENABLED).call()
    if (registeryKeyOrPathIndex == null)
      ManagementSystem.awaitGraphIndexStatus(graph, "byRegistryKeyOrPath").status(SchemaStatus.ENABLED).call()
    if (cidIndex == null)
      ManagementSystem.awaitGraphIndexStatus(graph, "byCid").status(SchemaStatus.ENABLED).call()
    if (cmdLineIndex == null)
      ManagementSystem.awaitGraphIndexStatus(graph, "byCmdLine").status(SchemaStatus.ENABLED).call()

    graph
  }

  // Create a titan transaction to insert a batch of objects
  def titanTx(cdms: Seq[DBNodeable]): Try[Unit] = {
    val transaction = //graph.newTransaction()
    graph.buildTransaction()
    //          .enableBatchLoading()
    //          .checkExternalVertexExistence(false)
    .start()

    // For the duration of the transaction, we keep a 'Map[UUID -> Vertex]' of vertices created
    // during this transaction (since we don't look those up in the usual manner).
    val newVertices = MutableMap.empty[UUID, Vertex]

    // We also need to keep track of edges that point to nodes we haven't found yet (this lets us
    // handle cases where nodes are out of order).
    var missingToUuid = MutableMap.empty[UUID, Set[(Vertex, String)]]

    // Accordingly, we define a function which lets us look up a vertex by UUID - first by checking
    // the 'newVertices' map, then falling back on a query to Titan.
    def findNode(uuid: UUID): Option[Vertex] = newVertices.get(uuid) orElse {
      val iterator = transaction.traversal().V().has("uuid", uuid)
      if (iterator.hasNext()) Some(iterator.next()) else None
    }

    val skipEdgesToThisUuid = new UUID(0L, 0L) //.fromString("00000000-0000-0000-0000-000000000000")

    for (cdm <- cdms) {
      // Note to Ryan: I'm sticking with the try block here instead of .recover since that seems to cancel out all following cdm statements.
      // iIf we have a failure on one CDM statement my thought is we want to log the failure but continue execution.
      Try {
        val props: List[Object] = cdm.asDBKeyValues.asInstanceOf[List[Object]]
        assert(props.length % 2 == 0, s"Node ($cdm) has odd length properties list: $props.")
        val newTitanVertex = transaction.addVertex(props: _*)
        newVertices += (cdm.getUuid -> newTitanVertex)

        for ((label, toUuid) <- cdm.asDBEdges) {
          if (toUuid == skipEdgesToThisUuid) Success(())
          else {
            findNode(toUuid) match {
            case Some(toTitanVertex) =>
              newTitanVertex.addEdge(label, toTitanVertex)
            case None =>
              missingToUuid(toUuid) = missingToUuid.getOrElse(toUuid, Set[(Vertex, String)]()) + (newTitanVertex -> label)
            }
          }
        }
      } match {
        case Success(_) =>
        case Failure(e: SchemaViolationException) =>  // TODO??
        case Failure(e: java.lang.IllegalArgumentException) =>
          if (!e.getMessage.contains("byUuidUnique")) {
            println("Failed CDM statement: " + cdm)
            println(e.getMessage) // Bad query
            e.printStackTrace()
          }
        case Failure(e) => println(s"Continuing after unknown exception:\n${e.printStackTrace()}")
      }
    }

    // Try to complete missing edges. If the node pointed to is _still_ not found, we
    // synthetically create it.
    var nodeCreatedCounter = 0
    var edgeCreatedCounter = 0

    for ((uuid, edges) <- missingToUuid; (fromTitanVertex, label) <- edges) {
      if (uuid != skipEdgesToThisUuid) {
        // Find or create the missing vertex (it may have been created earlier in this loop)
        val toTitanVertex = findNode(uuid) getOrElse {
          nodeCreatedCounter += 1
          val newNode = transaction.addVertex("uuid", UUID.randomUUID()) // uuid)
          newVertices += (uuid -> newNode)
          newNode
        }

        // Create the missing edge
        Try {
          fromTitanVertex.addEdge(label, toTitanVertex)
          edgeCreatedCounter += 1
        } match {
          case Success(_) =>
          case Failure(e: SchemaViolationException) => // TODO??
          case Failure(e: java.lang.IllegalArgumentException) =>
            if (!e.getMessage.contains("byUuidUnique")) {
              println(e.getMessage) // Bad query
              e.printStackTrace()
            }
          case Failure(e) => println(s"Continuing after unknown exception:\n${e.printStackTrace()}")
        }
      }
    }

    Try(
      transaction.commit()
    ) match {
      case Success(_) => Success(())
      case Failure(e) =>
        // Item of note: <logger name="com.thinkaurelius.titan.graphdb.database.StandardTitanGraph" level="OFF" /> in
        // logback.xml keeps Titan from spamming the console with transactional failures that we resolve via retry. Otherwise
        // with our logging level, we spam the console with so many stack traces you don't see the messages we actually want
        Failure(e)
    }
  }

  // Loop indefinetly over locking failures
  def retryFinalLoop(cdms: Seq[DBNodeable]): Boolean = {
    titanTx(cdms) match {
      case Success(_) => false
      case Failure(f: com.thinkaurelius.titan.diskstorage.locking.PermanentLockingException) => true
      case Failure(f) =>
        f.getCause.getCause match {
          case _: com.thinkaurelius.titan.diskstorage.locking.PermanentLockingException =>
            true
          case _ =>
            // If we had a non-locking failure, rethrow it
            throw f.getCause.getCause
        }
        true
    }
  }

  // Loops on insertion until we either insert the entire batch
  // A single statement may fail to insert but only after ln(batch size)+1 attempts
  // All ultimate failure errors are collected in the sequence and surfaced to the top.
  // If all statements insert successfully eventually then Seq(Success(())) is returned.
  def titanLoop(cdms: Seq[DBNodeable]): Seq[Try[Unit]] = {
    titanTx(cdms) match {
      case Success(()) => Seq(Success(()))
      case Failure(_) =>
        // If we're trying to ingest a single CDM statement, try it one more time before giving up
        if (cdms.length == 1) {
          // Loop indefinetly over locking failures
          Try {
                while (retryFinalLoop(cdms)) {
                  Thread.sleep(scala.util.Random.nextInt(100))
                }
          } match {
            case Failure(f) =>
              // If we saw a non-locking failure, try one more time to insert before reporting failure
              println("Final retry for statement")
              Seq(titanTx(cdms))
            case _ => Seq(Success())
          }
        } else {
          // Split the list of CDM objects in half (less likely to have object contention for each half of the list) and loop on insertion
          val (front, back) = cdms.splitAt(cdms.length / 2)
          titanLoop(front) match {
            case Seq(Success(_)) => titanLoop(back)
            case fails1 => titanLoop(back) match {
              case Seq(Success(_)) => fails1
              case fails2 => fails1++fails2
            }
          }
        }
    }
  }

  /* Given a 'TitanGraph', make a 'Flow' that writes CDM data into that graph in a buffered manner
   */
  def titanWrites(graph: TitanGraph = graph)(implicit ec: ExecutionContext) = Flow[CDM17]
    .collect { case cdm: DBNodeable => cdm }
    .groupedWithin(1000, 1 seconds)
    .mapAsyncUnordered(threadPool)(x => Future {titanLoop(x)}) // TODO or check mapasync unordered
    .toMat(
      Sink.foreach{ sOrF =>
        sOrF match {
          case Seq(Success(())) => ()
          case fails => println(s"${fails.length} insertion errors in batch")
        }
      }
    )(Keep.right)
}

