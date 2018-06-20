package com.galois.adapt

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }
import scala.util.control.Breaks._
import akka.pattern.ask
import scala.concurrent.Future
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util.Try
import spray.json._

/*
object PrinterActor {
  //def props(message: String, printerActor: ActorRef): Props = Props(new Greeter(message, printerActor))
  def props(x:Int): Props = Props(new PrinterActor(x))

}*/

/*
class SummarizerActor(dbActor:ActorRef) extends Actor{
  //import PrinterActor._

  def receive = {
    case msg:Any =>
      println("Msg received: ")
      println(msg)
  }
}
*/

/*
def findAllFilesRead() = {
    g.V(id)
}
*/

object Summarizer{
    def process_uuid(uuid: String): Future[String] = {
        ///???
        implicit val executionContext = Application.system.dispatcher
        implicit val timeout = Timeout(10 seconds)
        val msg = StringQuery("g.V().limit(1)")
        val ret = Application.dbActor ? msg
        //ret.map(_.toString())
        //ret.asInstanceOf[Future[Future[...]]
        //ret.mapTo[Future[Try[Stream[JsValue]]]].flatMap(_.map(_.toString()))
        ret.mapTo[Future[Try[Int]]].flatMap(_.map(_.toString()))
    }
}
