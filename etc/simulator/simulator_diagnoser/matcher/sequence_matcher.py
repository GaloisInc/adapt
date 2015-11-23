from symbol_matcher import *

class SequenceMatcher:
  
  def __init__(self, transitions):
    self.transitions = transitions
    self.clear()
  
  def clear(self):
    self.matchers = []
    for k,v in self.transitions.iteritems():
      if v[0]:
        self.matchers.append(SymbolMatcher(self.transitions, k, [], False))
  
  def accept_list(self, n, symbols):
    new_matchers = []
    for matcher in self.matchers:
      for m in matcher.accept_list(n, symbols):
        if m not in self.matchers and m not in new_matchers:
          new_matchers.append(m)
    self.matchers.extend(new_matchers)
  
  def get_matches(self):
    return [x.sequence for x in self.matchers if x.is_terminal]