package com.galois.adapt

import com.typesafe.config.ConfigFactory
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.graphdb.schema.Schema
import org.neo4j.kernel.api.exceptions.schema.AlreadyConstrainedException
import org.neo4j.tinkerpop.api.impl.Neo4jGraphAPIImpl
import akka.actor._
import org.apache.tinkerpop.gremlin.structure.{Edge, Graph, Vertex}
import org.neo4j.graphdb.{GraphDatabaseService, Label, Node => NeoNode}
import spray.json._

import collection.mutable.{Map => MutableMap}
import collection.JavaConverters._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class Neo4jDBQueryProxy extends Actor with ActorLogging {
  implicit val ec = context.dispatcher
  val config = ConfigFactory.load()

  /* Open a Neo4j graph database and create indices */
  val neoGraph = {
    val neo4jFile: java.io.File = new java.io.File(config.getString("adapt.runtime.neo4jfile"))
    val graphService = new GraphDatabaseFactory().newEmbeddedDatabase(neo4jFile)
    context.system.registerOnTermination(graphService.shutdown())

    def awaitSchemaCreation(g: GraphDatabaseService) = {
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

    def createIfNeededIndex(schema: Schema, labelString: String, prop: String) = {
      val label = Label.label(labelString)
      if(! findIndex(schema, label, prop)) {
        schema.indexFor(label).on(prop).create()
      }
    }

    Try (
      graphService.beginTx()
    ) match {
      case Success(tx) =>
        val schema = graphService.schema()

        createIfNeededUniqueConstraint(schema, "CDM", "uuid")

        // NOTE: The UI expects a specific format and collection of labels on each node.
        // Making a change to the labels on a node will need to correspond to a change made in the UI javascript code.

        createIfNeededIndex(schema, "Subject", "timestampNanos")
        createIfNeededIndex(schema, "Subject", "cid")
        createIfNeededIndex(schema, "Subject", "cmdLine")
        createIfNeededIndex(schema, "RegistryKeyObject", "registryKeyOrPath")
        createIfNeededIndex(schema, "NetFlowObject", "localAddress")
        createIfNeededIndex(schema, "NetFlowObject", "localPort")
        createIfNeededIndex(schema, "NetFlowObject", "remoteAddress")
        createIfNeededIndex(schema, "NetFlowObject", "remotePort")
        createIfNeededIndex(schema, "FileObject", "peInfo")
        createIfNeededIndex(schema, "Event", "timestampNanos")
        createIfNeededIndex(schema, "Event", "name")
        createIfNeededIndex(schema, "Event", "eventType")
        createIfNeededIndex(schema, "Event", "predicateObjectPath")

        tx.success()
        tx.close()

        awaitSchemaCreation(graphService)

        //schema.awaitIndexesOnline(10, TimeUnit.MINUTES)
      case Failure(err) => err.printStackTrace()
    }
    graphService
  }
  val graph: Graph = Neo4jGraph.open(new Neo4jGraphAPIImpl(neoGraph))

  var counter = 0L

  def receive = {
    case Ready => sender() ! Ready

    // Run the given query with the expectation that the output type be vertices. Optionally encode
    // the resulting list into JSON
    case NodeQuery(q, shouldParse) =>
      println(s"Received node query: $q")
      sender() ! Future(
        Query.run[Vertex](q, graph).map { vertices =>
          println(s"Found: ${vertices.length} nodes")
          if (shouldParse)
            JsArray(vertices.map(ApiJsonProtocol.vertexToJson).toVector)
          else
            vertices
        }
      )

    // Run the given query with the expectation that the output type be edges. Optionally encode
    // the resulting list into JSON
    case EdgeQuery(q, shouldParse) =>
      println(s"Received new edge query: $q")
      sender() ! Future(
        Query.run[Edge](q, graph).map { edges =>
          println(s"Found: ${edges.length} edges")
          if (shouldParse)
            JsArray(edges.map(ApiJsonProtocol.edgeToJson).toVector)
          else
            edges
        }
      )

    // Run the given query without specifying what the output type will be. This is the variant used
    // by 'cmdline_query.py'
    case StringQuery(q, shouldParse) =>
      println(s"Received string query: $q")
      sender() ! Future(
        Query.run[java.lang.Object](q, graph).map { results =>
          println(s"Found: ${results.length} items")
          if (shouldParse) {
            toJson(results.toList) 
          } else {
            JsString(results.map(r => s""""${r.toString.replace("\\", "\\\\").replace("\"", "\\\"")}"""").mkString("[",",","]"))
          }
        }
      )

    // Get all the edges that touch the given nodes (either incoming or outgoing) 
    case EdgesForNodes(nodeIdList) =>
      sender() ! Try {
        graph.traversal().V(nodeIdList.asJava.toArray).bothE().toList.asScala.mkString("[",",","]")
      }

    // Use with care! Unless you have a really good reason (like running acceptance tests), you
    // probably shouldn't be asking for the whole graph.
    case GiveMeTheGraph => sender() ! graph


    case WriteToNeo4jDB(cdms) =>
      counter = counter + cdms.size
//      log.info(s"DBActor received: $counter")
      sender() ! Neo4jFlowComponents.neo4jTx(cdms, neoGraph)

    case FailureMsg(e: Throwable) =>
      log.error(s"FAILED: {}", e)
    case CompleteMsg =>
      log.info(s"DBActor received a completion message")
      sender() ! Success(())
//      log.warning("shutting down...")
//      Runtime.getRuntime.halt(0)
    case InitMsg =>
      log.info(s"DBActor received an initialization message")
      sender() ! Success(())
  }

  import scala.collection.JavaConversions._

  def toJson: Any => JsValue = {
      
    // Numbers
    case n: Int => JsNumber(n)
    case n: Long => JsNumber(n)
    case n: Double => JsNumber(n)
    case n: java.lang.Long => JsNumber(n)
    case n: java.lang.Double => JsNumber(n)
                            
    // Strings 
    case s: String => JsString(s) 
            
    // Lists
    case l: java.util.List[_] => toJson(l.toList)
    case l: List[_] => JsArray(l map toJson)
                      
    // Maps
    case m: java.util.Map[_,_] => toJson(m.toMap)
    case m: Map[_,_] => JsObject(m map { case (k,v) => (k.toString, toJson(v)) })
                
    // Special cases (commented out because they are pretty verbose) and functionality is
    // anyways accessible via the "vertex" and "edges" endpoints
 //   case v: Vertex => ApiJsonProtocol.vertexToJson(v)
 //   case e: Edge => ApiJsonProtocol.edgeToJson(e)
                          
    // Other
                                    
    // Any custom 'toString' that is longer than 250 characters is probably not a good idea...
    case o => JsString(o.toString.take(250))
                                              
  }
}


sealed trait RestQuery { val query: String }
case class NodeQuery(query: String, shouldReturnJson: Boolean = true) extends RestQuery
case class EdgeQuery(query: String, shouldReturnJson: Boolean = true) extends RestQuery
case class StringQuery(query: String, shouldReturnJson: Boolean = false) extends RestQuery

case class EdgesForNodes(nodeIdList: Seq[Int])
case object GiveMeTheGraph
case object Ready

case class WriteToNeo4jDB(cdms: Seq[DBNodeable])
