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
#     ./time_span.py
#
'''
Reports on elapsed time during a trace, read from a forensic avro file.
'''
import argparse
import datetime
import io
import json
import math
import os
import subprocess


__author__ = 'John.Hanley@parc.com'


class AvroToJson:

    def __init__(self, trace_dir):
        assert os.path.isdir(trace_dir), 'must be a directory: %s' % trace_dir
        assert ' ' not in trace_dir, 'no blanks, please: %s' % trace_dir
        cmd = 'avroknife tojson local:' + trace_dir
        self.proc = subprocess.Popen(cmd.split(), stdout=subprocess.PIPE)

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


class TimeSpan:

    def __init__(self, json_src):
        self.json_src = json_src  # A generator of (avro) JSON lines.

    def get_events(self):
        '''Yields the sequence of TA1-reported Monitored Host events.'''
        for line in self.json_src.lines():
            datum = json.loads(line)
            assert 13 == int(datum['CDMVersion']), datum
            yield datum['datum']

    def report(self):
        b, e, n, m = math.inf, 0, 0, 0
        for event in self.get_events():
            try:
                n += 1
                ts = event['startTimestampMicros']
                if ts is None:
                    continue
                b = min(b, ts)
                e = max(e, ts)
                m += 1
            except KeyError:
                pass

        b = datetime.datetime.utcfromtimestamp(b / 1e6)
        e = datetime.datetime.utcfromtimestamp(e / 1e6)
        print('begin:  ', b, '\nend:    ', e, '\nelapsed:', e - b)
        print(n, 'records, of which', m, 'were timestamped.')


def arg_parser():
    d = 'Reports on elapsed time during a trace, read from a forensic file.'
    p = argparse.ArgumentParser(description=d)
    p.add_argument(
        '--trace-dir', default='/tmp/knife/ta5attack2_units/',
        help='directory containing exactly one forensic source file')
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    with AvroToJson(args.trace_dir) as src:
        TimeSpan(src).report()
