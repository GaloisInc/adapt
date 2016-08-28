from simulator_diagnoser.graph.traversal import ForwardAnalysis,\
                                                BackwardAnalysis
from .node_ranker import *

class APTDiagnoser(object):
    def __init__(self, graph, matcher):
        self.graph = graph
        self.graph.clear_matcher_states()
        self.matcher = matcher


    def forward_analysis(self):
        fa = ForwardAnalysis(self.graph)
        nr = NodeRanker()

        for node, parents in fa:
            score = self.matcher.match(self.graph, node, parents)
            nr.new_node(node, parents, score)

        return nr

    def backward_analysis(self, node):
        ba = BackwardAnalysis(self.graph, self.matcher)
        return ba.analyze(node)
