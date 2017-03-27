package com.galois.adapt

import java.io.ByteArrayOutputStream

import akka.actor._
import com.galois.adapt.cdm16._
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.io.IoCore
import org.apache.tinkerpop.gremlin.structure.{Graph, Edge, Vertex}
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.structure.T.label
import java.nio.file.{Files, Paths}
import java.util.UUID

import com.galois.adapt.ServiceRegistryProtocol.SubscribeToService
import com.thinkaurelius.titan.core.{TitanTransaction, TitanFactory, TitanVertex}
import com.thinkaurelius.titan.graphdb.database.management.{ManagementSystem}
import com.thinkaurelius.titan.core.schema.{TitanGraphIndex,SchemaAction, SchemaStatus}
import org.apache.tinkerpop.gremlin.structure.io.graphson._

import collection.JavaConverters._
import scala.util.Try
import scala.concurrent.duration._ // {FiniteDuration, Duration}


class DevDBActor(val registry: ActorRef, localStorage: Option[String] = None)
  extends Actor with ActorLogging with ServiceClient with SubscriptionActor[Nothing] {

  // Convert Java Optional to scala Option
  implicit def java2ScalaOption[T](o: java.util.Optional[T]): Option[T] =
    if (o.isPresent()) { Some(o.get) } else { None }
 
  // The DB only needs to wait on / subscribe to the 'FileIngestActor'
  val dependencies = "FileIngestActor" :: Nil
  lazy val subscriptions = Set[Subscription](Subscription(dependencyMap("FileIngestActor").get, _.isInstanceOf[CDM16]))

  def beginService() = initialize()
  def endService() = ()


  val graph = TitanFactory.build.set("storage.backend", "cassandra").set("storage.hostname","localhost").open
  // val graph = TitanFactory.build.set("storage.backend", "inmemory").open


  // The following is supposed to set up a 'uuid' index (since we do a lot of lookups by UUID).
  // unfortunately, based on the log messages, it isn't doing that. In fact, it doesn't seem to be
  // doing anything at all.  -.-
  var management: ManagementSystem = graph.openManagement().asInstanceOf[ManagementSystem]
  if (null == management.getGraphIndex("byUuidUnique")) {
    
    var idKey = if (management.getPropertyKey("uuid") != null) {
      management.getPropertyKey("uuid")
    } else {
      management.makePropertyKey("uuid").dataType(classOf[UUID]).make()
    }
    management.buildIndex("byUuidUnique", classOf[Vertex]).addKey(idKey).unique().buildCompositeIndex()
    management.commit()
    graph.tx().commit()

    management = graph.openManagement().asInstanceOf[ManagementSystem]
    idKey = management.getPropertyKey("uuid")
    val idx = management.getGraphIndex("byUuidUnique")
    if (idx.getIndexStatus(idKey).equals(SchemaStatus.INSTALLED)) {
      management.commit()
      ManagementSystem.awaitGraphIndexStatus(graph, "byUuidUnique").status(SchemaStatus.REGISTERED).call()
    } else { management.commit() }

    management = graph.openManagement().asInstanceOf[ManagementSystem]
    management.updateIndex(management.getGraphIndex("byUuidUnique"), SchemaAction.ENABLE_INDEX).get()
    management.commit()
    ManagementSystem.awaitGraphIndexStatus(graph, "byUuidUnique").status(SchemaStatus.ENABLED).call()

  } else { management.commit() }


  // Optionally load in existing data:
  localStorage.foreach( path =>
    if (Files.exists(Paths.get(path))) graph.io(IoCore.graphson()).readGraph(path)
  )

 
  // Buffer the nodes to write
  var buffered = collection.mutable.ListBuffer.empty[DBNodeable]

  // Empty the buffer of accumulated nodes.
  // Don't call this directly, use 'triggerTransaction'
  def emptyBuffer(transaction: TitanTransaction): Unit = {
    
    log.info("Emptying buffer")
     
    for (cdm <- buffered) {
      // Get the uuid of the node
      val uuid = cdm.getUuid
     
      // Only do something if you can't find the node already
      if (!findNode("uuid",uuid).isDefined) {
        
        // Create a new vertex with the properties on the node
        val props: List[Object] = cdm.asDBKeyValues.asInstanceOf[List[Object]]
        assert(props.length % 2 == 0, s"Node ($cdm) has odd length properties list: $props.")

        val newTitanVertex: TitanVertex = transaction.addVertex(props: _*)
      
        // Add this vertex to the map of vertices we know about and see if it is the destination of
        // any previous nodes (see next comment for more on this).
        nodeIdsCurrent += (uuid -> newTitanVertex)
        
        incrementWrites()
        
        // See if this is the destination of any previous node from this transaction
        for ((fromTitanVertex,label) <- missingToUuidCurrent.getOrElse(uuid,Nil)) {
          fromTitanVertex.addEdge(label, newTitanVertex)
          incrementWrites()
        }
        missingToUuidCurrent -= uuid
 
        // See if this is the destination of any previous node from a past transaction
        for ((fromTitanVertex,label) <- missingToUuidPast.getOrElse(uuid,Nil)) {
          transaction.getVertex(fromTitanVertex.longId()).addEdge(label, newTitanVertex)
          //fromTitanVertex.addEdge(label, newTitanVertex)
          incrementWrites()
        }
        missingToUuidPast -= uuid
     
        // Recall all edges are treated as outgoing. In general, we expect that the 'toUuid' has
        // already been found. However, if it hasn't, we add it to a map of edges keyed by the UUID
        // they point to (for which no corresponding vertex exists, as of yet). 
        for ((label,toUuid) <- cdm.asDBEdges)
          findNode("uuid",toUuid) match {
            case None =>
              missingToUuidCurrent(toUuid) = (newTitanVertex, label) :: missingToUuidCurrent.getOrElse(toUuid,Nil) 
            case Some(toTitanVertex) =>
              assert(toTitanVertex != null, "findNode should never return null")
              newTitanVertex.addEdge(label, toTitanVertex)
              incrementWrites()
          }
      }
    }

    buffered.clear()
    log.info("Buffer emptied")
  }
      

  /* These four maps represent the state the DB actor accumulates.
   *   
   *  1. The first two are straightforward: they keep track of UUID -> TitanVertex mapping. We need
   *     _two_ maps to differentiate vertices that need to be refreshed from those that don't.
   *
   *  2. The second two represent:
   *   (UUID that was destination of edge not in `nodeIds` at the time) -> (source TitanVertex, edge label)
   *     As before, they distinguish between vertices that need to be refreshed from those that
   *     don't
   */
  var nodeIdsPast    = collection.mutable.Map.empty[UUID, TitanVertex]
  var nodeIdsCurrent = collection.mutable.Map.empty[UUID, TitanVertex]
  var missingToUuidPast    = collection.mutable.Map.empty[UUID, List[(TitanVertex,String)]]
  var missingToUuidCurrent = collection.mutable.Map.empty[UUID, List[(TitanVertex,String)]]

  // Sometimes, we need to empty out the maps of 'missingToUuid...'. This function does that and
  // returns a tuple of (# of nodes created, # of edges created)
  // Don't call this directly, use 'triggerTransaction'
  def completeMissingEdges(transaction: TitanTransaction): (Int,Int) = {
    var nodeCreatedCounter = 0
    var edgeCreatedCounter = 0
 
    for ((uuid,edges) <- missingToUuidCurrent; (fromTitanVertex,label) <- edges) {
       
      // Find or create the missing vertex (it may have been created earlier in this loop)
      val toTitanVertex = findNode("uuid",uuid) getOrElse {
        nodeCreatedCounter += 1
        val newNode = transaction.addVertex("uuid", uuid)
        nodeIdsCurrent += (uuid -> newNode)
        newNode
      }

      // Create the missing edge
      edgeCreatedCounter += 1
      fromTitanVertex.addEdge(label, toTitanVertex)
    }

    for ((uuid,edges) <- missingToUuidPast; (fromTitanVertex,label) <- edges) {
       
      // Find or create the missing vertex (it may have been created earlier in this loop)
      val toTitanVertex = findNode("uuid",uuid) getOrElse {
        nodeCreatedCounter += 1
        val newNode = transaction.addVertex("uuid", uuid)
        nodeIdsCurrent += (uuid -> newNode)
        newNode
      }

      // Create the missing edge
      edgeCreatedCounter += 1
      transaction.getVertex(fromTitanVertex.longId()).addEdge(label, toTitanVertex)   // <- refresh the vertex here!
      // fromTitanVertex.addEdge(label, toTitanVertex)   // <- refresh the vertex here!
    }

    // Empty out the map
    // TODO: when should we empty out the nodeId's map?
    missingToUuidCurrent = collection.mutable.Map.empty
    missingToUuidPast = collection.mutable.Map.empty

    log.info("Complete missing edges")

    (nodeCreatedCounter,edgeCreatedCounter)
  }

  // This gets called before closing a transaction. It manages moving nodes over from the 'current'
  // maps to the 'past' maps.
  // Don't call this directly, use 'triggerTransaction'
  def prepareToCloseTransaction(): Unit = {
    // Move current node IDs to past node IDs
    nodeIdsPast ++= nodeIdsCurrent
    nodeIdsCurrent = collection.mutable.Map.empty[UUID,TitanVertex]

    // Move current dangling edges to past dangling edges
    missingToUuidPast = missingToUuidPast ++ missingToUuidCurrent.map {
      case (k,v) => k -> (v ++ missingToUuidPast.getOrElse(k,List()))
    }
    missingToUuidCurrent = collection.mutable.Map.empty[UUID, List[(TitanVertex,String)]]
  }

  // Record last time a transaction was triggered
  var lastTransactionTime: Long = 0

  // Create a transaction, empty the buffer of pending CDM, possibly complete edges (by generating
  // synthetic nodes), update the maps of ids, and then commit the transaction.
  def triggerTransaction(completeEdges: Boolean): Option[(Int,Int)] = {
    log.info("Opening a new transaction")
    val transaction = graph.newTransaction()

    emptyBuffer(transaction)
    val ret = if (completeEdges) {
      log.info(s"To nodes missed: ${missingToUuidCurrent.size + missingToUuidPast.size}")
      val (nodesCreated,edgesCreated) = completeMissingEdges(transaction)
      log.info(s"Nodes created: $nodesCreated, edges created: $edgesCreated")
      Some((nodesCreated,edgesCreated))
    } else {
      None
    }
    prepareToCloseTransaction()

    transaction.commit()
    log.info("Committed transaction")

    // Update last time a transaction was triggered
    lastTransactionTime = System.currentTimeMillis()

    ret
  }

  def findNode(key: String, value: Any): Option[Vertex] = {

    // For UUIDs, we can use our maps. Anything else requires a DB query
    if (key == "uuid" && value.isInstanceOf[UUID]) {
      val uuid = value.asInstanceOf[UUID]

      // Lookup the key in our maps. If we don't find it in the current nodeIds (those from this
      // transaction), look in the past nodeIds (from previous transactions) but remember to refresh
      // anything we get back from there.
      nodeIdsCurrent.get(uuid) orElse graph.traversal().V().has("uuid",uuid).tryNext()
      
     // nodeIdsPast.get(uuid).map(v => transaction.getVertex(v.longId()))
      //nodeIdsCurrent.get(uuid) orElse nodeIdsPast.get(uuid)
    } else {
      graph.traversal().V().has(key,value).tryNext()
    }
  }

  /* Writes to the DB need to happen every 1 second or every 1000 writes, whichever comes
   * first. To that end we:
   *
   *   - count every write and, when that number passes 1000, we trigger a write
   *   - after every message, we trigger a write if the time since last write is greater than a second
   *   - we send a 'heartbeat' message every second (until we finish ingesting) to trigger the above
   *     in the event there is a long period without messages
   *   - trigger a write at epoch boundaries and the end of ingestion
   *
   * Of these, only at epoch and end of ingestion do we try to resolve edges.
   */   

  implicit val ec = context.dispatcher

  // Schedule a 'heartbeat' message to make sure transactions occur regularly
  val heartbeat: Cancellable = context.system.scheduler.schedule(1 second, 1 second)(self ! Heartbeat)

  // Count the number of writes that we get. Whenever this number exceeds 'writesThreshold', trigger
  // a transaction
  var numWrites = 0
  val writesThreshold = 1000
  def incrementWrites(): Unit = {
    numWrites += 1
    if (numWrites > writesThreshold) {
      triggerTransaction(false)
      numWrites = 0
    }
  }


  // 'process' does the same work as 'messageHandler', but additionally tries to trigger a
  // transaction after every message.
  var doneIngest = false
  override def process: PartialFunction[Any,Unit] = messageHandler andThen { _ =>
    if (!doneIngest && (System.currentTimeMillis() - lastTransactionTime) > 1000)
      triggerTransaction(false)
  }

  // Main handler of messages. Due to type inference issues, it is better to put this inside its own
  // function (instead of trying to inline it in 'process')
  def messageHandler: PartialFunction[Any,Unit] = {

    case DoneIngest =>
      println("DONE INGEST")
      triggerTransaction(true)
      heartbeat.cancel()
      doneIngest = true
    //   broadCastUnsafe(DoneDevDB(Some(graph), missingToUuidCurrent.size + missingToUuidPast.size))

    case c: IngestControl => broadCastUnsafe(c)

    case EpochMarker =>
      println("EPOCH BOUNDARY")
      triggerTransaction(true)
    
    case Heartbeat => // Do nothing
    
    case cdm16: DBNodeable =>
      buffered += cdm16
      incrementWrites()

    case NodeQuery(q) =>
      sender() ! Query.run[Vertex](q, graph).map { vertices =>

        // Give a lower bound on the number of vertices
        Application.debug(s"Found: ${if (vertices.lengthCompare(1000) > 0) "> 1000" else vertices.length}")
        
        // Generate JSON to send back
        val byteStream = new ByteArrayOutputStream
        GraphSONWriter.build().create().writeVertices(byteStream, vertices.toIterator.asJava)

        byteStream.toString.split("\n").map { v =>
          if (v.length > 0) {
            val (first, second) = v.splitAt(1)
            first + """"type":"vertex",""" + second
          } else v
        }.mkString("[", ",", "]")
      }

    case EdgeQuery(q) =>
      sender() ! Query.run[Edge](q, graph).map { edges =>

        // Give a lower bound on the number of vertices
        Application.debug(s"Found: ${if (edges.lengthCompare(1000) > 0) "> 1000" else edges.length}")
        
        // Generate JSON to send back
        val byteStream = new ByteArrayOutputStream
        edges.foreach { edge => GraphSONWriter.build().create().writeEdge(byteStream, edge) }
        byteStream.toString.split("\\}\\{").mkString("[", "},{", "]")
     }

    case StringQuery(q) =>
      sender() ! Query.run[java.lang.Object](q, graph).map { results =>  
        
        // Give a lower bound on the number of vertices
        Application.debug(s"Found: ${results.length}")

        // Generate JSON to send back
        results.map(r => s""""${r.toString.replace("\\", "\\\\").replace("\"", "\\\"")}"""")
               .mkString("[",",","]")
      }
   
    case EdgesForNodes(nodeIdList) =>
      sender() ! Try {
        graph.traversal().V(nodeIdList.asJava.toArray).bothE().toList.asScala.mkString("[",",","]")
      }

    case GiveMeTheGraph => sender() ! graph

    case Shutdown =>
      val missing = missingToUuidCurrent.size + missingToUuidPast.size
      log.info(s"Incomplete Edge count: $missing")
      localStorage.foreach(path => graph.io(IoCore.graphson()).writeGraph(path))
      sender() ! missing
  }
}

// Messages this actor (or the actor system via a Scheduler) sends itself
case object Heartbeat

case class DoneDevDB(graph: Option[TinkerGraph], incompleteEdgeCount: Int)

sealed trait RestQuery { val query: String }
case class NodeQuery(query: String) extends RestQuery
case class EdgeQuery(query: String) extends RestQuery
case class StringQuery(query: String) extends RestQuery

case class EdgesForNodes(nodeIdList: Seq[Int])
case object GiveMeTheGraph
case object Shutdown



