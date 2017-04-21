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

        label2cluster = {}
        for segmentId, label in classification:
            lbl = 'activity' + str(label)
            print(lbl)
            if lbl not in label2cluster.keys():
                cluster = provenanceGraph.createActivityCluster(lbl)
                label2cluster[lbl] = cluster['id']

            activity = provenanceGraph.createActivity(segmentId, lbl, clusterId=label2cluster[lbl])
            print("New activity node {} of type '{}' for segment {}.".format(activity['id'],
                            activity['properties']['activity:type'][0]['value'], segmentId))
