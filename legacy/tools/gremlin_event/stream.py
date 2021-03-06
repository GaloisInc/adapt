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


__author__ = 'John.Hanley@parc.com'

import gremlin_query


class Stream:

    def __init__(self, gremlin, seg_ids):
        assert isinstance(gremlin, gremlin_query.Runner)
        self.gremlin = gremlin
        self.seg_ids = sorted(map(int, seg_ids))

    def events_by_seg(self):
        prev_id = None
        seg_props = []
        for seg_id, event in self._events():
            if seg_id != prev_id and len(seg_props) > 0:
                yield prev_id, seg_props
                seg_props = []
            prev_id = seg_id
            seg_props.append(event)
        yield prev_id, seg_props

    def _events(self):
        ids = ', '.join([str(n) for n in self.seg_ids])
        for msg in self.gremlin.fetch(self._get_query() % ids):
            if msg.data:
                for event in msg.data:
                    seg, subj, edge, esa = [
                        event[x] for x in 'SEG SUBJ EDGE ESA'.split()]
                    seg_id = 0 + seg['id']
                    props = []
                    for x in seg, subj, edge, esa:
                        if 'properties' in x:  # Sigh! No JSON for props.
                            assert len(x['properties']) == 1, x
                            props.append(x['properties'][0].strip('{}'))
                    # d = {**seg, **subj, **edge, **esa} (on xenial python3.5)
                    d = self._merge_dicts(seg, subj, edge, esa)
                    if len(props) > 0:
                        d['properties'] = '{%s}' % ', '.join(props)
                    yield seg_id, d

    def _merge_dicts(self, *dicts):
        '''Allow trusty python3.4 to do what xenial python3.5 does natively.'''
        ret = {}
        for dict in dicts:
            ret.update(dict)
        return ret

    def _get_query(self):
        return """
g.V(%s).hasLabel('Segment').order().by(id(), incr).as('SEG').
  out().hasLabel('Subject').dedup().as('SUBJ').
  out().dedup().as('EDGE').
  out().has(label, within('Entity-File',
                          'Entity-NetFlow',
                          'Entity-Memory',
                          'Subject',
                          'Agent')).dedup().as('ESA').
  select('SEG', 'SUBJ', 'EDGE', 'ESA').by(valueMap(true))
"""
