import argparse
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import edi.simple.klmeans as klmeans

parser = argparse.ArgumentParser(description='KL Means')
parser.add_argument('--input', '-i',
					help='input file')
parser.add_argument('--output', '-o',
					help='output file')
parser.add_argument('--classes', '-k', help='number of classes')
parser.add_argument('--iterations', '-n', help='number of iterations')


if __name__ == '__main__':
	args = parser.parse_args()
	klmeans.run(args.input, args.output, int(args.classes), int(args.iterations))
