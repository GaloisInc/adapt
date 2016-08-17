#! /usr/bin/env python3

import os
import sys

from ace.titan_database import TitanDatabase
from ace.provenance_graph import ProvenanceGraph
from ace.unsupervised_classifier import UnsupervisedClassifier
from ace.feature_extractor import FeatureExtractor

if __name__ == '__main__':
    with ProvenanceGraph() as provenanceGraph:
        provenanceGraph.deleteActivities()

        featureExtractor = FeatureExtractor()
        activityClassifier = UnsupervisedClassifier(provenanceGraph, featureExtractor)
        classification = activityClassifier.run()
        for segmentId, label in classification:
            provenanceGraph.createActivity(segmentId, 'activity' + str(label))

        for activity in provenanceGraph.activityNodes():
            print("Created a new activity node {} with value '{}'.".format(activity['id'],
                                                                           activity['properties']['activity:type'][0]['value']))
