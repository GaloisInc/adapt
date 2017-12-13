package com.galois.adapt

import java.io.ByteArrayOutputStream

import akka.actor._
import com.galois.adapt.cdm17._
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.io.IoCore
import org.apache.tinkerpop.gremlin.structure.{Edge, Vertex}
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import java.nio.file.{Files, Paths}
import java.util.UUID

//import com.galois.adapt.ServiceRegistryProtocol.SubscribeToService
import org.apache.tinkerpop.gremlin.structure.io.graphson._

import collection.JavaConverters._
import scala.util.Try

class DevDBActor(val registry: ActorRef, localStorage: Option[String] = None)
  extends Actor with ActorLogging { //with ServiceClient with SubscriptionActor[Nothing] with ReportsStatus {


//  def beginService() = {
//    log.info("Begin service")
//    initialize()
//  }
//  def endService() = ()  // TODO
//
//
//  def statusReport = {
//    Map("nodes received" -> nodesReceived)
//  }

  var nodesReceived = 0


  val graph = TinkerGraph.open()   // TODO: maybe don't hold this state inside the actor...?

  graph.createIndex("uuid", classOf[Vertex])
  graph.createIndex("pid", classOf[Vertex])

  // Optionally load in existing data:
  localStorage.foreach( path =>
    if (Files.exists(Paths.get(path))) graph.io(IoCore.graphson()).readGraph(path)
  )

  // The second map represents:
  //   (UUID that was destination of edge not in `nodeIds` at the time) -> (source Vertex, edge label)
  var nodeIds = collection.mutable.Map.empty[UUID, Vertex]
  var missingToUuid = collection.mutable.Map.empty[UUID, List[(Vertex,CDM17.EdgeTypes.EdgeTypes)]]


  // TODO: TinkerGraph doesn't update the starting IDs when reading data in from a file.
  // Will result in: IllegalArgumentException: Vertex with id already exists: 0
  //  ... on the THIRD run. (after it _writes_ a file containing two of the same index ids)

  def findNode(key: String, value: Any): Option[Vertex] = {
    if (key == "uuid" && value.isInstanceOf[UUID]) nodeIds.get(value.asInstanceOf[UUID])
    else graph.traversal().V().has(key,value).toList.asScala.headOption
  }

  def receive: PartialFunction[Any,Unit] = {

//    case DoneIngest => broadCastUnsafe(DoneDevDB(Some(graph), missingToUuid.toMap))

//    case c: IngestControl => broadCastUnsafe(c)

    case EpochMarker =>
      println("EPOCH BOUNDARY!")
      println(s"TO nodes missed during epoch: ${missingToUuid.size}")
      println("Creating all missing nodes...")
      
      var nodeCreatedCounter = 0
      var edgeCreatedCounter = 0
      
      // If at the end of an epoch there are still elements in `missingToUuid`, empty those out and
      // create placeholder vertices/edges for them.
      for ((uuid,edges) <- missingToUuid; (fromVertex,edgeName) <- edges) {
       
        // Find or create the missing vertex (it may have been created earlier in this loop)
        val toVertex = findNode("uuid",uuid) getOrElse {
          nodeCreatedCounter += 1
          val newNode = graph.addVertex("uuid", uuid)
          nodeIds += (uuid -> newNode)
          newNode
        }

        // Create the missing edge
        edgeCreatedCounter += 1
        //fromVertex.addEdge(edgeName, toVertex)
      }

      // Empty out the map
      // TODO: when should we empty out the nodeId's map?
      missingToUuid = collection.mutable.Map.empty

      println(s"Nodes created at epoch close: $nodeCreatedCounter")
      println(s"Edges created at epoch close: $edgeCreatedCounter")
      println("Done creating all missing nodes.")

    case cdm15: DBNodeable =>
      nodesReceived += 1

      val nodes = (cdm15.getUuid, cdm15.asDBKeyValues, cdm15.asDBEdges) :: cdm15.supportNodes
      
      for ((uuid, props, edges) <- nodes) {

        // Create a new vertex with the properties on the node
        assert(props.length % 2 == 0, s"Node ($uuid) has odd length properties list: $props.")
        val newVertex: Vertex = graph.addVertex(props.asInstanceOf[List[Object]]: _*)

        // Add this vertex to the map of vertices we know about and see if it is the destination of
        // any previous nodes (see next comment for more on this).
        nodeIds += (uuid -> newVertex)
        //for ((fromVertex,label) <- missingToUuid.getOrElse(uuid,Nil))
          //fromVertex.addEdge(label, newVertex)
        missingToUuid -= uuid

        // Recall all edges are treated as outgoing. In general, we expect that the 'toUuid' has
        // already been found. However, if it hasn't, we add it to a map of edges keyed by the UUID
        // they point to (for which no corresponding vertex exists, as of yet). 
//        for ((label,toUuid) <- edges)
//          nodeIds.get(toUuid) match {
//            case None => missingToUuid(toUuid) = (newVertex, label) :: missingToUuid.getOrElse(toUuid,Nil)
//            case Some(toVertex) => newVertex.addEdge(label, toVertex)
//          }
      }

    case NodeQuery(q,_) =>
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

    case EdgeQuery(q,_) =>
      sender() ! Query.run[Edge](q, graph).map { edges =>

        // Give a lower bound on the number of vertices
        Application.debug(s"Found: ${if (edges.lengthCompare(1000) > 0) "> 1000" else edges.length}")
        
        // Generate JSON to send back
        val byteStream = new ByteArrayOutputStream
        edges.foreach { edge => GraphSONWriter.build().create().writeEdge(byteStream, edge) }
        byteStream.toString.split("\\}\\{").mkString("[", "},{", "]")
     }

    case StringQuery(q,_) =>
      sender() ! Query.run[java.lang.Object](q, graph).map { results =>  
        
        // Give a lower bound on the number of vertices
        Application.debug(s"Found: ${results.length}")

        // Generate JSON to send back
        results.map(r => s""""${r.toString.replace("\\","\\\\").replace("\"","\\\"")}"""")
               .mkString("[",",","]")
      }
  }
}

case class DoneDevDB(graph: Option[TinkerGraph], incompleteEdgeCount: Map[UUID, List[(Vertex,String)]])

