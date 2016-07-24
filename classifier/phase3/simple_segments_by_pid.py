#! /usr/bin/env python3
'''
Segments a large DB graph using the simplest PID criterion: distinct PIDs.
'''
import argparse
import datetime
import kafka
import logging
import os
import sys
sys.path.append(os.path.expanduser('~/adapt/tools'))
import cdm.enums
import gremlin_query

# python3 -m json.tool ~/adapt/tests/tests.json | grep query

log = logging.getLogger(__name__)
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
handler = logging.StreamHandler()
handler.setFormatter(formatter)
log.addHandler(handler)
log.setLevel(logging.INFO)


STATUS_IN_PROGRESS = b'\x00'
STATUS_DONE = b'\x01'


class SPSegmenter:
    '''
    Simple PID Segmenter splits an input graph into PID-based subgraphs.

    This segmenter always produces a deterministic output for a given input.
    It does not attempt to insert edges connecting one segment to another.
    '''

    def __init__(self, timestamp_usec, wipe_segs=False):
        self.total_edges_inserted = 0
        self.consumer = kafka.KafkaConsumer('se')
        self.producer = kafka.KafkaProducer()
        self.gremlin = gremlin_query.Runner()
        # Timestamp_usec describes previously segmented base nodes,
        # and is strictly less than a startedAtTime that exists in the DB,
        # hence we never process the "most recent" event,
        # we leave it for next time.
        # If the kafka-delivered TA1 event stream obeys HappensBefore,
        # then querying nodes >= begin_stamp will yield fresh base events.
        # We rely on sparse timestamps. For example, if TA1 timer resolution
        # is 1ms, then low-order usec digits shall be 000 and timestamp_usec
        # shall end with 999. For dense stamps we could query then sleep()
        # a bit or simply accept that there's an event at ts1 in the DB which
        # we segment, and in future kafka might deliver additional ts1 events
        # which we ignore.
        self.begin_stamp = int(timestamp_usec + 1)
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
        types = [
            'Agent',
            'EDGE_EVENT_AFFECTS_FILE',
            'EDGE_EVENT_AFFECTS_MEMORY',
            'EDGE_EVENT_AFFECTS_NETFLOW',
            'EDGE_EVENT_AFFECTS_SRCSINK',
            'EDGE_EVENT_AFFECTS_SUBJECT',
            'EDGE_EVENT_CAUSES_EVENT',
            'EDGE_EVENT_HASPARENT_EVENT',
            'EDGE_EVENT_ISGENERATEDBY_SUBJECT',
            'EDGE_FILE_AFFECTS_EVENT',
            'EDGE_FILE_HAS_TAG',
            'EDGE_MEMORY_AFFECTS_EVENT',
            'EDGE_MEMORY_HAS_TAG',
            'EDGE_NETFLOW_AFFECTS_EVENT',
            'EDGE_NETFLOW_HAS_TAG',
            'EDGE_OBJECT_PREV_VERSION',
            'EDGE_SRCSINK_AFFECTS_EVENT',
            'EDGE_SUBJECT_AFFECTS_EVENT',
            'EDGE_SUBJECT_HASLOCALPRINCIPAL',
            'EDGE_SUBJECT_HASPARENT_SUBJECT',
            'EDGE_SUBJECT_HASPRINCIPAL',
            'EDGE_SUBJECT_HAS_TAG',
            'EDGE_SUBJECT_RUNSON',
            'Entity',
            'Entity-File',
            'Entity-Memory',
            'Entity-NetFlow',
            'Host',
            'Resource',
            'Subject',
            ]
        return ', '.join(["'%s'" % typ
                          for typ in types])

    def drop_all_existing_segments(self):
        '''Allows for idempotency during testing. Not for use in production.'''
        log.warn('Dropping any existing segments.')
        q = "g.V().has(label, 'Segment').drop().iterate()"
        self.execute(q)

    def await_base_nodes(self):
        log.info('Awaiting new base nodes from ingestd.')
        for msg in self.consumer:
            log.info("recvd msg: %s", msg)
            if msg.value == STATUS_DONE:
                return

    # In the cameragrab1 trace we see a pair of closely spaced pid 878 events:
    #   startedAtTime: 2016-06-22 17:28:34 Z 1466616514839838
    #   startedAtTime: 2016-06-22 17:28:36 Z 1466616516280587
    # We can't wrap through 32,000 forks in two seconds.
    # I don't know what those base nodes mean. Another example is pid 886:
    #   startedAtTime: 2016-06-22 17:28:36 Z 1466616516259767
    #   startedAtTime: 2016-06-22 17:28:36 Z 1466616516606469
    # Each of those nodes has no outE() edges.

    # We may still want to use a counter to preserve HappensBefore,
    # since at usec resolution we will still routinely see events happen
    # at the "same" stamp:
    #   2016-06-22 17:28:36.787430  Event.MMAP
    #   2016-06-22 17:28:36.791521  Event.READ
    #   2016-06-22 17:28:36.791521  Event.MMAP
    #   2016-06-22 17:28:36.791521  Event.READ
    #   2016-06-22 17:28:36.791521  Event.MMAP
    #   2016-06-22 17:28:36.796064  Event.MMAP
    #   2016-06-22 17:28:36.796064  Event.MMAP

    # Caveat executor. Tested on 5D youtube trace, plus cameragrab1.
    # For cameragrab1 we focus on 'Subject', as only Subjects offer a PID.

    def get_event_query(self):
        '''Finds subjects.'''
        # Tested with 5D youtube & cameragrab1 (GT/THEIA).
        # Common eventType's: 9 execute, 17 read, cf 'sequence'.
        #
        # .has('pid', between(21870, 21880))
        return """
g.V().has('pid').has('startedAtTime', between(%d, %d))
    .order()
    .as('a')
    .local(
        __.in().in().hasLabel('Subject').has('ident')
        .barrier().order()
        .as('b')
    )
    .select('a').values('startedAtTime').as('TIME')
    .select('a').values('pid').as('PID')
    .select('b').values('ident').as('IDENT')
    .select('TIME', 'PID', 'IDENT')
"""

    def get_principal_query(self):
        '''Finds agents.'''
        # Tested with ta5attack2_units (SRI/SPADE) and 5D youtube.
        #
        # Agents may have properties:[{euid=0, egid=0}]]
        return """
g.V().has('startedAtTime', between(%d, %d))
    .has('pid')
    .order()
    .as('a')
    .local(
        both().hasLabel('EDGE_SUBJECT_HASLOCALPRINCIPAL').both()
        .hasLabel('Agent')
        .has('userID')
        .has('gid')
        .has('ident')
        .barrier()
        .order()
        .as('b')
    )
    .select('a').values('startedAtTime').as('TIME')
    .select('a').values('pid').as('PID')
    .select('b').values('userID').as('USERID')
    .select('b').values('gid').as('GID')
    .select('b').values('ident').as('IDENT')
    .select('TIME', 'PID', 'USERID', 'GID', 'IDENT')
"""

    def gen_pid_segments(self, debug=False):
        self.procs = {}  # A pqueue should trim this down to fixed size.
        for q_getter in [
                self.get_event_query,
                self.get_principal_query,
                ]:
            self.gen_pid_segments1(q_getter(), debug)

    def gen_pid_segments1(self, pid_query, debug, end_stamp=None):
        '''Segments from begin_stamp up to but not including end_stamp.'''
        #
        # This mallocs proportional to number of segment nodes being inserted
        # on this iteration, which will surely be trouble if there are many
        # of them. A fancier approach would keep a fixed sized pqueue of
        # recently seen PIDs, and cope with occasional dup insert exceptions.
        # On the plus side it only stores segment nodes in RAM, not base nodes.
        # In practice `ps` usually shows it consuming < 1% of a 4-GiB VM.
        #
        # It would be nice if we could insert faster than 62 edge/sec.
        #
        far_future = int((2 ** 31 - 1) * 1e6)  # A timestamp at +Inf.
        end_stamp = end_stamp or far_future
        q_subj = pid_query % (self.begin_stamp, end_stamp)
        if debug:
            print(' '.join(q_subj.split()))
        # These happen often. Very often.
        boring = set([
            cdm.enums.Event.MMAP,
            cdm.enums.Event.READ,
            ])
        for p in self.gremlin.fetch_data(q_subj):
            stamp = datetime.datetime.utcfromtimestamp(p['TIME'] / 1e6)
            proc = '%d%05d' % (p['TIME'], p['PID'])
            if 'SUBJ' in p:
                subj = cdm.enums.Subject(p['SUBJ'])
                assert cdm.enums.Subject.SUBJECT_EVENT == subj, (subj, stamp)
            if debug:
                if 'EVENT' in p:
                    event = cdm.enums.Event(p['EVENT'])
                    if event not in boring:
                        log.info('%s  %s  %s' % (proc, stamp, event))
            if proc not in self.procs:
                self.procs[proc] = SegNode(self, p['TIME'], p['PID'], proc)
            self.execute(self.procs[proc].add_edge(p['IDENT']))
            self.total_edges_inserted += 1

        self.report_done()

    def report_done(self):
        for topic in ['ac']:  # Add others as needed, e.g. 'ad'
            self.producer.send(topic, STATUS_DONE).get()


class SegNode:
    '''Models a segment node stored by gremlin in the DB.'''

    def __init__(self, sseg, time, pid, proc):
        self.proc = proc
        cmd = ("g.addV(label, 'Segment',"
               "       'startedAtTime', %d,"
               "       'pid', '%s',"
               "       'segment:name', 's%s')") % (time, pid, proc)
        result = sseg.execute(cmd)
        assert result['type'] == 'vertex', result
        assert result['label'] == 'Segment', result
        # self.seg_db_id = result['id']  # Smaller SegNode => we malloc less.
        # Each proc is a 21-byte string, plus 8-byte SegNode object overhead.

    def add_edge(self, ident):
        q = ("g.V().has('segment:name', 's%s').next()"
             ".addEdge('segment:includes', g.V().has('ident','%s').next())" % (
                     self.proc, ident))
        return q


def arg_parser():
    p = argparse.ArgumentParser(
        description='Segments a graph into PID-based subgraphs.')
    p.add_argument('--drop-all-existing-segments', action='store_true',
                   help='destructive, useful during testing')
    p.add_argument('--debug', action='store_true',
                   help='Increase verbosity.')
    return p


if __name__ == '__main__':
    args = arg_parser().parse_args()
    with SPSegmenter(-1, wipe_segs=args.drop_all_existing_segments) as sseg:
        sseg.next_node_id = 1  # During testing we will segment everything.
        # sseg.await_base_nodes()
        sseg.gen_pid_segments(args.debug)
        log.info('Inserted %d edges.' % sseg.total_edges_inserted)
