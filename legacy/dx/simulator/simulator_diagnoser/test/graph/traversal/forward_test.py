import unittest
from simulator_diagnoser.graph import InmemoryGraph
from simulator_diagnoser.graph.traversal import ForwardAnalysis


class ForwardAnalysisTest(unittest.TestCase):
    def setUp(self):
        # Graph =
        #     9
        #  /  |  \
        # 6   7   8
        #  \ / \ /
        #   4   5
        #  / \ / \
        # 1   2   3
        self.g1 = InmemoryGraph()
        edges = [(1, 4), (2, 4), (2, 5), (3, 5),
                 (4, 6), (4, 7), (5, 7), (5, 8),
                 (6, 9), (7, 9), (8, 9)]

        for edge in edges:
            self.g1.add_edge(*edge)

    def test_none(self):
        fa = ForwardAnalysis(None)

        for x in fa:
            fail()

    def test_graph(self):
        fa = ForwardAnalysis(self.g1)

        for i, (node, parents) in enumerate(fa, start=1):
            self.assertEqual(i, node)
            self.assertEqual(parents, self.g1.get_node_parents(i)[0])

if __name__ == '__main__':
    unittest.main()
