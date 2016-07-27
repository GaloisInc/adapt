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
#
'''
Identifies a path between a pair of vertices.
'''
import argparse
import os
import sys
sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_query

__author__ = 'John.Hanley@parc.com'


class PathFinder:

    def __init__(self, gremlin, src, dst):
        self.gremlin = gremlin
        self.src = src
        self.dst = dst

    def find_path(self, thresh=10):
        step = 2  # In CDM13, 'vertex modeling an edge' yields out().out().
        n = 1
        while n < thresh and not self.both_path_exists(n):
            n += 1
        if n >= thresh:
            raise Exception('sorry, no path exists')
        path = ['both()'
                for i in range(n)]
        for i in range(0, n, step):
            path = self.replace_boths(path, i, step)

        print('\n')
        print(self.get_query(path))

    def both_path_exists(self, n):
        '''Predicate, answers the question: can a both() path be found?'''
        count = self.get_count('repeat(both()).times(%d)' % n)
        print(n, count)
        return count > 0

    def replace_boths(self, path, i, step):
        '''Given a path whose first both() is at i, replace with in or out.'''
        print('')
        print(i, path)
        for j in range(step):
            assert 'both()' == path[i + j], (path, i, j)
            path[i + j] = 'out()'  # Trial query.
        count = self.get_count('.'.join(path))
        print(count)
        if count == 0:  # Whoops, trial failed, go the other way.
            for j in range(step):
                path[i + j] = 'in()'
        post_condition = self.get_count('.'.join(path)) > 0
        assert post_condition
        return path

    def get_query(self, edges):
        '''Pass in some gremlin query fragment that generates edges.'''
        q = 'g.V() .%s .%s .%s .count()' % (self.src, edges, self.dst)
        print(q)
        return q

    def get_count(self, edges):
        count = self.gremlin.fetch_data(self.get_query(edges))[0]
        return count


def arg_parser():
    p = argparse.ArgumentParser(
        description='Identifies a path between a pair of vertices.')
    p.add_argument(
        '--src', help='source subgraph',
        default="has('pid').has('startedAtTime') .out().hasLabel('Subject')")
    # We go out() from Subject via 'EDGE_EVENT_AFFECTS_SUBJECT in',
    # arriving at another Subject that has pid but lacks startedAtTime.

    p.add_argument('--dst', help='destination vertex',
                   default="has('ident', '2bLWNaFx+xrolxrCW7fxkg==')")
    # This is in ta5attack2.
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    with gremlin_query.Runner() as gremlin:
        pf = PathFinder(gremlin, args.src, args.dst)
        pf.find_path()
