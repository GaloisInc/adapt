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
import io
import os
import re


class ExfilDetector(Detector):
    '''
    Classifies exfiltration activities found in subgraphs of a CDM13 trace.
    '''

    def __init__(self, gremlin, k=1):
        self.gremlin = gremlin
        self.k = k
        self.recent_events = ['' * k]
        self.cmd = 'unknown'  # This is always the most recent cmd seen.
        ip_pat = r'(?P<ip>\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})'
        self._ip_re = re.compile(
            r'data:destination host="' + ip_pat + '",'
            r'\s*audit:subtype="network",\s*data:destination port=')
        self._sensitive_fspecs_re = re.compile(
            r'^C:\\Windows\\inf\\hdaudio\.PNF'
            r'|^file:///etc/shadow'
            r'|^file:///etc/passwd'
            r'|^/etc/passwd'
            r'|^/dev/video\d'
            r'|^/dev/snd/controlC1'
            r'|^/dev/snd/pcmC1D0c'
        )
        # We no longer get commands from SPADE in this way.
        # self._file_re = re.compile(
        #     r'audit:path="(?P<fspec>[\w/\.-]+)",'
        #     r'\s*audit:subtype="file",')
        self._sensitive_re = re.compile(
            'Company Confidential'
            '|Xerox Confidential'
            '|Intel Proprietary'
            '|For Official Use Only'
            '|Buckshot Yankee'
            '|Byzantine Anchor'
            '|Xkeyscore',
            re.IGNORECASE)
        self._dont_read_re = re.compile(  # Binary files, or unreadable dirs.
            r'^/(dev|proc|var/run'
            r'|lib/x86_64-linux-gnu'
            r'|usr/lib/x86_64-linux-gnu'
            r'|usr/share/locale|usr/lib/sudo)/'
            r'|^/etc/localtime')

    def name_of_input_property(self):
        return 'url'

    def name_of_output_classification(self):
        return 'exfiltration'

    def finds_feature(self, event):
        return self._is_exfil(event)

    def remember(self, cmd):
        '''Maintain a history of recently seen events.'''
        assert self.k == 1
        self.recent_events = [cmd]  # Later we'll retain multiple events.

    def _is_exfil(self, cmd):
        '''Predicate is True for sensitive file exfiltration events.'''
        # recent_foreign_ip = self.get_foreign_ip(self.recent_events[0])
        return self._is_sensitive_file(cmd)  # and recent_foreign_ip

    def _is_exfil_segment(self, nodes):
        '''Predicate is True for sensitive file exfiltration events.'''
        is_exfil = False
        for node in nodes:
            prop = node['properties']
            if 'filename' in prop:
                if self.is_sensitive_file(prop['filename'][0]['value']):
                    is_exfil = True
        return is_exfil

    def test_is_sensitive_file(self):
        '''Exercise the several conditional cases.'''
        audit_tmpl = 'audit:path="%s", audit:subtype="file",'
        assert self._is_sensitive_file(audit_tmpl % __file__)
        for tst in ['/non/existent', '/proc/meminfo',
                    '/etc/issue.net', '/etc/shadow']:
            assert not self.is_sensitive_file(audit_tmpl % tst)

    def _is_sensitive_file(self, url):
        '''Predicate is True for files with restrictive markings.'''
        m = self._sensitive_fspecs_re.search(url)
        return m is not None

        # NB: Analysis filesystem must be quite similar to Monitored Host FS.
        # File paths relative to cwd may require us to track additional state.
        # We no longer get commands from SPADE in this way.
        # if (' auditctl ' in cmd and
        #         cmd.startswith('sudo ')):
        #     return True
        # This code is unused and likely will soon be deleted.
        event = url
        m = self._file_re.search(event)
        if m:
            # fspec = url.replace('file://', '')
            fspec = m.group('fspec')
            if not os.path.exists(fspec):
                return False
            if self._dont_read_re.search(fspec):  # Avoid binary files.
                return False
            try:
                is_sensitive = False
                with io.open(fspec) as fin:
                    for line in fin:  # This won't work well on a binary file.
                        if self._sensitive_re.search(line):
                            is_sensitive = True
                return is_sensitive
            except OSError:
                # if e.errno != errno.EACCES: print(e)  # 13
                return False
        else:
            return False
