package com.galois.adapt

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._


object ApiJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val a = jsonFormat1(Node)
  implicit val b = jsonFormat6(StatusReportString)
  implicit val c = jsonFormat3(UINode)
  implicit val d = jsonFormat3(UIEdge)
}


case class Node(properties: Map[String,Int])
