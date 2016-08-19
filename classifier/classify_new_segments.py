#! /usr/bin/env python3

import os
import sys
import pprint

from ace.titan_database import TitanDatabase
from ace.provenance_graph import ProvenanceGraph
from ace.supervised_classifier import SupervisedClassifier
from ace.feature_extractor import FeatureExtractor

if __name__ == '__main__':
    with ProvenanceGraph() as provenanceGraph:
        featureExtractor = FeatureExtractor()
        activityClassifier = SupervisedClassifier(provenanceGraph, featureExtractor)

        classifier, labelEncoder = activityClassifier.loadClassifier('classifier.pkl')
        
        classification = activityClassifier.classifyNew(classifier, labelEncoder)

        for segmentId, label in classification:
            activity = provenanceGraph.createActivity(segmentId, label)
            print("New activity node {} of type '{}' for segment {}.".format(activity['id'],
                                                                             activity['properties']['activity:type'][0]['value'],
                                                                             segmentId))
