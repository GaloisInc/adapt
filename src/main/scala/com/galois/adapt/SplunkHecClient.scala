package com.galois.adapt

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import spray.json.{JsObject, JsString, JsonWriter}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

//[Ref: https://doc.akka.io/docs/akka-http/10.0.2/scala/http/common/http-model.html]
import HttpMethods._
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, GenericHttpCredentials}
import akka.util.ByteString
//import HttpProtocols._
import MediaTypes._
import HttpCharsets._
import StatusCodes._

import akka.event.{Logging, LoggingAdapter}

import spray.json.JsValue

//[REF: https://doc.akka.io/docs/akka-http/10.0.2/scala/http/common/http-model.html https://doc.akka.io/docs/akka-http/current/common/uri-model.html]

/*
* Reference: Message format [http://dev.splunk.com/view/event-collector/SP-CAAAE6P]
*
* "time"
*         The event time. The default time format is epoch time format, in the format <sec>.<ms>. For example,
*         1433188255.500 indicates 1433188255 seconds and 500 milliseconds after epoch,
*         or Monday, June 1, 2015, at 7:50:55 PM GMT.
* "host"
*         The host value to assign to the event data. This is typically the hostname of the client from which you're
*         sending data.
* "source"
*         The source value to assign to the event data. For example, if you're sending data from an app you're
*         developing, you could set this key to the name of the app.
* "sourcetype"
*         The sourcetype value to assign to the event data.
* "index"
*         The name of the index by which the event data is to be indexed. The index you specify here must within the
*         list of allowed indexes if the token has the indexes parameter set.
* "fields"
*         (Not applicable to raw data.) Specifies a JSON object that contains explicit custom fields to be defined at
*         index time. Requests containing the "fields" property must be sent to the /collector/event endpoint, or they
*         will not be indexed. For more information, see Indexed field extractions.
* */

case class EventMsg(eventData: JsValue, time:Long, host:String="localhost", source: String="localhost", sourcetype:String="json", index:String="default") {

  //example:
  //  {
  //    "time": 1426279439, // epoch time
  //    "host": "localhost",
  //    "source": "datasource",
  //    "sourcetype": "txt",
  //    "index": "main",
  //    "event": { "Hello world!" }
  //  }

//  implicit val eventJsonWriter = new JsonWriter[Event] {
//    def write(event: Event): JsValue = {
//      JsObject(
//        "time" -> JsString(event.time.toString),
//        "host" -> JsString(event.host),
//        "source" -> JsString(event.source),
//        "sourcetype" -> JsString(event.sourcetype),
//        "index" -> JsString(event.index),
//        "event" -> event.eventData
//      )
//    }
//  }

  def toJson: JsValue = {
    JsObject(
      "time" -> JsString(this.time.toString),
      "host" -> JsString(this.host),
      "source" -> JsString(this.source),
      "sourcetype" -> JsString(this.sourcetype),
      "index" -> JsString(this.index),
      "event" -> this.eventData
    )
  }


}

/*
* Example Usage:
*
*     val event = JsObject(
*      "alarm_type" -> JsString("testAlarm2"),
*      "file" -> JsString("testFile2")
*    )
*    splunkHecClient(token = "58288208-9db4-4f42-99e2-f5fdcdf19d24", host = "127.0.0.1", port= 8088).sendEvent(event);*
*
*
*/

case class SplunkHecClient(token: String, host:String, port:Int) {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  val log: LoggingAdapter = Logging.getLogger(system, logSource = this)

  //uri:Uri = Uri("http://127.0.0.1:8088/services/collector/event/1.0")
  val homeUri =  Uri.from(scheme = "http", host=host, port=port, path = "/services/collector/event/1.0")

  def sendEvent(event:JsValue) = {
    val time = System.currentTimeMillis
    sendEventHttp(EventMsg(event, time))
  }

  def sendEventHttp(event:EventMsg) = {

    val data = ByteString(event.toJson.toString)

    // customize every detail of HTTP request
    //val authorization = headers.Authorization(BasicHttpCredentials("Splunk", token))
    val authorization = headers.Authorization(GenericHttpCredentials("Splunk", token))
    val req = HttpRequest(
      POST,
      uri = homeUri,
      headers = List(authorization),
      entity = HttpEntity(`text/plain` withCharset `UTF-8`, data)
      //protocol = `HTTP/1.0`)
    )

    val responseFuture: Future[HttpResponse] = Http().singleRequest(req)

    responseFuture.onComplete {
      case Success(res) => httpReqResponseHandler(res)
      case Failure(res) => log.error(s"splunk message not sent: ${res}")
    }
  }

  def httpReqResponseHandler(response: HttpResponse) = response.status match {
    // Splunk's reponse for malformed data:
    //[INFO] [09/14/2018 23:52:07.572] [default-akka.actor.default-dispatcher-4] [splunkHecClient$(akka://default)] HttpResponse(400 Bad Request,List(Date: Fri, 14 Sep 2018 23:52:07 GMT, X-Content-Type-Options: nosniff, Vary: Authorization, Connection: Keep-Alive, X-Frame-Options: SAMEORIGIN, Server: Splunkd),HttpEntity.Strict(application/json,{"text":"No data","code":5}),HttpProtocol(HTTP/1.1))

    // Splunk's reponse for valid data:
    //[INFO] [09/14/2018 23:53:11.348] [default-akka.actor.default-dispatcher-5] [splunkHecClient$(akka://default)] HttpResponse(200 OK,List(Date: Fri, 14 Sep 2018 23:53:11 GMT, X-Content-Type-Options: nosniff, Vary: Authorization, Connection: Keep-Alive, X-Frame-Options: SAMEORIGIN, Server: Splunkd),HttpEntity.Strict(application/json,{"text":"Success","code":0}),HttpProtocol(HTTP/1.1))

        case OK => log.info(response.toString)
        case _ => log.error(s"splunk message malformed? Failed with: ${response}")
      }
}