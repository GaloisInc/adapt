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
Uploads an AIDE filesystem report to Titan.
'''

__author__ = 'John.Hanley@parc.com'

import aide_reader
import argparse
import datetime
import gremlinrestclient
import logging
import os
import re
import stat

logging.basicConfig(format='%(asctime)s  %(message)s')
log = logging.getLogger(__name__)  # Will log to console (stdout).
log.setLevel(logging.INFO)


def is_too_deep(name, max_components):
    '''Predicate to limit how deeply nested directories may be.
    >>> is_too_deep('/var/log', 2)
    False
    >>> is_too_deep('/var/log', 1)
    True
    >>> is_too_deep('/var', 1)
    False
    >>> is_too_deep('/var', 0)
    True
    >>> is_too_deep('/', 1)
    False
    '''
    if name == '/':
        return False
    assert name.startswith('/'), name
    assert not name.endswith('/'), name
    components = len(name.split('/')) - 1
    return components > max_components


def ingest(fspec, db_url, max_components, log_interval=None):
    '''This inserts ~200 node/sec.'''
    db_client = gremlinrestclient.GremlinRestClient(url=db_url)
    aide = re.sub('\.gz$', '', os.path.basename(fspec))
    t0 = datetime.datetime.now()
    dirs = {}
    n = 0
    for mode, hash, size, name in aide_reader.AideReader(fspec):
        if stat.S_ISDIR(mode):
            if is_too_deep(name, max_components):
                continue  # Of 26k nodes, ignore 25k of them.
            if log_interval and n % log_interval == 0:
                elapsed = datetime.datetime.now() - t0
                log.info('%9.6f  %4d inserting %s',
                         elapsed.total_seconds(), n, name)
                t0 = datetime.datetime.now()
            assert name.startswith('/'), name
            label = '%s_%s' % (aide, name)
            add = ("graph.addVertex("
                   "label, p1, 'mode', p2,  'hash', p3,  'size', p4")
            bindings = {'p1': label, 'p2': mode, 'p3': hash, 'p4': size}
            if name != '/':  # if has_parent(name)
                parent = dirs['%s_%s' % (aide, os.path.dirname(name))]
                bindings['p5'] = parent
                add += ", 'parent', p5"
            try:
                resp = db_client.execute(add + ')', bindings=bindings)
            except gremlinrestclient.exceptions.GremlinServerError as e:
                log.error('trouble inserting %s', name)
                raise e
            dirs[label] = resp.data
            n += 1
    return n


def arg_parser():
    p = argparse.ArgumentParser(
        description='Upload an AIDE filesystem report to Titan.')
    p.add_argument('--input-file', help='a (compressed) AIDE database report',
                   default='/var/lib/aide/aide.db')
    p.add_argument('--db-url', help='Titan database location',
                   default='http://localhost:8182/')
    p.add_argument('--max-components',
                   help='pathnames longer than this will be pruned',
                   default=3)
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    ingest(args.input_file, args.db_url, args.max_components, log_interval=100)
