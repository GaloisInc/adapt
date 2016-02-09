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

import graphviz
import gremlinrestclient
import argparse
import classify
import collections
import logging
import re

__author__ = 'John.Hanley@parc.com'

log = logging.getLogger(__name__)
log.addHandler(logging.StreamHandler())  # log to console (stdout)
log.setLevel(logging.INFO)


def get_nodes(db_client):
    '''Returns the interesting part of each node (its properties).'''

    sri_or_adapt_label_re = re.compile(
        r'^http://spade.csl.sri.com/#:[a-f\d]{64}$'
        '|^classification$'
        '|^aide\.db_/'
        '|^vendor_hash_/')

    nodes = db_client.execute("g.V()").data
    for node in nodes:
        assert node['type'] == 'vertex', node
        assert node['id'] >= 0, node
        assert sri_or_adapt_label_re.search(node['label']), node
        yield node['properties']


def dump(node):
    print('')
    for k, v in sorted(node.items()):
        print(k, v)


def report(db_url, verbose=False):
    valid_sources = set(['/dev/audit', '/proc'])
    client = gremlinrestclient.GremlinRestClient(url=db_url)
    fs = classify.FsProxy(client, 'aide.db_')
    assert fs.is_locked_down('/usr/include')
    assert not fs.is_locked_down('/tmp')

    for node in get_nodes(client):

        if verbose:
            dump(node)

        if 'source' in node:
            assert node['source'][0]['value'] in valid_sources, node

        if 'label' in node:
            print(node['label'])

        assert 'vertexType' in node, node

        if node['vertexType'][0]['value'] == 'aide':
            name = node['name'][0]['value']
            if fs.stat(name)['uid'] > 0:
                print('%7d  %s' % (fs.stat(name)['uid'], name))


def arg_parser():
    p = argparse.ArgumentParser(
        description='Classify activities found in subgraphs of a SPADE trace.')
    p.add_argument('--db-url', help='Titan database location',
                   default='http://localhost:8182/')
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    report(**vars(args))
