package com.galois.adapt

import java.util.UUID
import akka.actor.ActorRef
import akka.stream.scaladsl.{Flow, Keep, Sink}
import com.galois.adapt.cdm17.CDM17
import com.galois.adapt.cdm17.CDM17.EdgeTypes.EdgeTypes
import com.typesafe.config.ConfigFactory
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.structure.Direction
import org.neo4j.graphdb.{ConstraintViolationException, GraphDatabaseService, Label, MultipleFoundException, Node => NeoNode}
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
  val shouldLogDuplicates = config.getBoolean("adapt.ingest.logduplicates")

  // Create a neo4j transaction to insert a batch of objects
  def neo4jTx(cdms: Seq[DBNodeable], g: GraphDatabaseService): Try[Unit] = {
    val transaction = g.beginTx()

    // For the duration of the transaction, we keep a 'Map[UUID -> NeoNode]' of vertices created
    // during this transaction (since we don't look those up in the existing database).
    val newVerticesInThisTX = MutableMap.empty[UUID, NeoNode]

    val skipEdgesToThisUuid = new UUID(0L, 0L) //.fromString("00000000-0000-0000-0000-000000000000")

    val cdmToNodeResults = cdms map { cdm =>
      Try {
        val cdmTypeName = cdm.getClass.getSimpleName
        val thisNeo4jVertex = newVerticesInThisTX.getOrElse(cdm.getUuid,
          g.createNode(Label.label("CDM"), Label.label(cdmTypeName))
        )
        // NOTE: The UI expects a specific format and collection of labels on each node.
        // Making a change to the labels on a node will need to correspond to a change made in the UI javascript code.

        cdm.asDBKeyValues.foreach {
          case (k, v: UUID) => thisNeo4jVertex.setProperty(k, v.toString)
          case (k,v) => thisNeo4jVertex.setProperty(k, v)
        }

        newVerticesInThisTX += (cdm.getUuid -> thisNeo4jVertex)

        cdm.asDBEdges.foreach { case (edgeName, toUuid) =>
          if (toUuid != skipEdgesToThisUuid) newVerticesInThisTX.get(toUuid) match {
            case Some(toNeo4jVertex) =>
              thisNeo4jVertex.createRelationshipTo(toNeo4jVertex, edgeName)
            case None =>
              val destinationNode = Option(g.findNode(Label.label("CDM"), "uuid", toUuid.toString)).getOrElse {
                newVerticesInThisTX(toUuid) = g.createNode(Label.label("CDM"))
                newVerticesInThisTX(toUuid)
              }
              thisNeo4jVertex.createRelationshipTo(destinationNode, edgeName)
          }
        }
        Some(cdm.getUuid)
      }.recoverWith {
        case e: ConstraintViolationException =>
          if (shouldLogDuplicates) println(s"Skipping duplicate creation of node: ${cdm.getUuid}")
          Success(None)
    //  case e: MultipleFoundException => Should never find multiple nodes with a unique constraint on the `uuid` field
        case e =>
          e.printStackTrace()
          Failure(e)
      }
    }


    val results = cdmToNodeResults //++ missingEdgeCreationResults

    Try {
      if (results.forall(_.isSuccess))
        transaction.success()
      else {
        println(s"TRANSACTION FAILURE! CDMs:\n$cdms")
        transaction.failure()
      }
      transaction.close()
    }
  }

  // Loop indefinitely over locking failures
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

  def neo4jActorWrite(neoActor: ActorRef)(implicit timeout: Timeout) = Flow[CDM17]
    .collect { case cdm: DBNodeable => cdm }
    .groupedWithin(1000, 1 second)
    .map(WriteToNeo4jDB.apply)
    .toMat(Sink.actorRefWithAck(neoActor, InitMsg, Success(()), CompleteMsg, FailureMsg.apply))(Keep.right)

  def neo4jActorWriteFlow(neoActor: ActorRef)(implicit timeout: Timeout) = Flow[CDM17]
    .collect { case cdm: DBNodeable => cdm }
    .groupedWithin(1000, 1 second)
    .map(WriteToNeo4jDB.apply)
    .mapAsync(1)(msg => (neoActor ? msg).mapTo[Try[Unit]])
}

case class FailureMsg(e: Throwable)
case object CompleteMsg
case object InitMsg
