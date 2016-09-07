#! /usr/bin/env python3

import os
import sys

from sets import Set

from ace.provenance_graph import ProvenanceGraph
from ace.unsupervised_classifier import UnsupervisedClassifier
from ace.feature_extractor import FeatureExtractor

if __name__ == '__main__':
    with ProvenanceGraph() as provenanceGraph:
        provenanceGraph.deleteActivities()

        featureExtractor = FeatureExtractor()
        activityClassifier = UnsupervisedClassifier(provenanceGraph, featureExtractor)
        classification = activityClassifier.classifyNew()

        #get distinct labels
        labels = Set([ ('activity' + str(label)) for (_,label) in classification ])
        label2cluster = {}
        for label in labels:
            cluster = provenanceGraph.createActivityCluster(label)
            label2cluster[label] = cluster['id']

        for segmentId, label in classification:
            activity = provenanceGraph.createActivity(segmentId, 'activity' + str(label),
                            clusterId=segmentId=label2cluster['activity' + str(label)])
            print("New activity node {} of type '{}' for segment {}.".format(activity['id'],
                                                                             activity['properties']['activity:type'][0]['value'],
                                                                             segmentId))
