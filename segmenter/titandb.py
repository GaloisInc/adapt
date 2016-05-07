import asyncio
from aiogremlin import GremlinClient
from provn_segmenter import DocumentGraph, Document
from provnparser import ResourceFactory, EventFactory
import re
import argparse
import os
import logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class TitanClient:
    def __init__(self, broker):
        self.broker = broker
        assert re.match('.+:\d+', self.broker),\
            'Broker must be in format url:port'
        self.loop = asyncio.get_event_loop()
        self.gc = GremlinClient(url=broker, loop=self.loop)

    def close(self):
        self.loop.run_until_complete(self.gc.close())
        self.loop.close()

    def execute(self, gremlin_query_str, bindings={}):
        execute = self.gc.execute(gremlin_query_str, bindings=bindings)
        logger.debug('QUERY:\n {}'.format(gremlin_query_str))
        result = self.loop.run_until_complete(execute)
        assert result[0].status_code in (200, 204), result[0].status_code
        if result == 204:
            return None
        else:
            return result[0].data

    def all_edges(self):
        return self.execute('g.E()')

    def all_nodes(self):
        return self.execute('g.V()')

    def add_edge(self, n1, d1, n2, d2, d, label):
        """
        Adds edge with label and dict d between nodes with names n1 and n2, 
        if it does not exist.
        If the nodes do not exist they are created with dicts d1 and d2.
        """
        r1 = self.add_node(n1, d1)
        r2 = self.add_node(n2, d2)
        id1 = r1[0]['id']
        id2 = r2[0]['id']
        properties_str = ', '.join(
            map(lambda x: '\'{0}\',\'{1}\''.format(x[0], x[1]), d.items()))
        r = self.execute(
            'g.V({0}).next().addEdge(\'{2}\', g.V({1}).next(), {3})'.format(
                id1, id2, label, properties_str))
        return r

    def add_node(self, n, d):
        """
        Adds node with name n to the DB if it does not already exist.
        """
        d['name'] = n
        properties_str = ', '.join(
            map(lambda x: '\'{0}\',\'{1}\''.format(x[0], x[1]), d.items()))
        r = self.execute('g.V().has(\'name\', \'{}\')'.format(n))
        if not r:
            r = self.execute('g.addV({})'.format(properties_str))
            logger.debug('add_node: Added node with properties {}'.format(d))
        else:
            logger.debug(
                'add_node: Node with name {} already exists'.format(n))
        return r

    def load_from_document_graph(self, dg):
        for n1, n2 in dg.g.edges():
            d1 = dg.g.node[n1]
            d2 = dg.g.node[n2]
            d = dg.g.edge[n1][n2]
            label = d['type']
            self.add_edge(n1, d1,
                n2, d2, d, label)

    def read_into_document_graph(self):
        doc = Document()
        node_id2name_map = {}
        nodes = self.all_nodes()
        for v in nodes:
            d = v['properties']
            assert 'type' in d, d
            assert 'name' in d, d
            resource_id = d['name'][0]['value']
            node_id2name_map[v['id']] = resource_id
            resource_type = d['type'][0]['value']
            att_val_list = [
                (str(k), str(val[0]['value']))
                for (k, val) in d.items()
                if k not in ['name', 'type']]
            r = ResourceFactory.create(
                resource_type, resource_id, att_val_list)
            doc.expression_list.append(r)
        edges = self.all_edges()
        for e in edges:
            d = e['properties']
            assert 'type' in d
            event_type = d['type']
            event_timestamp = d['timestamp'] if 'timestamp' in d else None
            att_val_list = [
                (k, val)
                for (k, val) in d.items()
                if k not in ['timestamp', 'type']]
            ev = EventFactory.create(
                event_type,
                node_id2name_map[e['outV']],
                node_id2name_map[e['inV']],
                att_val_list, event_timestamp)
            doc.expression_list.append(ev)
        return DocumentGraph(doc)

    def drop_db(self):
        r = self.execute('g.V().drop().iterate()')
        assert r == None


def test():
    provn_file = 'test/test_james.provn'
    broker = 'ws://localhost:8182/'
    doc = Document()
    doc.parse_provn(provn_file)
    in_dg = DocumentGraph(doc)
    tc = TitanClient(broker)
    tc.drop_db()
    tc.load_from_document_graph(in_dg)
    out_dg = tc.read_into_document_graph()
    tc.close()

    print('LOADED INTO DB:')
    print(in_dg.doc)
    print('READ FROM DB:')
    print(out_dg.doc)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description='A simple wrapper around a Titan DB')
    parser.add_argument('provn_file', help='A prov-tc file in provn format')
    parser.add_argument('broker', help='Teh broker to the Titan DB')
    parser.add_argument('--verbose', '-v', action='store_true',
        help='Run in verbose mode')

    args = parser.parse_args()
    VERBOSE = args.verbose
    # Check that provided non-optional files actually exist
    for f in [args.provn_file]:
        if not (os.path.isfile(f)):
            print('File {0} does not exist...aborting'.format(f))

    assert re.match('.+:\d+', args.broker), 'Broker must be in format url:port'

    doc = Document()
    doc.parse_provn(args.provn_file)
    in_dg = DocumentGraph(doc)
    tc = TitanClient(args.broker)
    tc.drop_db()
    tc.load_from_document_graph(in_dg)
    out_dg = tc.read_into_document_graph()
    tc.close()

    print('LOADED INTO DB:')
    print(in_dg.doc)
    print('READ FROM DB:')
    print(out_dg.doc)
