import abc


class AbstractGraph(metaclass=abc.ABCMeta):

    @abc.abstractmethod
    def starting_nodes(self):
        pass

    @abc.abstractmethod
    def get_node_parents(self, node, explored_edges=set()):
        pass

    @abc.abstractmethod
    def get_node_children(self, node, explored_edges=set()):
        pass

    @abc.abstractmethod
    def get_node_labels(self, node):
        pass

    @abc.abstractmethod
    def get_node_anomaly_score(self, node):
        pass

    @abc.abstractmethod
    def get_node_matcher_state(self, node):
        pass

    @abc.abstractmethod
    def set_node_matcher_state(self, node, state):
        pass

    @abc.abstractmethod
    def clear_matcher_states(self):
        pass
