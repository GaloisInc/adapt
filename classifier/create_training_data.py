#! /usr/bin/env python3

import os
import sys
import numpy

from ace.titan_database import TitanDatabase
from ace.provenance_graph import ProvenanceGraph
from ace.supervised_classifier import SupervisedClassifier
from ace.feature_extractor import FeatureExtractor

if __name__ == '__main__':
    with ProvenanceGraph() as provenanceGraph:
        featureExtractor = FeatureExtractor()
        activityClassifier = SupervisedClassifier(provenanceGraph, featureExtractor)
        labels, X = activityClassifier.createTrainingData()

        numpy.savez('training_data.npz', labels, X)
