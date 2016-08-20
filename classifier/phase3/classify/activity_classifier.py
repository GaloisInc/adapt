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
import gremlin_event


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
            classify.MarkerDetector,
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
        q = ("g.V().has(label, 'Segment')."
             " values('segment:name').is(gt('%s'))."
             " order().dedup()"
             % last_previously_processed_seg)
        # Ummm, unique segment:name recently became more of a segment "type".
        # During testing we simply return *all* segments.
        q = ("g.V().has(label, 'Segment').has('segment:name', 'byPID')."
             " order().dedup().id()")
        for msg in self.gremlin.fetch(q):
            if msg.data is not None:
                for seg_db_id in msg.data:
                    yield seg_db_id

    # g.V().has('segment:name').out().groupCount().by(label())

    def _get_queries(self):
        return [

            # 1st subj prop: '{uid=0, name=wget, gid=0, cwd=/tmp/victim}'
            # Reversed in/out bug: https://github.com/GaloisInc/adapt/issues/57
            # Typically 1st edge is EDGE_EVENT_AFFECTS_SUBJECT
            # or EDGE_EVENT_ISGENERATEDBY_SUBJECT.
            # Url is also accompanied by file-version & source.
            """
g.V(%d).hasLabel('Segment').out().order().dedup().
  hasLabel('Subject').has('commandLine').has('properties').
  order().dedup().as('a').
  in().in().hasLabel('Subject').has('startedAtTime').has('sequence').
  order().dedup().as('b').
  out().hasLabel('EDGE_FILE_AFFECTS_EVENT').order().dedup().
  out().hasLabel('Entity-File').has('url').order().dedup().as('c').
  select('a').values('commandLine').as('commandLine').
  select('a').values('properties').as('properties').
  select('b').values('startedAtTime').as('startedAtTime').
  select('b').values('sequence').as('sequence').
  select('c').values('url').as('url').
  select('c').values('ident').as('ident').
  select('commandLine', 'properties',
         'startedAtTime', 'sequence',
         'url', 'ident')
            """,

            # Sadly there's no pid on middle subject with eventType:9 execute.
            """
g.V(%d).hasLabel('Segment').
  out().hasLabel('Subject').has('startedAtTime').order().dedup().
  out().hasLabel('EDGE_FILE_AFFECTS_EVENT').order().dedup().
  out().hasLabel('Entity-File').has('url').order().dedup().
  valueMap(true)
            """,

            # This finds e.g. file:///tmp/adapt/tc-marker-001-begin.txt
            """
g.V(%d).hasLabel('Segment').
  out().hasLabel('Subject').has('startedAtTime').has('sequence').
  order().dedup().as('a').
  out().hasLabel('EDGE_EVENT_AFFECTS_FILE').order().dedup().
  out().hasLabel('Entity-File').has('url').order().dedup().as('b').
  select('a').values('startedAtTime').as('startedAtTime').
  select('a').values('sequence').as('sequence').
  select('a').values('ident').as('ident').
  select('b').values('url').as('url').
  select('startedAtTime', 'sequence', 'ident', 'url')
            """,

            """
g.V(%d).hasLabel('Segment').
  out().hasLabel('Agent').has('userID').order().dedup().
  valueMap(true)
            """,
        ]

    def classify(self, seg_ids, debug=False):
        stream = gremlin_event.Stream(self.gremlin, seg_ids)
        for seg_id, seg_props in stream.events_by_seg():
            self.num_nodes_fetched += len(seg_props)
            self.classify_one_seg(seg_id, seg_props)
            if debug:
                import pprint
                pprint.pprint(seg_id)
                pprint.pprint([p
                               for p in seg_props
                               if p['label'] != 'Entity-Memory'])

        if debug:
            nums = (self.num_nodes_fetched, self.num_classifications_inserted)
            self.log.info('fetched %d stream events; %d inserts' % nums)

        for seg_id in seg_ids:
            for query in self._get_queries():
                seg_props = self.gremlin.fetch_data(query % seg_id)
                self.num_nodes_fetched += len(seg_props)
                self.classify_one_seg(seg_id, seg_props)
                if debug and False:
                    print(query % seg_id)
                    print(seg_props)

    def classify_one_seg(self, seg_id, seg_props):
        for detector in self.detectors:
            activities = self.verify_c(
                detector.find_activities(seg_id, seg_props))
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
