import numpy
from query import *
from contexts import *


def getAtts(d): 
    atts = set()
    for x in d.values():
        atts = atts | x
    return atts


def cost(context,dictionary): 
    ctxCost = 0.0
    atts = getAtts(context)
    attCost = numpy.log2(len(atts))
    for k in context.values():
        ctxCost = ctxCost + len(k)*attCost
    dictCost = 0.0
    for k in dictionary.values():
        dictCost = dictCost + len(k)*attCost
    return (ctxCost+dictCost)

def attPairs(s): 
    return {frozenset({x,y}) for x in s for y in s if x < y}

def countAttPair(d,pair):
    return (len([x for x in d if pair <= d[x]]))


def countAttPairs(d,thresh):
    counts = {}
    for pair in attPairs(getAtts(d)):
        count = countAttPair(d,pair)
        if count > thresh:
            counts[pair] = count
    return counts

def mergePair(cd,pair,newName):
    newContext = cd[0].copy()
    newDictionary = cd[1].copy()
    newDictionary[newName] = pair
    for x in newContext.keys():
        if pair <= newContext[x]:
            newContext[x] = newContext[x].difference(pair)
            newContext[x] = newContext[x].union(frozenset({newName}))
    return (newContext,newDictionary)

i = 0
def gensym(s): 
    global i
    i = i + 1
    return (s + str(i))


def oneStepRePair(context,dictionary,thresh=2):
    atts = getAtts(context)
    pairs = attPairs(atts)
    attPairCounts = countAttPairs(context,thresh)
    topPairs = sorted(attPairCounts, key=attPairCounts.get, reverse=True)
    if len(topPairs) > 0:
        (newContext,newDictionary) = mergePair((context,dictionary),topPairs[0],gensym('new_att'))
        return (True,newContext,newDictionary)
    else:
        return (False,context,dictionary)

def rePair(context,dictionary):
    (b,newContext,newDictionary) = oneStepRePair(context,dictionary)
    if b:
        return (rePair(newContext,newDictionary))
    else:
        return (newContext,newDictionary)
