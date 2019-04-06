import argparse
import csv
import numpy
import random

from . import model
from ..util import utils

# Input: list of n records with l attrs each, number k
# Output: assignment of records to clusters C1..Ck,
#         probability distributions p1..pk
# such that -|C1|log (|C1|/n) H(p1) - ... - |Ck|log (|Ck|/n) H(pk)
# is minimized / "small"


class KLMeans:

	# Step 1: Initialize classes randomly and initialize cost to |n|*|l|
	def __init__(self,attrs,data,k,epsilon=0.01):
		self.attrs = attrs
		self.data = data
		self.n = len(data)
		self.l = len(attrs)
		self.k = k
		self.cost = self.n*self.l
		self.epsilon = epsilon
		self.models = [model.AVCOnlineModel(self.attrs)
					   for i in range(0,self.k)]
		self.assignment  = [None for i in range(0, self.n)]
		for i in range(0,self.n):
			r = random.randint(0,k-1)
			self.assignment[i] = r
			self.models[r].update(data[i])




	# Step 2: Reclassify each record to the class that compresses it best.
	def reclassify(self):
		self.assignment  = [None for i in range(0, self.n)]
		for i in range(0,self.n):
			costs = [self.models[j].score(self.data[i])
					 for j in range(0,self.k)]
			mincost = min(costs)
			for j in range(0,self.k):
				if costs[j] == mincost:
					self.assignment[i] = j
					break

	# Step 3: Calculate new cost and see if it is smaller than old cost
	def recost(self):
		newcost = 0.0
		for i in range(0,self.n):
			j = self.assignment[i]
			classcode = 0.0-numpy.log2((self.models[j].n+1)/(self.n+self.k))
			newcost = newcost + classcode + self.models[j].score(self.data[i])
		self.cost = newcost

	# Step 4: Calculate distribution of each class.
	def calculate_distributions(self):
		self.models = [model.AVCOnlineModel(self.attrs) for i in range(0,self.k)]
		for i in range(0,self.n):
			self.models[self.assignment[i]].update(self.data[i])

	def score(self,rec):
		return (min( [self.models[i].score(rec) - numpy.log2((self.models[i].n+1)/(self.n+self.k))
					   for i in range(0,self.k)]))

	def score_class(self,rec):
		min_score = None
		min_class = None
		for i in range(0,self.k):
			score = self.models[i].score(rec) - numpy.log2((self.models[i].n+1)/(self.n+self.k))
			if min_score == None or score < min_score:
				min_score = score
				min_class = i
		return (min_score, min_class)

	def iterate(self):
		self.reclassify()
		self.recost()
		self.calculate_distributions()

# TODO: Make this a unit test somewhere

def test(k,n):
	data = [{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':1},{'a':1,'b':0},
			{'a':0,'b':0},{'a':0,'b':0},
			{'a':1,'b':1},{'a':0,'b':1},
			]
	x = KLMeans({'a','b'},data,k)
	for i in range(0,n):
		print(x.cost)
		oldcost = x.cost
		x.iterate()
		if x.cost > (1-x.epsilon)*oldcost:
			break
	print("Final cost: " + str(x.cost))
	for j in range(0,x.k):
		for rec in x.classes[j]:
			print (rec, j, x.score(rec))


# TODO: Unify this framing code with that in ad.py

def run(inputfile,outputfile,k,modelfile=None,epsilon=0.01):
	print(inputfile)
	print(outputfile)
	print(k)
	def buildmodel(csvfile,k):
		reader = csv.reader(csvfile)
		header = next(reader)[1:]

		data = []
		for row in reader:
			(uuid,record) = utils.readRecord(header,row)
			data.append(record)
		m = KLMeans(header,data,k)
		print(m.cost)
		while True:
			oldcost = m.cost
			m.iterate()
			print(m.cost)
			if m.cost > (1-m.epsilon)*oldcost:
				break
		m.recost()
		print("Final cost: " + str(m.cost))

		return m


	def writescores(csvfile,scorefile,m,scoreheader):
		reader = csv.reader(csvfile)
		header = next(reader)[1:]
		scorefile.write(scoreheader)
		totalscore = 0.0

		for row in reader:
			(uuid,record) = utils.readRecord(header,row)
			(score,cl) = m.score_class(record)
			totalscore = totalscore + score
			scorefile.write("%s, %f, %d\n" % (uuid,score,cl))
		modelcost = m.k*m.l*numpy.log2(m.n)

		print('Score: %f Model Cost: %f Total Cost: %f Entropy: %f' % (totalscore, modelcost, totalscore+modelcost, (totalscore+modelcost)/ m.n))

	def writemodels(modelfile,m):
		attrs = sorted(m.attrs)
		modelfile.write('Size,' + ','.join(attrs) + '\n')
		for i in range(m.k):
			n = len(m.classes[i])
			modelfile.write(str(n) + ','
							+ ','.join([str((m.models[i].freqs[att]+1)/(n+2))
										for att in attrs])
							+ '\n')

	models = [None for i in range(0,k-1)]
	for i in range(0,k-1):
		max_k = i+1
		with open(inputfile,'rt') as csvfile:
			models[i] = buildmodel(csvfile,i+1)
			models[i].cost = models[i].cost + models[i].k*models[i].l*numpy.log2(models[i].n)
		if i > 1 and models[i-1].cost < models[i].cost and models[i-2].cost < models[i-1].cost:
			break
	mincost = models[0].cost
	m = models[0]
	min_k = 1
	for i in range(0,max_k-1):
		if models[i].cost < mincost:
			mincost = models[i].cost
			m = models[i]
			min_k = i+1
	print("Chose k = " + str(min_k))

	with open(inputfile,'rt') as csvfile, open(outputfile,'w') as scorefile:
		writescores(csvfile,scorefile,m,'UUID,Score,Class\n')
	if modelfile != None:
		with open(modelfile,'w') as modelfile:
			writemodels(modelfile,m)

