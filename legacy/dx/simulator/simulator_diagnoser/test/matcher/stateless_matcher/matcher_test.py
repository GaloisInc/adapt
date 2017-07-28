import unittest
from simulator_diagnoser.matcher import MatcherResult, \
                                        Terminal, \
                                        Sequence, \
                                        RuleException, \
                                        StatelessMatcher, \
                                        CrispStrategy


class MatcherTest(unittest.TestCase):
    def setUp(self):
        g1 = Sequence([Terminal('A'),
                       Terminal('B'),
                       Terminal('C')],
                      'sequence')
        self.matcher1 = StatelessMatcher(g1, CrispStrategy())

    def test_matched_sequence1(self):
        state = self.matcher1.match_state('A')
        state = self.matcher1.match_state('B', state)
        state = self.matcher1.match_state('C', state)
        self.assertEqual(1.0, self.matcher1.state_score(state))

    def test_unmatched_sequence1(self):
        state = self.matcher1.match_state('C')
        state = self.matcher1.match_state('B', state)
        state = self.matcher1.match_state('A', state)
        self.assertEqual(0.0, self.matcher1.state_score(state))
