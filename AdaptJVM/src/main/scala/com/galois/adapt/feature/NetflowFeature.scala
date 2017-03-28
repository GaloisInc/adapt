package com.galois.adapt.feature

import com.galois.adapt._
import com.galois.adapt.cdm16._

import java.util.UUID

import scala.collection.mutable.{Set => MutableSet, Map => MutableMap}

import akka.actor._


// TODO: CDM15

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
class NetflowFeature(val registry: ActorRef, root: ActorRef)
  extends Actor with ActorLogging with ServiceClient with SubscriptionActor[Map[Subject,Seq[Double]]] with ReportsStatus {
  
  val subscriptions: Set[Subscription] = Set(Subscription(
    target = root,
    interested = {
      /*
      case Subject(_, SUBJECT_PROCESS, _, _, _, _, _, _, _, _, _, _, _)  => true
      case Event(_, EVENT_SENDTO, _, _, _, _, _, _, _, _, _, _) => true
      case Event(_, EVENT_SENDMSG, _, _, _, _, _, _, _, _, _, _) => true
      case Event(_, EVENT_WRITE, _, _, _, _, _, _, _, _, _, _) => true
      case Event(_, EVENT_READ, _, _, _, _, _, _, _, _, _, _) => true
      case Event(_, EVENT_RECVMSG, _, _, _, _, _, _, _, _, _, _) => true
      case Event(_, EVENT_RECVFROM, _, _, _, _, _, _, _, _, _, _) => true
      case NetFlowObject(_, _, _, _, _, _, _) => true
      case SimpleEdge(_, _, EDGE_EVENT_ISGENERATEDBY_SUBJECT, _, _) => true
      case SimpleEdge(_, _, EDGE_EVENT_AFFECTS_NETFLOW, _, _) => true
      case EpochMarker => true
      */
      case _ => false
    }
  ))

  val dependencies = List.empty
  def beginService() = initialize()
  def endService() = ()

  def statusReport = Map(
    "netflows_size" -> netflows.size,
    "sendto_size" -> sendto.size,
    "sendmsg_size" -> sendmsg.size,
    "write_size" -> write.size,
    "reads_size" -> reads.size,
    "readmsg_size" -> readmsg.size,
    "recv_size" -> recv.size,
    "processes_size" -> processes.size,
    "event2process_size" -> event2process.size,
    "event2netflow_size" -> event2netflow.size
  )


  private val netflows = MutableMap.empty[UUID,(String,String)]   // UUIDs of NetFlowObject to their src/dst IP
  
  private val sendto = MutableMap.empty[UUID,Option[Long]]        // Event UUID -> size
  private val sendmsg = MutableMap.empty[UUID,Option[Long]]       // Event UUID -> size
  private val write = MutableMap.empty[UUID,Option[Long]]         // Event UUID -> size
  
  private val reads = MutableMap.empty[UUID,Option[Long]]         // Event UUID -> size
  private val readmsg = MutableMap.empty[UUID,Option[Long]]       // Event UUID -> size
  private val recv = MutableMap.empty[UUID,Option[Long]]          // Event UUID -> size

  private val processes = MutableMap.empty[UUID, Subject]         // Subject UUID -> Subject
  
  private val event2process = MutableMap.empty[UUID,UUID]         // Relevant edges
  private val event2netflow = MutableMap.empty[UUID,UUID]         // Relevant edges

  override def process = {
    /*
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
    */
    case EpochMarker =>
     
      /* The values in the map are:
       *   #sendto/sendmsg/write,
       *   #sendto/sendmsg/write with a size,
       *   sum of sizes of #sendto/sendmsg/write with a size
       *   #reads/readmsg/recv,
       *   #reads/readmsg/recv with a size,
       *   sum of sizes of #reads/readmsg/recv with a size
       */
      val counts = MutableMap.empty[(Subject,String),(Int,Int,Long,Int,Int,Long)]

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
      val countsImmutable: Map[(Subject,String),(Int,Int,Long,Int,Int,Long)] = counts.toMap
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
  def props(registry: ActorRef, root: ActorRef): Props = Props(new NetflowFeature(registry, root))
}

