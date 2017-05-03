package com.galois.adapt

import java.util
import java.util.{Properties, UUID}

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.http.scaladsl.marshalling._
import akka.util.Timeout
import org.mapdb.DBMaker
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import com.galois.adapt.cdm17.{CDM17, TimeMarker}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.tinkerpop.gremlin.structure.{Element => VertexOrEdge}

import scala.collection.mutable.{Map => MutableMap}
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Random, Success, Try}
import org.apache.tinkerpop.gremlin.structure.{Edge, Vertex}


class AnomalyManager(dbActor: ActorRef) extends Actor with ActorLogging {
  implicit val ec = context.dispatcher
  val anomalies = MutableMap.empty[UUID, MutableMap[String, (Double, Set[UUID])]]
  var threshold = 0.7D

  def query(uuid: UUID): Unit = {
    implicit val timeout = Timeout(10 seconds)
    val provQueryString = s"g.V().has('uuid',$uuid).as('tracedObject').union(_.in('flowObject').as('ptn').union(_.out('subject'),_).select('ptn').union(_.in('subject').has('eventType','EVENT_EXECUTE').out('predicateObject'),_.in('subject').has('eventType','EVENT_MMAP').out('predicateObject'),_).select('ptn').emit().repeat(_.out('prevTagId','tagId','subject','flowObject')).dedup().union(_,_.hasLabel('Subject').out('localPrincipal'),_.hasLabel('FileObject').out('localPrincipal'),_.hasLabel('Subject').emit().repeat(_.out('parentSubject'))).dedup(),_,_.in('predicateObject').has('eventType').out('parameterTagId').out('flowObject'),_.in('predicateObject2').has('eventType').out('parameterTagId').out('flowObject')).path().unrollPath().dedup()"
    val resultF = (dbActor ? NodeQuery(provQueryString, shouldReturnJson = false)).mapTo[Try[Stream[Vertex]]]
    resultF.map{x => println(x.get.head.value[UUID]("uuid")); query(anomalies.head._1)}.recover{ case e: Throwable => e.printStackTrace()}
  }


//  val topic: String = "test"
//  val brokers: String = "localhost:9092"
//  val props = new Properties()
//  props.put("bootstrap.servers", brokers)
//  props.put("client.id", "AdaptToTheiaRequestProducer")
//  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
//  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
//
//  val producer = new KafkaProducer[String, String](props)
//  val key = "test_key"
//  val msg = "test_message"
//  val data = new ProducerRecord[String, String](topic, TimeMarker(0L).toString)
//
//  producer.send(data)
//  producer.close()



//  context.system.scheduler.schedule(10 seconds, 10 seconds)(anomalies.headOption.map(_._1).map(query))



  def receive = {
    case card: RankingCard =>
      val existing = anomalies.getOrElse(card.keyNode, MutableMap.empty[String, (Double, Set[UUID])])

      if (card.suspicionScore < threshold) existing -= card.name
      else existing += (card.name -> (card.suspicionScore, card.subgraph))

      if (existing.isEmpty) anomalies -= card.keyNode
      else anomalies += (card.keyNode -> existing)

    case QueryAnomalies(uuids) =>
      sender() ! anomalies.filter(t => uuids.contains(t._1)).mapValues(_.toMap).toMap  //anomalies.getOrElse(uuid, MutableMap.empty[String, (Double, Set[UUID])])

    case GetRankedAnomalies(topK) =>
      sender() ! anomalies.toList.sortBy(_._2.values.map(_._1).sum)(Ordering.by[Double,Double](identity).reverse).take(topK).map(t => t._1 -> t._2.toMap)

    case SetThreshold(limit) => threshold = limit
  }
}

case class SetThreshold(threshold: Double)
case class QueryAnomalies(uuids: Seq[UUID])
case class GetRankedAnomalies(topK: Int = Int.MaxValue)



object ProdRoutes {

  val serveStaticFilesRoute =
    path("") {
      getFromResource("web/index.html")
    } ~
    path("graph") {
      getFromResource("web/graph.html")
    } ~
    pathPrefix("") {
      getFromResourceDirectory("web")
    }


  implicit val timeout = Timeout(20 seconds)

  def completedQuery[T <: VertexOrEdge](query: RestQuery, dbActor: ActorRef)(implicit ec: ExecutionContext) = {
    val qType = query match {
      case _: NodeQuery   => "node"
      case _: EdgeQuery   => "edge"
      case _: StringQuery => "generic"
    }
    println(s"Got $qType query: ${query.query}")
    val futureResponse = (dbActor ? query).mapTo[Try[String]].map { s =>
//      println("returning...")
      val toReturn = s match {
        case Success(json) => json
        case Failure(e) =>
          println(e.getMessage)
          "\"" + e.getMessage + "\""
      }
      HttpEntity(ContentTypes.`application/json`, toReturn)
    }
    complete(futureResponse)
  }

  implicit val mapMarshaller: ToEntityMarshaller[Map[String, Any]] = Marshaller.opaque { map =>
    HttpEntity(ContentType(MediaTypes.`application/json`), map.toString)
  }


  def mainRoute(dbActor: ActorRef, anomalyActor: ActorRef, statusActor: ActorRef)(implicit ec: ExecutionContext) =
    get {
      pathPrefix("ranked") {
        path(RemainingPath) { count =>
          val limit = Try(count.toString.toInt).getOrElse(Int.MaxValue)
          complete(
            (anomalyActor ? GetRankedAnomalies(limit))
              .mapTo[List[(UUID, Map[String, (Double, Set[UUID])])]]
              .map(convertToJsonTheOtherHardWay)
          )
        }
      } ~
      pathPrefix("status") {
        complete(
          "todo"
//          Map(
//            "nodes" -> statusList.map(s => UINode(s.from.toString, s.from.path.elements.last, s.measurements.mapValues(_.toString).toList.map(t => s"${t._1}: ${t._2}").mkString("<br />"))).map(ApiJsonProtocol.c.write),
//            "edges" -> statusList.flatMap(s => s.subscribers.map(x => UIEdge(s.from.toString, x.toString, ""))).map(ApiJsonProtocol.d.write)
//          )
        )
      } ~
      pathPrefix("query") {
          pathPrefix("nodes") {
            path(RemainingPath) { queryString =>
              completedQuery(NodeQuery(queryString.toString), dbActor)
            }
          } ~
          pathPrefix("edges") {
            path(RemainingPath) { queryString =>
              completedQuery(EdgeQuery(queryString.toString), dbActor)
            }
          } ~
          pathPrefix("generic") {
            path(RemainingPath) { queryString =>
              completedQuery(StringQuery(queryString.toString), dbActor)
            }
          }
      } ~
      serveStaticFilesRoute
    } ~
    post {
      pathPrefix("query") {
        path("anomalyScores") {
          formField('uuids) { commaSeparatedUuidString =>
            val uuids = commaSeparatedUuidString.split(",").map(s => UUID.fromString(s.trim))
            val mapF = (anomalyActor ? QueryAnomalies(uuids)).mapTo[Map[UUID, Map[String, (Double, Set[UUID])]]]
            val f = mapF.map(anoms => convertToJsonTheHardWay(anoms) )
            complete(f.map(s => HttpEntity(ContentTypes.`application/json`, s)))
          }
        } ~
        path("nodes") {
          formField('query) { queryString =>
            completedQuery(NodeQuery(queryString), dbActor)
          }
        } ~
        path("edges") {
          formField('query) { queryString =>
            completedQuery(EdgeQuery(queryString), dbActor)
          } ~
          formField('nodes) { nodeListString =>
            println(s"getting edges for nodes: $nodeListString")
            val idList = nodeListString.split(",").map(_.toInt)
            val futureResponse = (dbActor ? EdgesForNodes(idList)).mapTo[Try[String]].map { s =>
//                println("returning...")
              HttpEntity(ContentTypes.`application/json`, s.get)
            }
            complete(futureResponse)
          }
        } ~
        path("generic") {
          formField('query) { queryString =>
            completedQuery(StringQuery(queryString), dbActor)
          }
        }
      }
    }


  def convertToJsonTheHardWay(anomalyMap: Map[UUID, Map[String, (Double, Set[UUID])]]): String =    // Bad programmer! You know better than this. You should be ashamed of yourself.
    anomalyMap.mapValues(inner => inner.mapValues(t => s"""{"score":${t._1},"subgraph":${t._2.toList.map(u => s""""$u"""").mkString("[",",","]")}}""").map(t => s""""${t._1}":${t._2}""").mkString("{",",","}")  ).map(t => s""""${t._1}":${t._2}""").mkString("{",",","}")

  def convertToJsonTheOtherHardWay(anomalyList: List[(UUID, Map[String, (Double, Set[UUID])])]): String = {   // Bad programmer! You know better than this. You should be ashamed of yourself.
    anomalyList.map(anom => s"""{"${anom._1}":${anom._2.map(x => s""""${x._1}":{"score":${x._2._1},"subgraph":${x._2._2.map(u => s""""$u"""").mkString("[",",","]")}}""").mkString("{",",","}")}}""").mkString("[",",","]")
  }

}
