from . import database
from . import groundtruth


def getSummary(db,uuid):
    print('events')
    events = db.getQuery("MATCH (e:AdmEvent)-->(n:AdmSubject) WHERE n.uuid = '%s' RETURN DISTINCT e.eventType AS type" % uuid, endpoint="cypher")
    events = [e['type'] for e in events]
    print('execs')
    execs = db.getQuery("MATCH (n:AdmSubject)-->(pth:AdmPathNode) WHERE n.uuid = '%s' RETURN pth.path AS procName" % uuid, endpoint = "cypher")
    execs = [e['procName'] for e in execs if e['procName'] != None ]
    print('parents')
    parents = db.getQuery("MATCH (n:AdmSubject)-->(p:AdmSubject)--> (path:AdmPathNode) WHERE n.uuid = '%s' RETURN path.path AS parentProcName" % uuid, endpoint = "cypher")
    parents = [p['parentProcName'] for p in parents if p['parentProcName'] != None ]
    print('children')
    children = db.getQuery("MATCH (c:AdmSubject)-->(n:AdmSubject) MATCH (c: AdmSubject) --> (pth:AdmPathNode) WHERE n.uuid = '%s' RETURN  DISTINCT pth.path AS childProcName" % uuid, endpoint='cypher')
    children = [c['childProcName'] for c in children if c['childProcName'] != None]
    print('netflows')
    netflows = db.getQuery("MATCH (e:AdmEvent)-->(p:AdmSubject) MATCH (e:AdmEvent)-->(n:AdmNetFlowObject) WHERE n.remotePort <= 10000  AND n.remotePort <> -1  AND p.uuid = '%s' RETURN DISTINCT n.remotePort AS po, n.remoteAddress AS ip" % uuid, endpoint = 'cypher')
    #print('files')
    #files = db.getQuery("MATCH (x:AdmPathNode)<-[:path|:`(path)`]-(:AdmFileObject)<-[:predicateObject|:predicateObject1]-(e:AdmEvent)-[:subject]->(p:AdmSubject) WHERE p.uuid = '%s' RETURN DISTINCT x.path AS filepath, e.eventType as event" % uuid, endpoint="cypher")
    #files = [(f['filepath'],f['event']) for f in files]
    return {'uuid':uuid, 'events':events, 'execs': execs, 'parents': parents, 'children':children, 'netflows' : netflows}


def printFileActivity(files):
    d = dict()
    for (path,event) in sorted(files):
        if path in d.keys():
            d[path] = d[path] + [event]
        else:
            d[path] = [event]
    s = ""
    for path in d.keys():
        s = s + "\n\t\t%s : %s" % (path,d[path])
    return s

def printSummary(summ):
    print("\tParent(s): %s" % sorted(summ['parents']))
    print("\tExecutable(s): %s" % sorted(summ['execs']))
    print("\tChild(ren): %s" % sorted(summ['children']))
    print("\tEvents: %s" % sorted(summ['events']))
    print("\tNetwork activity: %s" % sorted(["%s:%d" % (n['ip'],n['po']) for n in summ['netflows']]))
    #print("\tFile activity: %s" % printFileActivity(summ['files']))

def writeSummary(file,summ):
    file.write("\tParent(s): %s\n" % sorted(summ['parents']))
    file.write("\tExecutable(s): %s\n" % sorted(summ['execs']))
    file.write("\tChild(ren): %s\n" % sorted(summ['children']))
    file.write("\tEvents: %s\n" % sorted(summ['events']))
    file.write("\tNetwork activity: %s\n" % sorted(["%s:%d" % (n['ip'],n['po']) for n in summ['netflows']]))
    #file.write("\tFile activity: %s\n" % printFileActivity(summ['files']))
