package com.galois.adapt

import java.util.UUID

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.tinkerpop.gremlin.structure.{Edge, Vertex}
import spray.json._
import scala.collection.JavaConverters._


object ApiJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val statusReport = jsonFormat5(StatusReport)
  implicit val c = jsonFormat3(UINode)
  implicit val d = jsonFormat3(UIEdge)
  implicit object UUIDFormat extends JsonFormat[UUID] {
    def write(uuid: UUID) = JsString(uuid.toString)
    def read(value: JsValue) = value match {
      case JsString(uuid) => UUID.fromString(uuid)
      case _              => throw new DeserializationException("Expected hexadecimal UUID string")
    }
  }

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