#! /usr/bin/env python3

# Copyright 2016, Palo Alto Research Center.
# Developed with sponsorship of DARPA.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# The software is provided "AS IS", without warranty of any kind, express or
# implied, including but not limited to the warranties of merchantability,
# fitness for a particular purpose and noninfringement. In no event shall the
# authors or copyright holders be liable for any claim, damages or other
# liability, whether in an action of contract, tort or otherwise, arising from,
# out of or in connection with the software or the use or other dealings in
# the software.
#
'''
The classifier daemon - a kafka consumer.
'''
import argparse
import kafka
import logging
import struct
import time
import os

from ace.provenance_graph import ProvenanceGraph
from ace.unsupervised_classifier import UnsupervisedClassifier
from ace.feature_extractor import FeatureExtractor

log = logging.getLogger(__name__)
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
#handler = logging.StreamHandler()
handler = logging.FileHandler(os.path.expanduser('~/adapt/classifier/ac.log'))
handler.setFormatter(formatter)
log.addHandler(handler)
log.setLevel(logging.INFO)

STATUS_IN_PROGRESS = b'\x00'
STATUS_DONE = b'\x01'

class TopLevelClassifier(object):
    def __init__(self, url):
        # Kafka might not be availble yet, due to supervisord race.
        retries = 6  # Eventually we may signal fatal error; this is a feature.
        self.consumer = None
        while retries >= 0 and self.consumer is None:
            try:
                retries -= 1
                self.consumer = kafka.KafkaConsumer('ac', bootstrap_servers = [url])
            except kafka.errors.NoBrokersAvailable:
                log.warn('Kafka not yet available.')
                time.sleep(2)
                log.warn('retrying')
        # The producer relies on kafka-python-1.1.1 (not 0.9.5).
        self.producer = kafka.KafkaProducer(bootstrap_servers = [url])

        self.provenanceGraph = ProvenanceGraph()
        self.featureExtractor = FeatureExtractor()
        self.activityClassifier = UnsupervisedClassifier(self.provenanceGraph, self.featureExtractor)

    def await_segments(self, start_msg = "Awaiting new segments..."):
        log.info(start_msg)
        self.producer.send("ac-log", bytes(start_msg, encoding='utf-8'))
        for msg in self.consumer:
            self.consumer.commit()
            log.info("recvd msg: %s", msg)
            if msg.value == STATUS_DONE:  # from Ad
                self.report_status(STATUS_IN_PROGRESS)
                self.cluster_segments()
                self.report_status(STATUS_DONE)
                log.info(start_msg)  # Go back and do it all again.

    def cluster_segments(self):
        log.info("starting processing...")
        self.producer.send("ac-log", b'starting processing (deleting current set of activities)...')

        self.provenanceGraph.deleteActivities()
        self.producer.send("ac-log", b'Deleting process done.')

        classification = self.activityClassifier.classifyNew()

        self.producer.send("ac-log", b'** Adding Activities to DB. **')
        for segmentId, label in classification:
            self.producer.send("ac-log", bytes('Adding activitiy to Segment {}').format(segmentId))
            activity = self.provenanceGraph.createActivity(segmentId, 'activity' + str(label))
            self.producer.send("ac-log",
                               bytes("new activity node {} of type '{}' for segment {}.".format(activity['id'],
                                                                                                activity['properties']['activity:type'][0]['value'],
                                                                                                segmentId),
                                     'utf-8'))

        self.producer.send("ac-log", b'** Adding Activities to DB. **')

    def report_status(self, status, downstreams = 'dx ui'.split()):
        def to_int(status_byte):
            return struct.unpack("B", status_byte)[0]

        log.info("reporting %d", to_int(status))
        for downstream in downstreams:
            s = self.producer.send(downstream, status)
            log.info("sent: %s", s)

def arg_parser():
    p = argparse.ArgumentParser(description = 'Classify activities found in segments of a CDM trace.')
    p.add_argument('--kafka',
                   help = 'location of the kafka pub-sub service',
                   default='localhost:9092')
    return p

if __name__ == '__main__':
    args = arg_parser().parse_args()
    TopLevelClassifier(args.kafka).await_segments()
