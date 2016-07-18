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
'''
Displays number of occurences of each distinct node label.
'''

import argparse
import gremlin_query

__author__ = 'John.Hanley@parc.com'


def get_label_counts(with_edges=False):
    '''Queries titan with read throughput of ~2700 node/sec.'''
    queries = ['g.V().groupCount().by(label())']
    if with_edges:
        queries.append('g.E().groupCount().by(label())')

    cnt = {}
    with gremlin_query.Runner() as gremlin:
        for query in queries:
            for msg in gremlin.fetch(query):
                if msg.data:
                    assert len(msg.data) == 1
                    cnt.update(msg.data[0])

    cnt['total'] = sum(cnt.values())

    return sorted(['%6d  %s' % (cnt[k], k)
                   for k in cnt.keys()])


def arg_parser():
    p = argparse.ArgumentParser(
        description='Reports on number of distinct labels (and edges).')
    p.add_argument('--with-edges', action='store_true',
                   help='report on edges, as well')
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    print('\n'.join(get_label_counts(args.with_edges)))
