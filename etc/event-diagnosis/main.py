#! /usr/bin/env python
import gremlinrestclient as grc
from graphviz import Digraph

class DBClient:
    GREM_NODES = "g.V()"
    GREM_NODE = "g.V(node)"
    GREM_NODE_IN_EDGES = "%s.inE()" % GREM_NODE
    GREM_NODE_OUT_EDGES = "%s.outE()" % GREM_NODE

    def __init__(self, url='http://localhost:8182/'):
        self.client = grc.GremlinRestClient(url=url)
        self.nodes = {}
        for node in self.__query_generator(self.GREM_NODES):
            self.nodes[node['id']] = node

    def __query_generator(self, gremlin, bindings=None):
        for result in self.client.execute(gremlin, bindings).data:
            yield result

    def get_nodes(self):
        for _, node in self.nodes.viewitems():
            yield node

    def get_node(self, id):
        if id in self.nodes:
            return self.nodes[id]
        else:
            return None

    def get_out_edges(self, id):
        for edge in self.__query_generator(self.GREM_NODE_OUT_EDGES,
                                           {'node': id}):
            yield edge

    def get_in_edges(self, id):
        for edge in self.__query_generator(self.GREM_NODE_IN_EDGES,
                                           {'node': id}):
            yield edge


def symptom_func(suspicous_command_line):
    def func(client):
        for node in client.get_nodes():
            if 'commandLine' in node['properties']:
                commandLine = node['properties']['commandLine'][0]['value']
                if commandLine == suspicous_command_line:
                    return node['id']
    return func


def display_node(node_object):
    node_type = node_object['vertexType'][0]['value']
    if node_type == 'artifact':
        node_type = node_type + "\n" + node_object['coarseLoc'][0]['value']
    elif node_type == 'unitOfExecution':
        if 'commandLine' in node_object:
            node_type = node_type + "\n" + node_object['commandLine'][0]['value']
        elif 'programName' in node_object:
            node_type = node_type + "\n" + node_object['programName'][0]['value']
    return node_type

def trace_nodes(client, symptom, backtrace=True, dot=None):
    trace = []
    queue = [symptom]

    while len(queue):
        node = queue.pop(0)
        if node not in trace:
            node_object = client.get_node(node)['properties']
            if dot:
                dot.node(str(node), display_node(node_object))
            
            trace.append(node)

            if backtrace:
                for edge in client.get_out_edges(node):
                    in_node = edge['inV']
                    queue.append(in_node)
                    if dot:
                        dot.edge(str(node), str(in_node), label=edge['label'])
            else:
                for edge in client.get_in_edges(node):
                    out_node = edge['outV']
                    queue.append(out_node)
                    if dot:
                        dot.edge(str(out_node), str(node), label=edge['label'])
    return trace


def diagnose_events(client, symptom_func):
    symptom_id = symptom_func(client)
    print "Symptom: Node", symptom_id
    
    dot = Digraph()
    
    print "Back trace: ", trace_nodes(client, symptom_id, dot=dot)
    # print "Forward trace: ", trace_nodes(client, symptom_id, False)

    dot.render('visualizations/output.dot')

symptom1_node = symptom_func('cat /etc/ssh/bad-ls-key')
symptom2_node = symptom_func('ncat -u seaside.galois.com 31337')


if __name__ == '__main__':
    client = DBClient()
    diagnose_events(client, symptom1_node)
