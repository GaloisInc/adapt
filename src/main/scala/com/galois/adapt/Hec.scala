package com.galois.adapt

//import com.galois.adapt.Application.system
//
//import scala.concurrent.Future
//import com.splunk.logging
import java.util.logging.Logger

object Hec {
  private val logger = Logger.getLogger("splunkLogger")

  def sendEvent(eventMsg:String) = {
    logger.info(eventMsg)
  }

}
