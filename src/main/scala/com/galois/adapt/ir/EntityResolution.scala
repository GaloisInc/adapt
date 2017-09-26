package com.galois.adapt.ir

import com.galois.adapt.ir.ERStreamComponents._
import com.galois.adapt.cdm17._
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}
import scala.concurrent.{ExecutionContext, Await, Future}
import com.galois.adapt.FlowComponents

import akka.actor.Props
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.dispatch.ExecutionContexts
import akka.util.Timeout
import akka.stream.{FlowShape, DelayOverflowStrategy, Attributes}
import akka.stream.scaladsl.{Flow, Source, GraphDSL, Broadcast, Merge, Partition}

object EntityResolution {

  /* Creates an ER flow converting CDM to IR.
   *
   *  @param tickTimeout time interval to wait for other entities to use in ER (ex: waiting for
   *  write events that may have information about a file object's path)
   *
   *  @param renameRetryLimit maximum number of times to retry converting an objects UUIDs to point
   *  to IR instead of CDM
   *
   *  @param renameRetryDelay amont of time to wait before retrying to convert an objects UUIDs to
   *  point to IR instead of CDM
   */
  def apply(tickTimeout: Long = 10, renameRetryLimit: Long = 10, renameRetryDelay: Long = 5)(implicit system: ActorSystem): Flow[CDM, IR, _] = {
    type CdmUUID = java.util.UUID
    type IrUUID = java.util.UUID

    implicit val ec: ExecutionContext = system.dispatcher
    val renameActor: ActorRef = system.actorOf(Props[MapActor[CdmUUID, IrUUID]])
   
    erWithoutRenamesFlow(renameActor, tickTimeout)
      .via(renameFlow(renameActor, renameRetryLimit, renameRetryDelay))
  }


  // Perform entity resolution on stream of CDMs to convert them into IRs
  private def erWithoutRenamesFlow(renameActor: ActorRef, tickTimeout: Long): Flow[CDM, IR, _] = Flow.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    /* The original CDM stream gets split into one stream per resolved CDM type. At each of the
     * nodes, only the bare minimum of state is maintained to perform entity resolution. Most of
     * that state is kept implicitly via 'groupBy' operations.
     *
     * Then, every time a new IR element is being created, a message is sent to a renaming actor.
     * This actor maintains information about the mapping from old CDM to new resolved IR. However,
     * the resolved events emitted by this flow still have UUIDs that point to the old CDM.
     *
     *                             ,__   Event    __
     *                            / ,_  Subject   _ `\
     *                           / /,_   File     _`\ \
     *                      --CDM +---  Netflow   _`_\_\_ IR (unrenamed) -->
     *                           \ \`-  SrcSink   _/ / /
     *                            \ `- Principal  _,/ /
     *                             `-- Provenance __,/
     *                                       \
     *                                        v
     *                                      ,-'''''''''-.
     *                                     | renameActor |
     *                                      `-_________-'
     */

    // split stream into 7 components (one for each CDM type)
    val broadcast = b.add(Broadcast[CDM](7))
    
    // merge those components back together
    val merge = b.add(Merge[IR](7))


    // Connect the graph
    broadcast.out(0).collect({ case e: Event => e })            .via(eventResolution(tickTimeout))                   ~> merge.in(0) 
    broadcast.out(1)                                            .via(subjectResolution(tickTimeout, renameActor))    ~> merge.in(1) 
    broadcast.out(2)                                            .via(fileObjectResolution(tickTimeout, renameActor)) ~> merge.in(2) 
    broadcast.out(3).collect({ case n: NetFlowObject => n })    .via(netflowResolution(renameActor))                 ~> merge.in(3) 
    broadcast.out(4).collect({ case s: SrcSinkObject => s })    .via(srcSinkResolution(renameActor))                 ~> merge.in(4) 
    broadcast.out(5).collect({ case p: Principal => p })        .via(principalResolution(renameActor))               ~> merge.in(5) 
    broadcast.out(6).collect({ case t: ProvenanceTagNode => t }).via(provenanceTagNodeResolution(renameActor))       ~> merge.in(6) 

    // expose ports
    FlowShape(broadcast.in, merge.out)
  })


  // Remap UUIDs in IR from CDMs to other IRs
  private def renameFlow(renameActor: ActorRef, retryLimit: Long, retryTime: Long)
                        (implicit ec: ExecutionContext): Flow[IR, IR, _] = Flow.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._
    
    // Function that does the actual renaming - either produces a 'Right' or a 'Left' with retries
    // incremented
    val rename: ((IR,Int)) => Either[(/* unrenamed */ IR, Int), /* renamed */ IR] = { case (ir, retries) =>

      // The 1 second timeout should never happen: the renameActor is supposed to be non-blocking
      implicit val timeout = Timeout(1 second)

      // Failure within the future is soft (failure to find a UUID should result in a retry)
      val renamed: Future[Option[IR]] = ir.remapUuids(renameActor)
        .map(Some(_))                  // Wrap all successful futures with 'Some'
        .recover({ case _ => None })   // Convert all failed futures into 'None'

      // Failure to get a result within 2 seconds is hard (the rename actor is not supposed to be
      // blocking)

      // TODO: get rid of Await
      Await.result(renamed, 2 second) match {
        case None if retries < retryLimit => Left((ir, retries + 1))
        case None => Right(ir) // TODO: in this case, make a new UUID
        case Some(renamedIr) => Right(renamedIr)
      }
    }

    // TODO: add type alias for resolved/unresolved IR

    /* This takes in IR where the consituent UUIDs still point to old CDM and outputs IR where the
     * constituent UUIDs point to new IR.
     *
     * The new UUIDs are obtained from the renameActor. Unresolved IR come in and check if the
     * rename actor has all the information for resolving them.
     *
     *  - if so, the resolved IR is just emitted downstream.
     *  - otherwise, the IR is fed back into the rename flow (with a rety time delay and a retry limit)
     *
     * We expect that _most_ of the time, there will be no need for retries (since something pointed
     * to should be before something doing the pointing in the CDM data, and our entity resolution
     * flows should not mix up that ordering too much.
     *
     *                                       ,-'''''''''-.
     *                                      | renameActor |
     *                                       `-_________-'         
     *                                           /   \
     *                                          ^    v
     *                                           \  /
     *                     ---IR (unrenamed)--+-Rename-+-->--IR (renamed)
     *                                       /          \
     *                                       \          /
     *                                        `---<<---'
     */

    // Zip incoming IR with 0 retries
    val preProcess = b.add(Flow.fromFunction[IR,(IR,Int)](ir => (ir, 0)))

    // merge the loop-back and initial components back together. The second element in the tuple is
    // the number of retries so far.
    val merge = b.add(Merge[(IR, Int)](2))
    
    // split the stream into the successes and the retries
    val partition = b.add(Partition[Either[(IR,Int), IR]](2, { case Right(_) => 0; case Left(_) => 1 }))

    // unpack unsuccessfully renamed IR elements and filter out the ones that have too many retries
    val retries = b.add(Flow[Either[(IR,Int), IR] /* Left[(IR,Int)] */ ]
      .collect {
        case Left((ir, retries)) => 
        if (retries >= retryLimit) {
            println(s"Too many rename attempts ($retries): $ir")
        }
        (ir, retries)
      }
      .delay(retryTime seconds, DelayOverflowStrategy.emitEarly)
      .withAttributes(Attributes.inputBuffer(initial = 10000, max = 10000))
    )

    // unpack successfully renamed IR elements
    val postProcess = b.add(Flow[Either[(IR,Int), IR] /* Right[IR] */ ].collect { case Right(ir) => ir })


    // Connect the graph
    preProcess.out ~> merge.in(0);  merge.out.map(rename) ~> partition.in;  partition.out(0) ~> postProcess.in;
    retries.out    ~> merge.in(1);                                          partition.out(1) ~> retries.in;

    // Expose ports
    FlowShape(preProcess.in, postProcess.out)
  })

}
