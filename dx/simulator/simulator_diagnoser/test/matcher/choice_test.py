import unittest
from test_util import map_path
from simulator_diagnoser.matcher import MatcherResult, \
                                        Terminal, \
                                        Choice, \
                                        RuleException


class ChoiceTest(unittest.TestCase):
    def setUp(self):
        self.c = Choice([Terminal('A'),
                         Terminal('B'),
                         Terminal('C')],
                        'choice')

    def test_simple_path(self):
        path = map_path('XAZ')
        matches = self.c.match_path(path)
        expected_result = [MatcherResult(path, [(1, 'A')], counter=1)]
        self.assertListEqual(matches, expected_result)

        path = map_path('XBZ')
        matches = self.c.match_path(path)
        expected_result = [MatcherResult(path, [(1, 'B')], counter=1)]
        self.assertListEqual(matches, expected_result)

        path = map_path('XCZ')
        matches = self.c.match_path(path)
        expected_result = [MatcherResult(path, [(1, 'C')], counter=1)]
        self.assertListEqual(matches, expected_result)

    def test_no_matches(self):
        path = map_path('XZ')
        matches = self.c.match_path(path)
        expected_result = []
        self.assertListEqual(matches, expected_result)

    def test_match_label(self):
        path = [['choice'], ['A'], ['X'], ['Z']]
        matches = self.c.match_path(path)
        expected_result = [MatcherResult(path, [(1, 'A')], counter=1),
                           MatcherResult(path, [(0, 'choice')], counter=0)]
        self.assertListEqual(matches, expected_result)

    def test_cardinality(self):
        with self.assertRaises(RuleException):
            Choice([Terminal('A')])

        with self.assertRaises(RuleException):
            Choice([])

if __name__ == '__main__':
    unittest.main()
