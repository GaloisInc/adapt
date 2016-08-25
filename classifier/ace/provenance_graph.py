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

    def createActivity(self, segmentId, name, suspicionScore = 0):
        query  = ("segmentNode = g.V({}).next();"
                  "activityNode = graph.addVertex(label, 'Activity', 'activity:type', '{}', 'activity:suspicionScore', {});"
                  "edge = segmentNode.addEdge('activity:includes', activityNode);"
                  "activityNode").format(segmentId, name, suspicionScore)
        node = self.titanClient.execute(query)

        return node[0]

    def changeActivityType(self, activityId, value):
        query  = "g.V({}).property('activity:type', '{}')".format(activityId, value)
        node = self.titanClient.execute(query)

    def changeActivitySuspicionScore(self, activityId, value):
        query  = "g.V({}).property('activity:suspicionScore', '{}')".format(activityId, value)
        node = self.titanClient.execute(query)

    def changeAcitivitySuspicionScore(self, acitivityId, value):
        query  = "x"
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
            query = "g.V({}).out('activity:includes')".format(segmentId)
            node = self.titanClient.execute(query)
            result.append(node[0]['properties']['activity:type'][0]['value'])

        return result

    def getActivity(self, segmentId):
        result = []

        query = "g.V({}).out('activity:includes')".format(segmentId)
        node = self.titanClient.execute(query)
        return (node[0]['id'],
                node[0]['properties']['activity:type'][0]['value'],
                node[0]['properties']['activity:suspicionScore'][0]['value'])
    
    def getUnclassifiedSegments(self):
        query = "g.V().hasLabel('Segment').where(__.not(outE('activity:includes')))"
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

    def getClassifiedSegments(self):
        query = "g.V().hasLabel('Segment').where(outE('activity:includes'))"
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
