package com.galois.adapt

import akka.util.Timeout
import com.galois.adapt.AdaptConfig.HostName
import com.galois.adapt.NoveltyDetection._
import com.galois.adapt.PpmSummarizer.AbstractionOne
import com.galois.adapt.cdm20._
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Try
import com.rrwright.quine.runtime.FutureRecoverWith
import com.rrwright.quine.language.BoopickleScheme._
import com.rrwright.quine.language._
import Application.{graph, ppmManagers}


case object StandingFetches extends LazyLogging {
  implicit val timeout = Timeout(10.4 seconds)
  implicit val ec: ExecutionContext = graph.system.dispatchers.lookup("quine.actor.node-dispatcher")

  val readTypes = Set[EventType](EVENT_READ, EVENT_RECVFROM, EVENT_RECVMSG)
  val writeTypes = Set[EventType](EVENT_WRITE, EVENT_SENDTO, EVENT_SENDMSG, EVENT_CREATE_OBJECT, EVENT_FLOWS_TO)
  val execTypes = Set[EventType](EVENT_EXECUTE, EVENT_LOADLIBRARY, EVENT_MMAP, EVENT_STARTSERVICE)
  val deleteTypes = Set[EventType](EVENT_UNLINK, EVENT_TRUNCATE)

  def makeComplexObsEdge(subjectQid: Option[QuineId], objectQid: Option[QuineId], eventType: EventType): Future[Unit] = {
    val eventlabelOpt = eventType match {
      case e if readTypes contains e   => Some("did_read")
      case e if writeTypes contains e  => Some("did_write")
      case e if execTypes contains e   => Some("did_execute")
      case e if deleteTypes contains e => Some("did_delete")
      case _ => None
//      case EVENT_ADD_OBJECT_ATTRIBUTE, EVENT_CHANGE_PRINCIPAL, EVENT_CLONE, EVENT_FORK, EVENT_MODIFY_FILE_ATTRIBUTES, EVENT_MODIFY_PROCESS, EVENT_MPROTECT, EVENT_READ_SOCKET_PARAMS, EVENT_SERVICEINSTALL, EVENT_SHM, EVENT_SIGNAL, EVENT_UMOUNT, EVENT_UNIT, EVENT_UPDATE, EVENT_WAIT, EVENT_WRITE_SOCKET_PARAMS
//      case EVENT_ACCEPT | EVENT_CONNECT =>
//      case EVENT_RENAME =>
    }
    val opt = for {
      eLabel <- eventlabelOpt
      sub <- subjectQid
      obj <- objectQid
    } yield {
//      println(s"Wrote: $eLabel from: ${graph.idProvider.customIdFromQid(sub)}, to: ${graph.idProvider.customIdFromQid(obj)}")
      graph.dumbOps.addEdge(sub, obj, eLabel).recoveryMessage("Failed to create edge: {} from: {} to: {}", eLabel, graph.idProvider.customIdFromQid(sub), graph.idProvider.customIdFromQid(obj))
    }
    opt.getOrElse(Future.successful(()))
  }
>
  val objectWriterBranch = branchOf[ObjectWriter]()
  val objectWriterQueryable = implicitly[Queryable[ObjectWriter]]

  val objectExecutorBranch = branchOf[ObjectExecutor]()
  val objectExecutorQuerable = implicitly[Queryable[ObjectExecutor]]

  val netflowReadingProcessBranch = branchOf[NetflowReadingProcess]()
  val netflowReadingProcessQueryable = implicitly[Queryable[NetflowReadingProcess]]


  def onESOFileMatch(l: List[ESOFileInstance]): Unit = l.foreach{ eso =>
    ppmManagers.get(eso.hostName).fold(logger.error(s"No PPM Actor with hostname: ${eso.hostName}"))(_.esoFileInstance(eso))
    makeComplexObsEdge(eso.subject.qid, eso.predicateObject.qid, eso.eventType)

    // CommunicationPathThroughObject:  (write, then read)
    if ((readTypes contains eso.eventType) && eso.predicateObject.qid.isDefined) {  // Test if this ESO matches the second half of the pattern
      // Get any existing patterns with ObjectWriter which form the first half of the SEOES pattern (viz. SEO).
      val objectQid = eso.predicateObject.qid.get
      val objectCustomId = graph.idProvider.customIdFromQid(objectQid)
      val objWritersFut = graph.getNodeComponents(objectQid, objectWriterBranch, None).map { ncList =>
        ncList.flatMap { nc => objectWriterQueryable.fromNodeComponents(nc)
          .collect{ case ow if ow.did_write.target.qid != eso.subject.qid => // Ignore matches when the subject is the same on each side.

            // Combine the found SEO components with the current standingFetch'd matches ESO to form the results into the desired SEOES shape:
            val s1 = PpmSubject(ow.did_write.target.cid, ow.did_write.target.subjectTypes, graph.idProvider.customIdFromQid(ow.did_write.target.qid.get).get)
            val pn1 = ow.did_write.target.path
            val o = PpmFileObject(eso.predicateObject.fileObjectType, objectCustomId.get)
            val pno = eso.predicateObject.path
            val e = PpmEvent(eso.eventType, eso.earliestTimestampNanos, eso.latestTimestampNanos, graph.idProvider.customIdFromQid(eso.qid.get).get)
            val s2 = PpmSubject(eso.subject.cid, eso.subject.subjectTypes, graph.idProvider.customIdFromQid(eso.subject.qid.get).get)
            val pn2 = eso.subject.path
            val seoesInstance = SEOESInstance((s1, Some(pn1)), "did_write", ESOInstance(e, (s2, Some(pn2)), (o, Some(pno))))
//            println(s"CommunicationPathThroughObject: $seoesInstance")
            ppmManagers.get(eso.hostName).fold(logger.error(s"No PPM Actor with hostname: ${eso.hostName}"))(_.seoesInstance(seoesInstance))
          }
        }
      }.recoveryMessage("SEOES extraction for CommunicationPathThroughObject failed after matching: {} and querying ObjectWriter on {}", eso, objectCustomId)
//              objWritersFut.map{ oList => if (oList.nonEmpty) println(s"FOUND ${oList.size}:\n${oList.mkString("\n")}") }
    }

    // FileExecuteDelete:  (execute, then delete)
    if ((deleteTypes contains eso.eventType) && eso.predicateObject.qid.isDefined) {  // Test if this ESO matches the second half of the pattern
      val objectQid = eso.predicateObject.qid.get
      val objectCustomId = graph.idProvider.customIdFromQid(objectQid)
      val objWritersFut = graph.getNodeComponents(objectQid, objectExecutorBranch, None).map { ncList =>
        ncList.flatMap { nc =>
          objectExecutorQuerable.fromNodeComponents(nc)
            .collect { case ow => // DONT'T ignore matches when the subject is the same on each side.

              // Combine the found SEO components with the current standingFetch'd matches ESO to form the results into the desired SEOES shape:
              val s1 = PpmSubject(ow.did_execute.target.cid, ow.did_execute.target.subjectTypes, graph.idProvider.customIdFromQid(ow.did_execute.target.qid.get).get)
              val pn1 = ow.did_execute.target.path
              val o = PpmFileObject(eso.predicateObject.fileObjectType, objectCustomId.get)
              val pno = eso.predicateObject.path
              val e = PpmEvent(eso.eventType, eso.earliestTimestampNanos, eso.latestTimestampNanos, graph.idProvider.customIdFromQid(eso.qid.get).get)
              val s2 = PpmSubject(eso.subject.cid, eso.subject.subjectTypes, graph.idProvider.customIdFromQid(eso.subject.qid.get).get)
              val pn2 = eso.subject.path
              val seoesInstance = SEOESInstance((s1, Some(pn1)), "did_execute", ESOInstance(e, (s2, Some(pn2)), (o, Some(pno))))
//              println(s"FileExecuteDelete: $seoesInstance")
              ppmManagers.get(eso.hostName).fold(logger.error(s"No PPM Actor with hostname: ${eso.hostName}"))(_.seoesInstance(seoesInstance))
            }
        }
      }.recoveryMessage("SEOES extraction for FileExecuteDelete failed after matching: {} and querying ObjectWriter on {}", eso, objectCustomId)
    }


    // FilesWrittenThenExecuted:  (write, then execute)
    if ((execTypes contains eso.eventType) && eso.predicateObject.qid.isDefined) {  // Test if this ESO matches the second half of the pattern
      val objectQid = eso.predicateObject.qid.get
      val objectCustomId = graph.idProvider.customIdFromQid(objectQid)
      val objWritersFut = graph.getNodeComponents(objectQid, objectWriterBranch, None).map { ncList =>
        ncList.flatMap { nc =>
          objectWriterQueryable.fromNodeComponents(nc)
            .collect { case ow => // DON'T ignore matches when the subject is the same on each side.

              // Combine the found SEO components with the current standingFetch'd matches ESO to form the results into the desired SEOES shape:
              val s1 = PpmSubject(ow.did_write.target.cid, ow.did_write.target.subjectTypes, graph.idProvider.customIdFromQid(ow.did_write.target.qid.get).get)
              val pn1 = ow.did_write.target.path
              val o = PpmFileObject(eso.predicateObject.fileObjectType, objectCustomId.get)
              val pno = eso.predicateObject.path
              val e = PpmEvent(eso.eventType, eso.earliestTimestampNanos, eso.latestTimestampNanos, graph.idProvider.customIdFromQid(eso.qid.get).get)
              val s2 = PpmSubject(eso.subject.cid, eso.subject.subjectTypes, graph.idProvider.customIdFromQid(eso.subject.qid.get).get)
              val pn2 = eso.subject.path
              val seoesInstance = SEOESInstance((s1, Some(pn1)), "did_write", ESOInstance(e, (s2, Some(pn2)), (o, Some(pno))))
//              println(s"FilesWrittenThenExecuted: $seoesInstance")
              ppmManagers.get(eso.hostName).fold(logger.error(s"No PPM Actor with hostname: ${eso.hostName}"))(_.seoesInstance(seoesInstance))
            }
        }
      }.recoveryMessage("SEOES extraction for FilesWrittenThenExecuted failed after matching: {} and querying ObjectWriter on {}", eso, objectCustomId)
    }


    // ProcessWritesFileSoonAfterNetflowRead: (alternate) (read, then write)  Part 2
    if ((writeTypes contains eso.eventType) && eso.predicateObject.qid.isDefined) {  // Test if this ESO matches the second half of the pattern
      val subjectQid = eso.subject.qid.get
      val subjectCustomId = graph.idProvider.customIdFromQid(subjectQid)
      val objWritersFut = graph.getNodeComponents(subjectQid, netflowReadingProcessBranch, None).map { ncList =>
        ncList.foreach { nc =>
          netflowReadingProcessQueryable.fromNodeComponents(nc)
            .foreach{ nfrp =>
              val delta = eso.latestTimestampNanos - nfrp.latestNetflowRead.latestTimestampNanos
              if (delta >= 0 && delta <= 1e10.toLong) {
                val n = PpmNetFlowObject(nfrp.latestNetflowRead.remotePort, nfrp.latestNetflowRead.localPort, nfrp.latestNetflowRead.remoteAddress, nfrp.latestNetflowRead.localAddress, graph.idProvider.customIdFromQid(new QuineId(nfrp.latestNetflowRead.qid)).get)
                val e = PpmEvent(eso.eventType, eso.earliestTimestampNanos, eso.latestTimestampNanos, graph.idProvider.customIdFromQid(eso.qid.get).get)
                val s = PpmSubject(eso.subject.cid, eso.subject.subjectTypes, graph.idProvider.customIdFromQid(eso.subject.qid.get).get)
                val pnS = eso.subject.path
                val o = PpmFileObject(eso.predicateObject.fileObjectType, graph.idProvider.customIdFromQid(eso.predicateObject.qid.get).get)
                val pnO = Some(eso.predicateObject.path)
                val oeseoInstance = OESEOInstance((n, None), "did_read", ESOInstance(e, (s, Some(pnS)), (o, pnO)))
//                println(s"ProcessWritesFileSoonAfterNetflowRead: $oeseoInstance")
                ppmManagers.get(eso.hostName).fold(logger.error(s"No PPM Actor with hostname: ${eso.hostName}"))(_.oeseoInstance(oeseoInstance))
              }
            }
        }
      }
    }
  }


  def onESONetworkMatch(l: List[ESONetworkInstance]): Unit = l.foreach{ eso =>
    ppmManagers.get(eso.hostName).fold(logger.error(s"No PPM Actor with hostname: ${eso.hostName}"))(_.esoNetworkInstance(eso))
    makeComplexObsEdge(eso.subject.qid, eso.predicateObject.qid, eso.eventType)

    // CommunicationPathThroughObject:  (write, then read)
    if ((readTypes contains eso.eventType) && eso.predicateObject.qid.isDefined) {  // Test if this ESO matches the second half of the pattern
      // Get any existing patterns with ObjectWriter which form the first half of the SEOES pattern (viz. SEO).
      val objectQid = eso.predicateObject.qid.get
      val objectCustomId = graph.idProvider.customIdFromQid(objectQid)
      val objWritersFut = graph.getNodeComponents(objectQid, objectWriterBranch, None).map { ncList =>
        ncList.flatMap { nc => objectWriterQueryable.fromNodeComponents(nc)
          .collect{ case ow if ow.did_write.target.qid != eso.subject.qid => // Ignore matches when the subject is the same on each side.

            // Combine the found SEO components with the current standingFetch'd matches ESO to form the results into the desired SEOES shape:
            val s1 = PpmSubject(ow.did_write.target.cid, ow.did_write.target.subjectTypes, graph.idProvider.customIdFromQid(ow.did_write.target.qid.get).get)
            val pn1 = ow.did_write.target.path
            val o = PpmNetFlowObject(eso.predicateObject.remotePort, eso.predicateObject.localPort, eso.predicateObject.remoteAddress, eso.predicateObject.localAddress, objectCustomId.get)
            val pno = None
            val e = PpmEvent(eso.eventType, eso.earliestTimestampNanos, eso.latestTimestampNanos, graph.idProvider.customIdFromQid(eso.qid.get).get)
            val s2 = PpmSubject(eso.subject.cid, eso.subject.subjectTypes, graph.idProvider.customIdFromQid(eso.subject.qid.get).get)
            val pn2 = eso.subject.path
            val seoesInstance = SEOESInstance((s1, Some(pn1)), "did_write", ESOInstance(e, (s2, Some(pn2)), (o, pno)))
//            println(s"CommunicationPathThroughObject: $seoesInstance")
            ppmManagers.get(eso.hostName).fold(logger.error(s"No PPM Actor with hostname: ${eso.hostName}"))(_.seoesInstance(seoesInstance))
          }
        }
      }.recoveryMessage("SEOES extraction for CommunicationPathThroughObject failed after matching: {} and querying ObjectWriter on {}", eso, objectCustomId)
    }


    // ProcessWritesFileSoonAfterNetflowRead: (alternate)  Part 1
    if (readTypes.contains(eso.eventType) && eso.subject.qid.isDefined) Try {  // Test if this ESO matches the FIRST half of the pattern
      val subjectQid = eso.subject.qid.get
      val n = eso.predicateObject
      val data = LatestNetflowRead(n.remoteAddress, n.remotePort, n.localAddress, n.localPort, eso.latestTimestampNanos, n.qid.get.array)
      graph.dumbOps.setProp(subjectQid, "latestNetflowRead", data)  // TODO: Consider: Reads from multiple netflows on the same process.
//      println(s"Set 'latestNetflowRead' on ${graph.idProvider.customIdFromQid(subjectQid).get} to: $data on process: ${graph.idProvider.customIdFromQid(subjectQid).get}")
    }
  }


  def onESOSrcSinkMatch(l: List[ESOSrcSnkInstance]): Unit = l.foreach{ eso =>
    ppmManagers.get(eso.hostName).fold(logger.error(s"No PPM Actor with hostname: ${eso.hostName}"))(_.esoSrcSinkInstance(eso))
    makeComplexObsEdge(eso.subject.qid, eso.predicateObject.qid, eso.eventType)

    // CommunicationPathThroughObject:  (write, then read)
    if ((readTypes contains eso.eventType) && eso.predicateObject.qid.isDefined) {  // Test if this ESO matches the second half of the pattern
      // Get any existing patterns with ObjectWriter which form the first half of the SEOES pattern (viz. SEO).
      val objectQid = eso.predicateObject.qid.get
      val objectCustomId = graph.idProvider.customIdFromQid(objectQid)
      val objWritersFut = graph.getNodeComponents(objectQid, objectWriterBranch, None).map { ncList =>
        ncList.flatMap { nc => objectWriterQueryable.fromNodeComponents(nc)
          .collect{ case ow if ow.did_write.target.qid != eso.subject.qid => // Ignore matches when the subject is the same on each side.

            // Combine the found SEO components with the current standingFetch'd matches ESO to form the results into the desired SEOES shape:
            val s1 = PpmSubject(ow.did_write.target.cid, ow.did_write.target.subjectTypes, graph.idProvider.customIdFromQid(ow.did_write.target.qid.get).get)
            val pn1 = ow.did_write.target.path
            val o = PpmSrcSinkObject(eso.predicateObject.srcSinkType, objectCustomId.get)
            val pno = None
            val e = PpmEvent(eso.eventType, eso.earliestTimestampNanos, eso.latestTimestampNanos, graph.idProvider.customIdFromQid(eso.qid.get).get)
            val s2 = PpmSubject(eso.subject.cid, eso.subject.subjectTypes, graph.idProvider.customIdFromQid(eso.subject.qid.get).get)
            val pn2 = eso.subject.path
            val seoesInstance = SEOESInstance((s1, Some(pn1)), "did_write", ESOInstance(e, (s2, Some(pn2)), (o, pno)))
//            println(s"CommunicationPathThroughObject: $seoesInstance")
            ppmManagers.get(eso.hostName).fold(logger.error(s"No PPM Actor with hostname: ${eso.hostName}"))(_.seoesInstance(seoesInstance))
          }
        }
      }.recoveryMessage("SEOES extraction for CommunicationPathThroughObject failed after matching: {} and querying ObjectWriter on {}", eso, objectCustomId)
    }
  }


  def onESOProcessMatch(l: List[ChildProcess]): Unit = l.foreach { pp =>
    val childParent = pp.path.path -> pp.parentSubject.path.path
    // println(s"ParentChildProcess: ${childParent._1} is the child of: ${childParent._2}")
    if (pp.parentSubject.qid.isDefined && pp.qid.isDefined) {
      val parentSubject = PpmSubject(pp.parentSubject.cid, pp.parentSubject.subjectTypes, pp.parentSubject.qid.map(q => graph.idProvider.customIdFromQid(q)).flatMap(_.toOption).get, Some(pp.parentSubject.startTimestampNanos))
      val childSubject = PpmSubject(pp.cid, pp.subjectTypes, pp.qid.map(q => graph.idProvider.customIdFromQid(q)).flatMap(_.toOption).get, Some(pp.startTimestampNanos))
      val ssInstance = SSInstance((parentSubject, Some(pp.parentSubject.path)), (childSubject, Some(pp.path)))
      val hostName = pp.hostName
      ppmManagers.get(hostName).fold(logger.error(s"No PPM Actor with hostname: {}", hostName))(_.ssInstance(ssInstance))
    }
  }







}
