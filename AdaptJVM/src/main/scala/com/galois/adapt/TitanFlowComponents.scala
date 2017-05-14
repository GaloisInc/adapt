package com.galois.adapt

import java.util.UUID

import akka.stream.scaladsl.{Flow, Keep, Sink}
import com.galois.adapt.cdm17.CDM17
import com.thinkaurelius.titan.core._
import com.thinkaurelius.titan.core.schema.{SchemaAction, SchemaStatus}
import com.thinkaurelius.titan.graphdb.database.management.ManagementSystem
import org.apache.tinkerpop.gremlin.structure.Vertex

import scala.concurrent.duration._
import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
import scala.concurrent.ExecutionContext
import scala.util.{Success, Failure, Try}


object TitanFlowComponents {

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
    val graph = TitanFactory.build
      .set("storage.backend","cassandra")
      .set("storage.hostname","localhost")
      .set("storage.read-time",120000)
      .open

    val management = graph.openManagement().asInstanceOf[ManagementSystem]

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

  /* Given a 'TitanGraph', make a 'Flow' that writes CDM data into that graph in a buffered manner
   */
  def titanWrites(graph: TitanGraph = graph)(implicit ec: ExecutionContext) = Flow[CDM17]
    .collect { case cdm: DBNodeable => cdm }
    .groupedWithin(1000, 1 seconds)
    .toMat(
      Sink.foreach[collection.immutable.Seq[DBNodeable]]{ cdms =>
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

//        println(s"Created $nodeCreatedCounter synthetic nodes and $edgeCreatedCounter edges")

        Try(
          transaction.commit()
        ) match {
          case Success(_) => // println(s"Committed transaction with ${cdms.length} statements")
          case Failure(e) => e.printStackTrace()
        }
      }
    )(Keep.right)
}

