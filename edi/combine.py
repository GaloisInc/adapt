import argparse
import csv
import os
import numpy
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import edi.util.check as check

# Given two compression-based score files, combines them into one
# Add the scores of uuid in each score file (if present)
# Also add cost of encoding presence/absence of uuid in each file, based
# on AVC model.
# For example, if 50% present in first file and 100% in second, then
# need one bit to represent presence/absence of each uuid in first.

#Alternatively, compute ranks of each score file and use geometric mean
# to combine the ranks.  

parser = argparse.ArgumentParser(description='Combine two score files')
parser.add_argument('inputs', metavar='<Context>', type=str, nargs='+',
					help='inputs to combine')
parser.add_argument('--times', '-t',
					help='time file')
parser.add_argument('--reverse', '-r',
					help='sort scores in decreasing order',
					action='store_true')
parser.add_argument('--output', '-o',
					help='output file',
					required = True)
parser.add_argument('--aggr','-a',
					choices=['uniform','weighted',
							 'average','geometric','min','median'],
					help='form of aggregation',
					default='uniform')

def combineWeighted(dicts,probs,allkeys):
	output = {}
	for k in allkeys:
		output[k] = 0.0
		for i in range(n):
			if k in dicts[i].keys():
				penalty = -probs[i]*numpy.log2(probs[i])
				output[k] = output[k] + dicts[i][k] + penalty
			else:
				penalty = -(1-probs[i])*numpy.log2(1-probs[i]) 
				output[k] =  output[k] + penalty

	return output

def geometricMean(l):
	return numpy.exp(numpy.average([numpy.log(x) for x in l]))

def combine(ranks,allkeys,f):
	output = {}
	def getRank(r,k):
		if k in r.keys():
			return r[k]
		else:
			return len(r)
	for k in allkeys:
		output[k] = f([getRank(r,k) for r in ranks])
	return output

def readScores(file):
	with open(file) as scorefile:
		scores = check.Scores(csv.reader(scorefile),reverse=args.reverse)
	d = {k : v for (k,v) in scores.data}
	r = {k : i+1 for i,(k,v) in enumerate(scores.data)}
	return(d,r)


if __name__ == '__main__':
	args = parser.parse_args()
	outputfile = args.output
	inputs = args.inputs


	# read in the n score files
	n = len(inputs)
	dicts = [dict() for i in range(n)]
	ranks = [dict() for i in range(n)]
	keys = [set() for i in range(n)]
	allkeys = set()
	for i in range(n):
		(dicts[i],ranks[i]) = readScores(inputs[i])
		allkeys = allkeys | dicts[i].keys()
		print('File %d (%s) has %d keys' % (i,inputs[i], len(dicts[i].keys())))

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
	# at this point, allkeys is either an unordered set, or a list
	# in time order.

	m = len(allkeys)
	if args.aggr == 'uniform':
		print("Uniform encoding")
		probs = [0.5 for i in range(n)]
		output = combineWeighted(dicts,probs,allkeys)
	elif args.aggr == 'weighted':
		print("Weighted encoding")
		probs = [len(dicts[i])/m for i in range(n)]
		output = combineWeighted(dicts,probs,allkeys)
	elif args.aggr == 'geometric':
		output = combine(ranks,allkeys,geometricMean)
	elif args.aggr == 'average':
		output = combine(ranks,allkeys,numpy.average)
	elif args.aggr == 'min':
		output = combine(ranks,allkeys,min)
	elif args.aggr == 'median':
		output = combine(ranks,allkeys,numpy.median)

	with open(outputfile,'w') as outfile:
		print('UUID,Score',file=outfile)
		for k in allkeys:
			print('%s,%s' % (k,output[k]),file=outfile)
