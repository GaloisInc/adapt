from query import *

# Queries to extract objects and attributes

all_cmdlinesQ = "g.V().has('eventType','EVENT_EXECUTE').out().values('cmdLine').as('cmd').select('cmd').dedup()"

all_filesQ = "g.V().hasLabel('FileObject').values('path').dedup().order()"

proc_event_fileQ = "g.V().hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').as('x').values('cid').as('pid').select('x').in().hasLabel('Event').as('y').values('eventType').as('type').select('y').out().hasLabel('FileObject').as('z').values('path').as('file').select('pid','file','type').dedup()"

all_principalsQ = "g.V().hasLabel('Principal').as('x').values('euid').as('euid').select('x').in().values('name').as('name').select('euid','name').dedup()"

all_netflowQ = "g.V().hasLabel('NetFlowObject').values('remoteAddress').dedup()"


proc_netflowQ = "g.V().hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').as('x').values('cid').as('pid').select('x').both().hasLabel('Event').both().hasLabel('NetFlowObject').values('remoteAddress').as('dest').select('pid','dest')"

proc_childQ = "g.V().hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').as('x').values('cid').as('parent').select('x').in('parentSubject').as('y').values('cid').as('child').select('parent','child')"

proc_child_nameQ = "g.V().hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').as('x').values('cid').as('parent').select('x').in('parentSubject').as('y').values('name').as('child').select('parent','child').dedup()"

proc_child_cmdLineQ = "g.V().hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').as('x').values('cid').as('parent').select('x').in('parentSubject').as('y').values('cmdLine').as('child').select('parent','child').dedup()"

proc_fileQ = "g.V().hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').as('x').values('cid').as('pid').select('x').in().hasLabel('Event').out().hasLabel('FileObject').as('z').values('path').as('file').select('pid','file').dedup()"

proc_event_fileQ = "g.V().hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').as('x').values('cid').as('pid').select('x').in().hasLabel('Event').as('y').values('eventType').as('type').select('y').out().hasLabel('FileObject').as('z').values('path').as('file').select('pid','file','type').dedup()"

proc_eventQ = "g.V().hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').as('x').values('cid').as('pid').select('x').in().hasLabel('Event').as('y').values('eventType').as('type').select('pid','type').dedup()"

proc_execQ = "g.V().hasLabel('Subject').has('subjectType','SUBJECT_PROCESS').as('x').values('cid').as('pid').select('x').both().has('eventType','EVENT_EXECUTE').out().values('cmdLine').as('cmd').select('pid','cmd').dedup()"



# A context is represented as a dictionary with keys = objects
# and values = sets of attributes


def makeContext(l,key,val): 
    return {x[key]:{y[val] for y in l if x[key] == y[key]} for x in l}

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
