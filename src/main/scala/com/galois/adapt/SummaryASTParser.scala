package com.galois.adapt

import com.galois.adapt
import com.galois.adapt.Parser.isEventAccept
import com.galois.adapt.cdm18.{EVENT_ACCEPT, EVENT_CLOSE, EVENT_EXIT, EVENT_LSEEK, EVENT_MMAP, EVENT_OPEN, EVENT_OTHER, EVENT_READ, EVENT_RECVFROM, EVENT_WRITE, EventType, FileObjectType, SrcSinkType}

import scala.reflect.ClassTag
import scala.util.parsing.input.Positional
import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.{NoPosition, Position, Reader}




//sealed trait CompilationError

//case class ParserError(location: Location, msg: String) extends CompilationError


trait AST extends Positional

trait ProcessActivityAST extends SummarizedActivityAST{
  def eventType: EventType
  def earliestTimestampNanos: TimestampNanos
  def subject: SubjectProcess

  def similar(pa: ProcessActivityAST): Boolean =
    earliestTimestampNanos == pa.earliestTimestampNanos &&
      eventType == pa.eventType &&
      subject == pa.subject
}
case object ProcessActivityAST{

}

case class ProcessFileActivityAST(eventType: EventType,
                                  subject: SubjectProcess,
                                  filePath: FilePath,
                                  earliestTimestampNanos: TimestampNanos
                                 )extends ProcessActivityAST{
}

case class ProcessNWActivityAST(eventType: EventType,
                                subject: SubjectProcess,
                                neLocal: NWEndpointLocal,
                                neRemote: NWEndpointRemote,
                                earliestTimestampNanos: TimestampNanos
                               ) extends ProcessActivityAST{

  def hasProcessAndNE(a2: ProcessNWActivityAST): Boolean =
    subject == a2.subject &&
      neLocal == a2.neLocal &&
      neRemote == a2.neRemote
}



case class ProcessProcessActivityAST(eventType: EventType,
                                     subject: SubjectProcess,
                                     subject2: SubjectProcess,
                                     earliestTimestampNanos: TimestampNanos
                                    ) extends ProcessActivityAST{
}

case class ProcessSrcSinkActivityAST(eventType: EventType,
                                     subject: SubjectProcess,
                                     srcSinkType: SrcSinkType,
                                     earliestTimestampNanos: TimestampNanos
                                    ) extends ProcessActivityAST{
}



trait SummarizedActivityAST extends ProcessActivitiesSetOfUptoTwoAST
case class FileReadAST(subject: SubjectProcess, filePaths: List[(FilePath, TimestampNanos)]) extends SummarizedActivityAST{
}

object FileReadAST {
  def fromA1A2(a1:AST, a2:AST): FileReadAST = (a1, a2) match {
    case (a1: ProcessFileActivityAST, a2: ProcessFileActivityAST) =>
      new FileReadAST(a1.subject,
        List((a1.filePath, a1.earliestTimestampNanos),
          (a2.filePath, a2.earliestTimestampNanos)) )
    case default => ???
  }

}


trait ProcessActivitiesSetOfUptoTwoAST extends AST

case class ProcessNWReadAndFileWriteAST(pna: ProcessNWActivityAST, pfa: ProcessFileActivityAST) extends ProcessActivitiesSetOfUptoTwoAST




/*EVENT_RECVFROM + EVENT_WRITE+*/
case class ProcessNWReadAndNWWriteAST(pna1: ProcessNWActivityAST, pna2: ProcessNWActivityAST) extends ProcessActivitiesSetOfUptoTwoAST{

  def similar (a:ProcessNWReadAndNWWriteAST): Boolean ={
    pna1.subject == a.pna1.subject &&
      pna1.neLocal == a.pna1.neLocal &&
      pna1.neRemote == a.pna1.neRemote
  }

  override def toString: String = {
    "ProcessNWReadAndNWWriteAST(" +
      pna1.subject.processPath.toString +
      pna1.subject.toString +
      pna1.neLocal.toString +
      pna1.neRemote.toString +
      pna1.earliestTimestampNanos.toString +
      pna2.earliestTimestampNanos.toString +
      ")"
  }
}

object ProcessNWReadAndNWWriteAST {
  def apply(pna1: AST, pna2: AST) = {
    (pna1, pna2) match {
      case (pna1:ProcessNWActivityAST, pna2:ProcessNWActivityAST) => new ProcessNWReadAndNWWriteAST(pna1, pna2)
      case default => ???
    }
  }

  def isApplicable(a1: ProcessNWActivityAST, a2: ProcessNWActivityAST): Boolean =
    a1.subject == a2.subject && // same process
      a1.neLocal == a2.neLocal && a1.neRemote == a2.neRemote && // same n/w endpoint
      a1.eventType == EVENT_RECVFROM && a2.eventType == EVENT_WRITE // Read followed by a write

}


object ProcessFileActivityAST {
  def fromProcessFileActivity(a: ProcessFileActivity) = ProcessFileActivityAST(a.event, a.subject, a.filePath, a.earliestTimestampNanos)
}

object ProcessNWActivityAST {
  def fromProcessNWActivity(a: ProcessNWActivity) = ProcessNWActivityAST(a.event, a.subject, a.neLocal, a.neRemote, a.earliestTimestampNanos)
}
object ProcessProcessActivityAST {
  def fromProcessProcessActivity(a: ProcessProcessActivity) = ProcessProcessActivityAST(a.event, a.subject, a.subject2, a.earliestTimestampNanos)
}
object ProcessSrcSinkActivityAST {
  def fromProcessSrcSinkActivity(a: ProcessSrcSinkActivity) = ProcessSrcSinkActivityAST(a.event, a.subject, a.srcSinkType, a.earliestTimestampNanos)
}

case class ProcessActivityListAST(activities: List[ProcessActivitiesSetOfUptoTwoAST]) extends AST

case class ListAST(asts: List[AST]) extends AST{


  /* Tried to mimic JSON, did not work
  override def toString: String = {
    val x = asts.map("\""+_.toString+"\"").toString
    "[" + x.slice(5, x.length-1) + "]"
  }
  */
}


case class ActivityGroupsAST(groupedActivities: List[ActivityGroupAST]) extends AST{
  def add(a: ProcessActivityAST) = {

    def canBeGroupedWithHead(hd:ActivityGroupAST) = hd.ag.head.earliestTimestampNanos == a.earliestTimestampNanos

    //    groupedActivities match{
    //      case hd::tail => if(canBeGrouped(hd)) ActivityGroups(hd.add(a)::tail) else ActivityGroups(ActivityGroup.empty.add(a)::groupedActivities)
    //      case Nil => ActivityGroups(ActivityGroup.empty.add(a)::groupedActivities)
    //    }
    if(canBeGroupedWithHead(groupedActivities.head))
      ActivityGroupsAST(groupedActivities.head.add(a)::groupedActivities.tail)
    else
      ActivityGroupsAST(ActivityGroupAST.empty.add(a)::groupedActivities)
  }
}

case object ActivityGroupsAST{
  def empty = ActivityGroupsAST(List.empty[ActivityGroupAST])
}

// GroupByTime
case class ActivityGroupAST(ag: List[ProcessActivityAST]) extends AST{

  def add(a: ProcessActivityAST) = {
    require(a.earliestTimestampNanos == ag.head.earliestTimestampNanos)
    ActivityGroupAST(a::ag)
  }
}
case object ActivityGroupAST{
  def empty = ActivityGroupAST(List.empty[ProcessActivityAST])
}


/********************************************************/
/********************************************************/

//trait AST extends Positional

trait fourthPass extends AST
trait thirdPass extends AST
trait secondPass extends AST
trait firstPass extends AST
trait elementary extends AST

case class PNWA(a: ProcessNWActivity) extends elementary
case class PPA(a: ProcessProcessActivity) extends elementary
case class PFA(a: ProcessFileActivity) extends elementary
case class PSA(a: ProcessSrcSinkActivity) extends elementary

//case class







object SummaryASTParser {
  //def apply(code: List[AST]): Either[CompilationError, AST] = {
  def apply(code: List[AST]): AST = {
    Parser(code)
    //    for {
    //      ast <- Parser(code).right
    //    } yield ast
  }
}


object Parser extends Parsers {
  override type Elem = AST

  class ASTReader(tokens: Seq[AST]) extends Reader[AST] {
    override def first: AST = tokens.head

    override def atEnd: Boolean = tokens.isEmpty

    override def pos: Position = tokens.headOption.map(_.pos).getOrElse(NoPosition)

    override def rest: Reader[AST] = new ASTReader(tokens.tail)
  }


  //def apply(tokens: Seq[AST]): Either[ParserError, AST] = {
  def apply(tokens: Seq[AST]): AST = {
    val reader = new ASTReader(tokens)
    block(reader) match {
      case NoSuccess(msg, next) => ??? //Left(ParserError(Location(next.pos.line, next.pos.column), msg))
      case Success(result, next) => result
    }
  }

  def block: Parser[AST] = positioned {

    def f: (ProcessNWActivityAST, ProcessNWActivityAST) => Boolean = ProcessNWReadAndNWWriteAST.isApplicable

    def rule1 = {
      val list = rep(activity) ^^ { l =>

        val res = l
          /*
          .filter(!isEventOpen(_))
          .filter(!isEventAccept(_))
          .filter(!isEventOther(_))
          .filter(!isEventClose(_))
          .filter(!isEventExit(_))
          .filter(!isEventLseek(_))
        */
          .filter(i => !(isEventOpen(i) || isEventAccept(i) || isEventOther(i) || isEventClose(i) || isEventExit(i) || isEventLseek(i)))

          .foldRight(List.empty[ProcessActivityAST])(mergeConsecutiveActivities(_ similar _, _, _))

          .foldRight(List.empty[ProcessActivitiesSetOfUptoTwoAST])(mergeConsecutiveSimpleActivitiesIntoNew(readFromNWFollowedByWriteToFile, _, _))

          //.foldRight(List.empty[AST])(genericMerge(readFromNWFollowedByWriteToNW, _, _))

          //.foldRight(List.empty[AST])(genericMerge(readFromNWFollowedByWriteToNW, ProcessNWReadAndNWWriteAST.apply, _, _))
          //.foldRight(List.empty[AST])(genericMerge(checkTypesAndCond(hasSameProcessAndNE, _, _), ProcessNWReadAndNWWriteAST.apply, _, _))
          //.foldRight(List.empty[AST])(genericMerge(checkTypesAndCond[ProcessNWActivityAST,ProcessNWActivityAST](hasSameProcessAndNE, _, _), ProcessNWReadAndNWWriteAST.apply, _, _))
          .foldRight(List.empty[AST])(genericMerge(checkTypesAndCond(ProcessNWReadAndNWWriteAST.isApplicable, _, _), ProcessNWReadAndNWWriteAST.apply, _, _))
          //if (a1 hasSameProcessAndNE

          .foldRight(List.empty[AST])(genericMerge(mergeFileReads,FileReadAST.fromA1A2, _, _))
          .foldRight(List.empty[AST])(genericMerge(mergeSimilarProcessNWReadAndNWWriteAST, (x1,x2)=>x1, _, _))
        //.map(summarizeEachActivity)

        res
      }
      //list.map(ProcessActivityListAST)
      list.map(ListAST)
    }

    rule1
  }

  //  /**/
  //  def summarizeEachActivity(a: ProcessActivitiesSetOfUptoTwoAST): ProcessActivitiesSetOfUptoTwoAST = a match {
  //    case a: ProcessFileActivityAST => summarizeFileRead(a)
  //    case a: ProcessNWActivityAST => a
  //    case a: ProcessProcessActivityAST => a
  //    case a: ProcessSrcSinkActivityAST => a
  //    case default => default
  //  }

  //  def summarizeFileRead(a: ProcessFileActivityAST): SummarizedActivityAST = {
  //    if (a.eventType == EVENT_READ || a.eventType == EVENT_MMAP)
  //      FileRead(a.subject, a.filePath, a.earliestTimestampNanos)
  //    else
  //      a
  //}

  //def funX[A](cond: Boolean, value:A): Option[A] = if (cond) Some(value) else None

  def readOrMmap(e:EventType): Boolean = e == EVENT_READ || e == EVENT_MMAP

  //TODO:delete
  //  def mergeSimilarProcessNWReadAndNWWriteAST(a1:AST, a2:AST): Option[ProcessNWReadAndNWWriteAST] = {
  //    (a1, a2) match {
  //      case (a1: ProcessNWReadAndNWWriteAST, a2: ProcessNWReadAndNWWriteAST) if a1 similar a2 => Some(a1)
  //      case default => None
  //    }
  //  }

  def mergeSimilarProcessNWReadAndNWWriteAST(a1:AST, a2:AST): Boolean = {
    (a1, a2) match {
      case (a1: ProcessNWReadAndNWWriteAST, a2: ProcessNWReadAndNWWriteAST) if a1 similar a2 => true
      case default => false
    }
  }
  //def checkTypesAndCond[A1<:AST,A2<:AST](cond: (A1, A2)=>Boolean, a1:A1, a2:A2)(implicit tag1: ClassTag[A1], tag2: ClassTag[A2])= cond(a1,a2)


  def checkTypesAndCond[A1<:AST,A2<:AST](cond: (A1, A2)=>Boolean, a1:AST, a2:AST)(implicit tag1: ClassTag[A1], tag2: ClassTag[A2])= (a1,a2) match{
    case (a1: A1, a2:A2) if cond(a1, a2) => true
    case _ => false
  }

  //TODO: delete
  //  def mergeFileReads(a1:AST, a2:AST): Option[FileRead] = {
  //    def check(pfa1:ProcessFileActivityAST, pfa2:ProcessFileActivityAST) = pfa1.subject == pfa2.subject && readOrMmap(pfa1.eventType)&& readOrMmap(pfa2.eventType)
  //    (a1, a2) match {
  //      case (a1: ProcessFileActivityAST, a2: ProcessFileActivityAST)
  //        if check(a1, a2) => Some(
  //        FileRead(a1.subject,
  //                 List(
  //                   (a1.filePath, a1.earliestTimestampNanos),
  //                   (a2.filePath, a2.earliestTimestampNanos))
  //        )
  //      )
  //
  //      case default => None
  //    }
  //  }

  def mergeFileReads(a1:AST, a2:AST): Boolean = {
    def check(pfa1:ProcessFileActivityAST, pfa2:ProcessFileActivityAST) = pfa1.subject == pfa2.subject && readOrMmap(pfa1.eventType)&& readOrMmap(pfa2.eventType)
    (a1, a2) match {
      case (a1: ProcessFileActivityAST, a2: ProcessFileActivityAST) if check(a1, a2) => true
      case default => false
    }
  }


  //TODO: delete
  //  def genericMerge(mergeRule: (AST, AST) => Option[AST], a1:AST, paList:List[AST]): List[AST] ={
  //    paList match{
  //      case a2::tail => mergeRule(a1, a2) match {
  //        case Some(mergedActivity) => mergedActivity::tail
  //        case None => a1::paList
  //      }
  //      case Nil => a1::paList
  //    }
  //  }


  def genericMerge[A<:AST](mergeRule: (AST, AST) => Boolean, transformationRule: (AST, AST) =>A, a1:AST, paList:List[AST]): List[AST] ={
    paList match{
      case a2::tail => if (mergeRule(a1, a2)) transformationRule(a1, a2)::tail else a1::paList
      case Nil => a1::paList
    }
  }


  def readFromNWFollowedByWriteToFile(a1:ProcessActivityAST, a2:ProcessActivityAST): Option[ProcessNWReadAndFileWriteAST] = {
    def check(a1:ProcessActivityAST, a2:ProcessActivityAST) = a1.eventType == EVENT_RECVFROM && a2.eventType == EVENT_WRITE && a1.subject == a2.subject
    (a1, a2) match {
      case (a1: ProcessNWActivityAST, a2: ProcessFileActivityAST) if check(a1, a2) => Some(ProcessNWReadAndFileWriteAST(a1, a2))
      case default => None
    }
  }


  def mergeConsecutiveSimpleActivitiesIntoNew(binaryMergeRule: (ProcessActivityAST, ProcessActivityAST) => Option[ProcessActivitiesSetOfUptoTwoAST], a1:ProcessActivityAST, paList:List[ProcessActivitiesSetOfUptoTwoAST]): List[ProcessActivitiesSetOfUptoTwoAST] = {
    paList match
    {
      case (a2: ProcessActivityAST):: tail => binaryMergeRule(a1, a2) match {
        case Some(mergedActivity) => mergedActivity::tail
        case None => a1::paList//List(a1,a2):::tail
      }
      case (a2:ProcessActivitiesSetOfUptoTwoAST)::tail => a1::paList
      case Nil => a1 :: paList
      case default => println(default); ???
    }
  }

  //  def f() = {
  //    List(binaryMergeRule(a1, a2).getOrElse((a1, a2))) ::: tail
  //  }

  def isEventAccept(a: ProcessActivityAST): Boolean = a.eventType == EVENT_ACCEPT
  def isEventOpen(a: ProcessActivityAST): Boolean = a.eventType == EVENT_OPEN
  def isEventOther(a: ProcessActivityAST): Boolean = a.eventType == EVENT_OTHER
  def isEventClose(a: ProcessActivityAST): Boolean = a.eventType == EVENT_CLOSE
  def isEventExit(a: ProcessActivityAST): Boolean = a.eventType == EVENT_EXIT
  def isEventLseek(a: ProcessActivityAST): Boolean = a.eventType == EVENT_LSEEK



  def mergeConsecutiveActivities(binaryMergeRule: (ProcessActivityAST, ProcessActivityAST) => Boolean = _==_, a:ProcessActivityAST, paList:List[ProcessActivityAST]): List[ProcessActivityAST] = {
    if (paList.nonEmpty && binaryMergeRule(a, paList.head)) paList else a :: paList
  }


  def combineActivities(a1:AST, a2:AST): ProcessActivityListAST = {
    (a1, a2) match {
      //remove duplicates
      case (a1: ProcessFileActivityAST, a2: ProcessFileActivityAST) => {
        if (a1 == a2) ProcessActivityListAST(List(a1)) else ProcessActivityListAST(List(a1, a2))
      }
      case (a1: ProcessNWActivityAST, a2: ProcessNWActivityAST) => {
        if (a1 == a2) ProcessActivityListAST(List(a1)) else ProcessActivityListAST(List(a1, a2))
      }
      case (a1: ProcessProcessActivityAST, a2: ProcessProcessActivityAST) => {
        if (a1 == a2) ProcessActivityListAST(List(a1)) else ProcessActivityListAST(List(a1, a2))
      }
      case (a1: ProcessSrcSinkActivityAST, a2: ProcessSrcSinkActivityAST) => {
        if (a1 == a2) ProcessActivityListAST(List(a1)) else ProcessActivityListAST(List(a1, a2))
      }

      //remove duplicates
      case (a1: ProcessFileActivityAST, paList: ProcessActivityListAST) => {
        if (a1 == paList.activities.head) paList else ProcessActivityListAST(a1 :: paList.activities)
      }
      case (a1: ProcessNWActivityAST, paList: ProcessActivityListAST) => {
        if (a1 == paList.activities.head) paList else ProcessActivityListAST(a1 :: paList.activities)
      }
      case (a1: ProcessProcessActivityAST, paList: ProcessActivityListAST) => {
        if (a1 == paList.activities.head) paList else ProcessActivityListAST(a1 :: paList.activities)
      }
      case (a1: ProcessSrcSinkActivityAST, paList: ProcessActivityListAST) => {
        if (a1 == paList.activities.head) paList else ProcessActivityListAST(a1 :: paList.activities)
      }

      case default => ???
    }
  }

  def applyUnaryRule(unaryRule: ProcessActivityAST => ProcessActivityAST)(a: ProcessActivityAST) = {
    unaryRule(a)
  }

  def activity: Parser[ProcessActivityAST] = positioned {
    accept("ProcessActivity", {
      case ProcessFileActivity(et,s,fp,t) => ProcessFileActivityAST(et,s,fp,t)
      case ProcessNWActivity(et,s,neL,neR,t) => ProcessNWActivityAST(et,s,neL,neR,t)
      case ProcessProcessActivity(et,s1,s2,t) => ProcessProcessActivityAST(et,s1,s2,t)
      case ProcessSrcSinkActivity(et,s,st,t) => ProcessSrcSinkActivityAST(et,s,st,t)
    }
    )
  }




  def getTimestamp(a: AST): TimestampNanos = a match {
    case a: ProcessFileActivityAST => a.earliestTimestampNanos
    case a: ProcessProcessActivityAST => a.earliestTimestampNanos
    case a: ProcessNWActivityAST => a.earliestTimestampNanos
    case a: ProcessSrcSinkActivityAST => a.earliestTimestampNanos
    case default => ???
  }




  def pfa: Parser[ProcessFileActivityAST] = positioned{
    accept("process file activity", {case ProcessFileActivity(et,s,fp,t) => ProcessFileActivityAST(et,s,fp,t)})
  }
  def pssa: Parser[ProcessSrcSinkActivityAST] = positioned{
    accept("process src/sink activity", {case ProcessSrcSinkActivity(et,s,st,t) => ProcessSrcSinkActivityAST(et,s,st,t)})
  }
  def ppa: Parser[ProcessProcessActivityAST] = positioned{
    accept("process process activity", {case ProcessProcessActivity(et,s1,s2,t) => ProcessProcessActivityAST(et,s1,s2,t)})
  }
  def pnwa: Parser[ProcessNWActivityAST] = positioned{
    accept("process nw activity", {case ProcessNWActivity(et,s,neL,neR,t) => ProcessNWActivityAST(et,s,neL,neR,t)})
  }











  /*******************************
    *
    * Second Iteration
    ******************************/





  def secondPass: Parser[AST] = positioned {
    ???
  }

  def firstPass: Parser[AST] = positioned {

    def rule1: Parser[AST] = pfa ~ pfa ^^ {
      case a1 ~ a2 => combine2Activities(a1,a2)
    }
    def rule2: Parser[AST] = pssa ~ pssa ^^ {
      case a1 ~ a2 => combine2Activities(a1,a2)
    }
    def rule3: Parser[AST] = pnwa ~ pnwa ^^ {
      case a1 ~ a2 => combine2Activities(a1,a2)
    }
    def rule4: Parser[AST] = ppa ~ ppa ^^ {
      case a1 ~ a2 => combine2Activities(a1,a2)
    }
    rule1|rule2|rule3|rule4
  }

  def activityGroups: Parser[ActivityGroupsAST] = positioned {
    def rule1: Parser[ActivityGroupsAST] = rep(pa) ^^ {_.foldLeft(ActivityGroupsAST.empty)((ag, a)=>ag.add(a))}
    rule1
  }

  def pa: Parser[ProcessActivityAST] = positioned{
    pfa|pssa|ppa|pnwa
  }




  def combine2Activities(a1:AST, a2:AST):AST = {
    (a1, a2) match {
      //remove duplicates
      case (a1: ProcessFileActivityAST, a2: ProcessFileActivityAST) => {
        if (a1.earliestTimestampNanos == a2.earliestTimestampNanos ) ProcessActivityListAST(List(a1)) else ProcessActivityListAST(List(a1, a2))
      }
      case (a1: ProcessNWActivityAST, a2: ProcessNWActivityAST) => {
        if (a1.earliestTimestampNanos == a2.earliestTimestampNanos ) ProcessActivityListAST(List(a1)) else ProcessActivityListAST(List(a1, a2))
      }
      case (a1: ProcessProcessActivityAST, a2: ProcessProcessActivityAST) => {
        if (a1.earliestTimestampNanos == a2.earliestTimestampNanos ) ProcessActivityListAST(List(a1)) else ProcessActivityListAST(List(a1, a2))
      }
      case (a1: ProcessSrcSinkActivityAST, a2: ProcessSrcSinkActivityAST) => {
        if (a1.earliestTimestampNanos == a2.earliestTimestampNanos ) ProcessActivityListAST(List(a1)) else ProcessActivityListAST(List(a1, a2))
      }
    }
  }















}