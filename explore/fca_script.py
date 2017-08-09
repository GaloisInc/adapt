import query_handling as q
import fcbo_core as fcbo
import argparse
import json
import os
import sys
import numpy

#------------------------------------------------------------------------------------------------------------------------------------------------------
#Analysis part (James' code with the exception of (minimal) modifications needed to use the output of the ported FCbO algorithm)
#This part is likely to change in a future version
#------------------------------------------------------------------------------------------------------------------------------------------------------

# build successor relation and edges among concepts
def successor(concepts,c1,c2):
    return c1 < c2 and {c for c in concepts if c1 < c and c < c2} == set()

def edges(concepts):
    return {(c1,c2) for c1 in concepts for c2 in concepts if successor(concepts,c1,c2)}
    
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
        if s <= context.getAttributeFromObject(k,namedentities):
            atts = atts & context.getAttributeFromObject(k,namedentities)
    return atts
    
# simple-minded implication rules

def getRulesWithConfidence(concepts,supports,minconf):
    edg = edges(concepts)
    return {(e[0],e[1]-e[0]): supports[e[1]]/supports[e[0]] for e in edg if supports[e[0]] > 0 and supports[e[1]]/supports[e[0]] > minconf}

# a rule X1 -> Y2 subsumes another X2 -> Y2 if X1 subseteq X2 and Y2 subseteq Y1
# this is naive, it would be better to filter out rules that are implied
# by combinations of other rules also
def impRuleSubsumes(rule1,rule2):
    return rule1[0] <= rule2[0] and rule2[1] <= rule1[1]

def impRuleRedundant(rules,rule):
    for exrule in rules:
        if impRuleSubsumes(exrule,rule):
            return True
    return False

def updateImpRules(rules,r):
    return {rule for rule in rules if not impRuleSubsumes(r,rule)} | {r}
    
def filterSubsumedImp(rules):
    newRules = set()
    for rule in rules.keys():
        if not(impRuleRedundant(newRules,rule)):
            newRules = updateImpRules(newRules,rule)
    return {newRule : rules[newRule] for newRule in newRules}    

def findImpRules(concepts,supports,confidence):
    rules = getRulesWithConfidence(concepts,supports,confidence)
    reducedRules = filterSubsumedImp(rules)
    return reducedRules

def printImpRule(rule,rules):
    print(str(set(rule[0])) + " implies " + str(set(rule[1])) + " [" + str(rules[rule] * 100) + "%]")

def printRules(rules):
    for rule in rules.keys():
        printImpRule(rule,rules)


def violations(s,rules):
    ent = 0
    for e in rules.keys():
        if e[0] <= s and not(e[1] <= s):
            ent = ent - numpy.log2(1-rules[e])
    return ent

def allViolations(context,rules,namedentities=False):
	if namedentities==False:
		list_objects=range(context.num_objects)
	else:
		list_objects=context.objects
	return {k:violations(context.getAttributeFromObject(k,namedentities),rules) for k in list_objects}

def printVios(vios):
    for v in vios:
        if v[1] > 0.0:
            print(str(v[0]) + ' with score ' + str(v[1]))

def printViolations(vios,context,rules,namedentities=False):
    for v in vios:
        if v[1] > 0.0:
            print('------------------------------------------')
            print(str(v[0]) + ' with score ' + str(v[1]))
            print('------------------------------------------')
            for e in rules.keys():
                if e[0] <= context.getAttributeFromObject(v[0],namedentities) and not(e[1] <= context.getAttributeFromObject(v[0],namedentities)):
                    printImpRule(e,rules)
          


# simple-minded anti-implication rules (X -> ~Y means X implies not Y with high confidence, i.e. X implies Y with low confidence)

def getAntiImpRulesWithLowConfidence(concepts,supports,maxconf):
    edg = edges(concepts)
    return {(e[0],e[1]-e[0]): supports[e[1]]/supports[e[0]] for e in edg if supports[e[0]] > 0 and supports[e[1]]/supports[e[0]] < maxconf}

# a rule X1 -> ~Y1 subsumes another X2 -> ~Y2 if X1 subseteq X2 and Y1 subseteq Y2
# this is naive, it would be better to filter out rules that are implied
# by combinations of other rules also
def antiImpRuleSubsumes(rule1,rule2):
    return rule1[0] <= rule2[0] and rule1[1] <= rule2[1]

def antiImpRuleRedundant(rules,rule):
    for exrule in rules:
        if antiImpRuleSubsumes(exrule,rule):
            return True
    return False

def updateAntiImpRules(rules,r):
    return {rule for rule in rules if not antiImpRuleSubsumes(r,rule)} | {r}
    
def filterSubsumedAntiImp(rules):
    newRules = set()
    for rule in rules.keys():
        if not(antiImpRuleRedundant(newRules,rule)):
            newRules = updateAntiImpRules(newRules,rule)
    return {newRule : rules[newRule] for newRule in newRules}    

def findAntiImpRules(concepts,supports,confidence):
    rules = getAntiImpRulesWithLowConfidence(concepts,supports,confidence)
    reducedRules = filterSubsumedAntiImp(rules)
    return reducedRules

def printAntiImpRule(rule,rules):
    print(str(set(rule[0])) + " implies not " + str(set(rule[1])) + " [" + str(rules[rule] * 100) + "%]")

def printAntiImpRules(rules):
    for rule in rules.keys():
        printAntiImpRule(rule,rules)


def antiImpViolations(s,rules):
    ent = 0
    for e in rules.keys():
        if e[0] <= s and e[1] <= s:
            ent = ent - numpy.log2(rules[e])
    return ent

def allAntiImpViolations(context,rules,namedentities=False):
	if namedentities==False:
		list_objects=range(context.num_objects)
	else:
		list_objects=context.objects
	return {k:antiImpViolations(context.getAttributeFromObject(k,namedentities),rules) for k in list_objects}

def printVios(vios):
    for v in vios:
        if v[1] > 0.0:
            print(str(v[0]) + ' with score ' + str(v[1]))

def printAntiImpViolations(vios,context,rules,namedentities=False):
    for v in vios:
        if v[1] > 0.0:
            print('------------------------------------------')
            print(str(v[0]) + ' with score ' + str(v[1]))
            print('------------------------------------------')
            for e in rules.keys():
                if e[0] <= context.getAttributeFromObject(v[0],namedentities) and not(e[1] <= context.getAttributeFromObject(v[0],namedentities)):
                    printAntiImpRule(e,rules)
          
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

# Disjointness rules

def disjointRuleImplied(r1,r2):
    (x1,y1) = r1
    (x2,y2) = r2
    return (x1 <= x2 and y1 <= y2) or (x1 <= y2 and y1 <= x2)
    
    
def disjointRuleRedundant(rules,r):
    for rule in rules:
        if disjointRuleImplied(rule,r):
            return True
    return False


def updateDisjRules(rules,r):
    return {rule for rule in rules if not disjointRuleImplied(r,rule)} | {r}
def disjointRules(context,concepts,namedentities=False): 
    rules = set()
    for x in concepts:
        for y in concepts:
            if not(disjointRuleRedundant(rules,(x,y))):
                z = getConcept(context,x|y,namedentities)
                if not(z in concepts):
                    rules = updateDisjRules(rules,(x,y))
    return rules
    
def getScaledSupport(context,transformed_concepts,concept_intent):
	supports=context.getScaledSupportsFromTransConcepts(transformed_concepts)
	if concept_intent in supports.keys():
		return supports[concept_intent]
	else:
		return 0
	
	

def getDisjRuleScaledSupports(context,transformed_concepts,rules):
    newRules = {}
    #n = len(context) 
    for rule in rules:
        concept = rule[0]|rule[1]
        newRules[rule] = getScaledSupport(context,transformed_concepts,concept)
    return newRules


def printDisjRules(rules): 
    for rule in rules.keys():
        printDisjRule(rule,rules)

def printDisjRule(rule,rules): 
    print(str(set(rule[0])) + " is disjoint from " + str(set(rule[1])) + "[" + str(rules[rule]*100)+ "]")

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

def printDisjViolations(vios,context,drules,namedentities=False):
    for v in vios:
        if v[1] > 0.0:
            print('------------------------------------------')
            print(str(v[0]) + ' with score ' + str(v[1]))
            print('------------------------------------------')
            for e in drules.keys():
                if e[0] <= context.getAttributeFromObject(v[0],namedentities) and e[1] <= context.getAttributeFromObject(v[0],namedentities):
                    printDisjRule(e,drules)
          

def doImpRules(context,concepts,supports,namedentities=False):
    rules = findImpRules(concepts,supports,0.95)
    print("Found implication rules with confidence > 0.95:")
    print("========================================")
    printRules(rules)
    print("The top 10 implication rule violators are:")
    print("========================================")
    vios = allViolations(context,rules,namedentities)
    printViolations(sorted(vios.items(),key=snd,reverse=True)[0:10],context,rules,namedentities)

def doAntiImpRules(context,concepts,supports,namedentities=False):
    rules = findAntiImpRules(concepts,supports,0.05)
    print("Found anti-implication rules with confidence < 0.05:")
    print("========================================")
    printAntiImpRules(rules)
    print("The top 10 anti-implication rule violators are:")
    print("========================================")
    vios = allAntiImpViolations(context,rules,namedentities)
    printAntiImpViolations(sorted(vios.items(),key=snd,reverse=True)[0:10],context,rules,namedentities)

def doDisjRules(context,concepts,transformed_concepts,namedentities=False):
    drules = disjointRules(context,concepts,namedentities)
    drulesSupp = getDisjRuleScaledSupports(context,transformed_concepts,drules)
    print("Found disjointness rules with confidence < 0.05:")
    print("========================================")
    printDisjRules(drulesSupp)
    print("The top 10 disjointness rules violators are:")
    print("========================================")
    dvios = druleViolationsCtxt(context,drules,namedentities)
    printDisjViolations(sorted(dvios.items(),key=snd,reverse=True)[0:10],context,drulesSupp,namedentities)
        
#------------------------------------------------------------------------------------------------------
#end of the analysis part
#------------------------------------------------------------------------------------------------------


#-------------------------------------------------------------------------------------------------------
#definition of command line arguments
#------------------------------------------------------------------------------------------------------------

parser=argparse.ArgumentParser(description='FCbO concept generation and analysis input arguments')
parser.add_argument('--inputfile','-i',help='Full path to FCA input file including extension (cxt or fimi). If not given, --specfile/-s is required.',default='')
parser.add_argument('--specfile','-s',help="Context specification file (json format+contains 'spec' in filename) that generates FCA input context. If not given, --inputfile/-i is required",default='')
#either an context input file (in cxt or fimi format) or a query specification file or both have to be provided. If only a context input file is provided, the context in the file is used as
#input to FCbO. If only a query specification file is provided, a query is sent to the database to generate a temporary context that is used as input to FCbO. If both a context input file
# and a query specification file are provided, a query is sent to the database to generate a context that is saved to the input file. The input file is then used as input to FCbO. 
#Either an context input file (in fimi or cxt format) or a query specification file (in json format+contains 'spec' in filename) is required. Otherwise, an error is thrown.

parser.add_argument('--min_support','-m',help="Minimum support of the concepts to be computed (float required). Default:0",type=float,default=0)
#argument to specify the minimal support of the concepts to be returned by FCbO. A default of 0 is set, which means that all concepts are genrated by default.

parser.add_argument('--disable_naming','-dn',help='Only applies when context is directly generated from json specification file or when context is a CXT file. Turns off naming of object and attribute entities in concepts. Objects and attributes only referred to by their position (index) in the context.',action='store_true')
#This argument cannot be invoked with an input FIMI file. It can be invoked with a json query specification file or an input file in CXT format. If invoked, the objects and attributes
#are identified by their positions in the context matrix (as is the case for FIMI files) and not by name.

parser.add_argument('--outputfile','-o',help='Full path to output file (saving concepts). If not specified, the concepts are printed on the screen.',default=sys.stdout)
#specifies where to save the FCbO concepts. By default, the concepts are just printed out on the screen

parser.add_argument('--analysis','-a',help='Specify whether to analyze the concepts further or not', action='store_true')
#specifies whether to analyze the concepts after they have been computed. Doesn't make use of the saved concepts files for now. 
#In an upcoming version, it will be possible to do the concepts computation and the analysis separately.


#launches concept generation with FCbO then analysis
#this part will need to be factorized in a subsequent version

if __name__ == '__main__':	
	args=parser.parse_args()
	if args.inputfile=='' and args.specfile=='': #either a specification file or a input (context file) must be supplied)
		parser.error('Either --specfile/-s or --inputfile/-i is required')# if not an error is thrown
	print('Starting FCA. Generating concepts...\n')	
	#retrieving the arguments from the command line
	inputfile=args.inputfile
	if inputfile!='':
		fileext=os.path.splitext(inputfile)[1].replace('.','')
	else:
		fileext=''
	specfile=args.specfile
	outputfile=args.outputfile
	min_support=args.min_support
	disable_naming=args.disable_naming
	
	#print('inputfile',inputfile)
	#print('fileext',fileext)
	#print('specfile',specfile)
	#print('min_support',min_support)
	#print('min_support type',type(min_support))
	#print('disable_naming',disable_naming)
	#print('outputfile',outputfile)
		
	if inputfile=='': 
		print('case where only specification supplied')
	#if no input file is specified (which means a specification has been specified),
		fca_context=fcbo.Context(specfile)#we query localhost based on the specification file), parse it to a context
		num_att=fca_context.num_attributes
		num_obj=fca_context.num_objects
		# then run FCbO on the context
		if min_support==0:
			concepts=fca_context.generateAllIntents() #generate all concepts if the minimal support is set to 0
		else:
			concepts=fca_context.generateAllIntentsWithSupp(min_support) #otherwise only generate the concepts whose minimal support is above the value provided by the user
		if disable_naming:
			named=False
		else:
			named=True
		fca_context.printConcepts(concepts,output=outputfile,namedentities=named)#output the concepts
		trans_concepts=fca_context.returnConcepts(concepts,namedentities=named)
		itemsets=fca_context.returnItemsets(concepts,namedentities=named)
	elif specfile=='': #if no specification file is supplied 
		
		print('case where only input file supplied')
		fca_context=fcbo.Context(inputfile)#a context input file has to be specified (in cxt or fimi format)
		if fileext=='cxt': #handling of CXT input files
			num_att=fca_context.num_attributes
			num_obj=fca_context.num_objects
			#run FCbO
			if min_support==0:
				concepts=fca_context.generateAllIntents() #generate all concepts if the minimal support is set to 0
			else:
				concepts=fca_context.generateAllIntentsWithSupp(min_support) #otherwise only generate the concepts whose minimal support is above the value provided by the user
			if disable_naming:
				named=False
			else:
				named=True
			fca_context.printConcepts(concepts,output=outputfile,namedentities=named)#output the concepts
			trans_concepts=fca_context.returnConcepts(concepts,namedentities=named)
			itemsets=fca_context.returnItemsets(concepts,namedentities=named)
		elif fileext=='fimi':
			named=False
			if min_support==0:
				concepts=fca_context.generateAllIntents() #generate all concepts if the minimal support is set to 0
			else:
				concepts=fca_context.generateAllIntentsWithSupp(min_support) #otherwise only generate the concepts whose minimal support is above the value provided by the user
			fca_context.printConcepts(concepts,output=outputfile,namedentities=named) #output the concepts
			trans_concepts=fca_context.returnConcepts(concepts,namedentities=named)
			itemsets=fca_context.returnItemsets(concepts,namedentities=named)
	else: #case where both a query specification file and a context input file are specified
		print('case where both specification and input file supplied')
		spec=fcbo.loadContextSpec(specfile) # a context is generated using the query specification file
		query=spec['query']
		obj_name=spec['objects']
		att_name=spec['attributes']
		res=q.getQuery(query)
		if fileext=='cxt': #if the input file is a CXT file
			q.convertQueryRes2Cxt(res,obj_name,att_name,inputfile) # save the generated context to a CXT file
			fca_context=fcbo.Context(inputfile)
			num_att=fca_context.num_attributes
			num_obj=fca_context.num_objects
			#run FCbO
			if min_support==0:
				concepts=fca_context.generateAllIntents() #generate all concepts if the minimal support is set to 0
			else:
				concepts=fca_context.generateAllIntentsWithSupp(min_support) #otherwise only generate the concepts whose minimal support is above the value provided by the user
			if disable_naming:
				named=False
			else:
				named=True
			fca_context.printConcepts(concepts,output=outputfile,namedentities=named)#output the concepts
			trans_concepts=fca_context.returnConcepts(concepts,namedentities=named)
			itemsets=fca_context.returnItemsets(concepts,namedentities=named)
		elif fileext=='fimi':  #if the input file is a FIMI file
			named=False 
			q.convertQueryRes2Fimi(res,obj_name,att_name,inputfile) # save the generated context to a FIMI file
			fca_context=fcbo.Context(inputfile) 
			#run FCbO
			if min_support==0:
				concepts=fca_context.generateAllIntents() #generate all concepts if the minimal support is set to 0
			else:
				concepts=fca_context.generateAllIntentsWithSupp(min_support) #otherwise only generate the concepts whose minimal support is above the value provided by the user
			fca_context.printConcepts(concepts,output=outputfile,namedentities=named)#output the concepts
			trans_concepts=fca_context.returnConcepts(concepts,namedentities=named)
			itemsets=fca_context.returnItemsets(concepts,namedentities=named)
			
	print('FCA finished. Concepts generated')
	#run the analysis of the concepts if the 'analysis' flag found
	if args.analysis:
		supports=fca_context.getSupportsFromTransConcepts(trans_concepts)
		print('-------------------------------------\nImplication rules\n--------------------------------------\n')
		doImpRules(fca_context,itemsets,supports,namedentities=named)
		print('-------------------------------------\nAnti-implication rules\n--------------------------------------\n')
		doAntiImpRules(fca_context,itemsets,supports,namedentities=named)
		print('-------------------------------------\nDisjointness rules\n--------------------------------------\n')
		doDisjRules(fca_context,itemsets,trans_concepts,namedentities=named)

	
			
			
				
