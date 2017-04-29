import numpy
from query import *
from contexts import *


def getAtts(d): 
    atts = set()
    for x in d.values():
        atts = atts | x
    return atts

def countAtts(d):
    attCounts = {}
    for x in d.values():
        for a in x:
            if a in attCounts.keys():
                attCounts[a] = attCounts[a]+1
            else:
                attCounts[a] = 1
    return attCounts


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


def countAttPairs(d,pairs,thresh=5):
    counts = {}
    for pair in pairs:
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


def oneStepRePair(context,dictionary,thresh=5):
    attCounts = countAtts(context)
    atts = {att for att in attCounts.keys() if attCounts[att] > thresh}
    pairs = attPairs(atts)
    attPairCounts = countAttPairs(context,pairs,thresh)
    topPairs = sorted(attPairCounts, key=attPairCounts.get, reverse=True)
    if len(topPairs) > 0:
        (newContext,newDictionary) = mergePair((context,dictionary),topPairs[0],gensym('new_att'))
        return (True,newContext,newDictionary)
    else:
        return (False,context,dictionary)

def rePair(context,dictionary,thresh=5,showCost=False):
    if showCost:
        print(cost(context,dictionary))
    (b,newContext,newDictionary) = oneStepRePair(context,dictionary,thresh)
    if b:
        return (rePair(newContext,newDictionary,thresh,showCost))
    else:
        return (newContext,newDictionary)
