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
Writes one or more nodes to Titan / Cassandra.
'''

import sys
sys.path.extend(['../titan', 'titan'])
import aiogremlin
import asyncio
import argparse
import classify
import logging
import re

__author__ = 'John.Hanley@parc.com'

log = logging.getLogger(__name__)
log.addHandler(logging.StreamHandler())
log.setLevel(logging.INFO)


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

    def _insert_node(self, unique_id, vertex_type,
                     attr1, val1, attr2, val2):
        assert vertex_type in 'vertex segment classification'.split()
        bindings = {'p1': unique_id, 'p2': unique_id, 'p3': vertex_type,
                    'p4': val1, 'p5': val2}
        gremlin = ("graph.addVertex(label, p1,"
                   " 'name', p2,"
                   " 'vertexType', p3")
        if attr1 is not None:
            gremlin += ", %s, p4" % attr1
        if attr2 is not None:
            gremlin += ", %s, p5" % attr2
        gremlin += ')'
        log.debug(gremlin)
        loop = asyncio.get_event_loop()

        resp = self.db_client.execute(gremlin, bindings=bindings)

    def _insert_reqd_events(self):
        '''Pretend that In established necessary DB pre-condition.'''
        # We used to see audit records like this from SPADE, but no longer do.
        # The spelling is not important, we can readily change that.
        # What matters is the vertex has attributes giving filename details.
        self._insert_node('spade1', 'vertex',
                          'audit_tmpl', 'audit:path="/etc/shadow"',
                          'audit:subtype', '"file"')

    def _insert_reqd_segment(self):
        '''Pretend that Se established necessary DB pre-condition.'''
        self._insert_node('seg1', 'segment',
                          'includes', 'spade1', None, None)

    def _get_segment(self, seg_name):
        return ''

    def phase1(self):
        '''Test Ac component in isolation, for phase1 development.'''
        self._insert_reqd_events()
        self._insert_reqd_segment()
        exfil_detect = classify.ExfilDetector()
        if exfil_detect.is_exfil(self._get_segment('seg1')):
            self._insert_node('ac1', 'classification'
                              'classificationType', 'exfiltrate_sensitive_file')
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
    ins.phase1()


if __name__ == '__main__':
    args = arg_parser().parse_args()
    ins = NodeInserter(args.db_url)
    ins.phase1()
