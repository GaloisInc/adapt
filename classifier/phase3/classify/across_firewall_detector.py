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


class AcrossFirewallDetector(Detector):
    '''
    Detects netflows between an intranet and an internet end-point.
    '''

    def __init__(self, gremlin):
        # self.gremlin = gremlin  # unused
        # Second prefix is 172.16.0.0/12, extending up to 172.31.255.255.
        self._rfc1597 = re.compile(
            r'^10\.\d{1,3}\.\d{1,3}\.\d{1,3}$'
            r'^172\.(1[6-9]|2\d|3[01])\.\d{1,3}\.\d{1,3}$'
            r'^192\.168\.\d{1,3}\.\d{1,3}$'
            r'^169\.254\.\d{1,3}\.\d{1,3}$'
            r'^198\.1[89]\.\d{1,3}\.\d{1,3}$'
            r'^127\.\d{1,3}\.\d{1,3}\.\d{1,3}$'
        )
        self._ip4 = re.compile(r'^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$')

    def name_of_input_property(self):
        return 'dstAddress'

    def name_of_output_classification(self):
        return 'internet_access'

    def finds_feature(self, event):
        return self._is_internet_access(event)

    def _is_internet_access(self, address):
        assert self._ip4.search(address), address  # Please supply an IP.
        m = self._rfc1597.search(address)
        return m is not None

    def find_activities(self, seg_id, seg_props):
        activities = super().find_activities(seg_id, seg_props)
        if len(activities) > 0:
            # Leave a note for the SensitiveFileDetector.
            seg_props[name_of_output_classification()] = True
        return activities
