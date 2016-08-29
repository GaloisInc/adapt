from .strategy import *


class CrispStrategy(Strategy):
    def update_score(self, state, node, score, confidence=1.0):
        return super(CrispStrategy, self).update_score(state, node, score, 1.0)

    def jump_likelihood(self, node_score, parent_score, weight):
        if weight == 0:
            return max(node_score, parent_score)
        return node_score
