package com.galois.adapt

import org.apache.tinkerpop.gremlin.groovy.jsr223.GremlinGroovyScriptEngine
import org.apache.tinkerpop.gremlin.structure.Graph
import javax.script.Bindings
import javax.script.ScriptException

import scala.util.Try


object QueryRunner {
  def eval(graph: Graph, query: String) = {
    val engine = new GremlinGroovyScriptEngine()
    val bindings = engine.createBindings()
    bindings.put("g", graph.traversal())

    val lineSeparatedQuery = query.split(";")
    if (lineSeparatedQuery.length > 1) (0 until lineSeparatedQuery.length - 1).foreach { idx =>
      val lastQuery = lineSeparatedQuery.last.trim
      val x = lineSeparatedQuery(idx).split("=")
      val symbol = x.head.trim
      val statement = x.last.trim
      val individualItemList = statement.stripPrefix("[").stripSuffix(".toArray()").stripSuffix("]").split(",")
      val newStatement = individualItemList.map(_.trim + "L").mkString("[",",","].toArray()")
      val preparedStatement = lastQuery.replaceFirst(symbol, newStatement)   //replaceFirst("g.V("+symbol+")", "g.V("+newStatement+")")
      bindings.put(symbol, preparedStatement)
    }


    engine.eval(query, bindings)
  }

  def transformQueryIntoSomethingThatStupidGremlinWillEvaluateCorrectlyThisIsABadIdeaShouldDoItAnotherWay(q: String): String = {
    val lineSeparatedQuery = q.split(";")
    var finalQuery = lineSeparatedQuery.last.trim
    if (lineSeparatedQuery.length > 1) (0 until lineSeparatedQuery.length - 1).foreach { idx =>
      val x = lineSeparatedQuery(idx).split("=")
      val symbol = x.head.trim
      val statement = x.last.trim
      val individualItemList = statement.stripPrefix("[").stripSuffix(".toArray()").stripSuffix("]").split(",")
      val newStatement = individualItemList.map(_.trim + "L").mkString("[",",","].toArray()")
      finalQuery = finalQuery.replaceFirst(symbol, newStatement)   //replaceFirst("g.V("+symbol+")", "g.V("+newStatement+")")
    } else {
      if (finalQuery.startsWith("g.V([")) {
        val splitArray = finalQuery.stripPrefix("g.V([").split("].toArray\\([)]\\)", 2)
        val items = splitArray(0).split(",")
        val remainder = splitArray(1)
        finalQuery = items.mkString("g.V([","L,","L].toArray())") + remainder
      } else if (finalQuery.startsWith("g.V(") && ! finalQuery.startsWith("g.V()")) {
        val splitArray = finalQuery.stripPrefix("g.V(").split("[)]", 2)
        val items = splitArray(0).stripPrefix("[").stripSuffix("]").split(",")
        val remainder = splitArray(1)
        finalQuery = items.mkString("g.V(","L,","L)") + remainder
      }
    }
    finalQuery
  }
}


