#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
    =======
    `PROVN-Segmenter` --- Main module
    =======

    Adria Gascon, 2016.
"""

import os
import argparse
import networkx as NX
from provnparser import *
import json
import datetime
import sys

VERBOSE = True

class Datetime:
    def __init__(self, s):
        date, time = s.split('T')
        year, month, day = date.split('-')
        hour, minute, second = time.split(':')
        self.datetime = datetime.datetime(
            int(year), int(month), int(day),
            int(hour), int(minute), int(second))

    def __str__(self):
        return '{0}-{1}-{2}T{3}:{4}:{5}'.format(self.date.year,
            self.date.month, self.date.day, self.time.hour,
            self.time.minute, self.time.second)

    def add(self, days, hs, ms, secs):
        assert hs < 24
        assert ms < 59
        assert secs < 59
        new_datetime = self.datetime + datetime.timedelta(
            days=days, hours=hs, minutes=ms, seconds=secs)

        def two_digits(v):
            if v < 10:
                return '0' + str(v)
            return v
        return '{0}-{1}-{2}T{3}:{4}:{5}'.format(
            two_digits(new_datetime.year),
            two_digits(new_datetime.month), two_digits(new_datetime.day),
            two_digits(new_datetime.hour),
            two_digits(new_datetime.minute), two_digits(new_datetime.second))

    def compare(self, d1, d2):
        if d1.date < d2.date:
            return -1
        elif d2.date < d1.date:
            return 1
        elif d1.time < d2.time:
            return -1
        elif d2.time < d1.time:
            return 1
        else:
            return 0

    @classmethod
    def time_slices(cls, start, end, days, hs, ms, secs):
        intervals = []
        x = start
        y = str(Datetime(start).add(days, hs, ms, secs))
        while x < end:
            intervals.append((x, y))
            x = y
            y = str(Datetime(y).add(days, hs, ms, secs))
        return intervals


class Segmenter:
    """
    This class implements a segmenter configured through a json file
    """

    def __init__(self, docgraph, json_spec_file):
        self.dg = docgraph
        with open(json_spec_file) as f:
            self.spec = json.loads(f.read())

    def k_reach(self, s_set, k, edge_type_set):
        """
            Returns the set of nodes reachable in <=k steps from the nodes in
            s_set, following edges with types in edge_type_set
        """
        reach_set = s_set
        reach_set_i_1 = reach_set
        for i in range(k):
            reach_set_i = set()
            for x in reach_set_i_1:
                for succ in self.dg.g.successors(x):
                    if self.dg.g.edge[x][succ]['label'] in edge_type_set:
                        reach_set_i.add(succ)
            reach_set_i_1 = reach_set_i
            reach_set |= reach_set_i
        return reach_set

    def time_slice(self, begin_time, end_time):
        """
            Returns the set of nodes in the graph with times
            in (begin_time, end_time]
        """
        res_set = set()
        for x, y in self.dg.g.edges():
            try:
                ts = self.dg.g.edge[x][y]['timestamp']
            except KeyError:
                if self.dg.g.edge[x][y]['label'] != "wasAssociatedWith" and \
                        self.dg.g.edge[x][y]['label'] != "segment:includes":
                    raise Exception(
                        'All events of type other than wasAssociatedWith '
                        'and segment:includes '
                        'must have a timestamp')
                continue
            if ts >= begin_time and ts < end_time:
                res_set.add(x)
                res_set.add(y)
        return res_set

    def segment_by_time(self, from_, window_dict):
        intervals = Datetime.time_slices(from_, self.dg.max_time,
            window_dict['days'], window_dict['hours'],
            window_dict['minutes'], window_dict['seconds'])
        return [(x, self.time_slice(x, y)) for x, y in intervals]

    def segment_by_att(self, att, radius, edges):
        values = self.dg.get_attribute_values(att)
        segments = []
        for v in values:
            segments.append(
                (v, self.k_reach(
                    self.dg.get_nodes_by_att(att, v), radius, edges)))
        return segments

    def eval_spec(self, add_segment2segment_edges=True):

        def segment2segment_exprs(segments_reverse_map):
            s2s_list = []
            edges = set()
            for _, l in segments_reverse_map.items():
                for i, x in enumerate(l):
                    for j, y in enumerate(l):
                        if i < j and not (x.id, y.id) in edges:
                            s2s_list.append(Segment2SegmentExpr(x.id, y.id))
                            edges.add((x.id, y.id))
             return s2s_list
            return s2s_list

        self.name = self.spec['segmentation_specification']['segment']['name']
        self.specifications = self.spec['segmentation_specification']['segment']['specifications']
        results = []
        assert len(self.specifications) < 3, 'At most 2 segmentation specifications supported'
        properties = [x['property'] for x in
            self.spec['segmentation_specification']['segment']['args']]
        segmentation_doc = Document()
        segmentation_doc.prefix_decls = self.dg.doc.prefix_decls
        segment_reverse_map = {}
        for i, d in enumerate(self.specifications):
            if 'radius' in d:
                # segment by att
                radius = int(d['radius']['r'])
                edges = d['radius']['edges']
                att = d['radius']['from']['property']
                results.append((properties[i],
                    self.segment_by_att(att, radius, edges)))
            elif 'time' in d:
                # segment by time
                from_ = d['time']['from']
                window_dict = d['time']['window']
                results.append((properties[i],
                    self.segment_by_time(from_, window_dict)))
        for i, (prop_i, r_i) in enumerate(results):
            for (val_i, segment_i) in r_i:
                for j, (prop_j, r_j) in enumerate(results):
                    if i >= j and len(results) > 1:
                        continue
                    else:
                        for (val_j, segment_j) in r_j:
                            att_val_dict = [(str(prop_i), str(val_i)),
                                (str(prop_j), str(val_j))]
                            s = Segment('segment_id_{0}'.format(
                                len(segmentation_doc.expression_list)),
                                att_val_dict)
                            segmentation_doc.expression_list += [s]
                            for n in segment_i & segment_j:
                                e = SegmentExpr(s.id, n)
                                try:
                                    segment_reverse_map[n] += [s]
                                except KeyError:
                                    segment_reverse_map[n] = [s]
                                segmentation_doc.expression_list += [e]
        if add_segment2segment_edges:
            segmentation_doc.expression_list += segment2segment_exprs(
                segment_reverse_map)
        segmentation_dg = DocumentGraph(segmentation_doc)
        return segmentation_dg


class DocumentGraph:
    def _populate_graph(self):
        for e in self.doc.expression_list:
            if isinstance(e, Activity) or isinstance(e, Entity) or isinstance(e, EntityFile) or isinstance(e, EntityNetFlow) or isinstance(e, EntityMemory) or isinstance(e, Resource) or isinstance(e, Subject) or isinstance(e, Host) or isinstance(e, Agent) or isinstance(e, Pattern) or isinstance(e, Phase) or isinstance(e, APT) or isinstance(e, Segment) or isinstance(e, EDGE_EVENT_AFFECTS_MEMORY) or isinstance(e, EDGE_EVENT_AFFECTS_FILE) or isinstance(e, EDGE_EVENT_AFFECTS_NETFLOW) or isinstance(e, EDGE_EVENT_AFFECTS_SUBJECT) or isinstance(e, EDGE_EVENT_AFFECTS_SRCSINK) or isinstance(e, EDGE_EVENT_HASPARENT_EVENT) or isinstance(e, EDGE_EVENT_ISGENERATEDBY_SUBJECT) or isinstance(e, EDGE_EVENT_CAUSES_EVENT) or isinstance(e, EDGE_SUBJECT_AFFECTS_EVENT) or isinstance(e, EDGE_SUBJECT_HASPARENT_SUBJECT) or isinstance(e, EDGE_SUBJECT_HASPRINCIPAL) or isinstance(e, EDGE_SUBJECT_RUNSON) or isinstance(e, EDGE_FILE_AFFECTS_EVENT) or isinstance(e, EDGE_NETFLOW_AFFECTS_EVENT) or isinstance(e, EDGE_MEMORY_AFFECTS_EVENT) or isinstance(e, EDGE_SRCSINK_AFFECTS_EVENT) or isinstance(e, EDGE_OBJECT_PREV_VERSION) or isinstance(e, EDGE_SUBJECT_HASLOCALPRINCIPAL):
                self.g.add_node(e.id, e.att_val_dict)
            elif e is not None:
                d = {}
                d['label'] = e.label()
                self.g.add_edge(e.s, e.t, d)

    def __init__(self, document):
        self.g = NX.DiGraph()
        self.doc = document
        self.max_time = None
        self.min_time = None

        self._populate_graph()

    def union(self, dg):
        self.doc.union(dg.doc)
        self.g = NX.DiGraph()
        self._populate_graph()

    def __str__(self):
        return str(self.doc)

    def print_summary(self):
        print('=' * 30)
        print('\tGraph summary')
        print('=' * 30)
        print('Min time: {0}'.format(self.min_time))
        print('Max time: {0}'.format(self.max_time))
        print('nodes ({0}):'.format(len(self.g.nodes())))
        for n in self.g.nodes():
            print('\t', n, self.g.node[n])
        print('edges ({0}):'.format(len(self.g.edges())))
        for e in self.g.edges():
            print('\t', e, self.g.edge[e[0]][e[1]])

    # def draw(self):
    #    import matplotlib.pyplot as plt
    #   print 'here'
    #    NX.draw(self.g)
    #    plt.show()

    def get_attribute_values(self, att):
        res = set()
        for x in self.g.nodes():
            if att in self.g.node[x]:
                res.add(self.g.node[x][att])
        for e in self.g.edges():
            if att in self.g.edge[e[0]][e[1]]:
                res.add(self.g.edge[e[0]][e[1]][att])
        return res

    def get_nodes_by_att(self, att, val):
        return set([n for n in self.g.nodes()
            if att in self.g.node[n] and self.g.node[n][att] == val])


def test_provn_segmenter():
    # Testing parser
    sys.stderr.write('*' * 30 + '\n')
    sys.stderr.write('Running PROVN Segmenter Tests\n')
    sys.stderr.write('*' * 30 + '\n')
    test_input_files = [
        # {'filename': 'test/2016-01-28/bad-ls.provn',
        # 'num_nodes': 2712,
        # 'num_edges': 9683},
        {'filename': 'test/test_james.provn',
         'num_nodes': 10,
         'num_edges': 11}]
    for test_d in test_input_files:
        filename = test_d['filename']
        sys.stderr.write('---> Parsing {0}\n'.format(filename))
        doc = Document()
        doc.parse_provn(filename)
        dg = DocumentGraph(doc)
        sys.stderr.write('---> Verifying number of edges and nodes\n')
        assert len(dg.g.nodes()) == test_d['num_nodes'], '{0} != {1}'.format(
            len(dg.g.nodes()), test_d['num_nodes'])
        assert len(dg.g.edges()) == test_d['num_edges'], '{0} != {1}'.format(
            len(dg.g.edges()), test_d['num_edges'])

    # Testing segmenter
    test_input_files = [
        {'filename': 'test/test_james.provn',
         'spec': 'test/test_james_spec.json',
         'num_nodes': 15,
         'num_edges': 13}]
    for test_d in test_input_files:
        filename = test_d['filename']
        spec = test_d['spec']
        doc = Document()
        doc.parse_provn(filename)
        dg = DocumentGraph(doc)
        s = Segmenter(dg, spec)
        segmentation_doc = s.eval_spec()
        sdg = DocumentGraph(segmentation_doc)
        seg_num_nodes = len(sdg.g.nodes())
        seg_num_edges = len(sdg.g.edges())
        assert seg_num_nodes == test_d['num_nodes'], '{0} != {1}'.format(
            seg_num_nodes, test_d['num_nodes'])
        assert seg_num_edges == test_d['num_edges'], '{0} != {1}'.format(
            seg_num_edges, test_d['num_edges'])


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='A provn segmenter')
    parser.add_argument(
        '--provn_file', '-p', help='A prov-tc file in provn format')
    parser.add_argument('spec_file',
                        help='A segment specification file in json format')
    parser.add_argument('--verbose', '-v', action='store_true',
        help='Run in verbose mode')
    parser.add_argument('--no_seg2seg', action='store_true',
        help='Do not add segment2segment edges')
    parser.add_argument('--summary', '-s', action='store_true',
        help='Print a summary of the input file an quit, segment spec is ignored')

    args = parser.parse_args()
    VERBOSE = args.verbose
    # Check that provided non-optional files actually exist
    for f in [args.provn_file, args.spec_file]:
        if not (os.path.isfile(f)):
            print('File {0} does not exist...aborting'.format(f))

    doc = Document()
    doc.parse_provn(args.provn_file)
    dg = DocumentGraph(doc)

    if args.summary:
        dg.print_summary()
        #  g.draw()
        sys.exit()

    s = Segmenter(dg, args.spec_file)

    segmentation_dg = s.eval_spec(add_segment2segment_edges=not args.no_seg2seg)
    print('=' * 30)
    print('\tSegmentation result')
    print('=' * 30)
    print(segmentation_dg)

    dg.union(segmentation_dg)
    dg.print_summary()


