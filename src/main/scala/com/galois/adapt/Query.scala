package com.galois.adapt

//import com.thinkaurelius.titan.core.attribute.Text
import org.apache.tinkerpop.gremlin.structure.{Edge, Vertex, Property => GremlinProperty, T => Token, Graph}
import org.apache.tinkerpop.gremlin.process.traversal.{P, Path, Traverser}
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
 *                 | traversal '.bothV()'
 *                 | traversal '.out(' string ',' ... ')'
 *                 | traversal '.outV()'
 *                 | traversal '.in(' string ',' ... ')'
 *                 | traversal '.inV()'
 *                 | traversal '.bothE()'
 *                 | traversal '.outE(' string ',' ... ')'
 *                 | traversal '.inE()'
 *                 | traversal '.repeat(' traversal ')'
 *                 | traversal '.union(' traversal ',' ... ')'
 *                 | traversal '.local(' traversal ')'
 *                 | traversal '.property(' string ',' literal ',' ... ')'
 *                 | traversal '.properties()'
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

  import QueryLanguage._

  // Run a 'Traversal' on a 'Graph'
  def run[T](query: Query[T], graph: Graph): Try[Stream[T]] = query.run(graph)
  def run[T](query: String, graph: Graph): Try[Stream[T]] =
    Query(query).flatMap(_.run(graph)).flatMap(t => Try(t.asInstanceOf[Stream[T]]))

  // Attempt to parse a traversal from a string
  def apply(input: String): Try[Query[_]] = {

    object Parsers extends JavaTokenParsers with RegexParsers {
      // Synonym for untyped traversal and query
      type Tr = Traversal[_,_]
      type Qy = Query[_]
     
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
        | stringLiteral            ^^ { case s => Raw(StringContext.treatEscapes(s.stripPrefix("\"").stripSuffix("\""))) }
        | "\'" ~! "[^\']*".r ~ "\'" ^^ { case _~s~_ => Raw(s) }
        ).withFailureMessage("string expected (ex: x, \"hello\", 'hello')")

      def uid: Parser[QueryValue[java.util.UUID]] = ( variable(uids) |
        """[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA_F]{4}-[0-9a-fA_F]{4}-[0-9a-fA_F]{12}""".r ^^ {
          s => Raw(java.util.UUID.fromString(s))
        }).withFailureMessage("UUID expected (ex: x, 123e4567-e89b-12d3-a456-426655440000)")

      def uidArr: Parser[QueryValue[Seq[java.util.UUID]]] =
        ( variable(arrs) | arr(uid) ).withFailureMessage("UUID array expected")

      def lngArr: Parser[QueryValue[Seq[java.lang.Long]]] =
        ( variable(arrs) | arr(lng) ).withFailureMessage("long array expected")

      def intArr: Parser[QueryValue[Seq[Int]]] =
        ( variable(arrs) | arr(int) ).withFailureMessage("int array expected")

      def strArr: Parser[QueryValue[Seq[String]]] =
        ( variable(arrs) | arr(str) ).withFailureMessage("string array expected")

      def lit: Parser[QueryValue[_]] =
        ( uid ||| int ||| lng ||| str
        ||| intArr ||| lngArr ||| uidArr ||| strArr
        ).withFailureMessage("literal expected (ex: 9, 12I, \"hello\", [1,2,3])")

      def pred: Parser[QueryValue[P[Any]]] =
        (   predicate(uid)
        ||| predicate(int)
        ||| predicate(lng)
        ||| predicate(str)
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
        | "lte(" ~ elem ~ ")"                    ^^ { case _~l~_     => RawLtePred(l) }
        | "gte(" ~ elem ~ ")"                    ^^ { case _~l~_     => RawGtePred(l) }
        | "between(" ~ elem ~ ',' ~ elem ~ ")"   ^^ { case _~l~_~h~_ => RawBetweenPred(l,h) }
        | "within(" ~ variable(arrs) ~ ")"       ^^ { case _~v~_     => RawWithinPred(v) }
        | "within(" ~ arr(elem) ~ ")"            ^^ { case _~l~_     => RawWithinPred(l) }
        ).asInstanceOf[Parser[QueryValue[P[T]]]]
         .withFailureMessage(s"predicate of type ${ev.runtimeClass.getSimpleName()} expected")

      // Since a traversal is recursively defined, it is convenient for parsing to think of suffixes.
      // Otherwise, we get left recursion...
      //
      // Remark: we need the type annotation only when parsing the very first disjunct because Scala type
      // inference is stupidly asymmetric.
      def travSuffix: Parser[Tr => Tr] =
        ( ".has(" ~ str ~ ")"              ^^ { case _~k~_      => Has(_: Tr, k): Tr }
        | ".has(" ~ str ~ "," ~ lit ~ ")"  ^^ { case _~k~_~v~_  => HasValue(_: Tr, k, v) }
        | ".has(" ~ str ~ "," ~ trav ~ ")" ^^ { case _~k~_~t~_  => HasTraversal(_: Tr, k, t) }
        | ".has(" ~ str ~ "," ~ pred ~ ")" ^^ { case _~l~_~p~_  => HasPredicate(_: Tr, l, p) }
        | ".hasLabel(" ~! str ~ ")"        ^^ { case _~l~_      => HasLabel(_: Tr, l) }
        | ".has(label," ~! str ~ ")"       ^^ { case _~l~_      => HasLabel(_: Tr, l) }
        | ".hasNot(" ~! str ~ ")"          ^^ { case _~s~_      => HasNot(_: Tr, s) }
        | ".hasId(" ~! lng ~ ")"           ^^ { case _~l~_      => HasId(_: Tr, l) }
        | ".where(" ~ trav ~ ")"           ^^ { case _~t~_      => Where(_: Tr, t) }
        | ".where(" ~ predicate(str) ~ ")" ^^ { case _~p~_      => WherePredicate(_: Tr, p) }
        | ".and(" ~! repsep(trav,",")~ ")" ^^ { case _~ts~_     => And(_: Tr, ts) }
        | ".or(" ~! repsep(trav,",")~ ")"  ^^ { case _~ts~_     => Or(_: Tr, ts) }
        | ".dedup()"                       ^^ { case _          => Dedup(_: Tr) }
        | ".limit(" ~! lng ~ ")"           ^^ { case _~i~_      => Limit(_: Tr, i) }
        | ".is(" ~ pred ~ ")"              ^^ { case _~p~_      => IsPredicate(_: Tr, p) }
        | ".is(" ~ lit ~ ")"               ^^ { case _~l~_      => Is(_: Tr, l) }
        | ".simplePath()"                  ^^ { case _          => SimplePath(_: Tr) }
        | ".times(" ~! int ~ ")"           ^^ { case _~l~_      => Times(_: Tr, l) }
        | ".order()"                       ^^ { case _          => Order(_: Tr) }
        | ".by(id)"                        ^^ { case _          => ByToken(_: Tr, Token.id) }
        | ".by(key)"                       ^^ { case _          => ByToken(_: Tr, Token.key) }
        | ".by(label)"                     ^^ { case _          => ByToken(_: Tr, Token.label) }
        | ".by(value)"                     ^^ { case _          => ByToken(_: Tr, Token.value) }
        | ".by(" ~ str ~ ")"               ^^ { case _~k~_      => By(_: Tr, k) }
        | ".by(" ~ trav ~ (",incr)" | ")") ^^ { case _~t~_      => ByTraversal(_: Tr, t, true) }
        | ".by(" ~ trav ~ ",decr)"         ^^ { case _~t~_      => ByTraversal(_: Tr, t, false) }
        | ".as(" ~! rep1sep(str,",") ~ ")" ^^ { case _~s~_      => As(_: Tr, RawArr(s)) }
        | ".until(" ~! trav ~ ")"          ^^ { case _~t~_      => Until(_: Tr, t) }
        | ".values(" ~! rep1sep(str,",")~")"    ^^ { case _~s~_      => Values(_: Tr, RawArr(s)) }
        | ".valueMap(" ~! repsep(str,",")~")"   ^^ { case _~s~_      => ValueMap(_: Tr, RawArr(s)) }
        | ".properties(" ~! repsep(str,",")~")" ^^ { case _~s~_     => Properties(_: Tr, RawArr(s)) }
        | ".label()"                       ^^ { case _          => Label(_: Tr) }
        | ".id()"                          ^^ { case _          => Id(_: Tr) }
        | ".max()"                         ^^ { case _          => Max(_: Traversal[_,java.lang.Long]) }
        | ".min()"                         ^^ { case _          => Min(_: Traversal[_,java.lang.Long]) }
        | ".emit()"                        ^^ { case _          => Emit(_: Tr) }
        | ".emit(" ~! trav ~ ")"           ^^ { case _~t~_      => EmitTrav(_: Tr, t) }
        | ".sum()"                         ^^ { case _          => Sum(_: Traversal[_,java.lang.Double]) }
        | ".select("~!rep1sep(str,",")~")" ^^ { case _~Seq(s)~_ => Select(_: Tr, s)
                                                case _~s~_      => SelectMult(_: Tr, RawArr(s)) }
        | ".fold()"                        ^^ { case _          => Fold(_: Tr) }
        | ".unfold()"                      ^^ { case _          => Unfold(_: Tr) }
        | ".count()"                       ^^ { case _          => Count(_: Tr) }
        | ".groupCount()"                  ^^ { case _          => GroupCount(_: Tr) }
        | ".both(" ~! repsep(str,",")~ ")" ^^ { case _~s~_      => Both(_: Traversal[_,Vertex], RawArr(s)) }
        | ".bothV()"                       ^^ { case _          => BothV(_: Traversal[_,Edge]) }
        | ".out(" ~! repsep(str,",")~ ")"  ^^ { case _~s~_      => Out(_: Traversal[_,Vertex], RawArr(s)) }
        | ".outV()"                        ^^ { case _          => OutV(_: Traversal[_,Edge]) }
        | ".in(" ~! repsep(str,",")~ ")"   ^^ { case _~s~_      => In(_: Traversal[_,Vertex], RawArr(s)) }
        | ".inV()"                         ^^ { case _          => InV(_: Traversal[_,Edge]) }
        | ".bothE()"                       ^^ { case _          => BothE(_: Traversal[_,Vertex]) }
        | ".outE(" ~! repsep(str,",")~ ")" ^^ { case _~s~_      => OutE(_: Traversal[_,Vertex], RawArr(s)) }
        | ".inE()"                         ^^ { case _          => InE(_: Traversal[_,Vertex]) }
        | ".repeat(" ~! trav ~ ")"         ^^ { case _~t~_      => Repeat(_: Traversal[_,Edge], t.asInstanceOf[Traversal[_,Edge]]) }
        | ".union(" ~!repsep(trav,",")~")" ^^ { case _~t~_      => Union(_: Tr, t.asInstanceOf[Seq[Traversal[_,A]] forSome { type A }]) }
        | ".local(" ~! trav ~ ")"          ^^ { case _~t~_      => Local(_: Tr, t) }
        | ".match(" ~!repsep(trav,",")~")" ^^ { case _~t~_      => Match(_: Tr, t) }
        | ".property(" ~! rep1sep(str ~ "," ~! lit, ",") ~ ")" ^^ { case _~s~_ =>
            Property(_: Tr, RawArr(s.map { case k~_~v => k }), RawArr(s.map { case k~_~v => v}))
          }
        | ".path()"                        ^^ { case _          => PathTraversal(_: Tr) }
        | ".unrollPath()"                  ^^ { case _          => UnrollPath(_: Traversal[_,Path]) }
        | ".aggregate(" ~! str ~ ")"       ^^ { case _~k~_      => Aggregate(_: Tr, k) }
        | ".toList()"                      ^^ { case _          => identity(_: Tr) }
        | ".choose(" ~! trav ~ ")"         ^^ { case _~t~_      => Choose(_:Tr, t) }
        | ".option(" ~! lit ~ "," ~! trav ~ ")"  ^^ { case _~x~_~t~_  => OptionTrav(_:Tr, x, t) }
        ).asInstanceOf[Parser[Tr => Tr]]
         .withFailureMessage("traversal step expected (ex: .inV(), .out('foo'))")

      // Either a comma-delimited list of longs, or an array of longs
      def longs: Parser[QueryValue[Seq[java.lang.Long]]] =
        ( lngArr
        | repsep(lng,",") ^^ { case x => RawArr[java.lang.Long](x) }
        ).withFailureMessage("list (or array) of longs expected (ex: '[1, 2, 3]', '1, 2, 3')")

      // Possible sources for traversals
      def travSource: Parser[Tr] =
        ( "g.V(" ~! longs ~ ")"                 ^^ { case _~ids~_ => Vertices(ids) }
        | "g.E(" ~! longs ~ ")"                 ^^ { case _~ids~_ => Edges(ids) }
        | ("__" | "_")                          ^^ { case _       => Anon(Raw(Seq())) }
        | "_(" ~! longs ~ ")"                   ^^ { case _~ids~_ => Anon(ids) }
        ).asInstanceOf[Parser[Tr]]
         .withFailureMessage("traversal source expected (ex: g.V(1, 2), g.V([1,2]), g.E(), _)")

      // Parser for a traversal
      def trav: Parser[Tr] = {
        val t = travSource ~ rep(travSuffix) ^^ {
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

  // Represents possibly a series of traversals/assignments ending in a traversal that produces a
  // stream of values of type 'T'.
  trait Query[T] {
    // Remark: running the query may actually involve _multiple_ queries to the DB, depending on the
    // nature of the query.
    def run(graph: Graph, context: Map[String,QueryValue[_]] = Map()): Try[Stream[T]]
    def apply(graph: Graph): Try[Stream[T]] = run(graph)
  }

  case class AssignLiteral[E](name: String, value: QueryValue[_], `then`: Query[E]) extends Query[E] {
    override def run(graph: Graph, context: Map[String,QueryValue[_]]) = `then`.run(graph, context + (name -> value))
  }
  case class AssignTraversal[S,E](name: String, value: Traversal[_,_], `then`: Query[E]) extends Query[E] {
    override def run(graph: Graph, context: Map[String,QueryValue[_]]) = for {
        s <- Try { value.buildTraversal(graph, context).asScala.toSeq }
        result = s.toSeq
        s1 <- `then`.run(graph, context + (name -> Raw(result)))
      } yield s1
  }
  case class DiscardTraversal[S,E](value: Traversal[_,_], `then`: Query[E]) extends Query[E] {
    override def run(graph: Graph, context: Map[String, QueryValue[_]]) = for {
      _ <- Try {
        value.buildTraversal(graph, context)
      }
      s1 <- `then`.run(graph, context)
    } yield s1
  }
  case class FinalTraversal[E](traversal: Traversal[_,E]) extends Query[E] {
    override def run(graph: Graph, context: Map[String,QueryValue[_]]) = Try {
      traversal.buildTraversal(graph, context).asScala.toStream
    }
  }


  // Represents a traversal across a graph where 'S' is the source type and 'T' the destination type
  sealed trait Traversal[S,T] {
    // Convert into a Gremlin traversal by "running" the current traversal on the graph passed in.
    def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]): GraphTraversal[S,T]

    // Run our traversal on a graph.
    def run(graph: Graph): Try[Stream[T]] = Try { buildTraversal(graph, Map()).asScala.toStream }
    def apply(graph: Graph): Try[Stream[T]] = run(graph)
  }



  // Something that at runtime will be of type `T`
  sealed trait QueryValue[+T] {
    def eval(context: Map[String,QueryValue[_]]): T
  }
  case class Raw[T](value: T) extends QueryValue[T] {
    override def eval(context: Map[String,QueryValue[_]]): T = value
  }
  case class RawArr[T](values: Seq[QueryValue[T]]) extends QueryValue[Seq[T]] {
    override def eval(context: Map[String,QueryValue[_]]): Seq[T] = values.map(_.eval(context))
  }
  case class RawEqPred[T](comp: QueryValue[T]) extends QueryValue[P[T]] {
    override def eval(context: Map[String,QueryValue[_]]): P[T] = P.eq(comp.eval(context))
  }
  case class RawNeqPred[T](comp: QueryValue[T]) extends QueryValue[P[T]] {
    override def eval(context: Map[String,QueryValue[_]]): P[T] = P.neq(comp.eval(context))
  }
  case class RawLtePred[T](comp: QueryValue[T]) extends QueryValue[P[T]] {
    override def eval(context: Map[String,QueryValue[_]]): P[T] = P.lte(comp.eval(context))
  }
  case class RawGtePred[T](comp: QueryValue[T]) extends QueryValue[P[T]] {
    override def eval(context: Map[String,QueryValue[_]]): P[T] = P.gte(comp.eval(context))
  }
  case class RawBetweenPred[T](lo: QueryValue[T], hi: QueryValue[T]) extends QueryValue[P[T]] {
    override def eval(context: Map[String,QueryValue[_]]): P[T] = P.between(lo.eval(context),hi.eval(context))
  }
  case class RawWithinPred[T](col: QueryValue[Seq[T]]) extends QueryValue[P[T]] {
    override def eval(context: Map[String,QueryValue[_]]): P[T] = P.within(col.eval(context): _*)
  }
  case class Variable[T](name: String) extends QueryValue[T] {
    override def eval(context: Map[String,QueryValue[_]]): T = context(name).eval(context).asInstanceOf[T]
  }


  // Sources of traversals
  // TODO: Maybe we should reconsider what the first generic parameter in 'Vertices' should do. We
  // could change it such that it is the _possible_ starting types. It would be '_' for Vertices and
  // Edges (since those don't need to assume anything), but still 'A' for Anon (since it depends on
  // what immediately preceded it).
  case class Vertices(ids: QueryValue[Seq[java.lang.Long]]) extends Traversal[Vertex,Vertex] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      graph.traversal().V(ids.eval(context): _*)
  }
  case class Edges(ids: QueryValue[Seq[java.lang.Long]]) extends Traversal[Edge,Edge] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      graph.traversal().E(ids.eval(context): _*)
  }
  case class Anon[A](starts: QueryValue[Seq[A]]) extends Traversal[A,A] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      __.__(starts.eval(context): _*)
  }


  // Filters of traversals
  case class Has[S,T](traversal: Traversal[S,T], key: QueryValue[String]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).has(key.eval(context))
  }
  case class HasValue[S,T,V](traversal: Traversal[S,T], k: QueryValue[String], v: QueryValue[V]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).has(k.eval(context),v.eval(context))
  }
  case class HasTraversal[S,T](traversal: Traversal[S,T], k: QueryValue[String], v: Traversal[_,_]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).has(k.eval(context), v.buildTraversal(graph, context))
  }
  case class HasPredicate[S,T](traversal: Traversal[S,T], k: QueryValue[String], p: QueryValue[P[_]]) extends Traversal[S,T] {
    def buildTraversal(graph: Graph, context: Map[String, QueryValue[_]]) =
      traversal.buildTraversal(graph,context).has(k.eval(context), p.eval(context))
  }
  case class HasLabel[S,T](traversal: Traversal[S,T], label: QueryValue[String]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).hasLabel(label.eval(context))
  }
  case class HasId[S,T](traversal: Traversal[S,T], id: QueryValue[java.lang.Long]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).hasId(id.eval(context))
  }
  case class HasNot[S,T](traversal: Traversal[S,T], id: QueryValue[String]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).hasNot(id.eval(context))
  }
  case class Where[S,T](traversal: Traversal[S,T], where: Traversal[_,_]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).where(where.buildTraversal(graph, context))
  }
  case class WherePredicate[S,T](traversal: Traversal[S,T], where: QueryValue[P[String]]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).where(where.eval(context))
  }
  case class And[S,T](traversal: Traversal[S,T], anded: Seq[Traversal[_,_]]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).and(anded.map(_.buildTraversal(graph, context)): _*)
  }
  case class Or[S,T](traversal: Traversal[S,T], ored: Seq[Traversal[_,_]]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).or(ored.map(_.buildTraversal(graph, context)): _*)
  }
  case class Dedup[S,T](traversal: Traversal[S,T]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = traversal.buildTraversal(graph,context).dedup()
  }
  case class Limit[S,T](traversal: Traversal[S,T], lim: QueryValue[java.lang.Long]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).limit(lim.eval(context))
  }
  case class Is[S,T](traversal: Traversal[S,T], value: QueryValue[Any]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).is(value.eval(context))
  }
  // TODO: figure out how to get this to compile with pred: Value[P[T]]
  case class IsPredicate[S,T](traversal: Traversal[S,T], pred: QueryValue[P[Any]]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).is(pred.eval(context))
  }
  case class Times[S,T](traversal: Traversal[S,T], limit: QueryValue[Int]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).times(limit.eval(context))
  }
  case class SimplePath[S,T](traversal: Traversal[S,T]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = traversal.buildTraversal(graph,context).simplePath()
  }


  // Indirect
  case class Order[S,T](traversal: Traversal[S,T]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = traversal.buildTraversal(graph,context).order()
  }
  case class By[S,T](traversal: Traversal[S,T], key: QueryValue[String]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).by(key.eval(context))
  }
  case class ByToken[S,T](traversal: Traversal[S,T], token: Token) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).by(token)
  }
  case class ByTraversal[S,T](traversal: Traversal[S,T], comp: Traversal[_,_], incr: Boolean) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = {
      val incrComp = new Comparator[java.lang.Long] {
        override def compare(x1: java.lang.Long, x2: java.lang.Long): Int = x1.compareTo(x2)
      }
      traversal.buildTraversal(graph,context).by(
        comp.buildTraversal(graph, context),
        if (incr) incrComp else Collections.reverseOrder(incrComp)
      )
    }
  }
  case class As[S,T](traversal: Traversal[S,T], labels: QueryValue[Seq[String]]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = {
      val labels1 = labels.eval(context)
      traversal.buildTraversal(graph,context).as(labels1(0), labels1.drop(1): _*)
    }
  }
  case class Until[S,E](traversal: Traversal[S,E], cond: Traversal[_,_]) extends Traversal[S,E] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]])
      = traversal.buildTraversal(graph,context).until(cond.buildTraversal(graph, context))
  }
  case class Aggregate[S,E](traversal: Traversal[S,E], key: QueryValue[String]) extends Traversal[S,E] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]])
      = traversal.buildTraversal(graph,context).aggregate(key.eval(context))
  }


  // Reduce/extract
  case class Values[S,T,V](traversal: Traversal[S,T], keys: QueryValue[Seq[String]]) extends Traversal[S,V] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).values(keys.eval(context): _*)
  }
  case class ValueMap[S,T,E](traversal: Traversal[S,T], keys: QueryValue[Seq[String]]) extends Traversal[S,java.util.Map[String,E]] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).valueMap(keys.eval(context): _*)
  }
  case class Label[S,T](traversal: Traversal[S,T]) extends Traversal[S,String] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = traversal.buildTraversal(graph,context).label()
  }
  case class Id[S,T](traversal: Traversal[S,T]) extends Traversal[S,java.lang.Object] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = traversal.buildTraversal(graph,context).id()
  }
  case class Emit[S,E](traversal: Traversal[S,E]) extends Traversal[S,E] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = traversal.buildTraversal(graph,context).emit()
  }
  case class EmitTrav[S,E](traversal: Traversal[S,E], emit: Traversal[_,_]) extends Traversal[S,E] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).emit(emit.buildTraversal(graph,context))
  }
  case class Max[S](traversal: Traversal[S,java.lang.Long]) extends Traversal[S,java.lang.Long] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = traversal.buildTraversal(graph,context).max()
  }
  case class Min[S](traversal: Traversal[S,java.lang.Long]) extends Traversal[S,java.lang.Long] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = traversal.buildTraversal(graph,context).min()
  }
  case class Sum[S](traversal: Traversal[S,java.lang.Double]) extends Traversal[S,java.lang.Double] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = traversal.buildTraversal(graph,context).sum()
  }
  case class Select[S,T](traversal: Traversal[S,_], key: QueryValue[String]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).select(key.eval(context))
  }
  case class SelectMult[S,T](traversal: Traversal[S,_], keys: QueryValue[Seq[String]]) extends Traversal[S,java.util.Map[String,T]] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = {
      val keys1 = keys.eval(context)
      traversal.buildTraversal(graph,context).select(keys1(0), keys1(1), keys1.drop(2): _*)
    }
  }
  case class Fold[S,T](traversal: Traversal[S,T]) extends Traversal[S,java.util.List[T]] {
    def buildTraversal(graph: Graph, context: Map[String, QueryValue[_]]) = traversal.buildTraversal(graph,context).fold()
  }
  case class Unfold[S,T](traversal: Traversal[S,_]) extends Traversal[S,T] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = traversal.buildTraversal(graph,context).unfold()
  }
  case class Count[S](traversal: Traversal[S,_]) extends Traversal[S,java.lang.Long] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = traversal.buildTraversal(graph,context).count()
  }
  case class GroupCount[S](traversal: Traversal[S,_]) extends Traversal[S,java.util.Map[String,java.lang.Long]] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = traversal.buildTraversal(graph,context).groupCount()
  }
  case class PathTraversal[S](traversal: Traversal[S,_]) extends Traversal[S,Path] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = traversal.buildTraversal(graph,context).path()
  }
  case class UnrollPath[S](traversal: Traversal[S,Path]) extends Traversal[S,Vertex] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).flatMap(new java.util.function.Function[Traverser[Path],Iterator[Vertex]]() {
        def apply(t: Traverser[Path]): Iterator[Vertex] =
          t.get().objects().iterator().asInstanceOf[Iterator[Vertex]]
      })
  }


  // Extend outwards
  case class Both[S](traversal: Traversal[S,Vertex], edgeLabels: QueryValue[Seq[String]]) extends Traversal[S,Vertex] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).both(edgeLabels.eval(context): _*)
  }
  case class BothV[S](traversal: Traversal[S,Edge]) extends Traversal[S,Vertex] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = traversal.buildTraversal(graph,context).bothV()
  }
  case class Out[S](traversal: Traversal[S,Vertex], edgeLabels: QueryValue[Seq[String]]) extends Traversal[S,Vertex] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).out(edgeLabels.eval(context): _*)
  }
  case class OutV[S](traversal: Traversal[S,Edge]) extends Traversal[S,Vertex] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = traversal.buildTraversal(graph,context).outV()
  }
  case class In[S](traversal: Traversal[S,Vertex], edgeLabels: QueryValue[Seq[String]]) extends Traversal[S,Vertex] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).in(edgeLabels.eval(context): _*)
  }
  case class InV[S](traversal: Traversal[S,Edge]) extends Traversal[S,Vertex] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = traversal.buildTraversal(graph,context).inV()
  }
  case class BothE[S](traversal: Traversal[S,Vertex]) extends Traversal[S,Edge] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = traversal.buildTraversal(graph,context).bothE()
  }
  case class OutE[S](traversal: Traversal[S,Vertex], edgeLabels: QueryValue[Seq[String]]) extends Traversal[S,Edge] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).outE(edgeLabels.eval(context): _*)
  }
  case class InE[S](traversal: Traversal[S,Vertex]) extends Traversal[S,Edge] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = traversal.buildTraversal(graph,context).inE()
  }
  case class Repeat[S](traversal: Traversal[S,Edge], rep: Traversal[_,Edge]) extends Traversal[S,Edge]{
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).repeat(rep.buildTraversal(graph, context))
  }
  case class Union[S,E](traversal: Traversal[S,_], unioned: Seq[Traversal[_,E]]) extends Traversal[S,E] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).union(unioned.map(_.buildTraversal(graph, context)): _*)
  }
  case class Local[S,E](traversal: Traversal[S,_], loc: Traversal[_,E]) extends Traversal[S,E] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).local(loc.buildTraversal(graph, context))
  }
  case class Match[S,E](traversal: Traversal[S,_], matches: Seq[Traversal[_,_]]) extends Traversal[S,java.util.Map[String,E]] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).`match`(matches.map(_.buildTraversal(graph, context)): _*)
  }
  case class Properties[S](traversal: Traversal[S,_], keys: QueryValue[Seq[String]]) extends Traversal[S,GremlinProperty[_]] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).properties(keys.eval(context)
      :_*).asInstanceOf[GraphTraversal[S,GremlinProperty[_]]]
  }
  case class Choose[S,E](traversal: Traversal[S,E], choice: Traversal[_,_]) extends Traversal[S,E] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).choose(choice.buildTraversal(graph, context))
  }
  case class OptionTrav[S,E](traversal: Traversal[S,E], value: QueryValue[_], option: Traversal[_,_]) extends Traversal[S,E] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) =
      traversal.buildTraversal(graph,context).option(value.eval(context), option.asInstanceOf[Traversal[E,_]].buildTraversal(graph, context))
  }



  // Mutating
  case class Property[S,E](traversal: Traversal[S,E], keys: QueryValue[Seq[String]], values: QueryValue[Seq[_]]) extends Traversal[S,E] {
    override def buildTraversal(graph: Graph, context: Map[String,QueryValue[_]]) = {
      val keys1 = keys.eval(context)
      val values1 = values.eval(context)
      traversal.buildTraversal(graph,context).property(
        keys1(0),
        values1(0),
        (keys1.drop(1) zip values1.drop(1)) flatMap { case (k,v) => Seq(k,v) }
      )
    }
  }
}
