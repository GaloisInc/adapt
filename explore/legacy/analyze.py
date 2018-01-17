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

def allViolations(context,rules):
    return {k:violations(context[k],rules) for k in context.keys()}

def printVios(vios):
    for v in vios:
        if v[1] > 0.0:
            print(v[0] + ' with score ' + str(v[1]))

def printViolations(vios,context,rules):
    for v in vios:
        if v[1] > 0.0:
            print('------------------------------------------')
            print(v[0] + ' with score ' + str(v[1]))
            print('------------------------------------------')
            for e in rules.keys():
                if e[0] <= context[v[0]] and not(e[1] <= context[v[0]]):
                    printImpRule(e,rules)
          


# simple-minded anti-implication rules (X -> ~Y means X implies not Y with high confidence, i.e. X implies Y with low confidence)

def getAntiImpRulesWithLowConfidence(context,concepts,maxconf):
    supports = getSupports(context,concepts)
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

def findAntiImpRules(context,concepts,confidence):
    rules = getAntiImpRulesWithLowConfidence(context,concepts,confidence)
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

def allAntiImpViolations(context,rules):
    return {k:antiImpViolations(context[k],rules) for k in context.keys()}

def printVios(vios):
    for v in vios:
        if v[1] > 0.0:
            print(v[0] + ' with score ' + str(v[1]))

def printAntiImpViolations(vios,context,rules):
    for v in vios:
        if v[1] > 0.0:
            print('------------------------------------------')
            print(v[0] + ' with score ' + str(v[1]))
            print('------------------------------------------')
            for e in rules.keys():
                if e[0] <= context[v[0]] and not(e[1] <= context[v[0]]):
                    printAntiImpRule(e,rules)
          


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
def disjointRules(context,concepts): 
    rules = set()
    for x in concepts:
        for y in concepts:
            if not(disjointRuleRedundant(rules,(x,y))):
                z = getConcept(context,x|y)
                if not(z in concepts):
                    rules = updateDisjRules(rules,(x,y))
    return rules

def getDisjRuleScaledSupports(context,rules):
    newRules = {}
    n = len(context) 
    for rule in rules:
        concept = rule[0]|rule[1]
        newRules[rule] = getSupport(context,concept)/n
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

def druleViolationsCtxt(context,drules):
    return {label:druleViolations(context[label],drules) for label in context.keys()}

def printDisjViolations(vios,context,drules):
    for v in vios:
        if v[1] > 0.0:
            print('------------------------------------------')
            print(v[0] + ' with score ' + str(v[1]))
            print('------------------------------------------')
            for e in drules.keys():
                if e[0] <= context[v[0]] and e[1] <= context[v[0]]:
                    printDisjRule(e,drules)
          

def doImpRules(context,concepts):
    rules = findImpRules(context,concepts,0.95)
    print("Found implication rules with confidence > 0.95:")
    print("========================================")
    printRules(rules)
    print("The top 10 implication rule violators are:")
    print("========================================")
    vios = allViolations(context,rules)
    printViolations(sorted(vios.items(),key=snd,reverse=True)[0:10],context,rules)

def doAntiImpRules(context,concepts):
    rules = findAntiImpRules(context,concepts,0.05)
    print("Found anti-implication rules with confidence < 0.05:")
    print("========================================")
    printAntiImpRules(rules)
    print("The top 10 anti-implication rule violators are:")
    print("========================================")
    vios = allAntiImpViolations(context,rules)
    printAntiImpViolations(sorted(vios.items(),key=snd,reverse=True)[0:10],context,rules)

def doDisjRules(context,concepts):
    drules = disjointRules(context,concepts)
    drulesSupp = getDisjRuleScaledSupports(context,drules)
    print("Found disjointness rules with confidence < 0.05:")
    print("========================================")
    printDisjRules(drulesSupp)
    print("The top 10 disjointness rules violators are:")
    print("========================================")
    dvios = druleViolationsCtxt(context,drules)
    printDisjViolations(sorted(dvios.items(),key=snd,reverse=True)[0:10],context,drulesSupp)
        
# this is a little silly, it should be equivalent to just filter out the
# frequent attributes since any frequent concept consists of frequent attributes
def abnormality(concepts,attrs):
    matches = {concept for concept in concepts if concept <= attrs}
    for match in matches:
        attrs = attrs - match
    return len(attrs)

def doAbnormalities(context,concepts):
    abnormals = {k: abnormality(concepts,context[k]) for k in context.keys()}
    print("The top 10 objects with the most infrequent attributes are:")
    print("========================================")
    printVios(sorted(abnormals.items(),key=snd,reverse=True)[0:10])

if __name__ == '__main__':
    for arg in sys.argv[1:]:
        context = None
        if arg == '--event':
            print('Analysis of events performed by processes')
            context = makeContext(getQuery(proc_eventQ),'pid','type')
        elif arg == '--netflow':
            print('Analysis of netflows by processes')
            context = makeContext(getQuery(proc_netflowQ),'pid','dest')
        else:
            break
        print("Done building context...")
        concepts = findAllConceptsWithMinSuppFrontier(context,0.05*len(context))
        print("Done building concept lattice...")

        for arg2 in sys.argv[1:]:
            if arg2 == '--imp':
                doImpRules(context,concepts)
            elif arg2 == '--antiimp':
                doAntiImpRules(context,concepts)
            elif arg2 == '--disjoint':
                doDisjRules(context,concepts)
            elif arg2 == '--abnormal':
                doAbnormalities(context,concepts)
