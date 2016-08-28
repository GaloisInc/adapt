import graphviz
import networkx as nx
from simulator_diagnoser.matcher.strategies import CrispStrategy


class StatelessMatcher(object):
    def __init__(self, grammar, strategy=CrispStrategy()):
        self.fsm = StateMachine()
        self.fsm.add(grammar)
        self.strategy = strategy

    def state_score(self, state, pointer=None):
        if not state:
            return 0.0
        if not pointer:
            pointer = self.fsm.get_end_node()
        return state[pointer]

    def backward_check(self, state, pointer, parent_states):
        score = self.state_score(state, pointer)

        for parent, pstate in parent_states:
            parent_score = self.state_score(pstate, pointer)
            if parent_score >= score:
                return pointer, parent, pstate

        pointers = self.fsm.predecessors(pointer)
        if len(pointers) > 0:
            best_score = 0.0
            next_parent = None
            next_state = None
            next_pointer = None

            for po in pointers:
                if po != self.fsm.get_start_node():
                    for pa, ps in parent_states:
                        parent_score = self.state_score(ps, po)
                        if parent_score > best_score:
                            next_parent = pa
                            next_state = ps
                            next_pointer = po

            return next_pointer, next_parent, next_state

        return None, None, None

    def match(self, graph, node, parents):
        node_state = graph.get_node_matcher_state(node)
        parent_states = []
        for p in parents:
            p_state = graph.get_node_matcher_state(p)
            if p_state:
                parent_states.append(p_state)
        if not parent_states:
            parent_states = [self.fsm.initial_state()]

        labels = graph.get_node_labels(node)
        anomaly_score = graph.get_node_anomaly_score(node)

        for label, confidence in labels:
            node_state = \
                self.match_state(label, node_state, parent_states, confidence, anomaly_score)

        if node_state:
            parent_states.append(node_state)
        new_state = self.strategy.reduce_states(parent_states)
        graph.set_node_matcher_state(node, new_state)
        return self.state_score(new_state)

    def match_state(self, label, state=None, parent_states=None, confidence=1.0, anomaly=0.0):
        if not parent_states:
            parent_states = [self.fsm.initial_state()]

        if state:
            parent_states.append(state)

        parent = self.strategy.reduce_states(parent_states)
        nodes = [i for (i, score) in enumerate(parent) if self.strategy.to_explore(score)]

        states = []
        for i in nodes:
            states.append(self.match_node([label], parent, i, confidence, anomaly))

        if len(states) == 0:
            return parent
        return self.strategy.reduce_states(states)

    def match_node(self, node_labels, state, node, confidence, anomaly):
        node_score = state[node]
        if not self.strategy.to_explore(node_score):
            return state

        edges = self.fsm.get_edges(node)

        for next_node, _, weight in edges:
            state = self.strategy.propagate_jumps(state, node, next_node, weight)

        next_states = []
        for next_node, edge_label, _ in edges:
            new_node_labels = node_labels
            new_state = state
            if edge_label in node_labels:
                new_node_labels = [n for n in node_labels if n != edge_label]
                new_state = self.strategy.update_score(state, next_node, node_score, confidence)

            next_states.append(\
                self.match_node(new_node_labels, new_state, next_node, confidence, anomaly))

        if len(next_states) == 0:
            return state
        return self.strategy.reduce_states(next_states)

    def generate_dot(self):
        return self.fsm.generate_dot()

class StateMachine(object):
    def __init__(self):
        self.clear()

    def clear(self):
        self.__fsm = nx.DiGraph()
        self.start_node = self.end_node = self.add_node(name='startnode', end=True)

    def predecessors(self, node):
        return self.__fsm.predecessors(node)

    def get_start_node(self):
        return self.start_node

    def get_end_node(self):
        return self.end_node

    def add_node(self, **attr):
        number = self.__fsm.number_of_nodes()
        self.__fsm.add_node(number, **attr)
        return number

    def add_edge(self, i, o, label='', weight=1, rule=None):
        if self.__fsm.has_edge(i, o):
            data = self.__fsm.get_edge_data(i,o,default={})
            data.get('label', []).append((label, weight, rule))
        else:
            self.__fsm.add_edge(i, o, label=[(label, weight, rule)])

    def get_edges(self, node):
        ret = []
        for _, out, data in self.__fsm.out_edges(node, data=True):
            for label, weight, _ in data.get('label', []):
                ret.append((out, label, weight))
        return ret

    def add(self, rule, node=None):
        if node == None:
            node = self.start_node

        self.__fsm.node[self.end_node]['end'] = False
        self.end_node = self.add_node()
        rule.generate_fsm(self, node, self.end_node)
        self.__fsm.node[self.end_node]['end'] = True
        return self.end_node

    def number_of_nodes(self):
        return self.__fsm.number_of_nodes()

    def initial_state(self):
        state = [0.0] * self.number_of_nodes()
        state[self.start_node] = 1.0
        return state

    def generate_dot(self):
        dot = graphviz.Digraph(graph_attr={'fontname': 'sans-serif',
                                           'rankdir': 'LR'},
                               node_attr={'margin': '0',
                                          'fontsize': '6',
                                          'fontname': 'sans-serif'})
        for n, node in self.__fsm.nodes_iter(data=True):
            label = str(n)
            if node.get('end', False):
                label += '\nEnd'
            dot.node(str(n), label)

        for i, o, data in self.__fsm.edges_iter(data=True):
            labels = data.get('label', [])
            for label in labels:
                dot.edge(str(i), str(o), label=str(label[:-1]))

        return dot
