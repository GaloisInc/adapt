from .strategy import *


class FuzzyStrategy(Strategy):
    def __init__(self, exploration_threshold=0.05, autojump_likelihood=0.1):
        self.exploration_threshold = exploration_threshold
        self.autojump_likelihood = autojump_likelihood

    def jump_likelihood(self, node_score, parent_score, weight):
        if weight == 0:
            return max(node_score, parent_score)
        return max(node_score, parent_score * self.autojump_likelihood / weight)
