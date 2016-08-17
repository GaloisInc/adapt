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
#     ./3_show_marker_events.py
#
'''
Reports on region {2,4} events found in a trace.
'''
import argparse
import os
import pprint
import sys
sys.path.append(os.path.expanduser('~/adapt/tools'))
sys.path.append(os.path.expanduser('~/adapt/classifier/phase3'))
import classify
import gremlin_event
import gremlin_query


class MarkerReporter:

    def __init__(self, gremlin):
        self.gremlin = gremlin
        self.det = classify.MarkerDetector(gremlin)

    def report(self):
        query = "g.V().hasLabel('Segment').order().by(id(), incr).id()"
        seg_ids = self.gremlin.fetch_data(query)
        print('')
        stream = gremlin_event.Stream(self.gremlin, seg_ids)
        for seg_id, seg_props in stream.events_by_seg():
            print('')
            activities = self.det.find_activities(seg_id, seg_props)
            idents = ', '.join(sorted(["'%s'" % i for i, m in activities][:250]))
            if activities != []:
                print(seg_id)
                q = "g.V().has('ident', within(%s)).values('url')" % idents
                for subj in self.gremlin.fetch_data(q):
                    print(subj)
            # pprint.pprint(seg_props)
            # print(seg_props)


def arg_parser():
    p = argparse.ArgumentParser(
        description='Report on region {2,4} events found in a trace.')
    p.add_argument('--debug', help='verbose output', action='store_true')
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    with gremlin_query.Runner() as gremlin:
        MarkerReporter(gremlin).report()
