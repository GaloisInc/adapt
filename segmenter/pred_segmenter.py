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
    This class implements a time-based segmenter.
    """

    def __init__(self, filepath, key, value, radius):
        self.filepath = filepath
        self.key = key
        self.value = value
        self.radius = radius
        self.rdf_graph = rdflib.Graph()
        self.rdf_graph.parse(source=filepath, format='n3')
        self.graph = NX.MultiDiGraph()
        for (s, p, o) in self.rdf_graph:
            self.graph.add_edge(s, o, label=p)

    def segment(self):
        subgraph_nodes = self.mark_graph()
        triples_to_be_removed = []
        for (s, p, o) in seg.rdf_graph:
            if not (s in subgraph_nodes or o in subgraph_nodes):
                triples_to_be_removed.append((s, p, o))
        for t in triples_to_be_removed:
            seg.rdf_graph.remove(t)
        filename, ext = os.path.splitext(args.rdf_turtle_file)
        res_filename = '{0}_segmented_{1}_{2}_radius_{3}{4}'.format(
            filename, self.key[self.key.find('#')+1:], self.value,
            self.radius, ext)
        with open(res_filename, 'w') as f:
            f.write(seg.rdf_graph.serialize(format='n3'))
        if VERBOSE:
            print '*' * 30
            print 'Created graph for {0} = {1} and radius = {2}'.format(
                self.key, self.value, self.radius)
            self.print_graph_summary()
        for t in triples_to_be_removed:
            seg.rdf_graph.add(t)

    def mark_graph(self):
        reach = set()
        for s, p, o in self.rdf_graph:
            # Mark s in triples of the form (s, self.key, self.value)
            if str(p) == self.key and str(o) == self.value:
                reach.add(s)
        previous_level = reach
        for i in range(1, self.radius + 1):
            this_level = []
            for x in previous_level:
                l = self.graph.neighbors(x)
                this_level += l
            #print i, previous_level, this_level
            reach |= set(this_level)
            previous_level = set(this_level)
        return reach

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
    seg = Segmenter(args.rdf_turtle_file, args.key, args.value, args.radius)
    if VERBOSE or args.summary:
        seg.print_graph_summary()
        if args.summary:
            sys.exit()

    seg.segment()
