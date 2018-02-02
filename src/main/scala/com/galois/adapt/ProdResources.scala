package com.galois.adapt

import java.io.{ByteArrayOutputStream, PrintWriter}
import java.nio.ByteBuffer
import java.util.{Properties, UUID}

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.util.Timeout
import org.mapdb.DBMaker
import akka.pattern.ask
import com.bbn.tc.schema.avro.{TheiaQuery, TheiaQueryType}
import com.typesafe.config.Config
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.collection.mutable.{MutableList, Map => MutableMap}
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success, Try}
import org.apache.tinkerpop.gremlin.structure.{Edge, Vertex}
import ApiJsonProtocol._
import spray.json._

import scala.concurrent.Future


class AnomalyManager(dbActor: ActorRef, config: Config) extends Actor with ActorLogging {
  implicit val ec = context.dispatcher
  var threshold = 0.8D
  val anomalies = MutableMap.empty[UUID, MutableMap[String, (Double, Set[UUID])]]
  var weights = MutableMap.empty[String, Double]

  val notesFilePath = config.getString("adapt.runtime.notesfile")

  var savedNotes = Try(scala.io.Source.fromFile(notesFilePath))
    .map { notesSource =>
      val notesString = try notesSource.mkString finally notesSource.close()
      notesString.parseJson.convertTo[List[SavedNotes]]
    } match {
      case Success(parsedNotes) => parsedNotes
      case Failure(e) =>
//        println("Could not load saved notes file: " + e.getMessage + " It will be created on first write.")
        List.empty[SavedNotes]
    }

  var queryQueue = List.empty[UUID]
  val freq = config.getInt("adapt.runtime.expansionqueryfreq")
  context.system.scheduler.schedule(freq seconds, freq seconds)(context.self ! MakeExpansionQueries)

  def calculateWeightedScorePerView(views: MutableMap[String, (Double, Set[UUID])]) = views.map{ case (k, v) => k -> (weights.getOrElse(k, 1D) * v._1 -> v._2)}
  def calculateSuspicionScore(views: MutableMap[String, (Double, Set[UUID])]) = calculateWeightedScorePerView(views).map(_._2._1).sum

  def makeTheiaQuery(q: MakeTheiaQuery): Future[String] = Future {
    val theia = new TheiaQuery()
    if (q.sinkId.isDefined) {
      val bb = ByteBuffer.allocate(16)
      bb.putLong(q.sinkId.get.getMostSignificantBits)
      bb.putLong(q.sinkId.get.getLeastSignificantBits)
      val targetUuid = new com.bbn.tc.schema.avro.cdm17.UUID(bb.array())
      theia.put("sinkId", targetUuid)
    }
    if (q.sourceId.isDefined) {
      val bb = ByteBuffer.allocate(16)
      bb.putLong(q.sourceId.get.getMostSignificantBits)
      bb.putLong(q.sourceId.get.getLeastSignificantBits)
      val targetUuid = new com.bbn.tc.schema.avro.cdm17.UUID(bb.array())
      theia.put("sourceId", targetUuid)
    }
    if (q.startTimestamp.isDefined) {
      theia.put("startTimestamp", q.startTimestamp.get)
    }
    if (q.endTimestamp.isDefined) {
      theia.put("endTimestamp", q.endTimestamp.get)
    }
    val bb2 = ByteBuffer.allocate(16)
    bb2.putLong(q.queryId.getMostSignificantBits)
    bb2.putLong(q.queryId.getLeastSignificantBits)
    val queryID = new com.bbn.tc.schema.avro.cdm17.UUID(bb2.array())
    theia.put("queryId", queryID)
    theia.put("type", q.`type`)

    val baos = new ByteArrayOutputStream
    val writer = new SpecificDatumWriter(classOf[com.bbn.tc.schema.avro.cdm17.TheiaQuery])
    val encoder = EncoderFactory.get.binaryEncoder(baos, null)
    writer.write(theia, encoder)
    encoder.flush()
    val elem = baos.toByteArray

    val scenario = config.getString("adapt.env.scenario")
    val topic: String = s"ta1-theia-$scenario-q"
    val brokers: String = config.getString("akka.kafka.producer.kafka-clients.bootstrap.servers")
    val props = new Properties()
    props.put("bootstrap.servers", brokers)
    props.put("client.id", "AdaptToTheiaRequestProducer")
    props.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")

    println(s"Making Theia query: $q")

    val data = new ProducerRecord[Array[Byte], Array[Byte]](topic, elem)
    val producer = new KafkaProducer[Array[Byte], Array[Byte]](props)
    val resultF = producer.send(data)
    producer.close()
    resultF.get(10L, java.util.concurrent.TimeUnit.SECONDS).toString
  }


  def receive = {
    case view: ViewScore =>
      val existing = anomalies.getOrElse(view.keyNode, MutableMap.empty[String, (Double, Set[UUID])])

      if (view.suspicionScore < threshold) existing -= view.viewName
      else existing += (view.viewName -> (view.suspicionScore, view.subgraph))

      if (existing.isEmpty) {
        anomalies -= view.keyNode
        if (queryQueue.contains(view.keyNode)) queryQueue = queryQueue.filterNot(_ == view.keyNode)
      } else {
        anomalies += (view.keyNode -> existing)
        if ( ! queryQueue.contains(view.keyNode)) queryQueue = view.keyNode :: queryQueue
      }

    case MakeExpansionQueries =>
//          println("Q: " + queryQueue.size)
        queryQueue.lastOption.foreach { startUuid =>

          // remove this now so if there is some data problem keeping this from ever returning, we can guarantee some forward progress
          queryQueue = queryQueue.take(queryQueue.length - 1)
          // TODO: this will mean some potentially successful second tries never get tried.

          implicit val timeout = Timeout(10 seconds)
          val provQueryString = s"g.V().has('uuid',$startUuid).as('tracedObject').union(_.in('flowObject').as('ptn').union(_.out('subject'),_).select('ptn').union(_.in('subject').has('eventType','EVENT_EXECUTE').out('predicateObject'),_.in('subject').has('eventType','EVENT_MMAP').out('predicateObject'),_).select('ptn').emit().repeat(_.out('prevTagId','tagId','subject','flowObject')).dedup().union(_,_.hasLabel('Subject').out('localPrincipal'),_.hasLabel('FileObject').out('localPrincipal'),_.hasLabel('Subject').emit().repeat(_.as('foo').out('parentSubject').where(neq('foo')))).dedup(),_,_.in('predicateObject').has('eventType').out('parameterTagId').out('flowObject'),_.in('predicateObject2').has('eventType').out('parameterTagId').out('flowObject')).path().unrollPath().dedup().where(neq('tracedObject'))"
          val progQueryString = s"g.V().has('uuid',$startUuid).as('tracedObject').in('flowObject').as('ptn').out('subject').as('causal_subject').select('ptn').emit().repeat(_.in('prevTagId','tagId').out('subject','flowObject')).path().unrollPath().dedup().where(neq('tracedObject'))"
          val provResultF = (dbActor ? NodeQuery(provQueryString, shouldReturnJson = false)).mapTo[Future[Try[Stream[Vertex]]]].flatMap(identity)
          provResultF.flatMap { v =>
            context.self ! ExpansionQueryResults(Provenance, startUuid, v.get.toList) // throws!!
            val progResultF = (dbActor ? NodeQuery(progQueryString, shouldReturnJson = false)).mapTo[Future[Try[Stream[Vertex]]]].flatMap(identity)
            progResultF.map { g =>
              context.self ! ExpansionQueryResults(Progenance, startUuid, g.get.toList) // throws!!
              context.self ! MakeExpansionQueries
            }
          }.recover { case e: Throwable => println(s"Expansion query failed for $startUuid with message ${e.getMessage}") }
        }


    case ExpansionQueryResults(Provenance, uuid, vertices) => Try {
//      println(s"Found ${vertices.length} provenance results")
      val newViews = List(
        "Has Network In Provenance" -> (if (vertices.exists(_.label() == "NetFlowObject")) 1D else 0D),
        "May Be Confidential (SrcSink)" -> (if (vertices.exists(_.label() == "SrcSinkObject")) 1D else 0D),
        "Max Provenance Suspicion Score" -> vertices.map(_.value[UUID]("uuid")).map( u =>
          anomalies.get(u).map(calculateSuspicionScore).getOrElse(0D)
        ).:+(0D).max
      )
      newViews.foreach { v =>
        val existing = anomalies.getOrElse(uuid, MutableMap.empty[String, (Double, Set[UUID])])

        if (v._2 < threshold) existing -= v._1
        else existing += (v._1 -> (v._2, vertices.map(_.value[UUID]("uuid")).toSet))

        if (existing.isEmpty) anomalies -= uuid
        else anomalies += (uuid -> existing)
      }
    }.recover{ case e: Throwable => e.printStackTrace()}

    case ExpansionQueryResults(Progenance, uuid, vertices) => Try {
//      println(s"Found ${vertices.length} proGenance results")
      val newViews = List(
        "Has Network In Progenance" -> (if (vertices.exists(_.label() == "NetFlowObject")) 1D else 0D)
      )
      newViews.foreach { v =>
        val existing = anomalies.getOrElse(uuid, MutableMap.empty[String, (Double, Set[UUID])])

        if (v._2 < threshold) existing -= v._1
        else existing += (v._1 -> (v._2, vertices.map(_.value[UUID]("uuid")).toSet))

        if (existing.isEmpty) anomalies -= uuid
        else anomalies += (uuid -> existing)
      }
    }.recover{ case e: Throwable => e.printStackTrace()}

    case q: MakeTheiaQuery =>
      val result = makeTheiaQuery(q)
      result.map(r => println(s"Theia query: $q returned result: $r"))
        .recover { case e: Throwable => println(s"Theia query failed with: ${e.getMessage}") }
      sender() ! result

    case QueryAnomalies(uuids) =>
      sender() ! anomalies.filter(t => uuids.contains(t._1)).mapValues(_.toMap).toMap  //anomalies.getOrElse(uuid, MutableMap.empty[String, (Double, Set[UUID])])

    case GetRankedAnomalies(topK) =>
      val weightedScores = anomalies.toList.map(a => a._1 -> calculateWeightedScorePerView(a._2))// a._2.map{ case (k, v) => k -> (weights.getOrElse(k, 1D) * v._1 -> v._2)})
      // Add subgraph scores:
      val weightedScoresLookupMap = weightedScores.map(t => t._1 -> t._2.toMap).toMap  // Make a stable, immutable copy.
      weightedScores.foreach { case (u, scored) =>
        val subgraphUuids = scored.values.flatMap(_._2).toSet.filterNot(_ == u)
        val subgraphScore = subgraphUuids.toList.map(u => weightedScoresLookupMap.get(u).map(nodeDetails => nodeDetails.values.toList.map(_._1).sum).getOrElse(0D)).sum
        scored += ("Subgraph" -> (weights.getOrElse("Subgraph", 1D) * subgraphScore, subgraphUuids))
      }
      val response = weightedScores.sortBy(ws => calculateSuspicionScore(ws._2))(Ordering.by[Double,Double](identity).reverse)
        .take(topK)
        .filter(ws => calculateSuspicionScore(ws._2) >= threshold)
        .map(t => t._1 -> t._2.toMap)
      sender() ! response

    case SetThreshold(limit) => threshold = limit

    case GetThreshold => sender() ! threshold

    case SetWeight(key, weight) => weights(key) = weight

    case GetWeights =>
      sender() ! anomalies.values.map { views =>
//        weights.toMap ++  // uncomment to show only the weights for actual anomaly scores
        val weightKeys = views.keys.toList.++(List("Subgraph"))
        weightKeys.map(k =>
          k -> weights.getOrElse(k, 1D)
        ).toMap
      }.fold(Map.empty[String,Double])((a,b) => a ++ b)

    case msg @ SavedNotes(keyUuid, rating, notes, subgraph) =>
      savedNotes = savedNotes :+ msg
      sender() ! Try {
        new PrintWriter(notesFilePath) {
          write(savedNotes.toJson.toString)
          close()
        }
      }.map(_ => ())

    case GetNotes(uuids) =>
      val revOrderNotes = savedNotes.reverse
      val toSend =
        if (uuids.isEmpty) revOrderNotes
        else uuids.flatMap(u => revOrderNotes.find(_.keyUuid == u))
      sender() ! toSend
  }
}

case class SetThreshold(threshold: Double)
case class SetWeight(key: String, weight: Double)
case object GetThreshold
case object GetWeights
case class QueryAnomalies(uuids: Seq[UUID])
case class GetRankedAnomalies(topK: Int = Int.MaxValue)
case class SavedNotes(keyUuid: UUID, rating: Int, notes: String, subgraph: Set[UUID]) {
  def toJsonString: String = s"""{"keyUuid": "$keyUuid", "rating": $rating, "notes":"$notes", "subgraph":${subgraph.map(u => s""""$u"""").mkString("[",",","]")}, "ratingTimeMillis": ${System.currentTimeMillis()} }"""
}
case class GetNotes(uuid: Seq[UUID])

case object MakeExpansionQueries
case class ExpansionQueryResults(expType: ExpansionQuery, uuid: UUID, resultVertices: List[Vertex])
sealed trait ExpansionQuery
case object Provenance extends ExpansionQuery
case object Progenance extends ExpansionQuery

case class MakeTheiaQuery(
  `type`: TheiaQueryType,
  sourceId: Option[UUID] = None,
  sinkId: Option[UUID] = None,
  startTimestamp: Option[Long] = None,
  endTimestamp: Option[Long] = None,
  queryId: UUID = UUID.randomUUID()
)
