// creates segment vertices for PID nodes, excluding already-existing ones

def createVertices(graph,g,criterion) {
  idWithProp = g.V().has(criterion).has(label,neq('Segment')).id().fold().next();
  existingSegNodes_parentIds = g.V().has('segment:name','byPID').values('parentVertexId').fold().next();
  idsToStore = idWithProp-existingSegNodes_parentIds; 
  if (idsToStore!=[]) { 
    for (i in idsToStore) {
      graph.addVertex(label,'Segment','parentVertexId',i,'segment:name','byPID',criterion,g.V(i).values(criterion).next())
    }
  }
}

def segmentNodesCreated(g) {
  g.V().has('segment:name','byPID').valueMap(true)
}


def createVerticesAndEdges(graph,g,criterion,radius) {
  idWithProp = g.V().has(criterion).has(label,neq('Segment')).id().fold().next(); 
  existingSegNodes_parentIds = g.V().has('segment:name','byPID').values('parentVertexId').fold().next();
  idsToStore = idWithProp - existingSegNodes_parentIds; 
  for (i in idWithProp) {
    sub = g.V(i).repeat(__.bothE().subgraph('sub').bothV().has(label,neq('Segment'))).times(radius).cap('sub').next();
    subtr = sub.traversal(); 
    if (i in idsToStore) { 
      s = graph.addVertex(label,'Segment','segment:name','byPID','pid',g.V(i).values('pid').next(),'parentVertexId',i)
    } else {
      s = g.V().has('segment:name','byPID').has('parentVertexId',i).next()
    }; 
    idNonLinkedNodes = subtr.V().id().fold().next()
                     - g.V().hasLabel('Segment').has('parentVertexId',i).outE('segment:includes').inV().id().fold().next();
    for (node in idNonLinkedNodes) {
      s.addEdge('segment:includes',g.V(node).next())
    }
  }
}



def createSeg2SegEdges(graph,g) {
  for (snode in g.V().has('segment:name','byPID').id().fold().next()) {
    linkedSeg = g.V(snode).as('a').out('segment:includes').out().in('segment:includes').dedup().where(neq('a')).id().fold().next()
              - g.V(snode).out('segment:edge').id().fold().next();
    for (s in linkedSeg) { 
      g.V(snode).next().addEdge('segment:edge',g.V(s).next())
    }
  }
}