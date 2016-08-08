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
import collections
import functools
import json
import re

__author__ = 'John.Hanley@parc.com'


class UnusualFileAccessDetector(Detector):
    '''
    Classifies a file access by a program as inevitable or unusual.

    Please see phase3/cambridge/unusual.md for additional discussion.
    '''

    def __init__(self, gremlin, max_files=1000, max_instance=6, max_prog=128):
        self.gremlin = gremlin
        self.max_files = max_files  # Ignore what a PID does after this many.
        self.max_instance = max_instance  # Retain this many process file sets.

        @functools.lru_cache(maxsize=max_prog)
        def _get_history(prog):
            # A list of recent file sets.
            instances = []
            # Maps file to sum of occurrences in all file sets.
            counts = collections.defaultdict(int)
            # print('get_hist: prog is ' + prog)
            return (instances, counts)

        self.history = _get_history

    def name_of_input_property(self):
        return 'properties'  # Uggh. Really want properties.commandLine.

    def name_of_output_classification(self):
        return 'unusual_file_access'  # Usual access is simply suppressed.

    def find_activities(self, seg_id, seg_props, threshold=12, debug=False):
        activities = []
        prog = self._get_prog(seg_props)
        if prog is None:
            return activities
        files = self._find_files(seg_props)

        instances, counts = self.history(prog)
        if debug:
            print(seg_id, prog)

        n = len(instances)
        if n == 0:
            instances.append(files)
            for file in files:
                counts[file] = 1
            return activities

        # A file is "usual" if it appears in every single historic instance.
        usual = set((file
                     for file, count in counts.items()
                     if count == n))
        unusual = files - usual

        if n == self.max_instance:
            # Oldest instance falls off the end.
            for file in instances[0]:
                counts[file] -= 1
            instances = instances[1:]
        instances.append(files)
        for file in files:
            counts[file] += 1

        for prop in seg_props:
            if 'url' in prop and prop['url'] in unusual:
                activities.append(
                    (prop['ident'], self.name_of_output_classification()))
                unusual.remove(prop['url'])  # Report each file just once.

        return activities

    def _find_files(self, seg_props):
        '''Gives segment's set of distinct filenames, with right censoring.'''
        # return set((self._extract_name(prop['properties'][0])
        return set((prop['url']
                    for prop in seg_props[:self.max_files]
                    if 'url' in prop))

    # def _extract_name(self, props):
    #     name_re = re.compile(r', name=(\w+), ')
    #     return name_re.search(props).group(1)

    def _get_prog(self, seg_props, debug=False):
        '''Gives the program name being run by the current PID, if known.'''
        for prop in seg_props:
            if 'commandLine' in prop:
                if debug:
                    print(prop)  # properties commandName?
                return prop['commandLine']
        return None
