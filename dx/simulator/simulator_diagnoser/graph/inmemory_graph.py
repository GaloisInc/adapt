import networkx as nx
from .abstract_graph import *


class InmemoryStatesMixin(object):
    def __init__(self):
        self.clear_matcher_states()

    def get_node_matcher_state(self, node):
        return self.states.get(node, None)

    def clear_matcher_states(self):
        self.states = {}


class InmemoryGraph(InmemoryStatesMixin, AbstractGraph):
    def __init__(self):
        self.clear()

    def clear(self):
        self.__G = nx.DiGraph()

    def add_edge(self, u, v):
        self.__G.add_edge(u, v)

    def add_node(self, n):
        self.__G.add_node(n)

    def starting_nodes(self):
        nodes = []
        for n in self.__G.nodes_iter():
            if len(self.__G.predecessors(n)) == 0:
                nodes.append(n)
        return nodes

    def get_node_parents(self, node, explored_edges=set()):
        return self.__G.predecessors(node)

    def get_node_children(self, node, explored_edges=set()):
        return self.__G.successors(node), []
