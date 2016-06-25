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
'''
Ad hoc query runner to report on distinct Entity-File node values.
'''
import argparse
import collections
import gremlin_query
import re


def report(query, threshold=3):
    with gremlin_query.Runner() as gremlin:

        # Number of times we've seen a given filename.
        counts = collections.defaultdict(int)

        fspec_re = re.compile('^(C:|file://)')

        for msg in gremlin.fetch(args.query):
            for item in msg.data:
                prop = item['properties']
                if 'url' in prop:
                    file = prop['url'][0]['value']
                    assert fspec_re.search(file), file
                    counts[file] += 1
        i = 1
        for file, count in sorted(counts.items()):
            if count >= threshold:
                print('%3d %4d  %s' % (i, count, file))
            i += 1


def arg_parser():
    p = argparse.ArgumentParser(
        description='Ad hoc query runner to report on Entity-File values.')
    p.add_argument('--query', help='gremlin query to run',
                   default="g.V().has(label, 'Entity-File').limit(5000)")
    return p


if __name__ == '__main__':

    args = arg_parser().parse_args()
    report(args.query)
