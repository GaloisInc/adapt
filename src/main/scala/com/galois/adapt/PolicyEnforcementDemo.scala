package com.galois.adapt

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import spray.json.DefaultJsonProtocol


object PolicyEnforcementDemo extends SprayJsonSupport with DefaultJsonProtocol {

  case class SimpleResponse(name: String)
  implicit val simpleResponseFormat = jsonFormat1(SimpleResponse)

  val route =
    pathPrefix("api") {
      pathPrefix("pem") {
        get {
          path("ping") {
            complete(
              SimpleResponse("ADAPT")
            )
          } ~
          path("checkPolicy") {
            parameters('policy.as[Int], 'clientIp, 'clientPort.as[Int], 'serverIp, 'serverPort.as[Int], 'timestamp.as[Long], 'requestId.as[Int], 'responseUri) {
              (policy, clientIp, clientPort, serverIp, serverPort, timestamp, requestId, responseUri) =>
//              TODO:
//              202 ACCEPTED: Started the policy check process, will respond later
//              400 BAD REQUEST: (some parameters were invalid)
//              500: Error
                complete(
                  StatusCodes.Accepted -> (policy, clientIp, clientPort, serverIp, serverPort, timestamp, requestId, responseUri).toString  // TODO
                )
            }
          } ~
          path("status") {
            parameter('requestId.as[Int]) { requestId =>
//            TODO:
//            Working on it: 202 ACCEPTED
//            Don't have an active request for that Id: 404 NOT FOUND
//            Error in the request parameters: 400 BAD REQUEST
//            Other Internal Error: 500
              complete(
                StatusCodes.Accepted -> requestId.toString  // TODO
              )
            }
          }
        }
      }
    }
}