import unittest
from simulator_diagnoser.matcher import MatcherResult, \
                                        NonTerminal, \
                                        Sequence, \
                                        RuleException


def map_path(path):
    return [[x] for x in path]

class NonTerminalMatcherTest(unittest.TestCase):
    def setUp(self):
        self.nt = NonTerminal('X')

    def test_label(self):
        self.assertEqual('X', self.nt.get_label())

    def test_simple_path(self):
        path = map_path('abcXdeX')
        matches = self.nt.match_path(path)
        expected_result = [MatcherResult(path, [(3, 'X')], counter=3),
                           MatcherResult(path, [(6, 'X')], counter=6)]
        self.assertListEqual(matches, expected_result)

    def test_multiple_symbols(self):
        path = [['a'],['b','X'],['X','c']]
        matches = self.nt.match_path(path)
        expected_result = [MatcherResult(path, [(1,'X')], counter=1),
                           MatcherResult(path, [(2,'X')], counter=2)]
        self.assertListEqual(matches, expected_result)

    def test_empty_path(self):
        path = []
        matches = self.nt.match_path(path)
        expected_result = []
        self.assertListEqual(matches, expected_result)

    def test_mid_match(self):
        path = map_path('wXyXyz')
        matches = self.nt.match(MatcherResult(path, [(1, 'X')], counter=2))
        expected_result = [MatcherResult(path, [(1, 'X'), (3, 'X')], counter=3)]

        self.assertListEqual(matches, expected_result)

    def test_not_matchable(self):
        self.nt = NonTerminal('X', matchable=False)

        path = map_path('X')
        matches = self.nt.match_path(path)
        expected_result = []
        self.assertListEqual(matches, expected_result)

    def test_with_children(self):
        # non terminal with child nodes should throw exception
        with self.assertRaises(RuleException):
            NonTerminal('nt', None)

    def test_reverse(self):
        path = map_path('abcXdeX')
        matches = self.nt.match_reverse_path(path)
        expected_result = [MatcherResult(path, [(6, 'X')], counter=6, reverse=True),
                           MatcherResult(path, [(3, 'X')], counter=3, reverse=True)]
        self.assertListEqual(matches, expected_result)


class SequenceMatcherTest(unittest.TestCase):
    def setUp(self):
        self.s = Sequence('sequence',
                          NonTerminal('A'),
                          NonTerminal('B'),
                          NonTerminal('C'))

    def test_label(self):
        self.assertEqual('sequence', self.s.get_label())

    def test_simple_path(self):
        path = map_path('AABCC')
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

        path = map_path('AABCC')
        matches = s2.match_path(path)
        expected_result = \
            [MatcherResult(path, [(0, 'A'), (1, 'A'), (2, 'B'), (3, 'C'), (4, 'C')], counter=4)]
        self.assertListEqual(matches, expected_result)

    def test_multiple_symbols(self):
        path = [['A'], ['B','C']]
        matches = self.s.match_path(path)
        expected_result = [MatcherResult(path, [(0,'A'), (1,'B'), (1,'C')], counter=1)]
        self.assertListEqual(matches, expected_result)

    def test_cardinality(self):
        with self.assertRaises(RuleException):
            Sequence('s')

        with self.assertRaises(RuleException):
            Sequence('s', NonTerminal('A'))

    def test_incomplete_matches(self):
        expected_result = []
        self.assertListEqual(self.s.match_path(map_path('A')), expected_result)
        self.assertListEqual(self.s.match_path(map_path('AB')), expected_result)
        self.assertListEqual(self.s.match_path(map_path('AC')), expected_result)
        self.assertListEqual(self.s.match_path(map_path('BC')), expected_result)
        self.assertListEqual(self.s.match_path(map_path('XYZ')), expected_result)

    def test_match_label(self):
        path = [['x'], ['sequence'], ['y'], ['z']]
        matches = self.s.match_path(path)
        expected_result = [MatcherResult(path, [(1,'sequence')], counter=1)]
        self.assertListEqual(matches, expected_result)

        path = [['A'], ['B'], ['sequence'], ['C']]
        matches = self.s.match_path(path)
        expected_result = [MatcherResult(path, [(0,'A'), (1,'B'), (3,'C')], counter=3),
                           MatcherResult(path, [(2,'sequence')], counter=2)]
        self.assertListEqual(matches, expected_result)

    def test_not_matchable(self):
        self.s.matchable = False
        path = [['x'], ['sequence'], ['y'], ['z']]
        matches = self.s.match_path(path)
        expected_result = []
        self.assertListEqual(matches, expected_result)

    def test_mid_match(self):
        path = map_path('XYZABDC')
        mid_match = MatcherResult(path, [(1, 'Y')], counter=1)
        matches = self.s.match(mid_match)
        expected_result = [MatcherResult(path, [(1, 'Y'), (3,'A'), (4,'B'), (6,'C')], counter=6)]
        self.assertListEqual(matches, expected_result)

    def test_empty_path(self):
        path = []
        matches = self.s.match_path(path)
        expected_result = []
        self.assertListEqual(matches, expected_result)

    def test_reverse(self):
        path = map_path('CBA')
        matches = self.s.match_reverse_path(path)
        expected_result = \
            [MatcherResult(path, [(0, 'C'), (1, 'B'), (2, 'A')], counter=0, reverse=True)]
        self.assertListEqual(matches, expected_result)

    def test_reverse2(self):
        path = map_path('CCBAA')
        matches = self.s.match_reverse_path(path)
        expected_result = \
            [MatcherResult(path, [(1, 'C'), (2, 'B'), (4, 'A')], counter=1, reverse=True),
             MatcherResult(path, [(0, 'C'), (2, 'B'), (4, 'A')], counter=0, reverse=True),
             MatcherResult(path, [(1, 'C'), (2, 'B'), (3, 'A')], counter=1, reverse=True),
             MatcherResult(path, [(0, 'C'), (2, 'B'), (3, 'A')], counter=0, reverse=True)]
        self.assertListEqual(matches, expected_result)

    def test_reverse_incomplete_matches(self):
        expected_result = []
        self.assertListEqual(self.s.match_reverse_path(map_path('A')), expected_result)
        self.assertListEqual(self.s.match_reverse_path(map_path('AB')), expected_result)
        self.assertListEqual(self.s.match_reverse_path(map_path('AC')), expected_result)
        self.assertListEqual(self.s.match_reverse_path(map_path('BC')), expected_result)
        self.assertListEqual(self.s.match_reverse_path(map_path('XYZ')), expected_result)

if __name__ == '__main__':
    unittest.main()
