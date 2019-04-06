import argparse
import csv
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import edi.util.context as context
import extract.extract as extract

parser = argparse.ArgumentParser(description='Join two contexts')
parser.add_argument('inputs', metavar='<Context>', type=str, nargs='+',
					help='inputs to combine')
parser.add_argument('--times', '-t',
					help='time file')
parser.add_argument('--output', '-o',
					help='output file',
					required = True)



def zeroVec(header):
	return {h:0 for h in header}

def combine(vecs):
	dict = {}
	for i in range(len(vecs)):
		for k in vecs[i].keys():
			dict['X' + str(i) + '_' + k] = vecs[i][k]
	return dict

def vec2str(header,dict):
	return ','.join([str(dict[k]) for k in header])


if __name__ == '__main__':
	args = parser.parse_args()
	outputfile = args.output

	n = len(args.inputs)
	contexts = [None for i in range(n)]
	allkeys = set()
	for i in range(n):
		with open(args.inputs[i]) as ctxfile:
			contexts[i] = context.Context(csv.reader(ctxfile))
		print('File %s has %d keys' % (args.inputs[i], len(contexts[i].data.keys())))
		allkeys = allkeys | contexts[i].data.keys()

	print('%d keys appear in at least one context' % len(allkeys))

		# read in the times, if present, and use them for the master key list
	if args.times != None:
		with open(args.times) as time_file:
			time_reader = csv.reader(time_file)
			header = next(time_reader)
			times = [(row[0], int(row[1]))
						for row in time_reader]
			times = sorted(times,
							key=lambda x: x[1])
			allkeys = [x[0] for x in times]

	output = {}
	for k in allkeys:
		vecs = [{} for i in range(n)]
		for i in range(n):
			if k in contexts[i].data.keys():
				vecs[i] = contexts[i].data[k]
			else:
				vecs[i] = zeroVec(contexts[i].header)
		output[k] = combine(vecs)

	header = ['X'+str(i)+'_'+att
			  for i in range(n)
			  for att in contexts[i].header]
	escapedheader = [extract.escape(h) for h in header]
	with open(outputfile,'w') as outfile:
		print('UUID,%s' % ','.join(escapedheader),file=outfile)
		for k in allkeys:
			print('%s,%s' % (k,vec2str(header,output[k])),file=outfile)
