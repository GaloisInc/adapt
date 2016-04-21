import unittest
from test_util import map_path
from simulator_diagnoser.matcher import MatcherResult, \
                                        Terminal, \
                                        RuleException


class TerminalMatcherTest(unittest.TestCase):
    def setUp(self):
        self.nt = Terminal('X')

    def test_label(self):
        self.assertEqual('X', self.nt.get_label())

    def test_simple_path(self):
        path = map_path('abcXdeX')
        matches = self.nt.match_path(path)
        expected_result = [MatcherResult(path, [(3, 'X')], counter=3),
                           MatcherResult(path, [(6, 'X')], counter=6)]
        self.assertListEqual(matches, expected_result)

    def test_multiple_symbols(self):
        path = [['a'], ['b', 'X'], ['X', 'c']]
        matches = self.nt.match_path(path)
        expected_result = [MatcherResult(path, [(1, 'X')], counter=1),
                           MatcherResult(path, [(2, 'X')], counter=2)]
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
        self.nt = Terminal('X', matchable=False)

        path = map_path('X')
        matches = self.nt.match_path(path)
        expected_result = []
        self.assertListEqual(matches, expected_result)

    def test_reverse(self):
        path = map_path('abcXdeX')
        matches = self.nt.match_reverse_path(path)
        expected_result = [MatcherResult(path, [(6, 'X')], counter=6, reverse=True),
                           MatcherResult(path, [(3, 'X')], counter=3, reverse=True)]
        self.assertListEqual(matches, expected_result)

if __name__ == '__main__':
    unittest.main()
