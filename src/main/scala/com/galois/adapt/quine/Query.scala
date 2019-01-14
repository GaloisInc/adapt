package com.galois.adapt.quine

import java.util
import java.util.function.BiPredicate
import java.util.{Collections, Comparator, Iterator}

import akka.util.Timeout
import com.rrwright.quine.language.EdgeDirections.{EdgeDirValue, Outgoing}
import com.rrwright.quine.language.{EdgeDirections, PickleReader, PickleScheme, QuineId}
import com.rrwright.quine.language.PicklingOps._
import com.rrwright.quine.runtime.GraphService

import scala.collection.JavaConverters._
import scala.collection.immutable.Stream
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.parsing.combinator._
import scala.util.{Failure, Success, Try}
import scala.language.existentials
import scala.reflect.ClassTag
import scala.util.matching.Regex

/*
 * A 'Traversal' represents roughly the shape of path(s) that can be used to explore a graph. This
 * class exists because:
 *
 *   - We need some way of going from strings to queries. Since Groovy reflection wasn't working
 *     well, we decided to roll our own DSL, then interpret that to using the Java tinkerpop API.
 *
 *   - The Java datatypes for this (mostly under 'tinkerpop.gremlin.process.traversal.dsl.graph')
 *     are unpleasant to handle. For instance, they tie traversals immediately to a graph.
 *
 *   - This could be a good first-class representation of a mutation to execute on a graph (for
 *     example to add vertices or edges), but without being opaque.
 *
 * Currently, the informal grammar of the supported subset of Java/Gremlin/Groovy is:
 *
 *   variable    ::= <same as Java variables>
 *
 *   int         ::= variable | <same as java int, but with "I" suffix>
 *   long        ::= variable | <same as java long, but with "L" suffix> | <same as Java long>
 *   string      ::= variable | <same as Java string>
 *   uuid        ::= variable | <same as toString of Java UUID>
 *
 *   intArray    ::= variable | '[' int ',' ... ']'
 *   longArray   ::= variable | '[' long ',' ... ']'
 *   stringArray ::= variable | '[' string ',' ... ']'
 *   uuidArray   ::= variable | '[' uuid ',' ... ']'
 *
 *   literal     ::= int | long | string | uuid | intArray | longArray | stringArray | uuidArray
 *
 *   predicate   ::= 'eq(' literal ')'
 *                 | 'neq(' literal ')'
 *                 | 'within([' literal ',' ... '])'
 *                 | 'lte(' literal ')'
 *                 | 'gte(' literal ')'
 *                 | 'between(' literal ',' literal ')'
 *                 | 'regex(' string ')'
 *
 *   traversal   ::= 'g.V(' long ',' ... ')'
 *                 | 'g.V(' longArray ')'
 *                 | 'g.E(' long ',' ... ')'
 *                 | 'g.E(' longArray ')'
 *                 | '_'
 *                 | '_(' long ',' ... ')'                                 TODO: What is this?
 *                 | traversal '.has(' string ')'
 *                 | traversal '.has(' string ',' literal ')'
 *                 | traversal '.has(' string ',' traversal ')'
 *                 | traversal '.has(' string ',' predicate ')'
 *                 | traversal '.hasLabel(' string ')'
 *                 | traversal '.has(label,' string ')'
 *                 | traversal '.hasId(' long ')'
 *                 | traversal '.hasNot(' string ')'
 *                 | traversal '.where(' traversal ')'
 *                 | traversal '.where(' predicate ')'
 *                 | traversal '.and(' traversal ',' ... ')'
 *                 | traversal '.or(' traversal ',' ... ')'
 *                 | traversal '.not(' traversal ')'
 *                 | traversal '.dedup()'
 *                 | traversal '.limit(' long ')'
 *                 | traversal '.is(' literal ')'
 *                 | traversal '.is(' predicate ')'
 *                 | traversal '.simplePath()'
 *                 | traversal '.times(' int ')'
 *                 | traversal '.order()'
 *                 | traversal '.by(' string ')'
 *                 | traversal '.by(id)'
 *                 | traversal '.by(key)'
 *                 | traversal '.by(label)'
 *                 | traversal '.by(value)'
 *                 | traversal '.by(' traversal ( ',incr' | ',decr' )? ')'
 *                 | traversal '.as(' string ',' ... ')'
 *                 | traversal '.until(' traveral ')'
 *                 | traversal '.values(' string ',' ... ')'
 *                 | traversal '.valueMap(' string ',' ... ')'
 *                 | traversal '.label()'
 *                 | traversal '.id()'
 *                 | traversal '.emit()'
 *                 | traversal '.emit(' traversal ')'
 *                 | traversal '.max()'
 *                 | traversal '.min()'
 *                 | traversal '.sum()'
 *                 | traversal '.select(' string ',' ... ')'
 *                 | traversal '.choose(' traversal ')'
 *                 | traversal '.option(' literal ',' traversal ')'
 *                 | traversal '.fold()'
 *                 | traversal '.unfold()'
 *                 | traversal '.count()'
 *                 | traversal '.groupCount()'
 *                 | traversal '.match(' traversal ',' ... ')'
 *                 | traversal '.both(' string ',' ... ')'
 *                 | traversal '.out(' string ',' ... ')'
 *                 | traversal '.in(' string ',' ... ')'
 *                 | traversal '.bothV()'
 *                 | traversal '.outV()'
 *                 | traversal '.inV()'
 *                 | traversal '.bothE(' string ',' ... ')'
 *                 | traversal '.outE(' string ',' ... ')'
 *                 | traversal '.inE(' string ',' ... ')'
 *                 | traversal '.repeat(' traversal ')'
 *                 | traversal '.union(' traversal ',' ... ')'
 *                 | traversal '.local(' traversal ')'
 *                 | traversal '.property(' string ',' literal ',' ... ')'
 *                 | traversal '.properties(' string ',' ... ')'
 *                 | traversal '.path()'
 *                 | traversal '.unrollPath()'
 *
 *   query     ::= ( ident '=' literal ';'
 *                 | ident '=' traversal ';'
 *                 | traversal ';'
 *                 )* traversal
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


object Query {
  import com.galois.adapt.AdaptConfig._
  import QueryLanguage._

  // Run a 'Traversal' on a 'Graph'
  def run[T](query: Query[T], graph: GraphService)(implicit ec: ExecutionContext): Future[IndexedSeq[T]] = query.run(graph)
  def run[T](query: String, graph: GraphService, namespaces: mutable.Set[String])(implicit ec: ExecutionContext): Future[IndexedSeq[T]] =
    Query(query, namespaces) match {
      case Success(s) => s.run(graph).map(_.map(_.asInstanceOf[T])) // cast cuz we went from "String"-ly typed to "T"
      case Failure(f) => Future.failed(f)
    }

  // Attempt to parse a traversal from a string
  def apply(input: String, namespaces: mutable.Set[String])(implicit ec: ExecutionContext): Try[Query[_]] = {

    object Parsers extends JavaTokenParsers with RegexParsers {
      // Synonym for untyped traversal and query
      type Tr = Traversal[_,_]
      type Qy = Query[_]
      type P[T] = T => Boolean

      // These are the stores of variables defined.
      // Remark: this really should be one dependently-typed map
      // Remark: if Scala's parser was a monad transformer instead of just a monad, we should thread
      // this state through it.
      var ints: Set[String] = Set()
      var lngs: Set[String] = Set()
      var strs: Set[String] = Set()
      var uids: Set[String] = Set()
      var arrs: Set[String] = Set()


      // These are arguments to the methods called in traversals
      def lng: Parser[QueryValue[java.lang.Long]] =
          ( variable(lngs)
          | wholeNumber ~ "L" ^^ { case x~_ => Raw(x.toLong) }
          | wholeNumber       ^^ { x => Raw(x.toLong) }
          ).asInstanceOf[Parser[QueryValue[java.lang.Long]]]
          .withFailureMessage("long expected (ex: x, 1L, 1)")

      def int: Parser[QueryValue[Int]] =
          ( variable[Int](ints)
          | wholeNumber ~ "I" ^^ { case x~_ => Raw(x.toInt) }
          ).withFailureMessage("int expected (ex: x, 1I)")

      def str: Parser[QueryValue[String]] =
          ( variable(strs)
          | stringLiteral             ^^ { case s => Raw(StringContext.treatEscapes(s.stripPrefix("\"").stripSuffix("\""))) }
          | "\'" ~! "[^\']*".r ~ "\'" ^^ { case _~s~_ => Raw(s) }
          ).withFailureMessage("string expected (ex: x, \"hello\", 'hello')")

      // NOTE: this is no longer exactly a UUID - it is a franken-UUID supporting namespaces.
      // For example, it should accept `cdm_cadets_123e4567-e89b-12d3-a456-426655440000`.
      //
      // We make one parser level exceptions around this: `has('uuid',<uuid-lit>)`
      def uuid: Parser[QueryValue[java.util.UUID]] = ( variable(uids) |
        """([0-9a-zA-Z-]*_)?[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA_F]{4}-[0-9a-fA_F]{4}-[0-9a-fA_F]{12}""".r ^^ {
          s => Raw(java.util.UUID.fromString(s))
        }).withFailureMessage("UUID expected (ex: x, 123e4567-e89b-12d3-a456-426655440000)")

      def uuidArr: Parser[QueryValue[Seq[java.util.UUID]]] =
          ( variable(arrs) | arr(uuid) ).withFailureMessage("UUID array expected")

      def lngArr: Parser[QueryValue[Seq[java.lang.Long]]] =
          ( variable(arrs) | arr(lng) ).withFailureMessage("long array expected")

      def intArr: Parser[QueryValue[Seq[Int]]] =
          ( variable(arrs) | arr(int) ).withFailureMessage("int array expected")

      def strArr: Parser[QueryValue[Seq[String]]] =
          ( variable(arrs) | arr(str) ).withFailureMessage("string array expected")

      def lit: Parser[QueryValue[_]] =
          ( uuid ||| int ||| lng ||| str
          ||| intArr ||| lngArr ||| uuidArr ||| strArr
          ).withFailureMessage("literal expected (ex: 9, 12I, \"hello\", [1,2,3])")

      def pred: Parser[QueryValue[P[Any]]] =
          (   predicate(uuid)
          ||| predicate(int)
          ||| predicate(lng)
          ||| strPredicate
          ).asInstanceOf[Parser[QueryValue[P[Any]]]]
          .withFailureMessage("predicate expected (ex: within([1,2,3]), eq(x))")

      // Parse an identifier iff it is in the store passed in.
      //def variable[T](store: Set[String]): Parser[Value[T]] = ident.map(Variable(_))
      def variable[T](store: Set[String]): Parser[QueryValue[T]] =
        ident.withFilter(store.contains(_)).map(Variable(_)).withFailureMessage("expected variable")

      // Parse an array.
      def arr[T](elem: Parser[QueryValue[T]])(implicit ev: scala.reflect.ClassTag[T]): Parser[QueryValue[Seq[T]]] =
          ( "[" ~ repsep(elem,",") ~ "]" ~ ".toArray()".? ^^ { case _~e~_~_ => RawArr(e) }
          ).withFailureMessage(s"array of ${ev.runtimeClass.getSimpleName()} expected")

      def predicate[T](elem: Parser[QueryValue[T]])(implicit ev: scala.reflect.ClassTag[T]): Parser[QueryValue[P[T]]] =
          ( "eq(" ~ elem ~ ")"                     ^^ { case _~l~_     => RawEqPred(l) }
          | "neq(" ~ elem ~ ")"                    ^^ { case _~l~_     => RawNeqPred(l) }
          | "within(" ~ variable(arrs) ~ ")"       ^^ { case _~v~_     => RawWithinPred(v) }
          | "within(" ~ arr(elem) ~ ")"            ^^ { case _~l~_     => RawWithinPred(l) }
          ).asInstanceOf[Parser[QueryValue[P[T]]]]
          .withFailureMessage(s"predicate of type ${ev.runtimeClass.getSimpleName()} expected")

      def strPredicate: Parser[QueryValue[P[String]]] =
          ( predicate(str)
          | "regex(" ~ str ~ ")"                   ^^ { case _~s~_     => RawRegexPred(s) }
          ).withFailureMessage(s"predicate of type String expected")

      // Since a traversal is recursively defined, it is convenient for parsing to think of suffixes.
      // Otherwise, we get left recursion...
      //
      // Remark: we need the type annotation only when parsing the very first disjunct because Scala type
      // inference is stupidly asymmetric.
      def travSuffix: Parser[Traversal[_,_]] =
          ( ".has(" ~ str ~ ")"              ^^ { case _~k~_      => Has(k) }
          | ".has(" ~ ("'uuid'"|"\"uuid\"") ~ "," ~ lit ~ ")" ^^ {  ???  }
          | ".has(" ~ ("'uuid'"|"\"uuid\"") ~ "," ~ pred ~ ")" ^^ { ??? }
          | ".has(" ~ str ~ "," ~ lit ~ ")"  ^^ { case _~k~_~v~_  => HasValue(k, v) }
          | ".has(" ~ str ~ "," ~ pred ~ ")" ^^ { case _~l~_~p~_  => HasPredicate(l, p) }
          | ".hasNot(" ~! str ~ ")"          ^^ { case _~s~_      => HasNot(s) }
          | ".hasId(" ~! uuid ~ ")"          ^^ { case _~l~_      => HasId(l) }
          | ".where(" ~ trav ~ ")"           ^^ { case _~t~_      => Where(t) }
          | ".and(" ~! repsep(trav,",")~ ")" ^^ { case _~ts~_     => And(ts) }
          | ".or(" ~! repsep(trav,",")~ ")"  ^^ { case _~ts~_     => Or(ts) }
          | ".not(" ~! trav ~ ")"            ^^ { case _~t~_      => Not(t) }
          | ".dedup()"                       ^^ {      _          => Dedup }
          | ".limit(" ~! lng ~ ")"           ^^ { case _~i~_      => Limit(i) }
          | ".is(" ~ pred ~ ")"              ^^ { case _~p~_      => IsPredicate(p) }
          | ".is(" ~ lit ~ ")"               ^^ { case _~l~_      => Is(l) }
          | ".order().by(" ~ str ~ ")"       ^^ { case _~k~_      => OrderBy(k) }
          | ".as(" ~! str ~ ")"              ^^ { case _~s~_      => As(s) }
          | ".values(" ~! rep1sep(str,",")~")"    ^^ { case _~s~_     => Values(RawArr(s)) }
          | ".valueMap(" ~! repsep(str,",")~")"   ^^ { case _~s~_     => ValueMap(RawArr(s)) }
          | ".max()"                         ^^ {      _          => Max }
          | ".min()"                         ^^ {      _          => Min }
          | ".sum()"                         ^^ {      _          => Sum }
          | ".select("~!rep1sep(str,",")~")" ^^ { case _~Seq(s)~_ => Select(s)
                                                  case _~s~_      => SelectMult(RawArr(s))
                                                }
          | ".count()"                       ^^ {      _          => Count }
          | ".groupCount()"                  ^^ {      _          => GroupCount() }
          | ".both(" ~! repsep(str,",")~ ")" ^^ { case _~s~_      => Both(RawArr(s)) }
          | ".bothV()"                       ^^ {      _          => BothV }
          | ".out(" ~! repsep(str,",")~ ")"  ^^ { case _~s~_      => Out(RawArr(s)) }
          | ".outV()"                        ^^ {      _          => OutV }
          | ".in(" ~! repsep(str,",")~ ")"   ^^ { case _~s~_      => In(RawArr(s)) }
          | ".inV()"                         ^^ {      _          => InV }
          | ".path()"                        ^^ {      _          => PathTraversal() }
          | ".unrollPath()"                  ^^ {      _          => UnrollPath }
          ).asInstanceOf[Parser[Traversal[_,_]]]
            .withFailureMessage("traversal step expected (ex: .inV(), .out('foo'))")

      // Either a comma-delimited list of longs, or an array of longs
      def quineIds: Parser[QueryValue[Seq[QuineId]]] =
          ( uuidArr
          | repsep(uuid,",") ^^ { case x => RawArr[QuineId](x) }
          ).withFailureMessage("list (or array) of uuids expected")

      // Possible sources for traversals
      def travSource: Parser[Traversal[Nothing, _]] =
          ( "g.V(" ~! quineIds ~ ")"             ^^ { case _~ids~_ => Vertices(ids.map(_.map(Vertex.apply))) }
          | ("__" | "_")                         ^^ {      _       => Anon() }
          ).asInstanceOf[Parser[Traversal[Nothing, _]]]
          .withFailureMessage("traversal source expected (ex: g.V(1, 2), g.V([1,2]), g.E(), _)")

      // Parser for a traversal
      def trav: Parser[Tr] = {
        val t = travSource ~ rep(travSuffix) ^^ {
          case src ~ sufs => sufs.foldLeft[Tr](src)((t,suf) => suf(t))
        }
        t.withFailureMessage("traversal expected")
      }



      // Possible sources for traversals
      def groundedTravSource: Parser[GroundedTraversal[_]] =
        ( "g.V(" ~! quineIds ~ ")"             ^^ { case _~ids~_ => Vertices(ids.map(_.map(Vertex.apply))) } )
          .withFailureMessage("traversal source expected (ex: g.V(1, 2), g.V([1,2]), g.E(), _)")

      // Parser for a traversal
      def trav: Parser[GroundedTraversal[_]] = {
        val t = groundedTravSource ~ rep(travSuffix) ^^ {
          case src ~ sufs => sufs.foldLeft[Tr](src)((t,suf) => suf(t))
        }
        t.withFailureMessage("traversal expected")
      }


      // Queries are _not_ left recursive (unlike the traversals), but we still need to abstract
      // prefixes (and then repeat them) so that variables of different types get noted before we
      // get to the queries that use them.
      def queryPrefix: Parser[Qy => Qy] =
          ( ident ~ "=" ~ int ~ ";"               ^^ { case i~_~v~_ => ints += i; AssignLiteral(i,v,_: Qy).asInstanceOf[Qy] }
          | ident ~ "=" ~ lng ~ ";"               ^^ { case i~_~v~_ => lngs += i; AssignLiteral(i,v,_: Qy) }
          | ident ~ "=" ~ str ~ ";"               ^^ { case i~_~v~_ => strs += i; AssignLiteral(i,v,_: Qy) }
          | ident ~ "=" ~ (intArr|strArr|lngArr) ~ ";" ^^ { case i~_~v~_ => arrs += i; AssignLiteral(i,v,_: Qy) }
          | ident ~ "=" ~ trav ~ ";"              ^^ { case i~_~t~_ => arrs += i; AssignTraversal(i,t,_: Qy) }
          | trav ~ ";"                            ^^ { case t~_     => DiscardTraversal(t,_: Qy) }
          ).asInstanceOf[Parser[Qy => Qy]]
          .withFailureMessage("query prefix expected (ex: 'x = 1;', 'xs = g.V();')")


      // Parser for a query
      def query: Parser[Qy] = rep(queryPrefix) ~ trav ^^ {
        case pres ~ src => pres.foldRight[Qy](FinalTraversal(src))((pre,t) => pre(t))
      }
    }

    Parsers.parseAll(Parsers.query, input.filterNot(_.isWhitespace)) match {
      case Parsers.Success(matched, _) => Success(matched)
      case Parsers.Error(msg, in) => Failure(new Exception(s"At ${in.pos}: $msg\n\n${in.pos.longString}"))
      case Parsers.Failure(msg, in) => Failure(new Exception(s"At ${in.pos}: $msg\n\n${in.pos.longString}"))
      case _ => Failure(new Exception("Parser failed in an unexpected way"))
    }
  }
}

// Contains all the AST of the query language - inside an object to avoid name collisions
object QueryLanguage {

  // TOOD, don't hardcode this?
  implicit val dumpOpsTimeout: Timeout = 5.seconds

  type P[T] = T => Boolean
  case class Vertex(id: QuineId)
  case class Edge(src: QuineId, tgt: QuineId, edgeType: Symbol, direction: EdgeDirValue)

  case class ResultContext[T](
      unwrap: T,
      path: List[Vertex],
      matchContext: Map[Symbol, _] // TODO: make dependent? :D
  ) {
    def map[B](f: T => B): ResultContext[B] = ResultContext(f(unwrap), path, matchContext)
  }
  object ResultContext {
    def singleton[T](x: T): ResultContext[T] = ResultContext(x, Nil, Map.empty)
    def vertex(v: Vertex): ResultContext[Vertex] = ResultContext(v, List(v), Map.empty)
  }

  // Represents possibly a series of traversals/assignments ending in a traversal that produces a
  // stream of values of type 'T'.
  trait Query[T] {
    // Remark: running the query may actually involve _multiple_ queries to the DB, depending on the
    // nature of the query.
    def run(graph: GraphService, context: Map[String,QueryValue[_]] = Map())(implicit ec: ExecutionContext): Future[IndexedSeq[T]]
    def apply(graph: GraphService)(implicit ec: ExecutionContext): Future[IndexedSeq[T]] = run(graph)
  }

  case class AssignLiteral[E](name: String, value: QueryValue[_], `then`: Query[E]) extends Query[E] {
    override def run(graph: GraphService, context: Map[String,QueryValue[_]])(implicit ec: ExecutionContext) =
      `then`.run(graph, context + (name -> value))
  }
  case class AssignTraversal[E](name: String, value: GroundedTraversal[_], `then`: Query[E]) extends Query[E] {
    override def run(graph: GraphService, context: Map[String,QueryValue[_]])(implicit ec: ExecutionContext) = for {
      result <- value.apply(graph, context)(IndexedSeq.empty)
      s1 <- `then`.run(graph, context + (name -> Raw(result)))
    } yield s1
  }
  case class DiscardTraversal[E](value: GroundedTraversal[_], `then`: Query[E]) extends Query[E] {
    override def run(graph: GraphService, context: Map[String, QueryValue[_]])(implicit ec: ExecutionContext) = for {
      _ <- value.apply(graph, context)(IndexedSeq.empty)
      s1 <- `then`.run(graph, context)
    } yield s1
  }
  case class FinalTraversal[E](traversal: GroundedTraversal[E]) extends Query[E] {
    override def run(graph: GraphService, context: Map[String,QueryValue[_]])(implicit ec: ExecutionContext) =
      traversal.apply(graph, context)(IndexedSeq.empty).map(_.map(_.unwrap))
  }


  // Represents a traversal across a graph where 'S' is the source type and 'T' the destination type
  sealed trait Traversal[S, T] { self =>

    // Run our traversal on a graph.
    def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[S]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[T]]]

    def chain[U](other: Traversal[T, U])(implicit ec: ExecutionContext): Traversal[S,U] = new Traversal[S, U] {
      def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[S]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[U]]] = for {
         inter <- self.apply(graph, context)(source)
         output <- other.apply(graph, context)(inter)
      } yield output
    }
  }

  type GroundedTraversal[T] = Traversal[Nothing, T]

  // Traversals that stand up on their own
  case class Vertices(ids: QueryValue[Seq[Vertex]]) extends GroundedTraversal[Vertex] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[Nothing]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[Vertex]]] =
      Future.successful(ids.eval(context).map(ResultContext.vertex).toIndexedSeq)
  }
  case class Edges(ids: QueryValue[Seq[Edge]]) extends GroundedTraversal[Edge] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[Nothing]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[Edge]]] =
      Future.successful(ids.eval(context).map(ResultContext.singleton).toIndexedSeq)
  }
  case class Anon[A]() extends Traversal[A,A] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[A]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[A]]] =
      Future.successful(source.toIndexedSeq)
  }





  // Something that at runtime will be of type `T`
  sealed trait QueryValue[+T] { self =>
    def eval(context: Map[String,QueryValue[_]]): T
    def map[U](f: T => U): QueryValue[U] = new QueryValue[U] {
      def eval(context: Map[String,QueryValue[_]]): U = f(self.eval(context))
    }
  }
  case class Raw[T](value: T) extends QueryValue[T] {
    override def eval(context: Map[String,QueryValue[_]]): T = value
  }
  case class RawArr[T](values: Seq[QueryValue[T]]) extends QueryValue[Seq[T]] {
    override def eval(context: Map[String,QueryValue[_]]): Seq[T] = values.map(_.eval(context))
  }
  case class RawEqPred[T](comp: QueryValue[T]) extends QueryValue[P[T]] {
    override def eval(context: Map[String,QueryValue[_]]): P[T] = _ == comp.eval(context)
  }
  case class RawNeqPred[T](comp: QueryValue[T]) extends QueryValue[P[T]] {
    override def eval(context: Map[String,QueryValue[_]]): P[T] = _ != comp.eval(context)
  }
  case class RawWithinPred[T](col: QueryValue[Seq[T]]) extends QueryValue[P[T]] {
    override def eval(context: Map[String,QueryValue[_]]): P[T] = col.eval(context).contains(_)
  }
  case class RawRegexPred(regex: QueryValue[String]) extends QueryValue[P[String]] {
    override def eval(context: Map[String,QueryValue[_]]): P[String] = _.matches(regex.eval(context))
  }
  case class Variable[T](name: String) extends QueryValue[T] {
    override def eval(context: Map[String,QueryValue[_]]): T = context(name).eval(context).asInstanceOf[T]
  }

  // Filters of traversals
  case class Has(key: QueryValue[String]) extends Traversal[Vertex,Vertex] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[Vertex]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[Vertex]]] = {
      val keySym = Symbol(key.eval(context))
      val filteredVerts = source.map(v => graph.dumbOps.getAllProps(v.unwrap.id).map(p => if (p.contains(keySym)) Some(v) else None))
      Future.sequence(filteredVerts).map(_.collect { case Some(v) => v })
    }
  }

  case class HasValue[V: PickleReader](k: QueryValue[String], v: QueryValue[V]) extends Traversal[Vertex,Vertex] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[Vertex]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[Vertex]]] = {
      val keySym = Symbol(k.eval(context))
      val value = v.eval(context)
      val filteredVerts = source.map(v => graph.dumbOps.getAllProps(v.unwrap.id).map(p =>
        for {
          pickled <- p.get(keySym)
          unpickled <- pickled.thisPickle.unpickleOption[V]
          if unpickled == value
        } yield v
      ))
      Future.sequence(filteredVerts).map(_.collect { case Some(v) => v })
    }
  }

  case class HasPredicate[V](k: QueryValue[String], p: QueryValue[P[V]]) extends Traversal[Vertex,Vertex] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[Vertex]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[Vertex]]] = {
      val keySym = Symbol(k.eval(context))
      val pred = p.eval(context)
      val filteredVerts = source.map(v => graph.dumbOps.getAllProps(v.unwrap.id).map(p =>
        for {
          pickled <- p.get(keySym)
          unpickled <- pickled.thisPickle.unpickleOption[V]
          if pred(unpickled)
        } yield v
      ))
      Future.sequence(filteredVerts).map(_.collect { case Some(v) => v })
    }
  }

//  case class HasLabel[S,T](traversal: Traversal[S,T], label: QueryValue[String]) extends Traversal[S,T] {
//    override def buildTraversal(graph: GraphService, context: Map[String,QueryValue[_]]) =
//      traversal.buildTraversal(graph,context).has(Token.label, LabelP.of(label.eval(context)))
//  }

  case class HasId(id: QueryValue[QuineId]) extends Traversal[Vertex,Vertex] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[Vertex]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[Vertex]]] = {
      val qid = id.eval(context)
      Future.successful(source.filter(_.unwrap.id == qid))
    }
  }

  case class HasNot(key: QueryValue[String]) extends Traversal[Vertex,Vertex] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[Vertex]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[Vertex]]] = {
      val keySym = Symbol(key.eval(context))
      val filteredVerts = source.map(v => graph.dumbOps.getAllProps(v.unwrap.id).map(p => if (!p.contains(keySym)) Some(v) else None))
      Future.sequence(filteredVerts).map(_.collect { case Some(v) => v })
    }
  }

  case class Where[S](where: Traversal[S,_]) extends Traversal[S,S] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[S]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[S]]] = {
      val filteredVerts = source.map(s => where.apply(graph, context)(IndexedSeq(s)).map(stuffs => if (stuffs.nonEmpty) Some(s) else None))
      Future.sequence(filteredVerts).map(_.collect { case Some(v) => v })
    }
  }

  case class And[S](anded: Seq[Traversal[S,_]]) extends Traversal[S,S] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[S]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[S]]] = {
      val conjunction = anded.par.map { and: Traversal[S, _] =>
        val filteredVerts = source.map(s => and.apply(graph, context)(IndexedSeq(s)).map(stuffs => if (stuffs.nonEmpty) Some(s) else None))
        Future.sequence(filteredVerts).map(_.collect { case Some(v) => v })
      }.toIndexedSeq
      Future.sequence[IndexedSeq[ResultContext[S]], IndexedSeq](conjunction).map(_.foldLeft(source)(_ intersect _)) // TODO wrong
    }
  }

  case class Or[S](ored: Seq[Traversal[S,_]]) extends Traversal[S,S] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[S]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[S]]] = {
      val disjunction = ored.par.map { and: Traversal[S, _] =>
        val filteredVerts = source.map(s => and.apply(graph, context)(IndexedSeq(s)).map(stuffs => if (stuffs.nonEmpty) Some(s) else None))
        Future.sequence(filteredVerts).map(_.collect { case Some(v) => v })
      }.toIndexedSeq
      Future.sequence[IndexedSeq[ResultContext[S]], IndexedSeq](disjunction).map(_.foldLeft(IndexedSeq.empty[S])(???)) // _ union _)) // TODO wrong
    }
  }

  case class Not[S](where: Traversal[S,_]) extends Traversal[S,S] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[S]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[S]]] = {
      val filteredVerts = source.map(s => where.apply(graph, context)(IndexedSeq(s)).map(stuffs => if (stuffs.isEmpty) Some(s) else None))
      Future.sequence(filteredVerts).map(_.collect { case Some(v) => v })
    }
  }

  case class Dedup[S]() extends Traversal[S,S] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[S]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[S]]] = {
      Future.successful({
        val seen = mutable.Set.empty[S]
        source.filter(r => seen.add(r.unwrap))
      })
    }
  }

  case class Limit[S](lim: QueryValue[java.lang.Long]) extends Traversal[S,S] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[S]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[S]]] = {
      Future.successful(source.take(lim.eval(context).toInt))
    }
  }

  case class Is[S](value: QueryValue[S]) extends Traversal[S,S] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[S]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[S]]] = {
      val v = value.eval(context)
      Future.successful(source.filter(_.unwrap == v))
    }
  }

  case class IsPredicate[S](pred: QueryValue[P[S]]) extends Traversal[S,S] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[S]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[S]]] = {
      val p = pred.eval(context)
      Future.successful(source.filter(x => p(x.unwrap)))
    }
  }


  // Indirect
  case class OrderBy[V](key: QueryValue[String])(implicit ordering: Ordering[V]) extends Traversal[Vertex,Vertex] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[Vertex]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[Vertex]]] = {
      val keySym = Symbol(key.eval(context))
      val pairedVerts = source.map(v => graph.dumbOps.getAllProps(v.unwrap.id).map(p => (p.get(keySym), v)))
      Future.sequence(pairedVerts).map(p => p.sortBy(_._1).map(_._2) )
    }
  }

  case class As[S](label: QueryValue[String]) extends Traversal[S,S] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[S]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[S]]] = {
      val labelSym = Symbol(label.eval(context))
      Future.successful(source.map(x => x.copy(matchContext = x.matchContext + (labelSym -> x.unwrap))))
    }
  }

  // Reduce/extract
  case class Values(keys: QueryValue[Seq[String]]) extends Traversal[Vertex,_] { }
  case class ValueMap(keys: QueryValue[Seq[String]]) extends Traversal[Vertex,java.util.Map[String,_]] { }

  case object Max extends Traversal[java.lang.Long,java.lang.Long] { }
  case object Min extends Traversal[java.lang.Long,java.lang.Long] { }
  case object Sum extends Traversal[java.lang.Long,java.lang.Long] { }
  case class Select[S,T](key: QueryValue[String]) extends Traversal[S,T] {  }  // bogus: the `T` should be existential
  case class SelectMult[S](keys: QueryValue[Seq[String]]) extends Traversal[S,java.util.Map[String,_]] { }
  case class Count[S]() extends Traversal[S,Long] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[S]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[Long]]] =
      Future.successful(IndexedSeq(ResultContext.singleton(source.length.toLong)))
  }

  case class GroupCount[S]() extends Traversal[S,java.util.Map[String,S]] { }
  case class PathTraversal[S]() extends Traversal[S,List[Vertex]] { }
  case object UnrollPath extends Traversal[List[Vertex], Vertex] { }


  // Extend outwards
  case class Both(edgeLabels: QueryValue[Seq[String]]) extends Traversal[Vertex,Vertex] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[Vertex]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[Vertex]]] = {
      val lbls = edgeLabels.eval(context).map(Symbol.apply).toSet

      val newVerts = source.map { v =>
        graph.dumbOps.getAllEdges(v.unwrap.id).map(edges =>
          for {
            (sym, dirId) <- edges
            if lbls.isEmpty || lbls.contains(sym)
            qId <- dirId.values.flatten
            vert = Vertex(qId)
          } yield ResultContext(vert, vert :: v.path, v.matchContext)
        )
      }

      Future.sequence(newVerts).map(_.flatten)
    }
  }

  case object BothV extends Traversal[Edge,Vertex] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[Edge]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[Vertex]]] = {
      Future.successful(source.flatMap(e => List(
        ResultContext(Vertex(e.unwrap.src), Vertex(e.unwrap.src) :: e.path, e.matchContext),
        ResultContext(Vertex(e.unwrap.tgt), Vertex(e.unwrap.tgt) :: e.path, e.matchContext)
      )))
    }
  }

  case class Out(edgeLabels: QueryValue[Seq[String]]) extends Traversal[Vertex,Vertex] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[Vertex]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[Vertex]]] = {
      val lbls = edgeLabels.eval(context).map(Symbol.apply).toSet

      val newVerts = source.map { v =>
        graph.dumbOps.getAllEdges(v.unwrap.id).map(edges =>
          for {
            (sym, dirId) <- edges
            if lbls.isEmpty || lbls.contains(sym)
            qId <- dirId.getOrElse(EdgeDirections.Outgoing, Set.empty)
            vert = Vertex(qId)
          } yield ResultContext(vert, vert :: v.path, v.matchContext)
        )
      }

      Future.sequence(newVerts).map(_.flatten)
    }
  }

  case object OutV extends Traversal[Edge,Vertex] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[Edge]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[Vertex]]] = {
      Future.successful(source.map(e =>
        ResultContext(Vertex(e.unwrap.tgt), Vertex(e.unwrap.tgt) :: e.path, e.matchContext)
      ))
    }
  }

  case class In(edgeLabels: QueryValue[Seq[String]]) extends Traversal[Vertex,Vertex] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[Vertex]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[Vertex]]] = {
      val lbls = edgeLabels.eval(context).map(Symbol.apply).toSet

      val newVerts = source.map { v =>
        graph.dumbOps.getAllEdges(v.unwrap.id).map(edges =>
          for {
            (sym, dirId) <- edges
            if lbls.isEmpty || lbls.contains(sym)
            qId <- dirId.getOrElse(EdgeDirections.Incoming, Set.empty)
            vert = Vertex(qId)
          } yield ResultContext(vert, vert :: v.path, v.matchContext)
        )
      }

      Future.sequence[Iterable[ResultContext[Vertex]], IndexedSeq](newVerts).map(_.flatten)
    }
  }

  case object InV extends Traversal[Edge,Vertex] {
    override def apply(graph: GraphService, context: Map[String,QueryValue[_]])(source: IndexedSeq[ResultContext[Edge]])(implicit ec: ExecutionContext): Future[IndexedSeq[ResultContext[Vertex]]] = {
      Future.successful(source.map(e =>
        ResultContext(Vertex(e.unwrap.src), Vertex(e.unwrap.src) :: e.path, e.matchContext)
      ))
    }
  }

}
