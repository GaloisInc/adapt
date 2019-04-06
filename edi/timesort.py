import argparse
import csv
import sys
import os

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import edi.util.context as c

# sort by time
# input: context CSV and time CSV
# second column of time csv is start time, used to sort
# output: context CSV with objects sorte by start time
# TODO: allow sorting by end time if present

parser = argparse.ArgumentParser(description='Sort context by start time')
parser.add_argument('--input', '-i',
					help='input context file', required=True)
parser.add_argument('--times', '-t',
					help='time file', required=True)
parser.add_argument('--output', '-o',
					help='output context file', required=True)


if __name__ == '__main__':
	args = parser.parse_args()
	with open(args.times) as time_file:
		time_reader = csv.reader(time_file)
		header = next(time_reader)
		times = [(row[0], int(row[1]))
					for row in time_reader]
		times = sorted(times,
					   key=lambda x: x[1])

	with open(args.input) as context_file:
		context_reader = csv.reader(context_file)
		ctx = c.Context(context_reader)

	with open(args.output,'w') as out_file:
		attributes = sorted({e for val in ctx.data.values() for e in val})
		out_file.write(','.join(['Object_ID']+attributes)+'\n')
		for row in times:
			obj = row[0]
			if obj in ctx.data.keys():
				atts = ','.join([str(ctx.data[obj][att]) for att in attributes])
				out_file.write('%s,%s\n' % (obj,atts))




