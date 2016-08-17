

def removePIDSegments(g) {
  g.V().has(label,'Segment').drop().iterate()
}

def getPIDSegments(g) {
  g.V().has(label,'Segment')
}

// creates segment vertices for PID nodes, excluding already-existing ones



def createVertices(graph,g,criterion) {
  t1 = new Date().getTime();
  idWithProp = g.V().has(criterion,gte(0)).has(label,neq('Segment')).id().fold().next();
  existingSegNodes_parentIds = g.V().hasLabel('Segment').values('parentVertexId').fold().next();
  idsToStore = idWithProp-existingSegNodes_parentIds; 
  if (idsToStore!=[]) { 
    for (i in idsToStore) {
      graph.addVertex(label,'Segment','parentVertexId',i,'segment:name','byPID',criterion,g.V(i).values(criterion).next())
    }
  }
  t2 = new Date().getTime();
  return t2 - t1
}

//ts = new Long[5]; for (int i = 0; i < 5; i++) { ts[i] = createVertices(graph,g,'pid'); removePIDSegments(g) } 


def segmentNodesCreated(g) {
  g.V().has('segment:name','byPID').valueMap(true)
}


def createVerticesAndEdges(graph,g,criterion,radius) {
  t1 = new Date().getTime();
  idWithProp = g.V().has(criterion,gte(0)).has(label,neq('Segment')).id().fold().next(); 
  existingSegNodes_parentIds = g.V().hasLabel('Segment').values('parentVertexId').fold().next();
  idsToStore = idWithProp - existingSegNodes_parentIds; 
  for (i in idWithProp) {
    sub = g.V(i).emit().repeat(__.bothE().subgraph('sub').bothV().has(label,neq('Segment'))).times(radius).cap('sub').next();
    subtr = sub.traversal(); 
    if (i in idsToStore) { 
      s = graph.addVertex(label,'Segment','segment:name','byPID','pid',g.V(i).values('pid').next(),'parentVertexId',i)
    } else {
      s = g.V().hasLabel('Segment').has('parentVertexId',i).next()
    }; 
    idNonLinkedNodes = subtr.V().id().fold().next()- g.V().hasLabel('Segment').has('parentVertexId',i).outE('segment:includes').inV().id().fold().next();
    for (node in idNonLinkedNodes) {
      s.addEdge('segment:includes',g.V(node).next())
    }
  }
  t2 = new Date().getTime();
  return t2 - t1;
}

//ts  = new Long[5]; for (int i = 0; i < 5; i++) { ts[i] =createVerticesAndEdges(graph,g,'pid',2); removePIDSegments(g) } 

def createSeg2SegEdges(graph,g) {
  t1 = new Date().getTime();
  for (snode in g.V().hasLabel('Segment').id().fold().next()) {
    linkedSeg = g.V(snode).as('a').out('segment:includes').out().in('segment:includes').dedup().where(neq('a')).id().fold().next()- g.V(snode).out('segment:edge').id().fold().next();
    for (s in linkedSeg) { 
      g.V(snode).next().addEdge('segment:edge',g.V(s).next())
    }
  }
  t2 = new Date().getTime();
  return t2 - t1;
}

//ts  = new Long[5]; for (int i = 0; i < 5; i++) { createVerticesAndEdges(graph,g,'pid',2); ts[i] = createSeg2SegEdges(graph,g); removePIDSegments(g) } 