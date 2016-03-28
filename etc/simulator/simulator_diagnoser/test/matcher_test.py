import unittest
from simulator_diagnoser.matcher import MatcherResult, \
                                        NonTerminal, \
                                        Sequence, \
                                        RuleException


class MatcherTest(unittest.TestCase):

    def test_non_terminal_label(self):
        nt = NonTerminal('nt')
        self.assertEqual('nt', nt.get_label())

    def test_non_terminal_simple_path(self):
        nt = NonTerminal('nt')

        path = ['a','b','c','nt','d','e','nt']
        path = [[x] for x in path]
        matches = nt.match_path(path)
        expected_result = [MatcherResult(path, [(3, 'nt')], counter=3),
                           MatcherResult(path, [(6, 'nt')], counter=6)]
        self.assertListEqual(matches, expected_result)

    def test_non_terminal_multiple_symbols(self):
        nt = NonTerminal('nt')
        path = [['a'],['b','nt'],['nt','c']]
        matches = nt.match_path(path)
        expected_result = [MatcherResult(path, [(1,'nt')], counter=1),
                           MatcherResult(path, [(2,'nt')], counter=2)]
        self.assertListEqual(matches, expected_result)


    def test_non_terminal_empty_path(self):
        nt = NonTerminal('nt')

        path = []
        matches = nt.match_path(path)
        expected_result = []
        self.assertListEqual(matches, expected_result)

    def test_non_terminal_mid_match(self):
        nt = NonTerminal('nt')

        path = ['w', 'nt', 'x', 'nt', 'y', 'z']
        path = [[x] for x in path]
        matches = nt.match(MatcherResult(path, [(1, 'nt')], counter=2))
        expected_result = [MatcherResult(path, [(1, 'nt'), (3, 'nt')], counter=3)]

        self.assertListEqual(matches, expected_result)

    #test matchable=False

    def test_non_terminal_with_children(self):
        # non terminal with child nodes should throw exception
        with self.assertRaises(RuleException):
            NonTerminal('nt', None)


    def test_sequence_simple_path(self):
        s = Sequence('sequence', NonTerminal('A'))
        self.assertEqual('sequence', s.get_label())

        s = Sequence('sequence',
                     NonTerminal('A'),
                     NonTerminal('B'),
                     NonTerminal('C'),
                     matchable='False')

        path = ['A','A','B','C','C']
        path = [[x] for x in path]
        matches = s.match_path(path)
        expected_result = [MatcherResult(path, [(0, 'A'), (2, 'B'), (3, 'C')], counter=3),
                           MatcherResult(path, [(0, 'A'), (2, 'B'), (4, 'C')], counter=4),
                           MatcherResult(path, [(1, 'A'), (2, 'B'), (3, 'C')], counter=3),
                           MatcherResult(path, [(1, 'A'), (2, 'B'), (4, 'C')], counter=4)]
        self.assertListEqual(matches, expected_result)

    def test_sequence_nested(self):
        s = Sequence('sequence',
                     NonTerminal('A'),
                     NonTerminal('B'),
                     NonTerminal('C'),
                     matchable='False')
        # repeated sequence
        s2 = Sequence('sequence2',
                     NonTerminal('A'),
                     s,
                     NonTerminal('C'),
                     matchable='False')

        path = ['A','A','B','C','C']
        path = [[x] for x in path]
        matches = s2.match_path(path)
        expected_result = [MatcherResult(path, [(0, 'A'), (1, 'A'), (2, 'B'), (3, 'C'), (4, 'C')], counter=4)]
        self.assertListEqual(matches, expected_result)

    def test_sequence_multiple_symbols(self):
        s2 = Sequence('sequence2',
                      NonTerminal('B'),
                      NonTerminal('C'))

        s = Sequence('sequence',
                     NonTerminal('A'),
                     s2)

        path = [['A'], ['B','C']]
        matches = s.match_path(path)
        expected_result = [MatcherResult(path, [(0,'A'), (1,'B'), (1,'C')], counter=1)]
        self.assertListEqual(matches, expected_result)

    # matchable sequence name / matchable

    # non matchable sequence name

    # sequence of sequences


if __name__ == '__main__':
    unittest.main()
