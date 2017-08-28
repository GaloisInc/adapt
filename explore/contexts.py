#! /usr/bin/env python3

import numpy
from query import *

# Queries to extract objects and attributes

all_cmdlinesQ = "g.V().has('eventType','EVENT_EXECUTE').out().values('cmdLine').as('cmd').select('cmd').dedup()"

all_filesQ = "g.V().hasLabel('FileObject').values('path').dedup().order()"

proc_event_fileQ = "g.V().hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').as('x').values('cid').as('pid').select('x').in().hasLabel('Event').as('y').values('eventType').as('type').select('y').out().hasLabel('FileObject').as('z').values('path').as('file').select('pid','file','type').dedup()"

all_principalsQ = "g.V().hasLabel('Principal').as('x').values('euid').as('euid').select('x').in().values('name').as('name').select('euid','name').dedup()"

all_netflowQ = "g.V().hasLabel('NetFlowObject').values('remoteAddress').dedup()"


proc_netflowQ = "g.V().hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').as('x').values('cid').as('pid').select('x').both().hasLabel('Event').both().hasLabel('NetFlowObject').values('remoteAddress').as('dest').select('pid','dest').dedup()"

proc_childQ = "g.V().hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').as('x').values('cid').as('parent').select('x').in('parentSubject').as('y').values('cid').as('child').select('parent','child')"

proc_child_nameQ = "g.V().hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').as('x').values('cid').as('parent').select('x').in('parentSubject').as('y').values('name').as('child').select('parent','child').dedup()"

proc_nameQ = "g.V().hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').as('x').values('cid').as('pid').select('x').values('name').as('name').select('pid','name').dedup()"

proc_child_cmdLineQ = "g.V().hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').as('x').values('cid').as('parent').select('x').in('parentSubject').as('y').values('cmdLine').as('child').select('parent','child').dedup()"

proc_fileQ = "g.V().hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').as('x').values('cid').as('pid').select('x').in().hasLabel('Event').out().hasLabel('FileObject').as('z').values('path').as('file').select('pid','file').dedup()"

proc_event_fileQ = "g.V().hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').as('x').values('cid').as('pid').select('x').in().hasLabel('Event').as('y').values('eventType').as('type').select('y').out().hasLabel('FileObject').as('z').values('path').as('file').select('pid','file','type').dedup()"

proc_eventQ = "g.V().hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').as('x').values('cid').as('pid').select('x').in().hasLabel('Event').as('y').values('eventType').as('type').select('pid','type').dedup()"

proc_execQ = "g.V().hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').as('x').values('cid').as('pid').select('x').both().has('eventType','EVENT_EXECUTE').values('cmdLine').as('cmd').select('pid','cmd').dedup()"



# A context is represented as a dictionary with keys = objects
# and values = sets of attributes

# Make a context out of a collection of records, grouping by 'key'
# ... quadratic, which is not ideal
def makeContextSlow(l,key,val): 
    return {x[key]:{y[val] for y in l if x[key] == y[key]} for x in l}


def makeContext(l,key,val):
    context = {}
    for x in l:
        if x[key] in context:
            context[x[key]] = context[x[key]] | {x[val]}
        else:
            context[x[key]] = {x[val]}
    return context

# Prefixes of a 
def prefixes(s):
    if s.rfind('/') > 0:
        pref = s.rsplit('/',1)[0]
        return {pref} | prefixes(pref)
    else:
        return set()


def allprefixes(s): 
    prefs = set()
    for x in s:
        prefs = prefs | prefixes(x)
    return prefs

def prefixCtxt(context):
    return {k : allprefixes(context[k]) for k in context.keys()}

# Convert an attribute into a bag of nonempty words
def bagofwords(s,sep):
    return {w for w in s.split(sep) if w != ''}

# Convert a set of attributes into the union of bags of words
def allbagofwords(s,sep):
    bags = set()
    for x in s:
        bags = bags | bagofwords(x,sep)
    return bags

def bagofwordsCtxt(context):
    return {k : allbagofwords(context[k]) for k in context.keys()}

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

# Estimate the cost of a context, i.e. |total number of attributes of all objects| * log_2 |number of possible attributes|
# If a dictionary mapping some attributes to sets fo other attributes is given,
# count its cost too.

def cost(context,dictionary={}): 
    ctxCost = 0.0
    atts = getAtts(context)
    attCost = numpy.log2(len(atts))
    for k in context.values():
        ctxCost = ctxCost + len(k)*attCost
    dictCost = 0.0
    for k in dictionary.values():
        dictCost = dictCost + len(k)*attCost
    return (ctxCost+dictCost)



def combineContexts(ctx1,ctx2):
    ctx = ctx1.copy()
    for k in ctx2.keys():
        if k in ctx1.keys(): 
            ctx[k] = ctx[k] | ctx2[k]
        else:
            ctx[k] = ctx2[k]
    return ctx
