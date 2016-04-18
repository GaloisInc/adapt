import gremlinrestclient
from provn_segmenter import DocumentGraph, Document
from provnparser import ResourceFactory, EventFactory
import re
import argparse
import os
import copy


class TitanClient:
    def __init__(self, broker):
        self.broker = broker
        assert re.match('.+:\d+', args.broker),\
            'Broker must be in format url:port'
        self.client = gremlinrestclient.GremlinRestClient(self.broker)
        self.graph = gremlinrestclient.TitanGraph(self.broker)

    def add_edge(self, n1, d1, n2, d2, d, label):
        #assert 'name' not in d1, d1
        d1['name'] = n1
        #assert 'name' not in d2, d2
        d2['name'] = n2
        e = (d1, label, d2, d)
        self.graph.create(e)

    def add_node(self, n, d):
        assert 'name' not in d
        d['name'] = n
        self.graph.create(d)

    def load_from_document_graph(self, dg):
        for n1, n2 in dg.g.edges():
            d1 = dg.g.node[n1]
            d2 = dg.g.node[n2]
            d = dg.g.edge[n1][n2]
            label = d['type']
            self.add_edge(n1, d1,
                n2, d2, d, label)
            #print n1, n2
            #print d1, d2
            #print d

    def read_into_document_graph(self):
        doc = Document()
        node_id2name_map = {}
        resp = self.client.execute(
            "g.V()",
            bindings={})
        assert resp.status_code == 200
        for v in resp.data:
            print v
            d = v['properties']
            assert 'type' in d
            assert 'name' in d
            resource_id = d['name'][0]['value']
            node_id2name_map[v['id']] = resource_id
            resource_type = d['type'][0]['value']
            att_val_list = [
                (str(k), str(val[0]['value']))
                for (k, val) in d.items()
                if k not in ['name', 'type']]
            r = ResourceFactory.create(
                resource_type, resource_id, att_val_list)
            print r
            doc.expression_list.append(r)
        resp = self.client.execute(
            "g.E()",
            bindings={})
        assert resp.status_code == 200
        for v in resp.data:
            d = v['properties']
            assert 'type' in d
            event_type = d['type']
            event_timestamp = d['timestamp'] if 'timestamp' in d else None
            att_val_list = [
                (k, val)
                for (k, val) in d.items()
                if k not in ['timestamp', 'type']]
            e = EventFactory.create(
                event_type,
                node_id2name_map[v['outV']],
                node_id2name_map[v['inV']],
                att_val_list, event_timestamp)
            #print e
            doc.expression_list.append(e)
        return DocumentGraph(doc)

    def drop_db(self):
        resp = self.client.execute('g.V().drop().iterate()')
        assert resp.status_code == 200


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
            print 'File {0} does not exist...aborting'.format(f)

    assert re.match('.+:\d+', args.broker), 'Broker must be in format url:port'

    doc = Document()
    doc.parse_provn(args.provn_file)
    in_dg = DocumentGraph(doc)
    tc = TitanClient(args.broker)
    tc.drop_db()
    tc.load_from_document_graph(in_dg)
    out_dg = tc.read_into_document_graph()

    print 'LOADED INTO DB:'
    print in_dg.doc
    print 'READ FROM DB:'
    print out_dg.doc
