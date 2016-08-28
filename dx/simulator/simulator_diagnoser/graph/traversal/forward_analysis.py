
class ForwardAnalysis(object):

    def __init__(self, graph):
        self.graph = graph

    def __iter__(self):
        self.queue = []

        self.explored = set()
        self.explored_edges = set()

        if self.graph:
            self.queue = self.graph.starting_nodes()
        return self

    def __next__(self):
        if len(self.queue) == 0:
            raise StopIteration

        node = self.queue.pop(0)
        parents, parent_edges = self.graph.get_node_parents(node, self.explored_edges)

        for parent in parents:
            if parent not in self.explored:
                return self.__next__()

        children, edges = self.graph.get_node_children(node, self.explored_edges)

        for child in children:
            if child not in self.queue:
                self.queue.append(child)

        self.explored.add(node)
        self.explored_edges |= set(edges)
        self.explored_edges |= set(parent_edges)

        return node, parents
