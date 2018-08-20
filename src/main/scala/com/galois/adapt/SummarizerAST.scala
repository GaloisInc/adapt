package com.galois.adapt

import java.util.UUID

import com.galois.adapt.cdm18.{EventType, FileObjectType, SrcSinkType}

import scala.util.parsing.input.Positional

/*definitions*/

trait ProcessActivity extends AST with AST2{//extends Positional{
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
