#! /usr/bin/env python3

#import numpy

from query import *
from contexts import *
from myconcepts import *


# simple-minded implication rules

def getRulesWithConfidence(context,concepts,minconf):
    supports = getSupports(context,concepts)
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

def findImpRules(context,concepts,confidence):
    rules = getRulesWithConfidence(context,concepts,confidence)
    reducedRules = filterSubsumedImp(rules)
    return reducedRules

def printRules(rules):
    for rule in rules.keys():
        print(str(set(rule[0])) + " implies " + str(set(rule[1])) + " [" + str(rules[rule] * 100) + "%]")


def violations(s,rules):
    ent = 0
    for e in rules.keys():
        if e[0] <= s and not(e[1] <= s):
            ent = ent - numpy.log2(1-rules[e])
    return ent

def allViolations(context,rules):
    return {k:violations(context[k],rules) for k in context.keys()}

# exact implications - not found by the above!

def exactRules(context,concepts): 
    rules = set()
    for x in concepts:
        for y in concepts:
            xy = x | y
            z = getConcept(context,xy)
            diff = z - xy
            if z in concepts and diff != set():
                rules = rules | {(xy,diff)}
    return (filterSubsumedImp({rule: 1.0 for rule in rules}))

def printVios(vios):
    for v in vios:
        print(v[0] + ' with score ' + str(v[1]))

def snd(x): return x[1]

if __name__ == '__main__':
    # for now, analyze a fixed context
    context = makeContext(getQuery(proc_eventQ),'pid','type')
    concepts = findAllConceptsWithMinSupp(context,0.05*len(context))
    rules = findImpRules(context,concepts,0.95)
    print("Found high-confidence implication rules:")
    print("========================================")
    printRules(rules)
    print("The top 10 rule violators are:")
    print("========================================")
    vios = allViolations(context,rules)
    printVios(sorted(vios.items(),key=snd,reverse=True)[0:10])
