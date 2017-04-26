#! /usr/bin/env python3

import requests, readline, sys, json

# get available labels and their counts

vertexQ = "g.V().label().groupCount()"

edgeQ = "g.E().label().groupCount()"

def vertexCountQ(lbl):
    return "g.V().hasLabel('" + lbl + "').count()"

def edgeCountQ(lbl):
    return "g.E().hasLabel('" + lbl + "').count()"

def vertexOutCountQ(lbl):
    return "g.V().hasLabel('" + lbl + "').outE().label().groupCount()"

def vertexInCountQ(lbl):
    return "g.V().hasLabel('" + lbl + "').inE().label().groupCount()"

def edgeOutCountQ(lbl):
    return "g.E().hasLabel('" + lbl + "').outV().label().groupCount()"

def edgeInCountQ(lbl):
    return "g.E().hasLabel('" + lbl + "').inV().label().groupCount()"


def getQuery(query):
    try:
        resp = requests.post("http://localhost:8080/query/generic", data={"query": query}).json()
        return resp
    except Exception as e:
        print("There was an error processing your query:")
        print(e)

def getSingletonQuery(query):
    return getQuery(query)[0]


def getGroupCountQuery(query):
    dict = getSingletonQuery(query)
    return {k:int(dict[k]) for k in dict.keys()}


def printResp(resp):  
    if isinstance(resp,list):
        for line in resp:
            print(line)
    else:
        print(resp)
        

def getVertices():
    vertices = getGroupCountQuery(vertexQ)
    vdict = {}
    print("Vertex types/counts:")
    for v in vertices.keys():
        vdict[v] = {'count':vertices[v],
                    'in': getGroupCountQuery(vertexInCountQ(v)),
                    'out': getGroupCountQuery(vertexOutCountQ(v))}
    return vdict

def getEdges():
    edges = getGroupCountQuery(edgeQ)
    edict = {}
    print("Edge types/counts:")
    for e in edges.keys():
        edict[e] = {'count':edges[e],
                    'in': getGroupCountQuery(edgeInCountQ(e)),
                    'out': getGroupCountQuery(edgeOutCountQ(e))}
    return edict

def emitWithCount(k,c):
    return (k + "[" + str(c) + "]")

def emitCountSet(dict):
    return ("{" + ",\n".join([emitWithCount(k,dict[k]) for k in dict.keys()]) + "}")

def printSummary(vdict):
    for k in vdict.keys():
        print(emitCountSet(vdict[k]['in']))
        print(" -> ")
        print(emitWithCount(k,vdict[k]['count']))
        print(" -> ")
        print(emitCountSet(vdict[k]['out']))
        print("")

if __name__ == '__main__':
    print("Graph summarizer 0.1.")
    vdict = getVertices()
    print(vdict)

    edict = getEdges()
    print(edict)
    

