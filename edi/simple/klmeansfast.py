

import csv
import numpy as np
import random as r

class KLMeansMatrix:

	def __init__(self,attrs,datamatx,k,epsilon=0.01):
		self.attrs = attrs
		self.datamatx = datamatx
		self.n = len(datamatx)
		self.m = len(attrs)
		self.k = k
		self.cost = self.n*self.m
		self.epsilon = epsilon
		self.classes = [r.randint(0,self.k-1) for row in datamatx]
		self.selectors = np.array([ [1 if i == j else 0 for i in range(self.k)] for j in self.classes])
		self.calculate_distributions()

	def calculate_distributions(self):
		correction = [1 for i in range(self.m)] + [2]
		freqs = np.matmul(self.selectors.transpose(),self.datamatx) + correction
		self.probs = np.array([row[0:-1]/row[-1] for row in freqs])
		self.classprobs = np.atleast_2d((freqs[:,-1]/sum(freqs[:,-1]))).transpose()

	def cost_matx(self):
		zerocosts = -np.log2(np.ones(self.probs.shape) - self.probs)
		a = - np.log2(self.probs) - zerocosts
		b = np.sum(zerocosts,axis=1,keepdims=True)
		return np.hstack((a,b))

	def scores(self):
		return np.matmul(self.datamatx, self.cost_matx().transpose())

	def classcosts(self):
		c = -np.log2(self.classprobs)
		return [c[cl] for cl in self.classes]

	def iterate(self):
		scores = self.scores()
		self.classes = np.argmin(scores,axis=1)
		self.selectors = np.array([ [1.0 if i == j else 0.0 for i in range(self.k)] for j in self.classes])
		self.calculate_distributions()
		classcosts = -np.log2(self.classprobs)
		self.cost = sum([sum([self.selectors[i][j] * (float(scores[i][j]) + float(classcosts[j]))
							  for j in range(self.k)])
						 for i in range(len(scores))])



	def go(self):
		self.iterate()
		print(self.cost)
		oldcost = self.cost
		while True:
			self.iterate()
			print(self.cost)
			if self.cost > (1-self.epsilon)*oldcost:
				break
			oldcost = self.cost


def readRecord(header,row):
	uuid = row[0]
	record = [int(x) for x in row[1:]]
	return (uuid,record)


def getCSV(inputfile):
	with open(inputfile,'rt') as csvfile:
		reader = csv.reader(csvfile)
		header = next(reader)[1:]
		data = []
		uuids = []
		for row in reader:
			(uuid,record) = readRecord(header,row)
			data.append(record)
			uuids.append(uuid)
	return (header,uuids,data)


def run(inputfile,outputfile,k,modelfile=None,epsilon=0.01):
	(attrs,uuids,data) = getCSV(inputfile)
	datamatx = np.hstack((data,np.ones((len(data),1))))
	min_cost = None
	min_klm = None
	for i in range(1,k):
		klm = KLMeansMatrix(attrs,datamatx,i)
		klm.go()
		cost = klm.cost + klm.k*klm.m*np.log2(klm.n)
		if min_cost == None or cost < min_cost:
			min_cost = cost
			min_klm = klm
	print('Minimum cost: %f achieved with k=%d' % (min_cost,min_klm.k))
	with open(outputfile,'w') as scorefile:
		scorefile.write('UUID,Score,Class\n')
		for (uuid,score,c_score,cl) in zip(uuids,min_klm.scores(),min_klm.classcosts(),min_klm.classes):
			scorefile.write("%s, %f, %d\n" % (uuid,score[cl]+c_score,cl))
