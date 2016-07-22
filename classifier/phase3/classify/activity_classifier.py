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

import classify
import os
import sys
sys.path.append(os.path.expanduser('~/adapt/tools'))
import cdm.enums
import gremlin_properties


class ActivityClassifier(object):
    '''
    Classifies activities found in segments of a CDM13 trace.
    '''

    def __init__(self, gremlin):
        self.gremlin = gremlin
        # At present we have only tackled challenge problems for a few threats:
        self.detectors = [
            classify.ExfilDetector(gremlin),
            classify.ScanDetector(gremlin),
            ]
        unused = classify.Escalation(gremlin, classify.FsProxy(self.gremlin))
        assert cdm.enums.Event.UNLINK.value == 12
        assert cdm.enums.Event.UNLINK == cdm.enums.Event(12)

    def find_new_segments(self, last_previously_processed_seg):
        q = ("g.V().has(label, 'Segment').values('ident').is(gt('%s')).order()"
             % last_previously_processed_seg)
        q = ("g.V().has(label, 'Segment').values('segment:name').is(gt('%s')).order()"
             % last_previously_processed_seg)
        for msg in self.gremlin.fetch(q):
            if msg.data is not None:
                for seg_db_id in msg.data:
                    yield seg_db_id

    def classify(self, seg_ids):
        for seg_id in seg_ids:
            self.classify1(seg_id)

    def classify1(self, seg_id):
        q = ("g.V().has('segment:name', '%s').out('segment:includes')"
             ".out().hasLabel('EDGE_EVENT_AFFECTS_FILE')"
             ".out().hasLabel('Entity-File').valueMap()"
             ) % seg_id

        for prop in self.gremlin.fetch_data(q):
            if 'url' not in prop:
                continue
            for detector in self.detectors:
                property = prop[detector.name_of_input_property()][0]
                property = property.strip('"')  # THEIA says "/tmp", not /tmp.
                if detector.finds_feature(property):
                    detector.insert_activity_classification(prop['ident'][0], seg_id)

    def insert_activity_classification(self, base_node_id, seg_id, typ, score):
        cmds = ["act = graph.addVertex(label, 'Activity',"
                "  'activity:type', '%s',"
                "  'activity:suspicionScore', %f)" % (typ, score)]
        cmds.append("g.V().has('ident', '%s').next()"
                    ".addEdge('segment:includes', act)" % seg_id)
        cmds.append("act.addEdge('activity:includes',"
                    " g.V().has('ident', '%s').next())" % base_node_id)
        self.fetch1(';  '.join(cmds))
    #
    # example Activity node:
    #
    # gremlin> g.V().hasLabel('Segment').out().has(label,'Activity').valueMap()
    # ==>[activity:type:[exfiltration], activity:suspicionScore:[0.1]]
    # gremlin>
    # gremlin>
    # gremlin> g.V().has(label, 'Activity').out().limit(1).valueMap()
    # ==>[ident:[...], source:[0], file-version:[0], url:[file:///etc/shadow]]
    #

    def fetch1(self, query):
        '''Return a single query result.'''
        ret = 0
        for msg in self.gremlin.fetch(query):
            for item in msg.data:
                ret = item
        return ret
