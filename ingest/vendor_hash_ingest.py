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
Uploads a vendor's software release hashes to Titan.
'''

__author__ = 'John.Hanley@parc.com'

import argparse
import gremlinrestclient
import logging
import re

logging.basicConfig(format='%(asctime)s  %(message)s')
log = logging.getLogger(__name__)
log.setLevel(logging.INFO)


def get_pairs(fin, min_hash_len=33):
    pair_re = re.compile(r'^(?P<hash>[a-f\d]+)\s+(?P<name>/.*)')
    for line in fin:
        m = pair_re.search(line)
        assert m, line
        if len(m.group('hash')) >= min_hash_len:  # Reject md5's.
            yield m.group('hash'), m.group('name')


def ingest(input_file, db_url):
    db_client = gremlinrestclient.GremlinRestClient(url=db_url)
    with open(input_file) as fin:
        for hash, name in get_pairs(fin):
            label = 'vendor_hash_' + name
            add = ("graph.addVertex(label, p1, 'hash', p2)")
            bindings = {'p1': label, 'p2': hash}
            try:
                db_client.execute(add, bindings=bindings)
            except gremlinrestclient.exceptions.GremlinServerError as e:
                log.error('trouble inserting %s', name)
                raise e


def arg_parser():
    p = argparse.ArgumentParser(
        description='Uploads vendor software release hashes to Titan.')
    p.add_argument('--input-file', help='2-column file of hashes & filespecs',
                   default='sha256sums')
    p.add_argument('--db-url', help='Titan database location',
                   default='http://localhost:8182/')
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    ingest(**vars(args))
