import itertools


class Matcher:
  
  def __init__(self, transitions, pattern, sequence, is_terminal):
    self.transitions = transitions
    self.pattern = pattern
    self.sequence = sequence
    self.is_terminal = is_terminal
  
  def __hash__(self):
    return hash((self.pattern, self.sequence, self.is_terminal))

  def __eq__(self, other):
    return (self.pattern, self.sequence, self.is_terminal) == (other.pattern, other.sequence, other.is_terminal)

  def __repr__(self):
    return "(%s,%s,%s)" % (self.pattern, self.sequence, self.is_terminal)
  
  def accept_list(self, n, symbols):
    perm_symbols = itertools.permutations(symbols)
    result = []
    for permutation in perm_symbols:
      matchers = [self]
      for s in permutation:
        temp_matchers = []
        for matcher in matchers:
          for new_matcher in matcher.accept(n, s):
            if new_matcher not in result:
              temp_matchers.append(new_matcher)
              result.append(new_matcher)
        matchers = temp_matchers
    return result
  
  def accept(self, n, symb):
    if self.pattern and symb == self.pattern:
      _, terminal, links = self.transitions[self.pattern]      
      
      new_sequence = self.sequence
      if n not in new_sequence:
        new_sequence = new_sequence + [n]
      
      a = []
      if links:
        for l in links:
          a.append(Matcher(self.transitions, l, new_sequence, terminal))
      else:
        a.append(Matcher(self.transitions, None, new_sequence, terminal))
      
      return a
    else:
      return []