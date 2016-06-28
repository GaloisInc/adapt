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

import io
import os
import re


class ActivityClassifier(object):
    '''
    Classifies activities found in segments of a CDM13 trace.
    '''

    def __init__(self, gremlin_client):
        self.gremlin = gremlin_client

    def find_new_segments(self, last_previously_processed_seg):
        q = "g.V().has(label, 'Segment').id().is(gt(%d)).order()" % (
            last_previously_processed_seg)
        for msg in self.gremlin.fetch(q):
            if msg is not None:
                for seg_db_id in msg.data:
                    yield seg_db_id

    def classify(self, seg_ids):
        for seg_id in seg_ids:
            self.classify1(seg_id)

    def classify1(self, seg_id):
        q = "g.V(%d).outE('segment:includes').inV()"

    def fetch1(self, query):
        '''Return a single query result.'''
        ret = 0
        for msg in self.gremlin.fetch(query):
            for item in msg.data:
                ret = item
        return ret

