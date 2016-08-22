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


class SensitiveFileDetector(Detector):
    '''
    Detects access of files that are manually tagged CONFIDENTIALITY_SENSITIVE.
    '''

    def __init__(self, gremlin):
        self.gremlin = gremlin
        self._sensitive_fspecs_re = re.compile(
            r'^C:\\Windows\\inf\\hdaudio\.PNF'
            r'|^file:///etc/shadow'
            r'|^file:///etc/passwd'
            r'|^/etc/passwd'
            r'|^/dev/video\d'
            r'|^/dev/snd/controlC1'
            r'|^/dev/snd/pcmC1D0c'
        )

    def name_of_input_property(self):
        return 'url'

    def name_of_output_classification(self):
        return 'access_sensitive_file'

    def finds_feature(self, event):
        return self._is_sensitive_file(event)

    def _is_sensitive_file(self, url):
        '''Predicate is True for files with restrictive markings.'''
        m = self._sensitive_fspecs_re.search(url)
        return m is not None

    def find_activities(self, seg_id, seg_props):
        activities = super().find_activities(seg_id, seg_props)
        if 'internet_access' in seg_props and len(activities) > 0:
            # This pid used both TCP + file.
            ident, c = activities[0]
            activities.append((ident, 'exfil_execution'))
        return activities
