package com.galois.adapt

import java.util.UUID

import akka.actor.{Actor, ActorRef}
import akka.stream.scaladsl.Flow
import com.galois.adapt.adm.ERStreamComponents.CDM
import com.galois.adapt.adm.EntityResolution.Timed
import com.galois.adapt.adm.UuidRemapper.{GetCdm2Adm, ResultOfGetCdm2Adm}
import com.galois.adapt.cdm18.Event
import akka.pattern.ask
import akka.util.Timeout
import com.galois.adapt.adm.CdmUUID
import com.galois.adapt.cdm18._
import collection.mutable.{Map => MutableMap}
import scala.concurrent.{ExecutionContext, Future}


class TagPropagationActor extends Actor {
  val trustValues = MutableMap.empty[UUID, TrustTag]
  val confidentialityValues = MutableMap.empty[UUID, ConfidentialityTag]

  val defaultTrust = Unknown
  val defaultConfidentiality = Secret

  def receive = {
    case PropagateTrustTags(from, to) =>
//      println(s"propagating trust from: $from to: $to")
      val resultingTrust = trustValues.getOrElse(from, defaultTrust) leastTrusted trustValues.getOrElse(to, defaultTrust)
      trustValues += to -> resultingTrust
      if (resultingTrust < BenignAuthentic && confidentialityValues.getOrElse(to, Public) > Public) println(s"Trust vs. Confidentiality violation on: $to")

    case PropagateConfidentialityTags(from, to) =>
//      println(s"propagating confidentiality from: $from to: $to")
      val resultingConfidentiality = confidentialityValues.getOrElse(from, defaultConfidentiality) mostConfidential confidentialityValues.getOrElse(to, defaultConfidentiality)
      confidentialityValues += to -> resultingConfidentiality
      if (resultingConfidentiality > Public && trustValues.getOrElse(to, BenignAuthentic) < BenignAuthentic) println(s"Confidentiality vs. Trust violation on: $to")

    case SetTrustTag(onNode, toLevel) => trustValues += onNode -> toLevel

    case SetConfidentialityTag(onNode, toLevel) => confidentialityValues += onNode -> toLevel

    case GetTagStatus(forNode) => sender() ! TagStatusResults(forNode, trustValues.get(forNode), confidentialityValues.get(forNode))
  }
}

case class PropagateTrustTags(from: UUID, to: UUID)
case class PropagateConfidentialityTags(from: UUID, to: UUID)
case class SetTrustTag(onNode: UUID, toLevel: TrustTag)
case class SetConfidentialityTag(onNode: UUID, toLevel: ConfidentialityTag)
case class GetTagStatus(forNode: UUID)
case class TagStatusResults(forNode: UUID, trust: Option[TrustTag], confidentiality: Option[ConfidentialityTag])


trait Tag

trait TrustTag {
  val ordered = List(Unknown, Benign, BenignAuthentic)  // Increasing trust
  def mostTrusted(other: TrustTag): TrustTag = if (ordered.indexOf(this) > ordered.indexOf(other)) this else other
  def leastTrusted(other: TrustTag): TrustTag = if (ordered.indexOf(this) < ordered.indexOf(other)) this else other
  def <(other: TrustTag): Boolean = ordered.indexOf(this) < ordered.indexOf(other)
  def >(other: TrustTag): Boolean = ordered.indexOf(this) > ordered.indexOf(other)
}

case object BenignAuthentic extends TrustTag
case object Benign extends TrustTag
case object Unknown extends TrustTag


trait ConfidentialityTag {
  val ordered: List[ConfidentialityTag] = List(Public, Private, Sensitive, Secret)  // Increasing confidentiality
  def mostConfidential(other: ConfidentialityTag): ConfidentialityTag = if (ordered.indexOf(this) > ordered.indexOf(other)) this else other
  def leastConfidential(other: ConfidentialityTag): ConfidentialityTag = if (ordered.indexOf(this) < ordered.indexOf(other)) this else other
  def <(other: ConfidentialityTag): Boolean = ordered.indexOf(this) < ordered.indexOf(other)
  def >(other: ConfidentialityTag): Boolean = ordered.indexOf(this) > ordered.indexOf(other)
}

case object Secret extends ConfidentialityTag
case object Sensitive extends ConfidentialityTag
case object Private extends ConfidentialityTag
case object Public extends ConfidentialityTag


object TagPropStreamComponents {

  def tagPropByEventFlow(uuidRemapper: ActorRef, tagPropActor: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext) = Flow.fromFunction[Timed[CDM], Timed[CDM]]( tcdm => {
    tcdm.unwrap match {
      case e @ Event(uuid, _, eventType, _, hostUuid, subjectUuidOpt, _, pUuid1, _, pUuid2, _,_,_,_,_,_,_) =>
        def admUuidsF = {
          val p1AdmOF = pUuid1.map(u => (uuidRemapper ? GetCdm2Adm(CdmUUID(u))).mapTo[ResultOfGetCdm2Adm])
          val p2AdmOF = pUuid2.map(u => (uuidRemapper ? GetCdm2Adm(CdmUUID(u))).mapTo[ResultOfGetCdm2Adm])
          val futureAdmUuids: Option[Future[(Option[UUID], Option[UUID])]] = p1AdmOF.map(p1AdmF =>
            p2AdmOF.fold[Future[(Option[UUID], Option[UUID])]](
              p1AdmF.map(p1Adm =>
                Some(p1Adm.target.uuid) -> None
              )
            )(p2AdmF =>
              p1AdmF.flatMap(p1Adm =>
                p2AdmF.map(p2Adm =>
                  Some(p1Adm.target.uuid) -> Some(p2Adm.target.uuid)
                )
              )
            )
          )
          futureAdmUuids.getOrElse(Future.successful(None -> None))
        }
        eventType match {
          case EVENT_READ | EVENT_LOADLIBRARY | EVENT_EXECUTE =>
            admUuidsF.map { admUuids =>
              admUuids._1.map(p1 => subjectUuidOpt.map(subUuid => tagPropActor ! PropagateConfidentialityTags(p1, subUuid)))  // Why is the cTag not propagated from the this file to its subject along the read event?  g.V().has('uuid',59911ac4-f6ef-3c11-9ddc-1edbb405e188)
              admUuids._2.map(p2 => subjectUuidOpt.map(subUuid => tagPropActor ! PropagateConfidentialityTags(p2, subUuid)))
            }
          case EVENT_WRITE | EVENT_RENAME | EVENT_UNLINK | EVENT_CHANGE_PRINCIPAL =>
            admUuidsF.map { admUuids =>
              admUuids._1.map(p1 => subjectUuidOpt.map(subUuid => tagPropActor ! PropagateConfidentialityTags(subUuid, p1)))
              admUuids._2.map(p2 => subjectUuidOpt.map(subUuid => tagPropActor ! PropagateConfidentialityTags(subUuid, p2)))
            }
          case _ => ()
        }
      case _ => ()
    }
    tcdm
  })

}