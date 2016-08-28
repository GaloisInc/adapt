
class BackwardAnalysis(object):

    def __init__(self, graph, matcher, max_iterations=None):
        self.graph = graph
        self.matcher = matcher
        self.max_iterations = max_iterations

    def analyze(self, node):
        self.explored_edges = set()
        pointer = self.matcher.fsm.get_end_node()
        paths = [[(node, pointer, None)]]
        finished_paths = []

        i = 0
        while True:
            current_path = paths.pop(0)
            parents, edges = self.graph.get_node_parents(current_path[0][0], self.explored_edges)
            self.explored_edges |= set(edges)

            self.matcher.backward_check(self.graph, current_path, parents, paths, finished_paths)

            i += 1
            if len(paths) == 0 or \
               (self.max_iterations != None and i > self.max_iterations):
                break
        return finished_paths
