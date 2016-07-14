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

import os

__author__ = 'John.Hanley@parc.com'


class Detector:
    '''
    A detector knows how to insert its activity classification into the DB.

    Descendents of this class may implement activity_suspicion_score(), and
    must implement:
    - name_of_input_property()
    - name_of_output_classification()
    - finds_feature() - a predicate
    '''

    def __init__(self, gremlin):
        self.gremlin = gremlin

    def activity_suspicion_score(self):
        default = 0.1
        return default

    def insert_activity_classification(self, base_node_id, seg_id):
        cmds = ["act = graph.addVertex(label, 'Activity',"
                "  'activity:type', '%s',"
                "  'activity:suspicionScore', %f)" % (
                    self.name_of_output_classification(),
                    self.activity_suspicion_score())]
        print(cmds)
        cmds.append("g.V().has('ident', '%s').next()"
                    ".addEdge('segment:includes', act)" % seg_id)
        cmds.append("act.addEdge('activity:includes',"
                    " g.V().has('ident', '%s').next())" % base_node_id)
        self.gremlin.fetch_data(';  '.join(cmds))

    def fetch1(self, query):
        '''Return a single query result.'''
        ret = 0
        for msg in self.gremlin.fetch(query):
            for item in msg.data:
                ret = item
        return ret
