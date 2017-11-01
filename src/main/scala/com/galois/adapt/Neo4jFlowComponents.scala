package com.galois.adapt

import java.util.UUID
import akka.actor.ActorRef
import akka.stream.scaladsl.{Flow, Keep, Sink}
import com.galois.adapt.cdm17.CDM17
import com.galois.adapt.cdm17.CDM17.EdgeTypes.EdgeTypes
import com.typesafe.config.ConfigFactory
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.structure.Direction
import org.neo4j.graphdb.{ConstraintViolationException, GraphDatabaseService, Label, Node => NeoNode}
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.kernel.api.proc.Neo4jTypes.RelationshipType
import org.neo4j.graphdb.schema.Schema
import org.neo4j.kernel.api.exceptions.schema.AlreadyConstrainedException
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._


object Neo4jFlowComponents {

  val config = ConfigFactory.load()
  val parallelismSize = config.getInt("adapt.ingest.parallelism")

  /* Open a Neo4j graph database and create indices */
  val graph: GraphDatabaseService = {
    val neo4jFile: java.io.File = new java.io.File(config.getString("adapt.runtime.neo4jfile"))
    val graph = new GraphDatabaseFactory().newEmbeddedDatabase(neo4jFile)

    Try (
      graph.beginTx()
    ) match {
      case Success(tx) =>
        val schema = graph.schema()

        createIfNeededUniqueConstraint(schema, "CDM17", "uuid")

        createIfNeededIndex(schema, "Subject", "timestampNanos")
        createIfNeededIndex(schema, "Subject", "cid")
        createIfNeededIndex(schema, "Subject", "cmdLine")
        createIfNeededIndex(schema, "RegistryKeyObject", "registryKeyOrPath")
        createIfNeededIndex(schema, "NetFlowObject", "remoteAddress")
        createIfNeededIndex(schema, "Event", "timestampnanos")
        createIfNeededIndex(schema, "Event", "name")
        createIfNeededIndex(schema, "Event", "predicateObjectPath")

        tx.success()
        tx.close()

        awaitSchemaCreation(graph)

        //schema.awaitIndexesOnline(10, TimeUnit.MINUTES)
      case Failure(err) => err.printStackTrace()
    }
    graph
  }

  def awaitSchemaCreation(g: GraphDatabaseService) = {
    Try (
      g.beginTx()
    ) match {
      case Success(tx) =>
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
      case Failure(error) => ()
    }
  }

  def findConstraint(schema: Schema, label: Label, prop: String): Boolean = {
    val constraints = schema.getConstraints(label).asScala
    constraints.exists { c =>
      val constrainedProps = c.getPropertyKeys.asScala
      constrainedProps.size == 1 && constrainedProps.exists(_.equals(prop))
    }
  }

  def createIfNeededUniqueConstraint(schema: Schema, labelString: String, prop: String) = {
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

  def createIfNeededIndex(schema: Schema, slabel: String, prop: String) = {
    val label = Label.label(slabel)
    if(! findIndex(schema, label, prop)) {
      schema.indexFor(label).on(prop).create()
    }
  }

  // Create a neo4j transaction to insert a batch of objects
  def neo4jTx(cdms: Seq[DBNodeable], g: GraphDatabaseService): Try[Unit] = {
    val transaction = g.beginTx()

    // For the duration of the transaction, we keep a 'Map[UUID -> NeoNode]' of vertices created
    // during this transaction (since we don't look those up in the existing database).
    val newVertices = MutableMap.empty[UUID, NeoNode]

    // We also need to keep track of edges that point to nodes we haven't found yet (this lets us
    // handle cases where nodes are out of order).
    val missingToUuid = MutableMap.empty[UUID, Set[(NeoNode, EdgeTypes)]]

    val skipEdgesToThisUuid = new UUID(0L, 0L) //.fromString("00000000-0000-0000-0000-000000000000")

    val cdmToNodeResults = cdms map { cdm =>
      Try {
        val newNeo4jVertex = g.createNode()

        val cdmTypeName = cdm.getClass.getSimpleName
        newNeo4jVertex.addLabel(Label.label(cdmTypeName))  // Creating labels are like creating indices. An index for all Events requires a lot of memory.
//        newNeo4jVertex.addLabel(Label.label("CDM17"))  // TODO: Why is the presence of this label changing how the UI renders the nodes?


        cdm.asDBKeyValues.foreach { case (k,v) =>
          newNeo4jVertex.setProperty(k, v.toString)
        }
        newNeo4jVertex.setProperty("db_label", cdmTypeName)   // ui_language.js in the UI depends on this property being set to the CDM type.

        newVertices += (cdm.getUuid -> newNeo4jVertex)

        cdm.asDBEdges.foreach {
          case (edgeName, toUuid) =>
            if (toUuid != skipEdgesToThisUuid) newVertices.get(toUuid) match {
              case Some(toNeo4jVertex) =>
                newNeo4jVertex.createRelationshipTo(toNeo4jVertex, edgeName)
              case None =>
                missingToUuid(toUuid) = missingToUuid.getOrElse(toUuid, Set.empty[(NeoNode, EdgeTypes)]) + (newNeo4jVertex -> edgeName)
            }
        }
//      } match {
//        case Success(_) =>
//        case Failure(e: ConstraintViolationException) =>
//          if (!e.getMessage.contains("uuid")) {
//            println("Failed CDM statement: " + cdm)
//            println(e.getMessage) // Bad query
//            e.printStackTrace()
//          }
//        case Failure(e) => println(s"Continuing after unknown exception:\n${e.printStackTrace()}")
        cdm.getUuid
      }
    }

    // Try to complete missing edges. If the node pointed to is _still_ not found, we
    // synthetically create it.
    var nodeCreatedCounter = 0
    var edgeCreatedCounter = 0

    for ((uuid, edges) <- missingToUuid) {
      for ((fromNeo4jVertex, label) <- edges) {
        if (uuid != skipEdgesToThisUuid) {
          // Find or create the missing vertex (it may have been created earlier in this loop)
          val toNeo4jVertex = newVertices.getOrElse(uuid, {
            nodeCreatedCounter += 1
            val newNode = g.createNode() //Label.label("CDM17"))
            newNode.setProperty("uuid", UUID.randomUUID().toString) // uuid
            newVertices += (uuid -> newNode)
            newNode
          })

          // Create the missing edge
          Try {
            fromNeo4jVertex.createRelationshipTo(toNeo4jVertex, label)
            edgeCreatedCounter += 1
          } match {
            case Success(_) =>
            case Failure(e: java.lang.IllegalArgumentException) =>
              if (!e.getMessage.contains("byUuidUnique")) {
                println(e.getMessage) // Bad query
                e.printStackTrace()
              }
            case Failure(e) => println(s"Continuing after unknown exception:\n${e.printStackTrace()}")
          }
        }
      }
    }

    Try {
      transaction.success()
      transaction.close()
      cdmToNodeResults.foreach(_.get)
    }
  }

  // Loop indefinetly over locking failures
  def retryFinalLoop(cdms: Seq[DBNodeable], g: GraphDatabaseService): Boolean = {
    neo4jTx(cdms, g) match {
      case Success(_) => false
      case Failure(f) => throw f.getCause.getCause
    }
  }

  // Loops on insertion until we either insert the entire batch
  // A single statement may fail to insert but only after ln(batch size)+1 attempts
  // All ultimate failure errors are collected in the sequence and surfaced to the top.
  // If all statements insert successfully eventually then Seq(Success(())) is returned.
  def neo4jLoop(cdms: Seq[DBNodeable], g: GraphDatabaseService): Seq[Try[Unit]] = {
    neo4jTx(cdms, g) match {
      case Success(()) => Seq(Success(()))
      case Failure(_) =>
        // If we're trying to ingest a single CDM statement, try it one more time before giving up
        if (cdms.length == 1) {
          // Loop indefinetly over locking failures
          Try {
                while (retryFinalLoop(cdms, g)) {
                  Thread.sleep(scala.util.Random.nextInt(100))
                }
          } match {
            case Failure(f) =>
              // If we saw a non-locking failure, try one more time to insert before reporting failure
              println("Final retry for statement")
              Seq(neo4jTx(cdms, g))
            case _ => Seq(Success())
          }
        } else {
          // Split the list of CDM objects in half (less likely to have object contention for each half of the list) and loop on insertion
          val (front, back) = cdms.splitAt(cdms.length / 2)
          neo4jLoop(front, g) match {
            case Seq(Success(_)) => neo4jLoop(back, g)
            case fails1 => neo4jLoop(back, g) match {
              case Seq(Success(_)) => fails1
              case fails2 => fails1 ++ fails2
            }
          }
        }
    }
  }

  /* Given a 'GraphDatabaseService', make a 'Flow' that writes CDM data into that graph in a buffered manner
   */
  def neo4jWrites(g: GraphDatabaseService)(implicit ec: ExecutionContext) = Flow[CDM17]
    .collect { case cdm: DBNodeable => cdm }
    .groupedWithin(1000, 1 second)
    .map(neo4jTx(_, g))
//    .map(neo4jLoop)
//    .mapAsyncUnordered(threadPool)(x => Future {neo4jLoop(x)})
//    .recover{ case e => e.printStackTrace(); ???}
//    .printCounter("DB Writer", 1000)
    .toMat(
      Sink.foreach {
        case Success(_) => ()
        case Seq(Success(())) => ()
        case fails => println(s"$fails. insertion errors in batch")
      }
    )(Keep.right)

  implicit

  def neo4jActorWrite(neoActor: ActorRef)(implicit timeout: Timeout) = Flow[CDM17]
    .collect { case cdm: DBNodeable => cdm }
    .groupedWithin(1000, 1 second)
    .map(WriteToNeo4jDB.apply)
//    .map(cdm => WriteToNeo4jDB(Seq(cdm)))
    .toMat(Sink.actorRefWithAck(neoActor, InitMsg, Success(()), CompleteMsg, FailureMsg.apply))(Keep.right)
//    .mapAsync(1)(cdms => (neoActor ? WriteToNeo4jDB(cdms)).mapTo[Try[Unit]])
//    .toMat(
//      Sink.foreach {
//        case Success(_) => ()
//        case Seq(Success(())) => ()
//        case fails => println(s"$fails. insertion errors in batch")
//      }
//    )(Keep.right)

  def neo4jActorWriteFlow(neoActor: ActorRef)(implicit timeout: Timeout) = Flow[CDM17]
    .collect { case cdm: DBNodeable => cdm }
    .groupedWithin(1000, 1 second)
    .map(WriteToNeo4jDB.apply)
    .mapAsync(1)(msg => (neoActor ? msg).mapTo[Try[Unit]])
//    .map(cdm => WriteToNeo4jDB(Seq(cdm)))
//    .toMat(Sink.actorRefWithAck(neoActor, InitMsg, Success(()), CompleteMsg, FailureMsg.apply))(Keep.right)
}

case class FailureMsg(e: Throwable)
case object CompleteMsg
case object InitMsg
