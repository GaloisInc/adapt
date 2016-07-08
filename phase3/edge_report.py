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
#     ./edge_report.py --report=EDGE_EVENT_AFFECTS_FILE | sort -n
#
# cf ./edge_report.py --help
#
'''
Ad hoc query runner to report on CDM provenance edges.
'''
import argparse
import os
import sys
sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_properties
import gremlin_query


def report(query, debug=False):

    with gremlin_query.Runner() as gremlin:

        for prop in gremlin_properties.fetch(gremlin, query):

            if debug:
                print(prop.prop)

            try:
                print('%4d' % prop['file-version'], prop['url'])
            except KeyError:
                pass

    print(-1, query)


def get_canned_reports():
    labels = ('EDGE_EVENT_AFFECTS_FILE'
              ' EDGE_EVENT_AFFECTS_MEMORY'
              ' EDGE_EVENT_AFFECTS_SRCSINK'
              ' EDGE_EVENT_AFFECTS_SUBJECT'
              ' EDGE_EVENT_ISGENERATEDBY_SUBJECT'
              ' EDGE_FILE_AFFECTS_EVENT'
              ' EDGE_MEMORY_AFFECTS_EVENT'
              ' EDGE_OBJECT_PREV_VERSION'
              ' EDGE_SRCSINK_AFFECTS_EVENT'
              ' EDGE_SUBJECT_HASLOCALPRINCIPAL')
    return {label: "g.V().has(label, '%s').out().limit(5000)" % label
            for label in labels.split()}


def arg_parser():
    p = argparse.ArgumentParser(
        description='Ad hoc query runner to report on CDM provenance edges.')
    p.add_argument('--query', help='gremlin query to run')
    p.add_argument('--report', help='name of canned report to run',
                   choices=sorted(get_canned_reports().keys()))
    p.add_argument('--debug', help='verbose output', action='store_true')
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    if args.report:
        args.query = get_canned_reports()[args.report]
    if args.query is None:
        arg_parser().error('Please specify a query or choose a canned report.')
    report(args.query, debug=args.debug)
