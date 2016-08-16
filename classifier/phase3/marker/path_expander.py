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
# usage:
#     ./path_expander.py "==>[v[11174008], v[4014240], v[8515824], v[8507632]]"
#
# bash -xc 'while read l; do marker/path_expander.py "$l"; done' < /tmp/db.txt
#
'''
Displays contents of several nodes in a .path() reported by gremlin.
'''
import argparse
import os
import pprint
import re
import sys
sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_query


class PathExpander:

    def __init__(self, gremlin, path):
        self.gremlin = gremlin
        self.path = path.strip('=>[]').replace(',', '')

    def get_node_ids(self):
        v_re = re.compile(r'^v\[(\d+)\]?$')
        for node in self.path.split():
            m = v_re.search(node)
            assert m, node
            yield int(m.group(1))

    def expand(self):
        pp = pprint.PrettyPrinter()
        for id_ in self.get_node_ids():
            query = 'g.V(%d).valueMap(true)' % id_
            print('')
            pp.pprint(gremlin.fetch_data(query))


def arg_parser():
    p = argparse.ArgumentParser(description='Displays contents of several'
                                ' nodes in a .path() reported by gremlin.')
    p.add_argument('path', help='line of gremlin output, e.g.'
                   ' ==>[v[11174008], v[4014240], v[8515824], v[8507632]]')
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    with gremlin_query.Runner() as gremlin:
        PathExpander(gremlin, args.path).expand()
