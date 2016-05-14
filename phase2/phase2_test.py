#! /usr/bin/env python3

# Copyright 2016, Palo Alto Research Center.
# Developed with sponsorship of DARPA.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# The software is provided "AS IS", without warranty of any kind, express or
# implied, including but not limited to the warranties of merchantability,
# fitness for a particular purpose and noninfringement. In no event shall the
# authors or copyright holders be liable for any claim, damages or other
# liability, whether in an action of contract, tort or otherwise, arising from,
# out of or in connection with the software or the use or other dealings in
# the software.
#
'''
Writes one or more classification nodes to Titan / Cassandra.
'''

import aiogremlin
import asyncio
import argparse
import classify
import logging
import re
import sys
import uuid

__author__ = 'John.Hanley@parc.com'

log = logging.getLogger(__name__)
log.addHandler(logging.StreamHandler())
log.setLevel(logging.DEBUG)


@asyncio.coroutine
def stream(db_client, query):
    resp = yield from db_client.submit(query)
    while True:
        result = yield from resp.stream.read()
        if result is None:
            break
        return result.data


class NodeInserter:

    def __init__(self, url='http://localhost:8182/'):
        self.loop = asyncio.get_event_loop()
        self.db_client = aiogremlin.GremlinClient(url=url, loop=self.loop)

    def __del__(self):
        self.loop.run_until_complete(self.db_client.close())

    def _quote(self, x):
        '''Double quote x if it is a string.
        >>> ins = NodeInserter()
        >>> ins._quote(3)
        3
        >>> ins._quote('hi')
        '"hi"'
        >>> ins._quote('say "hi"')
        '"say "hi""'
        '''
        if isinstance(x, str):
            x = '"%s"' % x.replace('"', '"')
        return x

    def _label_value(self):
        return str(uuid.uuid4())[:13]

    def _insert_node(self, label, vertex_type, *av_pairs):
        # Sigh. Omitting 'label' makes it match the 'type' value: 'vertex'.
        assert vertex_type in 'vertex segment classification'.split(), vertex_type
        bindings = {'p1': label, 'p2': vertex_type}
        gremlin = ("graph.addVertex(label, p1,"
                   " 'vertexType', p2")
        for (attr, val) in av_pairs:
            gremlin += ", '%s', %s" % (attr, self._quote(val))
        gremlin += ')'
        log.debug(gremlin)

        resp = self.db_client.execute(gremlin, bindings=bindings)
        self.loop.run_until_complete(resp)

    def _insert_reqd_events(self):
        '''Pretend that In established necessary DB pre-condition.'''
        # We used to see audit records like this from SPADE, but no longer do.
        # The spelling is not important, we can readily change that.
        # What matters is the vertex has attributes giving filename details.
        self._insert_node('spade1', 'vertex',
                          ('fileVersion', -1),
                          ('type', 'file'),
                          ('url', '/usr/bin/ls'))
        self._insert_node('spade2', 'vertex',
                          ('filename', 'audit:path=/etc/shadow'),
                          ('audit_subtype', 'file'))

    def _insert_reqd_segment(self):
        '''Pretend that Se established necessary DB pre-condition.'''
        for node_name in 'spade1 spade2'.split():
            self._insert_node('seg1', 'segment',
                              ('segmentContains', node_name))

    def _get_segment(self, seg_name):
        ret = []
        gremlin = 'graph.traversal().V().has("vertexType", "vertex")'
        gremlin = 'graph.traversal().V().has(label, "spade2")'
        bindings = {'p1': seg_name}  # revise query to get all in segment p1
        resp = self.db_client.execute(gremlin, bindings=bindings)
        for node in self.loop.run_until_complete(resp):
            if node.data is not None:
                ret.append(node.data[0])
        print(len(ret), ret)
        return ret


    def _drop_matching_nodes(self, label):
        gremlin = 'graph.traversal().V().has(label, p1).drop().iterate()'
        bindings = {'p1': label}
        resp = self.db_client.execute(gremlin, bindings=bindings)
        self.loop.run_until_complete(resp)

    def _drop_all_test_nodes(self):
        for label in 'spade1 spade2 seg1 ac1'.split():
            self._drop_matching_nodes(label)


    def phase2(self):
        '''Test Ac component in isolation, for phase1 development.'''
        exfil_detect = classify.ExfilDetector()
        self._drop_all_test_nodes()

        # precondition
        assert False == exfil_detect.is_exfil_segment(self._get_segment('seg1'))

        self._insert_reqd_events()
        self._insert_reqd_segment()

        # postcondition
        if exfil_detect.is_exfil_segment(self._get_segment('seg1')):
            self._insert_node('ac1', 'classification',
                              ('classificationType', 'exfiltrate_sensitive_file'))
        else:
            assert None, 'phase1 test failed'



def arg_parser():
    p = argparse.ArgumentParser(
        description='Writes one or more nodes to Titan / Cassandra.')
    p.add_argument('--db-url', help='Titan database location',
                   default='http://localhost:8182/')
    return p


# Invoke with:
#   $ nosetests3 *.py
def test_insert():
    ins = NodeInserter()
    ins.phase2()


if __name__ == '__main__':
    args = arg_parser().parse_args()
    ins = NodeInserter(args.db_url)
    ins.phase2()
