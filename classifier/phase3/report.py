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
Ad hoc query runner to report on e.g. distinct Entity-File node values.
'''
import argparse
import collections
import datetime
import dns.resolver
import ipaddress
import json
import os
import re
import sys
sys.path.append(os.path.expanduser('~/adapt/tools'))
import cdm.enums
import gremlin_properties
import gremlin_query


def report(query, threshold=1, debug=False):

    asn_re = re.compile('^AS\d+$')
    # ip4_re = re.compile('^(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})$')

    # In the apt1 trace, why does THEIA think '0x7fc9d5cf4250' is a filespec?
    filespec_re = re.compile('^(C:|[A-Z]:|/|file://|0x\w{12}$|\w|\.)')

    def validate_file(file):
        assert filespec_re.search(file), file
        return file

    def validate_ip(ip):
        '''Raise ValueError exception if ip is not a valid internet address.'''
        # THEIA's recordaudio1 trace contains empty "address" records,
        # so AF_UNIX IPC events will have attributes like this:
        # "srcAddress": "/tmp/.X11-unix/X0", "srcPort": 0,
        # "destAddress": "", "destPort": 0, "ipProtocol": null
        if ip == '':
            ip = '0.0.0.0'
        ipaddress.ip_address(ip)
        return ip

    def dns_query(fqdn):
        try:
            return dns.resolver.query(fqdn, 'TXT')
        except dns.resolver.NXDOMAIN:
            return []

    def get_asn(ip4):
        '''Maps to a BGP Autonomous System Number - cannot be airgapped.'''
        rev = '.'.join(reversed(ip4.split('.')))
        answers = dns_query(rev + '.origin.asn.cymru.com')
        resp = 0
        for rdata in answers:
            resp = int(str(rdata).split()[0].lstrip('"'))
            # e.g. "15169 | 216.58.192.0/19 | US | arin | 2012-01-27"
        return 'AS%d' % resp

    def get_asn_name(asn):
        '''Maps e.g. AS15169 to GOOGLE.'''
        assert asn_re.search(asn), asn
        answers = dns_query(asn + '.asn.cymru.com')
        name = 'unknown'
        for rdata in answers:
            name = str(rdata).split('| ')[4].rstrip('"')
            # e.g. "15169 | US | arin | 2000-03-30 | GOOGLE - Google Inc., US"
        return name

    def either_address_is(s, prop):
        return (s == prop['srcAddress'] or
                s == prop['dstAddress'])

    with gremlin_query.Runner() as gremlin:

        # Number of times we've seen a given filename or address.
        counts = collections.defaultdict(int)

        for prop in gremlin_properties.fetch(gremlin, query):

            source = prop.source()
            assert source in [
                cdm.enums.InstrumentationSource.ANDROID_JAVA_CLEARSCOPE,
                cdm.enums.InstrumentationSource.FREEBSD_DTRACE_CADETS,
                cdm.enums.InstrumentationSource.LINUX_AUDIT_TRACE,
                cdm.enums.InstrumentationSource.LINUX_BEEP_TRACE,
                cdm.enums.InstrumentationSource.LINUX_THEIA,
                cdm.enums.InstrumentationSource.WINDOWS_FIVEDIRECTIONS,
                None  # Hmmm, who's injecting untagged 'Resource' base events?
            ], prop.source()
            assert cdm.enums.InstrumentationSource \
                .WINDOWS_FIVEDIRECTIONS.value == 12
            if source:
                counts[str(source)] += 1

            if debug:
                print(sorted(prop.prop))

            try:
                counts['userID_' + prop['userID']] += 1
                counts['gid_' + prop['gid']] += 1
                continue
                # http://tinyurl.com/cdm13-spec says yes, we need this nonsense
                properties = json.loads(prop['properties'])
                # The above fails in ta5attack2_units.avro, because
                # we receive '{euid=0, egid=0}'
                # rather than '{"euid"=0, "egid"=0}'.
                counts['euid_%d' % int(properties['euid'])] += 1
                counts['egid_%d' % int(properties['egid'])] += 1
            except KeyError:
                pass

            # Remove strip() once this issue is closed:
            # https://git.tc.bbn.com/ta1-theia/ta1-integration-theia/issues/5
            try:
                if len(prop['url']) > 0:  # cf the CADETS remove_dir trace.
                    counts[validate_file(prop['url'].strip('"'))] += 1  # file
            except KeyError:
                pass

            try:
                counts['%08x' % prop['address']] += 1  # memory
            except KeyError:
                pass

            try:
                if either_address_is('var/run/dbus/system_bus_socket', prop):
                    continue  # Why does THEIA send corrupt AF_UNIX addresses?
                counts[validate_ip(prop['dstAddress'])] += 1  # netflow
                asn = get_asn(validate_ip(prop['dstAddress']))
                asn += '  ' + get_asn_name(asn)
                counts[asn] += 1
            except KeyError:
                pass

            try:
                ss = cdm.enums.SrcSink(prop['srcSinkType'])  # resource
                counts[ss] += 1
            except KeyError:
                pass

            try:
                counts[str(cdm.enums.Event(prop['eventType']))] += 1  # subject

                counts[str(cdm.enums.Subject(prop['subjectType']))] += 1

                usec = int(prop['startedAtTime'])
                stamp = datetime.datetime.utcfromtimestamp(usec / 1e6)
                five_d = cdm.enums.InstrumentationSource.WINDOWS_FIVEDIRECTIONS
                if prop.source() != five_d:  # 5D uses dates like 17-Jan-1970.
                    if usec != 0:  # Sigh! Why do people insert zeros?
                        assert str(stamp) > '2015-01-01', stamp

                continue  # infoleak AUDIT_TRACE contains: '{event id=65615}'
                properties = json.loads(prop['properties'])
                # The seq so nice, gotta say it twice.
                assert int(properties['event id']) == prop['sequence'], stamp
            except KeyError:
                pass
        i = 1
        for file, count in sorted(counts.items()):
            if count >= threshold:
                print('%4d %4d  %s' % (i, count, file))
            i += 1


def get_canned_reports():
    ret = {}
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
    labels = 'Agent Entity-File Entity-Memory Entity-NetFlow Resource Subject'
    for label in labels.split():
        name = re.sub(r'^Entity-', '', label).lower()
        ret[name] = "g.V().hasLabel('%s').limit(5000)" % label
    return ret
    # Finds a pair of accelerometer reports:
    # g.V().has(label, 'EDGE_EVENT_AFFECTS_SRCSINK').outE().inV().valueMap()


def arg_parser():
    p = argparse.ArgumentParser(
        description='Ad hoc query runner to report on Entity-File values &c.')
    p.add_argument('--query', help='gremlin query to run')
    p.add_argument('--report', help='name of canned report to run',
                   choices=sorted(get_canned_reports().keys()))
    p.add_argument('--all', help='run all canned reports', action='store_true')
    p.add_argument('--debug', help='verbose output', action='store_true')
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    if args.all:
        for name, query in sorted(get_canned_reports().items()):
            print('\n' + name)
            report(query, debug=args.debug)
    else:
        if args.report:
            args.query = get_canned_reports()[args.report]
        if args.query is None:
            arg_parser().error('Please give a query or a canned report name.')
        report(args.query, debug=args.debug)
