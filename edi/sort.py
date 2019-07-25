import argparse
import csv
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import edi.util.check as check


parser = argparse.ArgumentParser(description='Sort by scores')
parser.add_argument('--input',
					'-i',
					help='input score file',
					required=True)
parser.add_argument('--output', '-o',
					help='output score file',
					required=True)
parser.add_argument('--reverse','-r',
					help='sort anomaly scores in increasing order',
					action='store_true')

if __name__ == '__main__':
	args = parser.parse_args()
	inputfile = args.input
	outfile = args.output

	with open(inputfile) as infile:
		scores = check.Scores(csv.reader(infile),args.reverse)

	with open(outfile, 'w') as outfile:
		outfile.write("uuid,score\n")
		for (uuid, score) in scores.data:
			outfile.write("%s,%f\n" % (uuid, score))

