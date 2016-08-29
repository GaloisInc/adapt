
class BackwardAnalysis(object):

    def __init__(self, graph, matcher, max_iterations=None, avoid_cycles=False):
        self.graph = graph
        self.matcher = matcher
        self.max_iterations = max_iterations
        self.avoid_cycles = avoid_cycles

    def analyze(self, node):
        self.explored = set()
        self.explored_edges = set()
        pointer = self.matcher.fsm.get_end_node()
        paths = [[(node, pointer, None)]]
        finished_paths = []

        i = 0
        while True:
            current_path = paths.pop(0)
            current_node = current_path[0][0]
            if self.avoid_cycles and current_node in self.explored:
                continue

            parents, edges = self.graph.get_node_parents(current_node, self.explored_edges)
            self.explored_edges |= set(edges)
            self.explored.add(current_node)

            self.matcher.backward_check(self.graph, current_path, parents, paths, finished_paths)

            i += 1
            if len(paths) == 0 or \
               (self.max_iterations != None and i > self.max_iterations):
                break
        return finished_paths
