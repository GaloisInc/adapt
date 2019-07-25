import numpy
import dask.dataframe as dd
import operator
import collections
import re
import logging
#logging.basicConfig(format='%(asctime)s %(levelname)s [%(filename)s:%(lineno)d] %(message)s', level=logging.NONE)


# build successor relation and edges among concepts

def successor(concepts,c1,c2):
	return c1 < c2 and {c for c in concepts if c1 < c and c < c2} == set()

def edges(concepts):
	return {(c1,c2) for c1 in concepts for c2 in concepts if successor(concepts,c1,c2)}


# helper functions to format concepts

def tupToIndices(tup): # given a tuple t, returns a tuple whose values are the positions in which tuple t has a value of 1
	return tuple(i for i in range(len(tup)) if tup[i] == 1)

def indicesToNames(indices_tup,names_list): #given a list of names and a tuple t containing list positions/indices, returns a tuple of names
	return tuple(names_list[e] for e in indices_tup)


# helper functions for filtering redundant implication rules

def impRuleSubsumes(rule1,rule2):
	return rule1[0] <= rule2[0] and rule2[1] <= rule1[1]

def impRuleRedundant(rules,rule):
	return any(impRuleSubsumes(exrule,rule) == True for exrule in rules)

def updateImpRules(rules,r):
	return {rule for rule in rules if not impRuleSubsumes(r,rule)} | {r}

def filterSubsumedImp(rules):
	newRules = set()
	for rule in rules.keys():
		if not(impRuleRedundant(newRules,rule)):
			newRules = updateImpRules(newRules,rule)
	return {newRule : rules[newRule] for newRule in newRules}


class ContextProcessing():
	def __init__(self,input_file):
		logging.debug('Initializing context with input file '+input_file+' ...')
		self.inputfile = input_file
		self.full_context = dd.read_csv(input_file,header=0,low_memory=False) #contains all the csv content including the header
		self.header = list(self.full_context.head(1)) # csv header as a list
		self.attributes = self.header[1:]
		self.objects = self.full_context.values.compute()[:,0] # names of the data points
		self.num_attributes = len(self.attributes)
		self.num_objects = len(self.objects)
		self.data = self.full_context.values.compute()[:,1:]
		self.fca_stats = {'total':0,'closures':0, 'fail_canon':0, 'fail_fcbo':0, 'fail_support':0}
		self.scores = {}
		self.fca_properties = { 'min_support':0.5, 'min_confidence':0, 'num_rules':'*'}
		self.concepts = ()
		self.rules = {}
		logging.debug('Context initialized...')




	#--------------------------------------------------------------------------
	# FCA-related methods
	#--------------------------------------------------------------------------
	def setFcaProperties(self,dict_prop):
		for k in dict_prop.keys():
			if k in self.fca_properties and dict_prop[k] != self.fca_properties[k]:
				self.fca_properties[k] = dict_prop[k]

	#--------------------------------------------------------------------------
	# computeClosure, generateFrom and generateConcepts are a port
	# from the original FCbO algorithm C code
	#--------------------------------------------------------------------------

	#-----------------------------------------------------------------------
	# computeClosure: computes closures. Takes into account 
	# minimal support conditions
	#-----------------------------------------------------------------------
	def computeClosure(self,extent,intent,new_attribute):
		rows = numpy.flatnonzero(self.data[:,new_attribute]==1)
		C = numpy.zeros((self.num_objects,),dtype=int)
		D = numpy.ones((self.num_attributes,),dtype=int)
		intersect_extent_rows = rows[extent[rows]==1]
		supp = len(intersect_extent_rows)/self.num_objects
		for e in intersect_extent_rows:
			C[e] = 1
			for j in range(self.num_attributes):
				if self.data[e,j] == 0:
					D[j] = 0
		self.fca_stats['closures']+=1
		return C,D,supp

	#-------------------------------------------------------------------------------
	# generateFrom: main function of FCbO.
	#
	# Generates the concepts whose support is above a certain threshold.
	#
	# Currently a direct port of the original recursive algorithm.
	# For scalability reasons, it might be good to make this function iterative
	# in future versions of the code.
	#-------------------------------------------------------------------------------
	def generateFrom(self,extent,intent,new_attribute):
		concepts = {(tuple(extent),tuple(intent)),}
		if all(intent) != 1 and new_attribute <= self.num_attributes:
			for j in range(new_attribute,self.num_attributes):
				if intent[j] == 0:
					C,D,supp = self.computeClosure(extent,intent,j)
					skip = False
					for k in range(j-1):
						if D[k] != intent[k]:
							skip = True
							self.fca_stats['fail_canon'] += 1
							break
						if supp < self.fca_properties['min_support']:
							skip = True
							self.fca_stats['fail_support'] += 1
							break
					if skip == False:
						concept = self.generateFrom(C,D,j+1)
						concepts.update(concept)
		return concepts

	#-------------------------------------------------------------------------------
	# format concepts so that extents and intents contain named objects/attributes
	#-------------------------------------------------------------------------------

	def formatConcepts(self,concepts):
		transformed_concepts = {(tupToIndices(tup[0]),tupToIndices(tup[1])) for tup in concepts}
		transformed_concepts = {(indicesToNames(tup[0],self.objects),indicesToNames(tup[1],self.attributes)) for tup in transformed_concepts}
		return transformed_concepts

	#-------------------------------------------------------------------------------
	# Retrieve attribute names given an object name
	#-------------------------------------------------------------------------------

	def getAttributeNamesFromObjName(self,objname):
		obj_index = numpy.flatnonzero(self.objects==objname)
		if obj_index.size != 0:
			data_line = self.data[obj_index].ravel()
			if 1 in data_line:
				attributes = operator.itemgetter(*numpy.flatnonzero(data_line==1).tolist())(self.attributes)
			else:
				attributes = ()
		else:
			attributes = ()
		return attributes


	#-------------------------------------------------------------------------------
	# generateConcepts: generates all concepts whose support is above the
	# 'support' parameter
	#-------------------------------------------------------------------------------
	def generateConcepts(self,support):
		if support != self.fca_properties['min_support']:
			self.setFcaProperties({'min_support':support})
		start_extent = numpy.ones((self.num_objects,),dtype=int) #initialize the extent to an array of ones
		start_intent = numpy.zeros((self.num_attributes,),dtype=int)  #initialize the inttent to an array of zeros
		new_attribute = 0
		logging.debug('Generating FCA (FCbO) concepts for '+self.inputfile+'...')
		concepts = self.generateFrom(start_extent,start_intent,new_attribute) #generate the concepts
		#filter duplicate concepts
		logging.debug('De-duplicating FCA (FCbO) concepts for '+self.inputfile+'...')
		unfiltered_concepts = list(concepts)
		dic = collections.defaultdict(list)
		for i in range(len(unfiltered_concepts)):
			dic[unfiltered_concepts[i][0]].append((i,unfiltered_concepts[i][1].count(1)))
		concept_filter = [max(v,key=operator.itemgetter(1))[0] for v in dic.values()]
		filtered_concepts = set([unfiltered_concepts[c] for c in concept_filter])
		self.fca_stats['total'] = len(filtered_concepts)
		self.concepts = self.formatConcepts(filtered_concepts)
		logging.debug('FCA (FCbO) concepts generation for '+self.inputfile+' completed...')


	#----------------------------------------------------------------------------
	# Functions to compute the support of an itemset/concept intent (nominal
	# support or support scaled by the number of objects)
	#----------------------------------------------------------------------------

	def getConceptSupport(self,concept_intent):
		support = 0
		indices = [self.attributes.index(e) for e in concept_intent]
		for row in self.data:
			vals = row[indices]
			if all(vals == 1):
				support += 1
		return support

	def getItemsetScaledSupport(self,concept_intent):
		support = self.getConceptSupport(concept_intent)
		return support/self.num_objects

	#---------------------------------------------------------------------------------
	#generating non-redundant implication rules from concepts
	#---------------------------------------------------------------------------------
	def generateRules(self,support,confidence):
		logging.debug('Starting rule generation from FCA (FCbO) concepts for '+self.inputfile+'...')
		self.setFcaProperties({'min_support':support,'min_confidence':confidence})
		itemsets = {frozenset(c[1]) for c in self.concepts}
		edg = edges(itemsets)
		supports = {}
		logging.debug('Computation of required itemset supports'+self.inputfile+'...')
		for e in edg: #compute all the supports necessary for the rule computations
			if e[1] not in supports.keys():
				supports[e[1]] = self.getItemsetScaledSupport(e[1])
			if e[0] not in supports.keys():
				supports[e[0]] = self.getItemsetScaledSupport(e[0])
			if e[1]-e[0] not in supports.keys():
				supports[e[1]-e[0]] = self.getItemsetScaledSupport(e[1]-e[0])
		# compute implication rules along with their confidence and lift
		logging.debug('Computation of implication rules'+self.inputfile+'...')
		rules = {(e[0],e[1]-e[0]): (supports[e[1]]/supports[e[0]],supports[e[1]]/(supports[e[0]]*supports[e[1]-e[0]]))
		                 for e in edg if supports[e[0]] > 0 and supports[e[1]]/supports[e[0]] > self.fca_properties['min_confidence']}
		#filtering redundant rules
		logging.debug('Eliminating redundant rules'+self.inputfile+'...')
		reducedRules = filterSubsumedImp(rules)
		self.rules = reducedRules
		logging.debug('Rule generation for '+self.inputfile+' completed...')

	def violations(self,att_list):
		setS = (frozenset({att_list}) if type(att_list) == str else frozenset(att_list))
		vios = [e for e in self.rules.keys() if e[0] <= setS and not(e[1] <= setS)]
		ent = sum([-numpy.log2(1-self.rules[e][0]) for e in vios])
		entLift = max([self.rules[e][1] for e in vios]+ [0])
		count = len(vios)
		return {'conf':ent,'lift':entLift,'num':count}

	def fca_anomaly_score(self):
		logging.debug('Computing FCA-based anomaly scores for '+self.inputfile+'...')
		return {k:self.violations(self.getAttributeNamesFromObjName(k)) for k in self.objects}


	#--------------------------------------------------------------------------
	# More general methods
	#--------------------------------------------------------------------------

	def score(self,method='lift',score_file=''):
	# score_file is the path to the output file. If not provided, defaults to input_file path where
	# input_file extension is replaced by '.scored.csv'
		logging.debug('Computing anomaly scores of context objects for '+self.inputfile+' with FCA...')
		logging.debug('Computing anomaly scores for '+self.inputfile+'...')
		scores = self.fca_anomaly_score()
		method_header = 'FCA confidence score,FCA lift score'
		if method == 'lift':
			method_name = 'Lift_Score'
		elif method == 'conf':
			method_name = 'Confidence_Score'
		elif method == 'num':
			method_name = 'Score'
		header = 'Objects,'+method_name+'\n'

		logging.debug('Writing anomaly scores to file '+score_file+'...')
		with(open(score_file,'w')) as f:
			f.write(header)
			for k in scores.keys():
				f.write("%s, %f\n" % (k, scores[k][method]))
