#! /usr/bin/env python3

from provn_segmenter import Document, DocumentGraph, Segmenter
import argparse
import logging
import os
import sys
import titandb


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
    return p


if __name__ == '__main__':
    parser = arg_parser()

    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)

    args = parser.parse_args()
    VERBOSE = args.verbose

    if args.drop_db:
        tc = titandb.TitanClient(args.broker)
        tc.drop_db()
        tc.close()
        sys.exit()

    # Check that provided non-optional files actually exist
    for f in [args.provn_file, args.spec_file]:
        if f and not (os.path.isfile(f)):
            logger.error('File {0} does not exist...aborting'.format(f))

    doc = Document()
    tc = titandb.TitanClient(args.broker)
    if args.provn_file:
        doc.parse_provn(args.provn_file)
        dg = DocumentGraph(doc)
        tc.load_from_document_graph(dg)
    else:
        dg = tc.read_into_document_graph()
    # tc.close()

    if args.summary:
        dg.print_summary()
        #  g.draw()
        tc.close()
        sys.exit()

    s = Segmenter(dg, args.spec_file)

    # segmentation_dg is the DocumentGraph containing segment nodes
    segmentation_dg = s.eval_spec()
    # egmentation_dg.print_summary()

    logger.info('=' * 30)
    logger.info('\tSegmentation result')
    logger.info('=' * 30)
    logger.info(segmentation_dg)

    if args.store_segment:
        # Add the segment nodes and edges to our document graph representation
        dg.union(segmentation_dg)
        # This will add the segment nodes to the db, is equivalent to
        # tc.load_from_document_graph(dg), as long as we call it after
        # calling dg.union(segmentation_dg)
        tc.load_from_document_graph(dg)
        segmented_dg = tc.read_into_document_graph()

    tc.close()
