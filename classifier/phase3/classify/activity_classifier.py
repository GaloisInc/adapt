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
import logging
import os
import sys
sys.path.append(os.path.expanduser('~/adapt/tools'))
import cdm.enums


class ActivityClassifier(object):
    '''
    Classifies activities found in segments of a CDM13 trace.
    '''

    def __init__(self, gremlin, grammar):
        self.gremlin = gremlin
        self.grammar = grammar
        self.num_nodes_fetched = 0
        self.num_classifications_inserted = 0
        # At present we have only tackled challenge problems for a few threats:
        self.detectors = [detector(gremlin) for detector in [
            classify.UnusualFileAccessDetector,
            classify.AcrossFirewallDetector,
            classify.SensitiveFileDetector,
            classify.ScanDetector,
        ]]
        unused = classify.Escalation(gremlin, classify.FsProxy(self.gremlin))
        assert cdm.enums.Event.UNLINK.value == 12
        assert cdm.enums.Event.UNLINK == cdm.enums.Event(12)
        self.log = logging.getLogger(__name__)
        formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
        handler = logging.StreamHandler()
        handler.setFormatter(formatter)
        self.log.addHandler(handler)
        self.log.setLevel(logging.INFO)

    def find_new_segments(self, last_previously_processed_seg):
        q = ("g.V().has(label, 'Segment')"
             " .values('segment:name').is(gt('%s'))"
             " .order().dedup()"
             % last_previously_processed_seg)
        for msg in self.gremlin.fetch(q):
            if msg.data is not None:
                for seg_db_id in msg.data:
                    yield seg_db_id

    # g.V().has('segment:name').out().groupCount().by(label())

    def classify(self, seg_ids):
        queries = [

            "g.V().has('segment:name', '%s')"
            " .has('commandLine').has('properties').as('a')"
            " .out('segment:includes')"
            " .order().dedup().as('b')"
            " .select('a').values('commandLine').as('commandLine')"
            " .select('a').values('properties').as('properties')"
            " .select('b').values('url').as('url')"  # also file-version & source
            " .select('b').values('ident').as('ident')"
            " .select('commandLine', 'properties', 'url', 'ident')",

            "g.V().has('segment:name', '%s').out('segment:includes')"
            " .where(or(hasLabel('Subject'), hasLabel('Agent')))"
            " .order().dedup()"
            " .valueMap(true)",

            # Sadly there's no pid on middle subject with eventType:9 execute.
            "g.V().has('segment:name', '%s')"
            " .out('segment:includes').hasLabel('Subject')"
            " .out().out().hasLabel('Entity-File').has('url')"
            " .valueMap(true)",
        ]
        for seg_id in seg_ids:
            for query in queries:
                # print(query % seg_id)
                seg_props = self.gremlin.fetch_data(query % seg_id)
                self.num_nodes_fetched += len(seg_props)
                self.classify_one_seg(seg_id, seg_props)

    def classify_one_seg(self, seg_id, props):
        for detector in self.detectors:
            activities = self.verify_c(detector.find_activities(seg_id, props))
            detector.insert_activity_classifications(seg_id, activities)
            self.num_classifications_inserted += len(activities)

    def verify_c(self, activities):
        '''Validates each classification against the grammar.
        This keeps the various detectors honest.
        '''
        valid_rules = set(dir(self.grammar._grammarClass))
        cs = set([classification for ident, classification in activities])
        for c in cs:
            assert 'rule_' + c in valid_rules, c
        return activities

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
