package com.galois

import java.util.UUID
import akka.util.Timeout
import scala.concurrent.{ExecutionContext, Future}


package object adapt {

  trait CdmVersion
  type CurrentCdm = cdm20.CDM20

  /// Simple variant of [InstrumentationSource]
  sealed trait DataProvider
  case object Clearscope extends DataProvider
  case object Trace extends DataProvider
  case object Cadets extends DataProvider
  case object Theia extends DataProvider
  case object Faros extends DataProvider
  case object FiveDirections extends DataProvider
  case object Marple extends DataProvider

  object DataProvider {
    def fromInstrumentationSource(src: cdm20.InstrumentationSource): DataProvider = src.toString.split('_').last match {
      case "CLEARSCOPE" => Clearscope
      case "TRACE" => Trace
      case "CADETS" => Cadets
      case "THEIA" => Theia
      case "FAROS" => Faros
      case "FIVEDIRECTIONS" => FiveDirections
      case "MARPLE" => Marple
    }

    def isWindows: DataProvider => Boolean = {
      case Clearscope => false
      case Trace => false
      case Cadets => false
      case Theia => false
      case Faros => true
      case FiveDirections => true
      case Marple => true
    }
  }

  // Anything that can be converted into properties on a node
  trait DBWritable {
    // Returns an (even-length) list alternating between the string label of the property and its
    // value.
    def asDBKeyValues: List[(String, Any)]
  }

  // Anything that corresponds to a node in the graph
  trait DBNodeable[+EdgeType] extends DBWritable {
    // All nodes in the graph have a UUID, even if our current DB (Titan) doesn't support using that
    // as the internal ID.
    def getUuid: UUID

    // Find the host ID
    def getHostId: Option[UUID] = None

    // Outgoing edges coming off the node. The key is the label on the edge, the UUID the node
    // (which should be 'DBNodeable' too) the edge goes to.
    def asDBEdges: List[(EdgeType, UUID)]

    // Some CDM statements translate to more than one node. We put extra nodes into 'supportNodes'
    def supportNodes: List[(UUID, List[Any], List[(EdgeType, UUID)])] = List()
  }


  implicit class StringSetMethods(val set: Set[String]) extends AnyVal {
    def suffixFold: Set[String] = set.foldLeft(Set.empty[String])((acc, p) => if ((acc ++ set.-(p)).exists(s => s.endsWith(p))) acc else acc + p)
  }

  def time[A](name: String)(f: => A) = {
    val t0 = System.nanoTime
    val ans = f
    printf(s"$name elapsed: %.6f sec\n",(System.nanoTime-t0)*1e-9)
    ans
  }

  import com.rrwright.quine.runtime.FutureRecoverWith
  def retryOnFailure[T](maxRetries: Int)(action: => Future[T], originalMax: Int = maxRetries)(implicit ec: ExecutionContext): Future[T] =
    if (maxRetries > 0) {
//      val nextTimeout = if (maxRetries < 5) timeout.duration + (timeout.duration / 2) else timeout.duration
      action.recoverWith{ case e => retryOnFailure(maxRetries - 1)(action, originalMax) }
    } else action.recoveryMessage("retryOnFailure failed after {} attempts.", originalMax)
}
