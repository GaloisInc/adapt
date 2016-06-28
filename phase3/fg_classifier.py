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
The foreground classifier is a kafka producer.
'''
import argparse
import classify
import kafka
import logging
import os
import struct
import sys
import time
sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_query

__author__ = 'John.Hanley@parc.com'

log = logging.getLogger(__name__)
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
handler = logging.StreamHandler()
handler.setFormatter(formatter)
log.addHandler(handler)
log.setLevel(logging.INFO)


STATUS_IN_PROGRESS = b'\x00'
STATUS_DONE = b'\x01'


def report_status(status, downstreams='dx ui'.split()):
    def to_int(status_byte):
        return struct.unpack("B", status_byte)[0]

    log.info("reporting %d", to_int(status))
    producer = kafka.KafkaProducer()
    for downstream in downstreams:
        s = producer.send(downstream, status).get()
        log.debug("sent: %s", s)


def arg_parser():
    p = argparse.ArgumentParser(
        description='Classify activities found in segments of a CDM13 trace.')
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    with gremlin_query.Runner() as gremlin:
        ac = classify.ActivityClassifier(gremlin)
        ac.classify(ac.find_new_segments(0))
    report_status(STATUS_DONE)
