#! /usr/bin/env python3

import os
import sys
import pprint
import asyncio
from aiogremlin import GremlinClient
import graphviz

sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_properties
import gremlin_query

QUERYV = "g.V().hasLabel('Segment')"
QUERYE = "g.V({}).as('a').out('segment:includes').out().in('segment:includes').where(neq('a'))"

def toDot(graph, label='Segmentation Graph'):
    dot = graphviz.Digraph(graph_attr={'label': label,
                                       'labelloc': 't',
                                       'fontname': 'sans-serif'},
                           node_attr={'margin': '0',
                                      'fontsize': '6',
                                      'fontname': 'sans-serif'})
    for n in graph.keys():
        linecolor, color, penwidth = ('black', 'white', '1')
        fontcolor = linecolor
        node_label = graph[n]['name'] + ':' + str(graph[n]['criteria'])

        dot.node(str(n),
                 node_label,
                 style='filled',
                 fillcolor=color,
                 color=linecolor,
                 fontcolor=fontcolor,
                 penwidth=penwidth)

        for o in graph[n]['edges_out']:
            dot.edge(str(n), str(o), color=linecolor)

    return dot

def renderSegments(verbose = False):
    with gremlin_query.Runner() as gremlin:

        vertices = gremlin_properties.fetch(gremlin, QUERYV)
        graph = {}
        for v in vertices:
            val = {}
            val['criteria'] = v['pid']
            val['name'] = v['segment:name']

            edges = gremlin_properties.fetch(gremlin, QUERYE.format(v.getId()))
            val['edges_out'] = [e.getId() for e in edges]

            graph[v.getId()] = val

    dot = toDot(graph)

    if verbose:
        print(dot)

    dot.format = 'svg'
    dot.render('static/seggraph.dot', view=False)


if __name__ == '__main__':
    renderSegments()
