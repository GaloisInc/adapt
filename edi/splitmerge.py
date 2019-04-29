import argparse
import csv
import numpy as np
import os
import random
import shutil
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import edi.simple.ad as ad
import edi.krimp as krimp
import edi.util.context as context
import edi.util.check as check


parser = argparse.ArgumentParser(description='Krimp/OC3')
parser.add_argument('--input', '-i',
					help='input score file', required=True)
parser.add_argument('--context', '-c',
					help='context file', required=True)
parser.add_argument('--output', '-o',
					help='output file', required=True)
parser.add_argument('--score', '-s',
					help='avc or krimp',
					default='avc',
					choices=['avc','krimp'])

def getCategories(inputfile):
	with open(inputfile) as infile:
		reader = csv.reader(infile)
		header = next(reader)[1:]
		data = [(row[0], int(row[2])) for row in reader]
	categories = {category for (uuid,category) in data}
	d = {c : [] for c in categories}
	for (uuid,cat) in data:
		d[cat].append(uuid)
	return d

def getClassScores(d):
	freqs = {i: len(d[i]) for i in d.keys()}
	s = sum(freqs.values())
	return {i : -np.log2(freqs[i]/s) for i in freqs.keys()}

def getContext(ctxtfile):
	with open(ctxtfile) as infile:
		reader = csv.reader(infile)
		ctxt = context.Context(reader)
	return ctxt

def writeRow(outfile,ctxt,uuid):
	outfile.write('%s,%s\n' % (uuid, ','.join([str(ctxt.data[uuid][att]) for att in ctxt.header])))

def writeSplits(d,ctxt,filestem):
	for i in d.keys():
		with open('%s.%s.csv' % (filestem,i), 'w') as outfile:
			outfile.write("Object_ID,%s\n" % ','.join(ctxt.header))
			for uuid in d[i]:
				writeRow(outfile,ctxt,uuid)

def scoreSplitsAVC(d,filestem):
	for i in d.keys():
		ad.batch('%s.%s.csv' % (filestem,i),'%s.%s.scored.csv' % (filestem,i),'avc')

def scoreSplitsKrimp(d,filestem):
	for i in d.keys():
		krimp.run('%s.%s.csv' % (filestem,i),'%s.%s.scored.csv' % (filestem,i))

def mergeSplits(d,filestem,outfile):
	sc = dict()
	for i in d.keys():
		with open('%s.%s.scored.csv' % (filestem,i)) as infile:
			reader = csv.reader(infile) 
			sc[i] = check.Scores(reader)
	class_scores = getClassScores(d)
	with open(outfile,'w') as outfile:
		outfile.write("uuid,score\n")
		for i in sc.keys():
			for (uuid, score) in sc[i].data:
				outfile.write("%s,%f\n" % (uuid,score + class_scores[i]))

if __name__ == '__main__':
	args = parser.parse_args()

	prefix = '/tmp/splitmerge%s' % random.randint(1,10000000)
	os.mkdir(prefix)
	filestem = prefix + '/ctxt'

	d = getCategories(args.input)

	ctxt = getContext(args.context)

	writeSplits(d,ctxt,filestem)

	if args.score == 'avc':
		scoreSplitsAVC(d,filestem)
	elif args.score == 'krimp':
		scoreSplitsKrimp(d,filestem)

	mergeSplits(d,filestem,args.output)

	shutil.rmtree(prefix)
