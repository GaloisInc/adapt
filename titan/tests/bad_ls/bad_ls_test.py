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
Test bad_ls nodes.
'''

import gremlinrestclient
import unittest


class GenericTests(unittest.TestCase):

    def setUp(self):
        url = 'http://localhost:8182/'
        self.db_client = gremlinrestclient.GremlinRestClient(url=url)

    def get_one(self, query):
        return self.db_client.execute(query).data[0]

    def test_that_edges_exist(self):
        count = self.get_one('g.E().count()')
        self.assertEqual(6730, count)

    def test_that_nodes_exist(self):
        count = self.get_one('g.V().count()')
        self.assertEqual(8000, count)
