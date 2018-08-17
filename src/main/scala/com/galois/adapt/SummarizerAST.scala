package com.galois.adapt

import java.util.UUID

import com.galois.adapt.cdm18.{EventType, FileObjectType, SrcSinkType}

import scala.util.parsing.input.Positional

/*definitions*/

trait ProcessActivity extends AST{//extends Positional{
def eventType: EventType
  def earliestTimestampNanos: TimestampNanos
  def subject: SubjectProcess
}

//trait ProcessActivity

case class ProcessFileActivity(processPath: ProcessPath,
                               eventType: EventType,
                               subject: SubjectProcess,
                               fot: FileObjectType,
                               filePath: FilePath,
                               earliestTimestampNanos: TimestampNanos
                              )extends ProcessActivity{
}

case class ProcessNWActivity(processPath: ProcessPath,
                             eventType: EventType,
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
}

case class ProcessProcessActivity(processPath: ProcessPath,
                                  eventType: EventType,
                                  subject: SubjectProcess,
                                  subject2: SubjectProcess,
                                  earliestTimestampNanos: TimestampNanos
                                 ) extends ProcessActivity{
}


case class ProcessSrcSinkActivity(processPath: ProcessPath,
                                  eventType: EventType,
                                  subject: SubjectProcess,
                                  srcSinkType: SrcSinkType,
                                  earliestTimestampNanos: TimestampNanos
                                 ) extends ProcessActivity{
}

case class FilePath(path: String)
case class TimestampNanos(t: Long)
case class ProcessPath(path: String)
case class SubjectProcess(uuid: UUID, cid: Int, processPath:ProcessPath)

case class NWAddress(a: String) //extends Positional
case class NWPort(p: Int) //extends Positional
case class NWEndpointLocal(a: NWAddress, p: NWPort) //extends Positional
case class NWEndpointRemote(a: NWAddress, p: NWPort) //extends Positional

/********************************************************/

sealed trait CompilationError

case class ParserError(location: Location, msg: String) extends CompilationError


case class Location(line: Int, column: Int) {
  override def toString = s"$line:$column"
}


trait AST extends Positional

//case class ProcessActivityAST extends ProcessActivity with AST{
//  val eventType: EventType
//  val earliestTimestampNanos: TimestampNanos
//}

trait ProcessActivityAST extends ProcessActivitiesSetOfUptoTwo with AST{
  def eventType: EventType
  def earliestTimestampNanos: TimestampNanos
  def subject: SubjectProcess

  def similar(pa: ProcessActivityAST): Boolean = earliestTimestampNanos == pa.earliestTimestampNanos &&
    eventType == pa.eventType &&
    subject == pa.subject
}
case object ProcessActivityAST{

}

case class ProcessFileActivityAST(p: ProcessPath,
                                  eventType: EventType,
                                  subject: SubjectProcess,
                                  fot: FileObjectType,
                                  filePath: FilePath,
                                  earliestTimestampNanos: TimestampNanos
                                 )extends ProcessActivityAST{
}

case class ProcessNWActivityAST(processPath: ProcessPath,
                                eventType: EventType,
                                subject: SubjectProcess,
                                neLocal: NWEndpointLocal,
                                neRemote: NWEndpointRemote,
                                earliestTimestampNanos: TimestampNanos
                               ) extends ProcessActivityAST{
}

case class ProcessProcessActivityAST(processPath: ProcessPath,
                                     eventType: EventType,
                                     subject: SubjectProcess,
                                     subject2: SubjectProcess,
                                     earliestTimestampNanos: TimestampNanos
                                    ) extends ProcessActivityAST{
}

case class ProcessSrcSinkActivityAST(processPath: ProcessPath,
                                     eventType: EventType,
                                     subject: SubjectProcess,
                                     srcSinkType: SrcSinkType,
                                     earliestTimestampNanos: TimestampNanos
                                    ) extends ProcessActivityAST{
}

trait ProcessActivitiesSetOfUptoTwo extends AST
case class ProcessNWReadAndFileWriteAST(pna: ProcessNWActivityAST, pfa: ProcessFileActivityAST) extends ProcessActivitiesSetOfUptoTwo{
  override def toString: String = "ProcessNWReadAndFileWriteAST"
}






object ProcessFileActivityAST {
  def fromProcessFileActivity(a: ProcessFileActivity) = ProcessFileActivityAST(a.processPath, a.eventType, a.subject, a.fot, a.filePath, a.earliestTimestampNanos)
}

object ProcessNWActivityAST {
  def fromProcessNWActivity(a: ProcessNWActivity) = ProcessNWActivityAST(a.processPath, a.eventType, a.subject, a.neLocal, a.neRemote, a.earliestTimestampNanos)
}
object ProcessProcessActivityAST {
  def fromProcessProcessActivity(a: ProcessProcessActivity) = ProcessProcessActivityAST(a.processPath, a.eventType, a.subject, a.subject2, a.earliestTimestampNanos)
}
object ProcessSrcSinkActivityAST {
  def fromProcessSrcSinkActivity(a: ProcessSrcSinkActivity) = ProcessSrcSinkActivityAST(a.processPath, a.eventType, a.subject, a.srcSinkType, a.earliestTimestampNanos)
}

case class ProcessActivityListAST(activities: List[ProcessActivitiesSetOfUptoTwo]) extends AST


case class ActivityGroups(groupedActivities: List[ActivityGroup]) extends AST{
  def add(a: ProcessActivityAST) = {

    def canBeGroupedWithHead(hd:ActivityGroup) = hd.ag.head.earliestTimestampNanos == a.earliestTimestampNanos

//    groupedActivities match{
//      case hd::tail => if(canBeGrouped(hd)) ActivityGroups(hd.add(a)::tail) else ActivityGroups(ActivityGroup.empty.add(a)::groupedActivities)
//      case Nil => ActivityGroups(ActivityGroup.empty.add(a)::groupedActivities)
//    }
    if(canBeGroupedWithHead(groupedActivities.head))
      ActivityGroups(groupedActivities.head.add(a)::groupedActivities.tail)
    else
      ActivityGroups(ActivityGroup.empty.add(a)::groupedActivities)
  }
}

case object ActivityGroups{
  def empty = ActivityGroups(List.empty[ActivityGroup])
}

// GroupByTime
case class ActivityGroup(ag: List[ProcessActivityAST]) extends AST{

  def add(a: ProcessActivityAST) = {
    require(a.earliestTimestampNanos == ag.head.earliestTimestampNanos)
    ActivityGroup(a::ag)
  }
}
case object ActivityGroup{
  def empty = ActivityGroup(List.empty[ProcessActivityAST])
}