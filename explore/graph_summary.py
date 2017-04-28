#! /usr/bin/env python3

import query as q

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



def getVertexSummary():
    vertices = q.getGroupCountQuery(vertexQ)
    vdict = {}
    print("Vertex types/counts:")
    for v in vertices.keys():
        vdict[v] = {'count':vertices[v],
                    'in': q.getGroupCountQuery(vertexInCountQ(v)),
                    'out': q.getGroupCountQuery(vertexOutCountQ(v))}
    return vdict

def getEdgeSummary():
    edges = q.getGroupCountQuery(edgeQ)
    edict = {}
    print("Edge types/counts:")
    for e in edges.keys():
        edict[e] = {'count':edges[e],
                    'in': q.getGroupCountQuery(edgeInCountQ(e)),
                    'out': q.getGroupCountQuery(edgeOutCountQ(e))}
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
    vdict = getVertexSummary()
    printSummary(vdict)

    edict = getEdgeSummary()
    printSummary(edict)
    

