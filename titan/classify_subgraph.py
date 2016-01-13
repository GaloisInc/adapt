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
Classifies activities found in subgraphs of a SPADE trace.
'''

__author__ = 'John.Hanley@parc.com'

# from prov.model import Identifier, Literal, Namespace, PROV, ProvBundle, XSD
# from tornado import gen
# from tornado.concurrent import Future
# from tornado.ioloop import IOLoop
# import gremlinclient
#
# c.f. http://gremlinrestclient.readthedocs.org/en/latest/
# and  https://github.com/davebshow/gremlinrestclient
# sudo -H pip3 install gremlinrestclient gremlinclient
import gremlinrestclient
import argparse
import errno
import io
import os
import re


class ExfilDetector(object):

    def __init__(self, k=1):
        self.k = k
        self.recent_events = ['' * k]
        self.cmd = 'unknown'  # This is always the most recent cmd seen.
        ip_pat = r'(?P<ip>\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})'
        self._ip_re = re.compile(
            r'data:destination host="' + ip_pat + '",'
            r'\s*audit:subtype="network",\s*data:destination port=')
        self._file_re = re.compile(
            r'audit:path="(?P<fspec>[\w/\.-]+)",'
            r'\s*audit:subtype="file",')
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
        self._cmd_re = re.compile(r'audit:name="(?P<cmd>[\w\.-]+)",')  # slash?

    def scan(self, event):
        '''Updates state variable(s).'''
        m = self._cmd_re.search(event)
        if m:
            self.cmd = m.group('cmd')

    def remember(self, event):
        '''Maintain a history of recently seen events.'''
        assert self.k == 1
        self.recent_events = [event]  # Later we'll retain multiple events.

    def is_exfil(self, event):
        '''Predicate is True for sensitive file exfiltration events.'''
        recent_foreign_ip = self.get_foreign_ip(self.recent_events[0])
        return self.is_sensitive_file(event) and recent_foreign_ip

    def get_foreign_ip(self, event):
        '''Predicate is true for IPs outside of the building's firewall.'''
        m = self._ip_re.search(self.recent_events[0])
        if m:
            ip = m.group('ip')
            mh_local_ip = '13.1.101.46'  # Current monitored host is hangang.
            return ip if ip != mh_local_ip else None
        else:
            return None

    def is_sensitive_file(self, event):
        '''Predicate is True for files with restrictive markings.'''
        # NB: Analysis filesystem must be quite similar to Monitored Host FS.
        # File paths relative to cwd may require us to track additional state.
        m = self._file_re.search(event)
        if m:
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
            except OSError as e:
                if e.errno != errno.EACCES:  # 13
                    print(e)
                return False
        else:
            return False


def get_events(db_client):

    ignores = set('document endDocument'.split())
    ignore_re = re.compile(r'^\s*prefix '
                           r'|\baudit:path="(anon_inode|pipe|socket):')
    verbose_re = re.compile(r'\b(data:[0-9a-f]{5})[0-9a-f]{59}\b')
    squeeze_blanks_re = re.compile(r'\s+')
    squeeze_ids_re = re.compile(r'(?P<before>.*?)\b'
                                r'(?P<nums>(\d+)\s+(\d+)\s+(\d+)\s+(\d+))\b'
                                r'(?P<after>.*)')
    cmd_re = re.compile(r'(?P<before>.*?\baudit:commandline="sh -c )'
                        r'(?P<hexcmd>[0-9A-F]+)'
                        r'(?P<after>.*)')

    resp = db_client.execute("g.V()")

    for v in resp.data:
        print(v)
        yield v


def get_classifier():
    c = []
    for rex, classification in [
            (r'audit:commandline="cat /tmp/timestamp.txt",',
             'step3_distractor'),
            ]:
        c.append((re.compile(rex), classification))
    return c


def classify_provn_events(url):
    detector = ExfilDetector()
    c = get_classifier()

    # Edges currently are one of { used, wasGeneratedBy, wasInformedBy }.

    client = gremlinrestclient.GremlinRestClient(url=url)
    
    for event in get_events(client):
        print(event)
        if 'programName' in event:
            print(event['commandLine'])

    for event in get_events(client):
        detector.scan(event)
        if detector.is_exfil(event):
            assert detector.cmd == 'nc'
            classification = 'step4_exfiltrate_sensitive_file'
            print('\n' + classification)
        for rex, classification in c:
            if rex.search(event):
                print('\n' + classification)
        detector.remember(event)


def arg_parser():
    p = argparse.ArgumentParser(
        description='Classify activities found in subgraphs of a SPADE trace.')
    p.add_argument('--db-url', help='Titan database location',
                   default='http://localhost:8182/')
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    classify_provn_events(args.db_url)
