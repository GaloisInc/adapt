package com.galois.adapt

import java.util.UUID
import akka.actor._
import com.galois.adapt.adm._
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph
import org.apache.tinkerpop.gremlin.structure.{Edge, Graph, Vertex}
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.graphdb.schema.Schema
import org.neo4j.graphdb.{ConstraintViolationException, GraphDatabaseService, Label, RelationshipType, Result, Node => NeoNode}
import org.neo4j.kernel.api.exceptions.schema.AlreadyConstrainedException
import org.neo4j.tinkerpop.api.impl.Neo4jGraphAPIImpl
import spray.json._
import scala.collection.JavaConverters._
import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.language.postfixOps
import AdaptConfig._


class Neo4jDBQueryProxy(statusActor: ActorRef) extends DBQueryProxyActor {

  val neoGraph: GraphDatabaseService = {
    val neo4jFile: java.io.File = new java.io.File(runtimeConfig.neo4jfile)
    val graphService = new GraphDatabaseFactory().newEmbeddedDatabase(neo4jFile)
    context.system.registerOnTermination(graphService.shutdown())

    def awaitSchemaCreation(g: GraphDatabaseService): Unit = {
      val tx = g.beginTx()
      val schema = g.schema()
      for(i <- schema.getIndexes.asScala) {
        var status = schema.getIndexState(i)
        while(status != Schema.IndexState.ONLINE) {
          println(i + " is " + status)
          Thread.sleep(100)
          status = schema.getIndexState(i)
        }
        println(i + " is " + status)
      }
      tx.success()
      tx.close()
    }

    def findConstraint(schema: Schema, label: Label, prop: String): Boolean = {
      val constraints = schema.getConstraints(label).asScala
      constraints.exists { c =>
        val constrainedProps = c.getPropertyKeys.asScala
        constrainedProps.size == 1 && constrainedProps.exists(_.equals(prop))
      }
    }

    def createIfNeededUniqueConstraint(schema: Schema, labelString: String, prop: String): Unit = {
      val label = Label.label(labelString)
      if(! findConstraint(schema, label, prop)) {
        Try(schema.constraintFor(label).assertPropertyIsUnique(prop).create()) match {
          case Success(_) => ()
          case Failure(e) if e.getCause.isInstanceOf[AlreadyConstrainedException] => println(s"Ignoring an already constrained label: ${label.name}")
          case Failure(e) => throw e
        }
      }
    }

    def findIndex(schema: Schema, label: Label, prop: String): Boolean = {
      val indices = schema.getIndexes(label).asScala
      indices.exists { i =>
        val indexedProps = i.getPropertyKeys.asScala
        indexedProps.size == 1 && indexedProps.exists(_.equals(prop))
      }
    }

    def createIfNeededIndex(schema: Schema, labelString: String, prop: String) = {
      val label = Label.label(labelString)
      if(! findIndex(schema, label, prop)) {
        schema.indexFor(label).on(prop).create()
      }
    }


    val tx = graphService.beginTx()
    val schema = graphService.schema()

    createIfNeededUniqueConstraint(schema, "Node", "uuid")

    // NOTE: The UI expects a specific format and collection of labels on each node.
    // Making a change to the labels on a node will need to correspond to a change made in the UI javascript code.

    if (ingestConfig.producecdm) {

      createIfNeededIndex(schema, "Subject", "timestampNanos")
      createIfNeededIndex(schema, "Subject", "cid")
      createIfNeededIndex(schema, "Subject", "cmdLine")
      createIfNeededIndex(schema, "RegistryKeyObject", "registryKeyOrPath")
      createIfNeededIndex(schema, "NetFlowObject", "localAddress")
      createIfNeededIndex(schema, "NetFlowObject", "localPort")
      createIfNeededIndex(schema, "NetFlowObject", "remoteAddress")
      createIfNeededIndex(schema, "NetFlowObject", "remotePort")
      createIfNeededIndex(schema, "FileObject", "peInfo")
      createIfNeededIndex(schema, "FileObject", "path")
      createIfNeededIndex(schema, "Event", "timestampNanos")
      createIfNeededIndex(schema, "Event", "name")
      createIfNeededIndex(schema, "Event", "eventType")
      createIfNeededIndex(schema, "Event", "predicateObjectPath")

    }

    if (ingestConfig.produceadm) {

      createIfNeededIndex(schema, "AdmEvent", "eventType")
      createIfNeededIndex(schema, "AdmEvent", "earliestTimestampNanos")
      createIfNeededIndex(schema, "AdmEvent", "latestTimestampNanos")
      createIfNeededIndex(schema, "AdmSubject", "startTimestampNanos")
      createIfNeededIndex(schema, "AdmNetFlowObject", "localAddress")
      createIfNeededIndex(schema, "AdmNetFlowObject", "localPort")
      createIfNeededIndex(schema, "AdmNetFlowObject", "remoteAddress")
      createIfNeededIndex(schema, "AdmNetFlowObject", "remotePort")

    }

    // TODO: Neo4j doesn't want to index properties longer than 32766 bytes:
    //    createIfNeededIndex(schema, "AdmPathNode", "path")
    //    createIfNeededIndex(schema, "ADM", "originalCdmUuids")

    tx.success()
    tx.close()

    awaitSchemaCreation(graphService)
    //schema.awaitIndexesOnline(10, TimeUnit.MINUTES)

    graphService
  }

  val graph: Graph = Neo4jGraph.open(new Neo4jGraphAPIImpl(neoGraph))

  def DBNodeableTx(cdms: Seq[DBNodeable[_]]): Try[Unit] = {
    val transaction = neoGraph.beginTx()
    val verticesInThisTX = MutableMap.empty[UUID, NeoNode]

    val skipEdgesToThisUuid = new UUID(0L, 0L) //.fromString("00000000-0000-0000-0000-000000000000")

    val cdmToNodeResults: Seq[Try[Option[UUID]]] = cdms map { cdm =>
      Try {
        val cdmTypeName = cdm.getClass.getSimpleName
        val thisNeo4jVertex = verticesInThisTX.getOrElse(cdm.getUuid, {
          // IMPORTANT NOTE: The UI expects a specific format and collection of labels on each node.
          // Making a change to the labels on a node will need to correspond to a change made in the UI javascript code.
          val newVertex = neoGraph.createNode(Label.label("Node"), Label.label(cdmTypeName)) // Throws an exception instead of creating duplicate UUIDs.
          verticesInThisTX += (cdm.getUuid -> newVertex)
          newVertex
        })

        cdm.asDBKeyValues.foreach {
          case (k, v: UUID) => thisNeo4jVertex.setProperty(k, v.toString)
          case (k,v) => try {
            thisNeo4jVertex.setProperty(k, v)
          } catch {
            case e: Exception =>
              log.warning(s"Tried (and failed) to set the key $k to the value $v on a Neo4j node")
              e.printStackTrace()
          }
        }

        cdm.asDBEdges.foreach { case (edgeName, toUuid) =>
          if (toUuid != skipEdgesToThisUuid) verticesInThisTX.get(toUuid) match {
            case Some(toNeo4jVertex) =>
              val relationship = new RelationshipType() { def name: String = edgeName.toString }
              thisNeo4jVertex.createRelationshipTo(toNeo4jVertex, relationship)
            case None =>
              val destinationNode = Option(neoGraph.findNode(Label.label("Node"), "uuid", toUuid.toString)).getOrElse {
                verticesInThisTX(toUuid) = neoGraph.createNode(Label.label("Node"))  // Create empty node
                verticesInThisTX(toUuid)
              }
              val relationship = new RelationshipType() { def name: String = edgeName.toString }
              thisNeo4jVertex.createRelationshipTo(destinationNode, relationship)
          }
        }
        Some(cdm.getUuid)
      }.recoverWith[Option[UUID]] {
        case e: ConstraintViolationException =>
          if (ingestConfig.logduplicates) log.info(s"Skipping duplicate creation of node: ${cdm.getUuid}")
          Success(None)
        //  case e: MultipleFoundException => Should never find multiple nodes with a unique constraint on the `uuid` field
        case e =>
          e.printStackTrace()
          Failure(e)
      }
    }

    Try {
      if (cdmToNodeResults.forall(_.isSuccess))
        transaction.success()
      else {
        log.error(s"TRANSACTION FAILURE! CDMs:\n$cdms")
        transaction.failure()
      }
      transaction.close()
    }
  }

  def AdmTx(adms: Seq[Either[ADM, EdgeAdm2Adm]]): Try[Unit] = {
    val transaction = neoGraph.beginTx()
    val verticesInThisTX = MutableMap.empty[(UUID,String), NeoNode]

    val skipEdgesToThisUuid = new UUID(0L, 0L) //.fromString("00000000-0000-0000-0000-000000000000")

    var sawAnError = false
    val admToNodeResults: Seq[Try[Option[AdmUUID]]] = adms map {
      case Right(edge) => Try {
        if (edge.tgt.uuid != skipEdgesToThisUuid) {
          val source = verticesInThisTX
            .get(edge.src)
            .orElse(Option(neoGraph.findNode(Label.label("Node"), "uuid", edge.src.rendered)))
            .getOrElse({
              log.error(s"Had to synthesize the source of $edge!")
              val newVertex = neoGraph.createNode(Label.label("Node"), Label.label("AdmSynthesized"))
              newVertex.setProperty("uuid", edge.src.rendered)
              verticesInThisTX += (admToTuple(edge.src) -> newVertex)
              newVertex
            })

          val target = verticesInThisTX
            .get(edge.tgt)
            .orElse(Option(neoGraph.findNode(Label.label("Node"), "uuid", edge.tgt.rendered)))
            .getOrElse({
              log.error(s"Had to synthesize the target of $edge!")
              val newVertex = neoGraph.createNode(Label.label("Node"), Label.label("AdmSynthesized"))
              newVertex.setProperty("uuid", edge.tgt.rendered)
              verticesInThisTX += (admToTuple(edge.tgt) -> newVertex)
              newVertex
            })

          source.createRelationshipTo(target, new RelationshipType() {
            def name: String = edge.label
          })

          None
        } else None
      }.recoverWith {
        case e: ConstraintViolationException =>
          if (ingestConfig.logduplicates)
            log.warning(s"Skipping duplicate creation of node when creating $edge (I, Alec, am not sure how this could ever happen)")
          Success(None)
        case e: Throwable =>
          sawAnError = true
          Failure(e)
      }

      case Left(adm) => Try {
        val admTypeName = adm.getClass.getSimpleName
        val thisNeo4jVertex = verticesInThisTX.getOrElse(adm.uuid, {
          // IMPORTANT NOTE: The UI expects a specific format and collection of labels on each node.
          // Making a change to the labels on a node will need to correspond to a change made in the UI javascript code.
          val newVertex = neoGraph.createNode(Label.label("Node"), Label.label(admTypeName))
          verticesInThisTX += (admToTuple(adm.uuid) -> newVertex)
          newVertex
        })

        thisNeo4jVertex.setProperty("uuid", adm.uuid.rendered)
        adm.asDBKeyValues.foreach {
          case (k, v) => if (k != "uuid") { thisNeo4jVertex.setProperty(k, v) }
        }

        Some(adm.uuid)
      }.recoverWith[Option[AdmUUID]] {
        case e: ConstraintViolationException =>
          if (ingestConfig.logduplicates) log.warning(s"Skipping duplicate creation of node: ${adm.uuid}")
          Success(None)
        case e: Throwable =>
          sawAnError = true
          Failure(e)
      }
    }

    Try {
      if ( ! sawAnError)
        transaction.success()
      else {
        log.error(s"TRANSACTION FAILURE! ADMs:\n")
        admToNodeResults foreach { case t if t.isFailure => t.failed.get.printStackTrace() }
        transaction.failure()
      }
      transaction.close()
    }
  }

  def FutureTx[T](body: =>T)(implicit ec: ExecutionContext): Future[T] = Future {
    val tx = neoGraph.beginTx()
    val result: T = body
    tx.success()
    tx.close()
    result
  }

  override def receive: PartialFunction[Any,Unit] = ({
    // Cypher queries are only supported by Neo4j
    case CypherQuery(q, true) =>
      log.info(s"Received Cypher query: $q")
      sender() ! Future { Try { DBQueryProxyActor.toJson(neoGraph.execute(q).asScala.toList)  } }

    case InitMsg =>
      statusActor ! InitMsg
      super.receive(InitMsg)

    case CompleteMsg =>
      statusActor ! CompleteMsg
      super.receive(CompleteMsg)

  }: PartialFunction[Any,Unit]) orElse super.receive
}

