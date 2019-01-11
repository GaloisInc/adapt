import argparse
import csv
import sys
import os

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import edi.util.context as c

# simple-minded covnersion
# input: context CSV
# output: .dat, possibly other formats
# todo: save auxiliary files to allow mapping .dat / coordinates back to source

parser = argparse.ArgumentParser(description='Convert CSV to DAT')
parser.add_argument('--input', '-i',
					help='input file', required=True)
parser.add_argument('--output', '-o',
					help='output file', required=True)


if __name__ == '__main__':
	args = parser.parse_args()

	with open(args.input) as csv_file, open(args.output,'w') as out_file:
		reader = csv.reader(csv_file)
		header = next(reader)[1:]
		for row in reader:
			items = [str(i+1) for i,x in enumerate(row[1:]) if int(x) == 1]
			out_file.write("%s\n" % (" ".join(items)))


