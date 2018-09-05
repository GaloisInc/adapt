// what is difference b/w process forks and execute
package com.galois.adapt

import java.util.UUID

import scala.concurrent.{ExecutionContextExecutor, Future, Await}

import com.galois.adapt.cdm18.{EVENT_ACCEPT, EVENT_CLOSE, EVENT_EXECUTE, EVENT_EXIT, EVENT_FORK, EVENT_LSEEK, EVENT_MMAP, EVENT_MODIFY_PROCESS, EVENT_OPEN, EVENT_OTHER, EVENT_READ, EVENT_RECVFROM, EVENT_WRITE, EventType, FileObjectType, SrcSinkType}

import scala.reflect.ClassTag

trait Element
trait ComposedActivity

object SummaryParser {

  // Count
  def countActivities[A<:Element](l:Seq[Element], f:A=>Boolean = (_:A)=>true)(implicit tag: ClassTag[A]): Int = l.count{case a:A if f(a)=> true; case _ => false}
  def filter[A<:Element](l:List[Element], f:A=>Boolean = (_:A)=>true)(implicit tag: ClassTag[A]): List[A] = l.collect{case a:A if f(a)=> a}

  def isFileActivity(a:ProcessActivity):Boolean = a match {case a:ProcessFileActivity => true; case _ => false}
  def isNWActivity(a:ProcessActivity):Boolean = a match {case a:ProcessNWActivity => true; case _ => false}
  def isProcessActivity(a:ProcessActivity):Boolean = a match {case a:ProcessProcessActivity => true; case _ => false}
  def isSrcSinkActivity(a:ProcessActivity):Boolean = a match {case a:ProcessSrcSinkActivity => true; case _ => false}

  //def isOfType[A](a:ProcessActivity):Boolean = {case _:A => true; case _ => false}

  // elementary activities
  def isNWRead(a:ProcessNWActivity): Boolean = a.event == EVENT_RECVFROM
  def isNWWrite(a:ProcessNWActivity): Boolean = a.event == EVENT_WRITE
  def isFileWrite(a:ProcessFileActivity): Boolean = a.event == EVENT_WRITE
  def isFileRead(a:ProcessFileActivity): Boolean = a.event == EVENT_READ || a.event == EVENT_MMAP
  def isFileExec(a:ProcessFileActivity): Boolean = a.event == EVENT_EXECUTE
  def isProcessFork(a:ProcessProcessActivity): Boolean = a.event == EVENT_FORK
  def isProcessModify(a:ProcessProcessActivity): Boolean = a.event == EVENT_MODIFY_PROCESS

  def areTwoFilesSame(a1:ProcessFileActivity,a2:ProcessFileActivity): Boolean = a1.filePath == a2.filePath

  //def countFileReads(l:List[Element]): Int = {countActivities[ProcessFileActivity](l, _.event==EVENT_READ)}
//  def countFileWrites(l:List[Element]): Int = {countActivities[ProcessFileActivity](l, _.event==EVENT_WRITE)}
//  def countFileActivities(l:List[Element]): Int = {countActivities[ProcessFileActivity](l)}
//  def countNWActivities(l:List[Element]): Int = {countActivities[ProcessNWActivity](l)}
//  def countProcessActivities(l:List[Element]): Int = {countActivities[ProcessActivity](l)}
//  def countUniqueUuids(l:List[ProcessActivity]): Int = {l.map(_.subject.uuid).toSet.size}
  def countFileReads(l:List[Element]): Int = countActivities(l, isFileRead)
  def countFileWrites(l:List[Element]): Int = countActivities(l, isFileWrite)
  def countFileExecs(l:List[Element]): Int = countActivities(l, isFileExec)
  def countFileActivities(l:List[Element]): Int = {countActivities[ProcessFileActivity](l)}

  def countNWActivities(l:List[Element]): Int = {countActivities[ProcessNWActivity](l)}
  def countNWReads(l:List[Element]): Int = countActivities(l, isNWRead)
  def countNWWrites(l:List[Element]): Int = countActivities(l, isNWWrite)

  def countProcessActivities(l:List[Element]): Int = {countActivities[ProcessActivity](l)}
  def countProcessForks(l:List[Element]): Int = {countActivities(l, isProcessFork)}
  def countProcessModifications(l:List[Element]): Int = {countActivities(l, isProcessModify)}

  def countUniqueUuids(l:List[ProcessActivity]): Int = {l.map(_.subject.uuid).toSet.size}

  def getProcessActivities(l:List[Element]): List[ProcessActivity] = filter(l, (_:ProcessActivity)=> true)
  //Sub-Activities
  def getFilesReadActivities(l:List[Element]): List[ProcessFileActivity] = filter(l, isFileRead)
  def getFilesWrittenActivities(l:List[Element]): List[ProcessFileActivity] = filter(l, isFileWrite)
  def getFilesExecActivities(l:List[Element]): List[ProcessFileActivity] = filter(l, isFileExec)
  def getNWReadsActivities(l:List[Element]): List[ProcessNWActivity] = filter(l, isNWRead)
  def getNWWritesActivities(l:List[Element]): List[ProcessNWActivity] = filter(l, isNWWrite)
  def getProcessForkActivities(l:List[Element]): List[ProcessProcessActivity] = filter(l, isProcessFork)
  def getProcessModifyActivities(l:List[Element]): List[ProcessProcessActivity] = filter(l, isProcessModify)

  // Unique predicate objects
  def getUuids(l:List[Element]): Set[UUID] = getProcessActivities(l).map(_.subject.uuid).toSet

  def getFilesRead(l:List[Element]): Set[FilePath] = getFilesReadActivities(l).map(_.filePath).toSet
  def getFilesWritten(l:List[Element]): Set[FilePath] = getFilesWrittenActivities(l).map(_.filePath).toSet
  def getFilesExecuted(l:List[Element]): Set[FilePath] = getFilesExecActivities(l).map(_.filePath).toSet
  def getNWReads(l:List[Element]): Set[NWEndpointRemote] = getNWReadsActivities(l).map(_.neRemote).toSet
  def getNWWrites(l:List[Element]): Set[NWEndpointRemote] = getNWWritesActivities(l).map(_.neRemote).toSet
  def getProcessForks(l:List[Element]): Set[ProcessPath] = getProcessForkActivities(l).map(_.subject2.processPath).toSet
  def getProcessModified(l:List[Element]): Set[ProcessPath] = getProcessModifyActivities(l).map(_.subject2.processPath).toSet


  /*
  def FRA(f:FilePath) = (i:ProcessFileActivity) => i.filePath == f && i.event == EVENT_READ
  def sortedFileReads(l:List[Element]) = {
    val fr = getFilesRead(l)
    val fr_counts = fr.map(f=>(f, countActivities(l, (i:ProcessFileActivity) => i.filePath == f && i.event == EVENT_READ))).toList
    val sorted_frs = fr_counts.sortBy(_._2)
    sorted_frs
  }
*/
  def getNumActivities[A<:Element, B, C<:A:ClassTag](l:Seq[A], s:Set[B], f:(C, B)=>Boolean): List[(B, Int)] = {
    s.map(file=>(file, countActivities(l, (a:C) => f(a, file)))).toList.sortBy(_._2)
  }

  def sortByTime(l:List[ProcessActivity]): List[ProcessActivity] = {
    l.sortBy(_.earliestTimestampNanos.t)
  }

  def prettyPrintSorted[A](s:(A, Int)): String = s"\n\t${s._1}(${s._2})".replace(",","")

//  def getAllEventsBetween(paList:List[ProcessActivity], track:Int=0, acc:List[ProcessActivity]=List.empty):List[ProcessActivity] = paList match {
//    case (head:ProcessNWActivity)::tail if track==0 && head.event == EVENT_RECVFROM => {getAllEventsBetween(tail, 1, head::acc)}
//    case (head:ProcessNWActivity)::tail if track>0 && head.event == EVENT_RECVFROM => {getAllEventsBetween(tail, true, head::acc.tail)}
//
//    case (head:ProcessFileActivity)::tail if track && head.event == EVENT_WRITE => getAllEventsBetween(tail, false, head::acc)
//
//    case head::tail if track => getAllEventsBetween(tail, false, acc.tail)
//    case head::tail if !track => getAllEventsBetween(tail, false, acc)
//
//    case Nil => {if(acc.size%2 == 0) acc else acc.tail}.reverse
//  }

  def collectConsecutive_(paList:List[ProcessActivity], track:Boolean=false, acc:List[ProcessActivity]=List.empty):List[ProcessActivity] = paList match {
    case (head:ProcessNWActivity)::tail if !track && head.event == EVENT_RECVFROM => {collectConsecutive_(tail, true, head::acc)}
    case (head:ProcessNWActivity)::tail if track && head.event == EVENT_RECVFROM => {collectConsecutive_(tail, true, head::acc.tail)}

    case (head:ProcessFileActivity)::tail if track && head.event == EVENT_WRITE => collectConsecutive_(tail, false, head::acc)

    case head::tail if track => collectConsecutive_(tail, false, acc.tail)
    case head::tail if !track => collectConsecutive_(tail, false, acc)

    case Nil => {if(acc.size%2 == 0) acc else acc.tail}.reverse
  }
  def collectConsecutive__[A1<:ProcessActivity,A2<:ProcessActivity](paList:List[ProcessActivity], f1:A1 => Boolean, f2:A2 => Boolean, track:Boolean=false, acc:List[ProcessActivity]=List.empty)(implicit tag1: ClassTag[A1], tag2: ClassTag[A2]):List[ProcessActivity] = {
    //implicit f1, f2, f12
    paList match {
      case (head: A1) :: tail if !track && f1(head) => collectConsecutive__(tail, f1, f2, true, head :: acc)
      case (head: A1) :: tail if track && f1(head) => collectConsecutive__(tail, f1, f2, true, head :: acc.tail)

      case (head: A2) :: tail if track && f2(head) => collectConsecutive__(tail, f1, f2, false, head :: acc)

      case _ :: tail if track => collectConsecutive__(tail, f1, f2,  false, acc.tail)
      case _ :: tail if !track => collectConsecutive__(tail, f1, f2,  false, acc)

      case Nil => {
        if (acc.size % 2 == 0) acc else acc.tail
      }.reverse
    }
  }

  def collectConsecutive[A1<:ProcessActivity,A2<:ProcessActivity](l:List[ProcessActivity], f1:A1 => Boolean, f2:A2 => Boolean, f12:(A1, A2) => Boolean= (_:A1,_:A2)=>true)(implicit tag1: ClassTag[A1], tag2: ClassTag[A2]):List[(A1,A2)] = {
    //implicit f1, f2, f12
    def subFun(paList:List[ProcessActivity], track:Boolean, acc1:List[A1], acc2:List[A2]):List[(A1, A2)] = paList match {
      case (head: A1) :: tail if !track && f1(head) => subFun(tail, true, head::acc1, acc2)
      case (head: A1) :: tail if track && f1(head) => subFun(tail, true, head::acc1.tail, acc2)

      case (head: A2) :: tail if track && f2(head) && f12(acc1.head, head) => subFun(tail, false, acc1, head::acc2)

      case _ :: tail if track => subFun(tail, false, acc1.tail, acc2)
      case _ :: tail if !track => subFun(tail,false, acc1, acc2)

      case Nil => {
        if (acc1.length == acc2.length) acc1 zip acc2 else acc1.tail zip acc2
      }.reverse
    }

    subFun(l, false, List.empty[A1], List.empty[A2])
  }

  def listOfPairs[A](l:List[A]): List[(A, A)] = l.sliding(2,2).map(x => (x.head, x.tail.head)).toList
  def formatActPairs(paPair:(ProcessActivity, ProcessActivity)): String =  paPair match {case (a1, a2) => s"\n ${a1.toStr} => ${a2.toStr}"}

  //  def getAllActivitiesBetween = ???

  def getDirReads = ???
  def get = ???

//Files Read (# of times): ${sortedFileReads(l).map(i=>s"\n\t${i._1.path}(${i._2})").toString.replace(",","")}\n
def readableSummary(l:List[ProcessActivity]): String = {
  if (l.isEmpty) "" else{
    val p = l.head.subject.processPath

    //val uniqueUuids = l.view.map(_.subject.uuid).toSet
    val uniqueUuids_count: Map[UUID, Int] = l.groupBy{ a => a.subject.uuid}.map {case(num, occ) => (num, occ.length)}
    val uniqueUuids = uniqueUuids_count.keySet

    println("start")
    //TODO: How to parameterize groupby?
    val groupedActivities = l.view.groupBy{
      case a if isFileActivity(a) => "FileActivity"
      case a if isNWActivity(a) => "NWActivity"
      case a if isProcessActivity(a) => "ProcessActivity"
      case a if isSrcSinkActivity(a) => "SrcSinkActivity"
    }

    println("first group")

    //TODO: Better solution for typecasting?
    val allFileActivities = groupedActivities.getOrElse("FileActivity", Seq.empty).map{case a:ProcessFileActivity => a}
    val allNWActivities = groupedActivities.getOrElse("NWActivity", Seq.empty).map{case a:ProcessNWActivity=> a}
    val allProcessActivities = groupedActivities.getOrElse("ProcessActivity", Seq.empty).map{case a:ProcessProcessActivity => a}
    val allSrcSinkActivities = groupedActivities.getOrElse("SrcSinkActivity", Seq.empty).map{case a:ProcessSrcSinkActivity => a}


    val groupedFileActivities = allFileActivities.groupBy{
      case a if isFileRead(a) => "FileRead"
      case a if isFileWrite(a) => "FileWrite"
      case a if isFileExec(a) => "FileExec"
      case default => "FileMisc"
    }

    println("second group")
    //TODO: How to parameterize groupby and do all groupings in one traversal?
    val allFileReads = groupedFileActivities.getOrElse("FileRead", Seq.empty)
    println("done`")
    val allFileWrites = groupedFileActivities.getOrElse("FileWrite", Seq.empty)
    println("done1")
    val allFileExecs = groupedFileActivities.getOrElse("FileExec", Seq.empty)
    println("done3")

    val uniqueFilesRead = allFileReads.map(_.filePath).toSet
    println("setop1")
    val uniqueFilesWritten = allFileWrites.map(_.filePath).toSet
    println("setop2")
    val uniqueFilesExecuted = allFileExecs.map(_.filePath).toSet
    println("setop3")

    val groupedNWActivities = allNWActivities.groupBy{
      case a if isNWRead(a) => "NWRead"
      case a if isNWWrite(a) => "NWWrite"
      case default => "NWMisc"
    }
    println("sub NW group")

    val allNWReads = groupedNWActivities.getOrElse("NWRead", Seq.empty)
    val allNWWrites = groupedNWActivities.getOrElse("NWWrite", Seq.empty)

    val uniqueNWReads = allNWReads.map(_.neRemote).toSet
    val uniqueNWWrites = allNWWrites.map(_.neRemote).toSet

    println("sub NW group setop")

    val groupedProcessActivities = allProcessActivities.groupBy{
      case a if isProcessFork(a) => "ProcessFork"
      case a if isProcessModify(a) => "ProcessModify"
      case default => "ProcessMisc"
    }
    println("sub process group")

    val allProcessForks = groupedProcessActivities.getOrElse("ProcessFork", Seq.empty)
    val allProcessModifications = groupedProcessActivities.getOrElse("ProcessModify", Seq.empty)


    val uniqueProcessForks = allProcessForks.map(_.subject2.processPath).toSet
    val uniqueProcessModifications = allProcessModifications.map(_.subject2.processPath).toSet

    println("sub process setop")


    s"""
       |==================================================
       |======= Process Path: ${p.path}
       |==================================================
       |Number of File Activities: ${allFileActivities.length} (Reads: ${allFileReads.length}, Writes: ${allFileWrites.length}, Execs: ${allFileExecs.length})
       |Number of NW Activities: ${allNWActivities.length}(Reads: ${allNWReads.length}, Writes: ${allNWWrites.length})
       |Number of Process Activities: ${allProcessActivities.length}(Reads: ${allProcessForks.length}, Modifications: ${allProcessModifications.length})
       |Number of Src/Sink Activities: ${allSrcSinkActivities.length}
       |Number of UUIDS: ${uniqueUuids.size}
       |UUIDS (# of activities): ${uniqueUuids_count.toList.view.sortBy(_._2).map(prettyPrintSorted)}

       |Files Read (# of activities): ${getNumActivities(allFileReads, uniqueFilesRead, (a: ProcessFileActivity, f: FilePath) => a.filePath == f).map(prettyPrintSorted)}
       |Files Written (# of activities): ${getNumActivities(allFileWrites, uniqueFilesWritten, (a: ProcessFileActivity, f: FilePath) => a.filePath == f).map(prettyPrintSorted)}
       |Files Executed (# of activities): ${getNumActivities(allFileExecs, uniqueFilesExecuted, (a: ProcessFileActivity, f: FilePath) => a.filePath == f).map(prettyPrintSorted)}
       |NW Reads (# of activities): ${getNumActivities(allNWReads, uniqueNWReads, (a: ProcessNWActivity, nr: NWEndpointRemote) => a.neRemote == nr).map(prettyPrintSorted)}
       |NW Writes (# of activities): ${getNumActivities(allNWWrites, uniqueNWWrites, (a: ProcessNWActivity, nr: NWEndpointRemote) => a.neRemote == nr).map(prettyPrintSorted)}
       |Process Forked (# of activities): ${getNumActivities(allProcessForks, uniqueProcessForks, (a: ProcessProcessActivity, p: ProcessPath) => a.subject2.processPath == p).map(prettyPrintSorted)}
       |Process Modified(# of activities): ${getNumActivities(allProcessModifications, uniqueProcessModifications, (a: ProcessProcessActivity, p: ProcessPath) => a.subject2.processPath == p).map(prettyPrintSorted)}

       |NW Reads followed by File Writes (downloads?): ${collectConsecutive(l, isNWRead, isFileWrite).map(formatActPairs)}
       |File Writes followed by File Executes (malicious?): ${collectConsecutive(l, isFileWrite, isFileExec, areTwoFilesSame).map(formatActPairs)}


    """.stripMargin
  }
}


  // NW Reads followed by File Writes (downloads?): ${listOfPairs(collectConsecutive__(l,isNWRead,isFileWrite)).map { case (a1, a2) => s"\n ${a1.toStr} => ${a2.toStr}"; case _ => "" }}
//  UUIDS (# of activities): ${getNumActivities(l,uniqueUuids, (a:ProcessActivity, f:UUID)=>a.subject.uuid == f).map(prettyPrintSorted)}

  def f: (ProcessNWActivity, ProcessNWActivity) => Boolean = ProcessNWReadAndNWWrite.isApplicable
  def isEventAccept(a: ProcessActivity): Boolean = a.event == EVENT_ACCEPT
  def isEventOpen(a: ProcessActivity): Boolean = a.event == EVENT_OPEN
  def isEventOther(a: ProcessActivity): Boolean = a.event == EVENT_OTHER
  def isEventClose(a: ProcessActivity): Boolean = a.event == EVENT_CLOSE
  def isEventExit(a: ProcessActivity): Boolean = a.event == EVENT_EXIT
  def isEventLseek(a: ProcessActivity): Boolean = a.event == EVENT_LSEEK
  def isEventReadOrMmap(e:EventType): Boolean = e == EVENT_READ || e == EVENT_MMAP


  def compress(l:List[ProcessActivity]): List[Element] = {
    l
      .filter(i => !(isEventOpen(i) || isEventAccept(i) || isEventOther(i) || isEventClose(i) || isEventExit(i) || isEventLseek(i)))
      //merge same Consecutive Activities
      .foldRight(List.empty[Element])(genericMerge(_ == _, (a1,a2)=>a1, _, _))
      .foldRight(List.empty[Element])(genericMerge(checkTypesAndCond(NWReadFileWrite.isApplicable, _, _), NWReadFileWrite.apply, _, _))
      .foldRight(List.empty[Element])(genericMerge(checkTypesAndCond(ProcessNWReadAndNWWrite.isApplicable, _, _), ProcessNWReadAndNWWrite.apply, _, _))
      .foldRight(List.empty[Element])(genericMerge(mergeFileReads,FileRead.fromA1A2, _, _))
      .foldRight(List.empty[Element])(genericMerge(mergeSimilarProcessNWReadAndNWWrite, (x1, x2)=>x1, _, _))
  }

  def mergeFileReads(a1:Element, a2:Element): Boolean = {
    def check(pfa1:ProcessFileActivity, pfa2:ProcessFileActivity) = pfa1.subject == pfa2.subject && isEventReadOrMmap(pfa1.event)&& isEventReadOrMmap(pfa2.event)
    (a1, a2) match {
      case (a1: ProcessFileActivity, a2: ProcessFileActivity) if check(a1, a2) => true
      case default => false
    }
  }

  def mergeSimilarProcessNWReadAndNWWrite(a1:Element, a2:Element): Boolean = {
    (a1, a2) match {
      case (a1: ProcessNWReadAndNWWrite, a2: ProcessNWReadAndNWWrite) if a1 similar a2 => true
      case default => false
    }
  }

  def checkTypesAndCond[A1<:Element,A2<:Element](cond: (A1, A2)=>Boolean, a1:Element, a2:Element)(implicit tag1: ClassTag[A1], tag2: ClassTag[A2]): Boolean = (a1,a2) match{
    case (a1: A1, a2:A2) if cond(a1, a2) => true
    case _ => false
  }

  def genericMerge[A<:Element](mergeRule: (Element, Element) => Boolean, transformationRule: (Element, Element) =>A, a1:Element, paList:List[Element]): List[Element] ={
    paList match{
      case a2::tail => if (mergeRule(a1, a2)) transformationRule(a1, a2)::tail else a1::paList
      case Nil => a1::paList
    }
  }


  def readFromNWFollowedByWriteToFile(a1:ProcessActivity, a2:ProcessActivity): Option[NWReadFileWrite] = {
    def check(a1:ProcessActivity, a2:ProcessActivity) = a1.event == EVENT_RECVFROM && a2.event == EVENT_WRITE && a1.subject == a2.subject
    (a1, a2) match {
      case (a1: ProcessNWActivity, a2: ProcessFileActivity) if check(a1, a2) => Some(NWReadFileWrite(a1, a2))
      case default => None
    }
  }


  def mergeConsecutiveSimpleActivitiesIntoNew(binaryMergeRule: (ProcessActivity, ProcessActivity) => Option[Element], a1:ProcessActivity, paList:List[Element]): List[Element] = {
    paList match
    {
      case (a2: ProcessActivity):: tail => binaryMergeRule(a1, a2) match {
        case Some(mergedActivity) => mergedActivity::tail
        case None => a1::paList//List(a1,a2):::tail
      }
      case (a2:Element)::tail => a1::paList
      case Nil => a1 :: paList
      case default => println(default); ???
    }
  }



  def mergeConsecutiveActivities(binaryMergeRule: (ProcessActivity, ProcessActivity) => Boolean = _==_, a:ProcessActivity, paList:List[ProcessActivity]): List[ProcessActivity] = {
    if (paList.nonEmpty && binaryMergeRule(a, paList.head)) paList else a :: paList
  }


  def applyUnaryRule(unaryRule: ProcessActivity => ProcessActivity)(a: ProcessActivity) = {
    unaryRule(a)
  }


  def getTimestamp(a: Element): Long = a match {
    case a: ProcessFileActivity => a.earliestTimestampNanos.t
    case a: ProcessProcessActivity => a.earliestTimestampNanos.t
    case a: ProcessNWActivity => a.earliestTimestampNanos.t
    case a: ProcessSrcSinkActivity => a.earliestTimestampNanos.t
    case default => ???
  }
}



case class FileRead(subject: SubjectProcess, filePaths: List[(FilePath, TimestampNanos)]) extends Element{
}

object FileRead {
  def fromA1A2(a1:Element, a2:Element): FileRead = (a1, a2) match {
    case (a1: ProcessFileActivity, a2: ProcessFileActivity) =>
      new FileRead(a1.subject,
        List((a1.filePath, a1.earliestTimestampNanos),
          (a2.filePath, a2.earliestTimestampNanos)) )
    case default => ???
  }

}


//trait ProcessActivitiesSetOfUptoTwo extends Element



/*EVENT_RECVFROM + EVENT_WRITE+*/
case class ProcessNWReadAndNWWrite(pna1: ProcessNWActivity, pna2: ProcessNWActivity) extends Element{

  def similar (a:ProcessNWReadAndNWWrite): Boolean ={
    pna1.subject == a.pna1.subject &&
      pna1.neLocal == a.pna1.neLocal &&
      pna1.neRemote == a.pna1.neRemote
  }

  override def toString: String = {
    "ProcessNWReadAndNWWrite(" +
      pna1.subject.processPath.toString +
      pna1.subject.toString +
      pna1.neLocal.toString +
      pna1.neRemote.toString +
      pna1.earliestTimestampNanos.toString +
      pna2.earliestTimestampNanos.toString +
      ")"
  }

}

object ProcessNWReadAndNWWrite {
  def apply(pna1: Element, pna2: Element): ProcessNWReadAndNWWrite = {
    (pna1, pna2) match {
      case (pna1:ProcessNWActivity, pna2:ProcessNWActivity) => new ProcessNWReadAndNWWrite(pna1, pna2)
      case default => ???
    }
  }
  def isApplicable(a1: ProcessNWActivity, a2: ProcessNWActivity): Boolean =
    a1.subject == a2.subject && // same process
      a1.neLocal == a2.neLocal && a1.neRemote == a2.neRemote && // same n/w endpoint
      a1.event == EVENT_RECVFROM && a2.event == EVENT_WRITE // Read followed by a write

}

case class NWReadFileWrite(pna1: ProcessNWActivity, pfa2: ProcessFileActivity) extends Element

object NWReadFileWrite{
  def apply(a1:Element, a2:Element): NWReadFileWrite = {
    (a1, a2) match {
      case (a1: ProcessNWActivity, a2: ProcessFileActivity) => new NWReadFileWrite(a1, a2)
      case default => ???
    }
  }

  def isApplicable(pna1:ProcessNWActivity, pfa2:ProcessFileActivity) =
    pna1.event == EVENT_RECVFROM && pfa2.event == EVENT_WRITE && pna1.subject == pfa2.subject

}


case class ProcessActivityList(activities: List[ProcessActivity]) extends Element




/*
package com.galois.adapt

import com.galois.adapt.cdm18.{EVENT_ACCEPT, EVENT_CLOSE, EVENT_EXIT, EVENT_LSEEK, EVENT_MMAP, EVENT_OPEN, EVENT_OTHER, EVENT_READ, EVENT_RECVFROM, EVENT_WRITE, EventType, FileObjectType, SrcSinkType}
import scala.reflect.ClassTag

trait Element
trait ComposedActivity

object SummaryParser {

  def f: (ProcessNWActivity, ProcessNWActivity) => Boolean = ProcessNWReadAndNWWrite.isApplicable

  def isEventAccept(a: ProcessActivity): Boolean = a.event == EVENT_ACCEPT
  def isEventOpen(a: ProcessActivity): Boolean = a.event == EVENT_OPEN
  def isEventOther(a: ProcessActivity): Boolean = a.event == EVENT_OTHER
  def isEventClose(a: ProcessActivity): Boolean = a.event == EVENT_CLOSE
  def isEventExit(a: ProcessActivity): Boolean = a.event == EVENT_EXIT
  def isEventLseek(a: ProcessActivity): Boolean = a.event == EVENT_LSEEK
  def isEventReadOrMmap(e:EventType): Boolean = e == EVENT_READ || e == EVENT_MMAP


  def compress(l:List[ProcessActivity]): List[Element] = {
    l
      .filter(i => !(isEventOpen(i) || isEventAccept(i) || isEventOther(i) || isEventClose(i) || isEventExit(i) || isEventLseek(i)))
      //merge same Consecutive Activities
      .foldRight(List.empty[Element])(genericMerge(_ == _, (a1,a2)=>a1, _, _))
      .foldRight(List.empty[Element])(genericMerge(checkTypesAndCond(NWReadFileWrite.isApplicable, _, _), NWReadFileWrite.apply, _, _))
      .foldRight(List.empty[Element])(genericMerge(checkTypesAndCond(ProcessNWReadAndNWWrite.isApplicable, _, _), ProcessNWReadAndNWWrite.apply, _, _))
      .foldRight(List.empty[Element])(genericMerge(mergeFileReads,FileRead.fromA1A2, _, _))
      .foldRight(List.empty[Element])(genericMerge(mergeSimilarProcessNWReadAndNWWrite, (x1, x2)=>x1, _, _))
  }

  def mergeFileReads(a1:Element, a2:Element): Boolean = {
    def check(pfa1:ProcessFileActivity, pfa2:ProcessFileActivity) = pfa1.subject == pfa2.subject && isEventReadOrMmap(pfa1.event)&& isEventReadOrMmap(pfa2.event)
    (a1, a2) match {
      case (a1: ProcessFileActivity, a2: ProcessFileActivity) if check(a1, a2) => true
      case default => false
    }
  }

  def mergeSimilarProcessNWReadAndNWWrite(a1:Element, a2:Element): Boolean = {
    (a1, a2) match {
      case (a1: ProcessNWReadAndNWWrite, a2: ProcessNWReadAndNWWrite) if a1 similar a2 => true
      case default => false
    }
  }

  def checkTypesAndCond[A1<:Element,A2<:Element](cond: (A1, A2)=>Boolean, a1:Element, a2:Element)(implicit tag1: ClassTag[A1], tag2: ClassTag[A2]): Boolean = (a1,a2) match{
    case (a1: A1, a2:A2) if cond(a1, a2) => true
    case _ => false
  }

  def genericMerge[A<:Element](mergeRule: (Element, Element) => Boolean, transformationRule: (Element, Element) =>A, a1:Element, paList:List[Element]): List[Element] ={
    paList match{
      case a2::tail => if (mergeRule(a1, a2)) transformationRule(a1, a2)::tail else a1::paList
      case Nil => a1::paList
    }
  }


  def readFromNWFollowedByWriteToFile(a1:ProcessActivity, a2:ProcessActivity): Option[NWReadFileWrite] = {
    def check(a1:ProcessActivity, a2:ProcessActivity) = a1.event == EVENT_RECVFROM && a2.event == EVENT_WRITE && a1.subject == a2.subject
    (a1, a2) match {
      case (a1: ProcessNWActivity, a2: ProcessFileActivity) if check(a1, a2) => Some(NWReadFileWrite(a1, a2))
      case default => None
    }
  }


  def mergeConsecutiveSimpleActivitiesIntoNew(binaryMergeRule: (ProcessActivity, ProcessActivity) => Option[Element], a1:ProcessActivity, paList:List[Element]): List[Element] = {
    paList match
    {
      case (a2: ProcessActivity):: tail => binaryMergeRule(a1, a2) match {
        case Some(mergedActivity) => mergedActivity::tail
        case None => a1::paList//List(a1,a2):::tail
      }
      case (a2:Element)::tail => a1::paList
      case Nil => a1 :: paList
      case default => println(default); ???
    }
  }



  def mergeConsecutiveActivities(binaryMergeRule: (ProcessActivity, ProcessActivity) => Boolean = _==_, a:ProcessActivity, paList:List[ProcessActivity]): List[ProcessActivity] = {
    if (paList.nonEmpty && binaryMergeRule(a, paList.head)) paList else a :: paList
  }


  def applyUnaryRule(unaryRule: ProcessActivity => ProcessActivity)(a: ProcessActivity) = {
    unaryRule(a)
  }


  def getTimestamp(a: Element): TimestampNanos = a match {
    case a: ProcessFileActivity => a.earliestTimestampNanos
    case a: ProcessProcessActivity => a.earliestTimestampNanos
    case a: ProcessNWActivity => a.earliestTimestampNanos
    case a: ProcessSrcSinkActivity => a.earliestTimestampNanos
    case default => ???
  }
}



case class FileRead(subject: SubjectProcess, filePaths: List[(FilePath, TimestampNanos)]) extends Element{
}

object FileRead {
  def fromA1A2(a1:Element, a2:Element): FileRead = (a1, a2) match {
    case (a1: ProcessFileActivity, a2: ProcessFileActivity) =>
      new FileRead(a1.subject,
        List((a1.filePath, a1.earliestTimestampNanos),
          (a2.filePath, a2.earliestTimestampNanos)) )
    case default => ???
  }

}


//trait ProcessActivitiesSetOfUptoTwo extends Element



/*EVENT_RECVFROM + EVENT_WRITE+*/
case class ProcessNWReadAndNWWrite(pna1: ProcessNWActivity, pna2: ProcessNWActivity) extends Element{

  def similar (a:ProcessNWReadAndNWWrite): Boolean ={
    pna1.subject == a.pna1.subject &&
      pna1.neLocal == a.pna1.neLocal &&
      pna1.neRemote == a.pna1.neRemote
  }

  override def toString: String = {
    "ProcessNWReadAndNWWrite(" +
      pna1.subject.processPath.toString +
      pna1.subject.toString +
      pna1.neLocal.toString +
      pna1.neRemote.toString +
      pna1.earliestTimestampNanos.toString +
      pna2.earliestTimestampNanos.toString +
      ")"
  }

}

object ProcessNWReadAndNWWrite {
  def apply(pna1: Element, pna2: Element): ProcessNWReadAndNWWrite = {
    (pna1, pna2) match {
      case (pna1:ProcessNWActivity, pna2:ProcessNWActivity) => new ProcessNWReadAndNWWrite(pna1, pna2)
      case default => ???
    }
  }
  def isApplicable(a1: ProcessNWActivity, a2: ProcessNWActivity): Boolean =
    a1.subject == a2.subject && // same process
      a1.neLocal == a2.neLocal && a1.neRemote == a2.neRemote && // same n/w endpoint
      a1.event == EVENT_RECVFROM && a2.event == EVENT_WRITE // Read followed by a write

}

case class NWReadFileWrite(pna1: ProcessNWActivity, pfa2: ProcessFileActivity) extends Element

object NWReadFileWrite{
  def apply(a1:Element, a2:Element): NWReadFileWrite = {
    (a1, a2) match {
      case (a1: ProcessNWActivity, a2: ProcessFileActivity) => new NWReadFileWrite(a1, a2)
      case default => ???
    }
  }

  def isApplicable(pna1:ProcessNWActivity, pfa2:ProcessFileActivity) =
    pna1.event == EVENT_RECVFROM && pfa2.event == EVENT_WRITE && pna1.subject == pfa2.subject

}


case class ProcessActivityList(activities: List[ProcessActivity]) extends Element
*/