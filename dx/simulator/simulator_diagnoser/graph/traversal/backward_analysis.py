
class BackwardAnalysis(object):

    def __init__(self, graph, matcher):
        self.graph = graph
        self.matcher = matcher

    def analyze(self, node):
        self.explored_edges = set()
        pointer = self.matcher.fsm.get_end_node()
        state = self.graph.get_node_matcher_state(node)
        return self.get_path(node, state, pointer, [])

    def get_path(self, node, state, pointer, path):
        parents, edges = self.graph.get_node_parents(node, self.explored_edges)
        parent_states = []
        for p in parents:
            ps = self.graph.get_node_matcher_state(p)
            if ps:
                parent_states.append((p,ps))

        pointer, next_parent, next_state = \
            self.matcher.backward_check(state, pointer, parent_states)

        path.insert(0, node)
        if next_parent == None:
            return path

        return self.get_path(next_parent, next_state, pointer, path)
