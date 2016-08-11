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
#     ./time_span_avro.py
#
'''
Reports on time elapsed during a trace,
which is read from a forensic avro file.
'''
import argparse
import collections
import datetime
import io
import ipaddress
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


def ip(addr46):
    '''Converts address like 10.0.0.1 to 010.000.000.001, for sorting.'''
    if addr46 == '':
        addr46 = '0.0.0.0'
    addr = ipaddress.ip_address(addr46)
    if isinstance(addr, ipaddress.IPv6Address):
        # Convert short address like ::1 to constant length address.
        assert len(addr.packed) == 16, addr
        p = addr.packed
        ws = [256 * p[i] + p[i + 1]
              for i in range(0, 15, 2)]
        return '[%s]' % ':'.join(['%02x' % v for v in ws])
    else:
        return '.'.join(['%03d' % b for b in addr.packed])


def fmt_addr(s):
    if s.startswith('port '):
        return s
    # Compress zeros.
    if s.startswith('['):
        return '[%s]' % ipaddress.ip_address(s.strip('[]'))
    # Strip leading zeros from e.g. 127.000.000.001.
    ip4 = '.'.join([str(int(b)) for b in s.split('.')])
    return str(ipaddress.ip_address(ip4))


class TimeSpan:

    def __init__(self, json_src):
        self.json_src = json_src  # A generator of (avro) JSON lines.

    def get_events(self):
        '''Yields the sequence of TA1-reported Monitored Host events.'''
        for line in self.json_src.lines():
            datum = json.loads(line)
            assert 13 == int(datum['CDMVersion']), datum
            yield datum['datum']

    def report(self, show_commands=False):
        cmds = collections.defaultdict(int)
        pids = collections.defaultdict(int)
        addrs = collections.defaultdict(int)

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

            try:
                pids[event['pid']] += 1  # Typically has only one occurence.
                if 'cmdLine' in event:   # Grrr, ta5attack2 has cmdLine: None.
                    p = event['properties']
                    cmds[p['name']] += 1
            except KeyError:
                pass

            try:
                addrs[ip(event['srcAddress'])] += 1
                addrs[ip(event['destAddress'])] += 1
                if event['destPort'] > 0:
                    addrs['port %d' % event['destPort']] += 1
                del(addrs['000.000.000.000'])
            except KeyError:
                pass

        b = datetime.datetime.utcfromtimestamp(b / 1e6)
        e = datetime.datetime.utcfromtimestamp(e / 1e6)
        print('begin:  ', b, '\nend:    ', e, '\nelapsed:', e - b)
        print(n, 'records, of which', m, 'were timestamped.')

        if show_commands:
            print('\n')
            print('\n'.join(['%4d  %s' % (v, k)
                             for k, v in sorted(cmds.items())]))
            print('\n%d PIDs appear in the trace:' % len(pids))
            print(', '.join(map(str, sorted(pids))))
            print('\n')
            print('\n'.join(['%4d  %s' % (v, fmt_addr(k))
                             for k, v in sorted(addrs.items())]))


def arg_parser():
    d = 'Reports on time elapsed during a trace, read from a forensic file.'
    p = argparse.ArgumentParser(description=d)
    p.add_argument(
        '--trace-dir', default='/tmp/knife/ta5attack2_units/',
        help='directory containing exactly one forensic source file')
    p.add_argument('--commands', action='store_true',
                   help='Also report on commands executed and PIDs seen.')
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    with AvroToJson(args.trace_dir) as src:
        TimeSpan(src).report(args.commands)
