import unittest
from test_util import map_path
from simulator_diagnoser.matcher import MatcherResult, \
                                        Terminal, \
                                        Sequence, \
                                        Optional, \
                                        RuleException


class OptionalTest(unittest.TestCase):
    def setUp(self):
        self.ot = Optional([Terminal('A')])
        self.os = Optional([Sequence([Terminal('B'),
                                      Terminal('C')])])

    def test_simple_path(self):
        path = map_path('ABC')
        matches = self.ot.match_path(path)
        expected_result = [MatcherResult(path),
                           MatcherResult(path, [(0, 'A')], counter=0)]
        self.assertListEqual(matches, expected_result)

    def test_no_match(self):
        path = map_path('BC')
        matches = self.ot.match_path(path)
        expected_result = [MatcherResult(path)]
        self.assertListEqual(matches, expected_result)

    def test_sequence(self):
        path = map_path('XYZ')
        matches = self.os.match_path(path)
        expected_result = [MatcherResult(path)]
        self.assertListEqual(matches, expected_result)

        path = map_path('ABC')
        matches = self.os.match_path(path)
        expected_result = [MatcherResult(path),
                           MatcherResult(path, [(1, 'B'), (2, 'C')], counter=2)]
        self.assertListEqual(matches, expected_result)

if __name__ == '__main__':
    unittest.main()
