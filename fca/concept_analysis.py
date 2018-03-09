import numpy
import fcbo as fcbo
import math
import sys
import io
import operator
import itertools
import collections
import json

def openFile(output=sys.stdout):
	out=(output if (output==sys.stdout or isinstance(output,io.TextIOWrapper)==True) else open(output,'a+'))
	return out

#------------------------------------------------------------------------------------------------------------------------------------------------------
#Analysis part (James' code with the exception of (minimal) modifications needed to use the output of the ported FCbO algorithm)
#This part is likely to change in a future version
#------------------------------------------------------------------------------------------------------------------------------------------------------

# build successor relation and edges among concepts

def successor(concepts,c1,c2):
	return c1 < c2 and {c for c in concepts if c1 < c and c < c2} == set()

def edges(concepts):
	return {(c1,c2) for c1 in concepts for c2 in concepts if successor(concepts,c1,c2)}


# a few rule quality metrics

def computeItemsetPairSupports(itemset1,itemset2,context,itemsets,namedentities=False):
	supp_union=getConceptSupport(context,itemset1|itemset2,namedentities)
	supp_itemset1=getConceptSupport(context,itemset1,namedentities)
	supp_itemset2=getConceptSupport(context,itemset2,namedentities)
	return {'supp_union':supp_union,'supp_itemset1':supp_itemset1,'supp_itemset2':supp_itemset2}


def jaccard(supp_union,supp_itemset1,supp_itemset2): #Jaccard coefficient
	return supp_union/(supp_itemset1+supp_itemset2-supp_union)

def kulczynski(supp_union,supp_itemset1,supp_itemset2): #Kulczynski measure
	return 0.5*((supp_union/supp_itemset1)+(supp_union/supp_itemset2))

def klosgen(supp_union,supp_itemset1,supp_itemset2): #Klosgen measure
	return math.sqrt(supp_union)*((supp_union/supp_itemset1)-supp_itemset2)

def lift(supp_union,supp_itemset1,supp_itemset2): #lift (also called interest)
	return supp_union/(supp_itemset1*supp_itemset2)

def leverage(supp_union,supp_itemset1,supp_itemset2): #leverage or Piatetsky-Shapiro measure
	return supp_union-(supp_itemset1*supp_itemset2)

def imbalanceRatio(supp_union,supp_itemset1,supp_itemset2): #leverage or Piatetsky-Shapiro measure
	return (abs(supp_itemset1-supp_itemset2))/(supp_itemset1+supp_itemset2-supp_union)

#def mutualInfo():

def RPF(confidence,support):
	#implements rule power factor (rule "interest" measure introduced in "Rule Power Factor: A New Interest Measure in Associative Classification", Ochin et al., ICACC 2016)
	return confidence*support


def getConcept(context,s,namedentities=False):
	#the namedentities parameter specifies whether objects and attributes are named or identified by their position in the context matrix
	#(as is the case by default in a fimi file). This parameter is set to False by default (identification by position in context matrix),
	# it needs to be set to True for named objects and attributes
	if namedentities==False:
	   atts=frozenset(range(context.num_attributes))
	   keys=range(context.num_objects)
	else:
	   atts = frozenset(context.attributes)
	   keys=context.objects
	for k in keys:
		setAtts=set(context.getAttributeFromObject(k,namedentities))
		setS=({s} if type(s)==str else set(s))
		if  setS <= setAtts:
			atts = atts & setAtts
	return atts

# simple-minded implication rules

def getRulesWithConfidence(concepts,supports,minconf):
	edg = edges(concepts)
	return {(e[0],e[1]-e[0]): supports[e[1]]/supports[e[0]] for e in edg if supports[e[0]] > 0 and supports[e[1]]/supports[e[0]] > minconf}
	
def getRulesWithConfidenceLift(concepts,context,minconf,namedentities=False):
	edg = edges(concepts)
	supports={}
	for e in edg:
		if e[1] not in supports.keys():
			supports[e[1]]=getItemsetScaledSupport(context,e[1],namedentities)
		if e[0] not in supports.keys():
			supports[e[0]]=getItemsetScaledSupport(context,e[0],namedentities)
		if e[1]-e[0] not in supports.keys():
			supports[e[1]-e[0]]=getItemsetScaledSupport(context,e[1]-e[0],namedentities)
	#print(supports.keys())
	return {(e[0],e[1]-e[0]): (supports[e[1]]/supports[e[0]],supports[e[1]]/(supports[e[0]]*supports[e[1]-e[0]])) for e in edg if supports[e[0]] > 0 and supports[e[1]]/supports[e[0]] > minconf}

# a rule X1 -> Y2 subsumes another X2 -> Y2 if X1 subseteq X2 and Y2 subseteq Y1
# this is naive, it would be better to filter out rules that are implied
# by combinations of other rules also.

# Note that this is not equivalent to "rule1<=rule2"
def impRuleSubsumes(rule1,rule2):
	return rule1[0] <= rule2[0] and rule2[1] <= rule1[1]

#def impRuleRedundant(rules,rule):
	#for exrule in rules:
	#if impRuleSubsumes(exrule,rule):
		#return True
	#return False

def impRuleRedundant(rules,rule):
	return any(impRuleSubsumes(exrule,rule)==True for exrule in rules)

def updateImpRules(rules,r):
	return {rule for rule in rules if not impRuleSubsumes(r,rule)} | {r}

def filterSubsumedImp(rules):
	newRules = set()
	for rule in rules.keys():
		if not(impRuleRedundant(newRules,rule)):
			newRules = updateImpRules(newRules,rule)
	return {newRule : rules[newRule] for newRule in newRules}



def findImpRules_old(concepts,supports,confidence):
	rules = getRulesWithConfidence(concepts,supports,confidence)
	reducedRules = filterSubsumedImp(rules)
	return reducedRules
	
def findImpRules(concepts,context,confidence,namedentities=False):
	rules = getRulesWithConfidenceLift(concepts,context,confidence,namedentities)
	reducedRules = filterSubsumedImp(rules)
	return reducedRules


def violations_old(s,rules):
	setS=(frozenset({s}) if type(s)==str else frozenset(s))
	ent = 0-sum([numpy.log2(1-rules[e]) for e in rules.keys() if e[0]<=setS and not(e[1]<=setS)])
	return ent
	
def violations(s,rules):
	setS=(frozenset({s}) if type(s)==str else frozenset(s))
	ent = 0-sum([numpy.log2(1-rules[e][0]) for e in rules.keys() if e[0]<=setS and not(e[1]<=setS)])
	entLift = 0-sum([numpy.log2(1-rules[e][1]) for e in rules.keys() if e[0]<=setS and not(e[1]<=setS)])
	return (ent,entLift)

def allViolations(context,rules,namedentities=False):
	if namedentities==False:
		list_objects=range(context.num_objects)
	else:
		list_objects=context.objects
	return {k:violations(context.getAttributeFromObject(k,namedentities),rules) for k in list_objects}


def constructNonRedundantConceptCombinations(concepts):
	l=set()
	for (x,y) in itertools.product(concepts,concepts):
		if disjointRuleNotRedundant(l,(x,y)):
			l.add((x,y))
	return l

def constructNonRedundantConceptCombinationsWithSupports(context,concepts,namedentities=False):
	l=set()
	supports=dict()
	for (x,y) in itertools.product(concepts,concepts):
		if disjointRuleNotRedundant(l,(x,y)):
			l.add((x,y))
			supports[x|y]=getItemsetScaledSupport(context,x|y,namedentities)
			if x not in supports.keys():
				supports[x]=getItemsetScaledSupport(context,x,namedentities)
			if y not in supports.keys():
				supports[y]=getItemsetScaledSupport(context,y,namedentities)
	return {'combinations':l,'supports':supports}

#def constructImprovedRules(rules,improv_threshold)

def kulczynskiRule(concept_combis,supports,minkulc):
	if minkulc>=0.5:
		kulczynskiRules={e for e in concept_combis if kulczynski(supports[e[0]|e[1]],supports[e[0]],supports[e[1]])>=minkulc}
	else:
		kulczynskiRules={e for e in concept_combis if kulczynski(supports[e[0]|e[1]],supports[e[0]],supports[e[1]])<=minkulc}
	return kulczynskiRules


def imbalanceRule(concept_combis,supports,minImbalance):
	imbalanceRules={e for e in concept_combis if imbalanceRatio(supports[e[0]|e[1]],supports[e[0]],supports[e[1]])>=minImbalance}
	return imbalanceRules

def liftRule(concept_combis,supports,minlift):
	liftRules={e for e in concept_combis if lift(supports[e[0]|e[1]],supports[e[0]],supports[e[1]])>=minlift}
	return liftRules



def leverageRule(concept_combis,supports,minleverage):

	leverageRules={e for e in concept_combis if leverage(supports[e[0]|e[1]],supports[e[0]],supports[e[1]])>=minleverage}
	return leverageRules


def MutualInfoRule(concept_combis,supports,minMI):
	MIRules={e for e in concept_combis if leverage(supports[e[0]|e[1]],supports[e[0]],supports[e[1]])>=minMI}
	return MIRules


# simple-minded anti-implication rules (X -> ~Y means X implies not Y with high confidence, i.e. X implies Y with low confidence)

def getAntiImpRulesWithLowConfidence(concepts,supports,maxconf):
	edg = edges(concepts)
	return {(e[0],e[1]-e[0]): supports[e[1]]/supports[e[0]] for e in edg if supports[e[0]] > 0 and supports[e[1]]/supports[e[0]] < maxconf}
	
def getAntiImpRulesWithLowConfidenceLift(concepts,context,maxconf,namedentities=False):
	edg = edges(concepts)
	supports={}
	for e in edg:
		if e[1] not in supports.keys():
			supports[e[1]]=getItemsetScaledSupport(context,e[1],namedentities)
		if e[0] not in supports.keys():
			supports[e[0]]=getItemsetScaledSupport(context,e[0],namedentities)
		if e[1]-e[0] not in supports.keys():
			supports[e[1]-e[0]]=getItemsetScaledSupport(context,e[1]-e[0],namedentities)
	return {(e[0],e[1]-e[0]): (supports[e[1]]/supports[e[0]],supports[e[1]]/(supports[e[0]]*supports[e[1]-e[0]])) for e in edg if supports[e[0]] > 0 and supports[e[1]]/supports[e[0]] < maxconf}

# a rule X1 -> ~Y1 subsumes another X2 -> ~Y2 if X1 subseteq X2 and Y1 subseteq Y2
# this is naive, it would be better to filter out rules that are implied
# by combinations of other rules also
#def antiImpRuleSubsumes(rule1,rule2):
	#return rule1[0] <= rule2[0] and rule1[1] <= rule2[1]

def antiImpRuleSubsumes(rule1,rule2):
	return rule1<= rule2

#def antiImpRuleRedundant(rules,rule):
	#for exrule in rules:
	#if antiImpRuleSubsumes(exrule,rule):
		#return True
	#return False

def antiImpRuleRedundant(rules,rule):
	return any(antiImpRuleSubsumes(exrule,rule)==True for exrule in rules)


def updateAntiImpRules(rules,r):
	return {rule for rule in rules if not antiImpRuleSubsumes(r,rule)} | {r}

def filterSubsumedAntiImp(rules):
	newRules = set()
	for rule in rules.keys():
		if not(antiImpRuleRedundant(newRules,rule)):
			newRules = updateAntiImpRules(newRules,rule)
	return {newRule : rules[newRule] for newRule in newRules}

def findAntiImpRules_old(concepts,supports,confidence):
	rules = getAntiImpRulesWithLowConfidence(concepts,supports,confidence)
	reducedRules = filterSubsumedAntiImp(rules)
	return reducedRules
	
def findAntiImpRules(concepts,context,confidence,namedentities):
	rules = getAntiImpRulesWithLowConfidenceLift(concepts,context,confidence,namedentities)
	reducedRules = filterSubsumedAntiImp(rules)
	return reducedRules

def antiImpViolations_old(s,rules):
	setS=(frozenset({s}) if type(s)==str else frozenset(s))
	ent = 0-sum([numpy.log2(rules[e]) for e in rules.keys() if e[0]<=setS and e[1]<=setS])
	return ent
	
def antiImpViolations(s,rules):
	setS=(frozenset({s}) if type(s)==str else frozenset(s))
	ent = 0-sum([numpy.log2(rules[e][0]) for e in rules.keys() if e[0]<=setS and e[1]<=setS])
	entLift = 0-sum([numpy.log2(rules[e][1]) for e in rules.keys() if e[0]<=setS and e[1]<=setS])
	return (ent,entLift)

def allAntiImpViolations(context,rules,namedentities=False):
	if namedentities==False:
		list_objects=range(context.num_objects)
	else:
		list_objects=context.objects
	return {k:antiImpViolations(context.getAttributeFromObject(k,namedentities),rules) for k in list_objects}



# exact implications - not found by the above!

def exactRules(context,concepts,namedentities=False):
	rules = set()
	for x in concepts:
		for y in concepts:
			xy = x | y
			z = getConcept(context,xy,namedentities)
			print('z',z)
			diff = z - xy
			if z in concepts and diff != set():
				rules = rules | {(xy,diff)}
	return (filterSubsumedImp({rule: 1.0 for rule in rules}))


def snd(x): return x[1]

def comp(x): return (x[1][0],x[1][1])

# Disjointness rules

def disjointRuleImplied(r1,r2):
	(x1,y1) = r1
	(x2,y2) = r2
	return (x1 <= x2 and y1 <= y2) or (x1 <= y2 and y1 <= x2)


#def disjointRuleRedundant(rules,r):
	#for rule in rules:
	#if disjointRuleImplied(rule,r):
		#return True
	#return False

def disjointRuleRedundant(rules,r):
	return any(disjointRuleImplied(rule,r)==True for rule in rules)

def disjointRuleNotRedundant(rules,r):
	return all(disjointRuleImplied(rule,r)==False for rule in rules)

def updateDisjRules(rules,r):
	return {rule for rule in rules if not disjointRuleImplied(r,rule)} | {r}

#def disjointRules(context,concepts,namedentities=False):
	#rules = set()
	#for x in concepts:
	#for y in concepts:
		#if not(disjointRuleRedundant(rules,(x,y))):
		#z = getConcept(context,x|y,namedentities)
		#if not(z in concepts):
			#rules = updateDisjRules(rules,(x,y))
	#return rules


#def disjointRules2(context,concepts,namedentities=False):
	#possible_rules=
	#candidate_rules=dict(((x,y),getConcept(context,x|y,namedentities)) for x,y in itertools.product(concepts,concepts))
	#filtered_candidates={k for k,v in candidate_rules.items() if v not in concepts}
	#rules=set()
	#for e in filtered_candidates:
		#rules=updateDisjRules(rules,e)
	#return rules

def disjointRules(context,concepts,namedentities=False):
	rules = set()
	for x,y in itertools.product(concepts,concepts):
		if disjointRuleNotRedundant(rules,(x,y)):
				z = getConcept(context,x|y,namedentities)
				if not(z in concepts):
					rules = updateDisjRules(rules,(x,y))
	return rules

#def disjointRules3(context,concepts,namedentities=False):
	#possible_rules={(x,y) for x,y in itertools.product(concepts,concepts)}
	#rules
	##use bisect and check redundancy in whole set of possible unions of concepts
	##alternatively try doing the same with lift/leverage



def getScaledSupport(context,transformed_concepts,concept_intent,namedentities=False):
	support=getSupport(context,transformed_concepts,concept_intent,namedentities)
	return support/context.num_objects

def getItemsetScaledSupport(context,concept_intent,namedentities=False):
	support=getConceptSupport(context,concept_intent,namedentities)
	return support/context.num_objects

def getConceptSupport(context,concept_intent,namedentities=False):
	support=0
	#print('concept_intent',concept_intent)
	for row in context.context:
		if namedentities==True:
			indices=[context.attributes.index(e) for e in concept_intent]
		else:
			indices=concept_intent
		if indices!=[]:
			vals=operator.itemgetter(*indices)(row)
			if all(vals[i]=='1' for i in range(len(vals))):
				support+=1
		else:
			if all(row[i]=='0' for i in range(len(row))):
				support+=1
	return support


def getSupport(context,transformed_concepts,concept_intent,namedentities=False):
	supports=fcbo.getSupportsFromTransConcepts(transformed_concepts)
	if concept_intent in supports.keys():
		return supports[concept_intent]
	else:
		return getConceptSupport(context,concept_intent,namedentities)

def getItemsetsSupport(context,itemsets,concept_intent,namedentities=False):
	return getConceptSupport(context,concept_intent,namedentities)


def getDisjRuleScaledSupports(context,rules,namedentities=False):
	newRules = dict((rule,getItemsetScaledSupport(context,rule[0]|rule[1],namedentities)) for rule in rules)
	return newRules



def druleViolations(s,drules):
	count = 0
	for rule in drules:
		if rule[0] <= s and rule[1] <= s:
			count = count + 1
	return count

def druleViolationsCtxt(context,drules,namedentities=False):
	if namedentities==False:
	   list_objects=range(context.num_objects)
	else:
	   list_objects=context.objects
	return {label:druleViolations(context.getAttributeFromObject(label,namedentities),drules) for label in list_objects}


def doImpRules(context,concepts,min_conf,num_rules,namedentities=False):
	#supports=dict((c,getItemsetScaledSupport(context,c,namedentities)) for c in concepts)
	#rules = findImpRules(concepts,supports,min_conf)
	rules = findImpRules(concepts,context,min_conf,namedentities)
	vios = allViolations(context,rules,namedentities)
	violations=(sorted(vios.items(),key=comp,reverse=True)[0:num_rules] if type(num_rules)==int else sorted(vios.items(),key=comp,reverse=True))
	return {'type_rule':'implication','rules':rules,'violations':violations,'namedentities':namedentities,'confidence':min_conf}

def doAntiImpRules(context,concepts,max_conf,num_rules,namedentities=False):
	#supports=dict((c,getItemsetScaledSupport(context,c,namedentities)) for c in concepts)
	#rules = findAntiImpRules(concepts,supports,max_conf)
	rules = findAntiImpRules(concepts,context,max_conf,namedentities)
	vios = allAntiImpViolations(context,rules,namedentities)
	violations=(sorted(vios.items(),key=comp,reverse=True)[0:num_rules] if type(num_rules)==int else sorted(vios.items(),key=comp,reverse=True))
	return {'type_rule':'anti-implication','rules':rules,'violations':violations,'namedentities':namedentities,'confidence':max_conf}

#def doDisjRules(context,concepts,num_rules,namedentities=False):
	#drules = disjointRules(context,concepts,namedentities)
	#drulesSupp = getDisjRuleScaledSupports(context,drules,namedentities)
	#dvios = druleViolationsCtxt(context,drules,namedentities)
	#violations=sorted(dvios.items(),key=snd,reverse=True)[0:num_rules]
	#return {'type_rule':'disjointness','rules':drulesSupp,'violations':violations,'namedentities':namedentities,'confidence':0.05}

def doDisjRules(context,concepts,num_rules,namedentities=False):
	drules = disjointRules(context,concepts,namedentities)
	drulesSupp = getDisjRuleScaledSupports(context,drules,namedentities)
	dvios = druleViolationsCtxt(context,drules,namedentities)
	violations=(sorted(dvios.items(),key=snd,reverse=True)[0:num_rules] if type(num_rules)==int else sorted(dvios.items(),key=snd,reverse=True))
	return {'type_rule':'disjointness','rules':drulesSupp,'violations':violations,'namedentities':namedentities,'confidence':0.05}





def doLiftRules(context,concepts,minlift,num_rules,namedentities=False):
	supports=dict((c,getItemsetScaledSupport(context,c,namedentities)) for c in concepts)
	rules = liftRule(concepts,supports,minlift)
	vios = allViolations(context,rules,namedentities)
	violations=sorted(vios.items(),key=snd,reverse=True)[0:num_rules]
	return {'type_rule':'lift','rules':rules,'violations':violations,'namedentities':namedentities,'lift':minlift}


def doLeverageRules(context,concepts,minleverage,num_rules,namedentities=False):
	supports=dict((c,getItemsetScaledSupport(context,c,namedentities)) for c in concepts)
	rules = leverageRule(concepts,supports,minleverage)
	vios = allViolations(context,rules,namedentities)
	violations=sorted(vios.items(),key=snd,reverse=True)[0:num_rules]
	return {'type_rule':'leverage','rules':rules,'violations':violations,'namedentities':namedentities,'leverage':minleverage}


def doImbalanceRules(context,concepts,minImbalance,num_rules,namedentities=False):
	supports=dict((c,getItemsetScaledSupport(context,c,namedentities)) for c in concepts)
	rules = imbalanceRule(concepts,supports,minImbalance)
	vios = allViolations(context,rules,namedentities)
	violations=sorted(vios.items(),key=snd,reverse=True)[0:num_rules]
	return {'type_rule':'imbalance','rules':rules,'violations':violations,'namedentities':namedentities,'imbalance':minImbalance}


def doKulczynskiRules(context,concepts,minkulc,num_rules,namedentities=False):
	supports=dict((c,getItemsetScaledSupport(context,c,namedentities)) for c in concepts)
	rules = kulczynskiRule(concepts,supports,minkulc)
	vios = allViolations(context,rules,namedentities)
	violations=sorted(vios.items(),key=snd,reverse=True)[0:num_rules]
	return {'type_rule':'kulczynski','rules':rules,'violations':violations,'namedentities':namedentities,'kulczynski':minkulc}

ruleCalc={'lift':liftRule,'leverage':leverageRule,'imbalance':imbalanceRule,'kulczynski':kulczynskiRule}

def doUnitMeasureRule(rule_type,num_rules,min_threshold,context,concept_combis,supports):
	rules = ruleCalc[rule_type](concept_combis,supports,min_threshold)
	vios = allViolations(context,rules,namedentities)
	violations=sorted(vios.items(),key=snd,reverse=True)[0:num_rules]
	return {'type_rule':rule_type,'rules':rules,'violations':violations,'namedentities':namedentities,rule_type:min_threshold}

def doMeasureRules(rules,context,concepts,namedentities=False):
	combis=constructNonRedundantConceptCombinationsWithSupports(context,concepts,namedentities)
	l=[doUnitMeasureRule(k,v['num_rules'],v['min_threshold'],context,combis['combinations'],combis['supports']) for k,v in rules.items()]
	return l

#printing all types of rules
def printRules(rules,type_rule,print_flag=True,output=sys.stdout):
	if type_rule=='implication':
		toprint='\n'.join([''.join([str(set(rule[0]))," implies " , str(set(rule[1]))," [" , str(rules[rule] * 100),"%]"]) for rule in rules.keys()])
	elif type_rule=='anti-implication':
		toprint='\n'.join([''.join([str(set(rule[0]))," implies not ",str(set(rule[1]))," [" ,str(rules[rule] * 100),"%]"]) for rule in rules.keys()])
	elif type_rule=='disjointness':
		toprint='\n'.join([''.join([str(set(rule[0]))," is disjoint from ",str(set(rule[1])),"[", str(rules[rule]*100)+ "]"]) for rule in rules.keys()])
	if print_flag==True:
		out=openFile(output)
		print(toprint,file=out)
	else:
		return toprint

def printRulesGeneric(rules,middle_string,print_flag=True,output=sys.stdout):
	toprint='\n'.join([''.join([str(set(rule[0]))," ",middle_string," " , str(set(rule[1]))," [" , str(rules[rule] * 100),"%]"]) for rule in rules.keys()])
	if print_flag==True:
		out=openFile(output)
		print(toprint,file=out)
	else:
		return toprint


middleStringPerRule={'implication':'implies','anti-implication':'implies not','disjointness':'is disjoint from','lift':'is related to (lift)','leverage':'is related to (leverage)','kulczynski':'is related to (kulczynski)','imbalance':'is related to(imbalance)'}

def printRules2(rules,type_rule,print_flag=True,output=sys.stdout):
	return printRulesGeneric(rules,middleStringPerRule[type_rule],print_flag,output)


#printing rule violations

def printVios(vios,print_flag=True,output=sys.stdout):
	toprint='\n'.join([str(v[0]) + ' with score ' + str(v[1]) for v in vios if v[1]>0.0])
	if print_flag==True:
		out=openFile(output)
		print(toprint,file=out)
	else:
		return toprint


def printRuleViolations(vios,context,rules,type_rule,printing_flag=True,namedentities=False,output=sys.stdout,json_flag=False):
	out=openFile(output)
	sep_line='------------------------------------------'
	printlist=[]
	out_json = dict()
	for v in vios:
		#print('violation', v)
		if v[1] > 0.0:
			printlist+=[sep_line,str(v[0]) + ' with score ' + str(v[1]),sep_line]
			setAtts=frozenset(context.getAttributeFromObject(v[0],namedentities))
			violated_rules=dict((e,rules[e]) for e in rules.keys() if e[0] <= setAtts and not(e[1] <= setAtts))
			printlist+=[printRules2(violated_rules,type_rule,print_flag=False,output=out)]
			out_json[v[0]] = { 'score': v[1], 'rules': [ [ list(l), list(r) ] for l,r in violated_rules ] }
	printlist='\n'.join(printlist)
	if json_flag:
		return out_json
	elif printing_flag==True:
		print(printlist,file=out)
	else:
		return printlist
		
def produceScoreCSVPerRuleType(vios,context,rules,type_rule,namedentities=False):
	csv_output=[]
	#print('context.objects',context.objects)
	for v in vios:
		if v[1][0] > 0.0:
			#print('v[0]',v[0])
			obj=v[0]
			avg_entconf=v[1][0]
			avg_entlift=v[1][1]
			setAtts=frozenset(context.getAttributeFromObject(v[0],namedentities))
			violated_rules=dict((e,rules[e]) for e in rules.keys() if e[0] <= setAtts and not(e[1] <= setAtts))
			max_violated_rule_conf=max(violated_rules.items(), key=(lambda x: x[1][0]))
			max_violated_rule_lift=max(violated_rules.items(), key=(lambda x: x[1][1]))
			violated_rules_per_obj='#'.join([';'.join(list(list(v[0])))+'=>'+';'.join(list(list(v[1]))) for v in violated_rules.keys()])
			top_violated_rule_conf=';'.join(list(list(max_violated_rule_conf[0][0])))+'=>'+';'.join(list(list(max_violated_rule_conf[0][1])))
			top_violated_rule_lift=';'.join(list(list(max_violated_rule_lift[0][0])))+'=>'+';'.join(list(list(max_violated_rule_lift[0][1])))
			csv_output.append(','.join([obj,type_rule,violated_rules_per_obj,str(avg_entconf),top_violated_rule_conf,str(max_violated_rule_conf[1][0]),str(avg_entlift),top_violated_rule_lift,str(max_violated_rule_lift[1][0])]))
	return '\n'.join(csv_output)
			
def produceScoreCSV(rule_gen_results,context,output_csv):
	type_rule=rule_gen_results['type_rule']
	violations=rule_gen_results['violations']
	rules=rule_gen_results['rules']
	named_entities=rule_gen_results['namedentities']
	return produceScoreCSVPerRuleType(violations,context,rules,type_rule,namedentities=named_entities)


#printing rules and violations
def printComputedRules(rule_gen_results,context,output=sys.stdout,json_flag=False):
	type_rule=rule_gen_results['type_rule']
	violations=rule_gen_results['violations']
	rules=rule_gen_results['rules']
	named_entities=rule_gen_results['namedentities']
	measurePerRule={'implication':'confidence','anti-implication':'confidence','disjointness':'confidence','lift':'lift','leverage':'leverage','kulczynski':'kulczynski','imbalance':'imbalance'}
	confidence=rule_gen_results[measurePerRule[type_rule]]
	out=openFile(output)
	sep_line='------------------------------------------'
	sep_line2="========================================"
	preamble=[sep_line,type_rule[:1].upper()+type_rule[1:]+' rules',sep_line,'Found '+type_rule+' rules with '+measurePerRule[type_rule]+' '+str(confidence),sep_line2,]
	ruleprint=printRules2(rules,type_rule,print_flag=False)
	middle=[sep_line2,"The top "+str(len(violations))+" "+type_rule+" rule violators are:"]
	vios_print=printRuleViolations(violations,context,rules,type_rule,printing_flag=False,namedentities=named_entities,json_flag=json_flag)
	if json_flag:
		with open(type_rule + '.json','w') as outfile:
				json.dump(vios_print,outfile)
	else:
		toprint='\n'.join(preamble+[ruleprint]+middle+[vios_print])
		print(toprint,file=out)

#Luxemburger basis computation
#def generate_subsets()
#def luxemburger(concepts,min_confidence):

#------------------------------------------------------------------------------------------------------
#end of the analysis part
#------------------------------------------------------------------------------------------------------
