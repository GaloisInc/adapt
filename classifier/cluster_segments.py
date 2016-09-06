#! /usr/bin/env python3

import os
import sys

from ace.provenance_graph import ProvenanceGraph
from ace.unsupervised_classifier import UnsupervisedClassifier
from ace.feature_extractor import FeatureExtractor

if __name__ == '__main__':
    with ProvenanceGraph() as provenanceGraph:
        provenanceGraph.deleteActivities()

        featureExtractor = FeatureExtractor()
        activityClassifier = UnsupervisedClassifier(provenanceGraph, featureExtractor)
        classification = activityClassifier.classifyNew()
        for segmentId, label in classification:
            activity = provenanceGraph.createActivity(segmentId, 'activity' + str(label))
            print("New activity node {} of type '{}' for segment {}.".format(activity['id'],
                                                                             activity['properties']['activity:type'][0]['value'],
                                                                             segmentId))
