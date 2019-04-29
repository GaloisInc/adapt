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
parser.add_argument('--output', '-o',
					help='output file', required=True)
parser.add_argument('--classes', '-k', help='number of classes',default=1,type=int)
parser.add_argument('--epsilon', '-e', help='improvement threshold',default=0.01,type=float)



# a naive estimate of size needed to represent codetable: boolean matrix
# plus frequency count
def codeSize1(cte,numattrs,numtrans):
	size = 0
	size = size + np.log2(numtrans)
	if len(cte['attributes']) > 1: 
		 size = size + numattrs
	return size
# a more sophisticated encoding for sparse code tables
def codeSize2(cte,numattrs,numtrans):
	size = 0
	size = size + np.log2(numtrans)
	for att in cte['attributes']:
	        if len(cte['attributes']) > 1: 
	                 size = size + np.log2(numattrs)
	return size
def codeTableSize(ct,numattrs,numtrans):
	size1 = 0
	size2 = 0
	for cte in ct:
	        size1 = size1 + codeSize1(cte,numattrs,numtrans)
	        size2 = size2 + codeSize2(cte,numattrs,numtrans)
	return 1+min(size1,size2)

def randomCategories(data,k):
	d = {c : [] for c in range(0,k)}
	for uuid in data:
		d[random.randint(0,k-1)].append(uuid)
	return d

def getCodeTable(ctfile):
        with open(ctfile) as f:
                reader = csv.reader(f)
                header = next(reader)[1:]
                return [{'attributes': {x[1] for x in zip(row[1:],header)
                                if x[0] == '1'},
                         'cost': float(row[0])} for row in reader]

def getModels(filestem,k):
        models = []
        for i in range(k):
                ctfile = '%s.%s.code.csv' % (filestem,str(i))
                models = models + [getCodeTable(ctfile)]
        return models

def scoreTransactionWithCodeTable(atts,ct):
        cost = 0.0
        atts = atts.copy()
        for cte in ct:
                if atts == set():
                        break
                if cte['attributes'] <= atts:
                        cost += cte['cost']
                        atts -= cte['attributes']
        return cost

def reassignCategories(data,models,k):
        d = {c : [] for c in range(0,k)}
        for uuid in data.keys():
                scores = [scoreTransactionWithCodeTable(data[uuid],model)
                          for model in models]
                d[np.argmin(scores)].append(uuid)
        return d

def getClassScores(d):
	freqs = {i: len(d[i]) for i in d.keys()}
	s = sum(freqs.values())
	class_scores = {i : -np.log2(freqs[i]/s) for i in freqs.keys()}
	return class_scores

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

def scoreSplitsKrimp(d,filestem):
	for i in d.keys():
		krimp.run('%s.%s.csv' % (filestem,str(i)),
			  '%s.%s.scored.csv' % (filestem,str(i)),
			  '%s.%s.code.csv' % (filestem,str(i)))

def mergeSplits(d,filestem):
	sc = dict()
	for i in d.keys():
		with open('%s.%s.scored.csv' % (filestem,i)) as infile:
			reader = csv.reader(infile) 
			sc[i] = check.Scores(reader)
	class_scores = getClassScores(d)
	return (sc,class_scores)

def totalCost(sc,class_scores):
	total = 0.0
	for i in sc.keys():
		for (uuid,score) in sc[i].data:
			total = total + score + class_scores[i]
	return total

def writeMerged(sc, class_scores,outfile):
	with open(outfile,'w') as outfile:
		outfile.write("uuid,score\n")
		for i in sc.keys():
			for (uuid, score) in sc[i].data:
				outfile.write("%s,%f,%s\n" % (uuid,score + class_scores[i],i))

if __name__ == '__main__':
        args = parser.parse_args()

        prefix = '/tmp/krimpstar%s' % random.randint(1,10000000)
        filestem = prefix + '/ctxt'
        os.mkdir(prefix)

        ctxt = getContext(args.input)
        n = len(ctxt.data)
        m = len(ctxt.header)
        data = {uuid: {att for att in ctxt.data[uuid].keys()
                                            if ctxt.data[uuid][att] == 1}
                for uuid in ctxt.data.keys()}

        mincost = None

        for k in range(1,args.classes+1):
                d = randomCategories(ctxt.data.keys(),k)
                print('k = %d' % k)
                lastcost = None
                for i in range(10):
                        writeSplits(d,ctxt,filestem)
                        scoreSplitsKrimp(d,filestem)
                        models = getModels(filestem,k)
                        d = reassignCategories(data,models,k)
                        (sc,class_scores) = mergeSplits(d,filestem)
                        total = totalCost(sc,class_scores)
                        print('%f' % total)
                        if lastcost != None and total > lastcost * (1-args.epsilon):
                                break
                        lastcost = total
                        
                modelCost = sum([codeTableSize(model,m,n) for model in models])
                print('Model cost: %f' % modelCost)
                total_cost = modelCost + total
                print('Total cost: %f' % total_cost)
                if mincost == None or total_cost < mincost:
                        min_k = k
                        mincost = total_cost
                        min_sc = sc
                        min_class_scores = class_scores

        print('Min k = %d with cost %f' % (min_k, mincost))

        writeMerged(min_sc,min_class_scores,args.output)

        shutil.rmtree(prefix)
