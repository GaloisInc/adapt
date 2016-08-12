
def addTimeSegments(graph,g,delta) {
  t1 = new Date().getTime();
  segments =  g.V().has('startedAtTime',gte(0)).values('startedAtTime').map{t = it.get(); t - t % delta}.dedup().order();
  for(s in segments) {
    v = graph.addVertex(label,'Segment','segment:name','byTime','startedAtTime',s,'endedAtTime',s+delta);
    content = g.V().has('startedAtTime',gte(s).and(lt(s+delta))).has(label,neq('Segment'));
    for(z in content) {
      v.addEdge('segment:includes',z) 
    } 
  }
  t2 = new Date().getTime();
  return t2 - t1
}

def addTimeSegmentsFromTo(graph,g,Integer delta,Long start,Long end) {
  t1 = new Date().getTime();
  start = start - start % delta;
  end = end - end % delta + delta;
  segments =  g.V().has('startedAtTime',gte(start).and(lt(end))).values('startedAtTime').map{t = it.get(); t - t % delta}.dedup().order();
  for(s in segments) {
    v = graph.addVertex(label,'Segment','segment:name','byTime','startedAtTime',s,'endedAtTime',s+delta);
    content = g.V().has('startedAtTime',gte(s).and(lt(s+delta))).has(label,neq('Segment'));
    for(z in content) {
      v.addEdge('segment:includes',z) 
    } 
  }
  t2 = new Date().getTime();
  return t2 - t1
}



def removeTimeSegments(g) {
  g.V().has('segment:name','byTime').has(label,'Segment').drop().iterate()
}

// ts  = new Long[5]; for (int i = 0; i < 5; i++) { ts[i] = addTimeSegments(graph,g,1000*1000*60); removeTimeSegments(g) } 
