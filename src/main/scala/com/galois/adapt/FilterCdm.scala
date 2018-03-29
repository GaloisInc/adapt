package com.galois.adapt

import spray.json.{JsObject, JsString, JsValue, RootJsonFormat}
import spray.json.DefaultJsonProtocol._

object FilterCdm {

  // AST for what a filter function looks like
  //
  // TODO: maybe support regex property patterns?
  sealed trait Filter extends Product
  final case class HasProperty(key: String, value: Option[String]) extends Filter
  final case class HasEdge(label: String, target: Option[java.util.UUID]) extends Filter
  final case class Or(disjuncts: List[Filter]) extends Filter
  final case class And(conjuncts: List[Filter]) extends Filter
  final case class Not(negated: Filter) extends Filter

  // Convert the filter function AST into a Scala function
  def compile(filter: Filter): Filterable => Boolean = filter match {
    case HasProperty(key, None) =>
        cdm => cdm.properties.contains(key)

    case HasProperty(key, Some(value)) =>
        cdm => cdm.properties.get(key).fold(false)(x => x.toString == value)

    case HasEdge(label, None) =>
      cdm => cdm.edges.contains(label)

    case HasEdge(label, Some(target)) =>
        cdm => cdm.edges.get(label).fold(false)(_ == target)

    case Or(disjuncts) =>
      val disjunctPredicates = disjuncts.map(compile)
      cdm => disjunctPredicates.forall(func => func(cdm))

    case And(conjuncts) =>
      val conjunctPredicates = conjuncts.map(compile)
      cdm => conjunctPredicates.exists(func => func(cdm))

    case Not(negated) =>
      val negatedPredicate = compile(negated)
      cdm => !negatedPredicate(cdm)
  }

  // JSON serialization/deserialization for filter function ASTs
  implicit val filterFormat: RootJsonFormat[Filter] = new RootJsonFormat[Filter] {
    override def write(filter: Filter): JsValue = {
      val JsObject(payload) = filter match {
        case hasProp: HasProperty => hasPropertyFormat.write(hasProp)
        case hasEdge: HasEdge => hasEdgeFormat.write(hasEdge)
        case or: Or => orFormat.write(or)
        case and: And => andFormat.write(and)
        case not: Not => notFormat.write(not)
      }
      JsObject(payload + ("type" -> JsString(filter.productPrefix)))
    }

    override def read(json: JsValue): Filter =
      json.asJsObject.getFields("type") match {
        case Seq(JsString("HasProperty")) => hasPropertyFormat.read(json)
        case Seq(JsString("HasEdge")) => hasEdgeFormat.read(json)
        case Seq(JsString("Or")) => orFormat.read(json)
        case Seq(JsString("And")) => andFormat.read(json)
        case Seq(JsString("Not")) => notFormat.read(json)
      }
  }

  implicit val uuidFormat: RootJsonFormat[java.util.UUID] = new RootJsonFormat[java.util.UUID] {
    override def write(uuid: java.util.UUID): JsValue = JsString(uuid.toString)

    override def read(json: JsValue): java.util.UUID = {
      val JsString(uuidStr) = json
      java.util.UUID.fromString(uuidStr)
    }
  }

  val hasPropertyFormat: RootJsonFormat[HasProperty] = jsonFormat2(HasProperty.apply)
  val hasEdgeFormat: RootJsonFormat[HasEdge] = jsonFormat2(HasEdge.apply)
  val orFormat: RootJsonFormat[Or] = jsonFormat1(Or.apply)
  val andFormat: RootJsonFormat[And] = jsonFormat1(And.apply)
  val notFormat: RootJsonFormat[Not] = jsonFormat1(Not.apply)
}


// This is the unit of thing we are filtering over - basically just something that has properties and edges
trait Filterable {
  def properties: Map[String, Any]
  def edges: Map[String, java.util.UUID]
}
object Filterable {
  def apply(node: DBWritable with DBNodeable[AnyRef]): Filterable = new Filterable {
    override lazy val properties = node.asDBKeyValues.toMap
    override lazy val edges = node.asDBEdges.iterator.map({ case (l,t) => (l.toString, t) }).toMap
  }
}