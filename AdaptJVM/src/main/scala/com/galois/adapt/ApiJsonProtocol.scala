package com.galois.adapt

import java.util.UUID

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._


object ApiJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val a = jsonFormat1(Node)
  implicit val b = jsonFormat6(StatusReportString)
  implicit val c = jsonFormat3(UINode)
  implicit val d = jsonFormat3(UIEdge)
  implicit object UUIDFormat extends JsonFormat[UUID] {
    def write(uuid: UUID) = JsString(uuid.toString)
    def read(value: JsValue) = {
      value match {
        case JsString(uuid) => UUID.fromString(uuid)
        case _              => throw new DeserializationException("Expected hexadecimal UUID string")
      }
    }
  }
  implicit val savedNotesJsonProtocol = jsonFormat4(SavedNotes)
}


case class Node(properties: Map[String,Int])
