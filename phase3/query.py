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
#     ./query.py --query "g.V().has(label, 'Entity-NetFlow').limit(5000)" |
#       sort -nk2
#
'''
Ad hoc query runner to report on distinct Entity-File node values.
'''
import argparse
import collections
import dns.resolver
import gremlin_query
import re


class Prop:
    '''Models a gremlin node attribute, the "properties" map.'''

    def __init__(self, item):
        self.prop = item['properties']

    def __contains__(self, key):
        return key in self.prop

    def __getitem__(self, key):
        return self.prop[key][0]['value']


def report(query, threshold=1):

    ip4_re = re.compile('^(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})$')
    filespec_re = re.compile('^(C:|[A-Z]:|file://)')

    def validate_file(file):
        assert filespec_re.search(file), file
        return file

    def validate_ip(ip4):
        assert ip4_re.search(ip4), ip4
        return ip4

    def asn(ip4):
        '''Maps to a BGP Autonomous System Number - cannot be airgapped.'''
        rev = '.'.join(reversed(ip4.split('.')))
        answers = dns.resolver.query(rev + '.origin.asn.cymru.com', 'TXT')
        resp = 0
        for rdata in answers:
            resp = int(str(rdata).split()[0].lstrip('"'))
            # e.g. '15169 | 216.58.192.0/19 | US | arin | 2012-01-27'
        return 'AS%d' % resp

    with gremlin_query.Runner() as gremlin:

        # Number of times we've seen a given filename or address.
        counts = collections.defaultdict(int)

        for msg in gremlin.fetch(args.query):
            for item in msg.data:
                prop = Prop(item)
                try:
                    counts[validate_file(prop['url'])] += 1
                except KeyError:
                    pass

                try:
                    counts[validate_ip(prop['dstAddress'])] += 1
                    counts[asn(prop['dstAddress'])] += 1
                except KeyError:
                    pass
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
