import sys
import os
import titandb
from provn_segmenter import *

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='A provn segmenter')
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument(
        '--provn_file', '-p', help='A prov-tc file in provn format')
    group.add_argument('--broker', '-b', help='The broker to the Titan DB')
    parser.add_argument('spec_file',
                        help='A segment specification file in json format')
    parser.add_argument('--verbose', '-v', action='store_true',
        help='Run in verbose mode')
    parser.add_argument('--summary', '-s', action='store_true',
        help='Print a summary of the input file an quit, segment spec is ignored')

    args = parser.parse_args()
    VERBOSE = args.verbose
    # Check that provided non-optional files actually exist
    for f in [args.provn_file, args.spec_file]:
        if f and not (os.path.isfile(f)):
            print('File {0} does not exist...aborting'.format(f))

    doc = Document()
    if args.provn_file:
        doc.parse_provn(args.provn_file)
        dg = DocumentGraph(doc)
    else:
        dg = DocumentGraph(doc)
        tc = titandb.TitanClient(args.broker)
        tc.load_from_document_graph(dg)
        tc.close()

    if args.summary:
        dg.print_summary()
        #  g.draw()
        sys.exit()

    s = Segmenter(dg, args.spec_file)

    segmentation_doc = s.eval_spec()
    print('=' * 30)
    print('\tSegmentation result')
    print('=' * 30)
    print(segmentation_doc)
