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
Reports on node types stored in a Titan graph.
'''

import aiogremlin
import argparse
import asyncio
import collections
import graphviz
import logging
import re

__author__ = 'John.Hanley@parc.com'

log = logging.getLogger(__name__)
log.addHandler(logging.StreamHandler())  # log to console (stdout)
log.setLevel(logging.INFO)


class ProcessGraphNodes:
    '''Maintains a collection of processes we have seen.'''

    def __init__(self):
        self.ops = collections.defaultdict(int)  # PID to num. FS operations
        self.edges = set()  # Used for pruning multi-edges down to just one.
        self.nodes = {}  # Maps process id to process name.

    def was_seen(self, pid):
        '''We have seen a certain process, so make a note of it.'''
        if pid not in self.nodes:
            # pre-existing parent of unknown name
            self.nodes[pid] = 'parent_%d' % int(pid)

    def has_name(self, pid, name):
        '''Note the name of a process.'''
        name = re.sub(r'-$', '', name)
        self.nodes[pid] = '%s_%s' % (name, int(pid))
        return self.nodes[pid]

    def has_parent(self, pid, ppid, dot):
        dot.node(pid, self.nodes[pid])
        dot.node(ppid, self.nodes[ppid])

        # Avoid multi-edges.
        pair = (ppid, pid)
        if pair not in self.edges:
            self.edges.add(pair)
            dot.edge(ppid, pid)


@asyncio.coroutine
def stream(db_client, query):
    resp = yield from db_client.submit(query)
    while True:
        result = yield from resp.stream.read()
        if result is None or result.data is None:
            break
        return result.data


def db_results(db_client, query):
    message = loop.run_until_complete(stream(db_client, query))
    if message is not None:
        for node in message:
            yield node


valid_edge_types = set(['used', 'wasGeneratedBy', 'wasInformedBy'])


def edge_types(db_client):
    label_256bit_re = re.compile(r'^http://spade.csl.sri.com/#:[a-f\d]{64}$')
    in_labels = collections.defaultdict(int)
    out_labels = collections.defaultdict(int)
    types = collections.defaultdict(int)
    valid_operations = set(['read', 'recvfrom', 'recvmsg'])
    # count = db_client.execute('g.E().count()').data[0]
    # assert count > 0, count
    for edge in db_results(db_client, 'g.E()'):
        if 'properties' in edge:
            d = edge['properties']
            assert len(d) == 2, d
            assert 'atTime' in d, d     # e.g. '2015-09-28 01:06:56 UTC'
            assert 'operation' in d, d  # e.g. 'read'
            assert d['operation'] in valid_operations, d
        assert edge['type'] == 'edge', edge
        assert edge['id'] >= 0, edge
        assert edge['inV'] >= 0, edge
        assert edge['outV'] >= 0, edge
        assert label_256bit_re.search(edge['inVLabel'])
        assert label_256bit_re.search(edge['outVLabel'])
        in_labels[edge['inVLabel']] += 1
        out_labels[edge['inVLabel']] += 1
        typ = edge['label']
        assert typ in valid_edge_types, edge
        types[typ] += 1
    assert len(in_labels) == len(out_labels)
    # print(len(in_labels), sorted(in_labels.values()))
    assert sorted(in_labels.values()) == sorted(out_labels.values())
    return types


def get_nodes(db_client):
    '''Returns the interesting part of each node (its properties).'''

    # edges = list(db_client.execute("g.E()").data)
    # assert len(edges) > 0, len(edges)

    sri_or_adapt_label_re = re.compile(
        r'^http://spade.csl.sri.com/#:[a-f\d]{64}$'
        '|^vertex$'
        '|^classification$'
        '|^aide\.db_/'
        '|^vendor_hash_/')

    for node in loop.run_until_complete(stream(db_client, "g.V()")):
        assert node['type'] == 'vertex', node
        assert node['id'] >= 0, node
        assert sri_or_adapt_label_re.search(node['label']), node
        yield node['properties']


def node_types(client, name='infoleak', edge_type='wasInformedBy'):
    direction = {'rankdir': 'LR'}
    dot = graphviz.Digraph(format='pdf', graph_attr=direction,
                           name='PG_%s' % edge_type)
    pg_nodes = ProcessGraphNodes()
    valid_sources = set(['/dev/audit', '/proc'])
    valid_vertex_types = set(['aide', 'artifact', 'unitOfExecution'])
    coarse_loc_re = re.compile(
        r'^(/|stdout|address:|anon_inode:|pipe:|socket:)')
    types = collections.defaultdict(int)
    files = []
    root_pids = set([1])  # init, top-level sshd, systemd, launchd, etc.

    for node in get_nodes(client):

        # for k, v in sorted(node.items()):
        #     print(k, v)

        # Sadly, value now sometimes looks like '8iyv-ajaw-b2d'.
        # if 'source' in node:
        #     assert node['source'][0]['value'] in valid_sources, node

        if 'PPID' in node and 'programName' in node:
            pid = node['PID'][0]['value']
            ppid = node['PPID'][0]['value']
            if int(ppid) in root_pids:
                assert node['vertexType'][0]['value'] == 'unitOfExecution'
            pg_nodes.was_seen(ppid)
            pg_nodes.has_name(pid, node['programName'][0]['value'])  # e.g.pool
            pg_nodes.has_parent(pid, ppid, dot)

        value = None

        if 'vertexType' in node:
            d = node['vertexType']    # List of dictionaries.
            assert len(d) == 1, node  # Well, ok, a list of just one dict.
            id = d[0]['id']
            value = d[0]['value']
            assert id >= 0, id
            assert value in valid_vertex_types, value
            types[value] += 1
            # if value == 'unitOfExecution':
            #     optional attributes: CWD, PPID, commandLine, programName

        if 'coarseLoc' in node:
            d = node['coarseLoc']
            assert len(d) == 1, node
            assert value == 'artifact', value
            assert len(d[0]) == 2, d[0]
            id = d[0]['id']
            value = d[0]['value']
            assert id >= 0, id
            assert coarse_loc_re.search(value), value
            files.append(value)

        if 'commandLine' in node:
            assert 'programName' in node, node
            # print(node['commandLine'][0]['value'])

    for edge in db_results(client, "g.E()"):
        typ = edge['label']
        if edge_type == typ:
            in_v = lookup(client, edge['inV'])
            out_v = lookup(client, edge['outV'])
            if in_v and out_v:
                dot.edge(in_v, out_v,
                         style='dashed', color='blue', constraint='false')
                if out_v.startswith('/'):
                    dot.node(out_v, color='white')
                if in_v.startswith('/'):
                    dot.node(in_v, color='white')
                    log.debug('%s %s' % (out_v, in_v))

    dot.render(directory='/tmp')
    log.debug('\n'.join(sorted(files)))
    return types


def lookup(client, id):
    assert None, 'lookup is unused'
    ret = {}
    query = 'g.V(%d)' % id
    for node in client.execute(query).data:
        assert node['type'] == 'vertex', node
        log.debug(repr(sorted(node['properties'].items())))
        ret = node['properties']
    if 'programName' in ret:
        return ret['PID'][0]['value']
    if 'vertexType' in ret:
        assert ret['vertexType'][0]['value'] in ['artifact',
                                                 'unitOfExecution'], ret
        return ret['vertexType'][0]['value']
    if 'coarseLoc' in ret:
        loc = ret['coarseLoc'][0]['value']
        if loc.startswith('address:'):
            loc = loc.replace(' ', '')
            loc = loc.replace(':', '_')
            loc = loc.replace(',', '_')
            loc = '/' + loc
        assert ' ' not in loc, loc
        return loc
    assert None, ret  # pragma: no cover


def arg_parser():
    p = argparse.ArgumentParser(
        description='Classify activities found in subgraphs of a SPADE trace.')
    p.add_argument('--db-url', help='Titan database location',
                   default='http://localhost:8182/')
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    db_client = aiogremlin.GremlinClient(url=args.db_url)
    loop = asyncio.get_event_loop()  # global, paired with db_client
    for k, v in sorted(edge_types(db_client).items()):
        print('%5d  %s' % (v, k))
    print('')

    types = {}
    for edge_type in sorted(valid_edge_types):
        types = sorted(node_types(db_client, edge_type=edge_type).items())
    print('=' * 70)
    print('')
    for k, v in types:
        print('%5d  %s' % (v, k))
    loop.run_until_complete(db_client.close())
    loop.close()
