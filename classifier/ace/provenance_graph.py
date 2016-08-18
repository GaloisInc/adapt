# -*- coding: utf-8 -*-

import networkx
import pprint

from ace.titan_database import TitanDatabase

class ProvenanceGraph(object):
    def __init__(self):
        self.titanClient = TitanDatabase()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        self.close()

    def close(self):
        self.titanClient.close()

    def createActivity(self, segmentId, name):
        # @todo: figure out transactions or do it in one query
        nodeQuery = "graph.addVertex(label, 'Activity', 'activity:type', '{}')".format(name)
        node = self.titanClient.execute(nodeQuery)
        activityId = node[0]['id']
        edgeQuery = "g.V({}).next().addEdge('{}', g.V({}).next())".format(segmentId, 'activity:includes', activityId)
        edge = self.titanClient.execute(edgeQuery)

        return node, edge

    def deleteActivities(self):
        query = "g.V().has(label, 'Activity').drop().iterate()"
        # Removing a vertex removes all its incident edges as well.
        result = self.titanClient.execute(query)

    def activityNodes(self):
        query = "g.V().has(label, 'Activity')"
        nodes = self.titanClient.execute(query)
        for node in nodes:
            yield node

    def segmentNodes(self):
        query = "g.V().has(label, 'Segment')"
        nodes = self.titanClient.execute(query)
        for node in nodes:
            yield node

    def segments(self):
        query = "g.V().has(label, 'Segment')"
        nodes = self.titanClient.execute(query)
        for node in nodes:
            G = networkx.Graph()

            nodeId = node['id']
            G.add_node(nodeId)

            query = "g.V({}).as('a').out('segment:includes').out().in('segment:includes').where(neq('a'))"
            adjacentNodes = self.titanClient.execute(query.format(nodeId))
            for adjacentNode in adjacentNodes:
                adjacentNodeId = adjacentNode['id']
                G.add_node(adjacentNodeId)
                G.add_edge(nodeId, adjacentNodeId)

            yield(nodeId, G)

    def getActivityTypes(self, segmentIds):
        result = []

        query = "g.V({}).out('activity:includes')".format(", ".join(str(segmentId) for segmentId in segmentIds))
        nodes = self.titanClient.execute(query)

        for node in nodes:
            result.append(node['properties']['activity:type'][0]['value'])

        return result
