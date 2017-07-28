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
Injects a kafka message that says some component is done with processing.
'''
import argparse
import kafka
import logging
import struct

__author__ = 'John.Hanley@parc.com'

log = logging.getLogger(__name__)
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
handler = logging.StreamHandler()
handler.setFormatter(formatter)
log.addHandler(handler)
log.setLevel(logging.INFO)


STATUS_IN_PROGRESS = b'\x00'
STATUS_DONE = b'\x01'


class Injector:

    def __init__(self, url):
        self.producer = kafka.KafkaProducer(bootstrap_servers=[url])

    def report_status(self, status, topic):
        def to_int(status_byte):
            return struct.unpack("B", status_byte)[0]

        log.info("reporting %d", to_int(status))
        s = self.producer.send(topic, status).get()
        log.info("sent: %s", s)


def arg_parser():
    p = argparse.ArgumentParser(
        description='Injects a "done" kafka message,'
        ' signaling that processing is complete.')
    p.add_argument('--kafka', help='location of the kafka pub-sub service',
                   default='localhost:9092')
    p.add_argument('topic',
                   help="Send message via this kafka channel, e.g.: se")
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    Injector(args.kafka).report_status(STATUS_DONE, args.topic)
