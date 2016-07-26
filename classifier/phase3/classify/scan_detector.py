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

from .detector import Detector
import re

__author__ = 'John.Hanley@parc.com'


class ScanDetector(Detector):
    '''
    Classifies scanning activities found in subgraphs of a CDM13 trace.
    '''

    def __init__(self, gremlin):
        self.gremlin = gremlin
        self._scan_url_re = re.compile(
            r'^file:///proc/\d+/cmdline'
            r'|^file:///proc/\d+/status'
            r'|^file:///proc/\d+/stat'
            )

    def name_of_input_property(self):
        return 'url'

    def name_of_output_classification(self):
        return 'scanning'

    def finds_feature(self, event):
        return self._is_part_of_scan(event)

    def _is_part_of_scan(self, url):
        '''Predicate is True for url access that could be part of scan.'''
        return self._scan_url_re.search(url)
