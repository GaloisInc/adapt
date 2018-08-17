package com.galois.adapt

import com.galois.adapt
import com.galois.adapt.cdm18.{EVENT_ACCEPT, EVENT_CLOSE, EVENT_EXIT, EVENT_LSEEK, EVENT_OPEN, EVENT_OTHER, EVENT_RECVFROM, EVENT_WRITE, EventType, FileObjectType, SrcSinkType}

import scala.util.parsing.input.Positional
import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.{NoPosition, Position, Reader}

object SummaryASTParser {
  def apply(code: List[AST]): Either[CompilationError, AST] = {
    for {
      ast <- Parser(code).right
    } yield ast
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


  def apply(tokens: Seq[AST]): Either[ParserError, AST] = {
    val reader = new ASTReader(tokens)
    block(reader) match {
      case NoSuccess(msg, next) => Left(ParserError(Location(next.pos.line, next.pos.column), msg))
      case Success(result, next) => Right(result)
    }
  }

  def block: Parser[AST] = positioned {
    def rule1 = {
      val list = rep(activity) ^^ {l=>

        val res = l.filter(!isEventOpen(_))
          .filter(!isEventAccept(_))
          .filter(!isEventOther(_))
          .filter(!isEventClose(_))
          .filter(!isEventExit(_))
          .filter(!isEventLseek(_))
          .foldRight(List.empty[ProcessActivityAST])(mergeConsecutiveActivities(_==_, _, _))
          .foldRight(List.empty[ProcessActivityAST])(mergeConsecutiveActivities(_ similar _, _, _))
        /*List[ProcessActivitiesSetOfUptoTwo]*/
          .foldRight(List.empty[ProcessActivitiesSetOfUptoTwo])(mergeConsecutiveSimpleActivitiesIntoNew(readFromNWFollowedByWriteToFile, _, _))

        res
      }
      list.map(ProcessActivityListAST)
    }
    rule1
  }


  def readFromNWFollowedByWriteToFile(a1:ProcessActivityAST, a2:ProcessActivityAST): Option[ProcessNWReadAndFileWriteAST] = {
    def check(a1:ProcessActivityAST, a2:ProcessActivityAST) = a1.eventType == EVENT_RECVFROM && a2.eventType == EVENT_WRITE && a1.subject == a2.subject
    (a1, a2) match {
      case (a1: ProcessNWActivityAST, a2: ProcessFileActivityAST) if check(a1, a2) => Some(ProcessNWReadAndFileWriteAST(a1, a2))
      case default => None
    }
  }


  def mergeConsecutiveSimpleActivitiesIntoNew(binaryMergeRule: (ProcessActivityAST, ProcessActivityAST) => Option[ProcessActivitiesSetOfUptoTwo], a1:ProcessActivityAST, paList:List[ProcessActivitiesSetOfUptoTwo]): List[ProcessActivitiesSetOfUptoTwo] = {
    paList match
    {
      case Nil => a1 :: paList
      case (a2: ProcessActivityAST):: tail => binaryMergeRule(a1, a2) match {
        case Some(mergedActivity) => mergedActivity::tail
        case None => a1::paList//List(a1,a2):::tail
    }
      case (a2:ProcessActivitiesSetOfUptoTwo)::tail => a1::paList
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
      case ProcessFileActivity(p,et,s,fot,fp,t) => ProcessFileActivityAST(p,et,s,fot,fp,t)
      case ProcessNWActivity(p,et,s,neL,neR,t) => ProcessNWActivityAST(p,et,s,neL,neR,t)
      case ProcessProcessActivity(p,et,s1,s2,t) => ProcessProcessActivityAST(p,et,s1,s2,t)
      case ProcessSrcSinkActivity(p,et,s,st,t) => ProcessSrcSinkActivityAST(p,et,s,st,t)
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
    accept("process file activity", {case ProcessFileActivity(p,et,s,fot,fp,t) => ProcessFileActivityAST(p,et,s,fot,fp,t)})
  }
  def pssa: Parser[ProcessSrcSinkActivityAST] = positioned{
    accept("process src/sink activity", {case ProcessSrcSinkActivity(p,et,s,st,t) => ProcessSrcSinkActivityAST(p,et,s,st,t)})
  }
  def ppa: Parser[ProcessProcessActivityAST] = positioned{
    accept("process process activity", {case ProcessProcessActivity(p,et,s1,s2,t) => ProcessProcessActivityAST(p,et,s1,s2,t)})
  }
  def pnwa: Parser[ProcessNWActivityAST] = positioned{
    accept("process nw activity", {case ProcessNWActivity(p,et,s,neL,neR,t) => ProcessNWActivityAST(p,et,s,neL,neR,t)})
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

  def activityGroups: Parser[ActivityGroups] = positioned {
    def rule1: Parser[ActivityGroups] = rep(pa) ^^ {_.foldLeft(ActivityGroups.empty)((ag, a)=>ag.add(a))}
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