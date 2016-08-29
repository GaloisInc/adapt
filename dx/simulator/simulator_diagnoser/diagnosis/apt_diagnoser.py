from simulator_diagnoser.graph.traversal import ForwardAnalysis,\
                                                BackwardAnalysis
from .node_ranker import *

class APTDiagnoser(object):
    def __init__(self, graph, matcher, avoid_cycles=False):
        self.graph = graph
        self.graph.clear_matcher_states()
        self.matcher = matcher
        self.avoid_cycles = avoid_cycles


    def forward_analysis(self):
        fa = ForwardAnalysis(self.graph, avoid_cycles=self.avoid_cycles)
        nr = NodeRanker()

        for node, parents in fa:
            score = self.matcher.match(self.graph, node, parents)
            nr.new_node(node, parents, score)

        return nr

    def backward_analysis(self, node):
        ba = BackwardAnalysis(self.graph, self.matcher, avoid_cycles=self.avoid_cycles)
        return ba.analyze(node)

    def diagnose(self):
        nodes = self.forward_analysis()
        for node, _ in nodes.get_rank():
            paths = self.backward_analysis(node)
            for path in paths:
                yield path

    def store_diagnoses(self):
        for path in self.diagnose():
            self.graph.store_diagnoses(path)
