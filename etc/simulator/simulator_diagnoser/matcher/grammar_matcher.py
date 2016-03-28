
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


class RuleException(Exception):
    def __init__(self, obj, value):
        self.type = type(obj).__name__
        self.value = value

    def __str__(self):
        return "Rule Exception -> %s: %s" % (self.type, repr(self.value))


class NonTerminal(Rule):
    def self_check(self):
        if len(self.children) > 0:
            raise RuleException(self, "rule must not have children.")

    def match(self, matcher_result):
        return self.match_label(matcher_result)


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
            raise RuleException(self, "rule must have one child.")

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
        self.children = [Optional(c) for c in children]
        if len(self.children) != 1:
            raise RuleException(self, "rule must have one child.")

class MatcherResult(object):
    def __init__(self, path, matches=[], counter=None, reverse=False):
        self.path = path
        self.matches = matches
        self.reverse = reverse

        if counter == None:
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
            for i in reversed(xrange(0, start + 1)):
                yield i, self.path[i]
        else:
            for i in xrange(start, len(self.path)):
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
