# -*- coding: utf-8 -*-

from sklearn import cluster
import numpy
import pprint
import logging
import struct
import time
import os

log = logging.getLogger(__name__)
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
#handler = logging.StreamHandler()
handler = logging.FileHandler(os.path.expanduser('~/adapt/classifier/ac.log'))
handler.setFormatter(formatter)
log.addHandler(handler)
log.setLevel(logging.INFO)

class UnsupervisedClassifier(object):
    def __init__(self, provenanceGraph, featureExtractor):
        self.provenanceGraph = provenanceGraph
        self.featureExtractor = featureExtractor

    def classifyNew(self):
        segmentIds = []
        features = []

        log.info("Starting ClassifyNew")
        for segmentId, G in self.provenanceGraph.getUnclassifiedSegments():
            segmentIds.append(segmentId)
            features.append(self.featureExtractor.run(G))

        log.info("Done loop ClassifyNew")

        X = numpy.array(features)
        if len(X) == 0:
            return ()

        # Create a clustering estimator
        # estimator = cluster.AffinityPropagation() # doesn't work very well for some reason
        estimator = cluster.MeanShift()
        y = estimator.fit_predict(X)

        result = zip(segmentIds, y)

        return result
