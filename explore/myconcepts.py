from contexts import *

# A context is represented as a dictionary whose keys are objects and values are sets of attributes.

def getConcept(context,s):
    atts = frozenset(getAtts(context))
    for k in context.keys():
        if s <= context[k]:
            atts = atts & context[k]
    return atts

def extendConcept(context, concept):
    atts = frozenset(getAtts(context)) - concept
    return {getConcept(context,concept | frozenset({att})) for att in atts}

def findAllConcepts(context):
    knownConcepts = {frozenset()}
    conceptsTodo = [frozenset()]
    while conceptsTodo != []:
        nextConcept = conceptsTodo[0]
        conceptsTodo.remove(nextConcept)
        newConcepts = extendConcept(context,nextConcept)
        for newConcept in newConcepts:
            if newConcept not in knownConcepts:
                knownConcepts.add(newConcept)
                conceptsTodo = conceptsTodo + [newConcept]
    return knownConcepts

def getExtent(context,s):
    objs = frozenset()
    for k in context.keys():
        if s <= context[k]:
            objs = objs | {k}
    return objs

def getSupport(context,concept): 
    i = 0
    for k in context.keys():
        if concept <= context[k]:
            i = i+1
    return i        

def getSupports(context,concepts):
    return {concept: getSupport(context,concept) for concept in concepts}


def findAllConceptsWithMinSupp(context,minsupp):
    knownConcepts = set()
    conceptsTodo = [frozenset()]
    while conceptsTodo != []:
        nextConcept = conceptsTodo[0]
        conceptsTodo.remove(nextConcept)
        supp = getSupport(context,nextConcept)
        if nextConcept not in knownConcepts and supp >= minsupp:
            knownConcepts.add(nextConcept)
            newConcepts = extendConcept(context,nextConcept)
            for newConcept in newConcepts:
                conceptsTodo = conceptsTodo + [newConcept]
    return knownConcepts
