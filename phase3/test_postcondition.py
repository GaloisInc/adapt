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

import logging
import os
import sys
import unittest
sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_query

__author__ = 'John.Hanley@parc.com'

log = logging.getLogger(__name__)
log.addHandler(logging.StreamHandler())
log.setLevel(logging.DEBUG)


def fetch1(gremlin, query):
    '''Return a single query result.'''
    ret = 0
    for msg in gremlin.fetch(query):
        for item in msg.data:
            ret = item
    return ret


class TestPhase3Postcondition(unittest.TestCase):
    '''Test Ac component with upstream deps for phase3 development.'''

    def test_node_counts(self):

        with gremlin_query.Runner() as gremlin:
            count = fetch1(gremlin, "g.V().has(label, 'Activity').count()")
            self.assertEqual(1, count)


if __name__ == '__main__':
    unittest.main()
