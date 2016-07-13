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
Pause until hearing a kafka "done" message for some component.
'''
import argparse
import kafka
import logging

__author__ = 'John.Hanley@parc.com'

log = logging.getLogger(__name__)
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
handler = logging.StreamHandler()
handler.setFormatter(formatter)
log.addHandler(handler)
log.setLevel(logging.INFO)


# STATUS_IN_PROGRESS = b'\x00'
STATUS_DONE = b'\x01'


class Waiter:

    def __init__(self, topic, url):
        self.topic = topic
        self.consumer = kafka.KafkaConsumer(bootstrap_servers=[url])
        self.consumer.assign([kafka.TopicPartition(topic, 0)])

    def await_message(self):
        self.consumer.seek_to_end()
        log.info("Waiting for %s kafka message..." % self.topic)
        done = False
        while not done:
            for msg in self.consumer:
                log.info("recvd msg: %s", msg)
                if msg.value == STATUS_DONE:
                    done = True
                    break


def arg_parser():
    p = argparse.ArgumentParser(
        description='Pause until hearing a kafka "done" message.')
    p.add_argument('--kafka', help='location of the kafka pub-sub service',
                   default='localhost:9092')
    p.add_argument('topic',
                   help='Listen for "done" on this kafka channel, e.g.: se')
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    Waiter(args.topic, args.kafka).await_message()
