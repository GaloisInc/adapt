

class Strategy(object):
    def __init__(self):
        self.exploration_threshold = 0.0

    def reduce_states(self, states):
        new_state = list(states[0])
        for i in range(1, len(states)):
            for j, value in enumerate(states[i]):
                new_state[j] = max(new_state[j], value)
        return new_state

    def to_explore(self, score):
        return score > self.exploration_threshold

    def update_score(self, state, node, score, confidence=1.0):
        new_state = list(state)
        new_value = score * confidence
        new_state[node] = max(new_state[node], new_value)
        return new_state

    def propagate_jumps(self, state, node, next_node, weight):
        new_state = list(state)
        new_state[next_node] = \
            self.jump_likelihood(new_state[next_node], new_state[node], weight)
        return new_state

    def jump_likelihood(self, node_score, parent_score, weight):
        pass
