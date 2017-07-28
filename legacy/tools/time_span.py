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
#     tools/time_span.py
#
'''
Reports on the span of time covered by the currently loaded trace.
'''
import datetime
import os
import sys
sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_query


def report(gremlin):

    stamps = []

    # Dates before 1971 fail the sanity check and are rejected.
    sane = 365 * 86400 * 1e6

    queries = [
        "g.V().values('startedAtTime').is(gt(%d)).min()" % sane,
        "g.V().values('startedAtTime').max()",
        ]

    for query in queries:
        for msg in gremlin.fetch(query):
            if msg.data:
                for usec in msg.data:
                    stamp = datetime.datetime.utcfromtimestamp(usec / 1e6)
                    print(stamp)
                    stamps.append(stamp)
    delta = stamps[1] - stamps[0]
    print(delta, 'elapsed time')


if __name__ == '__main__':
    with gremlin_query.Runner() as gremlin:
        report(gremlin)
