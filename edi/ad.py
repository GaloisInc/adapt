import argparse
import csv
import gzip
import sys
import os

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import edi.simple.ad as ad

parser = argparse.ArgumentParser(description='Attribute value frequency')
parser.add_argument('--input', '-i',
					help='input file', required=True)
parser.add_argument('--output', '-o',
					help='output file', required=True)
parser.add_argument('--score', '-s',
					help='avf or avc or rasp',
					default='avf',
					choices=['avf','avc','rasp'])
parser.add_argument('--mode', '-m',
					help='batch or stream',
					default='batch',
					choices=['batch','stream'])


if __name__ == '__main__':
	args = parser.parse_args()

	if(args.mode == 'batch'):
		ad.batch(args.input, args.output, args.score)
	elif (args.mode == 'stream'):
		ad.stream(args.input, args.output, args.score)
