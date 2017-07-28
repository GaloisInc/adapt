import unittest
from simulator_diagnoser.graph import SegmentationGraph


class GraphTest(unittest.TestCase):
    def setUp(self):
        # Graph =
        #     9
        #  /  |  \
        # 6   7   8
        #  \ / \ /
        #   4   5
        #  / \ / \
        # 1   2   3
        self.g = SegmentationGraph()
        edges = [(1, 4), (2, 4), (2, 5), (3, 5),
                 (4, 6), (4, 7), (5, 7), (5, 8),
                 (6, 9), (7, 9), (8, 9)]

        for edge in edges:
            self.g.add_edge(*edge)

    def test_size(self):
        self.assertEqual(self.g.nodes_length(), 9)
        self.assertEqual(self.g.edges_length(), 11)

    def test_successors(self):
        self.assertListEqual(sorted(self.g.successors(1)), [4])
        self.assertListEqual(sorted(self.g.successors(9)), [])
        self.assertListEqual(sorted(self.g.successors(5)), [7, 8])

    def test_predecessors(self):
        self.assertListEqual(sorted(self.g.predecessors(9)), [6, 7, 8])
        self.assertListEqual(sorted(self.g.predecessors(2)), [])
        self.assertListEqual(sorted(self.g.predecessors(8)), [5])

    def test_node_data(self):
        self.g.set_node_data(7, 'key', 'value')
        self.assertEqual(self.g.get_node_data(7, 'key'), 'value')
        self.assertEqual(self.g.get_node_data(7, 'key2'), None)
        self.assertEqual(self.g.get_node_data(5, 'key'), None)

    def test_random_node(self):
        for x in range(50):
            self.assertTrue(self.g.get_random_node() in self.g.nodes())

    def test_successor_paths(self):
        result = sorted(self.g.successor_paths(7))
        expected_result = [[9]]
        self.assertEqual(result, expected_result)

        result = sorted(self.g.successor_paths(4))
        expected_result = [[6, 9], [7, 9]]
        self.assertEqual(result, expected_result)

        result = sorted(self.g.successor_paths(1))
        expected_result = [[4, 6, 9], [4, 7, 9]]
        self.assertEqual(result, expected_result)

        result = sorted(self.g.successor_paths(9))
        expected_result = [[]]
        self.assertEqual(result, expected_result)

    def test_predecessor_paths(self):
        result = sorted(self.g.predecessor_paths(3))
        expected_result = [[]]
        self.assertEqual(result, expected_result)

        result = sorted(self.g.predecessor_paths(7))
        expected_result = [[1, 4], [2, 4], [2, 5], [3, 5]]
        self.assertEqual(result, expected_result)

    def test_full_paths(self):
        result = sorted(self.g.full_paths(1))
        expected_result = [[1, 4, 6, 9], [1, 4, 7, 9]]
        self.assertEqual(result, expected_result)

        result = sorted(self.g.full_paths(5))
        expected_result = [[2, 5, 7, 9], [2, 5, 8, 9], [3, 5, 7, 9], [3, 5, 8, 9]]
        self.assertEqual(result, expected_result)

        result = sorted(self.g.full_paths(9))
        expected_result = [[1, 4, 6, 9], [1, 4, 7, 9], [2, 4, 6, 9], [2, 4, 7, 9],
                           [2, 5, 7, 9], [2, 5, 8, 9], [3, 5, 7, 9], [3, 5, 8, 9]]
        self.assertEqual(result, expected_result)

if __name__ == '__main__':
    unittest.main()
