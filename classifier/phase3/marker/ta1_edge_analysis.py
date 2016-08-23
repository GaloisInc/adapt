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
# usage:
#     ./copy_traces_for_knife.sh
#     ./ta1_edge_analysis.py
#     avroknife tojson local:/tmp/knife/ta5attack2_units/ |
#       egrep --color -n 'R9Loq6TtzuZCv\+DyAftQ3g=='
#
'''
Reports on whether TA1 introduces vertices prior to referencing them.
'''
import argparse
import collections
import io
import json
import os
import subprocess


__author__ = 'John.Hanley@parc.com'


class AvroToJson:

    def __init__(self, trace_dir):
        assert os.path.isdir(trace_dir), 'must be a directory: %s' % trace_dir
        assert ' ' not in trace_dir, 'no blanks, please: %s' % trace_dir
        cmd = 'avroknife tojson local:' + trace_dir
        bad_uuids = '|'.join(['th/ZlTmjO6gUQwVzWmBFZg==',
                              'R9Loq6TtzuZCv\+DyAftQ3g==',
                              'cLL25dfPM6OjDOre3qTqzA=='])
        filtered = ['bash', '-c', cmd + ' | egrep -v "%s"' % bad_uuids]
        self.proc = subprocess.Popen(filtered, stdout=subprocess.PIPE)
        # stdout, stderr = self.proc.communicate()
        # self.knife = stdout.decode('utf8')

    def __enter__(self):
        return self

    def __exit__(self, type, value, traceback):
        self.close()

    def close(self):
        self.proc.kill()
        self.proc.wait()

    def lines(self):
        for line in io.TextIOWrapper(self.proc.stdout, encoding='utf-8'):
            yield line


class EdgeChecker:

    def __init__(self, json_src):
        self.json_src = json_src  # A generator of (avro) JSON lines.

    def get_events(self):
        '''Yields the sequence of TA1-reported Monitored Host events.'''
        i = 0
        for line in self.json_src.lines():
            datum = json.loads(line)
            assert 13 == int(datum['CDMVersion']), datum
            yield i, datum['datum']
            i += 1

    def check_edges(self):
        v = {}
        type_counts = collections.defaultdict(int)
        for i, event in self.get_events():

            try:
                type_counts[event['type']] += 1
            except KeyError:
                pass  # Some reported events are enigmatic, for example:
            # gremlin> g.V().has('ident', 'eZRBQa0ZKgG36v/TwSEdiQ==').count()
            # ==>0
            # {'uuid': 'eZRBQa0ZKgG36v/TwSEdiQ==', 'isPipe': False,
            #  'baseObject': {
            #    'lastTimestampMicros': None,
            #    'source': 'SOURCE_LINUX_AUDIT_TRACE',
            #    'permission': None,
            #    'properties': None},
            #  'version': 0,
            #  'url': 'file:///tmp/sh-thd-471399543', 'size': None}
            # This is the target of EDGE_EVENT_AFFECTS_FILE and
            # EDGE_OBJECT_PREV_VERSION edges, yet it is not an Entity-File.

            try:
                v[event['uuid']] = i
            except KeyError:
                pass

            if 'type' not in event:
                continue

            if event['type'].startswith('EDGE_'):
                v1 = event['fromUuid']
                if 'toUuid' not in event:
                    print(event)
                v2 = event['toUuid']
                assert v[v1] >= 0, event
                if v2 not in v:
                    print(v2, event)

        print('\n'.join('%6d  %s' % (v, k)
                        for k, v in sorted(type_counts.items())))


def arg_parser():
    d = 'Reports on whether TA1 introduces vertices prior to referencing them.'
    p = argparse.ArgumentParser(description=d)
    p.add_argument(
        '--trace-dir', default='/tmp/knife/ta5attack2_units/',
        help='directory containing exactly one forensic source file')
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    with AvroToJson(args.trace_dir) as src:
        EdgeChecker(src).check_edges()
