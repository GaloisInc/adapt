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
A pretend segmenter.
'''
import argparse
import kafka
import logging
import struct
import time

__author__ = 'John.Hanley@parc.com'

log = logging.getLogger(__name__)
log.addHandler(logging.StreamHandler())
log.setLevel(logging.INFO)

STATUS_IN_PROGRESS = b'\x00'
STATUS_DONE = b'\x01'


class ErsatzSegmenter:

    def __init__(self, kafka_loc):
        self.producer = kafka.KafkaProducer(bootstrap_servers=[kafka_loc])

    def complete_all_segmentation(self):
        log.info("Working to produce segments...")
        self.report_status(STATUS_IN_PROGRESS)
        time.sleep(1)
        log.info("Finished producing segments.")
        self.report_status(STATUS_DONE)

    def report_status(self, status, downstreams='ac pe px'.split()):
        def to_int(status_byte):
            return struct.unpack("B", status_byte)[0]

        log.info("reporting %d", to_int(status))
        for downstream in downstreams:
            self.producer.send(downstream, status)


def arg_parser():
    p = argparse.ArgumentParser(
        description='Pretend to segment recent base nodes in a CDM trace.')
    p.add_argument('--kafka', help='location of the kafka pub-sub service',
                   default='localhost:9092')
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    ErsatzSegmenter(args.kafka).complete_all_segmentation()
