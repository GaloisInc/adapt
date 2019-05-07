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
import com.rrwright.quine.language.EdgeDirections._
import Application.{graph, ppmManagers}
import com.galois.adapt.adm.{AdmNetFlowObject, AdmUUID}


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
          .collect{ case ow if ow.did_write.qid != eso.subject.qid => // Ignore matches when the subject is the same on each side.

            // Combine the found SEO components with the current standingFetch'd matches ESO to form the results into the desired SEOES shape:
            val s1 = PpmSubject(ow.did_write.cid, ow.did_write.subjectTypes, graph.idProvider.customIdFromQid(ow.did_write.qid.get).get)
            val pn1 = ow.did_write.path
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
              val s1 = PpmSubject(ow.did_execute.cid, ow.did_execute.subjectTypes, graph.idProvider.customIdFromQid(ow.did_execute.qid.get).get)
              val pn1 = ow.did_execute.path
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
              val s1 = PpmSubject(ow.did_write.cid, ow.did_write.subjectTypes, graph.idProvider.customIdFromQid(ow.did_write.qid.get).get)
              val pn1 = ow.did_write.path
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
          .collect{ case ow if ow.did_write.qid != eso.subject.qid => // Ignore matches when the subject is the same on each side.

            // Combine the found SEO components with the current standingFetch'd matches ESO to form the results into the desired SEOES shape:
            val s1 = PpmSubject(ow.did_write.cid, ow.did_write.subjectTypes, graph.idProvider.customIdFromQid(ow.did_write.qid.get).get)
            val pn1 = ow.did_write.path
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
          .collect{ case ow if ow.did_write.qid != eso.subject.qid => // Ignore matches when the subject is the same on each side.

            // Combine the found SEO components with the current standingFetch'd matches ESO to form the results into the desired SEOES shape:
            val s1 = PpmSubject(ow.did_write.cid, ow.did_write.subjectTypes, graph.idProvider.customIdFromQid(ow.did_write.qid.get).get)
            val pn1 = ow.did_write.path
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


//  def onCommunicatingNetflowsMatch(l: List[CommunicatingNetflows]): Unit = l.foreach { cn  =>
//    cn.otherNetflow.foreach { other =>
////        val matching = s"g.V([${cn.provider}_${graph.idProvider.customIdFromQid(cn.qid.get).get._1}, ${cn.otherNetflow.get.provider}_${graph.idProvider.customIdFromQid(cn.otherNetflow.get.qid.get).get._1}])"
////  //      val matching = cn.otherNetflow.map(o => s"${cn.localAddress.get}:${cn.localPort.get} --> ${o.localAddress.get}:${o.localPort.get}    ${graph.idProvider.customIdFromQid(cn.qid.get).get}  ${graph.idProvider.customIdFromQid(o.qid.get).get}")
////        println(matching)
//      val addEdgeF = for {
//        first <- cn.qid
//        other <- cn.otherNetflow
//        second <- other.qid
//      } yield {
//        graph.dumbOps.addEdge(first, second, "reciprocal_netflow", isDirected = true) //.recover{ case e => e.printStackTrace()}
//        graph.dumbOps.addEdge(second, first, "reciprocal_netflow") //.recover{ case e => e.printStackTrace()}
//      }
//    }
//  }

  import akka.pattern.ask
  def addReciprocalNetflowByQuery(id: AdmUUID): Future[Boolean] = {
    val q = s"g.V(${id.rendered}).as('start').out('localPortAdm').in('remotePortAdm').as('portother').out('localPortAdm').in('remotePortAdm').as('portend').eqToVar('start').out('localAddressAdm').in('remoteAddressAdm').as('addyother').out('localAddressAdm').in('remoteAddressAdm').as('addyend').eqToVar('start').select('portother').eqToVar('addyother').id()"
    (Application.uiDBInterface ? RawQuery(q)).mapTo[Future[Stream[AdmUUID]]].flatten.map{ idList =>
      idList.toList match {
        case exactlyOne :: Nil =>
          graph.dumbOps.addEdge(graph.idProvider.customIdToQid(id), graph.idProvider.customIdToQid(exactlyOne), "reciprocal_netflow", isDirected = true) //.recover{ case e => e.printStackTrace()}
          graph.dumbOps.addEdge(graph.idProvider.customIdToQid(exactlyOne), graph.idProvider.customIdToQid(id), "reciprocal_netflow")
          true
        case _ => false
      }
    }
  }

  def proactiveAddReciprocalEdge(anAdm: AdmNetFlowObject): Future[Unit] = {
    for {
      la <- anAdm.localAddress
      lp <- anAdm.localPort
      ra <- anAdm.remoteAddress
      rp <- anAdm.remotePort
    } yield {
      val hostList = AdaptConfig.ingestConfig.hosts.map(_.hostName)
      Future.sequence(
        hostList.map{ h =>
          val otherId = AdmUUID(DeterministicUUID(ra, rp, la, lp), h)  // Invert local and remote to define the _other_ netflow.
          val qid = Application.graph.idProvider.customIdToQid(anAdm.uuid)
          val other = Application.graph.idProvider.customIdToQid(otherId)
          val a = Application.graph.dumbOps.addEdge(qid, other, "reciprocal_netflow")
          val b = Application.graph.dumbOps.addEdge(other, qid, "reciprocal_netflow")
          a.flatMap(_ => b)
        }
      ).map(_ => () )
    }
  }.getOrElse(Future.successful( () ))


  def onProcessNetworkCommsMatch(l: List[ProcessNetworkReadFromOtherProcess]): Unit = {
    // This will always be a list of size 1, done by the caller.
    l.foreach { readerNet =>
      for {
        readerQid <- readerNet.qid
        writerQid <- readerNet.did_read.reciprocal_netflow.did_write.qid
        if readerQid != writerQid
        readerUuid <- graph.idProvider.customIdFromQid(readerQid)
        writerUuid <- graph.idProvider.customIdFromQid(writerQid)
        readerNetflowQid <- readerNet.did_read.qid
        readerNetflowUuid <- graph.idProvider.customIdFromQid(readerNetflowQid)
        writerNetflowQid <- readerNet.did_read.reciprocal_netflow.did_write.qid
        writerNetflowUuid <- graph.idProvider.customIdFromQid(writerNetflowQid)
        readerPathQid <- readerNet.path.qid
        readerPathUuid <- graph.idProvider.customIdFromQid(readerPathQid)
        writerPathQid <- readerNet.did_read.reciprocal_netflow.did_write.path.qid
        writedPathUuid <- graph.idProvider.customIdFromQid(writerPathQid)
      } {
        graph.dumbOps.addEdge(readerQid, writerQid, "did_read_over_network")

        val writerPathName = readerNet.did_read.reciprocal_netflow.did_write.path.path
        val readerPathName = readerNet.path.path


        val readingNetflow = PpmNetFlowObject(readerNet.did_read.remotePort, readerNet.did_read.localPort, readerNet.did_read.remoteAddress, readerNet.did_read.localAddress, readerNetflowUuid)

        val obs = RemoteNetworkProcessReadObservation(
          readerPathName,
          readerNet.hostName,
          writerPathName,
          readerNet.did_read.reciprocal_netflow.did_write.hostName,
          readingNetflow,
          Set(
            NamespacedUuidDetails(readerUuid, Some(readerNet.path.path), Some(readerNet.cid)),
            NamespacedUuidDetails(writerUuid, Some(readerNet.did_read.reciprocal_netflow.did_write.path.path), Some(readerNet.did_read.reciprocal_netflow.did_write.cid)),
            NamespacedUuidDetails(readerNetflowUuid),
            NamespacedUuidDetails(writerNetflowUuid),
            NamespacedUuidDetails(readerPathUuid),
            NamespacedUuidDetails(writedPathUuid)
          ),
          Set(System.nanoTime())
        )

        val query = s"g.V([${obs.relevantUuids.map(_.extendedUuid.rendered).mkString(", ")}])"
        logger.info(s"Standing match: CrossHostProcessCommunication = ${readerNet.hostName}:$readerPathName <-- ${readerNet.did_read.reciprocal_netflow.did_write.hostName}:$writerPathName Q: " + query)

        ppmManagers.get(readerNet.hostName).fold(logger.error(s"No PPM Actor with hostname: ${readerNet.hostName}")){ ppm =>
//          println(s"OBSERVATION: $obs")
          ppm.crossHostTree.observe(obs)
        }
      }
    }
  }


}

case class RemoteNetworkProcessReadObservation(
  readingProcessName: String,
  readingProcessHost: HostName,
  writingProcessName: String,
  writingProcessHost: HostName,
  readingNetflow: PpmNetFlowObject,
  relevantUuids: Set[NamespacedUuidDetails],
  timestamps: Set[Long]
)