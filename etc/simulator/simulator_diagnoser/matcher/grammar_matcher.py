

class Matcher(object):

    def __init__(self, pattern):
        self.pattern = pattern

    def match(self, path):
        return self.pattern.match(path)

class RuleException(Exception):
    def __init__(self, value):
        self.value = value

    def __str__(self):
        return "Rule Exception: " + repr(self.value)

class MatcherResult(object):
    def __init__(self, path, matches=[], counter=0):
        self.path = path
        self.matches = matches
        self.counter = counter

    def __eq__(self, other):
        return (self.path, self.matches, self.counter) ==\
               (other.path, other.matches, other.counter)

    def __repr__(self):
        return "(path: %s, matches: %s, counter: %d)" %\
               (self.path, self.matches, self.counter)

    def new_match(self, i, symbol):
        return MatcherResult(self.path,
                             self.matches + [(i, symbol)],
                             i)

    def enumerate(self, start=None):
        if not start:
            start = self.counter

        for i in xrange(start, len(self.path)):
            yield i, self.path[i]

    def contains(self, labels):
        for i, symbol in self.matches:
            if i == self.counter:
                if symbol in labels:
                    return True
        return False

    def increment_counter(self):
        self.counter = self.counter + 1

    def new_increment_counter(self):
        return MatcherResult(self.path,
                             self.matches,
                             self.counter + 1)

    def is_valid(self):
        return self.counter < len(self.path)

class Rule(object):
    def __init__(self, label, *args, **kwargs):
        self.label = label
        self.children = args
        self.matchable = kwargs.get('matchable', True)
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

    def match_label(self, matcher_result):
        results = []

        if self.matchable:
            for i, symbols in matcher_result.enumerate():
                if self.label in symbols:
                    mr = matcher_result.new_match(i, self.label)
                    results.append(mr)
        return results

class NonTerminal(Rule):
    def self_check(self):
        if len(self.children) > 0:
            raise RuleException("NonTerminal rule must not have children.")

    def match(self, matcher_result):
        return self.match_label(matcher_result)

class Sequence(Rule):
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

class Choice(Rule):
    def match(self, matcher_result):
        matchers = []

        for child in self.children:
            matchers.extend(child.match(matcher_result))

        matchers.extend(self.match_label(matcher_result))
        return matchers

class Optional(Rule):
    def self_check(self):
        if len(self.children) != 1:
            raise RuleException("Optional rule must only have one child.")

    def match(self, matcher_result):
        matchers = [matcher_result]

        for child in self.children:
            matchers.extend(child.match(matcher_result))

        matchers.extend(self.match_label(matcher_result))
        return matchers

class OptionalSequence(Sequence):
    def self_check(self):
        self.children = [Optional(c) for c in children]

class OneOrMore(Rule):
    def self_check(self):
        if len(self.children) != 1:
            raise RuleException("OneOrMore rule must only have one child.")

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
        self.children = [Optional(c) for c in children]
        if len(self.children) != 1:
            raise RuleException("ZeroOrMore rule must only have one child.")
