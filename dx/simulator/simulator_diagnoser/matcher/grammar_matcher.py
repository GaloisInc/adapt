

class Rule(object):
    def __init__(self, children, label=None, **kwargs):
        self.label = label
        self.children = children
        self.matchable = kwargs.get('matchable', True)
        if self.label is None:
            self.matchable = False
        self.labels = None
        self.self_check()

    def get_label(self):
        return self.label

    def get_labels(self):
        if not self.labels:
            if self.matchable:
                self.labels = [self.label]
            else:
                self.labels = []

            for child in self.children:
                self.labels.extend(child.get_labels())
        return self.labels

    def self_check(self):
        pass

    def match(self, path):
        pass

    def match_path(self, path):
        return self.match(MatcherResult(path))

    def match_reverse_path(self, path):
        return self.match(MatcherResult(path, reverse=True))

    def match_label(self, matcher_result):
        results = []

        if self.matchable:
            for i, symbols in matcher_result.enumerate():
                if self.label in symbols:
                    mr = matcher_result.new_match(i, self.label)
                    results.append(mr)
        return results

    def generate_fsm(self, fsm, start, end):
        pass

    def generate_fsm_label(self, fsm, start, end, weight):
        w = 0
        if self.matchable:
            w = weight
            fsm.add_edge(start, end, label=self.label, weight=w, rule=self)
        return w


class RuleException(Exception):
    def __init__(self, obj, value):
        self.type = type(obj).__name__
        self.value = value

    def __str__(self):
        return "Rule Exception -> %s: %s" % (self.type, repr(self.value))


class Terminal(Rule):
    def __init__(self, label, **kwargs):
        super(Terminal, self).__init__([], label, **kwargs)

    def match(self, matcher_result):
        return self.match_label(matcher_result)

    def generate_fsm(self, fsm, start, end):
        return self.generate_fsm_label(fsm, start, end, 1)


class Sequence(Rule):
    def self_check(self):
        if len(self.children) < 2:
            raise RuleException(self, "rule must have at least two children.")

    def match(self, matcher_result):
        matchers = [matcher_result]

        for child in self.children:
            new_matchers = []
            for matcher in matchers:
                if matcher.contains(child.get_labels()):
                    matcher.increment_counter()

                new_matchers.extend(child.match(matcher))
            matchers = new_matchers

        matchers.extend(self.match_label(matcher_result))
        return matchers

    def generate_fsm(self, fsm, start, end):
        weight, temp_start, last = (0, start, len(self.children) - 1)
        for i, child in enumerate(self.children):
            if i == last:
                temp_end = end
            else:
                temp_end = fsm.add_node()

            weight = weight + child.generate_fsm(fsm, temp_start, temp_end)
            temp_start = temp_end

        self.generate_fsm_label(fsm, start, end, weight)
        return weight


class Choice(Rule):
    def self_check(self):
        if len(self.children) < 2:
            raise RuleException(self, "rule must have at least two children.")

    def match(self, matcher_result):
        matchers = []

        for child in self.children:
            matchers.extend(child.match(matcher_result))

        matchers.extend(self.match_label(matcher_result))
        return matchers

    def generate_fsm(self, fsm, start, end):
        weight, choices = (0, [])
        for child in self.children:
            choices.append(child.generate_fsm(fsm, start, end))

        if len(choices) != 0:
            weight = min(choices)

        self.generate_fsm_label(fsm, start, end, weight)
        return weight


class Optional(Rule):
    def self_check(self):
        if len(self.children) != 1:
            raise RuleException(self, "rule must have one child.")

    def match(self, matcher_result):
        matchers = [matcher_result]

        for child in self.children:
            matchers.extend(child.match(matcher_result))

        matchers.extend(self.match_label(matcher_result))
        return matchers

    def generate_fsm(self, fsm, start, end):
        fsm.add_edge(start, end, weight=0, rule=self)
        for child in self.children:
            weight = child.generate_fsm(fsm, start, end)
        self.generate_fsm_label(fsm, start, end, weight)
        return 0


class OptionalSequence(Sequence):
    def self_check(self):
        self.children = [Optional([c]) for c in self.children]


class OneOrMore(Rule):
    def self_check(self):
        if len(self.children) != 1:
            raise RuleException(self, "rule must have one child.")

    def match(self, matcher_result):
        child = self.children[0]

        current_matchers = child.match(matcher_result)
        matchers = current_matchers[:]

        while(len(current_matchers) > 0):
            new_matchers = []
            for matcher in current_matchers:
                matcher = matcher.new_increment_counter()
                if matcher.is_valid():
                    new_matchers.extend(child.match(matcher))
            current_matchers = new_matchers
            matchers.extend(current_matchers)

        matchers.extend(self.match_label(matcher_result))
        return matchers


class ZeroOrMore(OneOrMore):
    def self_check(self):
        self.children = [Optional([c]) for c in self.children]
        if len(self.children) != 1:
            raise RuleException(self, "rule must have one child.")


class MatcherResult(object):
    def __init__(self, path, matches=[], counter=None, reverse=False):
        self.path = path
        self.matches = matches
        self.reverse = reverse

        if counter is None:
            self.counter = len(self.path)-1 if self.reverse else 0
        else:
            self.counter = counter

    def __eq__(self, other):
        return (self.path, self.matches, self.counter, self.reverse) ==\
               (other.path, other.matches, other.counter, other.reverse)

    def __repr__(self):
        return "(path: %s, matches: %s, counter: %d, reverse: %s)" %\
               (self.path, self.matches, self.counter, self.reverse)

    def new_match(self, i, symbol):
        if self.reverse:
            new_matches = [(i, symbol)] + self.matches
        else:
            new_matches = self.matches + [(i, symbol)]

        return MatcherResult(self.path,
                             new_matches,
                             i,
                             self.reverse)

    def enumerate(self, start=None):
        if not start:
            start = self.counter

        if self.reverse:
            for i in reversed(range(0, start + 1)):
                yield i, self.path[i]
        else:
            for i in range(start, len(self.path)):
                yield i, self.path[i]

    def contains(self, labels):
        for i, symbol in self.matches:
            if i == self.counter:
                if symbol in labels:
                    return True
        return False

    def next_counter(self):
        if self.reverse:
            return self.counter - 1
        else:
            return self.counter + 1

    def increment_counter(self):
        self.counter = self.next_counter()

    def new_increment_counter(self):
        return MatcherResult(self.path,
                             self.matches,
                             self.next_counter(),
                             self.reverse)

    def is_valid(self):
        if self.reverse:
            return self.counter >= 0
        else:
            return self.counter < len(self.path)

    def get_labels(self, index):
        labels = []
        for i, l in self.matches:
            if index == i:
                labels.append(l)
        return labels
