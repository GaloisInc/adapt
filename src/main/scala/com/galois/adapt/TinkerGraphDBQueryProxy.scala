package com.galois.adapt

import java.io.ByteArrayOutputStream

import akka.actor._
import com.galois.adapt.cdm17._
import org.apache.tinkerpop.gremlin.structure.io.IoCore
import org.apache.tinkerpop.gremlin.structure.{Edge, Vertex}
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import java.nio.file.{Files, Paths}
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}
import collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class TinkerGraphDBQueryProxy extends DBQueryProxyActor {

  // In memory DB
  val graph: TinkerGraph = {
    val g = TinkerGraph.open()

    g.createIndex("uuid", classOf[Vertex])
    g.createIndex("pid", classOf[Vertex])

    // Optionally load in existing data:
//    localStorage.foreach( path =>
//      if (Files.exists(Paths.get(path))) g.io(IoCore.graphson()).readGraph(path)
//    )

    g
  }

  // TODO: TinkerGraph doesn't update the starting IDs when reading data in from a file.
  // Will result in: IllegalArgumentException: Vertex with id already exists: 0
  //  ... on the THIRD run. (after it _writes_ a file containing two of the same index ids)


  // The second map represents:
  //   (UUID that was destination of edge not in `nodeIds` at the time) -> (source Vertex, edge label)
  var nodeIds = collection.mutable.Map.empty[UUID, Vertex]
  var missingToUuid = collection.mutable.Map.empty[UUID, List[(Vertex,CDM17.EdgeTypes.EdgeTypes)]]

  def findNode(key: String, value: Any): Option[Vertex] = {
    if (key == "uuid" && value.isInstanceOf[UUID]) nodeIds.get(value.asInstanceOf[UUID])
    else graph.traversal().V().has(key,value).toList.asScala.headOption
  }

  // TinkerGraph doesn't need transactions
  override def FutureTx[T](body: => T)(implicit ec: ExecutionContext) = Future { body }


  override def DBNodeableTx(cdms: Seq[DBNodeable[_]]): Try[Unit] = ???

  override def AdmTx(adms: Seq[Either[adm.EdgeAdm2Adm, adm.ADM]]): Try[Unit] = {

    val skipEdgesToThisUuid = new UUID(0L, 0L) //.fromString("00000000-0000-0000-0000-000000000000")

    val admToNodeResults: Seq[Try[Unit]] = adms map {
      case Left(edge) => Try {
        if (edge.tgt.uuid != skipEdgesToThisUuid) {
          val source = findNode("uuid", edge.src.uuid)
            .getOrElse(throw AdmInvariantViolation(edge))

          val target = findNode("uuid", edge.tgt.uuid)
            .getOrElse(throw AdmInvariantViolation(edge))

          source.addEdge(edge.label, target)

          ()
        }
      }

      case Right(adm) => Try {
        val admTypeName = adm.getClass.getSimpleName

        val props: List[Object] = ((org.apache.tinkerpop.gremlin.structure.T.label, admTypeName) +: adm.asDBKeyValues)
          .flatMap { case (k, v) => List(k, v.asInstanceOf[AnyRef]) }

        assert(props.length % 2 == 0, s"Properties should have even size: $props")
        val newNode = graph.addVertex(props: _*)
        nodeIds += (adm.uuid.uuid -> newNode)

        ()
      }
    }

    admToNodeResults
      .collect { case f@Failure(_) => f }
      .headOption
      .getOrElse(Success(()))
  }


  override def receive: PartialFunction[Any,Unit] = super.receive orElse {
    case GiveMeTheGraph => sender() ! graph
  }

}

case object GiveMeTheGraph
case class DoneDevDB(graph: Option[TinkerGraph], incompleteEdgeCount: Map[UUID, List[(Vertex,String)]])

