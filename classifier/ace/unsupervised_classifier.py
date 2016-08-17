# -*- coding: utf-8 -*-

from sklearn import cluster
import numpy

class UnsupervisedClassifier(object):
    def __init__(self, provenanceGraph, featureExtractor):
        self.provenanceGraph = provenanceGraph
        self.featureExtractor = featureExtractor

    def run(self):
        segmentIds = []
        features = []

        for segmentId, G in self.provenanceGraph.segments():
            segmentIds.append(segmentId)
            features.append(self.featureExtractor.run(G))
        
        X = numpy.array(features)
            
        # Create a clustering estimator
#        estimator = cluster.AffinityPropagation() # doesn't work very well for some reason
        estimator = cluster.MeanShift()
        y = estimator.fit_predict(X)

        result = zip(segmentIds, y)

        return result
