package com.galois.adapt

import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._


object ApiJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val a = jsonFormat1(Node)
}


case class Node(properties: Map[String,Int])
