package com.galois.adapt

import java.util.UUID

import com.galois.adapt.cdm18.{EventType, FileObjectType, SrcSinkType}

import scala.util.parsing.input.Positional
import scala.collection.mutable.HashMap
/*definitions*/

trait ProcessActivity extends Positional with AST with AST2 with Element{//extends Positional{
def event: EventType
  def earliestTimestampNanos: TimestampNanos
  def subject: SubjectProcess
  def myFlatten: List[Object]
  def toStr:String
}

object ProcessActivity{
  def truncUuid(u:UUID) = u.toString.split("-")(4)
}

case class ProcessActivitySummary(a:ProcessActivity)extends Element{

  override def toString: String = s"${}"
}


//trait ProcessActivity

case class ProcessFileActivity(event: EventType,
                               subject: SubjectProcess,
                               /*fot: FileObjectType,*/
                               filePath: FilePath,
                               earliestTimestampNanos: TimestampNanos
                              )extends ProcessActivity{

  def myFlatten: List[Object] = {
    List(this.event, this.subject.uuid, this.subject.processPath, this.filePath, this.earliestTimestampNanos)
  }

  //TODO: For prefix trees
  ///override def toString: String = s"${subject.processPath},${event.toString},${subject.uuid},${filePath.path},${earliestTimestampNanos.t}"
  def toStr:String = s"${ProcessActivity.truncUuid(subject.uuid)},${event},${filePath.path},${earliestTimestampNanos.t}"
}

case class ProcessNWActivity(event: EventType,
                             subject: SubjectProcess,
                             /*
                             localAddress: NWAddress,
                             localPort: NWPort,
                             remoteAddress: NWAddress,
                             remotePort: NWPort,
                             */
                             neLocal: NWEndpointLocal,
                             neRemote: NWEndpointRemote,
                             earliestTimestampNanos: TimestampNanos
                            ) extends ProcessActivity{

  def myFlatten: List[Object] = {
    List(this.event, this.subject.uuid, this.subject.processPath, this.neLocal.a, this.neLocal.p, this.neRemote.a, this.neRemote.p, this.earliestTimestampNanos)
  }
  def toStr:String = s"${ProcessActivity.truncUuid(subject.uuid)},${event},${neRemote.a},${neRemote.p},${earliestTimestampNanos.t}"
}

case class ProcessProcessActivity(event: EventType,
                                  subject: SubjectProcess,
                                  subject2: SubjectProcess,
                                  earliestTimestampNanos: TimestampNanos
                                 ) extends ProcessActivity{

  def myFlatten: List[Object] = {
    List(this.event, this.subject.uuid, this.subject.processPath, this.subject2.uuid, this.subject2.processPath, this.earliestTimestampNanos)
  }
  def toStr:String = s"${ProcessActivity.truncUuid(subject.uuid)},${event},${subject2.processPath.path},${earliestTimestampNanos.t}"
}


case class ProcessSrcSinkActivity(event: EventType,
                                  subject: SubjectProcess,
                                  srcSinkType: SrcSinkType,
                                  earliestTimestampNanos: TimestampNanos
                                 ) extends ProcessActivity{

  def myFlatten: List[Object] = {
    List(this.event, subject.uuid, this.subject.processPath, this.srcSinkType, this.earliestTimestampNanos)
  }
  def toStr:String = s"${ProcessActivity.truncUuid(subject.uuid)},${event},${srcSinkType},${earliestTimestampNanos.t}"
}

case class FilePath(path: String)
case class TimestampNanos(t: Long)
case class ProcessPath(path: String)
case class SubjectProcess(uuid: UUID, cid: Int, processPath:ProcessPath)
object SubjectProcess {
  def empty = new SubjectProcess(UUID.fromString("00000000-0000-0000-0000-000000000000"), 0, ProcessPath(""))
}

case class NWAddress(a: String) //extends Positional
case class NWPort(p: Int) //extends Positional
case class NWEndpointLocal(a: NWAddress, p: NWPort) //extends Positional
case class NWEndpointRemote(a: NWAddress, p: NWPort) //extends Positional

/********************************************************/

case class Trie(char:String, children:HashMap[String, Trie]=HashMap.empty) {
  def add(tokens: List[String]) = {
    var node:Trie = this
    for (token <- tokens){
      node = node.children.getOrElseUpdate(token, Trie(token))
    }

  }
  def empty = Trie("")

  def pretty_print_html(lvl:Int = 0): String = {
    var s = ""

    s += "<li>"
    s += this.char

    if(this.children.nonEmpty) {
      s += "*"
      s += "<ul>"
      this.children.toList.foreach{case (_, c) => s += "\n" + c.pretty_print_html(lvl+1)}
      s += "</ul>"
    }
    s += "</li>"
    s
  }
}