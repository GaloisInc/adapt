package com.galois.adapt

import org.apache.tinkerpop.gremlin.structure.Graph


class QueryEval {
    static Object evaluate(Graph graph, String query) {
        Eval.me("g", graph.traversal(), query)
    }
}
