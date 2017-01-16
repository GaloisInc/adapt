package com.galois.adapt.scepter

import akka.actor.{Actor, ActorRef}
import com.galois.adapt.cdm13._

/* This actor tries to see if the CDM statements it is receiving come from 'BasicOps.sh' If it
 * receives enough evidence this is the case, it will expect to recieve _all_ evidence.
 */
class BasicOpsIdentifyingActor extends Actor {
  
  // 'BasicOps.sh'-like defining events
  var expectedEvents = Map(
    "FileObject for 'zqxf1'" -> false,
    "FileObject for 'zqxf4'" -> false,
    "Subject for moving 'zqxf1' to 'zqfx4'" -> false,
    "Subject for 'chmod' on 'zqxf1'" -> false,
    "Subject for 'touch' on 'zqxf1'" -> false,
    "Subject for 'sleep' on '1'" -> false,
    "Subject for 'nmap' on 'git.tc.bbn.com'" -> false,
    "Event for 'touch' on 'zqxf1'" -> false
  )
  var updates = 0

  // Current opinion on whether the current data-set is 'BasicOps.sh'
  def isBasicOps = true //updates > 3

  // Record the presence of the event
  private def logEvent(msg: String): Unit = {
    expectedEvents = expectedEvents.updated(msg, true)
    //updates = updates + 1
  }

  def receive = {
    // CDM statements that should be in BasicOps.sh
    case FileObject(_, _, "file:///tmp/zqxf1", false, _, _)
      => logEvent("FileObject for 'zqxf1'")
    case FileObject(_, _, "file:///tmp/zqxf4", false, _, _)
      => logEvent("FileObject for 'zqxf4'");
    case Subject(_, SUBJECT_PROCESS, _, _, _, _, _, _, Some("mv /tmp/zqxf1 /tmp/zqxf4"), _, _, _, _)
      => logEvent("Subject for moving 'zqxf1' to 'zqfx4'");
    case Subject(_, SUBJECT_PROCESS, _, _, _, _, _, _, Some("chmod 0666 /tmp/zqxf1"), _, _, _, _)
      => logEvent("Subject for 'chmod' on 'zqxf1'");
    case Subject(_, SUBJECT_PROCESS, _, _, _, _, _, _, Some("touch /tmp/zqxf1"), _, _, _, _)
      => logEvent("Subject for 'touch' on 'zqxf1'");
    case Subject(_, SUBJECT_PROCESS, _, _, _, _, _, _, Some("sleep 1"), _, _, _, _)
      => logEvent("Subject for 'sleep' on '1'");
    case Subject(_, SUBJECT_PROCESS, _, _, _, _, _, _, Some("nmap -A git.tc.bbn.com"), _, _, _, _)
      => logEvent("Subject for 'nmap' on 'git.tc.bbn.com'");
    case Event(_, _, _, _, _, _, Some("touch"), Some(Seq(_ /* "/tmp/zqfx1" */)), _, _, _, _)
      => logEvent("Event for 'touch' on 'zqxf1'");

    // Receive a query asking about the counts stored
    case IsBasicOps =>
       sender() ! (if (isBasicOps) Some(expectedEvents) else None);
  }
}

case object IsBasicOps
