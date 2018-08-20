package com.galois.adapt

import com.galois.adapt
import com.galois.adapt.cdm18.{EVENT_ACCEPT, EVENT_CLOSE, EVENT_EXIT, EVENT_LSEEK, EVENT_MMAP, EVENT_OPEN, EVENT_OTHER, EVENT_READ, EVENT_RECVFROM, EVENT_WRITE, EventType, FileObjectType, SrcSinkType}

import scala.util.parsing.input.Positional
import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.{NoPosition, Position, Reader}





//sealed trait CompilationError

//case class ParserError(location: Location, msg: String) extends CompilationError


trait AST2 extends Positional

trait ProcessActivityAST2 extends SummarizedActivityAST2{
  def eventType: EventType
  def earliestTimestampNanos: TimestampNanos
  def subject: SubjectProcess

  def similar(pa: ProcessActivityAST2): Boolean =
    earliestTimestampNanos == pa.earliestTimestampNanos &&
      eventType == pa.eventType &&
      subject == pa.subject
}
case object ProcessActivityAST2{

}

case class ProcessFileActivityAST2(p: ProcessPath,
                                  eventType: EventType,
                                  subject: SubjectProcess,
                                  fot: FileObjectType,
                                  filePath: FilePath,
                                  earliestTimestampNanos: TimestampNanos
                                 )extends ProcessActivityAST2{
}

case class ProcessNWActivityAST2(processPath: ProcessPath,
                                eventType: EventType,
                                subject: SubjectProcess,
                                neLocal: NWEndpointLocal,
                                neRemote: NWEndpointRemote,
                                earliestTimestampNanos: TimestampNanos
                               ) extends ProcessActivityAST2{

  def hasProcessAndNE(a2: ProcessNWActivityAST2): Boolean =
    subject == a2.subject &&
      neLocal == a2.neLocal &&
      neRemote == a2.neRemote
}



case class ProcessProcessActivityAST2(processPath: ProcessPath,
                                     eventType: EventType,
                                     subject: SubjectProcess,
                                     subject2: SubjectProcess,
                                     earliestTimestampNanos: TimestampNanos
                                    ) extends ProcessActivityAST2{
}

case class ProcessSrcSinkActivityAST2(processPath: ProcessPath,
                                     eventType: EventType,
                                     subject: SubjectProcess,
                                     srcSinkType: SrcSinkType,
                                     earliestTimestampNanos: TimestampNanos
                                    ) extends ProcessActivityAST2{
}



trait SummarizedActivityAST2 extends ProcessActivitiesSetOfUptoTwoAST2
case class FileReadAST2(subject: SubjectProcess, filePaths: List[(FilePath, TimestampNanos)]) extends SummarizedActivityAST2{
}

object FileReadAST2 {
  def fromA1A2(a1:AST2, a2:AST2): FileReadAST2 = (a1, a2) match {
    case (a1: ProcessFileActivityAST2, a2: ProcessFileActivityAST2) =>
      new FileReadAST2(a1.subject,
        List((a1.filePath, a1.earliestTimestampNanos),
          (a2.filePath, a2.earliestTimestampNanos)) )
    case default => ???
  }

}


trait ProcessActivitiesSetOfUptoTwoAST2 extends AST2

case class ProcessNWReadAndFileWriteAST2(pna: ProcessNWActivityAST2, pfa: ProcessFileActivityAST2) extends ProcessActivitiesSetOfUptoTwoAST2




/*EVENT_RECVFROM + EVENT_WRITE+*/
case class ProcessNWReadAndNWWriteAST2(pna1: ProcessNWActivityAST2, pna2: ProcessNWActivityAST2) extends ProcessActivitiesSetOfUptoTwoAST2{

  def similar (a:ProcessNWReadAndNWWriteAST2): Boolean ={
    pna1.subject == a.pna1.subject &&
      pna1.neLocal == a.pna1.neLocal &&
      pna1.neRemote == a.pna1.neRemote
  }

  override def toString: String = {
    "ProcessNWReadAndNWWriteAST2(" +
      pna1.processPath.toString +
      pna1.subject.toString +
      pna1.neLocal.toString +
      pna1.neRemote.toString +
      pna1.earliestTimestampNanos.toString +
      pna2.earliestTimestampNanos.toString +
      ")"
  }
}

object ProcessNWReadAndNWWriteAST2 {
  def apply(pna1: AST2, pna2: AST2) = {
    (pna1, pna2) match {
      case (pna1:ProcessNWActivityAST2, pna2:ProcessNWActivityAST2) => new ProcessNWReadAndNWWriteAST2(pna1, pna2)
      case default => ???
    }
  }
}


object ProcessFileActivityAST2 {
  def fromProcessFileActivity(a: ProcessFileActivity) = ProcessFileActivityAST2(a.processPath, a.eventType, a.subject, a.fot, a.filePath, a.earliestTimestampNanos)
}

object ProcessNWActivityAST2 {
  def fromProcessNWActivity(a: ProcessNWActivity) = ProcessNWActivityAST2(a.processPath, a.eventType, a.subject, a.neLocal, a.neRemote, a.earliestTimestampNanos)
}
object ProcessProcessActivityAST2 {
  def fromProcessProcessActivity(a: ProcessProcessActivity) = ProcessProcessActivityAST2(a.processPath, a.eventType, a.subject, a.subject2, a.earliestTimestampNanos)
}
object ProcessSrcSinkActivityAST2 {
  def fromProcessSrcSinkActivity(a: ProcessSrcSinkActivity) = ProcessSrcSinkActivityAST2(a.processPath, a.eventType, a.subject, a.srcSinkType, a.earliestTimestampNanos)
}

case class ProcessActivityListAST2(activities: List[ProcessActivitiesSetOfUptoTwoAST2]) extends AST2

case class ListAST2(AST2s: List[AST2]) extends AST2{


  /* Tried to mimic JSON, did not work
  override def toString: String = {
    val x = AST2s.map("\""+_.toString+"\"").toString
    "[" + x.slice(5, x.length-1) + "]"
  }
  */
}


case class ActivityGroupsAST2(groupedActivities: List[ActivityGroupAST2]) extends AST2{
  def add(a: ProcessActivityAST2) = {

    def canBeGroupedWithHead(hd:ActivityGroupAST2) = hd.ag.head.earliestTimestampNanos == a.earliestTimestampNanos

    //    groupedActivities match{
    //      case hd::tail => if(canBeGrouped(hd)) ActivityGroups(hd.add(a)::tail) else ActivityGroups(ActivityGroup.empty.add(a)::groupedActivities)
    //      case Nil => ActivityGroups(ActivityGroup.empty.add(a)::groupedActivities)
    //    }
    if(canBeGroupedWithHead(groupedActivities.head))
      ActivityGroupsAST2(groupedActivities.head.add(a)::groupedActivities.tail)
    else
      ActivityGroupsAST2(ActivityGroupAST2.empty.add(a)::groupedActivities)
  }
}

case object ActivityGroupsAST2{
  def empty = ActivityGroupsAST2(List.empty[ActivityGroupAST2])
}

// GroupByTime
case class ActivityGroupAST2(ag: List[ProcessActivityAST2]) extends AST2{

  def add(a: ProcessActivityAST2) = {
    require(a.earliestTimestampNanos == ag.head.earliestTimestampNanos)
    ActivityGroupAST2(a::ag)
  }
}
case object ActivityGroupAST2{
  def empty = ActivityGroupAST2(List.empty[ProcessActivityAST2])
}



object SummaryAST2Parser {
  //def apply(code: List[AST2]): Either[CompilationError, AST2] = {
  def apply(code: List[AST2]): AST2 = {
    Parser2(code)
//    for {
//      AST2 <- Parser(code).right
//    } yield AST2
  }
}


object Parser2 extends Parsers {
  override type Elem = AST2

  class AST2Reader(tokens: Seq[AST2]) extends Reader[AST2] {
    override def first: AST2 = tokens.head

    override def atEnd: Boolean = tokens.isEmpty

    override def pos: Position = tokens.headOption.map(_.pos).getOrElse(NoPosition)

    override def rest: Reader[AST2] = new AST2Reader(tokens.tail)
  }


  //def apply(tokens: Seq[AST2]): Either[ParserError, AST2] = {
  def apply(tokens: Seq[AST2]): AST2 = {
    val reader = new AST2Reader(tokens)
    block(reader) match {
      case NoSuccess(msg, next) => ??? //Left(ParserError(Location(next.pos.line, next.pos.column), msg))
      case Success(result, next) => result
    }
  }

  def block: Parser[AST2] = positioned {


    def rule1 = {
      val list = rep(activity) ^^ { l =>

        val res = l.filter(!isEventOpen(_))
          .filter(!isEventAccept(_))
          .filter(!isEventOther(_))
          .filter(!isEventClose(_))
          .filter(!isEventExit(_))
          .filter(!isEventLseek(_))
          .foldRight(List.empty[ProcessActivityAST2])(mergeConsecutiveActivities(_ similar _, _, _))
          .foldRight(List.empty[ProcessActivitiesSetOfUptoTwoAST2])(mergeConsecutiveSimpleActivitiesIntoNew(readFromNWFollowedByWriteToFile, _, _))
          .foldRight(List.empty[AST2])(genericMerge(readFromNWFollowedByWriteToNW, _, _))
          .foldRight(List.empty[AST2])(genericMerge(mergeFileReads, _, _))
          .foldRight(List.empty[AST2])(genericMerge(mergeSimilarProcessNWReadAndNWWriteAST2, _, _))
          //.map(summarizeEachActivity)

        res
      }
      //list.map(ProcessActivityListAST2)
      list.map(ListAST2)
    }

    rule1
  }

//  /**/
//  def summarizeEachActivity(a: ProcessActivitiesSetOfUptoTwoAST2): ProcessActivitiesSetOfUptoTwoAST2 = a match {
//    case a: ProcessFileActivityAST2 => summarizeFileRead(a)
//    case a: ProcessNWActivityAST2 => a
//    case a: ProcessProcessActivityAST2 => a
//    case a: ProcessSrcSinkActivityAST2 => a
//    case default => default
//  }

//  def summarizeFileRead(a: ProcessFileActivityAST2): SummarizedActivityAST2 = {
//    if (a.eventType == EVENT_READ || a.eventType == EVENT_MMAP)
//      FileRead(a.subject, a.filePath, a.earliestTimestampNanos)
//    else
//      a
//}

  //def funX[A](cond: Boolean, value:A): Option[A] = if (cond) Some(value) else None

  def readOrMmap(e:EventType): Boolean = e == EVENT_READ || e == EVENT_MMAP


  def mergeSimilarProcessNWReadAndNWWriteAST2(a1:AST2, a2:AST2): Option[ProcessNWReadAndNWWriteAST2] = {
    (a1, a2) match {
      case (a1: ProcessNWReadAndNWWriteAST2, a2: ProcessNWReadAndNWWriteAST2) if a1 similar a2 => Some(a1)
      case default => None
    }
  }



  def mergeFileReads(a1:AST2, a2:AST2): Option[FileReadAST2] = {
    def check(pfa1:ProcessFileActivityAST2, pfa2:ProcessFileActivityAST2) = pfa1.subject == pfa2.subject && readOrMmap(pfa1.eventType)&& readOrMmap(pfa2.eventType)
    (a1, a2) match {
      case (a1: ProcessFileActivityAST2, a2: ProcessFileActivityAST2)
        if check(a1, a2) => Some(
        FileReadAST2(a1.subject,
                 List(
                   (a1.filePath, a1.earliestTimestampNanos),
                   (a2.filePath, a2.earliestTimestampNanos))
        )
      )

      case default => None
    }
  }





  def genericMerge(mergeRule: (AST2, AST2) => Option[AST2], a1:AST2, paList:List[AST2]): List[AST2] ={
    paList match{
      case a2::tail => mergeRule(a1, a2) match {
        case Some(mergedActivity) => mergedActivity::tail
        case None => a1::paList
      }
      case Nil => a1::paList
    }
  }



  def readFromNWFollowedByWriteToFile(a1:ProcessActivityAST2, a2:ProcessActivityAST2): Option[ProcessNWReadAndFileWriteAST2] = {
    def check(a1:ProcessActivityAST2, a2:ProcessActivityAST2) = a1.eventType == EVENT_RECVFROM && a2.eventType == EVENT_WRITE && a1.subject == a2.subject
    (a1, a2) match {
      case (a1: ProcessNWActivityAST2, a2: ProcessFileActivityAST2) if check(a1, a2) => Some(ProcessNWReadAndFileWriteAST2(a1, a2))
      case default => None
    }
  }


  def readFromNWFollowedByWriteToNW(a1:AST2, a2:AST2): Option[ProcessNWReadAndNWWriteAST2] = {

    (a1, a2) match {
      case (a1: ProcessNWActivityAST2, a2: ProcessNWActivityAST2) if (a1 hasProcessAndNE a2) => Some(ProcessNWReadAndNWWriteAST2(a1, a2))
      case default => None
    }
  }



  def mergeConsecutiveSimpleActivitiesIntoNew(binaryMergeRule: (ProcessActivityAST2, ProcessActivityAST2) => Option[ProcessActivitiesSetOfUptoTwoAST2], a1:ProcessActivityAST2, paList:List[ProcessActivitiesSetOfUptoTwoAST2]): List[ProcessActivitiesSetOfUptoTwoAST2] = {
    paList match
    {
      case (a2: ProcessActivityAST2):: tail => binaryMergeRule(a1, a2) match {
        case Some(mergedActivity) => mergedActivity::tail
        case None => a1::paList//List(a1,a2):::tail
    }
      case (a2:ProcessActivitiesSetOfUptoTwoAST2)::tail => a1::paList
      case Nil => a1 :: paList
      case default => println(default); ???
    }
  }

//  def f() = {
//    List(binaryMergeRule(a1, a2).getOrElse((a1, a2))) ::: tail
//  }

  def isEventAccept(a: ProcessActivityAST2): Boolean = a.eventType == EVENT_ACCEPT
  def isEventOpen(a: ProcessActivityAST2): Boolean = a.eventType == EVENT_OPEN
  def isEventOther(a: ProcessActivityAST2): Boolean = a.eventType == EVENT_OTHER
  def isEventClose(a: ProcessActivityAST2): Boolean = a.eventType == EVENT_CLOSE
  def isEventExit(a: ProcessActivityAST2): Boolean = a.eventType == EVENT_EXIT
  def isEventLseek(a: ProcessActivityAST2): Boolean = a.eventType == EVENT_LSEEK



  def mergeConsecutiveActivities(binaryMergeRule: (ProcessActivityAST2, ProcessActivityAST2) => Boolean = _==_, a:ProcessActivityAST2, paList:List[ProcessActivityAST2]): List[ProcessActivityAST2] = {
    if (paList.nonEmpty && binaryMergeRule(a, paList.head)) paList else a :: paList
  }


  def combineActivities(a1:AST2, a2:AST2): ProcessActivityListAST2 = {
    (a1, a2) match {
      //remove duplicates
      case (a1: ProcessFileActivityAST2, a2: ProcessFileActivityAST2) => {
        if (a1 == a2) ProcessActivityListAST2(List(a1)) else ProcessActivityListAST2(List(a1, a2))
      }
      case (a1: ProcessNWActivityAST2, a2: ProcessNWActivityAST2) => {
        if (a1 == a2) ProcessActivityListAST2(List(a1)) else ProcessActivityListAST2(List(a1, a2))
      }
      case (a1: ProcessProcessActivityAST2, a2: ProcessProcessActivityAST2) => {
        if (a1 == a2) ProcessActivityListAST2(List(a1)) else ProcessActivityListAST2(List(a1, a2))
      }
      case (a1: ProcessSrcSinkActivityAST2, a2: ProcessSrcSinkActivityAST2) => {
        if (a1 == a2) ProcessActivityListAST2(List(a1)) else ProcessActivityListAST2(List(a1, a2))
      }

      //remove duplicates
      case (a1: ProcessFileActivityAST2, paList: ProcessActivityListAST2) => {
        if (a1 == paList.activities.head) paList else ProcessActivityListAST2(a1 :: paList.activities)
      }
      case (a1: ProcessNWActivityAST2, paList: ProcessActivityListAST2) => {
        if (a1 == paList.activities.head) paList else ProcessActivityListAST2(a1 :: paList.activities)
      }
      case (a1: ProcessProcessActivityAST2, paList: ProcessActivityListAST2) => {
        if (a1 == paList.activities.head) paList else ProcessActivityListAST2(a1 :: paList.activities)
      }
      case (a1: ProcessSrcSinkActivityAST2, paList: ProcessActivityListAST2) => {
        if (a1 == paList.activities.head) paList else ProcessActivityListAST2(a1 :: paList.activities)
      }

      case default => ???
    }
  }

  def applyUnaryRule(unaryRule: ProcessActivityAST2 => ProcessActivityAST2)(a: ProcessActivityAST2) = {
    unaryRule(a)
  }

  def activity: Parser[ProcessActivityAST2] = positioned {
    accept("ProcessActivity", {
      case ProcessFileActivity(p,et,s,fot,fp,t) => ProcessFileActivityAST2(p,et,s,fot,fp,t)
      case ProcessNWActivity(p,et,s,neL,neR,t) => ProcessNWActivityAST2(p,et,s,neL,neR,t)
      case ProcessProcessActivity(p,et,s1,s2,t) => ProcessProcessActivityAST2(p,et,s1,s2,t)
      case ProcessSrcSinkActivity(p,et,s,st,t) => ProcessSrcSinkActivityAST2(p,et,s,st,t)
    }
    )
  }




  def getTimestamp(a: AST2): TimestampNanos = a match {
    case a: ProcessFileActivityAST2 => a.earliestTimestampNanos
    case a: ProcessProcessActivityAST2 => a.earliestTimestampNanos
    case a: ProcessNWActivityAST2 => a.earliestTimestampNanos
    case a: ProcessSrcSinkActivityAST2 => a.earliestTimestampNanos
    case default => ???
  }




  def pfa: Parser[ProcessFileActivityAST2] = positioned{
    accept("process file activity", {case ProcessFileActivity(p,et,s,fot,fp,t) => ProcessFileActivityAST2(p,et,s,fot,fp,t)})
  }
  def pssa: Parser[ProcessSrcSinkActivityAST2] = positioned{
    accept("process src/sink activity", {case ProcessSrcSinkActivity(p,et,s,st,t) => ProcessSrcSinkActivityAST2(p,et,s,st,t)})
  }
  def ppa: Parser[ProcessProcessActivityAST2] = positioned{
    accept("process process activity", {case ProcessProcessActivity(p,et,s1,s2,t) => ProcessProcessActivityAST2(p,et,s1,s2,t)})
  }
  def pnwa: Parser[ProcessNWActivityAST2] = positioned{
    accept("process nw activity", {case ProcessNWActivity(p,et,s,neL,neR,t) => ProcessNWActivityAST2(p,et,s,neL,neR,t)})
  }











  /*******************************
    *
    * Second Iteration
    ******************************/





  def secondPass: Parser[AST2] = positioned {
    ???
  }

  def firstPass: Parser[AST2] = positioned {

    def rule1: Parser[AST2] = pfa ~ pfa ^^ {
      case a1 ~ a2 => combine2Activities(a1,a2)
    }
    def rule2: Parser[AST2] = pssa ~ pssa ^^ {
      case a1 ~ a2 => combine2Activities(a1,a2)
    }
    def rule3: Parser[AST2] = pnwa ~ pnwa ^^ {
      case a1 ~ a2 => combine2Activities(a1,a2)
    }
    def rule4: Parser[AST2] = ppa ~ ppa ^^ {
      case a1 ~ a2 => combine2Activities(a1,a2)
    }
    rule1|rule2|rule3|rule4
  }

  def activityGroups: Parser[ActivityGroupsAST2] = positioned {
    def rule1: Parser[ActivityGroupsAST2] = rep(pa) ^^ {_.foldLeft(ActivityGroupsAST2.empty)((ag, a)=>ag.add(a))}
    rule1
  }

  def pa: Parser[ProcessActivityAST2] = positioned{
    pfa|pssa|ppa|pnwa
  }




  def combine2Activities(a1:AST2, a2:AST2):AST2 = {
    (a1, a2) match {
      //remove duplicates
      case (a1: ProcessFileActivityAST2, a2: ProcessFileActivityAST2) => {
        if (a1.earliestTimestampNanos == a2.earliestTimestampNanos ) ProcessActivityListAST2(List(a1)) else ProcessActivityListAST2(List(a1, a2))
      }
      case (a1: ProcessNWActivityAST2, a2: ProcessNWActivityAST2) => {
        if (a1.earliestTimestampNanos == a2.earliestTimestampNanos ) ProcessActivityListAST2(List(a1)) else ProcessActivityListAST2(List(a1, a2))
      }
      case (a1: ProcessProcessActivityAST2, a2: ProcessProcessActivityAST2) => {
        if (a1.earliestTimestampNanos == a2.earliestTimestampNanos ) ProcessActivityListAST2(List(a1)) else ProcessActivityListAST2(List(a1, a2))
      }
      case (a1: ProcessSrcSinkActivityAST2, a2: ProcessSrcSinkActivityAST2) => {
        if (a1.earliestTimestampNanos == a2.earliestTimestampNanos ) ProcessActivityListAST2(List(a1)) else ProcessActivityListAST2(List(a1, a2))
      }
    }
  }















}