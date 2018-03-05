package com.galois.adapt

import java.util.UUID

import akka.NotUsed
import akka.actor._
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, Keep, Sink}
import akka.util.Timeout
import com.galois.adapt.adm._
import com.galois.adapt.cdm18.CDM18
import com.steelbridgelabs.oss.neo4j.structure.Neo4JGraph
import com.steelbridgelabs.oss.neo4j.structure.providers.Neo4JNativeElementIdProvider
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph
import org.apache.tinkerpop.gremlin.structure.{Edge, Graph, Vertex}
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.graphdb.schema.Schema
import org.neo4j.graphdb.{ConstraintViolationException, GraphDatabaseService, Label, RelationshipType, Result, Node => NeoNode}
import org.neo4j.kernel.api.exceptions.schema.AlreadyConstrainedException
import org.neo4j.tinkerpop.api.impl.Neo4jGraphAPIImpl
import org.apache.tinkerpop.gremlin.structure.T
import org.neo4j.driver.v1.{StatementResult, Transaction, TransactionWork}
import spray.json._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Random, Success, Try}
import scala.language.postfixOps

// https://github.com/SteelBridgeLabs/neo4j-gremlin-bolt#element-id-providers
import org.neo4j.driver.v1.AuthTokens
import org.neo4j.driver.v1.GraphDatabase
import org.neo4j.driver.v1.{Config => NeoConfig}


class Neo4jDBQueryProxy(statusActor: ActorRef) extends DBQueryProxyActor {

  val config: Config = ConfigFactory.load()

  val driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "badpassword"), NeoConfig.build().withoutEncryption().toConfig)

  val session = driver.session()
  val tx = session.beginTransaction()

  // NOTE: The UI expects a specific format and collection of labels on each node.
  // Making a change to the labels on a node will need to correspond to a change made in the UI javascript code.

  tx.run("CREATE CONSTRAINT ON (node:Node) ASSERT node.uuid IS UNIQUE")
//  createIfNeededUniqueConstraint(schema, "Node", "uuid")

  val cdmIndices = Map(
    "Subject"           -> List("timestampNanos", "cid", "cmdLine"),
    "RegistryKeyObject" -> List("registryKeyOrPath"),
    "NetFlowObject"     -> List("localAddress", "localPort", "remoteAddress", "remotePort"),
    "FileObject"        -> List("peInfo", "path"),
    "Event"             -> List("timestampNanos", "name", "eventType", "predicateObjectPath", "predicateObject2Path")
  )

  val admIndices = Map(
    "AdmEvent"         -> List("eventType", "earliestTimestampNanos", "latestTimestampNanos"),
    "AdmSubject"       -> List("startTimestampNanos"),
    "AdmNetFlowObject" -> List("localAddress", "localPort", "remoteAddress", "remotePort")
  )

  if (config.getBoolean("adapt.ingest.producecdm")) cdmIndices.foreach{
    case (label, keys) => keys.foreach(key => tx.run(s"CREATE INDEX ON :$label($key)"))
  }

  if (config.getBoolean("adapt.ingest.produceadm")) admIndices.foreach{
    case (label, keys) => keys.foreach(key => tx.run(s"CREATE INDEX ON :$label($key)"))
  }


//  tx.commitAsync()
  tx.success()
  tx.close()
  session.close()





//  val neoGraph: GraphDatabaseService = {
//    val neo4jFile: java.io.File = new java.io.File(config.getString("adapt.runtime.neo4jfile"))
//    val graphService = new GraphDatabaseFactory().newEmbeddedDatabase(neo4jFile)
//    context.system.registerOnTermination(graphService.shutdown())
//
//    def awaitSchemaCreation(g: GraphDatabaseService): Unit = {
//      val tx = g.beginTx()
//      val schema = g.schema()
//      for(i <- schema.getIndexes.asScala) {
//        var status = schema.getIndexState(i)
//        while(status != Schema.IndexState.ONLINE) {
//          println(i + " is " + status)
//          Thread.sleep(100)
//          status = schema.getIndexState(i)
//        }
//        println(i + " is " + status)
//      }
//      tx.success()
//      tx.close()
//    }
//
//    def findConstraint(schema: Schema, label: Label, prop: String): Boolean = {
//      val constraints = schema.getConstraints(label).asScala
//      constraints.exists { c =>
//        val constrainedProps = c.getPropertyKeys.asScala
//        constrainedProps.size == 1 && constrainedProps.exists(_.equals(prop))
//      }
//    }
//
//    def createIfNeededUniqueConstraint(schema: Schema, labelString: String, prop: String): Unit = {
//      val label = Label.label(labelString)
//      if(! findConstraint(schema, label, prop)) {
//        Try(schema.constraintFor(label).assertPropertyIsUnique(prop).create()) match {
//          case Success(_) => ()
//          case Failure(e) if e.getCause.isInstanceOf[AlreadyConstrainedException] => println(s"Ignoring an already constrained label: ${label.name}")
//          case Failure(e) => throw e
//        }
//      }
//    }
//
//    def findIndex(schema: Schema, label: Label, prop: String): Boolean = {
//      val indices = schema.getIndexes(label).asScala
//      indices.exists { i =>
//        val indexedProps = i.getPropertyKeys.asScala
//        indexedProps.size == 1 && indexedProps.exists(_.equals(prop))
//      }
//    }
//
//    def createIfNeededIndex(schema: Schema, labelString: String, prop: String) = {
//      val label = Label.label(labelString)
//      if(! findIndex(schema, label, prop)) {
//        schema.indexFor(label).on(prop).create()
//      }
//    }
//
//
//    val tx = graphService.beginTx()
//    val schema = graphService.schema()
//
//    createIfNeededUniqueConstraint(schema, "Node", "uuid")
//
//    // NOTE: The UI expects a specific format and collection of labels on each node.
//    // Making a change to the labels on a node will need to correspond to a change made in the UI javascript code.
//
//    if (config.getBoolean("adapt.ingest.producecdm")) cdmIndices.foreach{
//      case (label, keys) => keys.foreach(key => createIfNeededIndex(schema, label, key))
//    }
//
//    if (config.getBoolean("adapt.ingest.produceadm")) admIndices.foreach{
//      case (label, keys) => keys.foreach(key => createIfNeededIndex(schema, label, key))
//    }
//
//    // TODO: Neo4j doesn't want to index properties longer than 32766 bytes:
//    //    createIfNeededIndex(schema, "AdmPathNode", "path")
//    //    createIfNeededIndex(schema, "ADM", "originalCdmUuids")
//
//    tx.success()
//    tx.close()
//
//    awaitSchemaCreation(graphService)
//    //schema.awaitIndexesOnline(10, TimeUnit.MINUTES)
//
//    graphService
//  }

//  val graph: Graph = Neo4jGraph.open(new Neo4jGraphAPIImpl(neoGraph))


  val provider = new Neo4JNativeElementIdProvider()
  val graph/*: Graph*/ = new Neo4JGraph(driver, provider, provider) //vertexIdProvider, edgeIdProvider)


  val shouldLogDuplicates: Boolean = config.getBoolean("adapt.ingest.logduplicates")


  def findVertex(uuid: UUID, inThisTx: MutableMap[UUID, Vertex]): Option[Vertex] = inThisTx.get(uuid)
//    .map{v => if (UUID.fromString(v.property[String]("uuid").value()) != uuid) log.error(s"Specified: $uuid, Found: ${v.property[String]("uuid").value()}"); v}
    .orElse {    // TODO: Verify this works with Alec's new parameterized UUIDs.


      val lookupResults = graph.traversal().V().hasLabel("Node").has("uuid", uuid.toString).asScala.toList
      log.info(s"Looked up: ${lookupResults.map(_.property[String]("uuid").value())}")
      require(lookupResults.lengthCompare(1) <= 0, s"Looking up a node by its uuid: $uuid failed because multiple results were returned: $lookupResults")
      lookupResults.headOption.map{v =>
        inThisTx(uuid) = v
        v
      }
    }


  def dbNodeableTx(cdms: Seq[DBNodeable[_]]): Try[Unit] = {



    val verticesInThisTX = MutableMap.empty[UUID, Vertex]

    log.info(s"Starting transaction.")
    //    val transaction = neoGraph.beginTx()
//    val transaction = graph.tx()
//    transaction.open()

//    val verticesInThisTX = MutableMap.empty[UUID, NeoNode]

    val skipEdgesToThisUuid = new UUID(0L, 0L) //.fromString("00000000-0000-0000-0000-000000000000")

    val identifiers = mutable.Map.empty[UUID, String]
    def getName(uuid: UUID): String = identifiers.getOrElse(uuid, {identifiers(uuid) = Random.alphanumeric.filter(_.isLetter).take(10).mkString.toLowerCase; identifiers(uuid)})

    val (query, params) = cdms.foldLeft("" -> Map.empty[String,AnyRef]) { case ((query: String, allparams: Map[String,AnyRef]), cdm: DBNodeable[_]) =>
      val thisName = getName(cdm.getUuid)

      val props = cdm.asDBKeyValues.toMap.map {
        case (k, v: UUID) => s"${thisName}_$k" -> v.toString
        case (k, other)   => s"${thisName}_$k" -> other.asInstanceOf[AnyRef]
      }
      val cdmTypeName = cdm.getClass.getSimpleName
      val stmtProps = cdm.asDBKeyValues.map(k => s"${k._1}: ${"$"}${thisName}_${k._1}").sorted.mkString("{", ", ", "}")

      val edgesStmt = cdm.asDBEdges.map { case (edgeName, toUuid) =>
        val isNew = ! identifiers.contains(toUuid)
        val otherName = getName(toUuid)
        val otherStmt = if (isNew) s"""MERGE ($otherName: Node {uuid: "$toUuid"}) """ else ""
//        s"""MERGE ($otherName: Node {uuid: "$toUuid"}) CREATE UNIQUE ($thisName)-[:$edgeName]->($otherName)"""
        s"""${otherStmt}MERGE ($thisName)-[:$edgeName]->($otherName)"""
      }

      val baseStmt = s"MERGE ($thisName: $cdmTypeName $stmtProps) ${edgesStmt.mkString(" ")}\n"

      (query + baseStmt) -> (allparams ++ props)
    }


    Try {
      val s = driver.session()
      s.writeTransaction(new TransactionWork[StatementResult] {
        def execute(tx: Transaction) = tx.run(query, params.asJava)
      })
      s.close()
    }



    val cdmToNodeResults = cdms.map { cdm =>
      Try {
        val cdmTypeName = cdm.getClass.getSimpleName
        val thisNeo4jVertex = findVertex(cdm.getUuid, verticesInThisTX).getOrElse({
//          log.info(s"Creating node for: ${cdm.getUuid}")
          // IMPORTANT NOTE: The UI expects a specific format and collection of labels on each node.
          // Making a change to the labels on a node will need to correspond to a change made in the UI javascript code.
//          val newVertex = neoGraph.createNode(Label.label("Node"), Label.label(cdmTypeName)) // Throws an exception instead of creating duplicate UUIDs.



          val newVertex  = graph.addVertex(T.label, s"Node::$cdmTypeName")  // TODO: Verify that this is how multiple Neo4j labels are created via gremlin!
          verticesInThisTX += (cdm.getUuid -> newVertex)
          newVertex
        })

        cdm.asDBKeyValues.foreach {
          // TODO: should log if new value is replacing a different value!
          case (k, v: UUID) => thisNeo4jVertex.property(k, v.toString)
          case (k, v: Int)  => thisNeo4jVertex.property(k, v.toLong)  // Bolt protocol supports fewer types: https://github.com/SteelBridgeLabs/neo4j-gremlin-bolt/blob/master/src/main/java/com/steelbridgelabs/oss/neo4j/structure/Neo4JBoltSupport.java#L36
          case (k,v) => try {
            thisNeo4jVertex.property(k, v)
          } catch {
            case e: Exception =>
              println(s"Tried (and failed) to set the key $k to the value $v on a Neo4j node")
              e.printStackTrace()
          }
        }

        cdm.asDBEdges.foreach { case (edgeName, toUuid) =>
          if (toUuid != skipEdgesToThisUuid) findVertex(toUuid, verticesInThisTX) /*verticesInThisTX.get(toUuid)*/ match {
            case Some(toNeo4jVertex) =>
//              val relationship = new RelationshipType() { def name: String = edgeName.toString }
//              thisNeo4jVertex.createRelationshipTo(toNeo4jVertex, relationship)
              thisNeo4jVertex.addEdge(edgeName.toString, toNeo4jVertex)
            case None =>
//              val destinationNode = Option(neoGraph.findNode(Label.label("Node"), "uuid", toUuid.toString)).getOrElse {
//                verticesInThisTX(toUuid) = neoGraph.createNode(Label.label("Node"))  // Create empty node
//                verticesInThisTX(toUuid)
//              }

//              log.info(s"Creating node for edge to: ${toUuid}")
              val destinationNode = //findVertex(toUuid).getOrElse {
                graph.addVertex(T.label, "Node", "uuid", toUuid.toString)//  v.property("uuid", toUuid.toString)
//              }
//              verticesInThisTX(toUuid) = destinationNode
//              val relationship = new RelationshipType() { def name: String = edgeName.toString }
//              thisNeo4jVertex.createRelationshipTo(destinationNode, relationship)
              thisNeo4jVertex.addEdge(edgeName.toString, destinationNode)
          }
        }
        Some(cdm.getUuid)
      }.recoverWith {
        case e: org.neo4j.driver.v1.exceptions.ClientException =>
//          log.error(e.getMessage.split("\n").take(5).mkString("\n"))
          log.error(s"CLIENT EXCEPTION")
          Success(None)
        case e: ConstraintViolationException =>
          if (shouldLogDuplicates) log.info(s"Skipping duplicate creation of node: ${cdm.getUuid}")
          Success(None)
        //  case e: MultipleFoundException => Should never find multiple nodes with a unique constraint on the `uuid` field
        case e =>
          log.error("UNKNOWN FAILURE IN TRANSACTION:")
          e.printStackTrace()
          Failure(e)
      }
    }

    Try {
      if (cdmToNodeResults.forall(_.isSuccess)) {
//        transaction.success()
//        log.info("finished transaction. trying to commit.")
//        transaction.commit()
//        transaction.close()
        log.info("transaction successful")
      } else {
        log.error(s"DETECTABLE TRANSACTION FAILURE! CDMs:\n${cdms.length}")
        val errorMessages = cdmToNodeResults.collect{ case t: Failure[_] => t.exception.getMessage}
//        transaction.failure()
//        transaction.rollback()
//        transaction.close()
        throw new RuntimeException(s"Transaction rolled back due to: ${errorMessages.mkString("\n", "\n", "")}")
      }
    }
  }


  def admTx(adms: Seq[Either[EdgeAdm2Adm, ADM]]): Try[Unit] = {
    val transaction = graph.tx()
    val verticesInThisTX = MutableMap.empty[UUID, Vertex]

    val skipEdgesToThisUuid = new UUID(0L, 0L) //.fromString("00000000-0000-0000-0000-000000000000")

    val admToNodeResults = adms map {
      case Left(edge) => Try {

        if (edge.tgt.uuid != skipEdgesToThisUuid) {
          val source = verticesInThisTX
            .get(edge.src)
            .orElse(findVertex(edge.src.uuid, verticesInThisTX)) // Option(neoGraph.findNode(Label.label("Node"), "uuid", edge.src.uuid.toString)))
            .getOrElse(throw AdmInvariantViolation(edge))

          val target = verticesInThisTX
            .get(edge.tgt)
            .orElse(findVertex(edge.tgt.uuid, verticesInThisTX)) // Option(neoGraph.findNode(Label.label("Node"), "uuid", edge.tgt.uuid.toString)))
            .getOrElse(throw AdmInvariantViolation(edge))

//          source.createRelationshipTo(target, new RelationshipType() {
//            def name: String = edge.label
//          })
          source.addEdge(edge.label, target)
        }
      }

      case Right(adm) => Try {
        val admTypeName = adm.getClass.getSimpleName
        val thisNeo4jVertex = verticesInThisTX.getOrElse(adm.uuid, {
          // IMPORTANT NOTE: The UI expects a specific format and collection of labels on each node.
          // Making a change to the labels on a node will need to correspond to a change made in the UI javascript code.
//          val newVertex = neoGraph.createNode(Label.label("Node"), Label.label(admTypeName)) // Throws an exception instead of creating duplicate UUIDs.
          val newVertex  = graph.addVertex(T.label, s"Node::$admTypeName")  // TODO: Verify that this is how multiple Neo4j labels are created via gremlin!

          verticesInThisTX += (adm.uuid.uuid -> newVertex)
          newVertex
        })

        adm.asDBKeyValues.foreach {
          case (k, v: UUID) => thisNeo4jVertex.property(k, v.toString)
          case (k, v) => thisNeo4jVertex.property(k, v)
        }

        Some(adm.uuid)
      }.recoverWith {
        case e: ConstraintViolationException =>
          if (shouldLogDuplicates) println(s"Skipping duplicate creation of node: ${adm.uuid}")
          Success(None)
        case e: Throwable => Failure(e)
      }
    }

    Try {
      if (admToNodeResults.forall(_.isSuccess)) {
//        transaction.success()
        transaction.commit()
      } else {
        println(s"TRANSACTION FAILURE! ADMs:\n")
        admToNodeResults foreach { case t if t.isFailure => t.failed.get.printStackTrace() }
//        transaction.failure()
        transaction.rollback()
      }
      transaction.close()
    }
  }

  def FutureTx[T](body: => T)(implicit ec: ExecutionContext): Future[T] = Future {
//    val tx = neoGraph.beginTx()
    val tx = graph.tx()
    val result: T = body
//    tx.success()
    tx.commit()
    tx.close()
    result
  }

  override def receive: PartialFunction[Any,Unit] = ({
    // Cypher queries are only supported by Neo4j, and they are always streaming
    case CypherQuery(q, true) =>
      println(s"Received Cypher query: $q")
      sender() ! Future { Try {
//        DBQueryProxyActor.toJson(neoGraph.execute(q).asScala.toList)
        val tx = session.beginTransaction()
        val result = DBQueryProxyActor.toJson(tx.run(q).asScala.toList)
        tx.success()
        tx.close()
      } }

    case InitMsg =>
      statusActor ! InitMsg
      super.receive(InitMsg)

    case CompleteMsg =>
      statusActor ! CompleteMsg
      super.receive(CompleteMsg)

  }: PartialFunction[Any,Unit]) orElse super.receive
}


object Neo4jFlowComponents {

  def neo4jActorCdmWriteSink(neoActor: ActorRef, completionMsg: Any = CompleteMsg)(implicit timeout: Timeout): Sink[CDM18, NotUsed] = Flow[CDM18]
    .collect { case cdm: DBNodeable[_] => cdm }
    .groupedWithin(1000, 1 second)
    .map(WriteCdmToNeo4jDB.apply)
    .toMat(Sink.actorRefWithAck(neoActor, InitMsg, Ack, completionMsg))(Keep.right)

  def neo4jActorAdmWriteSink(neoActor: ActorRef, completionMsg: Any = CompleteMsg)(implicit timeout: Timeout): Sink[Either[EdgeAdm2Adm, ADM], NotUsed] = Flow[Either[EdgeAdm2Adm, ADM]]
    .groupedWithin(1000, 1 second)
    .map(WriteAdmToNeo4jDB.apply)
    .toMat(Sink.actorRefWithAck(neoActor, InitMsg, Ack, completionMsg))(Keep.right)
}
