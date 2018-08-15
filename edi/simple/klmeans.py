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
	def __init__(self,attrs,data,k):
		self.attrs = attrs
		self.data = data
		self.n = len(data)
		self.l = len(attrs)
		self.k = k
		self.cost = self.n*self.l
		self.models = [model.AVCOnlineModel(self.attrs)
					   for i in range(0,self.k)]
		for i in range(0,self.n):
			r = random.randint(0,k-1)
			self.models[r].update(data[i])


	# Step 2: Calculate distribution of each class.
	def calculate_distributions(self):
		self.models = [model.AVCOnlineModel(self.attrs) for i in range(0,self.k)]
		for i in range(0,self.k):
			for rec in self.classes[i]:
				self.models[i].update(rec)


	# Step 3: Reclassify each record to the class that compresses it best.
	def reclassify(self):
		self.classes = [[] for i in range(0,self.k)]
		for i in range(0,self.n):
			costs = [self.models[j].score(self.data[i])
					 for j in range(0,self.k)]
			mincost = min(costs)
			for j in range(0,self.k):
				if costs[j] == mincost:
					self.classes[j].append(self.data[i])
					break

	# Step 4: Calculate new cost and see if it is smaller than old cost
	def recost(self):
		newcost = 0.0
		for i in range(0,self.k):
			cl = self.classes[i]
			classcode = 0.0-numpy.log2(len(cl)/self.n)
			for rec in cl:
				newcost = newcost + classcode + self.models[i].score(rec)
		self.cost = newcost

	def score(self,rec):
		return (min( [(self.models[i].score(rec)
					   - numpy.log2(self.models[i].n/self.n))
					   for i in range(0,self.k)]))

	def score_class(self,rec):
		min_score = None
		min_class = None
		for i in range(0,self.k):
			score = self.models[i].score(rec) - numpy.log2(self.models[i].n/self.n)
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
		x.iterate()
	print(x.cost)
	for j in range(0,x.k):
		for rec in x.classes[j]:
			print (rec, j, x.score(rec))


# TODO: Unify this framing code with that in ad.py

def run(inputfile,outputfile,k,n):
	print(inputfile)
	print(outputfile)
	print(k)
	print(n)
	def buildmodel(csvfile):
		reader = csv.reader(csvfile)
		header = next(reader)[1:]

		data = []
		for row in reader:
			(uuid,record) = utils.readRecord(header,row)
			data.append(record)
		m = KLMeans(header,data,k)
		for i in range(0,n):
			print(m.cost)
			m.iterate()
		print(m.cost)

		return m


	def writescores(csvfile,scorefile,m,scoreheader):
		reader = csv.reader(csvfile)
		header = next(reader)[1:]
		scorefile.write(scoreheader)

		for row in reader:
			(uuid,record) = utils.readRecord(header,row)
			(score,cl) = m.score_class(record)
			scorefile.write("%s, %f, %d\n" % (uuid,score,cl))

	with open(inputfile,'rt') as csvfile:
		m = buildmodel(csvfile)
	with open(inputfile,'rt') as csvfile, open(outputfile,'w') as scorefile:
		writescores(csvfile,scorefile,m,'UUID,Score,Class\n')

