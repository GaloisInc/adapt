#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
    =======
    `TitanClient`
    =======

    Adria Gascon, 2016.
"""
from aiogremlin import GremlinClient
from provn_segmenter import DocumentGraph, Document
from provnparser import ResourceFactory, EventFactory
import argparse
import asyncio
import logging
import os
import pprint
import re
import sys

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def escape(s):
    """
    A simple-minded function to escape strings for use in Gremlin queries.
    Replaces qutes with their escaped versions.  Other escaping
    may be necessary to avoid problems.
    """
    return s.replace("\'", "\\\'").replace("\"", "\\\"")


class TitanClient:
    def __init__(self, broker='ws://localhost:8182/'):
        self.broker = broker
        self.loop = asyncio.get_event_loop()
        self.gc = GremlinClient(url=broker, loop=self.loop)

    def close(self):
        self.loop.run_until_complete(self.gc.close())
        self.loop.close()


    def execute(self, gremlin_query_str, bindings={}):
        @asyncio.coroutine
        def stream(gc):
            result_data = []
            resp = yield from gc.submit(gremlin_query_str, bindings=bindings)
            while True:
                result = yield from resp.stream.read()
                if result is None:
                    break
                assert result.status_code in [206, 200, 204], result.status_code
                if not result.status_code == 204:
                    result_data += result.data
            return result_data
        result = self.loop.run_until_complete(stream(self.gc))
        return result

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
            map(lambda x: '\'{0}\',\'{1}\''.format(x[0], x[1])
                if x[0] != 'label' else "", d.items()))
        r = self.execute(
            'g.V({0}).next().addEdge(\'{2}\', g.V({1}).next(), {3})'.format(
                id1, id2, label, properties_str))
        return r

    def add_node(self, n, d):
        """
        Adds node with name n to the DB if it does not already exist.
        A node with name n and dictionary d exists in the DB if
          c1) there is already a node with name n in the db, or
          c2) there is already a node with dictionary d in the db
        """
        d['ident'] = n
        c1 = self.execute('g.V().has(\'ident\', \'{}\')'.format(n))
        properties_str = ', '.join(
            map(lambda x: '__().has(\'{0}\',\'{1}\')'.format(
                x[0], escape(x[1])), d.items()))
        c2 = self.execute('g.V().and({0})'.format(properties_str))
        if not (c1 or c2):
            properties_str = ', '.join(
                map(lambda x: '\'{0}\',\'{1}\''.format(
                    x[0], escape(x[1])), d.items()))
            # Hack: for now, label all new nodes as segment nodes.
            c1 = self.execute(
                'g.addV(label,\'Segment\',{})'.format(properties_str))
            assert 'ident' in d, d
            logger.debug('add_node: Added node with properties {}'.format(d))
        else:
            if c1:
                logger.debug(
                    'add_node: Node with name {} already exists'.format(n))
            if c2:
                logger.debug(
                    'add_node: Node with dict {} already exists'.format(d))
        return c1

    def load_from_document_graph(self, dg):
        for n1, n2 in dg.g.edges():
            d1 = dg.g.node[n1]
            d2 = dg.g.node[n2]
            d = dg.g.edge[n1][n2]
            label = d['label']
            self.add_edge(n1, d1,
                          n2, d2, d, label)

    def load_segments_from_document_graph(self, dg):
        """
        Given a document graph dg, add edges (and any missing
        nodes) generated by segmentation
        """
        for n1, n2 in dg.g.edges():
            d1 = dg.g.node[n1]
            d2 = dg.g.node[n2]
            d = dg.g.edge[n1][n2]
            label = d['label']
            if label.startswith('segment:'):
                self.add_edge(n1, d1,
                              n2, d2, d, label)

    def read_into_document_graph(self):
        doc = Document()
        node_id2name_map = {}
        nodes = self.all_nodes()
        if not nodes:
            return DocumentGraph(doc)
        for v in nodes:
            d = v['properties']
            assert 'label' in v, v
            assert 'ident' in d, d
            resource_id = d['ident'][0]['value']
            logger.info('%9d  %s' % (int(v['id']), resource_id))
            node_id2name_map[v['id']] = resource_id
            resource_type = v['label']
            att_val_list = [
                (str(k), str(val[0]['value']))
                for (k, val) in d.items()
                if k not in ['ident', 'label']]
            r = ResourceFactory.create(
                resource_type, resource_id, att_val_list)
            doc.expression_list.append(r)
        pprint.pprint(node_id2name_map)
        edges = self.all_edges()
        for e in edges:
            event_type = e['label']
            ev = EventFactory.create(
                event_type,
                node_id2name_map.get(e['outV'], None),
                node_id2name_map.get(e['inV'], None))
            doc.expression_list.append(ev)
        return DocumentGraph(doc)

    def drop_db(self):
        r = self.execute('g.V().drop().iterate()')
        assert r is None


def test():
    sys.stderr.write('*' * 30 + '\n')
    sys.stderr.write('Running PROVN Segmenter Tests\n')
    sys.stderr.write('*' * 30 + '\n')
    provn_file = 'test/test_james.provn'
    broker = 'ws://localhost:8182/'
    doc = Document()
    sys.stderr.write('---> Parsing {0}\n'.format(provn_file))
    doc.parse_provn(provn_file)
    in_dg = DocumentGraph(doc)
    tc = TitanClient(broker)
    tc.drop_db()
    sys.stderr.write('---> Loading {0} into DB\n'.format(provn_file))
    tc.load_from_document_graph(in_dg)
    sys.stderr.write('---> Reading {0} from DB\n'.format(provn_file))
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
    parser.add_argument('broker', help='The broker to the Titan DB')
    parser.add_argument('--verbose', '-v', action='store_true',
                        help='Run in verbose mode')

    args = parser.parse_args()
    VERBOSE = args.verbose
    # Check that provided non-optional files actually exist
    for f in [args.provn_file]:
        if not (os.path.isfile(f)):
            print('File {0} does not exist...aborting'.format(f))

    assert not args.broker or re.match(
        '.+:\d+', args.broker), 'Broker must be in format url:port'

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
