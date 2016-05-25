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

import classify
import logging
import unittest

__author__ = 'John.Hanley@parc.com'

log = logging.getLogger(__name__)
log.addHandler(logging.StreamHandler())
log.setLevel(logging.DEBUG)


def test_phase2():
    '''Test Ac component with upstream deps for phase2 development.'''
    exfil_detect = classify.ExfilDetector()
    ins = classify.Phase2NodeInserter()
    ins.drop_all_test_nodes()

    # precondition
    assert False is exfil_detect.is_exfil_segment(
        ins._get_segment('seg1'))

    ins.insert_reqd_events()
    ins.insert_reqd_segment()

    # postcondition
    if exfil_detect.is_exfil_segment(ins._get_segment('seg1')):
        ins._insert_node('ac1', 'classification',
                         ('classificationType',
                          'exfiltrate_sensitive_file'))
    else:
        assert None, 'phase2 test failed'


if __name__ == '__main__':
    test_phase2()
    unittest.main()
