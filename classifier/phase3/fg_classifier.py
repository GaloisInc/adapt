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
import ometa.runtime
import os
import parsley
import re
import struct
import sys
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


def get_grammar():
    gr = """
# Declare some terminals.

access_sensitive_file = LEAF
http_post_activity = LEAF
ssh_post_activity = LEAF
tcp_activity = LEAF
scanning = LEAF

exfil_channel =   http_post_activity
                | ssh_activity
exfil_execution = exfil_channel tcp_activity
exfil_format = compress_activity? encrypt_activity?
"""
    # Turn abbreviated declarations of foo = LEAF into: foo = 'foo'
    leaf_re = re.compile(r'^(\w+)(\s*=\s*)LEAF$', flags=re.MULTILINE)
    return leaf_re.sub("\\1\\2'\\1'", gr)


def parsley_demo(apt_grammar):
    terminal = 'access_sensitive_file'  # An example that matches the grammar.
    g = apt_grammar(terminal)
    assert terminal == g.access_sensitive_file()

    g = apt_grammar('random junk ' + terminal)
    try:
        print(g.access_sensitive_file())
    except ometa.runtime.ParseError:
        pass  # The parsed string didn't match the grammar, as expected.


def report_status(status, downstreams='dx ui'.split()):
    def to_int(status_byte):
        return struct.unpack("B", status_byte)[0]

    log.debug("reporting %d", to_int(status))
    producer = kafka.KafkaProducer()
    for downstream in downstreams:
        s = producer.send(downstream, status).get()
        log.debug("sent: %s", s)


def drop_activities(gremlin):
    log.warn('Dropping any existing activities.')
    q = "g.V().has(label, 'Activity').drop().iterate()"
    gremlin.fetch_data(q)


def arg_parser():
    p = argparse.ArgumentParser(
        description='Classify activities found in segments of a CDM13 trace.')
    p.add_argument('--drop-all-existing-activities', action='store_true',
                   help='destructive, useful during testing')
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    grammar = parsley.makeGrammar(get_grammar(), {})
    parsley_demo(grammar)
    with gremlin_query.Runner() as gremlin:
        if args.drop_all_existing_activities:
            drop_activities(gremlin)
        ac = classify.ActivityClassifier(gremlin, grammar)
        ac.classify(ac.find_new_segments('s'))
        log.info('Fetched %d base nodes.' % ac.num_nodes_fetched)
        log.info('Inserted %d activity classifications.' %
            ac.num_classifications_inserted)

    report_status(STATUS_DONE)
