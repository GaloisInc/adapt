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

__author__ = 'John.Hanley@parc.com'

import gremlinrestclient
import argparse
import collections
import os
import re


def get_nodes(db_client):
    sri_label_re = re.compile(r'^http://spade.csl.sri.com/#:[a-f\d]{64}$')

    resp = db_client.execute("g.V()")
    for node in resp.data:
        assert node['id'] >= 0, node
        assert node['type'] == 'vertex', node
        assert sri_label_re.search(node['label']), node
        yield node['properties']


def node_types(url, verbose=True):
    types = collections.defaultdict(int)
    files = []
    mandatory_attrs = set(['PID', 'UID', 'group', 'source', 'vertexType'])
    client = gremlinrestclient.GremlinRestClient(url=url)
    
    for node in get_nodes(client):
        # for k, v in sorted(node.items()):
        #     print(k, v)

        value = None

        if 'vertexType' in node:
            d = node['vertexType']    # List of dictionaries.
            assert len(d) == 1, node  # Well, ok, a list of just one dict.
            id = d[0]['id']
            value = d[0]['value']
            assert id >= 0, id
            types[value] += 1
            if value == 'unitOfExecution':
                # optional attributes: CWD, PPID, commandLine, programName
                for attr in mandatory_attrs:
                    assert attr in node, attr + str(node)

        if 'coarseLoc' in node:
            d = node['coarseLoc']
            assert len(d) == 1, node
            assert value == 'artifact', value
            assert len(d[0]) == 2, d[0]
            id = d[0]['id']
            value = d[0]['value']
            assert id >= 0, id
            assert value.startswith('/') or value.startswith('address:'), value
            files.append(value)

    if verbose:
        print('\n'.join(sorted(files)))
    return types


def arg_parser():
    p = argparse.ArgumentParser(
        description='Classify activities found in subgraphs of a SPADE trace.')
    p.add_argument('--db-url', help='Titan database location',
                   default='http://localhost:8182/')
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    types = node_types(args.db_url)
    print('=' * 70)
    print('')
    for k, v in sorted(types.items()):
        print('%5d  %s' % (v, k))
