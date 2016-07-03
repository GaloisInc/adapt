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

    def __init__(self, gremlin_client):
        self.gremlin = gremlin_client
        # At present we have only tackled challenge problems for two threats:
        self.exfil = classify.ExfilDetector()
        self.escalation = classify.Escalation(classify.FsProxy(self.gremlin))
        assert cdm.enums.Event.UNLINK.value == 12
        assert cdm.enums.Event.UNLINK == cdm.enums.Event(12)

    def find_new_segments(self, last_previously_processed_seg):
        q = "g.V().has(label, 'Segment').id().is(gt(%d)).order()" % (
            last_previously_processed_seg)
        for msg in self.gremlin.fetch(q):
            if msg.data is not None:
                for seg_db_id in msg.data:
                    yield seg_db_id

    def classify(self, seg_ids):
        for seg_id in seg_ids:
            self.classify1(seg_id)

    def classify1(self, seg_id):
        exfil_idents = set()
        q = "g.V(%d).outE('segment:includes').inV()" % seg_id
        for prop in gremlin_properties.fetch(self.gremlin, q):
            # During phase3 integration we're more worried about
            # plumbing than correctness. Clearly this needs to improve.
            if 'url' not in prop:
                continue
            if self.exfil.is_exfil(prop['url']) or True:
                exfil_idents.add(prop['ident'])
        if len(exfil_idents) > 0:
            self.insert_activity_classification(
                exfil_idents, seg_id, 'exfiltration', .1)

    def insert_activity_classification(self, base_nodes, seg_id, typ, score):
        cmds = ["act = graph.addVertex(label, 'Activity',"
                "  'activity:type', '%s',"
                "  'activity:suspicionScore', %f)" % (typ, score)]
        cmds.append("g.V(%d).next().addEdge('segment:includes', act)" % seg_id)
        for base_node_ident in base_nodes:
            cmds.append("act.addEdge('activity:includes',"
                        " g.V().has('ident', '%s').next())" % base_node_ident)
        self.fetch1(';  '.join(cmds))

    def fetch1(self, query):
        '''Return a single query result.'''
        ret = 0
        for msg in self.gremlin.fetch(query):
            for item in msg.data:
                ret = item
        return ret
