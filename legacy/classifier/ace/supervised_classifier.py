# -*- coding: utf-8 -*-

import numpy
import pickle
import pprint

from sklearn import preprocessing
from sklearn import ensemble

class SupervisedClassifier(object):
    def __init__(self, provenanceGraph, featureExtractor):
        self.provenanceGraph = provenanceGraph
        self.featureExtractor = featureExtractor

    def createTrainingData(self):
        segmentIds = []
        features = []

        for segmentId, G in self.provenanceGraph.getClassifiedSegments():
            segmentIds.append(segmentId)
            features.append(self.featureExtractor.run(G))

        X = numpy.array(features)

        labels = self.provenanceGraph.getActivityTypes(segmentIds)

        labelEncoder = preprocessing.LabelEncoder()
        y = labelEncoder.fit_transform(labels)

        return X, y, labelEncoder

    def createClassifier(self, fn):
        X, y, labelEncoder = self.createTrainingData()

        classifier = ensemble.RandomForestClassifier()
        classifier.fit(X, y)

        with open(fn, 'wb') as fid:
            pickle.dump(classifier, fid)
            pickle.dump(labelEncoder, fid)

    def loadClassifier(self, fn):
        with open(fn, 'rb') as fid:
            classifier = pickle.load(fid)
            labelEncoder = pickle.load(fid)

            return classifier, labelEncoder

    def classifyNew(self, classifier, labelEncoder):
        segmentIds = []
        features = []

        for segmentId, G in self.provenanceGraph.getUnclassifiedSegments():
            segmentIds.append(segmentId)
            features.append(self.featureExtractor.run(G))

        if len(features) == 0:
            return zip()

        X = numpy.array(features)

        y = classifier.predict(X)

        labels = labelEncoder.inverse_transform(y)

        result = zip(segmentIds, labels)

        return result
