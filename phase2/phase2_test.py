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
Writes one or more classification nodes to Titan / Cassandra.
'''

import aiogremlin
import asyncio
import argparse
import classify
import logging
import re
import sys
import uuid

__author__ = 'John.Hanley@parc.com'

log = logging.getLogger(__name__)
log.addHandler(logging.StreamHandler())
log.setLevel(logging.DEBUG)



    def phase2(self):
        '''Test Ac component with upstream deps for phase2 development.'''
        exfil_detect = classify.ExfilDetector()
        self._drop_all_test_nodes()

        # precondition
        assert False == exfil_detect.is_exfil_segment(
            self._get_segment('seg1'))

        self._insert_reqd_events()
        self._insert_reqd_segment()

        # postcondition
        if exfil_detect.is_exfil_segment(self._get_segment('seg1')):
            self._insert_node('ac1', 'classification',
                              ('classificationType',
                               'exfiltrate_sensitive_file'))
        else:
            assert None, 'phase1 test failed'


def arg_parser():
    p = argparse.ArgumentParser(
        description='Writes one or more nodes to Titan / Cassandra.')
    p.add_argument('--db-url', help='Titan database location',
                   default='http://localhost:8182/')
    return p


# Invoke with:
#   $ nosetests3 *.py
def test_insert():
    ins = NodeInserter()
    ins.phase2()


if __name__ == '__main__':
    args = arg_parser().parse_args()
    ins = NodeInserter(args.db_url)
    ins.phase2()
