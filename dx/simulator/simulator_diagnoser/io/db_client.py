import asyncio
import aiogremlin
import logging
import uuid

class DBClient(object):
    def __init__(self, url='http://localhost:8182/'):
        self.loop = asyncio.get_event_loop()
        self.client = aiogremlin.GremlinClient(url=url, loop=self.loop)
        self.log = logging.getLogger('dx-logger')

    def __del__(self):
        self.loop.run_until_complete(self.client.close())

    def __query(self, gremlin, bindings={}):
        self.log.debug("Query: " + gremlin + " bindings: " + str(bindings))
        r = self.client.execute(gremlin, bindings=bindings)
        msg = self.loop.run_until_complete(r)[0]
        return msg.data

    @staticmethod
    def generate_uuid():
        return str(uuid.uuid4())

    def get_nodes(self, **attributes):
        gremlin = "g.V()"
        for x in list(attributes.keys()):
            if x == 'label':
                gremlin += '.hasLabel(l)'
                attributes['l'] = attributes[x]
            else:
                gremlin += ".has('{}',{})".format(x,x)
        return self.__query(gremlin, attributes)

    def insert_node(self, node_label, **attributes):
        gremlin = "graph.addVertex(label, l" + \
                  ''.join(",'{}',{}".format(x,x) for x in attributes.keys()) + \
                  ')'
        attributes['l'] = node_label
        return self.__query(gremlin, attributes)[0]

    def drop_nodes(self, force=False, **attributes):
        if not force and len(attributes) == 0:
            return None
        gremlin = "g.V()"
        for x in list(attributes.keys()):
            if x == 'label':
                gremlin += '.hasLabel(l)'
                attributes['l'] = attributes[x]
            else:
                gremlin += ".has('{}',{})".format(x,x)
        gremlin += ".drop().iterate()"
        return self.__query(gremlin, attributes)

    def get_edges(self, **attributes):
        gremlin = "g.E()"
        for x in list(attributes.keys()):
            if x == 'label':
                gremlin += '.hasLabel(l)'
                attributes['l'] = attributes[x]
            else:
                gremlin += ".has('{}',{})".format(x,x)
        return self.__query(gremlin, attributes)

    def insert_edge(self, outnode, innode, edge_label, **attributes):
        gremlin = "graph.vertices({})[0].addEdge(l,".format(outnode) + \
                  "graph.vertices({})[0]".format(innode) + \
                  ''.join(",'{}',{}".format(x,x) for x in attributes.keys()) + \
                  ')'
        attributes.update({'l': edge_label})
        return self.__query(gremlin, attributes)

    def drop_edges(self, force=False, **attributes):
        if not force and len(attributes) == 0:
            return None
        gremlin = "g.E()"
        for x in list(attributes.keys()):
            if x == 'label':
                gremlin += '.hasLabel(l)'
                attributes['l'] = attributes[x]
            else:
                gremlin += ".has('{}',{})".format(x,x)
        gremlin += ".drop().iterate()"
        return self.__query(gremlin, attributes)

    def get_successors(self, node):
        gremlin = "g.V({}).out()".format(node)
        return self.__query(gremlin)

    def get_transitive_successors(self, node, **attributes):
        gremlin = "g.V({}).repeat(out()".format(node)
        for x in list(attributes.keys()):
            if x == 'label':
                gremlin += '.hasLabel(l)'
                attributes['l'] = attributes[x]
            else:
                gremlin += ".has('{}',{})".format(x,x)
        gremlin += ".dedup()).emit().simplePath().path()"
        return self.__query(gremlin, attributes)

    def get_predecessors(self, node):
        gremlin = "g.V({}).in()".format(node)
        return self.__query(gremlin)

    def get_transitive_predecessors(self, node, **attributes):
        gremlin = "g.V({}).repeat(in()".format(node)
        for x in list(attributes.keys()):
            if x == 'label':
                gremlin += '.hasLabel(l)'
                attributes['l'] = attributes[x]
            else:
                gremlin += ".has('{}',{})".format(x,x)
        gremlin += ".dedup()).emit().simplePath().path()"
        return self.__query(gremlin, attributes)

    def set_node_properties(self, node, **attributes):
        gremlin = "g.V({})".format(node)
        for x in attributes.keys():
            gremlin += ".property('{}',{})".format(x,x)
        return self.__query(gremlin, attributes)
