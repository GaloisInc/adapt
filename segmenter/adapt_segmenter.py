#! /usr/bin/env python3

from provn_segmenter import Document, DocumentGraph, Segmenter
import argparse
import logging
import os
import sys
import titandb
import kafka


def arg_parser():
    p = argparse.ArgumentParser(description='A provn segmenter')
    p.add_argument('--broker', '-b',
                   help='The broker to the Titan DB', required=True)
    p.add_argument('--provn-file', '-p',
                   help='A prov-tc file in provn format')
    p.add_argument('spec_file',
                   help='A segment specification file in json format')
    p.add_argument('--verbose', '-v', action='store_true',
                   help='Run in verbose mode')
    p.add_argument('--summary', '-s', action='store_true',
                   help='Print a summary of the input file and quit,'
                   ' segment spec is ignored')
    p.add_argument('--drop-db', action='store_true',
                   help='Drop DB and quit, segment spec is ignored')
    p.add_argument('--store-segment', action='store_true',
                   help='Store segments in Titan DB')
    p.add_argument('--noseg2seg', action='store_true',
        help='Do not add edges between segment nodes')
    p.add_argument('--log-to-kafka', action='log_to_kafka',
                   help='Send logging information to kafka server')
    p.add_argument('--kafka', action='kafka',
                   help='location of the kafka server',
                   default='localhost:9092')
    return p


class AdaptSegmenter:
    def __init__(self, args):
        self.args = args

        self.drop_db = args.drop_db
        self.broker = args.broker
        self.provn_file = args.provn_file
        self.spec_file = args.spec_file
        self.summary = args.summary
        self.noseg2seg = args.noseg2seg
        self.store_segment = args.store_segment

        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(__name__)
        self.logToKafka = args.logToKafka
        
        if self.logToKafka:
            self.producer = kafka.KafkaProducer(bootstrap_servers=[args.kafka])

    def log_error(self, text):
        self.logger.error(text)
        if self.logToKafka:
            self.producer.send("se-log", text)

    def log_info(self, text):
        self.logger.info(text)
        if self.logToKafka:
            self.producer.send("se-log", text)

    def run(self):
        if self.drop_db:
            tc = titandb.TitanClient(self.broker)
            tc.drop_db()
            tc.close()
            sys.exit()

        # Check that provided non-optional files actually exist
        for f in [self.provn_file, self.spec_file]:
            if f and not (os.path.isfile(f)):
                self.log_error('File {0} does not exist...aborting'.format(f))

        doc = Document()
        tc = titandb.TitanClient(self.broker)
        if self.provn_file:
            doc.parse_provn(self.provn_file)
            dg = DocumentGraph(doc)
            tc.load_from_document_graph(dg)
        else:
            dg = tc.read_into_document_graph()

        if self.summary:
            dg.print_summary()
            #  g.draw()
            tc.close()
            sys.exit()

        s = Segmenter(dg, self.spec_file)

        # segmentation_dg is the DocumentGraph containing segment nodes
        segmentation_dg = s.eval_spec(add_segment2segment_edges=not self.noseg2seg)
        # segmentation_dg.print_summary()

        self.log_info('=' * 30)
        self.log_info('\tSegmentation result')
        self.log_info('=' * 30)
        self.log_info(segmentation_dg)

        if self.store_segment:
            # Add the segment nodes and edges to our document graph representation
            dg.union(segmentation_dg)
            # This will add the segment nodes to the db, is equivalent to
            # tc.load_from_document_graph(dg), as long as we call it after
            # calling dg.union(segmentation_dg)
            tc.load_segments_from_document_graph(dg)
            # segmented_dg = tc.read_into_document_graph()

        tc.close()

if __name__ == '__main__':
    args = arg_parser().parse_args()

    AdaptSegmenter(args).run()

