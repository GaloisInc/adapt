#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
    =======
    `RDF-Segmenter` --- Main module
    =======
    This module implements a simple predicate-based RDF segmenter.
    The main program takes as inputs
        (i) an RDF graph G in turtle format and
        (ii) a predicate name N,
        (iii) a predicate value V, and
        (iv) an integer radius R,
    and produces an RDF file consisting of 
    teh subgraph of G containing all nodes at distance
    R of nodes s such that (s, N, V) is in G.

    Adria Gascon, 2016.
"""

import os
import argparse
import sys
import rdflib
import networkx as NX
import rdflib.tools


class Segmenter:
    """
    This class implements a radius-based segmenter.
    """

    def __init__(self, filepath):
        self.filepath = filepath
        self.rdf_graph = rdflib.Graph()
        self.rdf_graph.parse(source=filepath, format='n3')
        self.graph = NX.MultiDiGraph()
        for (s, p, o) in self.rdf_graph:
            self.graph.add_edge(s, o, label=p)

    def get_neighborhood(s):
        precs = []
        for n in s:
            precs += seg.graph.predecessors[n]
        succs = []
        for n in s:
            succs += seg.graph.successors[n]
        return set(precs) - s, set(succs) - s

    def get_values(self, key):
        """
        Returns the list of all literals l such that
        (x, key, l) is in the store
        """
        return [o for (s, p, o) in self.rdf_graph if str(p) == key]

    def segment(self, key, radius):
        if VERBOSE:
            print 'Starting segmentation with radius' + \
                ' {0} and predicate {1}'.format(
                    radius, key)
        values = self.get_values(key)
        segment_map = {}
        segment_map_inverse = {}

        for v in values:
            if VERBOSE:
                print 'Extracting segment for ' + \
                    ' {0} = {1} and radius = {2}'.format(
                        key, v, radius)
            segment_map[(key, v, radius)], _ = self.get_segment_subgraph(
                    key, str(v), radius)
            for n in segment_map[(key, v, radius)]:
                if not (n in segment_map_inverse):
                    segment_map_inverse[n] = [(key, v, radius)]
                else:
                    segment_map_inverse[n] += [(key, v, radius)]
            print '\t...Done. Segment has size {0}'.format(
                    len(segment_map[key, v, radius]))
        if VERBOSE:
            print 'Pruning segments to make them disjoint ' + \
                '(triples with shared nodes will be left unassigned)'
        to_be_removed = set()
        for n in segment_map_inverse:
            if len(segment_map_inverse[n]) >= 2:
                if VERBOSE:
                    print '\t {0} is in {1} segments. We will remove it from them'.format(
                            n, len(segment_map_inverse[n]))
                    for segment in segment_map_inverse[n]:
                        l = segment_map[segment]
                        if n in l:
                            l.remove(n)
                        segment_map[segment] = l
                to_be_removed.add(n)
        for n in to_be_removed:
            del segment_map_inverse[n]
        return segment_map, segment_map_inverse

    def write_segmented_graph_to_file(self, segment_map,
            key, radius):
        if VERBOSE:
            print 'Abstracting segments from original graph'
        to_be_removed = set()
        to_be_added = set()
        fix_point = False
        while not fix_point:
            for k, v in segment_map.items():
                segment_node = rdflib.Literal(str(map(str, k)))
                if VERBOSE:
                    print '\tAbstracting segment {0}'.format(segment_node)
                for (s, p, o) in seg.rdf_graph:
                    if s in v and o in v:
                        to_be_removed.add((s, p, o))
                    elif s in v:  # o not in v
                        # if isinstance(o, rdflib.term.URIRef):
                        to_be_added.add((segment_node, p, o))
                        to_be_removed.add((s, p, o))
                    elif o in v:  # s not in v
                        to_be_added.add((s, p, segment_node))
                        #l = len(to_be_removed)
                        to_be_removed.add((s, p, o))
                        #if l == len(to_be_removed):
                        #    print (s, p, o)
                        #    print (s, p, segment_node)
                        #    print '*'*30

            #print len(to_be_removed)
            #print len(to_be_added)
            len_before = len(self.rdf_graph)
            for t in to_be_removed:
                seg.rdf_graph.remove(t)
            fix_point = not (len_before == len(self.rdf_graph))
            len_before = len(self.rdf_graph)
            for t in to_be_added:
                seg.rdf_graph.add(t)
            fix_point = fix_point and not (len_before == len(self.rdf_graph))
        filename, ext = os.path.splitext(args.rdf_turtle_file)
        res_filename = '{0}_segmented_{1}_radius_{2}{3}'.format(
            filename, key[key.find('#')+1:], radius, ext)
        with open(res_filename, 'w') as f:
            f.write(seg.rdf_graph.serialize(format='n3'))
        if VERBOSE:
            print '*' * 30
            print 'Created segmented graph for {0} and radius = {1}'.format(
                key, radius)
            self.print_graph_summary()
        for t in to_be_removed:
            seg.rdf_graph.add(t)
        for t in to_be_added:
            seg.rdf_graph.remove(t)

    def write_segment_to_file(self, key, value, radius):
        subgraph_nodes, _ = self.get_segment_subgraph()
        triples_to_be_removed = []
        for (s, p, o) in seg.rdf_graph:
            if not (s in subgraph_nodes or o in subgraph_nodes):
                triples_to_be_removed.append((s, p, o))
        for t in triples_to_be_removed:
            seg.rdf_graph.remove(t)
        filename, ext = os.path.splitext(args.rdf_turtle_file)
        res_filename = '{0}_segment_{1}_{2}_radius_{3}{4}'.format(
            filename, key[key.find('#')+1:], value,
            radius, ext)
        with open(res_filename, 'w') as f:
            f.write(seg.rdf_graph.serialize(format='n3'))
        if VERBOSE:
            print '*' * 30
            print 'Created graph for {0} = {1} and radius = {2}'.format(
                key, value, radius)
            self.print_graph_summary()
        for t in triples_to_be_removed:
            seg.rdf_graph.add(t)

    def get_segment_subgraph(self, key, value, radius):
        """
        Returns the set of nodes at distance <= radius
        of nodes s such that (s, self.key, self.value) is in G.
        This function operated on a multidirected labeled
        view of the rdf graph. Hence, *predicates are not returned*.
        """
        reach = set()
        for s, p, o in self.rdf_graph:
            # Mark s in triples of the form (s, self.key, self.value)
            if str(p) == key and str(o) == value:
                reach.add(s)
        previous_level = reach
        boundary = set()
        for i in range(1, radius + 1):
            this_level = []
            for x in previous_level:
                l = self.graph.neighbors(x)
                this_level += l
            # print i, previous_level, this_level
            boundary = set(this_level) - reach
            reach |= set(this_level)
            previous_level = set(this_level)
        return reach, boundary

    def print_graph_summary(self, recalculate=True):
        print '#triples = {0}'.format(len(self.rdf_graph))
        predicates = set([str(p) for (s, p, o) in self.rdf_graph])
        print '#predicates = {0}'.format(len(predicates))
        print 'predicates:'
        for p in predicates:
            print '\t', p
        # for stmt in self.rdf_graph:
        #    print stmt
        # for u, v in self.graph.edges():
        #    print u, v, self.graph[u][v]['label']


if __name__ == "__main__":
    # Read file
    parser = argparse.ArgumentParser(description='A simple RDF segmenter')
    parser.add_argument('rdf_turtle_file', help='An RDF in turtle format')
    parser.add_argument('key', help='A predicate for the '
        'segmentation, e.g. pid')
    parser.add_argument('value', help='A value for the predicate used in the'
        ' segmentation, e.g. 3233')
    parser.add_argument('radius', help='The radius used in the'
        ' segmentation, e.g. 5', type=int)
    parser.add_argument('--verbose', '-v', action='store_true',
        help='Run in verbose mode')
    parser.add_argument('--summary', '-s', action='store_true',
        help='Print a summary of the input graph an quit')

    args = parser.parse_args()
    VERBOSE = args.verbose
    # Check that provided non-optional files actually exist
    for f in [args.rdf_turtle_file]:
        if not (os.path.isfile(f)):
            print 'File {0} does not exist...aborting'.format(f)

    g = rdflib.Graph()
    seg = Segmenter(args.rdf_turtle_file)
    if VERBOSE or args.summary:
        seg.print_graph_summary()
        if args.summary:
            sys.exit()

    segment_map, _ = seg.segment(args.key, args.radius)
    seg.write_segmented_graph_to_file(
        segment_map, args.key, args.radius)
