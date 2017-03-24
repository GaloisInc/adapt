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
import com.thinkaurelius.titan.core.{TitanTransaction, TitanFactory}
import com.thinkaurelius.titan.core.schema.TitanGraphIndex
import org.apache.tinkerpop.gremlin.structure.io.graphson._

import collection.JavaConverters._
import scala.util.Try
import scala.concurrent.duration._ // {FiniteDuration, Duration}


class DBActor(val registry: ActorRef, localStorage: Option[String] = None)
  extends Actor with ActorLogging with ServiceClient with SubscriptionActor[Nothing] {

  implicit val ec = context.dispatcher

  // Tell the actor system to send us a 'CloseTransaction' message
  var closeNotifier: Option[Cancellable] = None
  def cancelCloseTransaction(): Unit = closeNotifier.foreach(_.cancel())
  def scheduleCloseTransaction(): Unit = {
    closeNotifier = Some(context.system.scheduler.scheduleOnce(1 seconds, self, CloseTransaction(true)))
  }
  
  // Count the number of writes that we get. Whenever this number exceeds 'writesThreshold', send us
  // a 'CloseTransaction' message
  var numWrites = 0
  val writesThreshold = 1000
  def incrementWrites(): Unit = {
    numWrites += 1
    if (numWrites > writesThreshold)
      self ! CloseTransaction(true)
  }

  // The DB only needs to wait on / subscribe to the 'FileIngestActor'
  val dependencies = "FileIngestActor" :: Nil
  lazy val subscriptions = Set[Subscription](Subscription(dependencyMap("FileIngestActor").get, _.isInstanceOf[CDM16]))

  def beginService() = {
    initialize()
    scheduleCloseTransaction()
  }
  def endService() = ()


 // val graph = TinkerGraph.open()   // TODO: maybe don't hold this state inside the actor...?
  val graph = TitanFactory.build.set("storage.backend", "inmemory").open

 // graph.createIndex("uuid", classOf[Vertex])
 // graph.createIndex("pid", classOf[Vertex])
//  graph.createIndex("url", classOf[Vertex])

  val management = graph.openManagement()
  management.buildIndex("uuid", classOf[Vertex]).unique()
  management.buildIndex("pid", classOf[Vertex])
  management.commit()
  
  var transaction: TitanTransaction = graph.newTransaction()

  // Optionally load in existing data:
  localStorage.foreach( path =>
    if (Files.exists(Paths.get(path))) graph.io(IoCore.graphson()).readGraph(path)
  )

  // These four maps represent the state the DB actor accumulates.
  //   
  //  1. The first two are straightforward: they keep track of UUID -> Vertex mapping. We need
  //     _two_ maps to differentiate vertices that need to be refreshed from those that don't.
  //
  //  2. The second two represent:
  //   (UUID that was destination of edge not in `nodeIds` at the time) -> (source Vertex, edge label)
  //     As before, they distinguish between vertices that need to be refreshed from those that
  //     don't
  var nodeIdsPast    = collection.mutable.Map.empty[UUID, Vertex]
  var nodeIdsCurrent = collection.mutable.Map.empty[UUID, Vertex]
  var missingToUuidPast    = collection.mutable.Map.empty[UUID, List[(Vertex,String)]]
  var missingToUuidCurrent = collection.mutable.Map.empty[UUID, List[(Vertex,String)]]

  // Sometimes, we need to empty out the maps of 'missingToUuid...'. This function does that and
  // returns a tuple of (# of nodes created, # of edges created)
  def completeMissingEdges(): (Int,Int) = {
    var nodeCreatedCounter = 0
    var edgeCreatedCounter = 0
 
    for ((uuid,edges) <- missingToUuidCurrent; (fromVertex,label) <- edges) {
       
      // Find or create the missing vertex (it may have been created earlier in this loop)
      val toVertex = findNode("uuid",uuid) getOrElse {
        nodeCreatedCounter += 1
        val newNode = graph.addVertex("uuid", uuid)
        nodeIdsCurrent += (uuid -> newNode)
        newNode
      }

      // Create the missing edge
      edgeCreatedCounter += 1
      fromVertex.addEdge(label, toVertex)
    }

    for ((uuid,edges) <- missingToUuidPast; (fromVertex,label) <- edges) {
       
      // Find or create the missing vertex (it may have been created earlier in this loop)
      val toVertex = findNode("uuid",uuid) getOrElse {
        nodeCreatedCounter += 1
        val newNode = graph.addVertex("uuid", uuid)
        nodeIdsCurrent += (uuid -> newNode)
        newNode
      }

      // Create the missing edge
      edgeCreatedCounter += 1
      // transaction.getVertex(fromVertex).addEdge(label, toVertex)   // <- refresh the vertex here!
      fromVertex.addEdge(label, toVertex)   // <- refresh the vertex here!
    }

    // Empty out the map
    // TODO: when should we empty out the nodeId's map?
    missingToUuidCurrent = collection.mutable.Map.empty
    missingToUuidPast = collection.mutable.Map.empty

    (nodeCreatedCounter,edgeCreatedCounter)
  }

  // TODO: TinkerGraph doesn't update the starting IDs when reading data in from a file.
  // Will result in: IllegalArgumentException: Vertex with id already exists: 0
  //  ... on the THIRD run. (after it _writes_ a file containing two of the same index ids)

  def findNode(key: String, value: Any): Option[Vertex] = {

    // For UUIDs, we can use our maps. Anything else requires a DB query
    if (key == "uuid" && value.isInstanceOf[UUID]) {
      val uuid = value.asInstanceOf[UUID]

      // Lookup the key in our maps. If we don't find it in the current nodeIds (those from this
      // transaction), look in the past nodeIds (from previous transactions) but remember to refresh
      // anything we get back from there.
      //nodeIdsCurrent.get(uuid) orElse nodeIdsPast.get(uuid).map(transaction.getVertex(_))
      nodeIdsCurrent.get(uuid) orElse nodeIdsPast.get(uuid)
    } else {
      graph.traversal().V().has(key,value).toList.asScala.headOption
    }
  }

  override def process: PartialFunction[Any,Unit] = {

    case CloseTransaction(scheduleNext) =>
      // End the current transaction by cancelling any pending notification and committing the
      // transaction
      cancelCloseTransaction()
      log.info("Committing a transcation")
      transaction.commit()
     
      // Move current node IDs to past node IDs
      nodeIdsPast ++= nodeIdsCurrent
      nodeIdsCurrent = collection.mutable.Map.empty[UUID,Vertex]

      // Reset the number of writes in the transaction
      numWrites = 0

      // Move current dangling edges to past dangling edges
      missingToUuidPast = missingToUuidPast ++ missingToUuidCurrent.map {
        case (k,v) => k -> (v ++ missingToUuidPast.getOrElse(k,List()))
      }
      missingToUuidCurrent = collection.mutable.Map.empty[UUID, List[(Vertex,String)]]

      // Schedule the next 'CloseTransaction' message and open a new transaction
      if (scheduleNext) scheduleCloseTransaction()
      log.info("Opening a new transaction")
      transaction = graph.newTransaction()

    case DoneIngest =>

      // Close the current transaction and don't schedule another 'CloseTransaction' message
      self ! CloseTransaction(scheduleNext = false)
    //  broadCastUnsafe(DoneDevDB(Some(graph), missingToUuidCurrent.size + missingToUuidPast.size))

    case c: IngestControl => broadCastUnsafe(c)

    case EpochMarker =>
      println("EPOCH BOUNDARY!")
      println(s"TO nodes missed during epoch: ${missingToUuidCurrent.size + missingToUuidPast.size}")
      println("Creating all missing nodes...")
     
      val (nodeCreatedCounter, edgeCreatedCounter) = completeMissingEdges()
      
      println(s"Nodes created at epoch close: $nodeCreatedCounter")
      println(s"Edges created at epoch close: $edgeCreatedCounter")
      println("Done creating all missing nodes.")

    case cdm15: DBNodeable =>
      // Get the uuid of the node
      val uuid = cdm15.getUuid

      // Create a new vertex with the properties on the node
      val props: List[Object] = cdm15.asDBKeyValues.asInstanceOf[List[Object]]
      assert(props.length % 2 == 0, s"Node ($cdm15) has odd length properties list: $props.")
      val newVertex: Vertex = graph.addVertex(props: _*)
      incrementWrites()

      // Add this vertex to the map of vertices we know about and see if it is the destination of
      // any previous nodes (see next comment for more on this).
      nodeIdsCurrent += (uuid -> newVertex)
      
      // See if this is the destination of any previous node from this transaction
      for ((fromVertex,label) <- missingToUuidCurrent.getOrElse(uuid,Nil)) {
        fromVertex.addEdge(label, newVertex)
        incrementWrites()
      }
      missingToUuidCurrent -= uuid

      // See if this is the destination of any previous node from a past transaction
      for ((fromVertex,label) <- missingToUuidPast.getOrElse(uuid,Nil)) {
        //transaction.getVertex(fromVertex).addEdge(label, newVertex)
        fromVertex.addEdge(label, newVertex)
        incrementWrites()
      }
      missingToUuidPast -= uuid
      
      // Recall all edges are treated as outgoing. In general, we expect that the 'toUuid' has
      // already been found. However, if it hasn't, we add it to a map of edges keyed by the UUID
      // they point to (for which no corresponding vertex exists, as of yet). 
      for ((label,toUuid) <- cdm15.asDBEdges)
        findNode("uuid",toUuid) match {
          case None => missingToUuidCurrent(toUuid) = (newVertex, label) :: missingToUuidCurrent.getOrElse(toUuid,Nil) 
          case Some(toVertex) =>
            newVertex.addEdge(label, toVertex)
            incrementWrites()
        }

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
case class CloseTransaction(scheduleNext: Boolean)

case class DoneDevDB(graph: Option[TinkerGraph], incompleteEdgeCount: Int)

sealed trait RestQuery { val query: String }
case class NodeQuery(query: String) extends RestQuery
case class EdgeQuery(query: String) extends RestQuery
case class StringQuery(query: String) extends RestQuery

case class EdgesForNodes(nodeIdList: Seq[Int])
case object GiveMeTheGraph
case object Shutdown

