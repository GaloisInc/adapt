package com.galois.adapt

import org.apache.tinkerpop.gremlin.structure.{Edge, Vertex, Graph}
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import java.util.{Collections, Comparator, Iterator}

import scala.collection.JavaConverters._
import scala.collection.immutable.Stream
import scala.util.parsing.combinator._
import scala.util.{Try, Failure, Success}
import scala.language.existentials

/*
 * A 'Traversal' represents roughly the shape of path(s) that can be used to explore a graph. This
 * class exists because:
 *
 *   - We need some way of going from strings to queries. Since Groovy reflection wasn't working
 *     well, we decided to roll our own parsing
 *   
 *   - The Java datatypes for this (mostly under 'tinkerpop.gremlin.process.traversal.dsl.graph')
 *     are unpleasant to handle. For instance, they tie traversals to immediately to a graph.
 *   
 *   - This could be a good first-class representation of a mutation to execute on a graph (for
 *     example to add vertices or edges), but without being opaque.
 *
 * Currently, the informal grammar of the supported subset of Java/Gremlin/Groovy is:
 *
 *   variable    ::= <same as Java variables>
 *
 *   long        ::= variable | <same as Java long>
 *   string      ::= variable | <same as Java string>
 *   longArray   ::= variable | '[' long ',' ... ']'
 *   stringArray ::= variable | '[' string ',' ... ']'
 *
 *   literal     ::= long | string | longArray | stringArray
 *
 *   traversal   ::= 'g.V(' long ',' ... ')'
 *                 | 'g.V(' longArray ')'
 *                 | 'g.E(' long ',' ... ')'
 *                 | 'g.E(' longArray ')'
 *                 | '_'
 *                 | '_(' long ',' ... ')'                                 TODO: What is this?
 *                 | traversal '.has(' string ')'
 *                 | traversal '.has(' string ',' literal ')'
 *                 | traversal '.hasLabel(' string ')'
 *                 | traversal '.has(label,' string ')'
 *                 | traversal '.hasId(' long ')'
 *                 | traversal '.and(' traversal ',' ... ')'
 *                 | traversal '.or(' traversal ',' ... ')'
 *                 | traversal '.dedup()'
 *                 | traversal '.limit(' long ')'
 *                 | traversal '.is(' literal ')'
 *                 | traversal '.order()'
 *                 | traversal '.by(' string ')'
 *                 | traversal '.by(' traversal ( ',incr' | ',decr' )? ')'
 *                 | traversal '.as(' string ',' ... ')'
 *                 | traversal '.until(' traveral ')'
 *                 | traversal '.values(' string ',' ... ')'
 *                 | traversal '.max()'
 *                 | traversal '.select(' string ',' ... ')'
 *                 | traversal '.unfold()'
 *                 | traversal '.count()'
 *                 | traversal ( '.both()' | '.bothV()' )
 *                 | traversal ( '.out()' | '.outV()' )
 *                 | traversal ( '.in()' | '.inV()' )
 *                 | traversal '.bothE()'
 *                 | traversal '.outE()'
 *                 | traversal '.inE()'
 *                 | traversal '.repeat(' traversal ')'
 *                 | traversal '.union(' traversal ',' ... ')'
 *                 | traversal '.local(' trav ')'
 *
 *   assignment  ::= variable '=' literal ';'
 *
 *   query       ::= assignment* traversal
 *
 * The top-level things you run against graphs are queries.
 *
 * Remark: in reality, a slightly larger superset of this grammar is supported to allow some gremlin
 * variants too. For example, `__` is accepted as the same as `_`.
 *
 * Relevant links:
 *
 *   [0] http://tinkerpop.apache.org/javadocs/3.2.2/full/org/apache/tinkerpop/gremlin/structure/Graph.html
 *   [1] http://tinkerpop.apache.org/javadocs/3.2.2/full/org/apache/tinkerpop/gremlin/process/traversal/dsl/graph/__.html
 *   [2] http://tinkerpop.apache.org/javadocs/3.2.2/full/org/apache/tinkerpop/gremlin/process/traversal/dsl/graph/GraphTraversal.html
 */


object Traversal {
  // Run a 'Traversal' on a 'Graph'
  def run[S,T](traversal: Traversal[S,T], graph: Graph): Try[Stream[T]] = traversal.run(graph)
  def run[T](traversal: String, graph: Graph): Try[Stream[T]] =
    Traversal(traversal).flatMap(_.run(graph)).flatMap(t => Try(t.asInstanceOf[Stream[T]]))
  
  // Attempt to parse a traversal from a string
  def apply(input: String): Try[Traversal[_,_]] = {

    object Parsers extends JavaTokenParsers with RegexParsers {
      // Synonym for untyped traversal
      type Tr = Traversal[_,_]
     
      // These are the stores of variables defined. 
      // Remark: this really should be one dependently-typed map
      // Remark: if Scala's parser was a monad transformer instead of just a monad, we should thread
      // this state through it.
      var ints: Map[String,java.lang.Long] = Map()
      var strs: Map[String,String] = Map()
      var intArrs: Map[String,Seq[java.lang.Long]] = Map()
      var strArrs: Map[String,Seq[String]] = Map()

      // These are arguments to the methods called in traversals
      def int: Parser[java.lang.Long] = variable(ints) | wholeNumber ^^ { _.toLong }
      def str: Parser[String] = variable(strs) | stringLiteral | "\'" ~ "[^\']*".r ~ "\'" ^^ { case _~s~_ => s }
      def intArr: Parser[Seq[java.lang.Long]] = variable(intArrs) | arr(int)
      def strArr: Parser[Seq[String]] = variable(strArrs) | arr(str)
      def lit: Parser[_] = int | str | intArr | strArr

      // Parse an identifier iff it is in the store passed in.
      def variable[T](store: Map[String,T]): Parser[T] = ident.withFilter(store.isDefinedAt(_)).map(store(_))
      // Parse an array.
      def arr[T](elem: Parser[T]): Parser[Seq[T]] =
        "["~repsep(elem,",")~"]"~".toArray()".? ^^ { case _~e~_~_ => e }

      // Since a traversal is recursively defined, it is convenient for parsing to think of suffixes.
      // Otherwise, we get left recursion...
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
        | ".is(" ~ lit ~ ")"               ^^ { case _~l~_      => Is(_: Tr, l) }
        | ".order()"                       ^^ { case _          => Order(_: Tr) }
        | ".by(" ~ str ~ ")"               ^^ { case _~k~_      => By(_: Tr, k) }
        | ".by(" ~ trav ~ (",incr)" | ")") ^^ { case _~t~_      => ByTraversal(_: Tr, t, true) }
        | ".by(" ~ trav ~ ",decr)"         ^^ { case _~t~_      => ByTraversal(_: Tr, t, false) }
        | ".as(" ~ rep1sep(str,",") ~ ")"  ^^ { case _~s~_      => As(_: Tr, s) }
        | ".until(" ~ trav ~ ")"           ^^ { case _~t~_      => Until(_: Tr, t) }
        | ".values("~rep1sep(str,",")~")"  ^^ { case _~s~_      => Values(_: Tr, s) }
        | ".max()"                         ^^ { case _          => Max(_: Traversal[_,java.lang.Long]) }
        | ".select("~rep1sep(str,",")~")"  ^^ { case _~Seq(s)~_ => Select(_: Tr, s) 
                                                case _~s~_      => SelectMult(_: Tr, s) }
        | ".unfold()"                      ^^ { case _          => Unfold(_: Tr) }
        | ".count()"                       ^^ { case _          => Count(_: Tr) }
        | ".both()"                        ^^ { case _          => Both(_: Traversal[_,Vertex]) }
        | ".bothV()"                       ^^ { case _          => BothV(_: Traversal[_,Edge]) }
        | (".out()" | ".outV()")           ^^ { case _          => OutV(_: Traversal[_,Edge]) }
        | (".in()" | ".inV()")             ^^ { case _          => InV(_: Traversal[_,Edge]) }
        | ".bothE()"                       ^^ { case _          => BothE(_: Traversal[_,Vertex]) }
        | ".outE()"                        ^^ { case _          => OutE(_: Traversal[_,Vertex]) }
        | ".inE()"                         ^^ { case _          => InE(_: Traversal[_,Vertex]) }
        | ".repeat(" ~ trav ~ ")"          ^^ { case _~t~_      => Repeat(_: Traversal[_,Edge], t.asInstanceOf[Traversal[_,Edge]]) }
        | ".union(" ~repsep(trav,",")~ ")" ^^ { case _~t~_      => Union(_: Tr, t.asInstanceOf[Seq[Traversal[_,A]] forSome { type A }]) }
        | ".local(" ~ trav ~ ")"           ^^ { case _~t~_      => Local(_: Tr, t) }
        ).asInstanceOf[Parser[Tr => Tr]]

      // Possible sources for traversals
      def source: Parser[Tr] = 
        ( "g.V(" ~ (intArr | repsep(int,",")) ~ ")" ^^ { case _~ids~_ => Vertices(ids) }
        | "g.E(" ~ (intArr | repsep(int,",")) ~ ")" ^^ { case _~ids~_ => Edges(ids) }
        | ("_" | "__")                              ^^ { case _       => Anon(List()) }
        | "_(" ~ repsep(int,",") ~ ")"              ^^ { case _~ids~_ => Anon(ids) }
        ).asInstanceOf[Parser[Tr]]

      // Parser for assignment statement. This parser has the side-effect of updating the stores of
      // variables.
      def assign: Parser[Unit] =
        ( ident ~ "=" ~ int    ^^ { case i~_~v => ints += (i -> v); }
        | ident ~ "=" ~ str    ^^ { case i~_~v => strs += (i -> v); }
        | ident ~ "=" ~ intArr ^^ { case i~_~v => intArrs += (i -> v); }
        | ident ~ "=" ~ strArr ^^ { case i~_~v => strArrs += (i -> v); }
        )
      
      // Parser for a traversal 
      def trav: Parser[Tr] = rep(assign ~ ";") ~ source ~ rep(suffix) ^^ {
          case _ ~ src ~ sufs => sufs.foldLeft[Tr](src)((t,suf) => suf(t))
        }
    }
 
    Parsers.parseAll(Parsers.trav, input.filterNot(_.isWhitespace)) match {
      case Parsers.Success(matched, _) => Success(matched)
      case Parsers.Error(msg, in) => Failure(throw new Exception(s"At ${in.pos}: $msg"))
      case Parsers.Failure(msg, in) => Failure(throw new Exception(s"At ${in.pos}: $msg"))
      case _ => Failure(throw new Exception("Parser failed in an unexpected way"))
    }
  }
}


// Represents a traversal across a graph where 'S' is the source type and 'T' the destination type
sealed trait Traversal[S,T] { 
  // Convert into a Gremlin traversal by "running" the current traversal on the graph passed in.
  def buildTraversal(graph: Graph): GraphTraversal[S,T] 

  // Run our traversal on a graph.
  def run(graph: Graph): Try[Stream[T]] = Try { buildTraversal(graph).asScala.toStream }
  def apply(graph: Graph): Try[Stream[T]] = run(graph)
}


// Sources of traversals
// TODO: Maybe we should reconsider what the first generic parameter in 'Vertices' should do. We
// could change it such that it is the _possible_ starting types. It would be '_' for Vertices and
// Edges (since those don't need to assume anything), but still 'A' for Anon (since it depends on
// what immediately preceded it).
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
case class Is[S,T](traversal: Traversal[S,T], value: Any) extends Traversal[S,T] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).is(value)
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
case class Until[S,E](traversal: Traversal[S,E], cond: Traversal[_,_]) extends Traversal[S,E] {
  override def buildTraversal(graph: Graph)
    = traversal.buildTraversal(graph).until(cond.buildTraversal(graph))
}


// Reduce/extract
case class Values[S,T,V](traversal: Traversal[S,T], keys: Seq[String]) extends Traversal[S,V] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).values(keys: _*)
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
case class Count[S](traversal: Traversal[S,_]) extends Traversal[S,java.lang.Long] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).count()
}


// Extend outwards
case class Both[S](traversal: Traversal[S,Vertex]) extends Traversal[S,Vertex] {
  override def buildTraversal(graph: Graph) = traversal.buildTraversal(graph).both()
}
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
case class Repeat[S](traversal: Traversal[S,Edge], rep: Traversal[_,Edge]) extends Traversal[S,Edge]{
  override def buildTraversal(graph: Graph)
    = traversal.buildTraversal(graph).repeat(rep.buildTraversal(graph))
}
case class Union[S,E](traversal: Traversal[S,_], unioned: Seq[Traversal[_,E]]) extends Traversal[S,E] {
  override def buildTraversal(graph: Graph)
    = traversal.buildTraversal(graph).union(unioned.map(_.buildTraversal(graph)): _*)
}
case class Local[S,E](traversal: Traversal[S,_], loc: Traversal[_,E]) extends Traversal[S,E] {
  override def buildTraversal(graph: Graph)
    = traversal.buildTraversal(graph).local(loc.buildTraversal(graph))
}

