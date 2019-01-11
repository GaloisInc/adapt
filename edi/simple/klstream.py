import argparse
import copy
import csv
import numpy
import random

from . import model
from ..util import utils

# Use KL-means approach to compress/score a stream.
# Based on infinite mixture model/Dirichlet "chinese restaurant process"
# Maintain an array of "known" models (e.g. AVC) and frequency counts
# indicating how often each has been used (this is the "n" of each model).
# Also keep one "default" model, built using all of the observations that have been made
# so far.
# When new data arrives, see what its compressed size would be for each known
# model and the default model.
# Use the approach that yields the smallest compressed size, score using that model, and
# add the record to
# the chosen model.
# If the default model is chosen, clone it to form a new "known" model and
# add the data to it, incrementing the number of uses of the default model.


class KLStream:
	def __init__(self,attrs, Model=model.AVCOnlineModel):
		self.attrs = attrs
		self.n = 0
		self.k = 0
		self.l = len(attrs)
		self.Model = Model
		self.models = []
		self.unknown = self.Model(self.attrs)
		self.unknowns = 0

	def score_class(self,x):
		mincost = self.unknown.score(x)
		cl = None
		for j in range(0,self.k):
			cost = self.models[j].score(x)
			if cost < mincost:
				cl = j
				mincost = cost
		if cl == None:
			#clone the unknown model and add it to the models
			cl = self.k
			classcost = 0.0-numpy.log2((self.unknowns+1)/(self.n+self.k + self.unknowns + 1))
			self.models.append(self.Model(self.attrs))
			self.unknowns = self.unknowns + 1
			self.k = self.k + 1
		else:
			classcost = 0.0-numpy.log2((self.models[cl].n+1)/(self.n+self.k + self.unknowns + 1))
		#encode x using the chosen class
		totalcost = classcost + mincost
		self.models[cl].update(x)
		self.n = self.n + 1
		return (totalcost,cl)


def run(inputfile,outputfile, modelfile=None,
		Model=lambda header: KLStream(header,Model=model.AVCOnlineModel)):

	print(inputfile)
	print(outputfile)


	def writescores(csvfile,scorefile,scoreheader):
		reader = csv.reader(csvfile)
		header = next(reader)[1:]
		m = Model(header)
		scorefile.write(scoreheader)
		cost = 0.0
		n = 0
		for row in reader:
			(uuid,record) = utils.readRecord(header,row)
			(score,cl) = m.score_class(record)
			cost = cost + score
			n = n + 1
			scorefile.write("%s, %f, %d\n" % (uuid,score,cl))
		print("Total cost: %f  Entropy: %f" % (cost, cost/n))

	def writemodels(modelfile,m):
		attrs = sorted(m.attrs)
		modelfile.write('Size,' + ','.join(attrs) + '\n')
		for i in range(m.k):
			n = len(m.classes[i])
			modelfile.write(str(n) + ','
							+ ','.join([str((m.models[i].freqs[att]+1)/(n+2))
										for att in attrs])
							+ '\n')

	with open(inputfile,'rt') as csvfile, open(outputfile,'w') as scorefile:
		writescores(csvfile,scorefile,'UUID,Score,Class\n')
	if modelfile != None:
		with open(modelfile,'w') as modelfile:
			writemodels(modelfile,m)

