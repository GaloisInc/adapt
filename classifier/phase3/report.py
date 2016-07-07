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
#     ./report.py --query "g.V().has(label, 'Entity-NetFlow').limit(5000)" |
#       sort -nk2
#
'''
Ad hoc query runner to report on distinct Entity-File node values.
'''
import argparse
import collections
import dns.resolver
import json
import os
import re
import sys
sys.path.append(os.path.expanduser('~/adapt/tools'))
import cdm.enums
import gremlin_properties
import gremlin_query


def report(query, threshold=1, debug=False):

    ip4_re = re.compile('^(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})$')
    filespec_re = re.compile('^(C:|[A-Z]:|file://)')

    def validate_file(file):
        assert filespec_re.search(file), file
        return file

    def validate_ip(ip4):
        assert ip4_re.search(ip4), ip4
        return ip4

    def get_asn(ip4):
        '''Maps to a BGP Autonomous System Number - cannot be airgapped.'''
        rev = '.'.join(reversed(ip4.split('.')))
        answers = dns.resolver.query(rev + '.origin.asn.cymru.com', 'TXT')
        resp = 0
        for rdata in answers:
            resp = int(str(rdata).split()[0].lstrip('"'))
            # e.g. "15169 | 216.58.192.0/19 | US | arin | 2012-01-27"
        return 'AS%d' % resp

    def get_asn_name(asn):
        '''Maps e.g. AS15169 to GOOGLE.'''
        assert asn.startswith('AS'), asn
        assert int(asn[2:]) > 0, asn
        answers = dns.resolver.query(asn + '.asn.cymru.com', 'TXT')
        name = 'unknown'
        for rdata in answers:
            name = str(rdata).split('| ')[4].rstrip('"')
            # e.g. "15169 | US | arin | 2000-03-30 | GOOGLE - Google Inc., US"
        return name

    with gremlin_query.Runner() as gremlin:

        # Number of times we've seen a given filename or address.
        counts = collections.defaultdict(int)

        for prop in gremlin_properties.fetch(gremlin, args.query):

            assert prop.source() in [  # So far, SRI SPADE & 5D are winning.
                cdm.enums.InstrumentationSource.LINUX_AUDIT_TRACE,
                cdm.enums.InstrumentationSource.LINUX_BEEP_TRACE,
                cdm.enums.InstrumentationSource.WINDOWS_FIVEDIRECTIONS,
                None  # Hmmm, who's injecting untagged 'Resource' base events?
            ], prop.source()
            assert cdm.enums.InstrumentationSource \
                .WINDOWS_FIVEDIRECTIONS.value == 12

            if debug:
                print(prop.prop)

            try:
                counts['userID_' + prop['userID']] += 1
                counts['gid_' + prop['gid']] += 1
                data_ghetto = json.loads(switch_brackets(
                    prop['properties'].strip("'")))
                counts['euid_%d' % data_ghetto['euid']] += 1
                counts['egid_%d' % data_ghetto['egid']] += 1
            except KeyError:
                pass

            try:
                counts[validate_file(prop['url'])] += 1
            except KeyError:
                pass

            try:
                counts[validate_ip(prop['dstAddress'])] += 1
                asn = get_asn(prop['dstAddress'])
                asn += '  ' + get_asn_name(asn)
                counts[asn] += 1
            except KeyError:
                pass

            try:
                ss = cdm.enums.SrcSink(prop['srcSinkType'])
                counts[ss] += 1
            except KeyError:
                pass
        i = 1
        for file, count in sorted(counts.items()):
            if count >= threshold:
                print('%4d %4d  %s' % (i, count, file))
            i += 1


def switch_brackets(s):
    '''Turns [x] into {x}, and also returns JSON-compatible quotes.'''
    return s.replace('[', '{').replace(']', '}').replace("'", '"')


def get_canned_reports():
    ret = {}
    for name, label in [
            ('agent', 'Agent'),
            ('file', 'Entity-File'),
            ('netflow', 'Entity-Netflow'),
            ('resource', 'Resource'),
    ]:
        ret[name] = "g.V().has(label, '%s').limit(5000)" % label
    return ret


def arg_parser():
    p = argparse.ArgumentParser(
        description='Ad hoc query runner to report on Entity-File values.')
    p.add_argument('--query', help='gremlin query to run')
    p.add_argument('--report', help='name of canned report to run',
                   choices=get_canned_reports().keys())
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    if args.report:
        args.query = get_canned_reports()[args.report]
    if args.query is None:
        arg_parser().error('Please specify a query or choose a canned report.')
    report(args.query)
