import unittest
from simulator_diagnoser.graph import InmemoryGraph
from simulator_diagnoser.diagnosis import APTDiagnoser
from simulator_diagnoser.matcher import Terminal, \
                                        Sequence, \
                                        StatelessMatcher

class APTDiagnoserTest(unittest.TestCase):
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


        self.g1.add_node_label(1, 'A')
        self.g1.add_node_label(3, 'A')
        self.g1.add_node_label(7, 'B')
        self.g1.add_node_label(9, 'C')

        g1 = Sequence([Terminal('A'),
                       Terminal('B'),
                       Terminal('C')],
                      'sequence')
        self.matcher1 = StatelessMatcher(g1)
        self.apt_diagnoser = APTDiagnoser(self.g1, self.matcher1)

    def test_forward_analysis(self):
        result = self.apt_diagnoser.forward_analysis()
        self.assertEqual(9, result.get_rank()[0][0])

    def test_backward_analysis(self):
        result = self.apt_diagnoser.forward_analysis()
        node = result.get_rank()[0][0]
        paths = self.apt_diagnoser.backward_analysis(node)
        self.assertEqual(2, len(paths))

    def test_diagnoser(self):
        self.apt_diagnoser.store_diagnoses()
        self.assertEqual(2, len(self.g1.diagnoses))

if __name__ == '__main__':
    unittest.main()
