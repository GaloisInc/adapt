package com.galois.adapt

import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Future
import akka.pattern._
import akka.actor._

class ClusterRepresentative() extends Actor {
  implicit val ec = context.dispatcher 

  override def receive = {
    case GetTotalIngestCount =>
      implicit val timeout = Timeout(10 seconds)
      Future
        .traverse(Application.qdbActorsPerHost.values.flatten) { qdbactor: ActorRef =>
          (qdbactor ? GetIngestCount).mapTo[ReplyIngestCount]
        }
        .map(_.foldLeft(ReplyIngestCount.empty)(_ + _))
        .pipeTo(sender())

    case other => println("Unexpected other message: " + other)
  }
}

case object GetTotalIngestCount // aggregates the results of `GetIngestCount` from all DB actors for this host
case object GetIngestCount      // replies with local stats
case class ReplyIngestCount(
  nodeCount: Long,
  edgeCount: Long,
  observationCount: Long
) {
  def +(other: ReplyIngestCount): ReplyIngestCount = ReplyIngestCount(
    nodeCount + other.nodeCount,
    edgeCount + other.edgeCount,
    observationCount + other.observationCount
  )
}
object ReplyIngestCount {
  val empty: ReplyIngestCount = ReplyIngestCount(0,0,0)
}
