package com.galois.adapt.feature

import com.galois.adapt._
import com.galois.adapt.cdm13._

import java.util.UUID

import scala.collection._

import akka.actor._

/*
 * Feature extractor that gets network information on a per process basis
 *
 * filter:        Subject with subjectType = Process that connects to at least one NetFlow
 * features:      number of SEND + SENDMSG + WRITE events,
 *                average size of SEND or SENDMSG or WRITE events,
 *                number of READ + RECVMSG + RECV events,
 *                average size of RECV, RECVMSG, or READ events (all per unique IP address) 
 * 
 * Thus, every subject has  4 * (# of unique IP addresses in the trace)  features
 */
class NetflowFeature(root: ActorRef) extends SubscriptionActor[CDM13,Map[Subject,Seq[Double]]] {
  val subscriptions: immutable.Set[Subscription[CDM13]] = immutable.Set(Subscription(
    target = root,
    pack = {
      case s @ Subject(u, SUBJECT_PROCESS, _, _, _, _, _, _, _, _, _, _, _)  => Some(s)
      case e @ Event(u, EVENT_SENDTO, _, _, _, _, _, _, _, _, _, _) => Some(e)
      case e @ Event(u, EVENT_SENDMSG, _, _, _, _, _, _, _, _, _, _) => Some(e)
      case e @ Event(u, EVENT_WRITE, _, _, _, _, _, _, _, _, _, _) => Some(e)
      case e @ Event(u, EVENT_READ, _, _, _, _, _, _, _, _, _, _) => Some(e)
      case e @ Event(u, EVENT_RECVMSG, _, _, _, _, _, _, _, _, _, _) => Some(e)
      case e @ Event(u, EVENT_RECVFROM, _, _, _, _, _, _, _, _, _, _) => Some(e)
      case n @ NetFlowObject(_, _, _, _, _, _, _) => Some(n) 
      case s @ SimpleEdge(f, t, EDGE_EVENT_ISGENERATEDBY_SUBJECT, _, _) => Some(s)
      case s @ SimpleEdge(f, t, EDGE_EVENT_AFFECTS_NETFLOW, _, _) => Some(s)
      case EpochMarker => Some(EpochMarker)
      case _ => None
    }
  ))

  initialize()

  private val netflows = mutable.Map.empty[UUID,(String,String)]   // UUIDs of NetFlowObject to their src/dst IP
  
  private val sendto = mutable.Map.empty[UUID,Option[Long]]        // Event UUID -> size
  private val sendmsg = mutable.Map.empty[UUID,Option[Long]]       // Event UUID -> size
  private val write = mutable.Map.empty[UUID,Option[Long]]         // Event UUID -> size
  
  private val reads = mutable.Map.empty[UUID,Option[Long]]         // Event UUID -> size
  private val readmsg = mutable.Map.empty[UUID,Option[Long]]       // Event UUID -> size
  private val recv = mutable.Map.empty[UUID,Option[Long]]          // Event UUID -> size

  private val processes = mutable.Map.empty[UUID, Subject]         // Subject UUID -> Subject
  
  private val event2process = mutable.Map.empty[UUID,UUID]         // Relevant edges
  private val event2netflow = mutable.Map.empty[UUID,UUID]         // Relevant edges

  override def process(c: CDM13) = c match {
    case s @ Subject(u, SUBJECT_PROCESS, _, _, _, _, _, _, _, _, _, _, _)  => processes += (u -> s)
    case e @ Event(u, EVENT_SENDTO, _, _, _, _, _, _, _, s, _, _) => sendto += (u -> s)
    case e @ Event(u, EVENT_SENDMSG, _, _, _, _, _, _, _, s, _, _) => sendmsg += (u -> s)
    case e @ Event(u, EVENT_WRITE, _, _, _, _, _, _, _, s, _, _) => write += (u -> s)
    case e @ Event(u, EVENT_READ, _, _, _, _, _, _, _, s, _, _) => reads += (u -> s)
    case e @ Event(u, EVENT_RECVMSG, _, _, _, _, _, _, _, s, _, _) => readmsg += (u -> s)
    case e @ Event(u, EVENT_RECVFROM, _, _, _, _, _, _, _, s, _, _) => recv += (u -> s)
    case n @ NetFlowObject(u, _, src, _, dst, _, _) => netflows += (u -> { (src,dst) } )
    case s @ SimpleEdge(f, t, EDGE_EVENT_ISGENERATEDBY_SUBJECT, _, _) => event2process += (f -> t)
    case s @ SimpleEdge(f, t, EDGE_EVENT_AFFECTS_NETFLOW, _, _) => event2netflow += (f -> t)
    case EpochMarker =>
     
      /* The values in the map are:
       *   #sendto/sendmsg/write,
       *   #sendto/sendmsg/write with a size,
       *   sum of sizes of #sendto/sendmsg/write with a size
       *   #reads/readmsg/recv,
       *   #reads/readmsg/recv with a size,
       *   sum of sizes of #reads/readmsg/recv with a size
       */
      val counts = mutable.Map.empty[(Subject,String),(Int,Int,Long,Int,Int,Long)]

      // Tally up the counts of different types of network activity per Subject
      for ((event,subj) <- event2process
           if processes isDefinedAt subj
           if event2netflow isDefinedAt event
           if netflows isDefinedAt event2netflow(event)) {
        val subject: Subject = processes(subj)
        val srcDst: (String,String) = netflows(event2netflow(event))

        if ((sendto isDefinedAt event) || (sendmsg isDefinedAt event) || (write isDefinedAt event)) {
          val ip: String = srcDst._2
          val size: Option[Long] = sendto.getOrElse(event,sendmsg.getOrElse(event,write(event)))
          val (n,n1,sn,m,m1,sm): (Int,Int,Long,Int,Int,Long) = counts.getOrElse((subject,ip), (0,0,0,0,0,0))

          val (delta_n1, delta_sn): (Int,Long) = size match {
            case None => (0,0)
            case Some(s) => (1,s)
          } 

          counts((subject,ip)) = (n+1,n1+delta_n1,sn+delta_sn,m,m1,sm)
        } else if ((reads isDefinedAt event) || (readmsg isDefinedAt event) || (recv isDefinedAt event)) {
          val ip: String = srcDst._1
          val size: Option[Long] = reads.getOrElse(event,readmsg.getOrElse(event,recv(event)))
          val (n,n1,sn,m,m1,sm): (Int,Int,Long,Int,Int,Long) = counts.getOrElse((subject,ip), (0,0,0,0,0,0))

          val (delta_m1, delta_sm): (Int,Long) = size match {
            case None => (0,0)
            case Some(s) => (1,s)
          } 

          counts((subject,ip)) = (n,n1,sn,m+1,m1+delta_m1,sm+delta_sm)
        }
      }

      // Broadcast the subjects, after calculating the average message size
      val countsImmutable: immutable.Map[(Subject,String),(Int,Int,Long,Int,Int,Long)] = counts.toMap
      val keySet = countsImmutable.keys
      val subjects: List[Subject] = keySet.map(_._1).toList
      val ips: List[String] = keySet.map(_._2).toList

      val toBroadCast = for (subj <- subjects) yield {
        val values = ips.flatMap { ip =>
          val (n,n1,sn,m,m1,sm): (Int,Int,Long,Int,Int,Long) = counts.getOrElse((subj,ip), (0,0,0,0,0,0))
          Seq(n, if (n1 == 0) 0.0 else { n1.toDouble / sn.toDouble },
              m, if (m1 == 0) 0.0 else { m1.toDouble / sm.toDouble })
        }
        subj -> values.toSeq
      }

      broadCast(toBroadCast.toMap)

      // Clear the stored state
      netflows.clear()
      sendto.clear()
      sendmsg.clear()
      write.clear()
      reads.clear()
      readmsg.clear()
      recv.clear()
      processes.clear()
      event2process.clear()
      event2netflow.clear()

      println("EpochMarker: NetflowFeature")
    }
}

object NetflowFeature {
  def props(root: ActorRef): Props = Props(new NetflowFeature(root))
}

