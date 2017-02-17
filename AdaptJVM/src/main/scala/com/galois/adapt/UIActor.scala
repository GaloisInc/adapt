package com.galois.adapt

import akka.actor._
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.galois.adapt.ServiceRegistryProtocol.{PublishService, SubscribeToService, UnPublishService}

import scala.concurrent.Await
import scala.concurrent.duration._


class UIActor(val registry: ActorRef, interface: String, port: Int) extends Actor with ActorLogging with ServiceClient {
  var httpService: Option[Http.ServerBinding] = None

  val dependencies = "DevDBActor" :: Nil

  def beginService() = localReceive(StartUI(dependencyMap("DevDBActor").get))

  def endService() = localReceive(StopUI)

  def localReceive: PartialFunction[Any,Unit] = {
    case msg @ StartUI(dbActor) =>
      if (httpService.isEmpty) {
        implicit val ec = context.dispatcher
        implicit val materializer = ActorMaterializer()
        val httpServiceF = Http()(context.system).bindAndHandle(Routes.mainRoute(dbActor), interface, port)
        httpService = Some(Await.result(httpServiceF, 10 seconds))
        registry ! PublishService(this.getClass.getSimpleName, context.self)
      } else {
        log.warning(s"Got a message to start the UI when it was already running: $msg\n from: $sender")
      }

    case msg @ StopUI =>
      if (httpService.nonEmpty) {
        httpService foreach { s =>
          Await.result(s.unbind(), 10 seconds) // onComplete { _ =>
          httpService = None
          registry ! UnPublishService(thisName)
        }
      } else {
        log.warning(s"Got a message to stop the UI when it was already stopped: $msg\n from: $sender")
      }
  }
}


case class StartUI(dbActor: ActorRef)
case object StopUI