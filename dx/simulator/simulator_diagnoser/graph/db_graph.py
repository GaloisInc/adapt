import sys
import os

from .abstract_graph import *
from .inmemory_graph import *

sys.path.append(os.path.expanduser('~/adapt/pylib/'))
from titanDB import TitanClient

class DBGraph(InmemoryStatesMixin, AbstractGraph):

    def __init__(self, url='http://localhost:8182/'):
        self.db = TitanClient(broker=url)

    def __del__(self):
        self.db.close()

    def starting_nodes(self):
        query = "g.V().has('segment:name','byPID').where(__.not(inE('segment:edge'))).id()"
        result = self.db.execute(query)
        return result

    def get_node_parents(self, node, explored_edges=set()):
        query = "g.V(node).inE('segment:edge')"
        result = self.db.execute(query, {'node': node})

        parents, edges = [], []
        for elem in result:
            if elem['id'] not in explored_edges:
                edges.append(elem['id'])
                parents.append(elem['outV'])
        return parents, edges

    def get_node_children(self, node, explored_edges=set()):
        query = "g.V(node).outE('segment:edge')"
        result = self.db.execute(query, {'node': node})

        children, edges = [], []
        for elem in result:
            if elem['id'] not in explored_edges:
                edges.append(elem['id'])
                children.append(elem['inV'])
        return children, edges

    def get_node_labels(self, node):
        query = "g.V(node).out('segment:activity')"
        result = self.db.execute(query, {'node': node})
        labels = []
        for act in result:
            if 'properties' in act:
                labels.append((act['properties']['activity:type'][0]['value'],
                               act['properties']['activity:suspicionScore'][0]['value']))
        return labels

    def get_node_anomaly_score(self, node):
        query = "g.V(node)"
        result = self.db.execute(query, {'node': node})
        for n in result:
            if 'properties' in n and 'anomalyScore' in n['properties']:
                return n['properties']['anomalyScore']
        return None

    def store_diagnoses(self, path):
        query = "graph.addVertex(label,'APT')"
        result = self.db.execute(query)

        print(result)
        apt_id = None
        for n in result:
            apt_id = n['id']

        if apt_id != None:
            i = 0
            for node, _, label in path:
                if label:
                    query = "p = graph.addVertex(label, 'Phase', 'phase:name', phase);" \
                            "g.V(apt).next().addEdge('apt:includes', p, 'phase:order', order);" \
                            "p.addEdge('phase:includes', g.V(node).next())"
                    result = self.db.execute(query, {'apt': apt_id,
                                                     'phase': label,
                                                     'node': node,
                                                     'order': i})
                    print(result)
                    i += 1

    def clear_diagnoses(self):
        query = "g.V().hasLabel('APT').drop().iterate();"\
                "g.V().hasLabel('Phase').drop().iterate();"
        result = self.db.execute(query)
