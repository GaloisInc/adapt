# -*- coding: utf-8 -*-

import networkx

class FeatureExtractor(object):
    def __edgeCount(self, G):
        return G.number_of_edges()

    def __nodeCount(self, G):
        return G.number_of_nodes()

    def __graphDensity(self, G):
        return networkx.density(G)
    
    def run(self, G):
        edgeCount = self.__edgeCount(G)
        nodeCount = self.__nodeCount(G)
        density = self.__graphDensity(G)
        return [ edgeCount, nodeCount, density ]
