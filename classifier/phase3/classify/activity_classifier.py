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


class ActivityClassifier(object):
    '''
    Classifies activities found in segments of a CDM13 trace.
    '''

    def __init__(self, gremlin):
        self.gremlin = gremlin
        self.num_nodes_fetched = 0
        # At present we have only tackled challenge problems for a few threats:
        self.detectors = [
            classify.ExfilDetector(gremlin),
            classify.ScanDetector(gremlin),
            ]
        unused = classify.Escalation(gremlin, classify.FsProxy(self.gremlin))
        assert cdm.enums.Event.UNLINK.value == 12
        assert cdm.enums.Event.UNLINK == cdm.enums.Event(12)

    def find_new_segments(self, last_previously_processed_seg):
        q = ("g.V().has(label, 'Segment')"
             ".values('segment:name').is(gt('%s')).barrier().order()"
             % last_previously_processed_seg)
        for msg in self.gremlin.fetch(q):
            if msg.data is not None:
                for seg_db_id in msg.data:
                    yield seg_db_id

    def classify(self, seg_ids):
        queries = [
            "g.V().has('segment:name', '%s').out('segment:includes')"
            " .where(or(hasLabel('Subject'), hasLabel('Agent')))"
            " .valueMap(true)",

            # Sadly there's no pid on middle subject with eventType:9 execute.
            "g.V().has('segment:name', '%s').out('segment:includes').hasLabel('Subject')"
            " .out().out().hasLabel('Entity-File').has('url')"
            " .valueMap(true)",
            ]
        for seg_id in seg_ids:
            for query in queries:
                # print(query % seg_id)
                self.classify1(query, seg_id)

    def classify1(self, query, seg_id):

        for prop in self.gremlin.fetch_data(query % seg_id):
            self.num_nodes_fetched += 1
            if prop['label'] == 'zEntity-File':
                print(prop)
            for detector in self.detectors:
                try:
                    property = prop[detector.name_of_input_property()][0]
                except KeyError:
                    # print(seg_id, detector.name_of_input_property(), prop['label'], detector)
                    continue  # We don't have an input to offer this detector.
                property = property.strip('"')  # THEIA says "/tmp", not /tmp.
                # print(seg_id, detector.name_of_input_property(), property)
                if detector.finds_feature(property):
                    ident = prop['ident'][0]
                    detector.insert_activity_classification(ident, seg_id)

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
