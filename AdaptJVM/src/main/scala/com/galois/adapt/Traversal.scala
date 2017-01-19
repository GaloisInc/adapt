package com.galois.adapt

import org.apache.tinkerpop.gremlin.structure.{Edge, Vertex, Graph}
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import java.util.{Collections, Comparator, Iterator}

import scala.collection.JavaConverters._
import scala.collection.immutable.Stream
import scala.util.parsing.combinator._
import scala.util.Try
import scala.language.existentials

object Traversal {

  // Run a 'Traversal' on a 'Graph'
  def run[S,T](traversal: Traversal[S,T], graph: Graph): Try[Stream[T]] = traversal.apply(graph)
  
  // Attempt to parse a traversal from a string
  def apply(input: String): Option[Traversal[_,_]] = {
    
    object Parsers extends JavaTokenParsers {
      // Synonym for untyped traversal
      type Tr = Traversal[_,_]
 
      // These are arguments to the methods called in traversals
      def int: Parser[java.lang.Long] = wholeNumber ^^ { _.toLong }
      def str: Parser[String] = stringLiteral
      def lit: Parser[_] = int | str

      // Since a traversal is recursively defined, it is convenient for parsing to think of suffixes.
      //
      // Remark: we need the type annotation only when parsing the very first disjunct because Scala type
      // inference is stupidly asymmetric.
      def suffix: Parser[Tr => Tr] =
        ( ".has(" ~ str ~ ")"              ^^ { case _~k~_      => Has(_: Tr, k): Tr }
        | ".has(" ~ str ~ "," ~ lit ~ ")"  ^^ { case _~k~_~v~_  => HasValue(_: Tr, k, v) }
        | ".hasLabel(" ~ str ~ ")"         ^^ { case _~l~_      => HasLabel(_: Tr, l) }
        | ".has(label," ~ str ~ ")"        ^^ { case _~l~_      => HasLabel(_: Tr, l) }
        | ".hasId(" ~ int ~ ")"            ^^ { case _~l~_      => HasId(_: Tr, l) }
        | ".and(" ~ repsep(trav,",") ~ ")" ^^ { case _~ts~_     => And(_: Tr, ts) }
        | ".or(" ~ repsep(trav,",") ~ ")"  ^^ { case _~ts~_     => Or(_: Tr, ts) }
        | ".dedup()"                       ^^ { case _          => Dedup(_: Tr) }
        | ".limit(" ~ int ~ ")"            ^^ { case _~i~_      => Limit(_: Tr, i) }
        | ".order()"                       ^^ { case _          => Order(_: Tr) }
        | ".by(" ~ str ~ ")"               ^^ { case _~k~_      => By(_: Tr, k) }
        | ".by(" ~ trav ~ (",incr)" | ")") ^^ { case _~t~_      => ByTraversal(_: Tr, t, true) }
        | ".by(" ~ trav ~ ",decr)"         ^^ { case _~t~_      => ByTraversal(_: Tr, t, false) }
        | ".as(" ~ rep1sep(str,",") ~ ")"  ^^ { case _~s~_      => As(_: Tr, s) }
        | ".values()"                      ^^ { case _          => Values(_: Tr) }
        | ".max()"                         ^^ { case _          => Max(_: Traversal[_,java.lang.Long]) }
        | ".select("~rep1sep(str,",")~")"  ^^ { case _~Seq(s)~_ => Select(_: Tr, s) 
                                                case _~s~_      => SelectMult(_: Tr, s) }
        | ".unfold()"                      ^^ { case _          => Unfold(_: Tr) }
        | (".both()" | ".bothV()")         ^^ { case _          => BothV(_: Traversal[_,Edge]) }
        | (".out()" | ".outV()")           ^^ { case _          => OutV(_: Traversal[_,Edge]) }
        | (".in()" | ".inV()")             ^^ { case _          => InV(_: Traversal[_,Edge]) }
        | ".bothE()"                       ^^ { case _          => BothE(_: Traversal[_,Vertex]) }
        | ".outE()"                        ^^ { case _          => OutE(_: Traversal[_,Vertex]) }
        | ".inE()"                         ^^ { case _          => InE(_: Traversal[_,Vertex]) }
        ).asInstanceOf[Parser[Tr => Tr]]

      // Possible sources for traversals
      def source: Parser[Tr] = 
        ( "g.V(" ~ repsep(int,",") ~ ")"   ^^ { case _~ids~_    => Vertices(ids) }
        | "g.E(" ~ repsep(int,",") ~ ")"   ^^ { case _~ids~_    => Edges(ids) }
        | "_"                              ^^ { case _          => Anon(List()) }
        | "_(" ~ repsep(int,",") ~ ")"     ^^ { case _~ids~_    => Anon(ids) }
        ).asInstanceOf[Parser[Tr]]

      // Parser for a traversal 
      def trav: Parser[Tr] = source ~ rep(suffix) ^^ {
          case src ~ sufs => sufs.foldLeft[Tr](src)((t,suf) => suf(t))
        }
    }
 
    Parsers.parse(Parsers.trav, input.filterNot(_.isWhitespace)) match {
      case Parsers.Success(matched, _) => Some(matched)
      case _ => None
    }
  }
}

// Represents a traversal across a graph where 'S' is the source type and 'T' the destination type
sealed trait Traversal[S,T] {
  // Convert into a Gremlin traversal by "running" the current traversal on the graph passed in.
  def buildTraversal(graph: Graph): GraphTraversal[S,T] 

  // Run our traversal on a graph.
  def apply(graph: Graph): Try[Stream[T]] = Try { buildTraversal(graph).asScala.toStream }
}


// Sources of traversals
case class Vertices(ids: Seq[java.lang.Long]) extends Traversal[Vertex,Vertex] {
  override def buildTraversal(graph: Graph) = graph.traversal().V(ids: _*)
}
case class Edges(ids: Seq[java.lang.Long]) extends Traversal[Edge,Edge] {
  override def buildTraversal(graph: Graph) = graph.traversal().E(ids: _*)
}
case class Anon[A](starts: Seq[A]) extends Traversal[A,A] {
  override def buildTraversal(graph: Graph) = __.__(starts: _*)
}


// Filters of traversals
case class Has[S,T](traversal: Traversal[S,T], key: String) extends Traversal[S,T] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).has(key) 
}
case class HasValue[S,T,V](traversal: Traversal[S,T], k: String, v: V) extends Traversal[S,T] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).has(k,v)
}
case class HasLabel[S,T](traversal: Traversal[S,T], label: String) extends Traversal[S,T] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).hasLabel(label)
}
case class HasId[S,T](traversal: Traversal[S,T], id: java.lang.Long) extends Traversal[S,T] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).hasId(id)
}
case class And[S,T](traversal: Traversal[S,T], anded: Seq[Traversal[_,_]]) extends Traversal[S,T] {
  override def buildTraversal(graph: Graph) =
    traversal.buildTraversal(graph).and(anded.map(_.buildTraversal(graph)): _*)
}
case class Or[S,T](traversal: Traversal[S,T], ored: Seq[Traversal[_,_]]) extends Traversal[S,T] {
  override def buildTraversal(graph: Graph) =
    traversal.buildTraversal(graph).or(ored.map(_.buildTraversal(graph)): _*)
}
case class Dedup[S,T](traversal: Traversal[S,T]) extends Traversal[S,T] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).dedup()
}
case class Limit[S,T](traversal: Traversal[S,T], lim: java.lang.Long) extends Traversal[S,T] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).limit(lim)
}


// Indirect
case class Order[S,T](traversal: Traversal[S,T]) extends Traversal[S,T] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).order()
}
case class By[S,T](traversal: Traversal[S,T], key: String) extends Traversal[S,T] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).by(key)
}
case class ByTraversal[S,T](traversal: Traversal[S,T], comp: Traversal[_,_], incr: Boolean) extends Traversal[S,T] {
  override def buildTraversal(graph: Graph) = {
    val incrComp = new Comparator[java.lang.Long] {
      override def compare(x1: java.lang.Long, x2: java.lang.Long): Int = x1.compareTo(x2)
    }
    traversal.buildTraversal(graph).by(
      comp.buildTraversal(graph),
      if (incr) incrComp else Collections.reverseOrder(incrComp)
    )
  }
}
case class As[S,T](traversal: Traversal[S,T], labels: Seq[String]) extends Traversal[S,T] {
  override def buildTraversal(graph: Graph) =
    traversal.buildTraversal(graph).as(labels(0), labels.drop(1): _*)
}


// Reduce/extract
case class Values[S,T,V](traversal: Traversal[S,T]) extends Traversal[S,V] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).values()
}
case class Max[S](traversal: Traversal[S,java.lang.Long]) extends Traversal[S,java.lang.Long] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).max()
}
case class Select[S,T](traversal: Traversal[S,_], key: String) extends Traversal[S,T] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).select(key)
}
case class SelectMult[S,T](traversal: Traversal[S,_], keys: Seq[String]) extends Traversal[S,java.util.Map[String,T]] {
  override def buildTraversal(graph: Graph)
    = traversal.buildTraversal(graph).select(keys(0), keys(1), keys.drop(2): _*)
}
case class Unfold[S,T](traversal: Traversal[S,_]) extends Traversal[S,T] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).unfold()
}


// Extend outwards
case class BothV[S](traversal: Traversal[S,Edge]) extends Traversal[S,Vertex] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).bothV()
}
case class OutV[S](traversal: Traversal[S,Edge]) extends Traversal[S,Vertex] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).outV()
}
case class InV[S](traversal: Traversal[S,Edge]) extends Traversal[S,Vertex] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).inV()
}
case class BothE[S](traversal: Traversal[S,Vertex]) extends Traversal[S,Edge] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).bothE()
}
case class OutE[S](traversal: Traversal[S,Vertex]) extends Traversal[S,Edge] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).outE()
}
case class InE[S](traversal: Traversal[S,Vertex]) extends Traversal[S,Edge] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).inE()
}

