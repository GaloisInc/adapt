#! /usr/bin/env python3

# Copyright 2016, University of Edinburgh
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
The segmenter daemon - a kafka consumer.
'''
import argparse
import kafka
import logging
import os
import struct
import time

__author__ = 'jcheney@inf.ed.ac.uk'

log = logging.getLogger(__name__)
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
handler = logging.StreamHandler()
handler.setFormatter(formatter)
log.addHandler(handler)
log.setLevel(logging.INFO)


STATUS_IN_PROGRESS = b'\x00'
STATUS_DONE = b'\x01'


class TopLevelSegmenter:

    def __init__(self, url):
        # Kafka might not be availble yet, due to supervisord race.
        retries = 6  # Eventually we may signal fatal error; this is a feature.
        self.consumer = None
        while retries >= 0 and self.consumer is None:
            try:
                retries -= 1
                self.consumer = kafka.KafkaConsumer(
                    'se', bootstrap_servers=[url])
            except kafka.errors.NoBrokersAvailable:
                log.warn('Kafka not yet available.')
                time.sleep(2)
                log.warn('retrying')
        # The producer relies on kafka-python-1.1.1 (not 0.9.5).
        self.producer = kafka.KafkaProducer(bootstrap_servers=[url])

               
    def await_ingest(self, broker, spec, kafkaUrl, processes,start_msg='Awaiting ingested data.'):
        os.chdir(os.path.expanduser('~/adapt/segment/segmenter'))
        log.info(start_msg)
        for msg in self.consumer:
            self.consumer.commit()
            log.info("recvd msg: %s", msg)
            if msg.value == STATUS_DONE:  # from Ingest
                self.producer.send("se-log", b'starting processing')
                self.report_status(STATUS_IN_PROGRESS)
                cmd = './adapt_segmenter.py --broker %s --radius-segment --criterion pid --radius 2 --name byPID --spec %s --log-to-kafka --kafka %s --processes %d' % (
                        broker, spec, kafkaUrl, processes)
                log.info(cmd)
                os.system(cmd)
                cmd = './adapt_segmenter.py --broker %s --segment-edges --name byPID --spec %s --log-to-kafka --kafka %s --processes %d' % (
                        broker, spec, kafkaUrl, processes)
                log.info(cmd)
                os.system(cmd)
                cmd = './adapt_segmenter.py --broker %s --time-segment --window 60 --name byTime --spec %s --log-to-kafka --kafka %s --processes %d' % (
                        broker, spec, kafkaUrl, processes)
                log.info(cmd)
                os.system(cmd)
                self.report_status(STATUS_DONE)
                self.producer.send("se-log", b'done processing')
                log.info(start_msg)  # Go back and do it all again.

    def report_status(self, status):
        def to_int(status_byte):
            return struct.unpack("B", status_byte)[0]

        log.info("reporting %d", to_int(status))
        # Segmenter only talks to AD
        s = self.producer.send("ad", status).get()
        log.info("sent: %s", s)


def arg_parser():
    p = argparse.ArgumentParser(
        description='Perform segmentation according to a given specification.')
    p.add_argument('--broker', help='location of the database broker',
                   default='ws://localhost:8182/')
    p.add_argument('--kafka', help='location of the kafka pub-sub service',
                   default='localhost:9092')
    p.add_argument('--spec', help='Segmentation specification to use',
                   default='/home/vagrant/adapt/segment/segmenter/test/spec/segmentByPID.json')
    p.add_argument('--processes', help='Number of transactions to spawn in parallel', default=1)
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    TopLevelSegmenter(args.kafka).await_ingest(args.broker, args.spec, args.kafka, int(args.processes))
