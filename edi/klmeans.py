import argparse
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import edi.simple.model as model
import edi.simple.klmeans as klmeans
import edi.simple.klmeansfast as klmeansfast
import edi.simple.klstream as klstream

parser = argparse.ArgumentParser(description='KL Means')
parser.add_argument('--input', '-i',
					help='input file',
					required=True)
parser.add_argument('--output', '-o',
					help='output file',
					required=True)
parser.add_argument('--cluster', '-c',
					help='cluster model file',
					default=None)
parser.add_argument('--mode', '-m',
					help='batch or stream',
					default='batch',
					choices=['batch','fastbatch','stream'])
parser.add_argument('--classes', '-k', help='number of classes',default=1)
parser.add_argument('--epsilon', '-e', help='improvement threshold',default=0.01)


if __name__ == '__main__':
	args = parser.parse_args()
	if args.mode == 'batch':
		klmeans.run(args.input, args.output, int(args.classes),
					epsilon=float(args.epsilon),
					modelfile=args.cluster)
	elif args.mode == 'fastbatch':
		klmeansfast.run(args.input, args.output, int(args.classes),
					epsilon=float(args.epsilon),
					modelfile=args.cluster)
	else:
		klstream.run(args.input, args.output, modelfile=args.cluster)
