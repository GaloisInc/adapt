# -*- coding: utf-8 -*-

import networkx
import pprint
import logging
import struct
import time
import os

from ace.titan_database import TitanDatabase

log = logging.getLogger(__name__)
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
#handler = logging.StreamHandler()
handler = logging.FileHandler(os.path.expanduser('~/adapt/classifier/ac.log'))
handler.setFormatter(formatter)
log.addHandler(handler)
log.setLevel(logging.INFO)

class ProvenanceGraph(object):
    def __init__(self):
        self.titanClient = TitanDatabase()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        self.close()

    def close(self):
        self.titanClient.close()

    def createActivity(self, segmentId, name, suspicionScore = 0):
        query  = ("segmentNode = g.V({}).next();"
                  "activityNode = graph.addVertex(label, 'Activity', 'activity:type', '{}', 'activity:suspicionScore', {});"
                  "edge = segmentNode.addEdge('segment:activity', activityNode);"
                  "activityNode").format(segmentId, name, suspicionScore)

        log.info('Creating Activity query: %s', query)

        node = self.titanClient.execute(query)
        return node[0]

    def changeActivityType(self, activityId, value):
        query  = "g.V({}).property('activity:type', '{}')".format(activityId, value)
        node = self.titanClient.execute(query)

    def changeActivitySuspicionScore(self, activityId, value):
        query  = "g.V({}).property('activity:suspicionScore', '{}')".format(activityId, value)
        node = self.titanClient.execute(query)

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

    def getSegments(self):
        query = "g.V().has(label, 'Segment')"
        nodes = self.titanClient.execute(query)
        for node in nodes:
            G = networkx.Graph()

            nodeId = node['id']
            G.add_node(nodeId)

            query = "g.V({}).out('segment:includes')"
            adjacentNodes = self.titanClient.execute(query.format(nodeId))
            for adjacentNode in adjacentNodes:
                adjacentNodeId = adjacentNode['id']
                G.add_node(adjacentNodeId)
                G.add_edge(nodeId, adjacentNodeId)

            yield(nodeId, G)

    def getSegment(self, segmentId):
        query = "g.V({}).has(label, 'Segment')".format(segmentId)
        node = self.titanClient.execute(query)[0]
        G = networkx.Graph()

        nodeId = node['id']
        G.add_node(nodeId)

        query = "g.V({}).out('segment:includes')"
        adjacentNodes = self.titanClient.execute(query.format(nodeId))
        for adjacentNode in adjacentNodes:
            adjacentNodeId = adjacentNode['id']
            G.add_node(adjacentNodeId)
            G.add_edge(nodeId, adjacentNodeId)

        return G

    def getActivityTypes(self, segmentIds):
        result = []

        for segmentId in segmentIds:
            query = "g.V({}).out('segment:activity')".format(segmentId)
            node = self.titanClient.execute(query)
            result.append(node[0]['properties']['activity:type'][0]['value'])

        return result

    def getActivity(self, segmentId):
        result = []

        query = "g.V({}).out('segment:activity')".format(segmentId)
        node = self.titanClient.execute(query)
        return (node[0]['id'],
                node[0]['properties']['activity:type'][0]['value'],
                node[0]['properties']['activity:suspicionScore'][0]['value'])

    def getUnclassifiedSegments(self):
        query = "g.V().has('segment:name','byPID').where(__.not(outE('segment:activity')))"
        nodes = self.titanClient.execute(query)
        for node in nodes:
            G = networkx.Graph()

            nodeId = node['id']
            G.add_node(nodeId)

            log.info("NodeId = " + str(nodeId))

            try:

                query = "g.V({}).out('segment:includes')"
                log.info("Query: " + query.format(nodeId))

                adjacentNodes = self.titanClient.execute(query.format(nodeId))
                log.info("\treturn (size): " + str(len(adjacentNodes)))

                for adjacentNode in adjacentNodes:
                    adjacentNodeId = adjacentNode['id']
                    G.add_node(adjacentNodeId)
                    G.add_edge(nodeId, adjacentNodeId)

                yield(nodeId, G)

            except Exception as e:
                log.info("Exception: " + str(e))
                raise

    def getClassifiedSegments(self):
        query = "g.V().has('segment:name','byPID').where(outE('segment:activity'))"
        nodes = self.titanClient.execute(query)
        for node in nodes:
            G = networkx.Graph()

            nodeId = node['id']
            G.add_node(nodeId)

            query = "g.V({}).out('segment:includes')"
            adjacentNodes = self.titanClient.execute(query.format(nodeId))
            for adjacentNode in adjacentNodes:
                adjacentNodeId = adjacentNode['id']
                G.add_node(adjacentNodeId)
                G.add_edge(nodeId, adjacentNodeId)

            yield(nodeId, G)
