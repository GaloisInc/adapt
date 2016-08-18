# -*- coding: utf-8 -*-

import numpy

class SupervisedClassifier(object):
    def __init__(self, provenanceGraph, featureExtractor):
        self.provenanceGraph = provenanceGraph
        self.featureExtractor = featureExtractor

    def createTrainingData(self):
        segmentIds = []
        features = []

        for segmentId, G in self.provenanceGraph.segments():
            segmentIds.append(segmentId)
            features.append(self.featureExtractor.run(G))
        
        X = numpy.array(features)

        labels = self.provenanceGraph.getActivityTypes(segmentIds)

        return numpy.array(labels), X
