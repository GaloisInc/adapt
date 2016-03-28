import unittest
from simulator_diagnoser.matcher import MatcherResult, \
                                        NonTerminal, \
                                        Sequence, \
                                        RuleException


class NonTerminalMatcherTest(unittest.TestCase):
    def setUp(self):
        self.nt = NonTerminal('nt')

    def test_label(self):
        self.assertEqual('nt', self.nt.get_label())

    def test_simple_path(self):
        path = [[x] for x in ['a','b','c','nt','d','e','nt']]
        matches = self.nt.match_path(path)
        expected_result = [MatcherResult(path, [(3, 'nt')], counter=3),
                           MatcherResult(path, [(6, 'nt')], counter=6)]
        self.assertListEqual(matches, expected_result)

    def test_multiple_symbols(self):
        path = [['a'],['b','nt'],['nt','c']]
        matches = self.nt.match_path(path)
        expected_result = [MatcherResult(path, [(1,'nt')], counter=1),
                           MatcherResult(path, [(2,'nt')], counter=2)]
        self.assertListEqual(matches, expected_result)

    def test_empty_path(self):
        path = []
        matches = self.nt.match_path(path)
        expected_result = []
        self.assertListEqual(matches, expected_result)

    def test_mid_match(self):
        path = [[x] for x in ['w', 'nt', 'x', 'nt', 'y', 'z']]
        matches = self.nt.match(MatcherResult(path, [(1, 'nt')], counter=2))
        expected_result = [MatcherResult(path, [(1, 'nt'), (3, 'nt')], counter=3)]

        self.assertListEqual(matches, expected_result)

    def test_not_matchable(self):
        self.nt = NonTerminal('nt', matchable=False)

        path = [['nt']]
        matches = self.nt.match_path(path)
        expected_result = []
        self.assertListEqual(matches, expected_result)

    def test_with_children(self):
        # non terminal with child nodes should throw exception
        with self.assertRaises(RuleException):
            NonTerminal('nt', None)


class SequenceMatcherTest(unittest.TestCase):
    def setUp(self):
        self.s = Sequence('sequence',
                          NonTerminal('A'),
                          NonTerminal('B'),
                          NonTerminal('C'))

    def test_label(self):
        self.assertEqual('sequence', self.s.get_label())

    def test_simple_path(self):
        path = [[x] for x in ['A','A','B','C','C']]
        matches = self.s.match_path(path)
        expected_result = [MatcherResult(path, [(0, 'A'), (2, 'B'), (3, 'C')], counter=3),
                           MatcherResult(path, [(0, 'A'), (2, 'B'), (4, 'C')], counter=4),
                           MatcherResult(path, [(1, 'A'), (2, 'B'), (3, 'C')], counter=3),
                           MatcherResult(path, [(1, 'A'), (2, 'B'), (4, 'C')], counter=4)]
        self.assertListEqual(matches, expected_result)

    def test_nested(self):
        s2 = Sequence('sequence2',
                     NonTerminal('A'),
                     self.s,
                     NonTerminal('C'),
                     matchable='False')

        path = [[x] for x in ['A','A','B','C','C']]
        matches = s2.match_path(path)
        expected_result = [MatcherResult(path,
                                         [(0, 'A'), (1, 'A'), (2, 'B'), (3, 'C'), (4, 'C')],
                                         counter=4)]
        self.assertListEqual(matches, expected_result)

    def test_multiple_symbols(self):
        path = [['A'], ['B','C']]
        matches = self.s.match_path(path)
        expected_result = [MatcherResult(path, [(0,'A'), (1,'B'), (1,'C')], counter=1)]
        self.assertListEqual(matches, expected_result)

    # test non matchable sequences (A, AB, ABF , XBC)

    # matchable sequence name / matchable

    # non matchable sequence name

    # sequence of sequences

    # test mid match


if __name__ == '__main__':
    unittest.main()
