#! /usr/bin/env python3
'''
Segments a large DB graph using the simplest possible criterion: every K nodes.
'''
import argparse
import kafka
import logging
import os
import sys
sys.path.append(os.path.expanduser('~/adapt/tools'))
import gremlin_properties
import gremlin_query

log = logging.getLogger(__name__)
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
handler = logging.StreamHandler()
handler.setFormatter(formatter)
log.addHandler(handler)
log.setLevel(logging.INFO)


STATUS_IN_PROGRESS = b'\x00'
STATUS_DONE = b'\x01'


class SSegmenter:
    '''
    Simple Segmenter splits input graph into subgraphs of K nodes.

    This segmenter always produces a deterministic output for a given input.
    If id1 < id2 implies a HappensBefore relationship for the input
    nodes, then that shall also be true for the output segments.
    More precisely, if seg1 has smaller ID than seg2, then max ID of seg1's
    base nodes shall be smaller than min ID of seg2's base nodes.
    '''

    def __init__(self, k, wipe_segs=False):
        self.total_edges_inserted = 0
        self.k = k  # max nodes per segment
        self.consumer = kafka.KafkaConsumer('se')
        self.producer = kafka.KafkaProducer()
        self.gremlin = gremlin_query.Runner()
        self.next_seg_id = 1 + self.max_seg_id()
        self.next_node_id = 1 + self.max_node_id()
        if wipe_segs:
            self.drop_all_existing_segments()

    def __enter__(self):
        return self

    def __exit__(self, type, value, traceback):
        self.close()

    def close(self):
        self.gremlin.close()

    def execute(self, cmd):
        '''Evaluate a gremlin command for side effects.'''
        return self.fetch_single_item(cmd)

    def fetch_single_item(self, query, default=0):
        ret = default
        for msg in self.gremlin.fetch(query):  # We anticipate a single msg.
            if msg.data is not None:
                for item in msg.data:  # We anticipate just a single item.
                    ret = item
        return ret

    def max_seg_id(self):
        q = "g.V().has(label, 'Segment').id().max()"
        return self.fetch_single_item(q)

    def max_node_id(self):
        q = "g.V().has(label, within(%s)).id().max()" % self.base_node_types()
        return self.fetch_single_item(q)

    def base_node_types(self):
        '''Types inserted by ingestd, and *not* by downstream components.'''
        # This is comes from gremlin query results, not the CDM13 spec.
        # Relying upon the spec would be better.
        # For example, DB query results have not yet returned
        #   EDGE_EVENT_AFFECTS_MEMORY,
        #   EDGE_EVENT_AFFECTS_SRCSINK,
        #   EDGE_EVENT_AFFECTS_SRCSINK,
        #   EDGE_EVENT_AFFECTS_SUBJECT,
        #   EDGE_SUBJECT_AFFECTS_EVENT, etc.
        types = [
            'Agent',
            'EDGE_EVENT_AFFECTS_FILE',
            'EDGE_EVENT_AFFECTS_NETFLOW',
            'EDGE_EVENT_ISGENERATEDBY_SUBJECT',
            'EDGE_FILE_AFFECTS_EVENT',
            'EDGE_OBJECT_PREV_VERSION',
            'EDGE_SUBJECT_HASLOCALPRINCIPAL',
            'Entity-File',
            'Entity-NetFlow',
            'Subject',
            ]
        return ', '.join(["'%s'" % typ
                          for typ in types])

    def drop_all_existing_segments(self):
        '''Allows for idempotency during testing. Not for use in production.'''
        log.warn('Dropping any existing segments.')
        for x in 'EV':
            q = "g.%s().has(label, 'Segment').drop().iterate()" % x
            self.execute(q)

    def await_base_nodes(self):
        log.info('Awaiting new base nodes from ingestd.')
        for msg in self.consumer:
            log.info("recvd msg: %s", msg)
            if msg.value == STATUS_DONE:
                return

    def gen_segments(self):
        # Often this generates segments, but at random it dies with
        # aiogremlin.exceptions.GremlinServerError:
        # Code [500]: SERVER_ERROR. A general server error occurred
        #
        # It would be nice if we could insert faster than 40 edge/sec.
        cur_seg = None  # We're not yet inserting into a valid segment node.
        q = "g.V().has(label, within(%s)).id().is(gte(%d)).sort()" % (
            self.base_node_types(),
            self.next_node_id)
        for msg in self.gremlin.fetch(q):
            if msg.data is not None:
                for item in msg.data:
                    if cur_seg is None or cur_seg.remaining <= 0:
                        cur_seg = SegNode(self)
                        self.next_seg_id += 1
                    self.add_edge(cur_seg, 0 + item)
                    cur_seg.remaining -= 1
        self.report_done()

    def report_done():
        for topic in ['ac']:  # Add others as needed, e.g. 'ad'
            self.producer.send(topic, STATUS_DONE).get()

    def add_edge(self, seg, base_node_id):
        q = "g.V(%d).next().addEdge('segment:includes', g.V(%d).next())" % (
            seg.seg_db_id, base_node_id)
        if base_node_id % 10000 == 0:
            log.info('adding %d' % base_node_id)
        self.execute(q)
        self.total_edges_inserted += 1


class SegNode:
    '''Models a segment node stored by gremlin in the DB.'''

    def __init__(self, sseg):
        self.remaining = sseg.k
        self.seg_id = sseg.next_seg_id
        # Might consider adding a property key 'k' to schema (or 'max_nodes').
        cmd = "g.addV(label, 'Segment',  'ident', 'segment_id_%09d')" % (
            self.seg_id)
        result = sseg.execute(cmd)
        assert result['type'] == 'vertex', result
        assert result['label'] == 'Segment', result
        self.seg_db_id = result['id']


def arg_parser():
    p = argparse.ArgumentParser(
        description='Segments a graph into subgraphs of at most K nodes.')
    p.add_argument('--k', help='max nodes per segment', type=int, default=100)
    p.add_argument('--drop-all-existing-segments', action='store_true',
                   help='destructive, useful during testing')
    return p


# To find members of a given segment:
#   g.V().has('ident', SEG).outE('segment:includes').inV().valueMap()
#   where SEG might be e.g. 'segment_id_007492263'.


if __name__ == '__main__':
    '''Observed throughput is 30 - 40 edges inserted per second.'''
    args = arg_parser().parse_args()
    with SSegmenter(args.k, wipe_segs=True) as sseg:
        sseg.next_node_id = 1  # During testing we will segment everything.
        # sseg.await_base_nodes()
        sseg.gen_segments()
        log.info('Inserted %d edges.' % sseg.total_edges_inserted)
