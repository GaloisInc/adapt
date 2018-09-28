package com.galois.adapt

import java.util.UUID

import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.galois.adapt.NoveltyDetection.NamespacedUuidDetails
import com.galois.adapt.adm._
import com.galois.adapt.cdm19.{CustomEnum, EventType, FileObjectType, HostIdentifier, HostType, Interface, PrincipalType, SrcSinkType, SubjectType}
import org.apache.tinkerpop.gremlin.structure.{Edge, Vertex}
import spray.json._

import scala.collection.JavaConverters._
import scala.collection.SortedSet


object ApiJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {

  implicit object UUIDFormat extends JsonFormat[UUID] {
    def write(uuid: UUID) = JsString(uuid.toString)
    def read(value: JsValue) = value match {
      case JsString(uuid) => UUID.fromString(uuid)
      case _              => throw new DeserializationException("Expected hexadecimal UUID string")
    }
  }

  implicit val statusReport = jsonFormat4(StatusReport)
  implicit val populationLog = jsonFormat16(PopulationLog)
  implicit val cdmUuid: RootJsonFormat[CdmUUID] = jsonFormat2(CdmUUID.apply)
  implicit val admUuid: RootJsonFormat[AdmUUID] = jsonFormat2(AdmUUID.apply)

  implicit val extendedUuid: RootJsonFormat[NamespacedUuid] = new RootJsonFormat[NamespacedUuid] {
    override def write(eUuid: NamespacedUuid): JsValue = {
      val JsObject(payload) = eUuid match {
        case cdm: CdmUUID => cdmUuid.write(cdm)
        case adm: AdmUUID => admUuid.write(adm)
      }
      JsObject(payload + ("type" -> JsString(eUuid.productPrefix)))
    }

    override def read(json: JsValue): NamespacedUuid =
      json.asJsObject.getFields("type") match {
        case Seq(JsString("CdmUUID")) => cdmUuid.read(json)
        case Seq(JsString("AdmUUID")) => admUuid.read(json)
      }
  }

  implicit val extendedUuidDetails: RootJsonFormat[NamespacedUuidDetails] = new RootJsonFormat[NamespacedUuidDetails] {
    override def write(eUuid: NamespacedUuidDetails): JsValue = {
      val JsObject(payload) = extendedUuid.write(eUuid.extendedUuid)
      JsObject(payload + ("name" -> JsString(eUuid.name.getOrElse(""))))
    }

    override def read(json: JsValue): NamespacedUuidDetails = {
      val eUuid = extendedUuid.read(json)
      val name = json.asJsObject.getFields("name") match {
        case Seq(JsString("")) => None
        case Seq(JsString(jsName)) => Some(jsName)
      }
      NamespacedUuidDetails(eUuid, name)
    }
  }

  // All enums are JSON-friendly through their string representation
  def jsonEnumFormat[T](enum: CustomEnum[T]): RootJsonFormat[T] = new RootJsonFormat[T]{
    override def write(t: T): JsValue = JsString(t.toString)
    override def read(o: JsValue): T = o match {
      case JsString(s) => enum.from(s).get
      case _ => throw DeserializationException("EventType: expected string")
    }
  }
  implicit val eventType = jsonEnumFormat(EventType)
  implicit val fileObjectType = jsonEnumFormat(FileObjectType)
  implicit val srcSinkType = jsonEnumFormat(SrcSinkType)
  implicit val subjectType = jsonEnumFormat(SubjectType)
  implicit val principalType = jsonEnumFormat(PrincipalType)
  implicit val hostType = jsonEnumFormat(HostType)

  implicit val cdmInterface = jsonFormat3(Interface.apply)
  implicit val cdmHostIdentifier = jsonFormat2(HostIdentifier.apply)

  // ADM nodes
  implicit val admEvent = jsonFormat(AdmEvent.apply, "originalCdmUuids", "eventType", "earliestTimestampNanos", "latestTimestampNanos", "provider")
  implicit val admSubject = jsonFormat(AdmSubject.apply, "originalCdmUuids", "subjectTypes", "cid", "startTimestampNanos", "provider")
  implicit val admPathNode = jsonFormat(AdmPathNode.apply, "path", "provider")
  implicit val admFileObject = jsonFormat(AdmFileObject.apply, "originalCdmUuids", "fileObjectType", "size", "provider")
  implicit val admNetFlowObject = jsonFormat(AdmNetFlowObject.apply, "originalCdmUuids", "localAddress", "localPort", "remoteAddress", "remotePort", "provider")
  implicit val admAddress = jsonFormat(AdmAddress.apply(_), "address")
  implicit val admPort = jsonFormat(AdmPort.apply(_), "port")
  implicit val admSrcSinkObject = jsonFormat(AdmSrcSinkObject.apply, "originalCdmUuids", "srcSinkType", "provider")
  implicit val admPrincipal = jsonFormat(AdmPrincipal.apply, "originalCdmUuids", "userId", "groupIds", "principalType", "username", "provider")
  implicit val admProvenanceTagNode = jsonFormat(AdmProvenanceTagNode.apply, "originalCdmUuids", "programPoint", "provider")
  implicit val admHost = jsonFormat(AdmHost.apply, "originalCdmUuids", "hostName", "hostIdentifiers", "osDetails", "hostType", "interfaces", "provider")
  implicit val admSynthesized = jsonFormat(AdmSynthesized.apply(_), "originalCdmUuids")

  implicit val adm: RootJsonFormat[ADM] = new RootJsonFormat[ADM] {
    override def write(adm: ADM): JsValue = {
      val JsObject(payload) = adm match {
        case p: AdmEvent => admEvent.write(p)
        case p: AdmSubject => admSubject.write(p)
        case p: AdmPathNode => admPathNode.write(p)
        case p: AdmFileObject => admFileObject.write(p)
        case p: AdmNetFlowObject => admNetFlowObject.write(p)
        case p: AdmAddress => admAddress.write(p)
        case p: AdmPort => admPort.write(p)
        case p: AdmSrcSinkObject => admSrcSinkObject.write(p)
        case p: AdmPrincipal => admPrincipal.write(p)
        case p: AdmProvenanceTagNode => admProvenanceTagNode.write(p)
        case p: AdmHost => admHost.write(p)
        case p: AdmSynthesized => admSynthesized.write(p)
      }
      JsObject(payload + ("type" -> JsString(adm.productPrefix)))
    }

    override def read(p: JsValue): ADM =
      p.asJsObject.getFields("type") match {
        case Seq(JsString("AdmEvent")) => admEvent.read(p)
        case Seq(JsString("AdmSubject")) => admSubject.read(p)
        case Seq(JsString("AdmPathNode")) => admPathNode.read(p)
        case Seq(JsString("AdmFileObject")) => admFileObject.read(p)
        case Seq(JsString("AdmNetFlowObject")) => admNetFlowObject.read(p)
        case Seq(JsString("AdmAddress")) => admAddress.read(p)
        case Seq(JsString("AdmPort")) => admPort.read(p)
        case Seq(JsString("AdmSrcSinkObject")) => admSrcSinkObject.read(p)
        case Seq(JsString("AdmPrincipal")) => admPrincipal.read(p)
        case Seq(JsString("AdmProvenanceTagNode")) => admProvenanceTagNode.read(p)
        case Seq(JsString("AdmHost")) => admHost.read(p)
        case Seq(JsString("AdmSynthesized")) => admSynthesized.read(p)
      }
  }

  implicit def sortedSetFormat[T : JsonFormat : Ordering] = viaSeq[SortedSet[T], T](seq => SortedSet.empty)
  implicit object uiTreeElementFormat extends JsonFormat[UiTreeElement] {
    def write(obj: UiTreeElement) = obj match {
      case node: UiTreeNode => uiTreeNodeFormat.write(node)
      case folder: UiTreeFolder => uiTreeFolderFormat.write(folder)
    }
    def read(json: JsValue) =
      if (json.asJsObject.fields.contains("folder")) uiTreeFolderFormat.read(json)
      else uiTreeNodeFormat.read(json)
  }
  implicit val uiDataContainerFormat = jsonFormat6(UiDataContainer.apply)
  implicit val uiTreeNodeFormat = jsonFormat2(UiTreeNode)
  implicit val uiTreeFolderFormat = jsonFormat4(UiTreeFolder.apply)

  implicit val uiNodeFormat = jsonFormat3(UINode)
  implicit val uiEdgeFormat = jsonFormat3(UIEdge)

  val vertexTypeTuple = "type" -> JsString("vertex")
  val edgeTypeTuple   = "type" -> JsString("edge")

  def vertexToJson(v: Vertex): JsValue = {
    val jsProps = v.keys().asScala.toList.map { k =>
      v.value[Any](k) match {
        case i: java.lang.Integer => k -> JsArray(JsObject("value" -> JsNumber(i)))
        case i: java.lang.Long => k -> JsArray(JsObject("value" -> JsNumber(i)))
        case i: java.lang.Boolean => k -> JsArray(JsObject("value" -> JsBoolean(i)))
        case i: java.lang.Float => k -> JsArray(JsObject("value" -> JsNumber(i.doubleValue())))
        case i: java.lang.Double => k -> JsArray(JsObject("value" -> JsNumber(i)))
        case i: java.lang.Character => k -> JsArray(JsObject("value" -> JsString(i.toString)))
        case i: java.lang.String => k -> JsArray(JsObject("value" -> JsString(i)))
        case i: java.util.UUID => k -> JsArray(JsObject("value" -> JsString(i.toString)))
        case x =>
          throw new RuntimeException(s"Unhandled type for: $x   Not all types are supported. See here:  http://s3.thinkaurelius.com/docs/titan/1.0.0/schema.html")
      }
    }.toMap[String,JsValue]

    JsObject(
      vertexTypeTuple,
      "id" -> JsNumber(v.id().asInstanceOf[Long]),
      "label" -> JsString(v.label()),
      "properties" -> JsObject(jsProps)
    )
  }


  def edgeToJson(e: Edge): JsValue = JsObject(
    "id" -> JsString(e.id().toString),
    "label" -> JsString(e.label()),
    edgeTypeTuple,
//    "inVLabel" -> JsString(e.inVertex().label()),
//    "outVLabel" -> JsString(e.outVertex().label()),
    "inV" -> JsNumber(e.inVertex().id().asInstanceOf[Long]),
    "outV" -> JsNumber(e.outVertex().id().asInstanceOf[Long])
  )
}



case class UIEdge(from: String, to: String, label: String)
case class UINode(id: String, label: String, title: String)