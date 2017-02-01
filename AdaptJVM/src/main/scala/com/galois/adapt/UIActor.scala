package com.galois.adapt

import akka.actor._
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import scala.concurrent.Await
import scala.concurrent.duration._


class UIActor(interface: String, port: Int) extends Actor with ActorLogging {
  var httpService: Option[Http.ServerBinding] = None

  def receive = {
    case StartUI(dbActor) =>
      implicit val ec = context.dispatcher
      implicit val materializer = ActorMaterializer()
      val httpServiceF = Http()(context.system).bindAndHandle(Routes.mainRoute(dbActor), interface, port)
      val httpService = Await.result(httpServiceF, 10 seconds)

    case StopUI =>
      httpService foreach { s =>
        Await.result(s.unbind(), 10 seconds) // onComplete { _ =>
        httpService = None
      }
  }
}


case class StartUI(dbActor: ActorRef)
case object StopUI