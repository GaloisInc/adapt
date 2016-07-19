#! /usr/bin/env python3

import os
import sys
import pprint
import asyncio
from aiogremlin import GremlinClient
import graphviz

sys.path.append(os.path.expanduser('~/adapt/tools'))
import cdm.enums
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

if __name__ == '__main__':

    with gremlin_query.Runner() as gremlin:

        vertices = gremlin.fetch(QUERYV)[0].data
        if vertices:
            graph = {}
            for v in vertices:
                val = {}
                val['criteria'] = v['properties']['pid'][0]['value']
                val['name'] = v['properties']['segment:name'][0]['value']

                edges = gremlin.fetch(QUERYE.format(v['id']))[0].data
                val['edges_out'] = []
                if edges != None:
                    val['edges_out'] = list(map(lambda e: e['id'], edges))

                graph[v['id']] = val

        dot = toDot(graph)
        print(dot)
